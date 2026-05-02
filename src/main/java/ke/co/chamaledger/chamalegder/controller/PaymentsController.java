package ke.co.chamaledger.chamalegder.controller;

import ke.co.chamaledger.chamalegder.mpesa.service.MpesaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/payments")
@CrossOrigin(origins = "http://localhost:3000")
@RequiredArgsConstructor
@Slf4j
public class PaymentsController {

    private final MpesaService mpesaService;

    @PostMapping("/stk-push")
    public ResponseEntity<Map<String, Object>> initiateContribution(
            Principal principal,
            @RequestBody Map<String, String> body
    ) {
        String phone = principal.getName();
        String amount = body.getOrDefault("amount", "0");
        String reference = body.getOrDefault("reference", "CHAMA-CONTRIBUTION");

        log.info("STK Push initiated by {} for KES {}", phone, amount);
        String mpesaResponse = mpesaService.sendStkPush(phone, amount, reference);

        return ResponseEntity.ok(Map.of(
                "message", "STK Push sent. Check your phone for the M-Pesa prompt.",
                "phone", phone,
                "amount", amount,
                "mpesaResponse", mpesaResponse
        ));
    }
}
