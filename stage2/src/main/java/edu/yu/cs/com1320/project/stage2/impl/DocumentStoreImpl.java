package edu.yu.cs.com1320.project.stage2.impl;

import edu.yu.cs.com1320.project.HashTable;
import edu.yu.cs.com1320.project.impl.HashTableImpl;
import edu.yu.cs.com1320.project.stage2.Document;
import edu.yu.cs.com1320.project.stage2.DocumentStore;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

public class DocumentStoreImpl implements DocumentStore {
    private HashTable<URI,Document> store;

    public DocumentStoreImpl(){
        this.store = new HashTableImpl<>();
    }

    @Override
    public String setMetadata(URI uri, String key, String value) {
        if(uri == null || uri.toString().isBlank()) {
            throw new IllegalArgumentException("uri cannot be null or blank");
        }
        if(key == null || key.isBlank()){
            throw new IllegalArgumentException("key cannot be null or blank");
        }
        Document doc = this.store.get(uri);
        if(doc == null){
            throw new IllegalArgumentException("no document stored with that url");
        }
        return doc.setMetadataValue(key,value);
    }

    @Override
    public String getMetadata(URI uri, String key) {
        if(uri == null || uri.toString().isBlank()) {
            throw new IllegalArgumentException("uri cannot be null or blank");
        }
        if(key == null || key.isBlank()){
            throw new IllegalArgumentException("key cannot be null or blank");
        }
        Document doc = this.store.get(uri);
        if(doc == null){
            throw new IllegalArgumentException("no document stored with that url");
        }
        return doc.getMetadataValue(key);
    }

    @Override
    public int put(InputStream input, URI url, DocumentFormat format) throws IOException {
        if(url == null || url.toString().isBlank()){
            throw new IllegalArgumentException("uri cannot be null or blank");
        }
        if(format == null){
            throw new IllegalArgumentException("format cannot be null");
        }
        if(input == null){
            Document old = this.store.put(url,null);
            if(old == null){
                return 0;
            }
            return old.hashCode();
        }
        byte[] bytes = input.readAllBytes();
        Document newDoc;
        if(format == DocumentFormat.TXT){
            String backToText = new String(bytes);
            newDoc = new DocumentImpl(url,backToText);
        }else{
            newDoc = new DocumentImpl(url,bytes);
        }
        Document old = this.store.put(url,newDoc);
        return (old == null) ? 0 : old.hashCode();
    }

    @Override
    public Document get(URI url) {
        if(url == null || url.toString().isBlank()){
            return null;
        }
        return this.store.get(url);
    }

    @Override
    public boolean delete(URI url) {
        if(url == null || url.toString().isBlank()){
            return false;
        }
        return this.store.put(url,null) != null;
    }
}
