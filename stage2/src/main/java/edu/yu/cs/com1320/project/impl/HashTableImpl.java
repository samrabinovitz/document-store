package edu.yu.cs.com1320.project.impl;

import edu.yu.cs.com1320.project.HashTable;

import java.util.*;

public class HashTableImpl<Key,Value> implements HashTable<Key,Value> {
    private int capacity = 5;

    private class Entry<K,V> {
        K key;
        V value;
        Entry<K,V> next;

        Entry(K key, V value, Entry<K,V> next) {
            this.key = key;
            this.value = value;
            this.next = next;
        }
    }

    private Entry<Key,Value>[] table = (Entry<Key,Value>[]) new Entry[capacity];
    private int size;

    public HashTableImpl(){}

    private int index(Key k) {
        if(k == null){
            throw new NullPointerException("key cannot be null");
        }
        return (k.hashCode() & 0x7fffffff) % capacity;
    }

    @Override
    public Value get(Key k) {
        if(k == null){
            return null;
        }
        int i = index(k);
        for(Entry<Key,Value> e = table[i]; e != null; e = e.next) {
            if(k.equals(e.key)) {
                return e.value;
            }
        }
        return null;
    }

    @Override
    public Value put(Key k, Value v) {
        int i = index(k);
        Entry<Key,Value> head = table[i];

        for(Entry<Key,Value> e = head; e != null; e = e.next) {
            if(k.equals(e.key)) {
                Value old = e.value;
                if (v == null) {
                    deleteNode(i, k);
                } else {
                    e.value = v;
                }
                return old;
            }
        }
        if(v == null) {
            return null;
        }
        table[i] = new Entry<>(k,v,head);
        size++;
        return null;
    }

    private void deleteNode(int bucketIndex,Key k)  {
        Entry<Key,Value> previous = null;
        Entry<Key,Value> current = table[bucketIndex];
        while (current != null) {
            if(k.equals(current.key)) {
                if (previous == null) {
                    table[bucketIndex] = current.next;
                } else {
                    previous.next = current.next;
                }
                size--;
                return;
            }
            previous = current;
            current = current.next;
        }
    }

    @Override
    public boolean containsKey(Key key) {
        if(key == null) {
            throw new NullPointerException("key cannot be null");
        }
        int i = index(key);
        for(Entry<Key,Value> e = table[i]; e != null; e = e.next) {
            if(key.equals(e.key)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Set<Key> keySet() {
        Set<Key> keys = new HashSet<>();
        for(int i = 0; i < this.table.length; i++) {
            Entry<Key,Value> current = this.table[i];
            while(current != null) {
                keys.add(current.key);
                current = current.next;
            }
        }
        return Collections.unmodifiableSet(keys);
    }

    @Override
    public Collection<Value> values() {
        Collection<Value> values = new ArrayList<>();
        for(int i = 0; i < this.table.length; i++){
            Entry<Key,Value> current = this.table[i];
            while(current != null){
                values.add(current.value);
                current = current.next;
            }
        }
        return Collections.unmodifiableCollection(values);
    }

    @Override
    public int size() {
        return size;
    }
}
