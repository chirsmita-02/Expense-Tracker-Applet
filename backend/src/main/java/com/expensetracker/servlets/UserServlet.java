package com.expensetracker.servlets;

import com.expensetracker.database.MongoDBConnection;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.google.gson.Gson;
import org.bson.Document;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.stream.Collectors;

@WebServlet("/UserServlet")
public class UserServlet extends HttpServlet {
    private MongoCollection<Document> usersCollection;
    private final Gson gson = new Gson();

    @Override
    public void init() {
        MongoDatabase database = MongoDBConnection.connect();
        usersCollection = database.getCollection("users");
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json");
        PrintWriter out = response.getWriter();

        String requestData = request.getReader().lines().collect(Collectors.joining());
        UserRequest userRequest = gson.fromJson(requestData, UserRequest.class);

        if (userRequest == null ||
                userRequest.username == null ||
                userRequest.password == null ||
                userRequest.action == null) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.println(gson.toJson(new ResponseMessage("Missing required fields")));
            return;
        }

        String username = userRequest.username.trim();
        String password = userRequest.password.trim();
        String action = userRequest.action.trim().toLowerCase();

        try {
            switch (action) {
                case "register":
                    handleRegistration(username, password, response, out);
                    break;
                case "login":
                    handleLogin(username, password, response, out);
                    break;
                default:
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    out.println(gson.toJson(new ResponseMessage("Invalid action")));
            }
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.println(gson.toJson(new ResponseMessage("Server error: " + e.getMessage())));
        }
    }

    private void handleRegistration(String username, String password,
                                    HttpServletResponse response, PrintWriter out) {
        if (usersCollection.find(new Document("username", username)).first() != null) {
            response.setStatus(HttpServletResponse.SC_CONFLICT);
            out.println(gson.toJson(new ResponseMessage("Username already exists")));
            return;
        }

        Document user = new Document("username", username)
                .append("password", password); // Consider hashing in real apps
        usersCollection.insertOne(user);

        response.setStatus(HttpServletResponse.SC_CREATED);
        out.println(gson.toJson(new ResponseMessage("User registered successfully")));
    }

    private void handleLogin(String username, String password,
                             HttpServletResponse response, PrintWriter out) {
        Document foundUser = usersCollection.find(new Document("username", username)).first();

        if (foundUser != null && password.equals(foundUser.getString("password"))) {
            response.setStatus(HttpServletResponse.SC_OK);
            out.println(gson.toJson(new ResponseMessage("Login successful")));
        } else {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            out.println(gson.toJson(new ResponseMessage("Invalid credentials")));
        }
    }

    private static class UserRequest {
        String action;
        String username;
        String password;
    }

    private static class ResponseMessage {
        private final String message;

        ResponseMessage(String message) {
            this.message = message;
        }
    }
}
