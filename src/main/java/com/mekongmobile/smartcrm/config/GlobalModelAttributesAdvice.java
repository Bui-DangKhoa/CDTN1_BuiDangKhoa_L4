package com.mekongmobile.smartcrm.config;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Controller Advice cung cấp các thuộc tính toàn cục cho giao diện Thymeleaf (UI States).
 * Luôn đảm bảo requestId (UUID) có mặt trong mọi trang HTML phục vụ Error State & tra cứu.
 */
@ControllerAdvice
public class GlobalModelAttributesAdvice {

    @ModelAttribute("requestId")
    public String populateRequestId() {
        return RequestIdFilter.getCurrentRequestId();
    }
}
