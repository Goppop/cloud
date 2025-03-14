package com.study.list;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

public class Listdemo {
    public static void main(String[] args) {
//        List<Integer> arrayList = new ArrayList<>();
//        List<Integer> linkedList = new LinkedList<>();
//
//        // 添加 100w 个元素
//        long start = System.nanoTime();
//        for (int i = 0; i < 1000000; i++) {
//            arrayList.add(i);
//        }
//        long end = System.nanoTime();
//        System.out.println("ArrayList 插入时间: " + (end - start) / 1e6 + " ms");
//
//        start = System.nanoTime();
//        for (int i = 0; i < 1000000; i++) {
//            linkedList.add(i);
//        }
//        end = System.nanoTime();
//        System.out.println("LinkedList 插入时间: " + (end - start) / 1e6 + " ms");
//
//        // 访问 1000 个随机索引
//        Random rand = new Random();
//        start = System.nanoTime();
//        for (int i = 0; i < 1000; i++) {
//            arrayList.get(rand.nextInt(1000000));
//        }
//        end = System.nanoTime();
//        System.out.println("ArrayList 访问时间: " + (end - start) / 1e6 + " ms");
//
//        start = System.nanoTime();
//        for (int i = 0; i < 1000; i++) {
//            linkedList.get(rand.nextInt(1000000));
//        }
//        end = System.nanoTime();
//        System.out.println("LinkedList 访问时间: " + (end - start) / 1e6 + " ms");
//
//        int testSize = 10000;  // 你可以改成更大的值，比如 100000
//
//        List<Integer> arrayList = new ArrayList<>();
//        List<Integer> linkedList = new LinkedList<>();
//
//        // 测试 ArrayList 头部插入
//        long start = System.nanoTime();
//        for (int i = 0; i < testSize; i++) {
//            arrayList.add(0, i); // 头部插入
//        }
//        long end = System.nanoTime();
//        System.out.println("ArrayList 头部插入时间: " + (end - start) / 1e6 + " ms");
//
//        // 测试 LinkedList 头部插入
//        start = System.nanoTime();
//        for (int i = 0; i < testSize; i++) {
//            linkedList.add(0, i); // 头部插入
//        }
//        end = System.nanoTime();
//        System.out.println("LinkedList 头部插入时间: " + (end - start) / 1e6 + " ms");

        int testSize = 100000;  // 你可以改成更大的值，比如 100000

        List<Integer> arrayList = new ArrayList<>();
        List<Integer> linkedList = new LinkedList<>();

        // 先填充 10000 个元素
        for (int i = 0; i < testSize; i++) {
            arrayList.add(i);
            linkedList.add(i);
        }

        // 测试 ArrayList 中间插入
        long start = System.nanoTime();
        for (int i = 0; i < testSize; i++) {
            arrayList.add(arrayList.size() / 2, i); // 中间插入
        }
        long end = System.nanoTime();
        System.out.println("ArrayList 中间插入时间: " + (end - start) / 1e6 + " ms");

        // 测试 LinkedList 中间插入
        start = System.nanoTime();
        for (int i = 0; i < testSize; i++) {
            linkedList.add(linkedList.size() / 2, i); // 中间插入
        }
        end = System.nanoTime();
        System.out.println("LinkedList 中间插入时间: " + (end - start) / 1e6 + " ms");
    }
}
