package cc.magickiat.crypto.botnaja.service;

import org.apache.commons.codec.digest.HmacAlgorithms;
import org.apache.commons.codec.digest.HmacUtils;

public class HmacService {
    private static final String BITKUB_API_SECRET = System.getenv("BITKUB_API_SECRET");
    private static final HmacUtils hmacUtils = new HmacUtils(HmacAlgorithms.HMAC_SHA_256, BITKUB_API_SECRET);

    public HmacService() {
    }

    public static String calculateHmac(String data) {
        return hmacUtils.hmacHex(data);
    }
}