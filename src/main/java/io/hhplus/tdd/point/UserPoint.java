package io.hhplus.tdd.point;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.hhplus.tdd.exception.InsufficientBalanceException;
import io.hhplus.tdd.exception.InvalidAmountException;
import io.hhplus.tdd.exception.MaxPointExceededException;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record UserPoint(
        long id,
        long point,
        long updateMillis
) {
    public static final long MAX_TOTAL_POINT = 10_000_000L;

    public static UserPoint empty(long id) {
        return new UserPoint(id, 0, System.currentTimeMillis());
    }

    public UserPoint charge(long amount) {
        validateAmount(amount);
        long newTotal = this.point + amount;
        if (newTotal > MAX_TOTAL_POINT) {
            throw new MaxPointExceededException("Point exceed max limit.");
        }

        return new UserPoint(this.id, newTotal, System.currentTimeMillis());
    }

    public UserPoint use(long amount) {
        validateAmount(amount);
        if (this.point < amount) {
            throw new InsufficientBalanceException("Insufficient balance.");
        }

        return new UserPoint(this.id, this.point - amount, System.currentTimeMillis());
    }

    private void validateAmount(long amount) {
        if (amount <= 0) {
            throw new InvalidAmountException("Amount must be > 0.");
        }
    }
}