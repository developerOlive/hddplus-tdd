package io.hhplus.tdd.point;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import io.hhplus.tdd.exception.InsufficientBalanceException;
import io.hhplus.tdd.exception.InvalidAmountException;
import io.hhplus.tdd.exception.MaxPointExceededException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class PointServiceTest {

    private UserPointTable userPointTable;
    private PointHistoryTable pointHistoryTable;
    private PointService pointService;

    private final long userId = 1L;
    private final long now = System.currentTimeMillis();

    @BeforeEach
    void setUp() {
        userPointTable = mock(UserPointTable.class);
        pointHistoryTable = mock(PointHistoryTable.class);
        pointService = new PointService(userPointTable, pointHistoryTable);
    }

    @Nested
    class 포인트_충전 {

        @Test
        void 정상적으로_포인트를_충전하면_저장소에_저장되고_히스토리가_남는다() {
            // given
            long amount = 500L;
            UserPoint before = new UserPoint(userId, 1000L, now);
            UserPoint after = before.charge(amount);
            when(userPointTable.selectById(userId)).thenReturn(before);
            when(userPointTable.insertOrUpdate(userId, 1500L)).thenReturn(after);

            // when
            UserPoint result = pointService.charge(userId, amount);

            // then
            assertThat(result.point()).isEqualTo(1500L);
            assertThat(result.id()).isEqualTo(userId);
            verify(userPointTable).insertOrUpdate(userId, 1500L);
            verify(pointHistoryTable).insert(
                    eq(userId),
                    eq(amount),
                    eq(TransactionType.CHARGE),
                    anyLong()
            );
        }

        @Test
        void 충전_금액이_음수면_InvalidAmountException이_발생한다() {
            // given
            UserPoint user = new UserPoint(userId, 1000L, now);
            when(userPointTable.selectById(userId)).thenReturn(user);

            // when & then
            assertThatThrownBy(() -> pointService.charge(userId, -500L))
                    .isInstanceOf(InvalidAmountException.class);
        }

        @Test
        void 충전_금액이_0원이면_InvalidAmountException이_발생한다() {
            // given
            UserPoint user = new UserPoint(userId, 1000L, now);
            when(userPointTable.selectById(userId)).thenReturn(user);

            // when & then
            assertThatThrownBy(() -> pointService.charge(userId, 0L))
                    .isInstanceOf(InvalidAmountException.class);
        }

        @Test
        void 충전_후_최대_포인트를_초과하면_MaxPointExceededException이_발생한다() {
            // given
            UserPoint user = new UserPoint(userId, 9_999_999L, now);
            when(userPointTable.selectById(userId)).thenReturn(user);

            // when & then
            assertThatThrownBy(() -> pointService.charge(userId, 2L))
                    .isInstanceOf(MaxPointExceededException.class);
        }
    }

    @Nested
    class 포인트_사용 {

        @Test
        void 정상적으로_포인트를_사용하면_차감되고_히스토리가_남는다() {
            // given
            long amount = 300L;
            UserPoint before = new UserPoint(userId, 1000L, now);
            UserPoint after = before.use(amount);
            when(userPointTable.selectById(userId)).thenReturn(before);
            when(userPointTable.insertOrUpdate(userId, 700L)).thenReturn(after);

            // when
            UserPoint result = pointService.use(userId, amount);

            // then
            assertThat(result.point()).isEqualTo(700L);
            assertThat(result.id()).isEqualTo(userId);
            verify(userPointTable).insertOrUpdate(userId, 700L);
            verify(pointHistoryTable).insert(
                    eq(userId),
                    eq(amount),
                    eq(TransactionType.USE),
                    anyLong()
            );
        }

        @Test
        void 사용_금액이_보유포인트보다_많으면_InsufficientBalanceException이_발생한다() {
            UserPoint user = new UserPoint(userId, 500L, now);
            when(userPointTable.selectById(userId)).thenReturn(user);

            assertThatThrownBy(() -> pointService.use(userId, 1000L))
                    .isInstanceOf(InsufficientBalanceException.class);
        }

        @Test
        void 사용_금액이_0원이면_InvalidAmountException이_발생한다() {
            UserPoint user = new UserPoint(userId, 1000L, now);
            when(userPointTable.selectById(userId)).thenReturn(user);

            assertThatThrownBy(() -> pointService.use(userId, 0L))
                    .isInstanceOf(InvalidAmountException.class);
        }

        @Test
        void 사용_금액이_음수면_InvalidAmountException이_발생한다() {
            UserPoint user = new UserPoint(userId, 1000L, now);
            when(userPointTable.selectById(userId)).thenReturn(user);

            assertThatThrownBy(() -> pointService.use(userId, -100L))
                    .isInstanceOf(InvalidAmountException.class);
        }
    }

    @Nested
    class 포인트_조회 {

        @Test
        void 유저_포인트가_존재하면_정상적으로_조회된다() {
            // given
            UserPoint expected = new UserPoint(userId, 5000L, now);
            when(userPointTable.selectById(userId)).thenReturn(expected);

            // when
            UserPoint result = pointService.getUserPoint(userId);

            // then
            assertThat(result).isEqualTo(expected);
        }

        @Test
        void 유저_포인트가_없으면_기본값으로_조회된다() {
            // given
            UserPoint defaultPoint = UserPoint.empty(userId);
            when(userPointTable.selectById(userId)).thenReturn(defaultPoint);

            // when
            UserPoint result = pointService.getUserPoint(userId);

            // then
            assertThat(result.id()).isEqualTo(userId);
            assertThat(result.point()).isZero();
        }
    }

    @Nested
    class 포인트_내역_조회 {

        @Test
        void 유저_포인트_내역이_존재하면_전체_내역을_반환한다() {
            // given
            List<PointHistory> histories = List.of(
                    new PointHistory(1L, userId, 1000L, TransactionType.CHARGE, now),
                    new PointHistory(2L, userId, 500L, TransactionType.USE, now)
            );
            when(pointHistoryTable.selectAllByUserId(userId)).thenReturn(histories);

            // when
            List<PointHistory> result = pointService.getPointHistories(userId);

            // then
            assertThat(result)
                    .isNotNull()
                    .hasSize(2)
                    .isEqualTo(histories);
        }

        @Test
        void 유저_포인트_내역이_없으면_빈_리스트를_반환한다() {
            // given
            when(pointHistoryTable.selectAllByUserId(userId)).thenReturn(List.of());

            // when
            List<PointHistory> result = pointService.getPointHistories(userId);

            // then
            assertThat(result).isNotNull().isEmpty();
        }
    }
}
