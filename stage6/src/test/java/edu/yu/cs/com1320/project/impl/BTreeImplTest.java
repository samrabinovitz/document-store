package edu.yu.cs.com1320.project.impl;

import edu.yu.cs.com1320.project.stage6.PersistenceManager;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class BTreeImplTest {

    private static class TestPM<Key, Value> implements PersistenceManager<Key, Value> {
        private Map<Key, Value> disk = new HashMap<>();

        @Override
        public void serialize(Key k, Value val) throws IOException {
            this.disk.put(k, val);
        }

        @Override
        public Value deserialize(Key k) throws IOException {
            return this.disk.get(k);
        }

        @Override
        public boolean delete(Key k) throws IOException {
            return this.disk.remove(k) != null;
        }
    }

    @Test
    public void testPutAndGetOneValue() {
        BTreeImpl<Integer, String> btree = new BTreeImpl<>();
        assertNull(btree.put(1, "one"));
        assertEquals("one", btree.get(1));
    }

    @Test
    public void testGetMissingKeyReturnsNull() {
        BTreeImpl<Integer, String> btree = new BTreeImpl<>();
        assertNull(btree.get(99));
    }

    @Test
    public void testPutReturnsOldValueWhenReplacing() {
        BTreeImpl<Integer, String> btree = new BTreeImpl<>();
        assertNull(btree.put(1, "one"));
        assertEquals("one", btree.put(1, "ONE"));
        assertEquals("ONE", btree.get(1));
    }

    @Test
    public void testManyPutsCauseSplitsAndStillWork() {
        BTreeImpl<Integer, String> btree = new BTreeImpl<>();
        for (int i = 0; i < 100; i++) {
            assertNull(btree.put(i, "value" + i));
        }
        for (int i = 0; i < 100; i++) {
            assertEquals("value" + i, btree.get(i));
        }
    }

    @Test
    public void testPutOutOfOrder() {
        BTreeImpl<Integer, String> btree = new BTreeImpl<>();
        btree.put(50, "fifty");
        btree.put(10, "ten");
        btree.put(30, "thirty");
        btree.put(20, "twenty");
        btree.put(40, "forty");
        assertEquals("ten", btree.get(10));
        assertEquals("twenty", btree.get(20));
        assertEquals("thirty", btree.get(30));
        assertEquals("forty", btree.get(40));
        assertEquals("fifty", btree.get(50));
    }

    @Test
    public void testNullKeyPutThrowsException() {
        BTreeImpl<Integer, String> btree = new BTreeImpl<>();
        assertThrows(IllegalArgumentException.class, () -> {
            btree.put(null, "bad");
        });
    }

    @Test
    public void testNullKeyGetThrowsException() {
        BTreeImpl<Integer, String> btree = new BTreeImpl<>();
        assertThrows(IllegalArgumentException.class, () -> {
            btree.get(null);
        });
    }

    @Test
    public void testSetNullPersistenceManagerThrowsException() {
        BTreeImpl<Integer, String> btree = new BTreeImpl<>();
        assertThrows(IllegalArgumentException.class, () -> {
            btree.setPersistenceManager(null);
        });
    }

    @Test
    public void testMoveToDiskWithoutPersistenceManagerThrowsException() {
        BTreeImpl<Integer, String> btree = new BTreeImpl<>();
        btree.put(1, "one");
        assertThrows(IllegalStateException.class, () -> {
            btree.moveToDisk(1);
        });
    }

    @Test
    public void testMoveToDiskThenGetBringsValueBack() throws IOException {
        BTreeImpl<Integer, String> btree = new BTreeImpl<>();
        TestPM<Integer, String> pm = new TestPM<>();
        btree.setPersistenceManager(pm);
        btree.put(1, "one");
        btree.moveToDisk(1);
        assertEquals("one", btree.get(1));
    }

    @Test
    public void testMoveToDiskMissingKeyDoesNothing() throws IOException {
        BTreeImpl<Integer, String> btree = new BTreeImpl<>();
        TestPM<Integer, String> pm = new TestPM<>();
        btree.setPersistenceManager(pm);
        assertDoesNotThrow(() -> {
            btree.moveToDisk(999);
        });
        assertNull(btree.get(999));
    }

    @Test
    public void testMoveToDiskNullKeyThrowsException() {
        BTreeImpl<Integer, String> btree = new BTreeImpl<>();
        assertThrows(IllegalArgumentException.class, () -> {
            btree.moveToDisk(null);
        });
    }

    @Test
    public void testPutReplacesValueThatWasMovedToDisk() throws IOException {
        BTreeImpl<Integer, String> btree = new BTreeImpl<>();
        TestPM<Integer, String> pm = new TestPM<>();
        btree.setPersistenceManager(pm);
        btree.put(1, "one");
        btree.moveToDisk(1);
        assertEquals("one", btree.put(1, "ONE"));
        assertEquals("ONE", btree.get(1));
    }

    @Test
    public void testManyValuesMovedToDiskAndRetrieved() throws IOException {
        BTreeImpl<Integer, String> btree = new BTreeImpl<>();
        TestPM<Integer, String> pm = new TestPM<>();
        btree.setPersistenceManager(pm);
        for (int i = 0; i < 50; i++) {
            btree.put(i, "value" + i);
        }
        for (int i = 0; i < 50; i += 2) {
            btree.moveToDisk(i);
        }
        for (int i = 0; i < 50; i++) {
            assertEquals("value" + i, btree.get(i));
        }
    }
}
