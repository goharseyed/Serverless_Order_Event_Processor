# Serverless Order Event Processor

A minimal serverless integration solution that processes customer order events using AWS Lambda, SNS, SQS, and DLQ.

## Architecture

```
Node.js Lambda (Validator)  →  SNS Topic  →  SQS Queue  →  Java Lambda (Processor)  →  CloudWatch Logs
                                                              ↓
                                                     Dead Letter Queue (DLQ)
```

## Prerequisites

- [AWS CLI](https://aws.amazon.com/cli/) installed and configured
- [AWS SAM CLI](https://docs.aws.amazon.com/serverless-application-model/latest/developerguide/install-sam-cli.html) installed
- Node.js 22+
- Java 21
- Maven 3.9+

## Project Structure

```
serverless-order-event-processor/
├── .github/workflows/ci.yml              # CI pipeline
├── docs/                                  # Documentation
│   ├── architecture.md
│   ├── project-idea.md
│   └── implementation-prompt.md
├── infrastructure/template.yaml           # AWS SAM template
├── node-lambda/                           # Node.js Lambda (validator)
│   ├── src/
│   │   ├── handler.js
│   │   ├── validator.js
│   │   └── snsPublisher.js
│   └── tests/
├── java-lambda/                           # Java Lambda (processor)
│   ├── src/main/java/com/example/
│   │   ├── OrderHandler.java
│   │   └── CanonicalMapper.java
│   └── src/test/java/com/example/
├── samples/
│   ├── valid-order.json
│   └── invalid-order.json
└── README.md
```

## Setup

### Node.js Lambda

```bash
cd node-lambda
npm install
```

### Java Lambda

```bash
cd java-lambda
mvn compile
```

## Running Tests

### Node.js Tests (Jest)

```bash
cd node-lambda
npm test
```

### Java Tests (JUnit 5 + Mockito)

```bash
cd java-lambda
mvn test
```

## Deploying with SAM

```bash
cd infrastructure
sam build
sam deploy --guided
```

After deployment, invoke the Node.js Lambda with a test event:

```bash
aws lambda invoke \
  --function-name order-validator-dev \
  --payload file://../samples/valid-order.json \
  response.json
```

## Example Payloads

### Valid Order

```json
{
  "orderId": "ORD-001",
  "customerId": "CUST-001",
  "customerName": "john smith",
  "orderDate": "2025-01-15T10:30:00.000Z",
  "items": [
    { "itemId": "ITEM-001", "quantity": 2, "price": 29.99 }
  ]
}
```

### Invalid Order (missing required fields, invalid values)

```json
{
  "orderId": "",
  "customerId": "CUST-002",
  "orderDate": "not-a-valid-date",
  "items": [
    { "itemId": "ITEM-003", "quantity": 0, "price": -5.00 }
  ]
}
```

## Canonical Output

The Java Lambda transforms and normalizes orders into the canonical format:

```json
{
  "orderId": "ORD-001",
  "customerId": "CUST-001",
  "customerName": "John Smith",
  "orderDate": "2025-01-15T10:30:00.000Z",
  "totalOrderValue": 109.97,
  "status": "PROCESSED",
  "correlationId": "abc-123"
}
```

## Troubleshooting

- **Node.js Lambda returns 400:** Check validation errors in the response body. Ensure all required fields are present and valid.
- **Messages not reaching Java Lambda:** Verify the SNS → SQS subscription is active. Check CloudWatch logs for the Node.js Lambda.
- **Messages in DLQ:** Check the DLQ in the AWS console. Messages arrive here after 3 failed processing attempts by the Java Lambda.
- **Test failures:** Ensure Node.js 22+ and Java 21 are installed. Run `npm install` and `mvn clean compile` before testing.
