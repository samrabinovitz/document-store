package edu.yu.cs.com1320.project.stage3.impl;

import edu.yu.cs.com1320.project.HashTable;
import edu.yu.cs.com1320.project.Stack;
import edu.yu.cs.com1320.project.impl.HashTableImpl;
import edu.yu.cs.com1320.project.impl.StackImpl;
import edu.yu.cs.com1320.project.stage3.Document;
import edu.yu.cs.com1320.project.stage3.DocumentStore;
import edu.yu.cs.com1320.project.undo.Command;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

public class DocumentStoreImpl implements DocumentStore {
    private HashTable<URI,Document> store;
    private Stack<Command> commandStack;

    public DocumentStoreImpl() {
        this.store = new HashTableImpl<>();
        this.commandStack = new StackImpl<>();
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
        Command command = new Command(uri,u -> {
            Document d = this.store.get(u);
            d.setMetadataValue(key,oldValue);
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

    @Override
    public int put(InputStream input, URI url, DocumentFormat format) throws IOException {
        if(url == null || url.toString().isBlank()) {
            throw new IllegalArgumentException("url cannot be null or blank");
        }
        if(format == null) {
            throw new IllegalArgumentException("format cannot be null");
        }
        Document oldDoc = this.store.get(url);
        if(input == null) {
            if(oldDoc == null) {
                return 0;
            }
            this.store.put(url,null);
            Command command = new Command(url,u -> {
                this.store.put(u,oldDoc);
            });
            this.commandStack.push(command);
            return oldDoc.hashCode();
        }
        byte[] bytes = input.readAllBytes();
        Document newDoc;
        if(format == DocumentFormat.TXT) {
            String backToText = new String(bytes);
            newDoc = new DocumentImpl(url,backToText);
        } else {
            newDoc = new DocumentImpl(url,bytes);
        }
        this.store.put(url,newDoc);
        Command command = new Command(url,u -> {
            this.store.put(u,oldDoc);
        });
        this.commandStack.push(command);
        return (oldDoc == null ? 0 : oldDoc.hashCode());
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
        Document oldDoc = this.store.get(url);
        if(oldDoc == null) {
            return false;
        }
        this.store.put(url,null);
        Command command = new Command(url,u-> {
            this.store.put(u,oldDoc);
        });
        this.commandStack.push(command);
        return true;
    }

    @Override
    public void undo() throws IllegalStateException {
        if(this.commandStack.size() == 0) {
            throw new IllegalStateException("nothing to be undone");
        }
        Command command = this.commandStack.pop();
        command.undo();
    }

    @Override
    public void undo(URI url) throws IllegalStateException {
        if(url == null) {
            throw new IllegalArgumentException("url cannot be null");
        }
        if(this.commandStack.size() == 0) {
            throw new IllegalStateException("nothing to be undone for that uri");
        }
        Stack<Command> temporaryStack = new StackImpl<>();
        while(this.commandStack.size() > 0) {
            Command command = this.commandStack.pop();
            if(command.getUri().equals(url)) {
                command.undo();
                while(temporaryStack.size() > 0) {
                    this.commandStack.push(temporaryStack.pop());
                }
                return;
            } else {
                temporaryStack.push(command);
            }
        }
        while(temporaryStack.size() > 0) {
            this.commandStack.push(temporaryStack.pop());
        }
        throw new IllegalStateException("nothing to be undone for that uri");
    }
}
