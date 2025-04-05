package io.hhplus.tdd.point;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import org.springframework.http.converter.HttpMessageNotReadableException;

/**
 * 포인트 트랜잭션 종류
 * - CHARGE : 충전
 * - USE : 사용
 */
public enum TransactionType {
    CHARGE, USE;

    @JsonCreator
    public static TransactionType from(String value) {
        if (value == null) {
            throw new IllegalArgumentException("Transaction type cannot be null");
        }

        try {
            return TransactionType.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid transaction type: " + value + ". Valid types are: CHARGE, USE.");
        }
    }

    @JsonValue
    public String toJson() {
        return name().toLowerCase();
    }
}
