package edu.yu.cs.com1320.project.impl;

import org.junit.jupiter.api.Test;

import java.util.Comparator;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class TrieImplTest {

    @Test
    public void testPutAndGet() {
        TrieImpl<String> trie = new TrieImpl<>();

        trie.put("cat", "doc1");
        trie.put("cat", "doc2");

        Set<String> result = trie.get("cat");

        assertEquals(2, result.size());
        assertTrue(result.contains("doc1"));
        assertTrue(result.contains("doc2"));
    }

    @Test
    public void testGetWithNothingThere() {
        TrieImpl<String> trie = new TrieImpl<>();

        Set<String> result = trie.get("cat");

        assertNotNull(result);
        assertEquals(0, result.size());
    }

    @Test
    public void testGetSorted() {
        TrieImpl<String> trie = new TrieImpl<>();

        trie.put("cat", "a");
        trie.put("cat", "bbb");
        trie.put("cat", "cc");

        Comparator<String> comp = (s1, s2) -> s2.length() - s1.length();

        List<String> result = trie.getSorted("cat", comp);

        assertEquals(3, result.size());
        assertEquals("bbb", result.get(0));
        assertEquals("cc", result.get(1));
        assertEquals("a", result.get(2));
    }

    @Test
    public void testPrefixSearch() {
        TrieImpl<String> trie = new TrieImpl<>();

        trie.put("to", "doc1");
        trie.put("top", "doc2");
        trie.put("tool", "doc3");
        trie.put("dog", "doc4");

        Comparator<String> comp = (s1, s2) -> s1.compareTo(s2);

        List<String> result = trie.getAllWithPrefixSorted("to", comp);

        assertEquals(3, result.size());
        assertTrue(result.contains("doc1"));
        assertTrue(result.contains("doc2"));
        assertTrue(result.contains("doc3"));
        assertFalse(result.contains("doc4"));
    }

    @Test
    public void testPrefixSearchNoMatches() {
        TrieImpl<String> trie = new TrieImpl<>();

        trie.put("cat", "doc1");
        trie.put("dog", "doc2");

        Comparator<String> comp = (s1, s2) -> s1.compareTo(s2);

        List<String> result = trie.getAllWithPrefixSorted("z", comp);

        assertNotNull(result);
        assertEquals(0, result.size());
    }

    @Test
    public void testDeleteSingleValue() {
        TrieImpl<String> trie = new TrieImpl<>();

        trie.put("cat", "doc1");
        trie.put("cat", "doc2");

        String deleted = trie.delete("cat", "doc1");
        Set<String> result = trie.get("cat");

        assertEquals("doc1", deleted);
        assertEquals(1, result.size());
        assertFalse(result.contains("doc1"));
        assertTrue(result.contains("doc2"));
    }

    @Test
    public void testDeleteSingleValueThatIsNotThere() {
        TrieImpl<String> trie = new TrieImpl<>();

        trie.put("cat", "doc1");

        String deleted = trie.delete("cat", "doc999");

        assertNull(deleted);
    }

    @Test
    public void testDeleteAll() {
        TrieImpl<String> trie = new TrieImpl<>();

        trie.put("cat", "doc1");
        trie.put("cat", "doc2");
        trie.put("car", "doc3");

        Set<String> deleted = trie.deleteAll("cat");

        assertEquals(2, deleted.size());
        assertTrue(deleted.contains("doc1"));
        assertTrue(deleted.contains("doc2"));

        Set<String> catResult = trie.get("cat");
        Set<String> carResult = trie.get("car");

        assertEquals(0, catResult.size());
        assertEquals(1, carResult.size());
        assertTrue(carResult.contains("doc3"));
    }

    @Test
    public void testDeleteAllWithPrefix() {
        TrieImpl<String> trie = new TrieImpl<>();

        trie.put("to", "doc1");
        trie.put("top", "doc2");
        trie.put("tool", "doc3");
        trie.put("cat", "doc4");

        Set<String> deleted = trie.deleteAllWithPrefix("to");

        assertEquals(3, deleted.size());
        assertTrue(deleted.contains("doc1"));
        assertTrue(deleted.contains("doc2"));
        assertTrue(deleted.contains("doc3"));

        List<String> result = trie.getAllWithPrefixSorted("to", (a, b) -> a.compareTo(b));
        assertEquals(0, result.size());

        Set<String> catResult = trie.get("cat");
        assertEquals(1, catResult.size());
        assertTrue(catResult.contains("doc4"));
    }

    @Test
    public void testDeleteAllWithPrefixEmptyString() {
        TrieImpl<String> trie = new TrieImpl<>();

        trie.put("cat", "doc1");
        trie.put("dog", "doc2");
        trie.put("apple", "doc3");

        Set<String> deleted = trie.deleteAllWithPrefix("");

        assertEquals(3, deleted.size());
        assertTrue(deleted.contains("doc1"));
        assertTrue(deleted.contains("doc2"));
        assertTrue(deleted.contains("doc3"));

        assertEquals(0, trie.get("cat").size());
        assertEquals(0, trie.get("dog").size());
        assertEquals(0, trie.get("apple").size());
    }

    @Test
    public void testDuplicatePutDoesNotDuplicateValue() {
        TrieImpl<String> trie = new TrieImpl<>();

        trie.put("cat", "doc1");
        trie.put("cat", "doc1");

        Set<String> result = trie.get("cat");

        assertEquals(1, result.size());
        assertTrue(result.contains("doc1"));
    }

    @Test
    public void testCaseSensitive() {
        TrieImpl<String> trie = new TrieImpl<>();

        trie.put("Cat", "doc1");
        trie.put("cat", "doc2");

        Set<String> upper = trie.get("Cat");
        Set<String> lower = trie.get("cat");

        assertEquals(1, upper.size());
        assertEquals(1, lower.size());
        assertTrue(upper.contains("doc1"));
        assertTrue(lower.contains("doc2"));
    }
}
