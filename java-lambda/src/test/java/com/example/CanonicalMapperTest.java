package com.example;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CanonicalMapperTest {

    @Test
    void shouldMapAllCanonicalFields() {
        JsonObject order = createOrder(
            "ORD-001", "CUST-001", "john smith",
            "2025-01-15T10:30:00.000Z",
            "abc-123"
        );
        addItem(order, "ITM-1", 2, 29.99);
        addItem(order, "ITM-2", 1, 49.99);

        String result = CanonicalMapper.mapToCanonical(order);
        JsonObject canonical = JsonParser.parseString(result).getAsJsonObject();

        assertEquals("ORD-001", canonical.get("orderId").getAsString());
        assertEquals("CUST-001", canonical.get("customerId").getAsString());
        assertEquals("John Smith", canonical.get("customerName").getAsString());
        assertEquals("2025-01-15T10:30:00.000Z", canonical.get("orderDate").getAsString());
        assertEquals(109.97, canonical.get("totalOrderValue").getAsDouble(), 0.01);
        assertEquals("PROCESSED", canonical.get("status").getAsString());
        assertEquals("abc-123", canonical.get("correlationId").getAsString());
    }

    @Test
    void titleCaseShouldHandleMultipleWords() {
        JsonObject order = createOrder("ORD-001", "CUST-001", "john michael smith",
            "2025-01-15T10:30:00.000Z", "abc");
        addItem(order, "ITM-1", 1, 10.0);

        String result = CanonicalMapper.mapToCanonical(order);
        JsonObject canonical = JsonParser.parseString(result).getAsJsonObject();

        assertEquals("John Michael Smith", canonical.get("customerName").getAsString());
    }

    @Test
    void titleCaseShouldHandleAlreadyCapitalizedName() {
        JsonObject order = createOrder("ORD-001", "CUST-001", "JANE DOE",
            "2025-01-15T10:30:00.000Z", "abc");
        addItem(order, "ITM-1", 1, 10.0);

        String result = CanonicalMapper.mapToCanonical(order);
        JsonObject canonical = JsonParser.parseString(result).getAsJsonObject();

        assertEquals("Jane Doe", canonical.get("customerName").getAsString());
    }

    @Test
    void titleCaseShouldHandleExtraWhitespace() {
        JsonObject order = createOrder("ORD-001", "CUST-001", "  john   smith  ",
            "2025-01-15T10:30:00.000Z", "abc");
        addItem(order, "ITM-1", 1, 10.0);

        String result = CanonicalMapper.mapToCanonical(order);
        JsonObject canonical = JsonParser.parseString(result).getAsJsonObject();

        assertEquals("John Smith", canonical.get("customerName").getAsString());
    }

    @Test
    void titleCaseShouldHandleNullInput() {
        assertEquals("", CanonicalMapper.titleCase(null));
    }

    @Test
    void titleCaseShouldHandleBlankInput() {
        assertEquals("", CanonicalMapper.titleCase("   "));
    }

    @Test
    void normalizeDateShouldHandleNull() {
        assertEquals("", CanonicalMapper.normalizeDate(null));
    }

    @Test
    void normalizeDateShouldHandleBlank() {
        assertEquals("", CanonicalMapper.normalizeDate(""));
    }

    @Test
    void normalizeDateShouldReturnValidISO() {
        String result = CanonicalMapper.normalizeDate("2025-01-15T10:30:00Z");
        assertEquals("2025-01-15T10:30:00Z", result);
    }

    @Test
    void calculateTotalShouldSumItems() {
        JsonObject order = createOrder("ORD-001", "CUST-001", "john smith",
            "2025-01-15T10:30:00.000Z", "abc");
        addItem(order, "ITM-1", 3, 10.0);
        addItem(order, "ITM-2", 2, 5.5);

        assertEquals(41.0, CanonicalMapper.calculateTotal(order), 0.01);
    }

    @Test
    void calculateTotalShouldHandleEmptyItems() {
        JsonObject order = createOrder("ORD-001", "CUST-001", "john smith",
            "2025-01-15T10:30:00.000Z", "abc");

        assertEquals(0.0, CanonicalMapper.calculateTotal(order), 0.01);
    }

    @Test
    void shouldFallbackWhenCorrelationIdMissing() {
        JsonObject order = new JsonObject();
        order.addProperty("orderId", "ORD-001");
        order.addProperty("customerId", "CUST-001");
        order.addProperty("customerName", "test");
        order.addProperty("orderDate", "2025-01-15T10:30:00.000Z");
        order.add("items", new com.google.gson.JsonArray());

        String result = CanonicalMapper.mapToCanonical(order);
        JsonObject canonical = JsonParser.parseString(result).getAsJsonObject();

        assertEquals("", canonical.get("correlationId").getAsString());
    }

    @Test
    void shouldFallbackWhenMetadataNotAnObject() {
        JsonObject order = new JsonObject();
        order.addProperty("orderId", "ORD-001");
        order.addProperty("customerId", "CUST-001");
        order.addProperty("customerName", "test");
        order.addProperty("orderDate", "2025-01-15T10:30:00.000Z");
        order.addProperty("metadata", "not-an-object");
        order.add("items", new com.google.gson.JsonArray());

        String result = CanonicalMapper.mapToCanonical(order);
        JsonObject canonical = JsonParser.parseString(result).getAsJsonObject();

        assertEquals("", canonical.get("correlationId").getAsString());
    }

    private JsonObject createOrder(String orderId, String customerId, String customerName,
                                    String orderDate, String correlationId) {
        JsonObject order = new JsonObject();
        order.addProperty("orderId", orderId);
        order.addProperty("customerId", customerId);
        order.addProperty("customerName", customerName);
        order.addProperty("orderDate", orderDate);

        JsonObject metadata = new JsonObject();
        metadata.addProperty("correlationId", correlationId);
        order.add("metadata", metadata);

        order.add("items", new com.google.gson.JsonArray());
        return order;
    }

    private void addItem(JsonObject order, String itemId, double quantity, double price) {
        JsonObject item = new JsonObject();
        item.addProperty("itemId", itemId);
        item.addProperty("quantity", quantity);
        item.addProperty("price", price);
        order.getAsJsonArray("items").add(item);
    }
}
