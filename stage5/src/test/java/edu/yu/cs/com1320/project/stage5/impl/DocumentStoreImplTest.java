package edu.yu.cs.com1320.project.stage5.impl;

import edu.yu.cs.com1320.project.stage5.Document;
import edu.yu.cs.com1320.project.stage5.DocumentStore;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;

import static org.junit.jupiter.api.Assertions.*;

public class DocumentStoreImplTest {
    @Test
    public void testPutNewTextDocument() throws IOException {
        DocumentStore store = new DocumentStoreImpl();
        URI uri = URI.create("http://doc1");

        ByteArrayInputStream input = new ByteArrayInputStream("hello".getBytes());
        int result = store.put(input, uri, DocumentStore.DocumentFormat.TXT);

        assertEquals(0, result);
        assertNotNull(store.get(uri));
        assertEquals("hello", store.get(uri).getDocumentTxt());
    }

    @Test
    public void testPutReplaceDocument() throws IOException {
        DocumentStore store = new DocumentStoreImpl();
        URI uri = URI.create("http://doc1");

        store.put(new ByteArrayInputStream("first".getBytes()), uri, DocumentStore.DocumentFormat.TXT);
        Document oldDoc = store.get(uri);

        int result = store.put(new ByteArrayInputStream("second".getBytes()), uri, DocumentStore.DocumentFormat.TXT);

        assertEquals(oldDoc.hashCode(), result);
        assertEquals("second", store.get(uri).getDocumentTxt());
    }

    @Test
    public void testPutNullDeletesDocument() throws IOException {
        DocumentStore store = new DocumentStoreImpl();
        URI uri = URI.create("http://doc1");

        store.put(new ByteArrayInputStream("hello".getBytes()), uri, DocumentStore.DocumentFormat.TXT);
        Document oldDoc = store.get(uri);

        int result = store.put(null, uri, DocumentStore.DocumentFormat.TXT);

        assertEquals(oldDoc.hashCode(), result);
        assertNull(store.get(uri));
    }

    @Test
    public void testUndoPutNewDocument() throws IOException {
        DocumentStore store = new DocumentStoreImpl();
        URI uri = URI.create("http://doc1");

        store.put(new ByteArrayInputStream("hello".getBytes()), uri, DocumentStore.DocumentFormat.TXT);
        assertNotNull(store.get(uri));

        store.undo();

        assertNull(store.get(uri));
    }

    @Test
    public void testUndoReplaceDocument() throws IOException {
        DocumentStore store = new DocumentStoreImpl();
        URI uri = URI.create("http://doc1");

        store.put(new ByteArrayInputStream("first".getBytes()), uri, DocumentStore.DocumentFormat.TXT);
        store.put(new ByteArrayInputStream("second".getBytes()), uri, DocumentStore.DocumentFormat.TXT);

        assertEquals("second", store.get(uri).getDocumentTxt());

        store.undo();

        assertEquals("first", store.get(uri).getDocumentTxt());
    }

    @Test
    public void testUndoDeleteThroughPutNull() throws IOException {
        DocumentStore store = new DocumentStoreImpl();
        URI uri = URI.create("http://doc1");

        store.put(new ByteArrayInputStream("hello".getBytes()), uri, DocumentStore.DocumentFormat.TXT);
        store.put(null, uri, DocumentStore.DocumentFormat.TXT);

        assertNull(store.get(uri));

        store.undo();

        assertNotNull(store.get(uri));
        assertEquals("hello", store.get(uri).getDocumentTxt());
    }

    @Test
    public void testSetMaxDocumentCountEvictsLeastRecentlyUsed() throws IOException {
        DocumentStore store = new DocumentStoreImpl();
        URI uri1 = URI.create("http://doc1");
        URI uri2 = URI.create("http://doc2");

        store.put(new ByteArrayInputStream("one".getBytes()), uri1, DocumentStore.DocumentFormat.TXT);
        store.put(new ByteArrayInputStream("two".getBytes()), uri2, DocumentStore.DocumentFormat.TXT);

        store.setMaxDocumentCount(1);

        int docsLeft = 0;
        if (store.get(uri1) != null) docsLeft++;
        if (store.get(uri2) != null) docsLeft++;

        assertEquals(1, docsLeft);
    }

    @Test
    public void testSetMaxDocumentBytesEvictsDocument() throws IOException {
        DocumentStore store = new DocumentStoreImpl();
        URI uri1 = URI.create("http://doc1");
        URI uri2 = URI.create("http://doc2");

        store.put(new ByteArrayInputStream("12345".getBytes()), uri1, DocumentStore.DocumentFormat.TXT);
        store.put(new ByteArrayInputStream("67890".getBytes()), uri2, DocumentStore.DocumentFormat.TXT);

        store.setMaxDocumentBytes(5);

        int docsLeft = 0;
        if (store.get(uri1) != null) docsLeft++;
        if (store.get(uri2) != null) docsLeft++;

        assertEquals(1, docsLeft);
    }

    @Test
    public void testDocumentTooLargeThrowsException() {
        DocumentStore store = new DocumentStoreImpl();
        URI uri = URI.create("http://doc1");

        store.setMaxDocumentBytes(3);

        assertThrows(IllegalArgumentException.class, () -> {
            store.put(new ByteArrayInputStream("hello".getBytes()), uri, DocumentStore.DocumentFormat.TXT);
        });
    }

    @Test
    public void testBinaryDocumentPut() throws IOException {
        DocumentStore store = new DocumentStoreImpl();
        URI uri = URI.create("http://binary");

        byte[] data = {1, 2, 3, 4};

        int result = store.put(new ByteArrayInputStream(data), uri, DocumentStore.DocumentFormat.BINARY);

        assertEquals(0, result);
        assertNotNull(store.get(uri));
        assertArrayEquals(data, store.get(uri).getDocumentBinaryData());
    }
}
