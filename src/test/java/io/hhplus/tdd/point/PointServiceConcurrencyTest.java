package io.hhplus.tdd.point;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import io.hhplus.tdd.exception.InvalidAmountException;
import io.hhplus.tdd.lock.UserReentrantLockManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

@SpringBootTest
class PointServiceConcurrencyTest {

    private static final Logger log = LoggerFactory.getLogger(PointServiceConcurrencyTest.class);

    @Autowired
    private UserPointTable userPointTable;
    @Autowired
    private PointHistoryTable pointHistoryTable;
    @Autowired
    private UserReentrantLockManager userReentrantLockManager;

    private final long userId = 1L;
    private PointService pointService;

    @BeforeEach
    void 테스트_테이블_초기화() throws Exception {
        resetUserPointTable();
        resetPointHistoryTable();
        pointService = new PointService(userPointTable, pointHistoryTable, userReentrantLockManager);
    }

    private void resetUserPointTable() throws Exception {
        Field tableField = UserPointTable.class.getDeclaredField("table");
        tableField.setAccessible(true);
        ((Map<?, ?>) tableField.get(userPointTable)).clear();
    }

    private void resetPointHistoryTable() throws Exception {
        Field tableField = PointHistoryTable.class.getDeclaredField("table");
        tableField.setAccessible(true);
        ((List<?>) tableField.get(pointHistoryTable)).clear();
    }

    @Nested
    class 포인트를_동시에_충전할_때 {

        @Test
        void 동시_충전요청이_순차적으로_처리되는지_확인한다_테스트도구_CountDownLatch() throws InterruptedException {
            int threadCount = 5;
            long chargeAmount = 100L;

            ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            CountDownLatch latch = new CountDownLatch(threadCount);

            for (int i = 0; i < threadCount; i++) {
                int idx = i;
                executor.execute(() -> {
                    Thread.currentThread().setName("ChargeThread-" + idx);
                    log.info("[{}] 포인트 충전 요청 금액 : {}P", Thread.currentThread().getName(), chargeAmount);

                    pointService.charge(userId, chargeAmount);
                    log.info("[{}] 포인트 충전 완료, 사용 금액: {}P", Thread.currentThread().getName(), chargeAmount);

                    latch.countDown();
                });
            }
            latch.await();

            UserPoint result = pointService.getUserPoint(userId);
            log.info("최종 포인트: {}", result.point());
            assertThat(result.point()).isEqualTo(threadCount * chargeAmount);
        }

        @Test
        void 동시_충전요청이_순차적으로_처리되는지_확인한다_테스트도구_CountDownLatch_CyclicBarrier() throws InterruptedException {
            int threadCount = 10;
            long chargeAmount = 100L;

            ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            CountDownLatch latch = new CountDownLatch(threadCount);
            CyclicBarrier barrier = new CyclicBarrier(threadCount);

            for (int i = 0; i < threadCount; i++) {
                int idx = i;
                executor.execute(() -> {
                    try {
                        Thread.currentThread().setName("ChargeThread-" + idx);
                        log.info("[{}] 충전 대기 중", Thread.currentThread().getName());

                        barrier.await();
                        log.info("[{}] 충전 시작", Thread.currentThread().getName());

                        pointService.charge(userId, chargeAmount);
                        log.info("[{}] 포인트 충전 완료, 사용 금액: {}P", Thread.currentThread().getName(), chargeAmount);
                    } catch (Exception e) {
                        log.error("[{}] 충전 중 예외 발생", Thread.currentThread().getName(), e);
                    } finally {
                        latch.countDown();
                    }
                });
            }
            latch.await();

            UserPoint result = pointService.getUserPoint(userId);
            log.info("최종 포인트: {}", result.point());
            assertThat(result.point()).isEqualTo(threadCount * chargeAmount);
        }

        @Test
        void 동시_충전_요청_이후_히스토리가_요청_수만큼_남는다() throws InterruptedException {
            int threadCount = 5;
            ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            CountDownLatch latch = new CountDownLatch(threadCount);

            for (int i = 0; i < threadCount; i++) {
                int idx = i;
                executor.execute(() -> {
                    Thread.currentThread().setName("T-" + idx);
                    log.info("[{}] 충전 요청", Thread.currentThread().getName());
                    pointService.charge(userId, 100L);
                    log.info("[{}] 충전 완료", Thread.currentThread().getName());
                    latch.countDown();
                });
            }
            latch.await();

            List<PointHistory> histories = pointService.getPointHistories(userId);
            for (PointHistory history : histories) {
                log.info("히스토리: [{}] {}P, {}", Thread.currentThread().getName(), history.amount(), history.type());
            }
            assertThat(histories).hasSize(threadCount);
        }
    }

    @Nested
    class 포인트를_동시에_사용할_때 {

        @Test
        void 동시_사용요청이_순차적으로_처리되는지_확인한다_테스트도구_CountDownLatch() throws InterruptedException {
            pointService.charge(userId, 1000);
            log.info("초기 포인트 1000 충전 완료");

            int threadCount = 5;
            long useAmount = 100L;

            ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            CountDownLatch latch = new CountDownLatch(threadCount);

            for (int i = 0; i < threadCount; i++) {
                int idx = i;
                executor.execute(() -> {
                    Thread.currentThread().setName("UseThread-" + idx);
                    log.info("[{}] 포인트 사용 요청 금액 : {}P", Thread.currentThread().getName(), useAmount);

                    pointService.use(userId, useAmount);

                    log.info("[{}] 포인트 사용 완료, 사용 금액: {}P", Thread.currentThread().getName(), useAmount);
                    latch.countDown();
                });
            }

            latch.await();

            UserPoint result = pointService.getUserPoint(userId);
            log.info("최종 포인트: {}", result.point());
            assertThat(result.point()).isEqualTo(1000 - (threadCount * useAmount));
        }

        @Test
        void 동시_사용요청이_순차적으로_처리되는지_확인한다_테스트도구_CountDownLatch_CyclicBarrier() throws InterruptedException {
            pointService.charge(userId, 1000);
            log.info("초기 포인트 1000 충전 완료");

            int threadCount = 5;
            long useAmount = 100L;

            ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            CountDownLatch latch = new CountDownLatch(threadCount);
            CyclicBarrier barrier = new CyclicBarrier(threadCount);

            for (int i = 0; i < threadCount; i++) {
                int idx = i;
                executor.execute(() -> {
                    try {
                        Thread.currentThread().setName("UseThread-" + idx);
                        log.info("[{}] 사용 대기 중", Thread.currentThread().getName());

                        barrier.await();
                        log.info("[{}] 사용 시작", Thread.currentThread().getName());

                        pointService.use(userId, useAmount);
                        log.info("[{}] 포인트 사용 완료, 사용 금액: {}P", Thread.currentThread().getName(), useAmount);
                    } catch (Exception e) {
                        log.error("[{}] 사용 중 예외 발생", Thread.currentThread().getName(), e);
                    } finally {
                        latch.countDown();
                    }
                });
            }

            latch.await();

            UserPoint result = pointService.getUserPoint(userId);
            log.info("최종 포인트: {}", result.point());
            assertThat(result.point()).isEqualTo(1000 - (threadCount * useAmount));
        }

        @Test
        void 동시_사용_요청_시_잔액보다_많이_사용하면_일부_요청만_성공하고_나머지는_예외가_발생한다() throws InterruptedException {
            pointService.charge(userId, 300);
            log.info("초기 포인트 300 충전 완료");

            int threadCount = 5;
            long useAmount = 100L;

            ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            CountDownLatch latch = new CountDownLatch(threadCount);

            for (int i = 0; i < threadCount; i++) {
                int idx = i;
                executor.execute(() -> {
                    try {
                        Thread.currentThread().setName("UseThread-" + idx);
                        pointService.use(userId, useAmount);
                        log.info("[{}] 사용 성공, 사용 금액: {}P", Thread.currentThread().getName(), useAmount);
                    } catch (Exception e) {
                        log.warn("[{}] 사용 실패: {}", Thread.currentThread().getName(), e.getMessage());
                    } finally {
                        latch.countDown();
                    }
                });
            }

            latch.await();

            UserPoint result = pointService.getUserPoint(userId);
            log.info("최종 포인트: {}", result.point());
            assertThat(result.point()).isLessThanOrEqualTo(0L);
            assertThat(result.point()).isGreaterThanOrEqualTo(0L);
        }
    }

    @Test
    void 충전과_사용이_동시에_요청되면_히스토리도_정상적으로_기록된다() throws InterruptedException {
        pointService.charge(userId, 500);
        log.info("초기 포인트 500 충전 완료");

        ExecutorService executor = Executors.newFixedThreadPool(4);
        CountDownLatch latch = new CountDownLatch(4);

        executor.execute(() -> {
            log.info("[T-0] charge 100");
            pointService.charge(userId, 100);
            latch.countDown();
        });

        executor.execute(() -> {
            log.info("[T-1] use 200");
            pointService.use(userId, 200);
            latch.countDown();
        });

        executor.execute(() -> {
            log.info("[T-2] charge 300");
            pointService.charge(userId, 300);
            latch.countDown();
        });

        executor.execute(() -> {
            log.info("[T-3] use 100");
            pointService.use(userId, 100);
            latch.countDown();
        });

        latch.await();

        List<PointHistory> histories = pointService.getPointHistories(userId);
        for (PointHistory history : histories) {
            log.info("히스토리 기록: [{}] {}P, {}", history.type(), history.amount(), history.updateMillis());
        }

        assertThat(histories).hasSize(5);
        long chargeCount = histories.stream().filter(h -> h.type() == TransactionType.CHARGE).count();
        long useCount = histories.stream().filter(h -> h.type() == TransactionType.USE).count();

        assertThat(chargeCount).isEqualTo(3);
        assertThat(useCount).isEqualTo(2);

        UserPoint result = pointService.getUserPoint(userId);
        log.info("최종 포인트: {}", result.point());
        assertThat(result.point()).isEqualTo(600L);
    }

    @Nested
    class 포인트_경계값_테스트 {

        @BeforeEach
        void setUp() {
            pointService = new PointService(userPointTable, pointHistoryTable, userReentrantLockManager);
            pointService.charge(userId, 1000);
            log.info("초기 포인트 1000 충전 완료");
        }

        @Nested
        class 사용_요청_경계값 {

            @Test
            void 사용금액이_0원이면_예외가_발생한다() {
                assertThatThrownBy(() -> pointService.use(userId, 0))
                        .isInstanceOf(InvalidAmountException.class);
            }

            @Test
            void 사용금액이_음수이면_예외가_발생한다() {
                assertThatThrownBy(() -> pointService.use(userId, -1))
                        .isInstanceOf(InvalidAmountException.class);
            }

            @Test
            void 사용금액이_양수이면_정상적으로_포인트가_차감된다() {
                pointService.use(userId, 100);
                log.info("100 포인트 사용 완료");

                UserPoint result = pointService.getUserPoint(userId);
                log.info("최종 포인트: {}", result.point());
                assertThat(result.point()).isEqualTo(900);
            }
        }

        @Nested
        class 충전_요청_경계값 {

            @Test
            void 충전금액이_0원이면_예외가_발생한다() {
                assertThatThrownBy(() -> pointService.charge(userId, 0))
                        .isInstanceOf(InvalidAmountException.class);
            }

            @Test
            void 충전금액이_음수이면_예외가_발생한다() {
                assertThatThrownBy(() -> pointService.charge(userId, -100))
                        .isInstanceOf(InvalidAmountException.class);
            }

            @Test
            void 충전금액이_양수이면_정상적으로_포인트가_증가한다() {
                pointService.charge(userId, 200);
                UserPoint result = pointService.getUserPoint(userId);
                assertThat(result.point()).isEqualTo(1200);
            }
        }
    }

    @Nested
    class 포인트_내역_조회 {

        @Test
        void 유저_포인트_내역이_존재하면_전체_내역을_반환한다() {
            pointService.charge(userId, 1000);
            pointService.use(userId, 500);

            List<PointHistory> result = pointService.getPointHistories(userId);

            assertThat(result).isNotNull().hasSize(2);
        }

        @Test
        void 유저_포인트_내역이_없으면_빈_리스트를_반환한다() {
            List<PointHistory> result = pointService.getPointHistories(userId);
            assertThat(result).isNotNull().isEmpty();
        }
    }
}
