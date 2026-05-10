package edu.yu.cs.com1320.project.stage6.impl;

import edu.yu.cs.com1320.project.BTree;
import edu.yu.cs.com1320.project.MinHeap;
import edu.yu.cs.com1320.project.Trie;
import edu.yu.cs.com1320.project.impl.BTreeImpl;
import edu.yu.cs.com1320.project.impl.MinHeapImpl;
import edu.yu.cs.com1320.project.impl.StackImpl;
import edu.yu.cs.com1320.project.impl.TrieImpl;
import edu.yu.cs.com1320.project.stage6.Document;
import edu.yu.cs.com1320.project.stage6.DocumentStore;
import edu.yu.cs.com1320.project.undo.CommandSet;
import edu.yu.cs.com1320.project.undo.GenericCommand;
import edu.yu.cs.com1320.project.undo.Undoable;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.*;

public class DocumentStoreImpl implements DocumentStore {
    private BTree<URI, Document> store;
    private StackImpl<Undoable> commandStack;
    private Trie<URI> trie;
    private MinHeap<HeapNode> minHeap;
    private Map<URI,HeapNode> uriToHeap;
    private Map<URI,Integer> uriToBytes;
    private int maxDocumentCount = Integer.MAX_VALUE;
    private int maxDocumentBytes = Integer.MAX_VALUE;
    private int currentDocumentCount = 0;
    private int currentDocumentBytes = 0;

    private class HeapNode implements Comparable<HeapNode> {
        private URI uri;
        private long lastUseTime;

        private HeapNode(URI uri, long lastUseTime) {
            this.uri = uri;
            this.lastUseTime = lastUseTime;
        }

        @Override
        public int compareTo(HeapNode o) {
            return Long.compare(this.lastUseTime,o.lastUseTime);
        }
    }

    public DocumentStoreImpl() {
        this(null);
    }

    public DocumentStoreImpl(File baseDir) {
        this.store = new BTreeImpl<>();
        this.store.setPersistenceManager(new DocumentPersistenceManager(baseDir));
        this.commandStack = new StackImpl<>();
        this.trie = new TrieImpl<>();
        this.minHeap = new MinHeapImpl<>();
        this.uriToHeap = new HashMap<>();
        this.uriToBytes = new HashMap<>();
    }
    @Override
    public String setMetadata(URI uri, String key, String value) throws IOException {
        if(uri == null || uri.toString().isBlank()) {
            throw new IllegalArgumentException("uri cannot be null or blank");
        }
        if(key == null || key.isBlank()) {
            throw new IllegalArgumentException("key cannot be null or blank");
        }
        Document doc = this.store.get(uri);
        if(doc == null) {
            throw new IllegalArgumentException("no document at that uri");
        }
        String oldValue = doc.getMetadataValue(key);
        doc.setMetadataValue(key,value);
        updateLastUseTime(doc);
        GenericCommand<URI> command = new GenericCommand<>(uri, u-> {
            try {
                Document undoDoc = this.store.get(u);
                if(undoDoc != null) {
                    undoDoc.setMetadataValue(key,oldValue);
                    updateLastUseTime(undoDoc);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
        this.commandStack.push(command);
        return oldValue;
    }

    @Override
    public String getMetadata(URI uri, String key) throws IOException {
        if(uri == null || uri.toString().isBlank()) {
            throw new IllegalArgumentException("uri cannot be null or blank");
        }
        if(key == null || key.isBlank()) {
            throw new IllegalArgumentException("key cannot be null or blank");
        }
        Document doc = this.store.get(uri);
        if(doc == null) {
            throw new IllegalArgumentException("no document stored at that uri");
        }
        updateLastUseTime(doc);
        return doc.getMetadataValue(key);
    }

    private Document makeDocument(InputStream input, URI uri, DocumentFormat format) throws IOException {
        byte[] bytes = input.readAllBytes();
        if(format == DocumentFormat.TXT) {
            return new DocumentImpl(uri, new String(bytes), null);
        }
        return new DocumentImpl(uri,bytes);
    }

    private void removeFromMemory(URI uri) {
        if(!isInMemory(uri)) {
            return;
        }
        HeapNode node = this.uriToHeap.get(uri);
        node.lastUseTime = Long.MIN_VALUE;
        this.minHeap.reHeapify(node);
        this.minHeap.remove();
        this.currentDocumentCount--;
        this.currentDocumentBytes -= this.uriToBytes.get(uri);
        this.uriToHeap.remove(uri);
    }

    private void removeDoc(URI uri) throws IOException {
        Document doc = this.store.get(uri);
        if(doc == null) {
            return;
        }
        removeDocFromTrie(doc);
        removeFromMemory(uri);
        this.uriToBytes.remove(uri);
        this.store.put(uri,null);
    }

    private void putDocIn(Document doc) throws IOException {
        doc.setLastUseTime(System.nanoTime());
        this.store.put(doc.getKey(),doc);
        addDocToTrie(doc);
        addToMemory(doc);
        int bytes = getDocumentBytes(doc);
        this.uriToBytes.put(doc.getKey(),bytes);
        if(bytes > this.maxDocumentBytes) {
            this.store.moveToDisk(doc.getKey());
            return;
        }
        addToMemory(doc);
    }

    @Override
    public int put(InputStream input, URI url, DocumentFormat format) throws IOException {
        if(url == null || url.toString().isBlank() || format == null) {
            throw new IllegalArgumentException("uri or format cannot be null or blank");
        }
        if(input == null) {
            Document oldDoc = this.store.get(url);
            int oldHash = oldDoc == null ? 0 : oldDoc.hashCode();
            delete(url);
            return oldHash;
        }
        Document newDoc = makeDocument(input, url, format);
        Document oldDoc = this.store.get(url);
        int oldHash = oldDoc == null ? 0 : oldDoc.hashCode();
        removeDoc(url);
        putDocIn(newDoc);
        GenericCommand<URI> command = new GenericCommand<>(url,u -> {
            try {
                removeDoc(u);
                if(oldDoc != null) {
                    putDocIn(oldDoc);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
        this.commandStack.push(command);
        return oldHash;
    }

    private boolean isInMemory(URI uri) {
        return this.uriToHeap.containsKey(uri);
    }

    private int getDocumentBytes(Document doc) {
        if (doc.getDocumentTxt() != null) {
            return doc.getDocumentTxt().getBytes().length;
        }
        return doc.getDocumentBinaryData().length;
    }

    @Override
    public Document get(URI url) throws IOException {
        if(url == null || url.toString().isBlank()) {
            throw new IllegalArgumentException("uri cannot be null or blank");
        }
        Document doc = this.store.get(url);
        if(doc == null) {
            return null;
        }
        updateLastUseTime(doc);
        return doc;
    }

    private void addDocToTrie(Document doc) {
        if(doc == null || doc.getDocumentTxt() == null) {
            return;
        }
        for(String word : doc.getWords()) {
            this.trie.put(word,doc.getKey());
        }
    }

    private void removeDocFromTrie(Document doc) {
        if(doc == null || doc.getDocumentTxt() == null) {
            return;
        }
        for(String word : doc.getWords()) {
            this.trie.delete(word,doc.getKey());
        }
    }

    private void completelyDeleteDoc(URI uri) throws IOException {
        Document doc = this.store.get(uri);
        if(doc == null) {
            return;
        }
        removeDocFromTrie(doc);
        removeFromMemory(uri);
        this.uriToBytes.remove(uri);
        this.store.put(uri,null);
    }

    private void addToMemory(Document doc) throws IOException {
        URI uri = doc.getKey();
        if(isInMemory(uri)) {
            return;
        }
        int bytes = getDocumentBytes(doc);
        this.currentDocumentCount++;
        this.currentDocumentBytes += bytes;
        this.uriToBytes.put(uri,bytes);
        HeapNode node = new HeapNode(uri,doc.getLastUseTime());
        this.uriToHeap.put(uri,node);
        this.minHeap.insert(node);
        enforceMemoryLimits();
    }

    private void enforceMemoryLimits() throws IOException {
        while(this.currentDocumentCount > this.maxDocumentCount || this.currentDocumentBytes > this.maxDocumentBytes) {
            HeapNode node = this.minHeap.remove();
            URI uri = node.uri;
            int bytes = this.uriToBytes.get(uri);
            this.store.moveToDisk(uri);
            this.uriToHeap.remove(uri);
            this.currentDocumentCount--;
            this.currentDocumentBytes -= bytes;
        }
    }

    @Override
    public boolean delete(URI url) {
        if(url == null || url.toString().isBlank()) {
            throw new IllegalArgumentException("uri cannot be null or blank");
        }
        Document oldDoc;
        oldDoc = this.store.get(url);
        if(oldDoc == null) {
            return false;
        }
        try {
            completelyDeleteDoc(url);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        GenericCommand<URI> command = new GenericCommand<>(url, u -> {
            try {
                this.store.put(u,oldDoc);
                addDocToTrie(oldDoc);
                oldDoc.setLastUseTime(System.nanoTime());
                addToMemory(oldDoc);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
        this.commandStack.push(command);
        return true;
    }

    @Override
    public void undo() throws IllegalStateException {
        if(this.commandStack.size() == 0) {
            throw new IllegalStateException("nothing to undo");
        }
        Undoable command = this.commandStack.pop();
        command.undo();
    }

    @Override
    public void undo(URI url) throws IllegalStateException {
        if(url == null || url.toString().isBlank()) {
            throw new IllegalArgumentException("uri cannot be null or blank");
        }
        StackImpl<Undoable> tempStack = new StackImpl<>();
        boolean undone = false;
        while(this.commandStack.size() > 0 && !undone) {
            Undoable current = this.commandStack.pop();
            if(current instanceof GenericCommand<?>) {
                GenericCommand<URI> gc = (GenericCommand<URI>) current;
                if(gc.getTarget().equals(url)) {
                    gc.undo();
                    undone = true;
                } else {
                    tempStack.push(current);
                }
            } else if (current instanceof CommandSet<?>) {
                CommandSet<URI> cs = (CommandSet<URI>) current;
                if(cs.containsTarget(url)) {
                    cs.undo(url);
                    if(cs.size() > 0) {
                        this.commandStack.push(cs);
                    }
                    undone = true;
                } else {
                    tempStack.push(current);
                }
            }
        }
        while (tempStack.size() > 0) {
            this.commandStack.push(tempStack.pop());
        }
        if(!undone) {
            throw new IllegalStateException("nothing to be undone");
        }
    }

    @Override
    public List<Document> search(String keyword) throws IOException {
        Comparator<URI> comparator = (uri1,uri2) -> {
            Document doc1 = this.store.get(uri1);
            Document doc2 = this.store.get(uri2);
            return Integer.compare(
                    doc2.wordCount(keyword),
                    doc1.wordCount(keyword)
            );
        };
        List<URI> uris = this.trie.getSorted(keyword,comparator);
        List<Document> docs = new ArrayList<>();
        long now = System.nanoTime();
        for(URI uri : uris) {
            Document doc = this.store.get(uri);
            doc.setLastUseTime(now);
            if(isInMemory(uri)) {
                HeapNode node = this.uriToHeap.get(uri);
                node.lastUseTime = now;
                this.minHeap.reHeapify(node);
            } else {
                addToMemory(doc);
            }
            docs.add(doc);
        }
        enforceMemoryLimits();
        return docs;
    }

    private int prefixCount(Document doc, String prefix) {
        int count = 0;
        for(String word : doc.getWords()) {
            if(word.startsWith(prefix)) {
                count += doc.wordCount(word);
            }
        }
        return count;
    }

    @Override
    public List<Document> searchByPrefix(String keywordPrefix) throws IOException {
        Comparator<URI> comparator = (uri1,uri2) -> {
            Document doc1 = this.store.get(uri1);
            Document doc2 = this.store.get(uri2);
            return Integer.compare(
                    prefixCount(doc2,keywordPrefix),
                    prefixCount(doc1,keywordPrefix)
            );
        };
        List<URI> uris = this.trie.getAllWithPrefixSorted(keywordPrefix,comparator);
        List<Document> docs = new ArrayList<>();
        long now = System.nanoTime();
        for(URI uri : uris) {
            Document doc = this.store.get(uri);
            doc.setLastUseTime(now);
            if(isInMemory(uri)) {
                HeapNode node = this.uriToHeap.get(uri);
                node.lastUseTime = now;
                this.minHeap.reHeapify(node);
            } else {
                addToMemory(doc);
            }
            docs.add(doc);
        }
        enforceMemoryLimits();
        return docs;
    }

    @Override
    public Set<URI> deleteAll(String keyword) {
        Set<URI> uris = this.trie.deleteAll(keyword);
        CommandSet<URI> commandSet = new CommandSet<>();
        for(URI uri : uris) {
            Document deletedDoc;
            try {
                deletedDoc = this.store.get(uri);
                if(deletedDoc == null) {
                    continue;
                }
                completelyDeleteDoc(uri);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            GenericCommand<URI> command = new GenericCommand<>(uri, u-> {
                try {
                    this.store.put(u,deletedDoc);
                    addDocToTrie(deletedDoc);
                    deletedDoc.setLastUseTime(System.nanoTime());
                    addToMemory(deletedDoc);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
            commandSet.addCommand(command);
        }
        if(!uris.isEmpty()) {
            this.commandStack.push(commandSet);
        }
        return uris;
    }

    @Override
    public Set<URI> deleteAllWithPrefix(String keywordPrefix) {
        Set<URI> uris = this.trie.deleteAllWithPrefix(keywordPrefix);
        CommandSet<URI> commandSet = new CommandSet<>();
        for(URI uri : uris) {
            Document deletedDoc;
            try {
                deletedDoc = this.store.get(uri);
                if(deletedDoc == null) {
                    continue;
                }
                completelyDeleteDoc(uri);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            GenericCommand<URI> command = new GenericCommand<>(uri, u -> {
                try {
                    this.store.put(u,deletedDoc);
                        addDocToTrie(deletedDoc);
                        deletedDoc.setLastUseTime(System.nanoTime());
                        addToMemory(deletedDoc);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
            });
            commandSet.addCommand(command);
        }
        if(!uris.isEmpty()) {
            this.commandStack.push(commandSet);
        }
        return uris;
    }

    private boolean matchesMetadata(Document doc, Map<String,String> keysValues) {
        if(doc == null || keysValues == null || keysValues.isEmpty()) {
            return false;
        }
        for(String key : keysValues.keySet()) {
            String requiredValue = keysValues.get(key);
            String actualValue = doc.getMetadataValue(key);
            if(!requiredValue.equals(actualValue)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public List<Document> searchByMetadata(Map<String, String> keysValues) throws IOException {
        List<Document> matches = new ArrayList<>();
        Set<URI> allUris = new HashSet<>(this.uriToBytes.keySet());
        for(URI uri : allUris) {
            Document doc = this.store.get(uri);
            if(matchesMetadata(doc,keysValues)) {
                updateLastUseTime(doc);
                matches.add(doc);
            }
        }
        return matches;
    }

    @Override
    public List<Document> searchByKeywordAndMetadata(String keyword, Map<String, String> keysValues) throws IOException {
        List<Document> keywordMatches = search(keyword);
        List<Document> matches = new ArrayList<>();
        for(Document doc : keywordMatches) {
            if(matchesMetadata(doc,keysValues)) {
                matches.add(doc);
            }
        }
        return matches;
    }

    @Override
    public List<Document> searchByPrefixAndMetadata(String keywordPrefix, Map<String, String> keysValues) throws IOException {
        List<Document> keywordMatches = searchByPrefix(keywordPrefix);
        List<Document> matches = new ArrayList<>();
        for(Document doc : keywordMatches) {
            if(matchesMetadata(doc,keysValues)) {
                matches.add(doc);
            }
        }
        return matches;
    }

    @Override
    public Set<URI> deleteAllWithMetadata(Map<String, String> keysValues) throws IOException {
        Set<URI> deletedUris = new HashSet<>();
        CommandSet<URI> commandSet = new CommandSet<>();
        Set<URI> allUris = new HashSet<>(this.uriToBytes.keySet());
        for(URI uri : allUris) {
            Document doc = this.store.get(uri);
            if(doc != null && matchesMetadata(doc,keysValues)) {
                deletedUris.add((uri));
                completelyDeleteDoc(uri);
                GenericCommand<URI> command = new GenericCommand<>(uri,u -> {
                    try {
                        this.store.put(u,doc);
                        addDocToTrie(doc);
                        doc.setLastUseTime(System.nanoTime());
                        addToMemory(doc);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
                commandSet.addCommand(command);
            }
        }
        if(!deletedUris.isEmpty()) {
            this.commandStack.push(commandSet);
        }
        return deletedUris;
    }

    @Override
    public Set<URI> deleteAllWithKeywordAndMetadata(String keyword, Map<String, String> keysValues) throws IOException {
        List<Document> docsToDelete = searchByKeywordAndMetadata(keyword,keysValues);
        Set<URI> deletedUris = new HashSet<>();
        CommandSet<URI> commandSet = new CommandSet<>();
        for(Document doc : docsToDelete) {
            URI uri = doc.getKey();
            deletedUris.add(uri);
            completelyDeleteDoc(uri);
            GenericCommand<URI> command = new GenericCommand<>(uri,u -> {
                try {
                    this.store.put(u,doc);
                    addDocToTrie(doc);
                    doc.setLastUseTime(System.nanoTime());
                    addToMemory(doc);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
            commandSet.addCommand(command);
        }
        if(!deletedUris.isEmpty()) {
            commandStack.push(commandSet);
        }
        return deletedUris;
    }

    @Override
    public Set<URI> deleteAllWithPrefixAndMetadata(String keywordPrefix, Map<String, String> keysValues) throws IOException {
        List<Document> docsToDelete = searchByPrefixAndMetadata(keywordPrefix,keysValues);
        Set<URI> deletedUris = new HashSet<>();
        CommandSet<URI> commandSet = new CommandSet<>();
        for(Document doc : docsToDelete) {
            URI uri = doc.getKey();
            deletedUris.add(uri);
            completelyDeleteDoc(uri);
            GenericCommand<URI> command = new GenericCommand<>(uri,u -> {
                try {
                    this.store.put(u,doc);
                    addDocToTrie(doc);
                    doc.setLastUseTime(System.nanoTime());
                    addToMemory(doc);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
            commandSet.addCommand(command);
        }
        if(!deletedUris.isEmpty()) {
            commandStack.push(commandSet);
        }
        return deletedUris;
    }

    @Override
    public void setMaxDocumentCount(int limit) {
        if (limit < 1) {
            throw new IllegalArgumentException("limit must be more than 1");
        }
        this.maxDocumentCount = limit;
        try {
            enforceMemoryLimits();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void setMaxDocumentBytes(int limit) {
        if (limit < 1) {
            throw new IllegalArgumentException("limit must be more than 1");
        }
        this.maxDocumentBytes = limit;
        try {
            enforceMemoryLimits();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void updateLastUseTime(Document doc) throws IOException {
        long now = System.nanoTime();
        doc.setLastUseTime(now);
        URI uri = doc.getKey();
        if(isInMemory(uri)) {
            HeapNode node = this.uriToHeap.get(uri);
            node.lastUseTime = doc.getLastUseTime();
            this.minHeap.reHeapify(node);
        } else {
            addToMemory(doc);
        }
        enforceMemoryLimits();
    }
}
