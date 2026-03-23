package edu.yu.cs.com1320.project.stage4.impl;

import edu.yu.cs.com1320.project.stage4.Document;
import edu.yu.cs.com1320.project.stage4.DocumentStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class DocumentStoreImplTest {

    private DocumentStore store;
    private URI uri1;
    private URI uri2;
    private URI uri3;

    @BeforeEach
    public void setUp() throws Exception {
        this.store = new DocumentStoreImpl();
        this.uri1 = new URI("http://doc1");
        this.uri2 = new URI("http://doc2");
        this.uri3 = new URI("http://doc3");
    }

    private ByteArrayInputStream text(String s) {
        return new ByteArrayInputStream(s.getBytes());
    }

    @Test
    public void testPutAndGet() throws IOException {
        int result = this.store.put(text("hello world"), this.uri1, DocumentStore.DocumentFormat.TXT);

        assertEquals(0, result);

        Document doc = this.store.get(this.uri1);
        assertNotNull(doc);
        assertEquals("hello world", doc.getDocumentTxt());
    }

    @Test
    public void testDelete() throws IOException {
        this.store.put(text("hello world"), this.uri1, DocumentStore.DocumentFormat.TXT);

        assertTrue(this.store.delete(this.uri1));
        assertNull(this.store.get(this.uri1));
    }

    @Test
    public void testUndoDelete() throws IOException {
        this.store.put(text("hello world"), this.uri1, DocumentStore.DocumentFormat.TXT);
        this.store.delete(this.uri1);

        this.store.undo();

        assertNotNull(this.store.get(this.uri1));
        assertEquals("hello world", this.store.get(this.uri1).getDocumentTxt());
    }

    @Test
    public void testUndoPut() throws IOException {
        this.store.put(text("hello world"), this.uri1, DocumentStore.DocumentFormat.TXT);

        this.store.undo();

        assertNull(this.store.get(this.uri1));
    }

    @Test
    public void testSearch() throws IOException {
        this.store.put(text("apple apple banana"), this.uri1, DocumentStore.DocumentFormat.TXT);
        this.store.put(text("apple banana"), this.uri2, DocumentStore.DocumentFormat.TXT);

        List<Document> results = this.store.search("apple");

        assertEquals(2, results.size());
        assertEquals(this.uri1, results.get(0).getKey());
        assertEquals(this.uri2, results.get(1).getKey());
    }

    @Test
    public void testSearchByPrefix() throws IOException {
        this.store.put(text("app apple application"), this.uri1, DocumentStore.DocumentFormat.TXT);
        this.store.put(text("apple"), this.uri2, DocumentStore.DocumentFormat.TXT);

        List<Document> results = this.store.searchByPrefix("app");

        assertEquals(2, results.size());
        assertEquals(this.uri1, results.get(0).getKey());
    }

    @Test
    public void testSetAndGetMetadata() throws IOException {
        this.store.put(text("hello world"), this.uri1, DocumentStore.DocumentFormat.TXT);

        assertNull(this.store.setMetadata(this.uri1, "author", "sam"));
        assertEquals("sam", this.store.getMetadata(this.uri1, "author"));
    }

    @Test
    public void testSearchByMetadata() throws IOException {
        this.store.put(text("hello world"), this.uri1, DocumentStore.DocumentFormat.TXT);
        this.store.put(text("apple banana"), this.uri2, DocumentStore.DocumentFormat.TXT);

        this.store.setMetadata(this.uri1, "type", "notes");
        this.store.setMetadata(this.uri2, "type", "hw");

        HashMap<String, String> map = new HashMap<>();
        map.put("type", "notes");

        List<Document> results = this.store.searchByMetadata(map);

        assertEquals(1, results.size());
        assertEquals(this.uri1, results.get(0).getKey());
    }

    @Test
    public void testSearchByKeywordAndMetadata() throws IOException {
        this.store.put(text("apple apple"), this.uri1, DocumentStore.DocumentFormat.TXT);
        this.store.put(text("apple"), this.uri2, DocumentStore.DocumentFormat.TXT);

        this.store.setMetadata(this.uri1, "group", "A");
        this.store.setMetadata(this.uri2, "group", "B");

        HashMap<String, String> map = new HashMap<>();
        map.put("group", "A");

        List<Document> results = this.store.searchByKeywordAndMetadata("apple", map);

        assertEquals(1, results.size());
        assertEquals(this.uri1, results.get(0).getKey());
    }

    @Test
    public void testDeleteAll() throws IOException {
        this.store.put(text("apple apple"), this.uri1, DocumentStore.DocumentFormat.TXT);
        this.store.put(text("apple"), this.uri2, DocumentStore.DocumentFormat.TXT);
        this.store.put(text("banana"), this.uri3, DocumentStore.DocumentFormat.TXT);

        Set<URI> deleted = this.store.deleteAll("apple");

        assertEquals(2, deleted.size());
        assertNull(this.store.get(this.uri1));
        assertNull(this.store.get(this.uri2));
        assertNotNull(this.store.get(this.uri3));
    }

    @Test
    public void testUndoDeleteAll() throws IOException {
        this.store.put(text("apple apple"), this.uri1, DocumentStore.DocumentFormat.TXT);
        this.store.put(text("apple"), this.uri2, DocumentStore.DocumentFormat.TXT);

        this.store.deleteAll("apple");
        this.store.undo();

        assertNotNull(this.store.get(this.uri1));
        assertNotNull(this.store.get(this.uri2));
    }

    @Test
    public void testDeleteAllWithMetadata() throws IOException {
        this.store.put(text("apple"), this.uri1, DocumentStore.DocumentFormat.TXT);
        this.store.put(text("apple"), this.uri2, DocumentStore.DocumentFormat.TXT);

        this.store.setMetadata(this.uri1, "type", "keep");
        this.store.setMetadata(this.uri2, "type", "delete");

        HashMap<String, String> map = new HashMap<>();
        map.put("type", "delete");

        Set<URI> deleted = this.store.deleteAllWithMetadata(map);

        assertEquals(1, deleted.size());
        assertNull(this.store.get(this.uri2));
        assertNotNull(this.store.get(this.uri1));
    }

    @Test
    public void testUndoByUri() throws IOException {
        this.store.put(text("apple"), this.uri1, DocumentStore.DocumentFormat.TXT);
        this.store.put(text("banana"), this.uri2, DocumentStore.DocumentFormat.TXT);

        this.store.delete(this.uri1);
        this.store.delete(this.uri2);

        this.store.undo(this.uri1);

        assertNotNull(this.store.get(this.uri1));
        assertNull(this.store.get(this.uri2));
    }
}
