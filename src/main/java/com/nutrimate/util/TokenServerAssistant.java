package com.nutrimate.util;

import com.fasterxml.jackson.databind.ObjectMapper;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * Zego Token04 generator - port from zego_server_assistant.
 * Dùng để tạo token cho Video Call (Zego Cloud).
 */
public final class TokenServerAssistant {

    private static final String VERSION_FLAG = "04";
    private static final int IV_LENGTH = 16;
    private static final String TRANSFORMATION = "AES/CBC/PKCS5Padding";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public static final String PrivilegeKeyLogin = "1";
    public static final String PrivilegeKeyPublish = "2";
    public static final int PrivilegeEnable = 1;
    public static final int PrivilegeDisable = 0;

    public enum ErrorCode {
        SUCCESS(0),
        ILLEGAL_APP_ID(1),
        ILLEGAL_USER_ID(3),
        ILLEGAL_SECRET(5),
        ILLEGAL_EFFECTIVE_TIME(6),
        OTHER(-1);

        public final int value;

        ErrorCode(int value) {
            this.value = value;
        }
    }

    public static class ErrorInfo {
        public ErrorCode code = ErrorCode.SUCCESS;
        public String message = "";
    }

    public static class TokenInfo {
        public String data = "";
        public ErrorInfo error = new ErrorInfo();
    }

    private TokenServerAssistant() {}

    /**
     * Generate Token04 for Zego Video Call.
     *
     * @param appId                  Zego App ID
     * @param userId                 User ID (unique per app)
     * @param secret                Server Secret (32 chars)
     * @param effectiveTimeInSeconds Token validity in seconds (e.g. 3600 = 1 hour)
     * @param payload               Custom payload (empty string "" for basic auth)
     * @return TokenInfo with token.data or error
     */
    public static TokenInfo generateToken04(long appId, String userId, String secret,
                                            int effectiveTimeInSeconds, String payload) {
        TokenInfo token = new TokenInfo();

        if (appId == 0) {
            token.error.code = ErrorCode.ILLEGAL_APP_ID;
            token.error.message = "illegal appId";
            return token;
        }
        if (userId == null || userId.isEmpty() || userId.length() > 64) {
            token.error.code = ErrorCode.ILLEGAL_USER_ID;
            token.error.message = "illegal userId";
            return token;
        }
        if (secret == null || secret.isEmpty() || secret.length() != 32) {
            token.error.code = ErrorCode.ILLEGAL_SECRET;
            token.error.message = "illegal secret (must be 32 characters)";
            return token;
        }
        if (effectiveTimeInSeconds <= 0) {
            token.error.code = ErrorCode.ILLEGAL_EFFECTIVE_TIME;
            token.error.message = "effectiveTimeInSeconds must > 0";
            return token;
        }

        byte[] ivBytes = new byte[IV_LENGTH];
        new SecureRandom().nextBytes(ivBytes);

        long nowTime = System.currentTimeMillis() / 1000;
        long expireTime = nowTime + effectiveTimeInSeconds;
        int nonce = new Random().nextInt();

        Map<String, Object> json = new HashMap<>();
        json.put("app_id", appId);
        json.put("user_id", userId);
        json.put("ctime", nowTime);
        json.put("expire", expireTime);
        json.put("nonce", nonce);
        json.put("payload", payload != null ? payload : "");

        try {
            String content = OBJECT_MAPPER.writeValueAsString(json);
            byte[] contentBytes = encrypt(content.getBytes(java.nio.charset.StandardCharsets.UTF_8),
                    secret.getBytes(java.nio.charset.StandardCharsets.UTF_8), ivBytes);

            ByteBuffer buffer = ByteBuffer.allocate(contentBytes.length + IV_LENGTH + 12);
            buffer.order(ByteOrder.BIG_ENDIAN);
            buffer.putLong(expireTime);
            packBytes(ivBytes, buffer);
            packBytes(contentBytes, buffer);

            token.data = VERSION_FLAG + Base64.getEncoder().encodeToString(buffer.array());
            token.error.code = ErrorCode.SUCCESS;
        } catch (Exception e) {
            token.error.code = ErrorCode.OTHER;
            token.error.message = e.getMessage();
        }

        return token;
    }

    private static byte[] encrypt(byte[] content, byte[] secretKey, byte[] ivBytes) throws Exception {
        if (secretKey == null || secretKey.length != 32) {
            throw new IllegalArgumentException("secret key's length must be 32 bytes");
        }
        if (ivBytes == null || ivBytes.length != 16) {
            throw new IllegalArgumentException("ivBytes's length must be 16 bytes");
        }
        if (content == null) {
            content = new byte[0];
        }

        SecretKeySpec key = new SecretKeySpec(secretKey, "AES");
        IvParameterSpec iv = new IvParameterSpec(ivBytes);
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        cipher.init(Cipher.ENCRYPT_MODE, key, iv);
        return cipher.doFinal(content);
    }

    private static void packBytes(byte[] data, ByteBuffer target) {
        target.putShort((short) data.length);
        target.put(data);
    }
}
