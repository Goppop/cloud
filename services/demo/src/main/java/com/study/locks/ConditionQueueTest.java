package com.study.locks;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class ConditionQueueTest {
    private final ReentrantLock lock = new ReentrantLock();
    private final Condition condition = lock.newCondition();

    public void awaitMethod() {
        lock.lock();
        try {
            System.out.println(Thread.currentThread().getName() + " 进入 Condition 队列");
            condition.await(); // 进入 Condition 队列，并释放锁
            System.out.println(Thread.currentThread().getName() + " 被唤醒，尝试重新获取锁");
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            lock.unlock();
        }
    }

    public void signalMethod() {
        lock.lock();
        try {
            System.out.println("调用 signal()，唤醒 Condition 队列的线程");
            condition.signal(); // 让 Condition 队列中的一个线程进入 AQS 同步队列
        } finally {
            lock.unlock();
        }
    }

    public static void main(String[] args) throws InterruptedException {
        ConditionQueueTest test = new ConditionQueueTest();

        Thread t1 = new Thread(test::awaitMethod, "线程1");
        Thread t2 = new Thread(test::awaitMethod, "线程2");

        t1.start();
        t2.start();

        Thread.sleep(2000); // 等待两个线程都进入 Condition 队列

        test.signalMethod(); // 只唤醒一个 Condition 队列的线程
    }
}
