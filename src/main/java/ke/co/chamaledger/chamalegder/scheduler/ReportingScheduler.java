package ke.co.chamaledger.chamalegder.scheduler;

import ke.co.chamaledger.chamalegder.service.ReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;

@Component
@RequiredArgsConstructor
@Slf4j
public class ReportingScheduler {

    private final ReportService reportService;
    private final AtomicBoolean running = new AtomicBoolean(false);

    @Scheduled(cron = "0 0 20 * * SUN", zone = "Africa/Nairobi")
    public void sendWeeklyChamaHealthReport() {
        if (!running.compareAndSet(false, true)) {
            log.warn("Skipping weekly Chama health report because a previous job is still running");
            return;
        }

        try {
            log.info("Starting weekly Chama health report job");
            reportService.sendWeeklyHealthReport();
        } finally {
            running.set(false);
            log.info("Finished weekly Chama health report job");
        }
    }
}
