package io.hhplus.tdd.point;

import io.hhplus.tdd.exception.InvalidAmountException;

public record PointHistory(
        long id,
        long userId,
        long amount,
        TransactionType type,
        long updateMillis
) {
    public PointHistory {
        if (amount <= 0) {
            throw new InvalidAmountException("Amount must be greater than 0.");
        }
        if (type == null) {
            throw new IllegalArgumentException("TransactionType must not be null.");
        }
    }
}