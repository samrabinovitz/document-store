package edu.yu.cs.com1320.project.stage5.impl;

import edu.yu.cs.com1320.project.HashTable;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class DocumentImplTest {

    @Test
    public void testTextDocumentConstructorAndGetters() throws Exception {
        URI uri = new URI("http://yu.edu/doc1");
        DocumentImpl doc = new DocumentImpl(uri, "hello world hello");

        assertEquals(uri, doc.getKey());
        assertEquals("hello world hello", doc.getDocumentTxt());
        assertNull(doc.getDocumentBinaryData());
    }

    @Test
    public void testBinaryDocumentConstructorAndGetters() throws Exception {
        URI uri = new URI("http://yu.edu/doc2");
        byte[] data = {1, 2, 3, 4};
        DocumentImpl doc = new DocumentImpl(uri, data);

        assertEquals(uri, doc.getKey());
        assertArrayEquals(data, doc.getDocumentBinaryData());
        assertNull(doc.getDocumentTxt());
    }

    @Test
    public void testTextConstructorThrowsIfUriNull() {
        assertThrows(IllegalArgumentException.class, () -> {
            new DocumentImpl(null, "some text");
        });
    }

    @Test
    public void testTextConstructorThrowsIfUriBlank() {
        assertThrows(IllegalArgumentException.class, () -> {
            new DocumentImpl(URI.create(""), "some text");
        });
    }

    @Test
    public void testTextConstructorThrowsIfTextNull() throws Exception {
        URI uri = new URI("http://yu.edu/doc3");

        assertThrows(IllegalArgumentException.class, () -> {
            new DocumentImpl(uri, (String) null);
        });
    }

    @Test
    public void testTextConstructorThrowsIfTextBlank() throws Exception {
        URI uri = new URI("http://yu.edu/doc4");

        assertThrows(IllegalArgumentException.class, () -> {
            new DocumentImpl(uri, "   ");
        });
    }

    @Test
    public void testBinaryConstructorThrowsIfUriNull() {
        byte[] data = {1, 2};

        assertThrows(IllegalArgumentException.class, () -> {
            new DocumentImpl(null, data);
        });
    }

    @Test
    public void testBinaryConstructorThrowsIfBinaryNull() throws Exception {
        URI uri = new URI("http://yu.edu/doc5");

        assertThrows(IllegalArgumentException.class, () -> {
            new DocumentImpl(uri, (byte[]) null);
        });
    }

    @Test
    public void testBinaryConstructorThrowsIfBinaryEmpty() throws Exception {
        URI uri = new URI("http://yu.edu/doc6");

        assertThrows(IllegalArgumentException.class, () -> {
            new DocumentImpl(uri, new byte[0]);
        });
    }

    @Test
    public void testSetAndGetMetadataValue() throws Exception {
        URI uri = new URI("http://yu.edu/doc7");
        DocumentImpl doc = new DocumentImpl(uri, "hello");

        String old = doc.setMetadataValue("author", "sam");
        assertNull(old);

        assertEquals("sam", doc.getMetadataValue("author"));

        String old2 = doc.setMetadataValue("author", "joe");
        assertEquals("sam", old2);
        assertEquals("joe", doc.getMetadataValue("author"));
    }

    @Test
    public void testSetMetadataThrowsOnBadKey() throws Exception {
        URI uri = new URI("http://yu.edu/doc8");
        DocumentImpl doc = new DocumentImpl(uri, "hello");

        assertThrows(IllegalArgumentException.class, () -> {
            doc.setMetadataValue(null, "x");
        });

        assertThrows(IllegalArgumentException.class, () -> {
            doc.setMetadataValue("   ", "x");
        });
    }

    @Test
    public void testGetMetadataThrowsOnBadKey() throws Exception {
        URI uri = new URI("http://yu.edu/doc9");
        DocumentImpl doc = new DocumentImpl(uri, "hello");

        assertThrows(IllegalArgumentException.class, () -> {
            doc.getMetadataValue(null);
        });

        assertThrows(IllegalArgumentException.class, () -> {
            doc.getMetadataValue("   ");
        });
    }

    @Test
    public void testGetMetadataReturnsCopy() throws Exception {
        URI uri = new URI("http://yu.edu/doc10");
        DocumentImpl doc = new DocumentImpl(uri, "hello");

        doc.setMetadataValue("a", "1");
        doc.setMetadataValue("b", "2");

        HashTable<String, String> copy = doc.getMetadata();
        assertEquals("1", copy.get("a"));
        assertEquals("2", copy.get("b"));

        copy.put("a", "999");

        assertEquals("1", doc.getMetadataValue("a"));
    }

    @Test
    public void testWordCountBasic() throws Exception {
        URI uri = new URI("http://yu.edu/doc11");
        DocumentImpl doc = new DocumentImpl(uri, "cat dog cat bird cat");

        assertEquals(3, doc.wordCount("cat"));
        assertEquals(1, doc.wordCount("dog"));
        assertEquals(1, doc.wordCount("bird"));
        assertEquals(0, doc.wordCount("fish"));
    }

    @Test
    public void testWordCountIgnoresPunctuation() throws Exception {
        URI uri = new URI("http://yu.edu/doc12");
        DocumentImpl doc = new DocumentImpl(uri, "cat, cat! cat? dog.");

        assertEquals(3, doc.wordCount("cat"));
        assertEquals(3, doc.wordCount("cat!!!"));
        assertEquals(1, doc.wordCount("dog"));
        assertEquals(1, doc.wordCount("dog..."));
    }

    @Test
    public void testWordCountCaseSensitive() throws Exception {
        URI uri = new URI("http://yu.edu/doc13");
        DocumentImpl doc = new DocumentImpl(uri, "Cat cat CAT cAt");

        assertEquals(1, doc.wordCount("Cat"));
        assertEquals(1, doc.wordCount("cat"));
        assertEquals(1, doc.wordCount("CAT"));
        assertEquals(1, doc.wordCount("cAt"));
    }

    @Test
    public void testWordCountNullAndOnlyPunctuation() throws Exception {
        URI uri = new URI("http://yu.edu/doc14");
        DocumentImpl doc = new DocumentImpl(uri, "hello hello");

        assertEquals(0, doc.wordCount(null));
        assertEquals(0, doc.wordCount("!!!"));
    }

    @Test
    public void testGetWords() throws Exception {
        URI uri = new URI("http://yu.edu/doc15");
        DocumentImpl doc = new DocumentImpl(uri, "apple banana apple car!");

        Set<String> words = doc.getWords();

        assertEquals(3, words.size());
        assertTrue(words.contains("apple"));
        assertTrue(words.contains("banana"));
        assertTrue(words.contains("car"));
    }

    @Test
    public void testGetWordsBinaryDocIsEmpty() throws Exception {
        URI uri = new URI("http://yu.edu/doc16");
        byte[] data = {9, 8, 7};
        DocumentImpl doc = new DocumentImpl(uri, data);

        Set<String> words = doc.getWords();

        assertNotNull(words);
        assertEquals(0, words.size());
    }

    @Test
    public void testEqualsSameData() throws Exception {
        URI uri1 = new URI("http://yu.edu/doc17");
        URI uri2 = new URI("http://yu.edu/doc17");

        DocumentImpl doc1 = new DocumentImpl(uri1, "hello");
        DocumentImpl doc2 = new DocumentImpl(uri2, "hello");

        assertEquals(doc1, doc2);
    }

    @Test
    public void testNotEqualsDifferentDocs() throws Exception {
        DocumentImpl doc1 = new DocumentImpl(new URI("http://yu.edu/doc18"), "hello");
        DocumentImpl doc2 = new DocumentImpl(new URI("http://yu.edu/doc19"), "hello");

        assertNotEquals(doc1, doc2);
    }

    @Test
    public void testHashCodeSameForEqualDocs() throws Exception {
        URI uri1 = new URI("http://yu.edu/doc20");
        URI uri2 = new URI("http://yu.edu/doc20");

        DocumentImpl doc1 = new DocumentImpl(uri1, "abc");
        DocumentImpl doc2 = new DocumentImpl(uri2, "abc");

        assertEquals(doc1.hashCode(), doc2.hashCode());
    }

    @Test
    public void testBinaryDocsEqualIfSameUriAndBytes() throws Exception {
        URI uri1 = new URI("http://yu.edu/doc21");
        URI uri2 = new URI("http://yu.edu/doc21");

        byte[] a = {1, 2, 3};
        byte[] b = {1, 2, 3};

        DocumentImpl doc1 = new DocumentImpl(uri1, a);
        DocumentImpl doc2 = new DocumentImpl(uri2, b);

        assertEquals(doc1, doc2);
    }

    @Test
    public void testGetAndSetLastUseTime() throws Exception {
        DocumentImpl doc = new DocumentImpl(new URI("http://yu.edu/doc1"), "hello world");

        doc.setLastUseTime(12345L);

        assertEquals(12345L, doc.getLastUseTime());
    }

    @Test
    public void testCompareToSmallerTime() throws Exception {
        DocumentImpl doc1 = new DocumentImpl(new URI("http://yu.edu/doc1"), "hello");
        DocumentImpl doc2 = new DocumentImpl(new URI("http://yu.edu/doc2"), "world");

        doc1.setLastUseTime(100L);
        doc2.setLastUseTime(200L);

        assertTrue(doc1.compareTo(doc2) < 0);
    }

    @Test
    public void testCompareToGreaterTime() throws Exception {
        DocumentImpl doc1 = new DocumentImpl(new URI("http://yu.edu/doc1"), "hello");
        DocumentImpl doc2 = new DocumentImpl(new URI("http://yu.edu/doc2"), "world");

        doc1.setLastUseTime(300L);
        doc2.setLastUseTime(200L);

        assertTrue(doc1.compareTo(doc2) > 0);
    }

    @Test
    public void testCompareToEqualTime() throws Exception {
        DocumentImpl doc1 = new DocumentImpl(new URI("http://yu.edu/doc1"), "hello");
        DocumentImpl doc2 = new DocumentImpl(new URI("http://yu.edu/doc2"), "world");

        doc1.setLastUseTime(500L);
        doc2.setLastUseTime(500L);

        assertEquals(0, doc1.compareTo(doc2));
    }

}
