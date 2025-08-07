package com.expensetracker.frontend;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import com.google.gson.*;
import java.util.Objects;
import org.jdatepicker.impl.*;
import java.text.SimpleDateFormat;
import java.util.Properties;
import java.nio.charset.StandardCharsets;
import java.net.URLEncoder;

public class ExpenseTrackerUI {
    private static String username = "";
    private static final Gson gson = new Gson();

    public static void main(String[] args) {
        SwingUtilities.invokeLater(ExpenseTrackerUI::showLoginScreen);
    }
    private static String encode(String value) {
        try {
            return URLEncoder.encode(value, StandardCharsets.UTF_8.toString());
        } catch (Exception e) {
            return value;
        }
    }
    private static void showLoginScreen() {
        JFrame loginFrame = new JFrame("Login / Register");
        loginFrame.setSize(350, 200);
        loginFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        JPanel panel = new JPanel(new GridLayout(4, 2, 10, 10));
        loginFrame.add(panel);

        JTextField userText = new JTextField(15);
        JPasswordField passwordText = new JPasswordField(15);
        JButton loginButton = new JButton("Login");
        JButton registerButton = new JButton("Register");

        panel.add(new JLabel("Username:"));
        panel.add(userText);
        panel.add(new JLabel("Password:"));
        panel.add(passwordText);
        panel.add(loginButton);
        panel.add(registerButton);

        loginButton.addActionListener(e -> {
            username = userText.getText().trim();
            String password = new String(passwordText.getPassword()).trim();

            if (authenticateUser(username, password)) {
                loginFrame.dispose();
                showMainUI();
            } else {
                JOptionPane.showMessageDialog(loginFrame, "❌ Invalid credentials", "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        registerButton.addActionListener(e -> {
            String registerUsername = userText.getText().trim();
            String registerPassword = new String(passwordText.getPassword()).trim();

            if (registerUser(registerUsername, registerPassword)) {
                JOptionPane.showMessageDialog(loginFrame, "✅ User Registered! Please Login.", "Success", JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(loginFrame, "❌ Registration Failed!", "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        loginFrame.setLocationRelativeTo(null);
        loginFrame.setVisible(true);
    }

    private static boolean authenticateUser(String username, String password) {
        JsonObject json = new JsonObject();
        json.addProperty("action", "login");
        json.addProperty("username", username);
        json.addProperty("password", password);

        JsonObject response = HttpClient.sendPostRequest("/UserServlet", json);
        return response != null && "Login successful".equalsIgnoreCase(response.get("message").getAsString());
    }

    private static boolean registerUser(String username, String password) {
        JsonObject json = new JsonObject();
        json.addProperty("action", "register");
        json.addProperty("username", username);
        json.addProperty("password", password);

        JsonObject response = HttpClient.sendPostRequest("/UserServlet", json);
        return response != null && response.get("message").getAsString().toLowerCase().contains("registered");
    }
    private static void refreshTable(DefaultTableModel tableModel, String username) {
        JsonArray response = HttpClient.sendGetRequest("/expense?username=" + username);
        tableModel.setRowCount(0); // clear old data
        if (response == null || response.isEmpty() ) {
            JOptionPane.showMessageDialog(null, "No expenses found.");
            return;
        }

        response.forEach(item -> {
            JsonObject obj = item.getAsJsonObject();
            tableModel.addRow(new Object[]{
                    obj.get("amount").getAsDouble(),
                    obj.get("category").getAsString(),
                    obj.get("date").getAsString(),
                    obj.get("description").getAsString()
            });
        });
    }
    private static void showMainUI() {
        JFrame frame = new JFrame("Expense Tracker - Welcome " + username);
        frame.setSize(800, 600);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());

        // Top Input Panel
        JPanel inputPanel = new JPanel(new GridBagLayout());
        inputPanel.setBackground(new Color(224, 255, 255));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 8, 5, 8);
        gbc.anchor = GridBagConstraints.WEST;

        JTextField amountField = new JTextField(10);
        String[] categories = {"Food", "Transport", "Shopping", "Utilities", "Health", "Entertainment", "Education", "Others"};
        JComboBox<String> categoryDropdown = new JComboBox<>(categories);
        UtilDateModel model = new UtilDateModel();
        Properties p = new Properties();
        p.put("text.today", "Today");
        p.put("text.month", "Month");
        p.put("text.year", "Year");
        JDatePanelImpl datePanel = new JDatePanelImpl(model, p);
        JDatePickerImpl datePicker = new JDatePickerImpl(datePanel, new DateLabelFormatter());

        JTextField descField = new JTextField(10);

        String[] labels = {"Amount:", "Category:", "Date:", "Description:"};
        JTextField[] fields = {amountField, descField};

        for (int i = 0; i < labels.length; i++) {
            gbc.gridx = 0;
            gbc.gridy = i;
            inputPanel.add(new JLabel(labels[i]), gbc);
            gbc.gridx = 1;
//            if (labels[i].equals("Category:")) {
//                inputPanel.add(categoryDropdown, gbc);
//            }
//
//            if (labels[i].equals("Date:")) {
//                inputPanel.add(datePicker, gbc);
//            } else {
//                inputPanel.add(fields[i < 2 ? i : i - 1], gbc); // adjust index
//            }
            switch (labels[i]) {
                case "Category:":
                    inputPanel.add(categoryDropdown, gbc);
                    break;
                case "Date:":
                    inputPanel.add(datePicker, gbc);
                    break;
                case "Amount:":
                    inputPanel.add(fields[0], gbc);
                    break;
                case "Description:":
                    inputPanel.add(fields[1], gbc);
                    break;
            }
        }

        JButton addButton = new JButton("➕ Add Expense");
        JButton filterButton = new JButton("Filter By Category");
        JButton deleteButton = new JButton("❌ Delete Expense");

        JPanel buttonPanel = new JPanel();
        buttonPanel.add(addButton);
        buttonPanel.add(filterButton);
        buttonPanel.add(deleteButton);

        gbc.gridx = 0;
        gbc.gridy = labels.length;
        gbc.gridwidth = 2;
        inputPanel.add(buttonPanel, gbc);

        // Bottom Table Panel
        String[] columnNames = {"Amount", "Category", "Date", "Description"};
        DefaultTableModel tableModel = new DefaultTableModel(columnNames, 0);
        JTable table = new JTable(tableModel);
        JScrollPane tableScrollPane = new JScrollPane(table);

        // Add to frame
        frame.add(inputPanel, BorderLayout.NORTH);
        frame.add(tableScrollPane, BorderLayout.CENTER);

        // Button Logic
        addButton.addActionListener(e -> {
            JsonObject expense = new JsonObject();
            expense.addProperty("user", username);
            try {
                expense.addProperty("amount", Double.parseDouble(amountField.getText()));
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(frame, "❌ Enter valid amount!", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            expense.addProperty("category", categoryDropdown.getSelectedItem().toString().toLowerCase());
            Object selectedDate = datePicker.getModel().getValue();
            if (selectedDate == null) {
                JOptionPane.showMessageDialog(frame, "❌ Please select a date!", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            String formattedDate = new SimpleDateFormat("yyyy-MM-dd").format((java.util.Date) selectedDate);
            expense.addProperty("date", formattedDate);

            expense.addProperty("description", descField.getText());

            JsonObject res = HttpClient.sendPostRequest("/expense", expense);
            if (res != null && res.has("message")) {
                JOptionPane.showMessageDialog(frame, res.get("message").getAsString());
                refreshTable(tableModel, username); // refresh table
            } else {
                JOptionPane.showMessageDialog(frame, "❌ Failed to add expense.");
            }
        });



        deleteButton.addActionListener(e -> {
            int selectedRow = table.getSelectedRow();
            if (selectedRow == -1) {
                JOptionPane.showMessageDialog(frame, "❌ Please select a row to delete.");
                return;
            }

            String category = Objects.toString(table.getValueAt(selectedRow, 1));
            String date = Objects.toString(table.getValueAt(selectedRow, 2));
            String desc = Objects.toString(table.getValueAt(selectedRow, 3));

            int confirm = JOptionPane.showConfirmDialog(frame,
                    "Delete expense:\n" + category + " | " + date + " | " + desc + "?",
                    "Confirm Deletion", JOptionPane.YES_NO_OPTION);

            if (confirm == JOptionPane.YES_OPTION) {
                // Construct DELETE URL
                String url = "/expense?username=" + username +
                        "&category=" + encode(category) +
                        "&date=" + encode(date) +
                        "&description=" + encode(desc);

                JsonObject response = HttpClient.sendDeleteRequest(url);
                if (response != null && response.get("message").getAsString().contains("deleted")) {
                    JOptionPane.showMessageDialog(frame, response.get("message").getAsString());
                    tableModel.removeRow(selectedRow);
                } else {
                    JOptionPane.showMessageDialog(frame, "❌ Failed to delete expense.");
                }
            }
        });
        filterButton.addActionListener(e -> {
            String selectedCategory = categoryDropdown.getSelectedItem().toString().toLowerCase();
            System.out.println("🔍 Selected Category: " + selectedCategory);
            JsonArray response = HttpClient.sendGetRequest(
                    "/expense?username=" + encode(username) + "&category=" + encode(selectedCategory)
            );

            tableModel.setRowCount(0); // clear table first

            if (response == null || response.size() == 0) {
                JOptionPane.showMessageDialog(frame, "No expenses found for category: " + selectedCategory);
                return;
            }

            response.forEach(item -> {
                JsonObject obj = item.getAsJsonObject();
                tableModel.addRow(new Object[]{
                        obj.get("amount").getAsDouble(),
                        obj.get("category").getAsString().toLowerCase(),
                        obj.get("date").getAsString(),
                        obj.get("description").getAsString()
                });
            });
        });



        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
}
