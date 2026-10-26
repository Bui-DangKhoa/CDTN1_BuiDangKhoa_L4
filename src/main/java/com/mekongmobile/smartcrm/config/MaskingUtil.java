package com.mekongmobile.smartcrm.config;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Tiện ích che dữ liệu nhạy cảm (QT-15: Che SĐT khách hàng 090****567, không log mật khẩu/token).
 */
public final class MaskingUtil {

    private MaskingUtil() {}

    // Regex tìm số điện thoại Việt Nam 10-11 số (090xxxxxxx -> 090****xxx)
    private static final Pattern PHONE_PATTERN = Pattern.compile("(?<!\\d)(0\\d{2})(\\d{4})(\\d{3,4})(?!\\d)");

    // Regex che mật khẩu / token / secret trong chuỗi log
    private static final Pattern SENSITIVE_PATTERN = Pattern.compile("(?i)(password|passwd|matkhau|mat_khau|token|secret|jwt)\\s*([:=])\\s*([\"']?)([^\"'\\s,;}]+)([\"']?)");

    /**
     * Che 4 số giữa SĐT khách hàng (dạng 090****567) theo quy tắc QT-15.
     */
    public static String maskPhoneNumber(String text) {
        if (text == null || text.isBlank()) {
            return text;
        }
        Matcher matcher = PHONE_PATTERN.matcher(text);
        return matcher.replaceAll("$1****$3");
    }

    /**
     * Che toàn bộ dữ liệu nhạy cảm gồm SĐT (QT-15) và mật khẩu / token / secret.
     */
    public static String maskSensitiveData(String text) {
        if (text == null || text.isBlank()) {
            return text;
        }
        String masked = maskPhoneNumber(text);
        Matcher sensitiveMatcher = SENSITIVE_PATTERN.matcher(masked);
        return sensitiveMatcher.replaceAll("$1$2$3******$5");
    }
}
