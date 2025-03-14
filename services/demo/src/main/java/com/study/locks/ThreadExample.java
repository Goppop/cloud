package com.study.locks;

class MyThread extends Thread {
    @Override
    public void run() {
        System.out.println(Thread.currentThread().getName() + " 正在执行 run 方法");
    }
}
public class ThreadExample {



        public static void main(String[] args) {
            MyThread t1 = new MyThread();

            System.out.println("直接调用 run 方法：");
            t1.run();  // 仅仅是普通方法调用

            System.out.println("调用 start 方法：");
            t1.start();  // 线程启动，新线程执行 run()
        }
    }



