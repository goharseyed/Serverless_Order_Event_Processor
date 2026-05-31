package com.example;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class OrderHandler implements RequestHandler<SQSEvent, Void> {

    private static final Gson gson = new Gson();

    @Override
    public Void handleRequest(SQSEvent event, Context context) {
        for (SQSEvent.SQSMessage message : event.getRecords()) {
            try {
                String body = message.getBody();
                JsonObject snsPayload = JsonParser.parseString(body).getAsJsonObject();

                String messageContent;
                if (snsPayload.has("Message")) {
                    messageContent = snsPayload.get("Message").getAsString();
                } else {
                    messageContent = body;
                }

                JsonObject orderJson = JsonParser.parseString(messageContent).getAsJsonObject();

                String correlationId = "unknown";
                if (orderJson.has("metadata")
                        && orderJson.getAsJsonObject("metadata").has("correlationId")) {
                    correlationId = orderJson.getAsJsonObject("metadata").get("correlationId").getAsString();
                }

                log("INFO", "Message processed", correlationId,
                        orderJson.has("orderId") ? orderJson.get("orderId").getAsString() : "unknown");

                String canonicalRecord = CanonicalMapper.mapToCanonical(orderJson);

                log("INFO", "Transformation completed", correlationId,
                        orderJson.has("orderId") ? orderJson.get("orderId").getAsString() : "unknown");

                context.getLogger().log("CANONICAL_RECORD: " + canonicalRecord + "\n");

            } catch (Exception e) {
                log("ERROR", "Processing failed", "unknown", "unknown");
                context.getLogger().log("ERROR: " + e.getMessage() + "\n");
                throw new RuntimeException("Failed to process SQS message", e);
            }
        }
        return null;
    }

    private void log(String level, String message, String correlationId, String orderId) {
        JsonObject logEntry = new JsonObject();
        logEntry.addProperty("level", level);
        logEntry.addProperty("timestamp", java.time.Instant.now().toString());
        logEntry.addProperty("message", message);
        logEntry.addProperty("correlationId", correlationId);
        logEntry.addProperty("orderId", orderId);
        System.out.println(gson.toJson(logEntry));
    }
}
