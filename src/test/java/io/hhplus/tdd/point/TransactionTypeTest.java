package io.hhplus.tdd.point;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TransactionTypeTest {

    @Nested
    class TransactionTypeEnum_잘못된_타입_검증 {

        @Test
        void 잘못된_타입이_들어왔을_때_예외가_발생한다() {
            assertThatThrownBy(() ->
                    TransactionType.from("INVALID_TYPE")
            ).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Invalid transaction type");
        }

        @Test
        void 타입이_null이면_예외가_발생한다() {
            assertThatThrownBy(() ->
                    TransactionType.from(null)
            ).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Transaction type cannot be null");
        }
    }

    @Nested
    class TransactionTypeEnum_유효한_타입_검증 {

        @Test
        void 유효한_값이_들어왔을_때_정상적으로_TransactionType이_생성된다() {
            TransactionType charge = TransactionType.from("CHARGE");
            assertThat(charge).isEqualTo(TransactionType.CHARGE);

            TransactionType use = TransactionType.from("USE");
            assertThat(use).isEqualTo(TransactionType.USE);
        }
    }
}
