package com.expensetracker.servlets;

import com.expensetracker.database.MongoDBConnection;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.google.gson.Gson;
import org.bson.Document;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

@WebServlet("/subscription") // Maps to /subscription URL
public class SubscriptionServlet extends HttpServlet {
    private MongoCollection<Document> subscriptionCollection;
    private final Gson gson = new Gson(); // Gson instance for JSON conversion

    @Override
    public void init() {
        MongoDatabase database = MongoDBConnection.connect();
        subscriptionCollection = database.getCollection("subscriptions"); // Ensure this collection exists in Compass
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        PrintWriter out = response.getWriter();

        try {
            // Read JSON body
            String requestBody = request.getReader().lines().reduce("", (acc, line) -> acc + line);

            // Parse into SubscriptionRequest class
            SubscriptionRequest subscriptionRequest = gson.fromJson(requestBody, SubscriptionRequest.class);

            // Validate required fields
            if (subscriptionRequest.user == null || subscriptionRequest.service == null || subscriptionRequest.renewal_date == null) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                out.println(gson.toJson(new ResponseMessage("❌ Missing required fields")));
                return;
            }

            Document subscription = new Document("user", subscriptionRequest.user)
                    .append("service", subscriptionRequest.service)
                    .append("renewal_date", subscriptionRequest.renewal_date);

            subscriptionCollection.insertOne(subscription);

            response.setStatus(HttpServletResponse.SC_CREATED);
            out.println(gson.toJson(new ResponseMessage("✅ Subscription added successfully")));
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.println(gson.toJson(new ResponseMessage("❌ Error adding subscription: " + e.getMessage())));
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        PrintWriter out = response.getWriter();

        try {
            String user = request.getParameter("user");

            if (user == null) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                out.println(gson.toJson(new ResponseMessage("❌ User parameter is required")));
                return;
            }

            List<Document> subscriptionsList = new ArrayList<>();
            try (MongoCursor<Document> cursor = subscriptionCollection.find(new Document("user", user)).iterator()) {
                while (cursor.hasNext()) {
                    subscriptionsList.add(cursor.next());
                }
            }

            response.setStatus(HttpServletResponse.SC_OK);
            out.println(gson.toJson(subscriptionsList));
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.println(gson.toJson(new ResponseMessage("❌ Error fetching subscriptions: " + e.getMessage())));
        }
    }
    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();

        try {
            String user = request.getParameter("user");
            String service = request.getParameter("service"); // Optional

            if (user == null || user.trim().isEmpty()) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                out.println(gson.toJson(new ResponseMessage("❌ 'user' parameter is required")));
                return;
            }

            Document query = new Document("user", user.trim());
            if (service != null && !service.trim().isEmpty()) {
                query.append("service", service.trim());
            }

            long deletedCount = (service != null && !service.trim().isEmpty()) ?
                    subscriptionCollection.deleteOne(query).getDeletedCount() :
                    subscriptionCollection.deleteMany(query).getDeletedCount();

            response.setStatus(HttpServletResponse.SC_OK);
            out.println(gson.toJson(new ResponseMessage("✅ Deleted " + deletedCount + " subscription(s)")));
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.println(gson.toJson(new ResponseMessage("❌ Error deleting subscription(s): " + e.getMessage())));
        }
    }

    // Helper class for JSON responses
    private static class ResponseMessage {
        private final String message;

        ResponseMessage(String message) {
            this.message = message;
        }
    }
    private static class SubscriptionRequest {
        String user;
        String service;
        String renewal_date;
    }

}
