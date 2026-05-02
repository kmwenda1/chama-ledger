package ke.co.chamaledger.chamalegder.controller;

import ke.co.chamaledger.chamalegder.dto.TransactionHistoryDTO;
import ke.co.chamaledger.chamalegder.mpesa.service.LedgerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/ledger")
@RequiredArgsConstructor
public class LedgerController {

    private final LedgerService ledgerService;

    @GetMapping("/my-history")
    public ResponseEntity<List<TransactionHistoryDTO>> getMyHistory(Authentication authentication) {
        String phoneNumber = authentication.getName();
        return ResponseEntity.ok(ledgerService.getMemberHistory(phoneNumber));
    }
}
