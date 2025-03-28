package io.hhplus.tdd.point;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public record PointService(
        UserPointTable userPointTable,
        PointHistoryTable pointHistoryTable
) {

    public UserPoint getUserPoint(long userId) {
        return userPointTable.selectById(userId);
    }

    public List<PointHistory> getPointHistories(long userId) {
        return pointHistoryTable.selectAllByUserId(userId);
    }

    public UserPoint charge(long userId, long amount) {
        UserPoint beforePoint = userPointTable.selectById(userId);
        UserPoint chargedPoint = beforePoint.charge(amount);

        userPointTable.insertOrUpdate(userId, chargedPoint.point());
        pointHistoryTable.insert(
                userId,
                amount,
                TransactionType.CHARGE,
                chargedPoint.updateMillis()
        );

        return chargedPoint;
    }

    public UserPoint use(long userId, long amount) {
        UserPoint beforePoint = userPointTable.selectById(userId);
        UserPoint usedPoint = beforePoint.use(amount);

        userPointTable.insertOrUpdate(userId, usedPoint.point());
        pointHistoryTable.insert(
                userId,
                amount,
                TransactionType.USE,
                usedPoint.updateMillis()
        );

        return usedPoint;
    }
}
