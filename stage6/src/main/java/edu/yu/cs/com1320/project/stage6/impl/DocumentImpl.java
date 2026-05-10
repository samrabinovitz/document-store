package edu.yu.cs.com1320.project.stage6.impl;

import edu.yu.cs.com1320.project.stage6.Document;

import java.net.URI;
import java.util.*;

public class DocumentImpl implements Document {
    private URI uri;
    private String text;
    private byte[] binaryData;
    private long lastUseTime;
    private HashMap<String,String> metadata;
    private HashMap<String,Integer> wordMap;

    public DocumentImpl(URI uri, String txt, Map<String,Integer> wordCountMap) {
        if(uri == null || uri.toString().isBlank()){
            throw new IllegalArgumentException("uri cannot be null or blank");
        }
        this.uri = uri;
        this.text = txt;
        this.binaryData = null;
        this.metadata = new HashMap<>();
        if(wordCountMap == null) {
            this.wordMap = (HashMap<String, Integer>) buildWordMap(txt);
        } else {
            this.wordMap = new HashMap<>(wordCountMap);
        }
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
        this.metadata = new HashMap<>();
        this.wordMap = new HashMap<>();
    }
    @Override
    public String setMetadataValue(String key, String value) {
        if(key == null || key.isBlank()) {
            throw new IllegalArgumentException("key cannot be null or blank");
        }
        return this.metadata.put(key,value);
    }

    @Override
    public String getMetadataValue(String key) {
        if(key == null || key.isBlank()) {
            throw new IllegalArgumentException("key cannot be null or blank");
        }
        return this.metadata.get(key);
    }

    @Override
    public HashMap<String, String> getMetadata() {
        return new HashMap<>(this.metadata);
    }

    @Override
    public void setMetadata(HashMap<String, String> metadata) {
        if(metadata == null) {
            this.metadata = new HashMap<>();
        } else {
            this.metadata = new HashMap<>(metadata);
        }
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

    @Override
    public long getLastUseTime() {
        return this.lastUseTime;
    }

    @Override
    public void setLastUseTime(long timeInNanoseconds) {
        this.lastUseTime = timeInNanoseconds;
    }

    @Override
    public HashMap<String, Integer> getWordMap() {
        return new HashMap<>(this.wordMap);
    }

    @Override
    public void setWordMap(HashMap<String, Integer> wordMap) {
        if(wordMap == null) {
            this.wordMap = new HashMap<>();
        } else {
            this.wordMap = new HashMap<>(wordMap);
        }
    }

    @Override
    public int compareTo(Document o) {
        if(o == null) {
            throw new IllegalArgumentException("no other document");
        }
        return Long.compare(this.lastUseTime, o.getLastUseTime());
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
        Document other = (DocumentImpl) obj;
        return this.hashCode() == other.hashCode();
    }

    private Map<String,Integer> buildWordMap(String text) {
        HashMap<String,Integer> map = new HashMap<>();
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
