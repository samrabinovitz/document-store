package edu.yu.cs.com1320.project.impl;

import edu.yu.cs.com1320.project.BTree;
import edu.yu.cs.com1320.project.stage6.PersistenceManager;


import java.io.IOException;

public class BTreeImpl<Key extends Comparable<Key>, Value> implements BTree<Key,Value> {
    private static final int Max = 4;
    private Node root;
    private int height;
    private int size;
    private PersistenceManager<Key,Value> persistenceManager;

    private static final class Node {
        private int entryCount;
        private Entry[] entries;

        private Node(int entryCount) {
            this.entryCount = entryCount;
            this.entries = new Entry[Max];
        }
    }

    private static final class Entry {
        private Comparable key;
        private Object value;
        private Node child;

        private Entry(Comparable key, Object value, Node child) {
            this.key = key;
            this.value = value;
            this.child = child;
        }
    }

    public BTreeImpl() {
        this.root = new Node(0);
        this.height = 0;
        this.size = 0;
    }

    private Entry getEntry(Node currentNode, Key key, int height) {
        Entry[] entries = currentNode.entries;
        if (height == 0) {
            for(int i = 0; i < currentNode.entryCount; i++) {
                if(isEqual(key,entries[i].key)) {
                    return entries[i];
                }
            }
            return null;
        }
        for(int i = 0; i < currentNode.entryCount; i++) {
            if(i + 1 == currentNode.entryCount || less(key,entries[i+1].key)) {
                return getEntry(entries[i].child,key,height-1);
            }
        }
        return null;
    }

    private boolean less(Comparable k1, Comparable k2) {
        return k1.compareTo(k2) < 0;
    }
    private boolean isEqual(Comparable k1, Comparable k2) {
        return k1.compareTo(k2) == 0;
    }

    @Override
    public Value get(Key k) {
        if(k == null) {
            throw new IllegalArgumentException("key cannot be null");
        }
        Entry entry = getEntry(this.root,k,this.height);
        if(entry == null) {
            return null;
        }
        if(entry.value != null) {
            return(Value) entry.value;
        }
        try {
            Value valueFromDisk = this.persistenceManager.deserialize(k);
            if(valueFromDisk == null) {
                return null;
            }
            entry.value = valueFromDisk;
            return valueFromDisk;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Value put(Key k, Value v) {
        if(k == null) {
            throw new IllegalArgumentException("key cannot be null");
        }
        Entry there = getEntry(this.root,k,this.height);
        if(there != null) {
            Value oldValue = null;

            if (there.value == null && this.persistenceManager != null) {
                try {
                    oldValue = this.persistenceManager.deserialize(k);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            } else {
                oldValue = (Value) there.value;
            }
            there.value = v;
            return oldValue;
        }
        Node newNode = put(this.root,k,v,this.height);
        this.size++;
        if(newNode == null) {
            return null;
        }
        Node newRoot = new Node(2);
        newRoot.entries[0] = new Entry(this.root.entries[0].key,null,this.root);
        newRoot.entries[1] = new Entry(newNode.entries[0].key,null,newNode);
        this.root = newRoot;
        this.height++;
        return null;
    }

    private Node put(Node currentNode, Key k, Value v, int height) {
        int index;
        Entry newEntry = new Entry(k,v,null);

        if(height == 0) {
            for(index = 0; index < currentNode.entryCount; index++) {
                if(less(k,currentNode.entries[index].key)) {
                    break;
                }
            }
        } else {
            for(index = 0; index < currentNode.entryCount; index++) {
                if(index + 1 == currentNode.entryCount || less(k,currentNode.entries[index + 1].key)) {
                    Node newNode = put(currentNode.entries[index++].child,k,v,height-1);
                    if(newNode == null) {
                        return null;
                    }
                    newEntry.key = newNode.entries[0].key;
                    newEntry.value = null;
                    newEntry.child = newNode;
                    break;
                }
            }
        }
        for(int i = currentNode.entryCount; i > index; i--) {
            currentNode.entries[i] = currentNode.entries[i-1];
        }
        currentNode.entries[index] = newEntry;
        currentNode.entryCount++;
        if(currentNode.entryCount < Max) {
            return null;
        }
        return split(currentNode);
    }

    private Node split(Node currentNode) {
        Node newNode = new Node(Max/2);
        currentNode.entryCount = Max/2;
        for(int i = 0; i < Max/2; i++) {
            newNode.entries[i] = currentNode.entries[Max/2 + i];
        }
        return newNode;
    }

    @Override
    public void moveToDisk(Key k) throws IOException {
        if (k == null) {
            throw new IllegalArgumentException("key cannot be null");
        }
        if(this.persistenceManager == null) {
            throw new IllegalStateException("no persistenceManager");
        }
        Entry entry = getEntry(this.root,k,this.height);
        if(entry == null || entry.value == null) {
            return;
        }
        this.persistenceManager.serialize(k,(Value) entry.value);
        entry.value = null;
    }

    @Override
    public void setPersistenceManager(PersistenceManager<Key, Value> pm) {
        if(pm == null) {
            throw new IllegalArgumentException("pm cannot be null");
        }
        this.persistenceManager = pm;
    }
}
