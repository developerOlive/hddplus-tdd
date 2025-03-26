package io.hhplus.tdd.point;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
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
        @DisplayName("정상적으로 포인트를 충전하면 저장소에 저장되고 히스토리가 남는다")
        void 충전_성공_시_포인트_업데이트와_히스토리_기록이_발생한다() {
            long amount = 500L;
            UserPoint before = new UserPoint(userId, 1000L, now);
            UserPoint after = before.charge(amount);

            when(userPointTable.selectById(userId)).thenReturn(before);
            when(userPointTable.insertOrUpdate(userId, 1500L)).thenReturn(after);

            UserPoint result = pointService.charge(userId, amount);

            assertThat(result.point()).isEqualTo(1500L);
            assertThat(result.id()).isEqualTo(userId);
            assertThat(result.updateMillis()).isPositive();

            verify(userPointTable).insertOrUpdate(userId, 1500L);
            verify(pointHistoryTable).insert(
                    eq(userId),
                    eq(amount),
                    eq(TransactionType.CHARGE),
                    anyLong()
            );
        }
    }

    @Nested
    class 포인트_사용 {

        @Test
        @DisplayName("정상적으로 포인트를 사용하면 차감되고 히스토리가 남는다")
        void 사용_성공_시_포인트_업데이트와_히스토리_기록이_발생한다() {
            long amount = 300L;
            UserPoint before = new UserPoint(userId, 1000L, now);
            UserPoint after = before.use(amount);

            when(userPointTable.selectById(userId)).thenReturn(before);
            when(userPointTable.insertOrUpdate(userId, 700L)).thenReturn(after);

            UserPoint result = pointService.use(userId, amount);

            assertThat(result.point()).isEqualTo(700L);
            assertThat(result.id()).isEqualTo(userId);
            assertThat(result.updateMillis()).isPositive();

            verify(userPointTable).insertOrUpdate(userId, 700L);
            verify(pointHistoryTable).insert(
                    eq(userId),
                    eq(amount),
                    eq(TransactionType.USE),
                    anyLong()
            );
        }
    }

    @Nested
    class 포인트_조회 {

        @Test
        @DisplayName("유저 포인트가 존재하면 정상적으로 조회된다")
        void 유저_포인트_정상_조회() {
            UserPoint expected = new UserPoint(userId, 5000L, now);
            when(userPointTable.selectById(userId)).thenReturn(expected);

            UserPoint result = pointService.getUserPoint(userId);

            assertThat(result).isEqualTo(expected);
        }

        @Test
        @DisplayName("유저 포인트가 없을 경우 기본값(0포인트)으로 조회된다")
        void 유저_포인트_없을_경우_기본값_반환() {
            UserPoint defaultPoint = UserPoint.empty(userId);
            when(userPointTable.selectById(userId)).thenReturn(defaultPoint);

            UserPoint result = pointService.getUserPoint(userId);

            assertThat(result.id()).isEqualTo(userId);
            assertThat(result.point()).isZero();
        }
    }

    @Nested
    class 포인트_내역_조회 {

        @Test
        @DisplayName("유저 포인트 내역이 존재하면 전체 내역이 반환된다")
        void 포인트_히스토리_정상_조회() {
            List<PointHistory> histories = List.of(
                    new PointHistory(1L, userId, 1000L, TransactionType.CHARGE, now),
                    new PointHistory(2L, userId, 500L, TransactionType.USE, now)
            );

            when(pointHistoryTable.selectAllByUserId(userId)).thenReturn(histories);

            List<PointHistory> result = pointService.getPointHistories(userId);

            assertThat(result)
                    .isNotNull()
                    .hasSize(2)
                    .isEqualTo(histories);
        }

        @Test
        @DisplayName("유저 포인트 내역이 없으면 빈 리스트가 반환된다")
        void 포인트_히스토리_없을_경우_빈_리스트_반환() {
            when(pointHistoryTable.selectAllByUserId(userId)).thenReturn(List.of());

            List<PointHistory> result = pointService.getPointHistories(userId);

            assertThat(result).isNotNull().isEmpty();
        }
    }
}
