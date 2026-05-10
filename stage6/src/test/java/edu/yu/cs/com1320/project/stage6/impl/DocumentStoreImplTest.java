package edu.yu.cs.com1320.project.stage6.impl;

import edu.yu.cs.com1320.project.stage6.Document;
import edu.yu.cs.com1320.project.stage6.DocumentStore;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.net.URI;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class DocumentStoreImplTest {

    private ByteArrayInputStream input(String text) {
        return new ByteArrayInputStream(text.getBytes());
    }

    @Test
    public void testPutAndGetTextDocument() throws Exception {
        DocumentStore store = new DocumentStoreImpl();

        URI uri = new URI("http://edu.yu/doc1");
        store.put(input("hello world"), uri, DocumentStore.DocumentFormat.TXT);

        Document doc = store.get(uri);

        assertNotNull(doc);
        assertEquals("hello world", doc.getDocumentTxt());
        assertEquals(uri, doc.getKey());
    }

    @Test
    public void testPutReplaceReturnsOldHashCode() throws Exception {
        DocumentStore store = new DocumentStoreImpl();

        URI uri = new URI("http://edu.yu/doc1");

        store.put(input("old text"), uri, DocumentStore.DocumentFormat.TXT);
        int oldHash = store.get(uri).hashCode();

        int returnedHash = store.put(input("new text"), uri, DocumentStore.DocumentFormat.TXT);

        assertEquals(oldHash, returnedHash);
        assertEquals("new text", store.get(uri).getDocumentTxt());
    }

    @Test
    public void testDelete() throws Exception {
        DocumentStore store = new DocumentStoreImpl();

        URI uri = new URI("http://edu.yu/doc1");
        store.put(input("hello world"), uri, DocumentStore.DocumentFormat.TXT);

        assertTrue(store.delete(uri));
        assertNull(store.get(uri));
    }

    @Test
    public void testUndoDelete() throws Exception {
        DocumentStore store = new DocumentStoreImpl();

        URI uri = new URI("http://edu.yu/doc1");
        store.put(input("hello world"), uri, DocumentStore.DocumentFormat.TXT);

        store.delete(uri);
        assertNull(store.get(uri));

        store.undo();
        assertEquals("hello world", store.get(uri).getDocumentTxt());
    }

    @Test
    public void testUndoPutReplacement() throws Exception {
        DocumentStore store = new DocumentStoreImpl();

        URI uri = new URI("http://edu.yu/doc1");

        store.put(input("first"), uri, DocumentStore.DocumentFormat.TXT);
        store.put(input("second"), uri, DocumentStore.DocumentFormat.TXT);

        assertEquals("second", store.get(uri).getDocumentTxt());

        store.undo();

        assertEquals("first", store.get(uri).getDocumentTxt());
    }

    @Test
    public void testSearchKeywordSorted() throws Exception {
        DocumentStore store = new DocumentStoreImpl();

        URI uri1 = new URI("http://edu.yu/doc1");
        URI uri2 = new URI("http://edu.yu/doc2");

        store.put(input("apple apple banana"), uri1, DocumentStore.DocumentFormat.TXT);
        store.put(input("apple banana"), uri2, DocumentStore.DocumentFormat.TXT);

        List<Document> results = store.search("apple");

        assertEquals(2, results.size());
        assertEquals(uri1, results.get(0).getKey());
        assertEquals(uri2, results.get(1).getKey());
    }

    @Test
    public void testSearchByPrefixSorted() throws Exception {
        DocumentStore store = new DocumentStoreImpl();

        URI uri1 = new URI("http://edu.yu/doc1");
        URI uri2 = new URI("http://edu.yu/doc2");

        store.put(input("apple application banana"), uri1, DocumentStore.DocumentFormat.TXT);
        store.put(input("apple banana"), uri2, DocumentStore.DocumentFormat.TXT);

        List<Document> results = store.searchByPrefix("app");

        assertEquals(2, results.size());
        assertEquals(uri1, results.get(0).getKey());
        assertEquals(uri2, results.get(1).getKey());
    }

    @Test
    public void testMetadataSearch() throws Exception {
        DocumentStore store = new DocumentStoreImpl();

        URI uri1 = new URI("http://edu.yu/doc1");
        URI uri2 = new URI("http://edu.yu/doc2");

        store.put(input("hello"), uri1, DocumentStore.DocumentFormat.TXT);
        store.put(input("hello"), uri2, DocumentStore.DocumentFormat.TXT);

        store.setMetadata(uri1, "author", "sam");
        store.setMetadata(uri2, "author", "bob");

        List<Document> results = store.searchByMetadata(Map.of("author", "sam"));

        assertEquals(1, results.size());
        assertEquals(uri1, results.get(0).getKey());
    }

    @Test
    public void testDeleteAllAndUndo() throws Exception {
        DocumentStore store = new DocumentStoreImpl();

        URI uri1 = new URI("http://edu.yu/doc1");
        URI uri2 = new URI("http://edu.yu/doc2");
        URI uri3 = new URI("http://edu.yu/doc3");

        store.put(input("apple"), uri1, DocumentStore.DocumentFormat.TXT);
        store.put(input("apple"), uri2, DocumentStore.DocumentFormat.TXT);
        store.put(input("banana"), uri3, DocumentStore.DocumentFormat.TXT);

        Set<URI> deleted = store.deleteAll("apple");

        assertEquals(2, deleted.size());
        assertNull(store.get(uri1));
        assertNull(store.get(uri2));
        assertNotNull(store.get(uri3));

        store.undo();

        assertNotNull(store.get(uri1));
        assertNotNull(store.get(uri2));
        assertNotNull(store.get(uri3));
    }

    @Test
    public void testMemoryLimitMovesDocToDiskAndGetBringsBack() throws Exception {
        File tempDir = Files.createTempDirectory("stage6test").toFile();

        DocumentStore store = new DocumentStoreImpl(tempDir);

        URI uri1 = new URI("http://edu.yu/doc1");
        URI uri2 = new URI("http://edu.yu/doc2");

        store.put(input("aaaaa"), uri1, DocumentStore.DocumentFormat.TXT);
        store.put(input("bbbbb"), uri2, DocumentStore.DocumentFormat.TXT);

        store.setMaxDocumentCount(1);

        Document doc1 = store.get(uri1);
        Document doc2 = store.get(uri2);

        assertNotNull(doc1);
        assertNotNull(doc2);
    }

    @Test
    public void testUndoUriOnlyUndoesThatUri() throws Exception {
        DocumentStore store = new DocumentStoreImpl();

        URI uri1 = new URI("http://edu.yu/doc1");
        URI uri2 = new URI("http://edu.yu/doc2");

        store.put(input("first one"), uri1, DocumentStore.DocumentFormat.TXT);
        store.put(input("first two"), uri2, DocumentStore.DocumentFormat.TXT);

        store.put(input("second one"), uri1, DocumentStore.DocumentFormat.TXT);
        store.put(input("second two"), uri2, DocumentStore.DocumentFormat.TXT);

        store.undo(uri1);

        assertEquals("first one", store.get(uri1).getDocumentTxt());
        assertEquals("second two", store.get(uri2).getDocumentTxt());
    }
}

