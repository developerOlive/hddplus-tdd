package io.hhplus.tdd.lock;

import java.util.concurrent.locks.ReentrantLock;

public class UserReentrantLock implements UserLock {

    private final ReentrantLock lock;

    public UserReentrantLock(ReentrantLock lock) {
        this.lock = lock;
    }

    @Override
    public void lock() {
        lock.lock();
    }

    @Override
    public void unlock() {
        if (lock.isHeldByCurrentThread()) {
            lock.unlock();
        }
    }
}
