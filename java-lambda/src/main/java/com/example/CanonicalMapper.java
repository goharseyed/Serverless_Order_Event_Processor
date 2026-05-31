package com.example;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class CanonicalMapper {

    private static final Gson gson = new Gson();

    public static String mapToCanonical(JsonObject orderJson) {
        JsonObject canonical = new JsonObject();

        canonical.addProperty("orderId", getString(orderJson, "orderId"));
        canonical.addProperty("customerId", getString(orderJson, "customerId"));
        canonical.addProperty("customerName", titleCase(getString(orderJson, "customerName")));
        canonical.addProperty("orderDate", normalizeDate(getString(orderJson, "orderDate")));
        canonical.addProperty("totalOrderValue", calculateTotal(orderJson));
        canonical.addProperty("status", "PROCESSED");
        canonical.addProperty("correlationId", extractCorrelationId(orderJson));

        return gson.toJson(canonical);
    }

    private static String getString(JsonObject obj, String key) {
        if (obj.has(key) && !obj.get(key).isJsonNull()) {
            return obj.get(key).getAsString();
        }
        return "";
    }

    static String titleCase(String input) {
        if (input == null || input.isBlank()) {
            return "";
        }
        String trimmed = input.trim().toLowerCase();
        String[] words = trimmed.split("\\s+");
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < words.length; i++) {
            if (i > 0) result.append(" ");
            if (!words[i].isEmpty()) {
                result.append(Character.toUpperCase(words[i].charAt(0)));
                if (words[i].length() > 1) {
                    result.append(words[i].substring(1));
                }
            }
        }
        return result.toString();
    }

    static String normalizeDate(String input) {
        if (input == null || input.isBlank()) {
            return "";
        }
        try {
            Instant instant = Instant.parse(input);
            return DateTimeFormatter.ISO_INSTANT.format(instant);
        } catch (DateTimeParseException e) {
            try {
                Instant instant = Instant.parse(input.replace(" ", "T") + "Z");
                return DateTimeFormatter.ISO_INSTANT.format(instant);
            } catch (DateTimeParseException e2) {
                return input;
            }
        }
    }

    static double calculateTotal(JsonObject orderJson) {
        if (!orderJson.has("items") || orderJson.get("items").isJsonNull()) {
            return 0.0;
        }
        JsonElement itemsElement = orderJson.get("items");
        if (!itemsElement.isJsonArray()) {
            return 0.0;
        }
        JsonArray items = itemsElement.getAsJsonArray();
        double total = 0.0;
        for (JsonElement itemElement : items) {
            JsonObject item = itemElement.getAsJsonObject();
            double quantity = item.has("quantity") ? item.get("quantity").getAsDouble() : 0.0;
            double price = item.has("price") ? item.get("price").getAsDouble() : 0.0;
            total += quantity * price;
        }
        return Math.round(total * 100.0) / 100.0;
    }

    private static String extractCorrelationId(JsonObject orderJson) {
        if (orderJson.has("metadata")
                && orderJson.get("metadata").isJsonObject()
                && orderJson.getAsJsonObject("metadata").has("correlationId")) {
            return orderJson.getAsJsonObject("metadata").get("correlationId").getAsString();
        }
        return "";
    }
}
