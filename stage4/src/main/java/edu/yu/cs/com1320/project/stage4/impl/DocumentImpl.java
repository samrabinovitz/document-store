package edu.yu.cs.com1320.project.stage4.impl;

import edu.yu.cs.com1320.project.HashTable;
import edu.yu.cs.com1320.project.impl.HashTableImpl;
import edu.yu.cs.com1320.project.stage4.Document;

import java.net.URI;
import java.util.*;

public class DocumentImpl implements Document {

    private URI uri;
    private String text;
    private byte[] binaryData;
    private HashTable<String,String> metadata;
    private Map<String,Integer> wordMap;

    public DocumentImpl(URI uri,String txt) {
        if(uri == null || uri.toString().isBlank()){
            throw new IllegalArgumentException("uri cannot be null or blank");
        }
        if(txt == null || txt.isBlank()){
            throw new IllegalArgumentException("txt cannot be null or blank");
        }
        this.uri = uri;
        this.text = txt;
        this.binaryData = null;
        this.metadata = new HashTableImpl<>();
        this.wordMap = buildWordMap(txt);
    }

    public DocumentImpl(URI uri, byte[] binaryData) {
        if (uri == null || uri.toString().isBlank()) {
            throw new IllegalArgumentException("uri cannot be null or blank");
        }
        if (binaryData == null || binaryData.length == 0) {
            throw new IllegalArgumentException("binaryData cannot be null or blank");
        }
        this.uri = uri;
        this.text = null;
        this.binaryData = binaryData;
        this.metadata = new HashTableImpl<>();
        this.wordMap = new HashMap<>();
    }

    @Override
    public String setMetadataValue(String key, String value) {
        if(key == null || key.isBlank()){
            throw new IllegalArgumentException("key cannot be null or blank");
        }
        return this.metadata.put(key,value);
    }

    @Override
    public String getMetadataValue(String key) {
        if(key == null || key.isBlank()){
            throw new IllegalArgumentException("key cannot be null or blank");
        }
        return this.metadata.get(key);
    }

    @Override
    public HashTable<String, String> getMetadata() {
        HashTable<String,String> copy = new HashTableImpl<>();
        for(String k : this.metadata.keySet()) {
            copy.put(k,this.metadata.get(k));
        }
        return copy;
    }

    @Override
    public String getDocumentTxt() {
        return this.text;
    }

    @Override
    public byte[] getDocumentBinaryData() {
        return this.binaryData;
    }

    @Override
    public URI getKey() {
        return this.uri;
    }

    @Override
    public int hashCode(){
        int result = uri.hashCode();
        result = 31 * result + (text != null ? text.hashCode() : 0);
        result = 31 * result + Arrays.hashCode(binaryData);
        return Math.abs(result);
    }

    @Override
    public boolean equals(Object obj){
        if(this == obj){
            return true;
        }
        if(! (obj instanceof Document)){
            return false;
        }
        DocumentImpl other = (DocumentImpl) obj;
        return this.hashCode() == other.hashCode();
    }

    @Override
    public int wordCount(String word) {
        if(word == null) {
            return 0;
        }
        String cleaned = word.replaceAll("[^a-zA-Z0-9]","");
        if(cleaned.isEmpty()) {
            return 0;
        }
        Integer count = this.wordMap.get(cleaned);
        if(count == null) {
            return 0;
        }
        return count;
    }

    @Override
    public Set<String> getWords() {
        return new HashSet<>(this.wordMap.keySet());
    }

    private Map<String,Integer> buildWordMap(String text) {
        Map<String,Integer> map = new HashMap<>();
        if(text == null || text.isBlank()) {
            return map;
        }
        String[] words = text.split("\\s+");
        for(String word : words) {
            String cleaned = word.replaceAll("[^a-zA-Z0-9]","");
            if(! cleaned.isEmpty()) {
                Integer count = map.get(cleaned);
                if(count == null) {
                    map.put(cleaned,1);
                } else {
                    map.put(cleaned, count + 1);
                }
            }
        }
        return map;
    }
}
