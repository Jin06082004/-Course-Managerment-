package com._6.CourseManagerment.service;

import com._6.CourseManagerment.util.MoMoSecurityUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

/**
 * Service xử lý thanh toán qua MoMo Payment Gateway (API v2 - captureWallet).
 */
@Service
@Slf4j
public class MoMoPaymentService {

    @Value("${momo.partner-code:}")
    private String partnerCode;

    @Value("${momo.access-key:}")
    private String accessKey;

    @Value("${momo.secret-key:}")
    private String secretKey;

    @Value("${momo.endpoint:}")
    private String endpoint;

    @Value("${momo.return-url:}")
    private String returnUrl;

    @Value("${momo.notify-url:}")
    private String notifyUrl;

    @Value("${momo.simulate:false}")
    private boolean simulate;

    private final RestTemplate restTemplate;

    public MoMoPaymentService() {
        // Set connection and read timeout to 10s to avoid hanging on network issues
        org.springframework.http.client.SimpleClientHttpRequestFactory factory =
                new org.springframework.http.client.SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10_000);
        factory.setReadTimeout(10_000);
        this.restTemplate = new RestTemplate(factory);
    }

    /**
     * Tạo đơn thanh toán MoMo và trả về payUrl để redirect người dùng.
     *
     * @param orderId   mã đơn hàng duy nhất
     * @param amount    số tiền thanh toán (VND, dạng chuỗi)
     * @param orderInfo mô tả đơn hàng
     * @return payUrl - URL chuyển hướng người dùng đến trang thanh toán MoMo
     */
    public String createPayment(String orderId, long amount, String orderInfo) {
        // Simulation mode: skip real MoMo API, return a local confirm URL
        if (simulate) {
            log.info("[MoMo SIMULATE] Bỏ qua API thật, tạo URL giả lập: orderId={}, amount={}", orderId, amount);
            return "/api/payment/simulate-success?orderId=" + orderId;
        }

        if (!isConfigured()) {
            throw new RuntimeException("MoMo chưa được cấu hình. Vui lòng thiết lập momo.partner-code, momo.access-key, momo.secret-key, momo.endpoint, momo.return-url, momo.notify-url");
        }

        String requestId = UUID.randomUUID().toString();
        String requestType = "captureWallet";
        String extraData = ""; // Dữ liệu bổ sung (base64 encoded nếu cần)

        // Tạo rawData theo thứ tự alphabet đúng format MoMo API v2
        String rawData = "accessKey=" + accessKey
                + "&amount=" + amount
                + "&extraData=" + extraData
                + "&ipnUrl=" + notifyUrl
                + "&orderId=" + orderId
                + "&orderInfo=" + orderInfo
                + "&partnerCode=" + partnerCode
                + "&redirectUrl=" + returnUrl
                + "&requestId=" + requestId
                + "&requestType=" + requestType;

        // Tạo chữ ký HMAC-SHA256
        String signature = MoMoSecurityUtil.hmacSHA256(secretKey, rawData);
        log.info("MoMo rawData: {}", rawData);
        log.info("MoMo signature: {}", signature);

        // Xây dựng request body gửi lên MoMo
        Map<String, Object> requestBody = new LinkedHashMap<>();
        requestBody.put("partnerCode", partnerCode);
        requestBody.put("accessKey", accessKey);
        requestBody.put("requestId", requestId);
        requestBody.put("amount", amount);
        requestBody.put("orderId", orderId);
        requestBody.put("orderInfo", orderInfo);
        requestBody.put("redirectUrl", returnUrl);
        requestBody.put("ipnUrl", notifyUrl);
        requestBody.put("extraData", extraData);
        requestBody.put("requestType", requestType);
        requestBody.put("signature", signature);
        requestBody.put("lang", "vi");

        // Gửi POST request đến MoMo endpoint
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> httpEntity = new HttpEntity<>(requestBody, headers);

        try {
            log.info("Gửi request đến MoMo: endpoint={}, orderId={}, amount={}", endpoint, orderId, amount);
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    endpoint, HttpMethod.POST, httpEntity, new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {});

            Map<String, Object> responseBody = response.getBody();
            log.info("MoMo response: {}", responseBody);
            
            if (responseBody == null) {
                throw new RuntimeException("MoMo trả về response rỗng");
            }

            int resultCode = ((Number) responseBody.get("resultCode")).intValue();
            if (resultCode != 0) {
                String message = (String) responseBody.getOrDefault("message", "Unknown error");
                log.error("MoMo trả về lỗi: resultCode={}, message={}", resultCode, message);
                throw new RuntimeException("MoMo error (code=" + resultCode + "): " + message);
            }

            // Trích xuất payUrl để redirect người dùng
            String payUrl = (String) responseBody.get("payUrl");
            log.info("MoMo payUrl: {}", payUrl);
            return payUrl;

        } catch (org.springframework.web.client.HttpClientErrorException | org.springframework.web.client.HttpServerErrorException e) {
            log.error("MoMo HTTP error: status={}, body={}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("MoMo HTTP error: " + e.getResponseBodyAsString(), e);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.error("Lỗi khi gọi MoMo API: {}", e.getMessage(), e);
            throw new RuntimeException("Không thể kết nối đến MoMo", e);
        }
    }

    /**
     * Xác thực chữ ký từ MoMo IPN/Webhook callback.
     * Đảm bảo dữ liệu không bị giả mạo trước khi xử lý kết quả thanh toán.
     *
     * @param params Map chứa tất cả tham số MoMo gửi về qua IPN
     * @return true nếu chữ ký hợp lệ, false nếu không khớp
     */
    public boolean verifyIpnSignature(Map<String, String> params) {
        if (!isConfigured()) {
            log.warn("Bỏ qua verify IPN vì MoMo chưa được cấu hình");
            return false;
        }

        String receivedSignature = params.get("signature");
        if (receivedSignature == null || receivedSignature.isBlank()) {
            log.warn("IPN thiếu signature");
            return false;
        }

        // Tạo lại rawData từ các tham số MoMo gửi về (thứ tự alphabet)
        String rawData = "accessKey=" + accessKey
                + "&amount=" + params.getOrDefault("amount", "")
                + "&extraData=" + params.getOrDefault("extraData", "")
                + "&message=" + params.getOrDefault("message", "")
                + "&orderId=" + params.getOrDefault("orderId", "")
                + "&orderInfo=" + params.getOrDefault("orderInfo", "")
                + "&orderType=" + params.getOrDefault("orderType", "")
                + "&partnerCode=" + params.getOrDefault("partnerCode", "")
                + "&payType=" + params.getOrDefault("payType", "")
                + "&requestId=" + params.getOrDefault("requestId", "")
                + "&responseTime=" + params.getOrDefault("responseTime", "")
                + "&resultCode=" + params.getOrDefault("resultCode", "")
                + "&transId=" + params.getOrDefault("transId", "");

        String computedSignature = MoMoSecurityUtil.hmacSHA256(secretKey, rawData);
        boolean valid = computedSignature.equals(receivedSignature);

        if (!valid) {
            log.warn("MoMo IPN signature không khớp! orderId={}", params.get("orderId"));
        }
        return valid;
    }

    public boolean isConfigured() {
        return notBlank(partnerCode)
                && notBlank(accessKey)
                && notBlank(secretKey)
                && notBlank(endpoint)
                && notBlank(returnUrl)
                && notBlank(notifyUrl);
    }

    private boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }
}
