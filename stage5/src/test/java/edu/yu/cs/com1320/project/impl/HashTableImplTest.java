package edu.yu.cs.com1320.project.impl;

import edu.yu.cs.com1320.project.HashTable;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class HashTableImplTest {

    @Test
    void testPutNewKeyReturnsNullAndGetWorks() {
        HashTable<String, Integer> ht = new HashTableImpl<>();

        assertEquals(0, ht.size());

        assertNull(ht.put("a", 1));
        assertEquals(1, ht.size());

        assertEquals(1, ht.get("a"));
        assertNull(ht.get("missing"));
    }

    @Test
    void testPutOverwriteReturnsOldValueAndUpdatesValue() {
        HashTable<String, String> ht = new HashTableImpl<>();

        assertNull(ht.put("k", "v1"));
        assertEquals("v1", ht.get("k"));
        assertEquals(1, ht.size());

        assertEquals("v1", ht.put("k", "v2"));
        assertEquals("v2", ht.get("k"));
        assertEquals(1, ht.size(), "Overwriting should not change size");
    }

    @Test
    void testDeleteUsingPutNullReturnsOldValueAndRemoves() {
        HashTable<String, String> ht = new HashTableImpl<>();

        ht.put("k", "v");
        assertEquals(1, ht.size());

        assertEquals("v", ht.put("k", null));
        assertEquals(0, ht.size());
        assertNull(ht.get("k"));
        assertFalse(ht.containsKey("k"));

        // deleting again should return null and not go negative
        assertNull(ht.put("k", null));
        assertEquals(0, ht.size());
    }

    @Test
    void testContainsKey() {
        HashTable<String, String> ht = new HashTableImpl<>();

        assertFalse(ht.containsKey("a"));
        ht.put("a", "x");
        assertTrue(ht.containsKey("a"));

        // once deleted, should be false
        ht.put("a", null);
        assertFalse(ht.containsKey("a"));
    }

    @Test
    void testContainsKeyNullThrowsNPE() {
        HashTable<String, String> ht = new HashTableImpl<>();
        assertThrows(NullPointerException.class, () -> ht.containsKey(null));
    }

    @Test
    void testPutNullKeyThrowsNPE() {
        HashTable<String, String> ht = new HashTableImpl<>();
        assertThrows(NullPointerException.class, () -> ht.put(null, "x"));
        assertThrows(NullPointerException.class, () -> ht.put(null, null));
    }

    @Test
    void testGetNullKeyBehavior() {
        HashTable<String, String> ht = new HashTableImpl<>();

        // Your current implementation returns null for get(null).
        // If your grader expects NPE, change your HashTableImpl.get to throw,
        // and change this test accordingly.
        assertNull(ht.get(null));
    }

    @Test
    void testKeySetContainsAllKeysAndIsUnmodifiable() {
        HashTable<String, Integer> ht = new HashTableImpl<>();
        ht.put("a", 1);
        ht.put("b", 2);

        Set<String> keys = ht.keySet();
        assertEquals(2, keys.size());
        assertTrue(keys.contains("a"));
        assertTrue(keys.contains("b"));

        assertThrows(UnsupportedOperationException.class, () -> keys.add("c"));
        assertThrows(UnsupportedOperationException.class, keys::clear);
    }

    @Test
    void testValuesContainsAllValuesAndIsUnmodifiable() {
        HashTable<String, String> ht = new HashTableImpl<>();
        ht.put("a", "one");
        ht.put("b", "two");

        Collection<String> vals = ht.values();
        assertEquals(2, vals.size());
        assertTrue(vals.contains("one"));
        assertTrue(vals.contains("two"));

        assertThrows(UnsupportedOperationException.class, () -> vals.add("three"));
        assertThrows(UnsupportedOperationException.class, vals::clear);
    }

    @Test
    void testKeySetAndValuesAreSnapshotsNotLiveViews() {
        HashTable<String, Integer> ht = new HashTableImpl<>();
        ht.put("a", 1);

        Set<String> keys = ht.keySet();
        Collection<Integer> vals = ht.values();

        ht.put("b", 2);

        // Since your implementation builds new collections, these should NOT update.
        assertEquals(1, keys.size());
        assertFalse(keys.contains("b"));

        assertEquals(1, vals.size());
        assertFalse(vals.contains(2));
    }

    @Test
    void testSizeWithMultipleOperations() {
        HashTable<String, String> ht = new HashTableImpl<>();
        assertEquals(0, ht.size());

        ht.put("a", "1");
        ht.put("b", "2");
        ht.put("c", "3");
        assertEquals(3, ht.size());

        ht.put("b", "20");   // overwrite
        assertEquals(3, ht.size());

        ht.put("a", null);   // delete
        assertEquals(2, ht.size());

        ht.put("missing", null); // delete non-existent
        assertEquals(2, ht.size());
    }

    @Test
    void testCollisionBehavior() {
        HashTable<Integer, String> ht = new HashTableImpl<>();

        // These often collide in small tables (capacity = 5)
        ht.put(1, "one");
        ht.put(6, "six");   // 1 % 5 == 6 % 5
        ht.put(11, "eleven");

        assertEquals("one", ht.get(1));
        assertEquals("six", ht.get(6));
        assertEquals("eleven", ht.get(11));

        assertEquals(3, ht.size());
    }
}