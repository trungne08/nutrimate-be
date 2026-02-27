package com.nutrimate.service;

import com.nutrimate.dto.CreatePaymentLinkRequestDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import vn.payos.PayOS;
import vn.payos.exception.PayOSException;
import vn.payos.model.v2.paymentRequests.CreatePaymentLinkRequest;
import vn.payos.model.v2.paymentRequests.CreatePaymentLinkResponse;
import vn.payos.model.webhooks.WebhookData;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private static final String RETURN_URL = "http://localhost:5173";
    private static final String CANCEL_URL = "http://localhost:5173";

    private final PayOS payOS;

    /**
     * Tạo orderCode duy nhất (timestamp ms + nano suffix).
     */
    private long generateOrderCode() {
        return System.currentTimeMillis() * 1000 + (System.nanoTime() % 1000);
    }

    private String truncate(String s, int maxLen) {
        if (s == null) return "";
        return s.length() <= maxLen ? s : s.substring(0, maxLen);
    }

    /** PayOS giới hạn description tối đa 25 ký tự. */
    private static final int MAX_DESCRIPTION_LENGTH = 25;

    public String createPaymentLink(CreatePaymentLinkRequestDTO request) throws PayOSException {
        long orderCode = generateOrderCode();
        String desc = "NUTRI " + truncate(request.getUserId(), 8) + " " + truncate(request.getPlanId(), 8);
        String description = desc.length() <= MAX_DESCRIPTION_LENGTH ? desc : desc.substring(0, MAX_DESCRIPTION_LENGTH);

        CreatePaymentLinkRequest paymentData = CreatePaymentLinkRequest.builder()
                .orderCode(orderCode)
                .amount(request.getAmount().longValue())
                .description(description)
                .returnUrl(RETURN_URL)
                .cancelUrl(CANCEL_URL)
                .build();

        CreatePaymentLinkResponse response = payOS.paymentRequests().create(paymentData);
        return response.getCheckoutUrl();
    }

    public void processPayOSWebhook(Object webhookBody) {
        WebhookData data = payOS.webhooks().verify(webhookBody);
        if ("00".equals(data.getCode())) {
            log.info("Đã nhận được tiền thành công cho đơn hàng: {}", data.getOrderCode());
        }
    }
}
