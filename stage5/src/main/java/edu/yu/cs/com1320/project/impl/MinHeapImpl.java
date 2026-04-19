package edu.yu.cs.com1320.project.impl;

import edu.yu.cs.com1320.project.MinHeap;

import java.util.NoSuchElementException;

public class MinHeapImpl<E extends Comparable<E>> extends MinHeap<E> {
    public MinHeapImpl() {
        this.elements = (E[]) new Comparable[5];
    }
    @Override
    public void reHeapify(E element) {
        int index = this.getArrayIndex(element);
        this.upHeap(index);
        index = this.getArrayIndex(element);
        this.downHeap(index);
    }

    @Override
    protected int getArrayIndex(E element) {
        if (element == null) {
            throw new IllegalArgumentException("element cannot be null");
        }
        for(int i = 1; i <= this.count; i++) {
            if(this.elements[i].equals(element)) {
                return i;
            }
        }
        throw new NoSuchElementException("element not found");
    }

    @Override
    protected void doubleArraySize() {
        E[] newArray = (E[]) new Comparable[this.elements.length *2 ];
        for(int i = 1; i < this.elements.length; i++) {
            newArray[i] = this.elements[i];
        }
        this.elements = newArray;
    }
}
