package edu.yu.cs.com1320.project.stage3.impl;

import edu.yu.cs.com1320.project.stage3.Document;
import edu.yu.cs.com1320.project.stage3.DocumentStore;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertThrows;
import static org.testng.AssertJUnit.assertNull;

class DocumentStoreImplTest{
    @Test
    void testPutNewTextDocReturnsZeroAndGetWorks() throws Exception {
        DocumentStore store = new DocumentStoreImpl();
        URI uri = new URI("http://a.com");

        byte[] bytes = "hello".getBytes(StandardCharsets.UTF_8);
        int oldHash = store.put(new ByteArrayInputStream(bytes), uri, DocumentStore.DocumentFormat.TXT);

        assertEquals(0, oldHash);

        Document doc = store.get(uri);
        assertNotNull(doc);
        assertEquals(uri, doc.getKey());
        assertEquals("hello", doc.getDocumentTxt());
        assertNull(doc.getDocumentBinaryData());
    }

    @Test
    void testPutReplacesAndReturnsOldHashCode() throws Exception {
        DocumentStore store = new DocumentStoreImpl();
        URI uri = new URI("http://replace.com");

        store.put(new ByteArrayInputStream("one".getBytes(StandardCharsets.UTF_8)), uri, DocumentStore.DocumentFormat.TXT);
        int expectedOldHash = store.get(uri).hashCode();

        int returned = store.put(new ByteArrayInputStream("two".getBytes(StandardCharsets.UTF_8)), uri, DocumentStore.DocumentFormat.TXT);

        assertEquals(expectedOldHash, returned);
        assertEquals("two", store.get(uri).getDocumentTxt());
    }

    @Test
    void testPutBinaryDocAndGetWorks() throws Exception {
        DocumentStore store = new DocumentStoreImpl();
        URI uri = new URI("http://bin.com");

        byte[] data = {1, 2, 3, 4};
        int oldHash = store.put(new ByteArrayInputStream(data), uri, DocumentStore.DocumentFormat.BINARY);

        assertEquals(0, oldHash);

        Document doc = store.get(uri);
        assertNotNull(doc);
        assertNull(doc.getDocumentTxt());
        assertArrayEquals(data, doc.getDocumentBinaryData());
    }

    @Test
    void testDeleteReturnsTrueAndGetIsNull() throws Exception {
        DocumentStore store = new DocumentStoreImpl();
        URI uri = new URI("http://del.com");

        store.put(new ByteArrayInputStream("hi".getBytes(StandardCharsets.UTF_8)), uri, DocumentStore.DocumentFormat.TXT);
        assertNotNull(store.get(uri));

        assertTrue(store.delete(uri));
        assertNull(store.get(uri));
        assertFalse(store.delete(uri)); // already gone
    }

    @Test
    void testPutNullInputActsLikeDeleteAndReturnsDeletedHash() throws Exception {
        DocumentStore store = new DocumentStoreImpl();
        URI uri = new URI("http://nulldelete.com");

        store.put(new ByteArrayInputStream("bye".getBytes(StandardCharsets.UTF_8)), uri, DocumentStore.DocumentFormat.TXT);
        int deletedHash = store.get(uri).hashCode();

        int returned = store.put(null, uri, DocumentStore.DocumentFormat.TXT);

        assertEquals(deletedHash, returned);
        assertNull(store.get(uri));

        // if nothing to delete, should return 0
        assertEquals(0, store.put(null, uri, DocumentStore.DocumentFormat.TXT));
    }

    @Test
    void testSetAndGetMetadataThroughStore() throws Exception {
        DocumentStore store = new DocumentStoreImpl();
        URI uri = new URI("http://meta-store.com");

        store.put(new ByteArrayInputStream("doc".getBytes(StandardCharsets.UTF_8)), uri, DocumentStore.DocumentFormat.TXT);

        assertNull(store.setMetadata(uri, "author", "sam"));
        assertEquals("sam", store.getMetadata(uri, "author"));

        assertEquals("sam", store.setMetadata(uri, "author", "david"));
        assertEquals("david", store.getMetadata(uri, "author"));
    }

    @Test
    void testSetMetadataThrowsIfNoDocAtUri() throws Exception {
        DocumentStore store = new DocumentStoreImpl();
        URI uri = new URI("http://missing.com");

        assertThrows(IllegalArgumentException.class, () -> store.setMetadata(uri, "k", "v"));
        assertThrows(IllegalArgumentException.class, () -> store.getMetadata(uri, "k"));
    }

    @Test
    void testIllegalArgumentsForPut() throws Exception {
        DocumentStore store = new DocumentStoreImpl();
        URI uri = new URI("http://ok.com");

        assertThrows(IllegalArgumentException.class, () ->
                store.put(new ByteArrayInputStream("x".getBytes(StandardCharsets.UTF_8)), null, DocumentStore.DocumentFormat.TXT));

        assertThrows(IllegalArgumentException.class, () ->
                store.put(new ByteArrayInputStream("x".getBytes(StandardCharsets.UTF_8)), new URI(""), DocumentStore.DocumentFormat.TXT));

        assertThrows(IllegalArgumentException.class, () ->
                store.put(new ByteArrayInputStream("x".getBytes(StandardCharsets.UTF_8)), uri, null));
    }

    @Test
    void testIllegalArgumentsForMetadataKeys() throws Exception {
        DocumentStore store = new DocumentStoreImpl();
        URI uri = new URI("http://meta-illegal.com");

        store.put(new ByteArrayInputStream("doc".getBytes(StandardCharsets.UTF_8)), uri, DocumentStore.DocumentFormat.TXT);

        assertThrows(IllegalArgumentException.class, () -> store.setMetadata(uri, null, "v"));
        assertThrows(IllegalArgumentException.class, () -> store.setMetadata(uri, "   ", "v"));

        assertThrows(IllegalArgumentException.class, () -> store.getMetadata(uri, null));
        assertThrows(IllegalArgumentException.class, () -> store.getMetadata(uri, "   "));
    }

    @Test
    void testUndoPutNewDocumentRemovesIt() throws Exception {
        DocumentStore store = new DocumentStoreImpl();
        URI uri = new URI("http://undo-put-new.com");

        store.put(new ByteArrayInputStream("hello".getBytes(StandardCharsets.UTF_8)), uri, DocumentStore.DocumentFormat.TXT);
        assertNotNull(store.get(uri));

        store.undo();

        assertNull(store.get(uri));
    }

    @Test
    void testUndoPutReplaceRestoresOldDocument() throws Exception {
        DocumentStore store = new DocumentStoreImpl();
        URI uri = new URI("http://undo-replace.com");

        store.put(new ByteArrayInputStream("one".getBytes(StandardCharsets.UTF_8)), uri, DocumentStore.DocumentFormat.TXT);
        store.put(new ByteArrayInputStream("two".getBytes(StandardCharsets.UTF_8)), uri, DocumentStore.DocumentFormat.TXT);

        assertEquals("two", store.get(uri).getDocumentTxt());

        store.undo();

        assertEquals("one", store.get(uri).getDocumentTxt());
    }

    @Test
    void testUndoDeleteRestoresDocument() throws Exception {
        DocumentStore store = new DocumentStoreImpl();
        URI uri = new URI("http://undo-delete.com");

        store.put(new ByteArrayInputStream("bye".getBytes(StandardCharsets.UTF_8)), uri, DocumentStore.DocumentFormat.TXT);
        store.delete(uri);

        assertNull(store.get(uri));

        store.undo();

        assertEquals("bye", store.get(uri).getDocumentTxt());
    }

    @Test
    void testUndoSetMetadataRestoresOldValue() throws Exception {
        DocumentStore store = new DocumentStoreImpl();
        URI uri = new URI("http://undo-meta.com");

        store.put(new ByteArrayInputStream("doc".getBytes(StandardCharsets.UTF_8)), uri, DocumentStore.DocumentFormat.TXT);

        store.setMetadata(uri, "author", "sam");
        store.setMetadata(uri, "author", "david");

        assertEquals("david", store.getMetadata(uri, "author"));

        store.undo();

        assertEquals("sam", store.getMetadata(uri, "author"));
    }

    @Test
    void testUndoUriUndoesMostRecentCommandForThatUri() throws Exception {
        DocumentStore store = new DocumentStoreImpl();

        URI uri1 = new URI("http://doc1.com");
        URI uri2 = new URI("http://doc2.com");

        store.put(new ByteArrayInputStream("one".getBytes(StandardCharsets.UTF_8)), uri1, DocumentStore.DocumentFormat.TXT);
        store.put(new ByteArrayInputStream("two".getBytes(StandardCharsets.UTF_8)), uri2, DocumentStore.DocumentFormat.TXT);
        store.put(new ByteArrayInputStream("ONE-NEW".getBytes(StandardCharsets.UTF_8)), uri1, DocumentStore.DocumentFormat.TXT);

        store.undo(uri1);

        assertEquals("one", store.get(uri1).getDocumentTxt());
        assertEquals("two", store.get(uri2).getDocumentTxt());
    }

    @Test
    void testUndoOnEmptyStackThrowsException() {
        DocumentStore store = new DocumentStoreImpl();

        assertThrows(IllegalStateException.class, store::undo);
    }

    @Test
    void testUndoUriThrowsExceptionWhenNoCommandForUri() throws Exception {
        DocumentStore store = new DocumentStoreImpl();

        URI uri1 = new URI("http://exists.com");
        URI uri2 = new URI("http://missing.com");

        store.put(new ByteArrayInputStream("doc".getBytes(StandardCharsets.UTF_8)), uri1, DocumentStore.DocumentFormat.TXT);

        assertThrows(IllegalStateException.class, () -> store.undo(uri2));
    }
}