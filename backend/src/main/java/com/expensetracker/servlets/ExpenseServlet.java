package com.expensetracker.servlets;

import com.expensetracker.database.MongoDBConnection;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import org.bson.Document;
import org.bson.conversions.Bson;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
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
import java.util.regex.Pattern;
import static com.mongodb.client.model.Filters.*;
import org.bson.conversions.Bson;


@WebServlet("/expense") // Maps to /expense URL
public class ExpenseServlet extends HttpServlet {
    private MongoCollection<Document> expenseCollection;
    private final Gson gson = new Gson(); // Gson instance for JSON conversion

    @Override
    public void init() {
        MongoDatabase database = MongoDBConnection.connect();
        expenseCollection = database.getCollection("expenses"); // Ensure this collection exists in Compass
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();

        try {
            // Read the request body
            String requestBody = request.getReader().lines().reduce("", (acc, line) -> acc + line);

            // Parse JSON
            ExpenseRequest expenseRequest = gson.fromJson(requestBody, ExpenseRequest.class);

            // Validate fields
            if (expenseRequest.user == null || expenseRequest.category == null ||
                    expenseRequest.amount == null || expenseRequest.date == null ||
                    expenseRequest.description == null) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                out.println(gson.toJson(new ResponseMessage("❌ Missing required fields")));
                return;
            }

            Document expense = new Document("user", expenseRequest.user)
                    .append("category", expenseRequest.category)
                    .append("amount", expenseRequest.amount)
                    .append("date", expenseRequest.date)
                    .append("description", expenseRequest.description);

            expenseCollection.insertOne(expense);

            response.setStatus(HttpServletResponse.SC_CREATED);
            out.println(gson.toJson(new ResponseMessage("✅ Expense added successfully")));
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.println(gson.toJson(new ResponseMessage("❌ Error adding expense: " + e.getMessage())));
        }
    }



    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        String username = request.getParameter("username");
        String category = request.getParameter("category");

        PrintWriter out = response.getWriter();

        try {
            MongoCollection<Document> collection = MongoDBConnection.getCollection("expenses");

            Bson filter;
            if (category != null && !category.isEmpty()) {
                filter = Filters.and(Filters.eq("username", username), Filters.eq("category", category));
            } else {
                filter = Filters.eq("username", username);
            }

            JsonArray resultArray = new JsonArray();

            collection.find(filter).forEach((Document doc) -> {
                JsonObject obj = JsonParser.parseString(doc.toJson()).getAsJsonObject();
                resultArray.add(obj);
            });

            out.print(resultArray.toString());
            out.flush();
        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print("{\"error\":\"" + e.getMessage() + "\"}");
        }
    }


    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        PrintWriter out = resp.getWriter();

        String username = req.getParameter("username");
        String category = req.getParameter("category");
        String date = req.getParameter("date");
        String description = req.getParameter("description");

        if (username == null || username.isEmpty()) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.println(gson.toJson(new ResponseMessage("❌ Username is required")));
            return;
        }

        try {
            Document query = new Document("user", username);

            if (category != null && date != null && description != null) {
                // Delete specific expense
                query.append("category", category)
                        .append("date", date)
                        .append("description", description);

                var result = expenseCollection.deleteOne(query);

                if (result.getDeletedCount() > 0) {
                    resp.setStatus(HttpServletResponse.SC_OK);
                    out.println(gson.toJson(new ResponseMessage("✅ Expense deleted successfully")));
                } else {
                    resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    out.println(gson.toJson(new ResponseMessage("⚠️ Expense not found")));
                }
            } else {
                // Delete all expenses of the user
                var result = expenseCollection.deleteMany(query);
                resp.setStatus(HttpServletResponse.SC_OK);
                out.println(gson.toJson(new ResponseMessage("🧹 " + result.getDeletedCount() + " expenses deleted for user: " + username)));
            }

        } catch (Exception e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.println(gson.toJson(new ResponseMessage("❌ Error deleting expense(s): " + e.getMessage())));
        }
    }


    // Helper class for JSON responses
    private static class ResponseMessage {

        ResponseMessage(String message) {
        }
    }
    private static class ExpenseRequest {
        String user;
        String category;
        Double amount;
        String date;
        String description;
    }

}
