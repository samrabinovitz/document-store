package edu.yu.cs.com1320.project.stage4.impl;

import edu.yu.cs.com1320.project.HashTable;
import edu.yu.cs.com1320.project.Trie;
import edu.yu.cs.com1320.project.impl.HashTableImpl;
import edu.yu.cs.com1320.project.impl.StackImpl;
import edu.yu.cs.com1320.project.impl.TrieImpl;
import edu.yu.cs.com1320.project.stage4.Document;
import edu.yu.cs.com1320.project.stage4.DocumentStore;
import edu.yu.cs.com1320.project.undo.CommandSet;
import edu.yu.cs.com1320.project.undo.GenericCommand;
import edu.yu.cs.com1320.project.undo.Undoable;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.*;

public class DocumentStoreImpl implements DocumentStore {
    private HashTable<URI, Document> store;
    private StackImpl<Undoable> commandStack;
    private Trie<Document> trie;

    public DocumentStoreImpl() {
        this.store = new HashTableImpl<>();
        this.commandStack = new StackImpl<>();
        this.trie = new TrieImpl<>();
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
        GenericCommand<URI> command = new GenericCommand<>(uri, u -> {
            Document undoDoc = this.store.get(u);
            if(undoDoc != null) {
                undoDoc.setMetadataValue(key, oldValue);
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
        return doc.getMetadataValue(key);
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
            this.store.put(url,null);
            removeDocFromTrie(oldDoc);
            GenericCommand<URI> command = new GenericCommand<>(url, u -> {
                this.store.put(u,oldDoc);
                addDocToTrie(oldDoc);
            });
            this.commandStack.push(command);
            return oldDoc.hashCode();
        }
        byte[] bytes = input.readAllBytes();
        Document newDoc;
        if(format == DocumentFormat.TXT) {
            String text = new String (bytes);
            newDoc = new DocumentImpl(url,text);
        } else {
            newDoc = new DocumentImpl(url,bytes);
        }
        Document oldDoc = this.store.get(url);
        this.store.put(url,newDoc);
        addDocToTrie(newDoc);
        if(oldDoc != null) {
            removeDocFromTrie(oldDoc);
        }
        GenericCommand<URI> command;
        if(oldDoc == null) {
            command = new GenericCommand<>(url, u -> {
                this.store.put(u,null);
                removeDocFromTrie(newDoc);
            });
        } else {
            command = new GenericCommand<>(url, u -> {
                this.store.put(u,oldDoc);
                removeDocFromTrie(newDoc);
                addDocToTrie(oldDoc);
            });
        }
        this.commandStack.push(command);
        return (oldDoc == null) ? 0 : oldDoc.hashCode();
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
        if(url == null || url.toString().isBlank()) {
            throw new IllegalArgumentException("uri cannot be null or blank");
        }
        Document deletedDoc = this.store.get(url);
        if(deletedDoc == null) {
            return false;
        }
        this.store.put(url,null);
        removeDocFromTrie(deletedDoc);
        GenericCommand<URI> command = new GenericCommand<>(url, u -> {
            this.store.put(u,deletedDoc);
            addDocToTrie(deletedDoc);
        });
        this.commandStack.push(command);
        return true;
    }

    @Override
    public void undo() throws IllegalStateException {
        if(this.commandStack.size() == 0) {
            throw new IllegalStateException("nothing to be undone");
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
        return this.trie.getSorted(keyword,comparator);
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
    public List<Document> searchByPrefix(String keywordPrefix) {
        if(keywordPrefix == null) {
            return new ArrayList<>();
        }
        Comparator<Document> comparator = (doc1, doc2) ->
                Integer.compare(prefixCount(doc2,keywordPrefix),prefixCount(doc1,keywordPrefix));
        return this.trie.getAllWithPrefixSorted(keywordPrefix,comparator);
    }

    @Override
    public Set<URI> deleteAll(String keyword) {
        Set<Document> docsToDelete = this.trie.deleteAll(keyword);
        Set<URI> deletedUris = new HashSet<>();
        if(docsToDelete == null || docsToDelete.isEmpty()) {
            return deletedUris;
        }
        CommandSet<URI> commandSet = new CommandSet<>();
        for(Document doc : docsToDelete) {
            URI uri = doc.getKey();
            deletedUris.add(uri);
            this.store.put(uri, null);
            removeDocFromTrie(doc);

            GenericCommand<URI> command = new GenericCommand<>(uri, u -> {
                this.store.put(u, doc);
                addDocToTrie(doc);
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
        if(docsToDelete == null || docsToDelete.isEmpty()) {
            return deletedUris;
        }
        CommandSet<URI> commandSet = new CommandSet<>();
        for(Document doc : docsToDelete) {
            URI uri = doc.getKey();
            deletedUris.add(uri);
            this.store.put(uri,null);
            removeDocFromTrie(doc);

            GenericCommand<URI> command = new GenericCommand<>(uri, u -> {
                this.store.put(u,doc);
                addDocToTrie(doc);
            });
            commandSet.addCommand(command);
        }
        this.commandStack.push(commandSet);
        return deletedUris;
    }

    private boolean matchesMetadata(Document doc, Map<String,String> keysValues) {
        if(doc == null) {
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
        return results;
    }

    @Override
    public List<Document> searchByKeywordAndMetadata(String keyword, Map<String, String> keysValues) {
        List<Document> results = new ArrayList<>();
        for(Document doc : this.search(keyword)) {
            if(matchesMetadata(doc,keysValues)) {
                results.add(doc);
            }
        }
        return results;
    }

    @Override
    public List<Document> searchByPrefixAndMetadata(String keywordPrefix, Map<String, String> keysValues) {
        List<Document> results = new ArrayList<>();
        for(Document doc : this.searchByPrefix(keywordPrefix)) {
            if(matchesMetadata(doc,keysValues)) {
                results.add(doc);
            }
        }
        return results;
    }

    @Override
    public Set<URI> deleteAllWithMetadata(Map<String, String> keysValues) {
        Set<URI> deletedUris = new HashSet<>();
        CommandSet<URI>  commandSet = new CommandSet<>();
        for(URI uri : new HashSet<>(this.store.keySet())) {
            Document doc = this.store.get(uri);
            if(matchesMetadata(doc,keysValues)) {
                deletedUris.add(uri);
                this.store.put(uri,null);
                removeDocFromTrie(doc);
                GenericCommand<URI> command = new GenericCommand<>(uri, u -> {
                    this.store.put(u,doc);
                    addDocToTrie(doc);
                });
                commandSet.addCommand(command);
            }
        }
        if(! deletedUris.isEmpty()) {
            this.commandStack.push(commandSet);
        }
        return deletedUris;
    }

    @Override
    public Set<URI> deleteAllWithKeywordAndMetadata(String keyword, Map<String, String> keysValues) {
        Set<URI> deletedUris = new HashSet<>();
        CommandSet<URI> commandSet = new CommandSet<>();
        for(Document doc : this.search(keyword)) {
            if (matchesMetadata(doc, keysValues)) {
                URI uri = doc.getKey();
                deletedUris.add(uri);
                this.store.put(uri, null);
                removeDocFromTrie(doc);
                GenericCommand<URI> command = new GenericCommand<>(uri, u -> {
                    this.store.put(u, doc);
                    addDocToTrie(doc);
                });
                commandSet.addCommand(command);
            }
        }
        if(! deletedUris.isEmpty()) {
            commandStack.push(commandSet);
        }
        return deletedUris;
    }

    @Override
    public Set<URI> deleteAllWithPrefixAndMetadata(String keywordPrefix, Map<String, String> keysValues) {
        Set<URI> deletedUris = new HashSet<>();
        CommandSet<URI> commandSet = new CommandSet<>();
        for(Document doc : this.searchByPrefix(keywordPrefix)) {
            if (matchesMetadata(doc, keysValues)) {
                URI uri = doc.getKey();
                deletedUris.add(uri);
                this.store.put(uri, null);
                removeDocFromTrie(doc);
                GenericCommand<URI> command = new GenericCommand<>(uri, u -> {
                    this.store.put(u, doc);
                    addDocToTrie(doc);
                });
                commandSet.addCommand(command);
            }
        }
        if(! deletedUris.isEmpty()) {
            commandStack.push(commandSet);
        }
        return deletedUris;
    }
}
