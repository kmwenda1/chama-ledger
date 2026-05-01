package ke.co.chamaledger.chamalegder.mpesa.controller;

import ke.co.chamaledger.chamalegder.mpesa.service.MpesaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/mpesa")
@RequiredArgsConstructor
@Slf4j
public class MpesaController {

    private final MpesaService mpesaService;

    /**
     * Endpoint to trigger the STK Push PIN prompt on a user's phone.
     */
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

    /**
     * Webhook called by Safaricom Daraja API after the user enters their PIN.
     */
    @PostMapping("/callback")
    public ResponseEntity<String> receiveMpesaCallback(@RequestBody String jsonResponse) {
        log.info("Incoming M-Pesa Callback...");

        // Process and save to database
        mpesaService.processCallback(jsonResponse);

        // Return 200 OK so Safaricom knows we got the message
        return ResponseEntity.ok("Success");
    }
}