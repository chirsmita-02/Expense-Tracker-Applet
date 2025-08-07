package com.expensetracker.database;

import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.MongoCollection;
import org.bson.Document;

public class MongoDBConnection {
    private static final String URI = "mongodb://localhost:27017";
    private static final String DATABASE_NAME = "ExpenseTracker";
    private static MongoClient mongoClient = null;

    public static MongoDatabase connect() {
        if (mongoClient == null) {
            synchronized (MongoDBConnection.class) {
                if (mongoClient == null) {
                    mongoClient = MongoClients.create(URI);
                }
            }
        }
        return mongoClient.getDatabase(DATABASE_NAME);
    }

    public static MongoCollection<Document> getCollection(String collectionName) {
        return connect().getCollection(collectionName);
    }

    public static void main(String[] args) {
        try {
            MongoDatabase db = connect();
            System.out.println("✅ Connected to database: " + db.getName());
        } catch (Exception e) {
            System.out.println("❌ Connection failed!");
            e.printStackTrace();
        }
    }
}
