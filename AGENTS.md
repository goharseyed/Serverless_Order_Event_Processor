# AGENTS.md — Serverless Order Event Processor

**Purpose:** A minimal serverless integration solution that processes customer order events using AWS Lambda, SNS, SQS, and DLQ.

## Architecture Summary (5-step flow)

1. Node.js Lambda receives order event → validates
2. Node.js Lambda enriches with correlation ID → publishes to SNS
3. SNS → SQS (decoupled messaging)
4. Java Lambda consumes SQS messages → transforms/normalizes → logs canonical record
5. Failures → DLQ (Dead Letter Queue)

## Technology Stack

- **Languages:** Node.js 22+, Java 21
- **AWS Services:** Lambda, SNS, SQS, DLQ, CloudWatch Logs, IAM
- **Testing:** Jest (Node.js), JUnit 5 + Mockito (Java)
- **DevOps:** GitHub Actions CI
- **Infrastructure:** AWS SAM

## Canonical Model Fields

`orderId`, `customerId`, `customerName`, `orderDate`, `totalOrderValue`, `status`, `correlationId`

## Scope Boundaries

**In scope:** Lambda, SNS, SQS, DLQ, CloudWatch, IAM, Node.js Lambda (validation/publish), Java Lambda (transform/normalize), unit tests, CI pipeline
**Out of scope:** API Gateway, DynamoDB, S3, Step Functions, EventBridge, authentication, deployment, Docker, monitoring dashboards, DB persistence, extra Lambda functions, extra fields/rules

## Repository Structure

```
├── .github/workflows/ci.yml
├── docs/ (architecture, project-idea, implementation-prompt)
├── infrastructure/template.yaml
├── node-lambda/ (src: handler, validator, snsPublisher; tests)
├── java-lambda/ (src: OrderHandler, CanonicalMapper; tests)
├── samples/ (valid-order.json, invalid-order.json)
└── README.md
```
