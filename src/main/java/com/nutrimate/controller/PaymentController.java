package com.nutrimate.controller;

import com.nutrimate.dto.CreatePaymentLinkRequestDTO;
import com.nutrimate.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.payos.exception.PayOSException;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/payment")
@Tag(name = "Payment", description = "API thanh toán PayOS")
@Slf4j
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @Operation(summary = "Tạo link thanh toán PayOS (Subscription/Plan)")
    @PostMapping("/create-payment-link")
    public ResponseEntity<Map<String, String>> createPaymentLink(
            @Valid @RequestBody CreatePaymentLinkRequestDTO request) throws PayOSException {
        String checkoutUrl = paymentService.createPaymentLink(request);
        return ResponseEntity.ok(Map.of("checkoutUrl", checkoutUrl));
    }

    @Operation(summary = "Tạo link thanh toán cho Booking")
    @PostMapping("/create-booking-link")
    public ResponseEntity<Map<String, String>> createBookingPaymentLink(
            @RequestBody Map<String, String> payload) throws PayOSException {
        String bookingId = payload.get("bookingId");
        String checkoutUrl = paymentService.createBookingPaymentLink(bookingId);
        return ResponseEntity.ok(Map.of("checkoutUrl", checkoutUrl));
    }

    @Operation(summary = "Webhook PayOS", description = "Nhận callback từ PayOS khi giao dịch hoàn tất")
    @PostMapping("/payos-webhook")
    public ResponseEntity<Map<String, ?>> payosWebhook(@RequestBody Map<String, Object> webhookBody) {
        try {
            paymentService.processPayOSWebhook(webhookBody);
        } catch (Exception e) {
            log.error("PayOS Webhook xác thực thất bại hoặc xử lý lỗi: ", e);
        }
        // BẮT BUỘC trả về đúng format để PayOS không spam gọi lại
        Map<String, Object> response = new HashMap<>();
        response.put("error", 0);
        response.put("message", "Ok");
        response.put("data", null);
        return ResponseEntity.ok(response);
    }
}
