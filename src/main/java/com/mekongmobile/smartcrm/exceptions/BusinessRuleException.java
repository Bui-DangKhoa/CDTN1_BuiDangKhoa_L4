package com.mekongmobile.smartcrm.exceptions;

/**
 * Ngoại lệ vi phạm quy tắc nghiệp vụ điều phối hoặc xử lý ticket.
 * Tuân thủ quy tắc tách lớp (C2, S2): Ánh xạ sang HTTP 422 được thực hiện tại GlobalExceptionHandler.
 */
public class BusinessRuleException extends AppException {

    public BusinessRuleException(String message) {
        super("BUSINESS_RULE_VIOLATION", message);
    }

    public BusinessRuleException(String errorCode, String message) {
        super(errorCode, message);
    }
}
