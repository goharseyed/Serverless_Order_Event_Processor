# Implementation Prompt: Serverless Order Event Processor

## Node.js Lambda Responsibilities

- Receive order event
- Validate payload (required fields: orderId, customerId, customerName, orderDate, items)
- Generate correlation ID (UUID v4)
- Add metadata (timestamp, source)
- Publish event to SNS with retry on failure
- Return success response with correlation ID

**Validation Rules:**
- Required fields must exist
- Strings cannot be empty
- Item quantity > 0
- Item price > 0
- Date must be valid ISO 8601 format

## Java Lambda Responsibilities

- Consume SQS messages (SQSEvent)
- Deserialize SNS notification payload
- Transform data: title-case customer name, normalize date format
- Calculate totalOrderValue (sum of item quantity × price)
- Create canonical order record
- Log canonical output as structured JSON

## Transformation Rules

- Convert `"john smith"` → `"John Smith"` (title case)
- Normalize date formats to ISO 8601
- Calculate `totalOrderValue` = sum(item.quantity × item.price)
- Set status to `"PROCESSED"`
- Preserve `correlationId` from metadata

## Canonical Model

```json
{
  "orderId": "",
  "customerId": "",
  "customerName": "",
  "orderDate": "",
  "totalOrderValue": 0,
  "status": "",
  "correlationId": ""
}
```

## Error Handling

- **Validation errors:** Reject invalid events, return 400 with error details
- **SNS failures:** Log error, retry once, return 500
- **SQS processing failures:** Throw exception, SQS auto-routes to DLQ after max receives
- **Unexpected errors:** Structured JSON logging with correlation ID tracking

## Logging

Structured JSON logs for: event received, validation passed, event published, message processed, transformation completed, processing failed.

## Testing Requirements

- **Node.js:** Jest — validator tests, SNS publisher tests, error handling tests
- **Java:** JUnit 5 + Mockito — transformation tests, canonical mapper tests, exception handling tests
- **Coverage goal:** 80%+

## CI Requirements

GitHub Actions workflow: install deps → build Node.js → build Java → run Jest → run JUnit. No deployment stage.
