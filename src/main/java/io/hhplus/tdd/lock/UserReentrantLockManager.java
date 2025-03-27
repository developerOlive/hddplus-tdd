package io.hhplus.tdd.lock;

import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

@Component
public class UserReentrantLockManager {

    // KEY: userId (사용자 식별자)
    // VALUE: 해당 사용자 전용 락 객체 (UserLock)
    private final ConcurrentHashMap<Long, UserLock> lockMap = new ConcurrentHashMap<>();

    public UserLock getLock(long userId) {
        return lockMap.computeIfAbsent(userId, id -> new UserReentrantLock(new ReentrantLock()));
    }
}
