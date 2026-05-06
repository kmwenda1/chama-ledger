package ke.co.chamaledger.chamalegder.service;

import ke.co.chamaledger.chamalegder.entity.ChamaMember;
import ke.co.chamaledger.chamalegder.model.FundLedger;
import ke.co.chamaledger.chamalegder.mpesa.repository.FundLedgerRepository;
import ke.co.chamaledger.chamalegder.notification.SmsService;
import ke.co.chamaledger.chamalegder.repository.ChamaMemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReportService {

    private static final List<String> REPORT_RECIPIENT_ROLES = List.of("CHAIRPERSON", "TREASURER");
    private static final int WEEKLY_REPORT_DAYS = 7;
    private static final int MAX_DEFAULTER_NAMES_IN_SMS = 5;
    private static final int MAX_EXECUTIVE_SUMMARY_SMS_CHARS = 180;
    private static final int MAX_SMS_CHARS = 480;

    private final FundLedgerRepository fundLedgerRepository;
    private final ChamaMemberRepository chamaMemberRepository;
    private final GroqAiService groqAiService;           // ✅ Replaced GeminiAiService
    private final SmsService smsService;

    public WeeklyChamaHealthReport generateWeeklyHealthReport() {
        LocalDateTime periodEnd = LocalDateTime.now();
        LocalDateTime periodStart = periodEnd.minusDays(WEEKLY_REPORT_DAYS);

        BigDecimal totalContributions = fundLedgerRepository
                .sumContributionCreditsBetween(periodStart, periodEnd)
                .orElse(BigDecimal.ZERO);
        BigDecimal currentBalance = fundLedgerRepository.findTopByOrderByIdDesc()
                .map(FundLedger::getRunningBalance)
                .orElse(BigDecimal.ZERO);

        List<ChamaMember> activeMembers = chamaMemberRepository.findActiveMembersWithUsers();
        Set<String> contributorPhones = fundLedgerRepository.findContributionPhoneNumbersBetween(periodStart, periodEnd)
                .stream()
                .map(this::canonicalPhone)
                .filter(phone -> !phone.isBlank())
                .collect(Collectors.toSet());

        List<String> defaulters = activeMembers.stream()
                .filter(member -> !hasContributedThisWeek(member, contributorPhones))
                .map(this::memberName)
                .distinct()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();

        String executiveSummary = groqAiService.generateExecutiveSummary(  // ✅ Replaced Gemini call
                money(totalContributions),
                money(currentBalance),
                defaulters.size(),
                activeMembers.size()
        );

        return new WeeklyChamaHealthReport(
                periodStart,
                periodEnd,
                money(totalContributions),
                money(currentBalance),
                defaulters,
                executiveSummary
        );
    }

    public void sendWeeklyHealthReport() {
        WeeklyChamaHealthReport report = generateWeeklyHealthReport();
        List<ChamaMember> recipients = uniqueRecipientsByPhone(
                chamaMemberRepository.findActiveMembersByRoles(REPORT_RECIPIENT_ROLES)
        );

        if (recipients.isEmpty()) {
            log.warn("No active CHAIRPERSON or TREASURER members found for weekly Chama health report");
            return;
        }

        String message = buildSmsMessage(report);
        recipients.forEach(member -> {
            String phoneNumber = member.getUser().getPhoneNumber();
            log.info("Sending weekly Chama health report to {} ({})", memberName(member), phoneNumber);
            smsService.sendSms(phoneNumber, message);
        });
    }

    private boolean hasContributedThisWeek(ChamaMember member, Set<String> contributorPhones) {
        if (member.getUser() == null) {
            return false;
        }
        String memberPhone = canonicalPhone(member.getUser().getPhoneNumber());
        return !memberPhone.isBlank() && contributorPhones.contains(memberPhone);
    }

    private List<ChamaMember> uniqueRecipientsByPhone(List<ChamaMember> recipients) {
        Map<String, ChamaMember> membersByPhone = new LinkedHashMap<>();
        recipients.forEach(member -> {
            if (member.getUser() == null) {
                return;
            }

            String phone = canonicalPhone(member.getUser().getPhoneNumber());
            if (!phone.isBlank()) {
                membersByPhone.putIfAbsent(phone, member);
            }
        });
        return List.copyOf(membersByPhone.values());
    }

    private String buildSmsMessage(WeeklyChamaHealthReport report) {
        String defaulterText = formatDefaulters(report.defaulters());
        String summary = abbreviate(cleanText(report.executiveSummary()), MAX_EXECUTIVE_SUMMARY_SMS_CHARS);
        String message = "Weekly Chama Health: in KES %s, bal KES %s. Defaulters(%d): %s. Summary: %s"
                .formatted(
                        formatMoney(report.totalContributions()),
                        formatMoney(report.currentBalance()),
                        report.defaulters().size(),
                        defaulterText,
                        summary
                );

        return abbreviate(message, MAX_SMS_CHARS);
    }

    private String formatDefaulters(List<String> defaulters) {
        if (defaulters.isEmpty()) {
            return "None";
        }

        List<String> visibleNames = defaulters.stream()
                .limit(MAX_DEFAULTER_NAMES_IN_SMS)
                .toList();
        int remaining = defaulters.size() - visibleNames.size();
        String names = String.join(", ", visibleNames);
        if (remaining <= 0) {
            return names;
        }

        return names + " +" + remaining + " more";
    }

    private String memberName(ChamaMember member) {
        if (member.getUser() == null) {
            return "Unknown member";
        }

        String fullName = member.getUser().getFullName();
        if (fullName != null && !fullName.isBlank()) {
            return cleanText(fullName);
        }

        String phoneNumber = member.getUser().getPhoneNumber();
        return phoneNumber == null || phoneNumber.isBlank() ? "Unknown member" : phoneNumber.trim();
    }

    private String canonicalPhone(String phoneNumber) {
        if (phoneNumber == null) {
            return "";
        }

        String digits = phoneNumber.replaceAll("\\D", "");
        if (digits.startsWith("0") && digits.length() > 1) {
            return "254" + digits.substring(1);
        }
        return digits;
    }

    private BigDecimal money(BigDecimal value) {
        BigDecimal amount = value == null ? BigDecimal.ZERO : value;
        return amount.setScale(2, RoundingMode.HALF_UP);
    }

    private String formatMoney(BigDecimal value) {
        return money(value).toPlainString();
    }

    private String abbreviate(String value, int maxLength) {
        String cleaned = cleanText(value);
        if (cleaned.length() <= maxLength) {
            return cleaned;
        }
        return cleaned.substring(0, maxLength - 3).trim() + "...";
    }

    private String cleanText(String value) {
        return value == null ? "" : value.replaceAll("\\s+", " ").trim();
    }

    public record WeeklyChamaHealthReport(
            LocalDateTime periodStart,
            LocalDateTime periodEnd,
            BigDecimal totalContributions,
            BigDecimal currentBalance,
            List<String> defaulters,
            String executiveSummary
    ) {
    }
}