package ke.co.chamaledger.chamalegder.scheduler;

import ke.co.chamaledger.chamalegder.mpesa.service.MpesaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;

@Component
@RequiredArgsConstructor
@Slf4j
public class ReconciliationScheduler {

    private final MpesaService mpesaService;
    private final AtomicBoolean running = new AtomicBoolean(false);

    @Scheduled(initialDelay = 600_000, fixedDelay = 600_000)
    public void runReconciliation() {
        if (!running.compareAndSet(false, true)) {
            log.warn("Skipping M-Pesa reconciliation because a previous job is still running");
            return;
        }

        try {
            log.info("Starting scheduled M-Pesa reconciliation job");
            mpesaService.reconcileStaleTransactions();
        } finally {
            running.set(false);
            log.info("Finished scheduled M-Pesa reconciliation job");
        }
    }
}
