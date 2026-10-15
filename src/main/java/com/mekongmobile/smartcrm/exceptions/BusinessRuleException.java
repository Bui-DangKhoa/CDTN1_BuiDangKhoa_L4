package com.mekongmobile.smartcrm.exceptions;

import org.springframework.http.HttpStatus;

/**
 * Ngoại lệ vi phạm quy tắc nghiệp vụ điều phối hoặc xử lý ticket (422 Unprocessable Entity).
 */
public class BusinessRuleException extends AppException {

    public BusinessRuleException(String message) {
        super(message, HttpStatus.UNPROCESSABLE_ENTITY, "BUSINESS_RULE_VIOLATION");
    }

    public BusinessRuleException(String message, String errorCode) {
        super(message, HttpStatus.UNPROCESSABLE_ENTITY, errorCode);
    }
}
