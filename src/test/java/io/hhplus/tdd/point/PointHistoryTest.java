package io.hhplus.tdd.point;

import io.hhplus.tdd.exception.InvalidAmountException;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PointHistoryTest {

    private static final long HISTORY_ID = 1L;
    private static final long USER_ID = 1L;
    private static final long NOW = System.currentTimeMillis();

    @Nested
    class 포인트_충전 {

        @Test
        void 충전_내역을_생성할_수_있다() {
            PointHistory history = new PointHistory(HISTORY_ID, USER_ID, 1000L, TransactionType.CHARGE, NOW);

            assertThat(history).isNotNull();
            assertThat(history.userId()).isEqualTo(USER_ID);
            assertThat(history.amount()).isEqualTo(1000L);
            assertThat(history.type()).isEqualTo(TransactionType.CHARGE);
            assertThat(history.updateMillis()).isEqualTo(NOW);
        }

        @Test
        void 사용_내역을_생성할_수_있다() {
            PointHistory history = new PointHistory(HISTORY_ID, USER_ID, 500L, TransactionType.USE, NOW);

            assertThat(history.userId()).isEqualTo(USER_ID);
            assertThat(history.amount()).isEqualTo(500L);
            assertThat(history.type()).isEqualTo(TransactionType.USE);
            assertThat(history.updateMillis()).isEqualTo(NOW);
        }
    }

    @Nested
    class 금액_유효성_검사 {

        @Test
        void 금액이_0원이면_InvalidAmountException_예외가_발생한다() {
            assertThatThrownBy(() ->
                    new PointHistory(HISTORY_ID, USER_ID, 0L, TransactionType.CHARGE, NOW))
                    .isInstanceOf(InvalidAmountException.class);
        }

        @Test
        void 금액이_음수이면_InvalidAmountException_예외가_발생한다() {
            assertThatThrownBy(() ->
                    new PointHistory(HISTORY_ID, USER_ID, -100L, TransactionType.CHARGE, NOW))
                    .isInstanceOf(InvalidAmountException.class);
        }

        @Test
        void 금액이_1원이면_정상적으로_내역이_생성된다() {
            PointHistory history = new PointHistory(HISTORY_ID, USER_ID, 1L, TransactionType.CHARGE, NOW);
            assertThat(history.amount()).isEqualTo(1L);
        }
    }

    @Nested
    class 타입_유효성_검사 {

        @Test
        void 타입이_null이면_IllegalArgumentException_예외가_발생한다() {
            assertThatThrownBy(() ->
                    new PointHistory(HISTORY_ID, USER_ID, 1000L, null, NOW))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void 타입이_CHARGE이면_정상적으로_설정된다() {
            PointHistory history = new PointHistory(HISTORY_ID, USER_ID, 1000L, TransactionType.CHARGE, NOW);
            assertThat(history.type()).isEqualTo(TransactionType.CHARGE);
        }

        @Test
        void 타입이_USE이면_정상적으로_설정된다() {
            PointHistory history = new PointHistory(HISTORY_ID, USER_ID, 1000L, TransactionType.USE, NOW);
            assertThat(history.type()).isEqualTo(TransactionType.USE);
        }
    }
}
