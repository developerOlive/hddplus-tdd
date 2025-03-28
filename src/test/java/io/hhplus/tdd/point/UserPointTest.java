package io.hhplus.tdd.point;

import io.hhplus.tdd.exception.InsufficientBalanceException;
import io.hhplus.tdd.exception.InvalidAmountException;
import io.hhplus.tdd.exception.MaxPointExceededException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserPointTest {

    private static final long USER_ID = 1L;
    private static final long ANY_TIME = 1L;

    private UserPoint userPoint;

    @BeforeEach
    void 사용자_기본_포인트_설정() {
        userPoint = new UserPoint(USER_ID, 1000L, ANY_TIME);
    }

    @Nested
    class 포인트_충전 {

        @Test
        void 잔액이_정상적으로_증가한다() {
            UserPoint result = userPoint.charge(500L);
            assertThat(result.point()).isEqualTo(1500L);
        }

        @Test
        void 금액이_0원이면_InvalidAmountException이_발생한다() {
            assertThatThrownBy(() -> userPoint.charge(0L))
                    .isInstanceOf(InvalidAmountException.class);
        }

        @Test
        void 금액이_음수면_InvalidAmountException이_발생한다() {
            assertThatThrownBy(() -> userPoint.charge(-100L))
                    .isInstanceOf(InvalidAmountException.class);
        }

        @Test
        void 금액이_1원이면_정상적으로_포인트가_증가한다() {
            UserPoint result = userPoint.charge(1L);
            assertThat(result.point()).isEqualTo(1001L);
        }

        @Test
        void 총포인트가_최대한도를_초과하면_MaxPointExceededException이_발생한다() {
            userPoint = new UserPoint(USER_ID, 9_999_999L, ANY_TIME);
            assertThatThrownBy(() -> userPoint.charge(2L))
                    .isInstanceOf(MaxPointExceededException.class);
        }
    }

    @Nested
    class 포인트_사용 {

        @Test
        void 보유포인트_이내면_정상적으로_차감된다() {
            UserPoint result = userPoint.use(300L);
            assertThat(result.point()).isEqualTo(700L);
        }

        @Test
        void 금액이_보유포인트_초과하면_InsufficientBalanceException이_발생한다() {
            assertThatThrownBy(() -> userPoint.use(1500L))
                    .isInstanceOf(InsufficientBalanceException.class);
        }

        @Test
        void 금액이_0원이면_InvalidAmountException이_발생한다() {
            assertThatThrownBy(() -> userPoint.use(0L))
                    .isInstanceOf(InvalidAmountException.class);
        }

        @Test
        void 금액이_음수면_InvalidAmountException이_발생한다() {
            assertThatThrownBy(() -> userPoint.use(-1L))
                    .isInstanceOf(InvalidAmountException.class);
        }

        @Test
        void 금액이_1원이면_정상적으로_차감된다() {
            UserPoint result = userPoint.use(1L);
            assertThat(result.point()).isEqualTo(999L);
        }
    }
}
