package edu.yu.cs.com1320.project.stage6.impl;

import edu.yu.cs.com1320.project.stage6.Document;
import edu.yu.cs.com1320.project.stage6.PersistenceManager;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.Base64;
import java.util.HashMap;

public class DocumentPersistenceManager implements PersistenceManager<URI, Document> {
    private File baseDir;
    private Gson gson;

    public DocumentPersistenceManager(File baseDir) {
        if(baseDir == null) {
            this.baseDir = new File(System.getProperty("user.dir"));
        } else {
            this.baseDir = baseDir;
        }
        this.gson = new GsonBuilder().registerTypeAdapter(Document.class,new DocumentSerializer()).registerTypeAdapter(Document.class,new DocumentDeserializer()).create();
    }

    private class DocumentSerializer implements JsonSerializer<Document> {
        @Override
        public JsonElement serialize(Document doc, Type type, JsonSerializationContext context) {
            JsonObject json = new JsonObject();
            json.addProperty("uri",doc.getKey().toString());
            if(doc.getDocumentTxt() != null) {
                json.addProperty("text",doc.getDocumentTxt());
            } else {
                String binaryAsString = Base64.getEncoder().encodeToString(doc.getDocumentBinaryData());
                json.addProperty("binaryData",binaryAsString);
            }
            json.add("metadata",context.serialize(doc.getMetadata()));
            json.add("wordMap",context.serialize(doc.getWordMap()));
            return json;
        }
    }

    @Override
    public void serialize(URI uri, Document val) throws IOException {
        if(uri == null || val == null) {
            throw new IllegalArgumentException("uri or doc cannot be null");
        }
        File file = getFile(uri);
        file.getParentFile().mkdirs();
        try (FileWriter writer = new FileWriter(file)) {
            this.gson.toJson(val,writer);
        }
    }

    private class DocumentDeserializer implements JsonDeserializer<Document> {

        @Override
        public Document deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext context) throws JsonParseException {
            JsonObject json = jsonElement.getAsJsonObject();
            URI uri = URI.create(json.get("uri").getAsString());
            HashMap<String,String> metadata = context.deserialize(json.get("metadata"),new TypeToken<HashMap<String,String>>(){}.getType());
            HashMap<String,Integer> wordMap = context.deserialize(json.get("wordMap"),new TypeToken<HashMap<String,Integer>>(){}.getType());
            Document doc;
            if(json.has("text")) {
                String text = json.get("text").getAsString();
                doc = new DocumentImpl(uri,text,wordMap);
            } else {
                JsonElement binaryElement = json.get("binaryData");
                byte[] binaryData;
                if(binaryElement.isJsonPrimitive()) {
                    String binaryAsString = binaryElement.getAsString();
                    binaryData = Base64.getDecoder().decode(binaryAsString);
                } else {
                    JsonArray array = binaryElement.getAsJsonArray();
                    binaryData = new byte[array.size()];
                    for(int i = 0; i < array.size(); i++) {
                        binaryData[i] = array.get(i).getAsByte();
                    }
                }
                doc = new DocumentImpl(uri,binaryData);
            }
            doc.setMetadata(metadata);
            doc.setWordMap(wordMap);
            return doc;
        }
    }

    @Override
    public Document deserialize(URI uri) throws IOException {
        if(uri == null) {
            throw new IllegalArgumentException("uri cannot be null");
        }
        File file = getFile(uri);
        if(!file.exists()) {
            return null;
        }
        Document doc;
        try (FileReader reader = new FileReader(file)) {
            doc = this.gson.fromJson(reader,Document.class);
        }
        this.delete(uri);
        return doc;
    }

    @Override
    public boolean delete(URI uri) throws IOException {
        File file = getFile(uri);
        if(!file.exists()) {
            return false;
        }
        return file.delete();
    }

    private File getFile(URI uri) {
        String path = uri.toString();
        path = path.replaceFirst("https?://","");
        return new File(this.baseDir,path + ".json");
    }
}
