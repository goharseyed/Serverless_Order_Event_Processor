import { validateOrder } from "./validator.js";
import { publishToSns } from "./snsPublisher.js";
import { v4 as uuidv4 } from "uuid";

function log(level, message, data = {}) {
  console.log(JSON.stringify({ level, timestamp: new Date().toISOString(), ...data, message }));
}

export async function handler(event) {
  const correlationId = uuidv4();

  log("INFO", "Event received", { correlationId });

  try {
    const payload = typeof event === "string" ? JSON.parse(event) : event;

    validateOrder(payload);

    const enriched = {
      ...payload,
      metadata: {
        correlationId,
        receivedAt: new Date().toISOString(),
        source: "order-validator-lambda"
      }
    };

    log("INFO", "Validation passed", { correlationId, orderId: enriched.orderId });

    const snsTopicArn = process.env.SNS_TOPIC_ARN;

    await publishToSns(snsTopicArn, enriched, correlationId);

    log("INFO", "Event published", { correlationId, orderId: enriched.orderId });

    return {
      statusCode: 200,
      body: JSON.stringify({
        message: "Order event validated and published",
        correlationId
      })
    };
  } catch (error) {
    if (error.name === "ValidationError") {
      log("ERROR", "Validation failed", { correlationId, error: error.message, details: error.details });
      return {
        statusCode: 400,
        body: JSON.stringify({
          message: "Validation failed",
          correlationId,
          errors: error.details
        })
      };
    }

    log("ERROR", "Processing failed", { correlationId, error: error.message });
    return {
      statusCode: 500,
      body: JSON.stringify({
        message: "Internal error",
        correlationId
      })
    };
  }
}
