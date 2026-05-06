package ke.co.chamaledger.chamalegder.config;

import ke.co.chamaledger.chamalegder.entity.Chama;
import ke.co.chamaledger.chamalegder.entity.ChamaMember;
import ke.co.chamaledger.chamalegder.entity.User;
import ke.co.chamaledger.chamalegder.repository.ChamaRepository;
import ke.co.chamaledger.chamalegder.repository.ChamaMemberRepository;
import ke.co.chamaledger.chamalegder.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private static final String SEED_CHAMA_REG = "CL-SEED-001";

    private static final String[][] SEED_USERS = {
            {"254711111111", "Admin@123",      "Admin Manager",    "CHAIRPERSON"},
            {"254722222222", "Treasurer@123",  "Finance Treasurer","TREASURER"},
            {"254700000000", "User@123",        "Regular Member",  "MEMBER"},
    };

    private final UserRepository userRepository;
    private final ChamaRepository chamaRepository;
    private final ChamaMemberRepository chamaMemberRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        log.info("[Seed] Provisioning/refreshing seed accounts…");

        User[] users = new User[SEED_USERS.length];
        for (int i = 0; i < SEED_USERS.length; i++) {
            String phone    = SEED_USERS[i][0];
            String password = SEED_USERS[i][1];
            String name     = SEED_USERS[i][2];
            users[i] = upsertUser(phone, name, password);
        }

        User manager   = users[0];
        User treasurer = users[1];
        User member    = users[2];

        Chama chama = chamaRepository.findAll().stream()
                .filter(c -> SEED_CHAMA_REG.equals(c.getRegistrationNumber()))
                .findFirst()
                .orElseGet(() -> chamaRepository.save(
                        Chama.builder()
                                .name("ChamaLedger Demo Group")
                                .description("Seed chama for development and RBAC testing")
                                .registrationNumber(SEED_CHAMA_REG)
                                .monthlyContribution(new BigDecimal("1000.00"))
                                .contributionDay(15)
                                .meetingFrequency("MONTHLY")
                                .loanInterestRate(new BigDecimal("10.00"))
                                .maxLoanMultiplier(new BigDecimal("3.0"))
                                .createdBy(manager)
                                .isActive(true)
                                .build()
                ));

        for (int i = 0; i < SEED_USERS.length; i++) {
            ensureMember(chama, users[i], SEED_USERS[i][3]);
        }

        log.info("[Seed] ✓ Seed accounts ready:");
        log.info("[Seed]   MANAGER   → 254711111111 / Admin@123");
        log.info("[Seed]   TREASURER → 254722222222 / Treasurer@123");
        log.info("[Seed]   MEMBER    → 254700000000 / User@123");
    }

    private User upsertUser(String phone, String fullName, String rawPassword) {
        return userRepository.findByPhoneNumber(phone)
                .map(existing -> {
                    existing.setPasswordHash(passwordEncoder.encode(rawPassword));
                    return userRepository.save(existing);
                })
                .orElseGet(() -> userRepository.save(
                        User.builder()
                                .fullName(fullName)
                                .phoneNumber(phone)
                                .passwordHash(passwordEncoder.encode(rawPassword))
                                .isActive(true)
                                .isPhoneVerified(true)
                                .build()
                ));
    }

    private void ensureMember(Chama chama, User user, String role) {
        chamaMemberRepository.findByChama_IdAndUser_Id(chama.getId(), user.getId())
                .ifPresentOrElse(
                        existing -> {
                            if (!role.equalsIgnoreCase(existing.getRole())) {
                                existing.setRole(role);
                                chamaMemberRepository.save(existing);
                            }
                        },
                        () -> chamaMemberRepository.save(
                                ChamaMember.builder()
                                        .chama(chama)
                                        .user(user)
                                        .role(role)
                                        .isActive(true)
                                        .build()
                        )
                );
    }
}
