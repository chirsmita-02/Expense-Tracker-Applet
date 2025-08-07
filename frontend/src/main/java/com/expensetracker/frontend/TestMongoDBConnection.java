package com.expensetracker.frontend;





import com.expensetracker.database.MongoDBConnection;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;

public class TestMongoDBConnection {
    public static void main(String[] args) {
        try {
            // Step 1: Connect to DB
            MongoDatabase db = MongoDBConnection.connect();
            System.out.println("✅ Connected to DB");

            // Step 2: Access the "users" collection
            MongoCollection<Document> usersCollection = db.getCollection("users");

            // Step 3: Insert test data
            Document testUser = new Document("username", "connectionTestUser")
                    .append("password", "123456");
            usersCollection.insertOne(testUser);
            System.out.println("✅ Inserted test user");

            // Step 4: Fetch and print user
            Document foundUser = usersCollection.find(new Document("username", "connectionTestUser")).first();

            if (foundUser != null) {
                System.out.println("✅ Found user: " + foundUser.toJson());
            } else {
                System.out.println("❌ User not found");
            }

        } catch (Exception e) {
            System.out.println("❌ Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
