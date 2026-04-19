package edu.yu.cs.com1320.project.impl;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class MinHeapImplTest {

    private static class TestDoc implements Comparable<TestDoc> {
        int value;

        TestDoc(int value) {
            this.value = value;
        }

        @Override
        public int compareTo(TestDoc o) {
            return Integer.compare(this.value, o.value);
        }
    }

    @Test
    public void testInsertAndRemove() {
        MinHeapImpl<Integer> heap = new MinHeapImpl<>();

        heap.insert(5);
        heap.insert(3);
        heap.insert(7);
        heap.insert(1);

        assertEquals(1, heap.remove());
        assertEquals(3, heap.remove());
        assertEquals(5, heap.remove());
        assertEquals(7, heap.remove());
    }

    @Test
    public void testReHeapify() {
        MinHeapImpl<TestDoc> heap = new MinHeapImpl<>();

        TestDoc a = new TestDoc(10);
        TestDoc b = new TestDoc(20);
        TestDoc c = new TestDoc(30);

        heap.insert(a);
        heap.insert(b);
        heap.insert(c);

        // change priority
        c.value = 5;

        heap.reHeapify(c);

        assertEquals(c, heap.remove());
    }

    @Test
    public void testResize() {
        MinHeapImpl<Integer> heap = new MinHeapImpl<>();

        for (int i = 10; i >= 1; i--) {
            heap.insert(i);
        }

        for (int i = 1; i <= 10; i++) {
            assertEquals(i, heap.remove());
        }
    }
}
