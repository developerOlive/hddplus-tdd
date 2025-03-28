package io.hhplus.tdd.point;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import io.hhplus.tdd.lock.UserReentrantLockManager;
import org.springframework.stereotype.Service;
import io.hhplus.tdd.lock.UserLock;

import java.util.List;

@Service
public record PointService(UserPointTable userPointTable,
                           PointHistoryTable pointHistoryTable,
                           UserReentrantLockManager userReentrantLockManager) {

    public UserPoint getUserPoint(long userId) {
        return userPointTable.selectById(userId);
    }

    public List<PointHistory> getPointHistories(long userId) {
        return pointHistoryTable.selectAllByUserId(userId);
    }

    public UserPoint charge(long userId, long amount) {
        UserLock lock = userReentrantLockManager.getLock(userId);
        lock.lock();
        try {
            UserPoint before = userPointTable.selectById(userId);
            UserPoint after = before.charge(amount);
            userPointTable.insertOrUpdate(userId, after.point());
            pointHistoryTable.insert(userId, amount, TransactionType.CHARGE, after.updateMillis());
            return after;
        } finally {
            lock.unlock();
        }
    }

    public UserPoint use(long userId, long amount) {
        UserLock lock = userReentrantLockManager.getLock(userId);
        lock.lock();
        try {
            UserPoint before = userPointTable.selectById(userId);
            UserPoint after = before.use(amount);
            userPointTable.insertOrUpdate(userId, after.point());
            pointHistoryTable.insert(userId, amount, TransactionType.USE, after.updateMillis());
            return after;
        } finally {
            lock.unlock();
        }
    }
}
