package edu.yu.cs.com1320.project.impl;

import edu.yu.cs.com1320.project.Stack;

public class StackImpl<T> implements Stack<T> {

    private class Node<T> {
        T value;
        Node<T> next;

        Node(T value, Node<T> next) {
            this.value = value;
            this.next = next;
        }
    }

    private Node<T> top;
    private int size;

    public StackImpl() {
        top = null;
        size = 0;
    }

    @Override
    public void push(T element) {
        Node<T> newNode = new Node<>(element, this.top);
        this.top = newNode;
        this.size++;
    }

    @Override
    public T pop() {
        if(this.top == null) {
            return null;
        }
        T value = this.top.value;
        this.top = this.top.next;
        this.size--;
        return value;
    }

    @Override
    public T peek() {
        if(this.top == null) {
            return null;
        }
        return this.top.value;
    }

    @Override
    public int size() {
        return this.size;
    }
}
