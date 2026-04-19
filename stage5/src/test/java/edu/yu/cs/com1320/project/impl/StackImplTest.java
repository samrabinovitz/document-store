package edu.yu.cs.com1320.project.impl;

import edu.yu.cs.com1320.project.Stack;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class StackImplTest {

    @Test
    void testPushAndSize() {
        Stack<Integer> stack = new StackImpl<>();

        stack.push(10);
        stack.push(20);

        assertEquals(2, stack.size());
    }

    @Test
    void testPeekReturnsTopWithoutRemoving() {
        Stack<Integer> stack = new StackImpl<>();

        stack.push(5);
        stack.push(8);

        assertEquals(8, stack.peek());
        assertEquals(2, stack.size());
    }

    @Test
    void testPopReturnsTopAndRemovesIt() {
        Stack<Integer> stack = new StackImpl<>();

        stack.push(1);
        stack.push(2);
        stack.push(3);

        assertEquals(3, stack.pop());
        assertEquals(2, stack.size());
        assertEquals(2, stack.peek());
    }

    @Test
    void testPopOnEmptyStackReturnsNull() {
        Stack<Integer> stack = new StackImpl<>();

        assertNull(stack.pop());
    }

    @Test
    void testPeekOnEmptyStackReturnsNull() {
        Stack<Integer> stack = new StackImpl<>();

        assertNull(stack.peek());
    }
}