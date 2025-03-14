package com.study;


import java.util.ArrayList;
import java.util.List;

public class size {

    public List<String> test(float lengthCm, float widthCm, float heightCm, float weightKg) {
        // 单位转换
        double length = lengthCm / 2.54;  // cm -> in
        double width = widthCm / 2.54;
        double height = heightCm / 2.54;
        double weight = weightKg / 0.454; // kg -> lb

        // 计算围长（girth）
        double girth = length + 2 * (width + height);

        // 计算体积重（向上取整）
        double volumeWeight = Math.ceil((length * width * height) / 250.0);

        // 变量重量，取产品重量（LB）和体积重之间的最大值
        double variableWeight = Math.max(weight, volumeWeight);

        List<String> categories = new ArrayList<>();

        // 1. 判断 OUT_SPACE
        if (variableWeight > 150 || length > 108 || girth > 165) {
            categories.add("OUT_SPACE");
            return categories; // 满足则直接返回，不再判断后续类型
        }

        // 2. 判断 OVERSIZE（如果不属于 OUT_SPACE）
        if ((girth > 130 && girth <= 165) || (length > 96 && length <= 108)) {
            categories.add("OVERSIZE");
            return categories; // 满足 OVERSIZE 就不再判断 AHS
        }

        // 3. 判断 AHS（如果不属于 OUT_SPACE 或 OVERSIZE）
        boolean isAHSWeight = (variableWeight > 50 && variableWeight <= 150);
        boolean isAHSSize = (girth > 105) || (length > 48 && length <= 63) || (width >= 30);

        if (isAHSWeight) {
            categories.add("AHS-WEIGHT");
        }
        if (isAHSSize) {
            categories.add("AHS-SIZE");
        }

        return categories;
    }


    public static void main(String[] args) {
        size obj = new size();

        System.out.println(obj.test(68, 70, 60, 23));
        System.out.println(obj.test(114.5f, 42, 26, 47.5f));
        System.out.println(obj.test(162, 60, 11, 14));
        System.out.println(obj.test(113, 64, 42.5f, 35.85f));
        System.out.println(obj.test(114.5f, 17, 51.5f, 16.5f));

    }
}
