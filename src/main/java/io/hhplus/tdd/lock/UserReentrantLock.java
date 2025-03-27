package io.hhplus.tdd.lock;

public class UserReentrantLock implements UserLock {

    private final java.util.concurrent.locks.ReentrantLock lock;

    public UserReentrantLock(java.util.concurrent.locks.ReentrantLock lock) {
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
