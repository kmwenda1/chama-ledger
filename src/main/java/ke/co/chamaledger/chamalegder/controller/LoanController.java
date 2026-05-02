package ke.co.chamaledger.chamalegder.controller;

import jakarta.validation.Valid;
import ke.co.chamaledger.chamalegder.dto.LoanApplicationRequest;
import ke.co.chamaledger.chamalegder.dto.LoanDetailResponse;
import ke.co.chamaledger.chamalegder.service.LoanService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/loans")
@RequiredArgsConstructor
public class LoanController {

    private final LoanService loanService;

    @PostMapping
    public ResponseEntity<LoanDetailResponse> applyForLoan(
            @Valid @RequestBody LoanApplicationRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(loanService.applyForLoan(authentication.getName(), request));
    }

    @GetMapping("/my")
    public ResponseEntity<List<LoanDetailResponse>> getMyLoans(Authentication authentication) {
        return ResponseEntity.ok(loanService.getMyLoans(authentication.getName()));
    }
}
