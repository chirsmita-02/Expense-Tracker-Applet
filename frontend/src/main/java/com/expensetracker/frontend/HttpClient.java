package com.expensetracker.frontend;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;


public class HttpClient {

    private static final String BASE_URL = "http://localhost:8080/expense-tracker";
    private static final Gson gson = new Gson();

    // Method to send a GET request
    public static JsonArray sendGetRequest(String endpoint) {

        JsonArray jsonResponse = new JsonArray();
        try {
            URL url = new URL(BASE_URL + endpoint);
            System.out.println("📡 GET request to: " + url);

            HttpURLConnection conn = (HttpURLConnection) url.openConnection();

            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "application/json");
            conn.setConnectTimeout(5000); // 5 seconds
            conn.setReadTimeout(5000);    // 5 seconds
            int responseCode = conn.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                System.err.println("GET request failed! HTTP Error Code: " + responseCode);
                return jsonResponse;
            }

            try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                jsonResponse = JsonParser.parseReader(br).getAsJsonArray();
            }
            conn.disconnect();
        } catch (Exception e) {
            System.err.println("Error in GET request: " + e.getMessage());
            e.printStackTrace();
        }
        return jsonResponse;
    }

    // Method to send a POST request
    public static JsonObject sendPostRequest(String endpoint, JsonObject requestBody) {
        JsonObject jsonResponse = new JsonObject();
        try {
            URL url = new URL(BASE_URL + endpoint);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(gson.toJson(requestBody).getBytes());
                os.flush();
            }

            int responseCode = conn.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK && responseCode != HttpURLConnection.HTTP_CREATED) {
                System.err.println("POST request failed! HTTP Error Code: " + responseCode);
                return jsonResponse;
            }

            try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                jsonResponse = JsonParser.parseReader(br).getAsJsonObject();
            }
            conn.disconnect();
        } catch (Exception e) {
            System.err.println("Error in POST request: " + e.getMessage());
            e.printStackTrace();
        }
        return jsonResponse;
    }
    public static JsonObject sendDeleteRequest(String endpointWithParams) {
        try {
            URL url = new URL(BASE_URL + endpointWithParams);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("DELETE");
            conn.setRequestProperty("Content-Type", "application/json");

            int responseCode = conn.getResponseCode();
            if (responseCode == 200) {
                try (BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                    String line;
                    StringBuilder response = new StringBuilder();
                    while ((line = in.readLine()) != null) {
                        response.append(line);
                    }
                    return JsonParser.parseString(response.toString()).getAsJsonObject();
                }
            } else {
                System.out.println("❌ DELETE failed. Code: " + responseCode);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }



    // User Registration Request
    public static JsonObject registerUser(String username, String password) {
        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("action", "register");
        requestBody.addProperty("username", username);
        requestBody.addProperty("password", password);
        return sendPostRequest("/UserServlet", requestBody);
    }

    // User Login Request
    public static JsonObject loginUser(String username, String password) {
        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("action", "login");
        requestBody.addProperty("username", username);
        requestBody.addProperty("password", password);
        return sendPostRequest("/UserServlet", requestBody);
    }

    public static JsonArray filterExpenses(String username, String category, String startDate, String endDate, Double minAmount, Double maxAmount) {
        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("action", "filter");
        requestBody.addProperty("username", username);
        if (category != null && !category.isEmpty()) requestBody.addProperty("category", category);
        if (startDate != null && !startDate.isEmpty()) requestBody.addProperty("startDate", startDate);
        if (endDate != null && !endDate.isEmpty()) requestBody.addProperty("endDate", endDate);
        if (minAmount != null) requestBody.addProperty("minAmount", minAmount);
        if (maxAmount != null) requestBody.addProperty("maxAmount", maxAmount);

        JsonArray result = new JsonArray();
        try {
            URL url = new URL(BASE_URL + "/ExpenseServlet");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(gson.toJson(requestBody).getBytes());
                os.flush();
            }

            int responseCode = conn.getResponseCode();
            if (responseCode == 200) {
                try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                    result = JsonParser.parseReader(br).getAsJsonArray();
                }
            } else {
                System.out.println("❌ Filter request failed. Code: " + responseCode);
            }
            conn.disconnect();
        } catch (Exception e) {
            System.err.println("Error filtering expenses: " + e.getMessage());
            e.printStackTrace();
        }
        return result;
    }
}
