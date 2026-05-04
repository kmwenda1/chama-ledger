package ke.co.chamaledger.chamalegder.mpesa.service;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Date;

public class MpesaUtils {
    private static final DateTimeFormatter MPESA_TS = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    public static String getTimestamp() {
        return new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
    }

    public static String getPassword(String shortCode, String passKey, String timestamp) {
        String str = shortCode + passKey + timestamp;
        return Base64.getEncoder().encodeToString(str.getBytes());
    }

    public static LocalDateTime parseMpesaTimestamp(String timestamp) {
        if (timestamp == null || timestamp.isBlank()) {
            return null;
        }
        return LocalDateTime.parse(timestamp, MPESA_TS);
    }
}
