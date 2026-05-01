package ke.co.chamaledger.chamalegder.mpesa.service;

import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;

public class MpesaUtils {
    public static String getTimestamp() {
        return new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
    }

    public static String getPassword(String shortCode, String passKey, String timestamp) {
        String str = shortCode + passKey + timestamp;
        return Base64.getEncoder().encodeToString(str.getBytes());
    }
}