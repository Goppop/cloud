package com.study.locks;

import java.util.concurrent.locks.ReentrantLock;

public class ReentrantLockReentrancy {
    private final ReentrantLock lock = new ReentrantLock();

    public void outerMethod() {
        lock.lock();
        try {
            System.out.println(Thread.currentThread().getName() + " - 进入 outerMethod");
            innerMethod(); // 再次获取同一个锁
        } finally {
            lock.unlock();
        }
    }

    private void innerMethod() {
        lock.lock();
        try {
            System.out.println(Thread.currentThread().getName() + " - 进入 innerMethod");
        } finally {
            lock.unlock();
        }
    }

    public static void main(String[] args) {
        ReentrantLockReentrancy example = new ReentrantLockReentrancy();
        Thread t1 = new Thread(example::outerMethod);
        Thread t2 = new Thread(example::outerMethod);

        t1.start();
        t2.start();
    }
}
