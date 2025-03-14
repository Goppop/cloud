package com.study.threadlocal;

public class ThreadLocalExample {
    private static final ThreadLocal<Integer> threadLocalVar = new ThreadLocal<>();

    public static void main(String[] args) {
        Runnable task = () -> {
            threadLocalVar.set((int) (Math.random() * 100));  // 每个线程存储不同的值
            System.out.println(Thread.currentThread().getName() + " - " + threadLocalVar.get());
            threadLocalVar.remove(); // 避免内存泄漏
        };

        Thread thread1 = new Thread(task, "Thread-1");
        Thread thread2 = new Thread(task, "Thread-2");

        thread1.start();
        thread2.start();
    }
}
