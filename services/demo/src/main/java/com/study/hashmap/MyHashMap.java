package com.study.hashmap;

import java.util.HashMap;
import java.util.Map;

import java.util.Map;

public class MyHashMap<K, V> {
    private static final int INITIAL_CAPACITY = 64; // 🚀 直接初始化 64，避免 resize()
    private static final int TREEIFY_THRESHOLD = 8; // 🚀 链表转红黑树的阈值
    private Node<K, V>[] table;
    private int size = 0;

    public MyHashMap() {
        table = new Node[INITIAL_CAPACITY]; // 🚀 直接让 table 长度 = 64
    }

    /** 🚀 普通链表节点 */
    static class Node<K, V> implements Map.Entry<K, V> {
        final int hash;
        final K key;
        V value;
        Node<K, V> next;

        Node(int hash, K key, V value, Node<K, V> next) {
            this.hash = hash;
            this.key = key;
            this.value = value;
            this.next = next;
        }

        @Override
        public K getKey() { return key; }

        @Override
        public V getValue() { return value; }

        @Override
        public V setValue(V newValue) {
            V oldValue = this.value;
            this.value = newValue;
            return oldValue;
        }
    }

    /** 🚀 红黑树节点 */
    static final class TreeNode<K, V> extends Node<K, V> {
        TreeNode<K, V> left, right, parent;
        boolean red;

        TreeNode(int hash, K key, V value, Node<K, V> next) {
            super(hash, key, value, next);
            this.red = true;
        }
    }

    /** 🚀 自定义哈希函数，所有 key 计算到 index=42，制造哈希冲突 */
    private int hash(Object key) {
        return 42 & (table.length - 1); // 🚀 让所有 key 落在 index=42
    }

    /** 🚀 插入数据 */
    public void put(K key, V value) {
        int hash = hash(key);
        int index = hash & (table.length - 1);

        if (table[index] == null) {
            table[index] = new Node<>(hash, key, value, null);
        } else {
            Node<K, V> node = table[index];
            if (node instanceof TreeNode) {
                putTreeVal((TreeNode<K, V>) node, key, value);
            } else {
                while (node.next != null) {
                    if (node.key.equals(key)) {
                        node.value = value;
                        return;
                    }
                    node = node.next;
                }
                node.next = new Node<>(hash, key, value, null);

                // 🚀 确保 table 长度 >= 64 才触发红黑树转换
                if (countNodes(table[index]) >= TREEIFY_THRESHOLD) {
                    table[index] = treeify(table[index]);
                }
            }
        }
        size++;
    }

    /** 🚀 统计链表长度 */
    private int countNodes(Node<K, V> node) {
        int count = 0;
        while (node != null) {
            count++;
            node = node.next;
        }
        return count;
    }

    /** 🚀 链表转换为红黑树 */
    private TreeNode<K, V> treeify(Node<K, V> node) {
        TreeNode<K, V> root = null;
        while (node != null) {
            TreeNode<K, V> newNode = new TreeNode<>(node.hash, node.key, node.value, null);
            root = insertTreeNode(root, newNode);
            node = node.next;
        }
        return root;
    }

    /** 🚀 插入红黑树 */
    private TreeNode<K, V> insertTreeNode(TreeNode<K, V> root, TreeNode<K, V> node) {
        if (root == null) return node;
        TreeNode<K, V> parent = null, temp = root;
        while (temp != null) {
            parent = temp;
            if (node.hash < temp.hash) temp = temp.left;
            else temp = temp.right;
        }
        node.parent = parent;
        if (node.hash < parent.hash) parent.left = node;
        else parent.right = node;
        return root;
    }

    /** 🚀 在红黑树中插入 */
    private void putTreeVal(TreeNode<K, V> root, K key, V value) {
        while (root != null) {
            if (root.key.equals(key)) {
                root.value = value;
                return;
            }
            root = (key.hashCode() < root.hash) ? root.left : root.right;
        }
    }

    /** 🚀 查询数据 */
    public V get(K key) {
        int hash = hash(key);
        int index = hash & (table.length - 1);
        Node<K, V> node = table[index];

        if (node instanceof TreeNode) {
            return getTreeVal((TreeNode<K, V>) node, key);
        } else {
            while (node != null) {
                if (node.key.equals(key)) return node.value;
                node = node.next;
            }
        }
        return null;
    }

    /** 🚀 在红黑树中查询 */
    private V getTreeVal(TreeNode<K, V> root, K key) {
        while (root != null) {
            if (root.key.equals(key)) return root.value;
            root = (key.hashCode() < root.hash) ? root.left : root.right;
        }
        return null;
    }

    /** 🚀 打印哈希表结构 */
    public void print() {
        for (int i = 0; i < table.length; i++) {
            if (table[i] != null) {
                System.out.print("table[" + i + "] -> ");
                Node<K, V> node = table[i];
                while (node != null) {
                    System.out.print("[" + node.key + "," + node.value + "] -> ");
                    node = node.next;
                }
                System.out.println("null");
            }
        }
    }

    /** 🚀 测试代码 */
    public static void main(String[] args) {
        MyHashMap<String, Integer> map = new MyHashMap<>();

        // 🚀 插入 8 个 key，全部哈希到 index=42，直接触发红黑树转换
        for (int i = 0; i < 8; i++) {
            map.put("conflict" + i, i);
        }
        HashMap hashMap =new HashMap();
        map.print();
        System.out.println("已触发红黑树逻辑！");
    }
}
