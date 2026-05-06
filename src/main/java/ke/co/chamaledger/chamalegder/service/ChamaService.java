package ke.co.chamaledger.chamalegder.service;

import ke.co.chamaledger.chamalegder.dto.ChamaRequest;
import ke.co.chamaledger.chamalegder.entity.Chama;
import ke.co.chamaledger.chamalegder.entity.ChamaMember;
import ke.co.chamaledger.chamalegder.entity.User;
import ke.co.chamaledger.chamalegder.repository.ChamaMemberRepository;
import ke.co.chamaledger.chamalegder.repository.ChamaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ChamaService {

    private final ChamaRepository chamaRepository;
    private final ChamaMemberRepository chamaMemberRepository;

    @Transactional
    public Chama createChama(ChamaRequest request, User currentUser) {
        Chama chama = Chama.builder()
                .name(request.getName())
                .description(request.getDescription())
                .registrationNumber(request.getRegistrationNumber())
                .monthlyContribution(request.getMonthlyContribution())
                .contributionDay(request.getContributionDay())
                .loanInterestRate(request.getLoanInterestRate())
                .maxLoanMultiplier(request.getMaxLoanMultiplier())
                .createdBy(currentUser)
                .isActive(true)
                .meetingFrequency("MONTHLY")
                .build();

        Chama savedChama = chamaRepository.save(chama);

        ChamaMember member = ChamaMember.builder()
                .chama(savedChama)
                .user(currentUser)
                .role("TREASURER")
                .isActive(true)
                .build();

        chamaMemberRepository.save(member);
        return savedChama;
    }
}