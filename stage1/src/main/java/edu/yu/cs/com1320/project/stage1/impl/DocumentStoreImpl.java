package edu.yu.cs.com1320.project.stage1.impl;

import edu.yu.cs.com1320.project.stage1.Document;
import edu.yu.cs.com1320.project.stage1.DocumentStore;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.HashMap;

public class DocumentStoreImpl implements DocumentStore {
    private HashMap<URI,Document> store;

    public DocumentStoreImpl(){
        this.store = new HashMap<>();
    }

    @Override
    public String setMetadata(URI uri, String key, String value) {
        if(uri == null || uri.toString().isBlank()){
            throw new IllegalArgumentException("uri cannot be null or blank");
        }
        if(key == null || key.isBlank()){
            throw new IllegalArgumentException("key cannot be null or blank");
        }
        Document doc = this.store.get(uri);
        if(doc == null){
            throw new IllegalArgumentException("no document stored at that uri");
        }
        return doc.setMetadataValue(key,value);
    }

    @Override
    public String getMetadata(URI uri, String key) {
        if(uri == null || uri.toString().isBlank()){
            throw new IllegalArgumentException("uri cannot be null or blank");
        }
        if(key == null || key.isBlank()){
            throw new IllegalArgumentException("key cannot be null or blank");
        }
        Document doc = this.store.get(uri);
        if(doc == null){
            throw new IllegalArgumentException("no document stored at that uri");
        }
        return doc.getMetadataValue(key);
    }

    @Override
    public int put(InputStream input, URI uri, DocumentFormat format) throws IOException {
        if(uri == null || uri.toString().isBlank()){
            throw new IllegalArgumentException("uri cannot be null or blank");
        }
        if(format == null){
            throw new IllegalArgumentException("format cannot be null");
        }
        if(input == null){
            Document old = this.store.remove(uri);
            if(old == null){
                return 0;
            }
            return old.hashCode();
        }
        byte[] bytes = input.readAllBytes();
        Document newDoc;
        if(format == DocumentFormat.TXT){
           String backToText = new String(bytes);
           newDoc = new DocumentImpl(uri,backToText);
        }else{
            newDoc = new DocumentImpl(uri,bytes);
        }
        Document old = this.store.put(uri,newDoc);
        return (old == null) ? 0 : old.hashCode();
    }

    @Override
    public Document get(URI url) {
        return this.store.get(url);
    }

    @Override
    public boolean delete(URI url) {
        return this.store.remove(url) != null;
    }
}

