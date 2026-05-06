package ke.co.chamaledger.chamalegder.mpesa.controller;

import ke.co.chamaledger.chamalegder.mpesa.service.MpesaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/mpesa")
@RequiredArgsConstructor
@Slf4j
public class MpesaController {

    private final MpesaService mpesaService;

    @PostMapping("/stk-push")
    public ResponseEntity<String> initiatePayment(
            @RequestParam String phoneNumber,
            @RequestParam String amount,
            @RequestParam String accountReference
    ) {
        log.info("Received STK Push request for {} amount {}", phoneNumber, amount);
        String response = mpesaService.sendStkPush(phoneNumber, amount, accountReference);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/callback")
    public ResponseEntity<Map<String, Object>> receiveMpesaCallback(@RequestBody String jsonResponse) {
        System.out.println("[CALLBACK DEBUG] M-Pesa callback endpoint was hit.");

        mpesaService.processCallback(jsonResponse);

        return ResponseEntity.ok(Map.of(
                "ResultCode", 0,
                "ResultDesc", "Success"
        ));
    }
}
