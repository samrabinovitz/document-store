package edu.yu.cs.com1320.project.stage5.impl;

import edu.yu.cs.com1320.project.HashTable;
import edu.yu.cs.com1320.project.MinHeap;
import edu.yu.cs.com1320.project.Stack;
import edu.yu.cs.com1320.project.Trie;
import edu.yu.cs.com1320.project.impl.HashTableImpl;
import edu.yu.cs.com1320.project.impl.MinHeapImpl;
import edu.yu.cs.com1320.project.impl.StackImpl;
import edu.yu.cs.com1320.project.impl.TrieImpl;
import edu.yu.cs.com1320.project.stage5.Document;
import edu.yu.cs.com1320.project.stage5.DocumentStore;
import edu.yu.cs.com1320.project.undo.CommandSet;
import edu.yu.cs.com1320.project.undo.GenericCommand;
import edu.yu.cs.com1320.project.undo.Undoable;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.*;

public class DocumentStoreImpl implements DocumentStore {

    private HashTable<URI, Document> store;
    private Stack<Undoable> commandStack;
    private Trie<Document> trie;

    private MinHeap<Document> minHeap;
    private int maxDocumentCount = Integer.MAX_VALUE;
    private int maxDocumentBytes = Integer.MAX_VALUE;
    private int currentDocumentCount = 0;
    private int currentDocumentBytes = 0;

    public DocumentStoreImpl() {
        this.store = new HashTableImpl<>();
        this.commandStack = new StackImpl<>();
        this.trie = new TrieImpl<>();
        this.minHeap = new MinHeapImpl<>();
    }

    @Override
    public String setMetadata(URI uri, String key, String value) {
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
        String oldValue = doc.getMetadataValue(key);
        doc.setMetadataValue(key,value);
        updateDocTime(doc);
        GenericCommand<URI> command = new GenericCommand<>(uri, u -> {
            Document undoDoc = this.store.get(u);
            if(undoDoc != null) {
                undoDoc.setMetadataValue(key, oldValue);
                updateDocTime(undoDoc);
            }
        });
        this.commandStack.push(command);
        return oldValue;
    }

    @Override
    public String getMetadata(URI uri, String key) {
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
        updateDocTime(doc);
        return doc.getMetadataValue(key);
    }

    @Override
    public int put(InputStream input, URI url, DocumentFormat format) throws IOException {
        if(url == null || url.toString().isBlank()) {
            throw new IllegalArgumentException("uri cannot be null or blank");
        }
        if(format == null) {
            throw new IllegalArgumentException("format cannot be null");
        }
        if(input == null) {
            Document oldDoc = this.store.get(url);
            if(oldDoc == null) {
                return 0;
            }
            int oldHash = oldDoc.hashCode();
            fullyDeleteDocument(oldDoc);
            GenericCommand<URI> command = new GenericCommand<>(url, u -> {
                restoreDocument(oldDoc);
            });
            this.commandStack.push(command);
            return oldHash;
        }
        byte[] bytes = input.readAllBytes();
        Document newDoc;
        if(format == DocumentFormat.TXT) {
            String text = new String (bytes);
            newDoc = new DocumentImpl(url,text);
        } else {
            newDoc = new DocumentImpl(url,bytes);
        }
        if(getDocumentBytes(newDoc) > this.maxDocumentBytes) {
            throw new IllegalArgumentException("document exceeds byte limit");
        }
        Document oldDoc = this.store.get(url);
        int oldHash = (oldDoc == null) ? 0 : oldDoc.hashCode();
        if(oldDoc != null) {
            fullyDeleteDocument(oldDoc);
        }
        this.store.put(url,newDoc);
        addDocToTrie(newDoc);
        addDocToHeap(newDoc);
        this.currentDocumentCount++;
        this.currentDocumentBytes += getDocumentBytes(newDoc);
        GenericCommand<URI> command;
        if(oldDoc == null) {
            command = new GenericCommand<>(url, u -> {
                Document currentDoc = this.store.get(u);
                if(currentDoc != null) {
                    fullyDeleteDocument(currentDoc);
                }
            });
        } else {
            command = new GenericCommand<>(url, u -> {
                Document currentDoc = this.store.get(u);
                if (currentDoc != null) {
                    fullyDeleteDocument(currentDoc);
                }
                restoreDocument(oldDoc);
            });
        }
        this.commandStack.push(command);
        enforceMemoryLimits();
        return oldHash;
    }

    private int getDocumentBytes(Document doc) {
        if (doc.getDocumentTxt() != null) {
            return doc.getDocumentTxt().getBytes().length;
        }
        return doc.getDocumentBinaryData().length;
    }

    private void addDocToHeap(Document doc) {
        doc.setLastUseTime(System.nanoTime());
        this.minHeap.insert(doc);
    }

    private void removeDocFromHeap(Document doc) {
        doc.setLastUseTime(Long.MIN_VALUE);
        this.minHeap.reHeapify(doc);
        this.minHeap.remove();
    }

    private void updateDocTime(Document doc) {
        doc.setLastUseTime(System.nanoTime());
        this.minHeap.reHeapify(doc);
    }

    private void addDocToTrie(Document doc) {
        if(doc == null || doc.getDocumentTxt() == null) {
            return;
        }
        for(String word : doc.getWords()) {
            this.trie.put(word,doc);
        }
    }

    private void removeDocFromTrie(Document doc) {
        if(doc == null || doc.getDocumentTxt() == null) {
            return;
        }
        for(String word : doc.getWords()) {
            this.trie.delete(word,doc);
        }
    }

    @Override
    public Document get(URI url) {
        Document doc = this.store.get(url);
        if(doc != null) {
            updateDocTime(doc);
        }
        return doc;
    }

    private void fullyDeleteDocument(Document doc) {
        if(doc == null) {
            return;
        }
        URI uri = doc.getKey();
        this.store.put(uri,null);
        removeDocFromTrie(doc);
        removeDocFromHeap(doc);
        this.currentDocumentCount--;
        this.currentDocumentBytes -= getDocumentBytes(doc);
    }

    private void restoreDocument(Document doc) {
        this.store.put(doc.getKey(),doc);
        addDocToTrie(doc);
        addDocToHeap(doc);
        this.currentDocumentCount++;
        this.currentDocumentBytes += getDocumentBytes(doc);
        enforceMemoryLimits();
    }

    @Override
    public boolean delete(URI url) {
        if(url == null || url.toString().isBlank()) {
            throw new IllegalArgumentException("url cannot be null or blank");
        }
        Document doc = this.store.get(url);
        if(doc == null) {
            return false;
        }
        fullyDeleteDocument(doc);
        GenericCommand<URI> command = new GenericCommand<>(url, u -> {
            restoreDocument(doc);
        });
        this.commandStack.push(command);
        return true;
    }

    @Override
    public void undo() throws IllegalStateException {
        if(this.commandStack.size() == 0) {
            throw new IllegalStateException("no actions to undo");
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
    public List<Document> search(String keyword) {
        if(keyword == null) {
            return new ArrayList<>();
        }
        Comparator<Document> comparator = (doc1, doc2) ->
                Integer.compare(doc2.wordCount(keyword),doc1.wordCount(keyword));
        List<Document> docs = this.trie.getSorted(keyword,comparator);
        updateDocTimes(docs);
        return docs;
    }

    private void updateDocTimes(List<Document> docs) {
        for(Document doc : docs) {
            updateDocTime(doc);
        }
    }

    @Override
    public List<Document> searchByPrefix(String keywordPrefix) {
        if(keywordPrefix == null) {
            return new ArrayList<>();
        }
        Comparator<Document> comparator = (doc1, doc2) ->
                Integer.compare(prefixCount(doc2,keywordPrefix),prefixCount(doc1,keywordPrefix));
        List<Document> docs = this.trie.getAllWithPrefixSorted(keywordPrefix,comparator);
        updateDocTimes(docs);
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
    public Set<URI> deleteAll(String keyword) {
        Set<Document> docsToDelete = this.trie.deleteAll(keyword);
        Set<URI> deletedUris = new HashSet<>();
        CommandSet<URI> commandSet = new CommandSet<>();
        for(Document doc : docsToDelete) {
            URI uri = doc.getKey();
            deletedUris.add(uri);
            this.store.put(uri,null);
            removeDocFromHeap(doc);
            this.currentDocumentCount--;
            this.currentDocumentBytes -= getDocumentBytes(doc);
            GenericCommand<URI> command = new GenericCommand<>(uri, u-> {
                restoreDocument(doc);
            });
            commandSet.addCommand(command);
        }
        this.commandStack.push(commandSet);
        return deletedUris;
    }

    @Override
    public Set<URI> deleteAllWithPrefix(String keywordPrefix) {
        Set<Document> docsToDelete = this.trie.deleteAllWithPrefix(keywordPrefix);
        Set<URI> deletedUris = new HashSet<>();
        CommandSet<URI> commandSet = new CommandSet<>();
        for(Document doc : docsToDelete) {
            URI uri = doc.getKey();
            deletedUris.add(uri);
            this.store.put(uri, null);
            removeDocFromHeap(doc);
            this.currentDocumentCount--;
            this.currentDocumentBytes -= getDocumentBytes(doc);
            GenericCommand<URI> command = new GenericCommand<>(uri, u -> {
                restoreDocument(doc);
            });
            commandSet.addCommand(command);
        }
        this.commandStack.push(commandSet);
        return deletedUris;
    }

    @Override
    public List<Document> searchByMetadata(Map<String, String> keysValues) {
        List<Document> results = new ArrayList<>();
        if(keysValues == null || keysValues.isEmpty()) {
            return results;
        }
        for(URI uri : this.store.keySet()) {
            Document doc = this.store.get(uri);
            if(matchesMetadata(doc,keysValues)) {
                results.add(doc);
            }
        }
        updateDocTimes(results);
        return results;
    }

    private boolean matchesMetadata(Document doc, Map<String,String> keysValues) {
        if(doc == null || keysValues == null || keysValues.isEmpty()) {
            return false;
        }
        for(Map.Entry<String,String> entry : keysValues.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            String docValue = doc.getMetadataValue(key);
            if(docValue == null || !docValue.equals(value)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public List<Document> searchByKeywordAndMetadata(String keyword, Map<String, String> keysValues) {
        List<Document> results = new ArrayList<>();
        Comparator<Document> comparator = (d1,d2) ->
                Integer.compare(d2.wordCount(keyword),d1.wordCount(keyword));
        List<Document> docs = this.trie.getSorted(keyword,comparator);
        for(Document doc : docs) {
            if(matchesMetadata(doc,keysValues)) {
                results.add(doc);
            }
        }
        updateDocTimes(results);
        return results;
    }

    @Override
    public List<Document> searchByPrefixAndMetadata(String keywordPrefix, Map<String, String> keysValues) {
        List<Document> results = new ArrayList<>();
        Comparator<Document> comparator = (d1,d2) ->
                Integer.compare(prefixCount(d2,keywordPrefix),prefixCount(d1,keywordPrefix));
        List<Document> docs = this.trie.getAllWithPrefixSorted(keywordPrefix,comparator);
        for(Document doc : docs) {
            if(matchesMetadata(doc,keysValues)) {
                results.add(doc);
            }
        }
        updateDocTimes(results);
        return results;
    }

    @Override
    public Set<URI> deleteAllWithMetadata(Map<String, String> keysValues) {
        List<Document> docs = searchByMetadata(keysValues);
        Set<URI> deletedUris = new HashSet<>();
        CommandSet<URI> commandSet = new CommandSet<>();
        for(Document doc : docs) {
            URI uri = doc.getKey();
            deletedUris.add(uri);
            fullyDeleteDocument(doc);
            GenericCommand<URI> command = new GenericCommand<>(uri,u -> {
                restoreDocument(doc);
            });
            commandSet.addCommand(command);
        }
        this.commandStack.push(commandSet);
        return deletedUris;
    }

    @Override
    public Set<URI> deleteAllWithKeywordAndMetadata(String keyword, Map<String, String> keysValues) {
        List<Document> docs = searchByKeywordAndMetadata(keyword,keysValues);
        Set<URI> deletedUris = new HashSet<>();
        CommandSet<URI> commandSet = new CommandSet<>();
        for(Document doc : docs) {
            URI uri = doc.getKey();
            deletedUris.add(uri);
            fullyDeleteDocument(doc);
            GenericCommand<URI> command = new GenericCommand<>(uri,u -> {
                restoreDocument(doc);
            });
            commandSet.addCommand(command);
        }
        this.commandStack.push(commandSet);
        return deletedUris;
    }

    @Override
    public Set<URI> deleteAllWithPrefixAndMetadata(String keywordPrefix, Map<String, String> keysValues) {
        List<Document> docs = searchByPrefixAndMetadata(keywordPrefix,keysValues);
        Set<URI> deletedUris = new HashSet<>();
        CommandSet<URI> commandSet = new CommandSet<>();
        for(Document doc : docs) {
            URI uri = doc.getKey();
            deletedUris.add(uri);
            fullyDeleteDocument(doc);
            GenericCommand<URI> command = new GenericCommand<>(uri,u -> {
                restoreDocument(doc);
            });
            commandSet.addCommand(command);
        }
        this.commandStack.push(commandSet);
        return deletedUris;
    }

    @Override
    public void setMaxDocumentCount(int limit) {
        if (limit < 1) {
            throw new IllegalArgumentException("limit must be more than 1");
        }
        this.maxDocumentCount = limit;
        enforceMemoryLimits();
    }

    @Override
    public void setMaxDocumentBytes(int limit) {
        if (limit < 1) {
            throw new IllegalArgumentException("limit must be more than 1");
        }
        this.maxDocumentBytes = limit;
        enforceMemoryLimits();
    }

    private void enforceMemoryLimits() {
        while(this.currentDocumentCount > this.maxDocumentCount || this.currentDocumentBytes > this.maxDocumentBytes) {
            getRidOfLeastRecentlyUsed();
        }
    }

    private void getRidOfLeastRecentlyUsed() {
        Document doc = this.minHeap.remove();
        URI uri = doc.getKey();
        this.store.put(uri,null);
        removeDocFromTrie(doc);
        this.currentDocumentCount --;
        this.currentDocumentBytes -= getDocumentBytes(doc);
        removeUndoCommands(uri);
    }

    private void removeUndoCommands(URI uri) {
        Stack<Undoable> temp = new StackImpl<>();
        while(this.commandStack.size() > 0) {
            Undoable command = this.commandStack.pop();
            if(command instanceof GenericCommand<?> gc) {
                if(!gc.getTarget().equals(uri)) {
                    temp.push(command);
                }
            } else if (command instanceof CommandSet<?> cs) {
                CommandSet<URI> newSet = new CommandSet<>();
                for(Object obj : cs) {
                    GenericCommand<URI> gc = (GenericCommand<URI>) obj;
                    if(!gc.getTarget().equals(uri)) {
                        newSet.addCommand(gc);
                    }
                }
                if (newSet.size() > 0) {
                    temp.push(newSet);
                }
            }
        }
        while(temp.size() > 0) {
            this.commandStack.push(temp.pop());
        }
    }
}
