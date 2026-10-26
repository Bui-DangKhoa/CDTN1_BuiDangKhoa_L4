package com.mekongmobile.smartcrm.config;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.ThrowableProxyUtil;
import ch.qos.logback.core.LayoutBase;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * Logback Layout định dạng JSON chuẩn ISO 8601, tích hợp requestId từ MDC
 * và tự động che dữ liệu nhạy cảm (SĐT QT-15, mật khẩu, token).
 */
public class JsonMaskingLayout extends LayoutBase<ILoggingEvent> {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_OFFSET_DATE_TIME.withZone(ZoneId.systemDefault());

    @Override
    public String doLayout(ILoggingEvent event) {
        try {
            ObjectNode root = OBJECT_MAPPER.createObjectNode();

            // Thời điểm ISO 8601
            root.put("timestamp", ISO_FORMATTER.format(Instant.ofEpochMilli(event.getTimeStamp())));

            // Mức log (INFO, WARN, ERROR, DEBUG)
            root.put("level", event.getLevel().toString());

            // Ngữ cảnh & RequestId từ MDC
            Map<String, String> mdc = event.getMDCPropertyMap();
            String requestId = (mdc != null && mdc.containsKey(RequestIdFilter.MDC_REQUEST_ID_KEY))
                    ? mdc.get(RequestIdFilter.MDC_REQUEST_ID_KEY)
                    : "";
            root.put("requestId", requestId);

            root.put("thread", event.getThreadName());
            root.put("logger", event.getLoggerName());

            // Ngữ cảnh ứng dụng
            String contextName = (event.getLoggerContextVO() != null)
                    ? event.getLoggerContextVO().getName()
                    : "smart-crm-technician-scheduling";
            root.put("context", contextName);

            // Nội dung thông điệp đã che SĐT (QT-15) và mật khẩu/token
            String rawMessage = event.getFormattedMessage();
            String maskedMessage = MaskingUtil.maskSensitiveData(rawMessage);
            root.put("message", maskedMessage);

            // Stack trace nếu có ngoại lệ
            if (event.getThrowableProxy() != null) {
                String stackTrace = ThrowableProxyUtil.asString(event.getThrowableProxy());
                root.put("stackTrace", MaskingUtil.maskSensitiveData(stackTrace));
            }

            return OBJECT_MAPPER.writeValueAsString(root) + "\n";
        } catch (Exception e) {
            // Fallback phòng ngừa lỗi serialize
            return "{\"level\":\"" + event.getLevel() + "\",\"message\":\"" + event.getFormattedMessage() + "\"}\n";
        }
    }
}
