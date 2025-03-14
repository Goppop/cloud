package com.study.treemap;

import java.util.TreeMap;

public class TreeMapExample {
    public static void main(String[] args) {
        TreeMap<Integer, String> treeMap = new TreeMap<>();

        // 添加键值对（会自动排序）
        treeMap.put(5, "苹果");
        treeMap.put(2, "香蕉");
        treeMap.put(8, "橙子");
        treeMap.put(1, "葡萄");

        // 遍历输出（按 Key 递增排序）
        System.out.println(treeMap);
    }
}
