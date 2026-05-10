package edu.yu.cs.com1320.project.stage6.impl;

import edu.yu.cs.com1320.project.stage6.Document;
import org.junit.jupiter.api.Test;
import java.io.File;
import java.net.URI;
import static org.junit.jupiter.api.Assertions.*;

public class DocumentPersistenceManagerTest {

    @Test
    public void testSerializeDeserializeText() throws Exception {
        File baseDir = new File("testDir");
        DocumentPersistenceManager pm = new DocumentPersistenceManager(baseDir);
        URI uri = new URI("http://test/doc1");
        Document doc = new DocumentImpl(uri, "hello world hello", null);
        doc.setMetadataValue("author", "sam");
        pm.serialize(uri, doc);
        Document loaded = pm.deserialize(uri);
        assertNotNull(loaded);
        assertEquals(uri, loaded.getKey());
        assertEquals("hello world hello", loaded.getDocumentTxt());
        assertEquals("sam", loaded.getMetadataValue("author"));
        assertEquals(2, loaded.wordCount("hello"));
    }

    @Test
    public void testSerializeDeserializeBinary() throws Exception {
        File baseDir = new File("testDir");
        DocumentPersistenceManager pm = new DocumentPersistenceManager(baseDir);
        URI uri = new URI("http://test/doc2");
        byte[] data = {1, 2, 3, 4};
        Document doc = new DocumentImpl(uri, data);
        pm.serialize(uri, doc);
        Document loaded = pm.deserialize(uri);
        assertNotNull(loaded);
        assertArrayEquals(data, loaded.getDocumentBinaryData());
    }

    @Test
    public void testDelete() throws Exception {
        File baseDir = new File("testDir");
        DocumentPersistenceManager pm = new DocumentPersistenceManager(baseDir);
        URI uri = new URI("http://test/doc3");
        Document doc = new DocumentImpl(uri, "delete me", null);
        pm.serialize(uri, doc);
        boolean deleted = pm.delete(uri);
        assertTrue(deleted);
        Document loaded = pm.deserialize(uri);
        assertNull(loaded);
    }
}