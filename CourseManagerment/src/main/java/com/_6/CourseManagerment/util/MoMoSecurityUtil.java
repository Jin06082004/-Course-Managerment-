package com._6.CourseManagerment.util;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

/**
 * Utility class để tạo chữ ký HMAC-SHA256 cho MoMo Payment API v2.
 */
public class MoMoSecurityUtil {

    private static final String HMAC_SHA256 = "HmacSHA256";

    private MoMoSecurityUtil() {
    }

    /**
     * Tạo chữ ký HMAC-SHA256 từ secretKey và chuỗi dữ liệu đầu vào.
     *
     * @param secretKey khóa bí mật do MoMo cung cấp
     * @param data      chuỗi rawData cần ký
     * @return chuỗi signature dạng hex lowercase
     */
    public static String hmacSHA256(String secretKey, String data) {
        try {
            Mac hmac = Mac.getInstance(HMAC_SHA256);
            SecretKeySpec secretKeySpec = new SecretKeySpec(
                    secretKey.getBytes(StandardCharsets.UTF_8), HMAC_SHA256);
            hmac.init(secretKeySpec);
            byte[] hash = hmac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi tạo HMAC-SHA256 signature", e);
        }
    }
}
