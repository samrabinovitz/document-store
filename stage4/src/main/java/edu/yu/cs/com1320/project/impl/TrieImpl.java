package edu.yu.cs.com1320.project.impl;

import edu.yu.cs.com1320.project.Trie;

import java.util.*;

public class TrieImpl<Value> implements Trie<Value> {
    private int alphabetSize = 256;
    private Node<Value> root;

    private class Node<Value> {
        private Set<Value> values = new HashSet<>();
        private Node<Value>[] links = new Node[alphabetSize];
    }

    public TrieImpl() {
        this.root = new Node<>();
    }

    @Override
    public void put(String key, Value val) {
        if(key == null || val == null) {
            return;
        }
        this.root = put(this.root , key, val, 0);
    }

    private Node<Value> put(Node<Value> node, String key, Value val, int d) {
        if(node == null) {
            node = new Node<>();
        }
        if(d == key.length()) {
            node.values.add(val);
            return node;
        }
        char c = key.charAt(d);
        node.links[c] = put(node.links[c],key,val,d+1);
        return node;
    }

    @Override
    public List<Value> getSorted(String key, Comparator<Value> comparator) {
        if(key == null || comparator == null) {
            return new ArrayList<>();
        }
        Set<Value> values = this.get(key);
        List<Value> list = new ArrayList<>(values);
        list.sort(comparator);
        return list;
    }

    @Override
    public Set<Value> get(String key) {
        if(key == null) {
            return new HashSet<>();
        }
        Node<Value> node = get(this.root, key, 0);
        if(node == null) {
            return new HashSet<>();
        }
        return new HashSet<>(node.values);
    }

    private Node<Value> get(Node<Value> node, String key, int d) {
        if(node == null) {
            return null;
        }
        if(d == key.length()) {
            return node;
        }
        char c = key.charAt(d);
        return get(node.links[c], key, d+1);
    }

    @Override
    public List<Value> getAllWithPrefixSorted(String prefix, Comparator<Value> comparator) {
        if(prefix == null || comparator == null) {
            return new ArrayList<>();
        }
        Node<Value> prefixNode = get(this.root, prefix, 0);
        if(prefixNode == null) {
            return new ArrayList<>();
        }
        Set<Value> matches = new HashSet<>();
        collect(prefixNode, matches);
        List<Value> result = new ArrayList<>(matches);
        result.sort(comparator);
        return result;
    }

    private void collect(Node<Value> node, Set<Value> matches) {
        if(node == null) {
            return;
        }
        matches.addAll(node.values);
        for(int i = 0; i < node.links.length; i++) {
            collect(node.links[i], matches);
        }
    }

    @Override
    public Set<Value> deleteAllWithPrefix(String prefix) {
        if(prefix == null) {
            return new HashSet<>();
        }
        Set<Value> deletedValues = new HashSet<>();
        if(prefix.isEmpty()) {
            collect(this.root, deletedValues);
            this.root = new Node<>();
            return deletedValues;
        }
        this.root = deleteAllWithPrefix(this.root, prefix, 0, deletedValues);
        return deletedValues;
    }

    private Node<Value> deleteAllWithPrefix(Node<Value> node, String prefix, int d, Set<Value> deletedValues) {
        if(node == null) {
            return null;
        }
        if(d == prefix.length()) {
            collect(node, deletedValues);
            return null;
        }
        char c = prefix.charAt(d);
        node.links[c] = deleteAllWithPrefix(node.links[c], prefix, d+1, deletedValues);
        if(! node.values.isEmpty()) {
            return node;
        }
        for(int i = 0; i < node.links.length; i++) {
            if(node.links[i] != null) {
                return node;
            }
        }
        return null;
    }

    @Override
    public Set<Value> deleteAll(String key) {
        if(key == null) {
            return new HashSet<>();
        }
        Set<Value> deletedValues = new HashSet<>();
        this.root = deleteAll(this.root, key, 0 , deletedValues);
        return deletedValues;
    }

    private Node<Value> deleteAll(Node<Value> node, String key, int d, Set<Value> deletedValues) {
        if(node == null) {
            return null;
        }
        if(d == key.length()) {
            deletedValues.addAll(node.values);
            node.values.clear();
        } else {
            char c = key.charAt(d);
            node.links[c] = deleteAll(node.links[c], key, d+1, deletedValues);
        }
        if(! node.values.isEmpty()) {
            return node;
        }
        for(int i = 0; i < node.links.length; i++) {
            if (node.links[i] != null) {
                return node;
            }
        }
        return null;
    }

    @Override
    public Value delete(String key, Value val) {
        if(key == null || val == null) {
            return null;
        }
        Value[] deleted = (Value[]) new Object[1];
        this.root = delete(this.root,key, val, 0 , deleted);
        return deleted[0];
    }

    private Node<Value> delete(Node<Value> node, String key, Value val, int d , Value[] deleted) {
        if(node == null) {
            return null;
        }
        if(d == key.length()) {
            if(node.values.remove(val)) {
                deleted[0] = val;
            }
        } else {
            char c = key.charAt(d);
            node.links[c] = delete(node.links[c], key , val, d+1, deleted);
        }
        if(! node.values.isEmpty()) {
            return node;
        }
        for(int i = 0; i < node.links.length; i++) {
            if(node.links[i] != null) {
                return node;
            }
        }
        return null;
    }
}
