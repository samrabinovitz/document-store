package edu.yu.cs.com1320.project.stage1.impl;

import org.junit.jupiter.api.Test;
import java.net.URI;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertThrows;
import static org.testng.AssertJUnit.assertNull;

class DocumentImplTest {
    @Test
    void testTextConstructor() throws Exception {
        URI uri = new URI("http://test.com");
        DocumentImpl doc = new DocumentImpl(uri,"hello");
        assertEquals(uri,doc.getKey());
        assertEquals("hello",doc.getDocumentTxt());
        assertNull(doc.getDocumentBinaryData());
    }

    @Test
    void testBinaryConstructor() throws Exception{
        URI uri = new URI("http://test.com");
        byte[] data = {1,2,3};
        DocumentImpl doc = new DocumentImpl(uri,data);
        assertEquals(uri,doc.getKey());
        assertNull(doc.getDocumentTxt());
        assertArrayEquals(data,doc.getDocumentBinaryData());
    }

    @Test
    void testSetAndGetMetadataValue() throws Exception{
        URI uri = new URI("http://nhl.com");
        DocumentImpl doc = new DocumentImpl(uri,"text");
        assertNull(doc.setMetadataValue("author","sam"));
        assertEquals("sam",doc.getMetadataValue("author"));
        assertEquals("sam",doc.setMetadataValue("author","david"));
        assertEquals("david",doc.getMetadataValue("author"));
    }

    @Test
    void testHashMap() throws Exception{
        URI uri = new URI("http://nhl.com");
        DocumentImpl doc = new DocumentImpl(uri,"text");
        doc.setMetadataValue("key","value");
        HashMap<String,String> metadataCopy = doc.getMetadata();
        metadataCopy.put("newkey","newvalue");
        assertNull(doc.getMetadataValue("newkey"));
    }

    @Test
    void testEqualsAndHAshCode() throws Exception{
        URI uri = new URI("http://nhl.com");
        DocumentImpl doc1 = new DocumentImpl(uri,"text");
        DocumentImpl doc2 = new DocumentImpl(uri,"text");
        assertEquals(doc1.hashCode(),doc2.hashCode());
        assertEquals(doc1,doc2);
    }
    @Test
    void testIllegalArguments() throws Exception{
        URI uri = new URI("http://nhl.com");
        assertThrows(IllegalArgumentException.class,() -> new DocumentImpl(null, "text"));
        assertThrows(IllegalArgumentException.class,() -> new DocumentImpl(uri,(String)null));
        assertThrows(IllegalArgumentException.class,() -> new DocumentImpl(null, new byte[]{}));
    }
}
