# Project Idea: Serverless Order Event Processor

## Business Problem

A retail company receives customer order events from external systems. Incoming order data may contain inconsistent formats and must be standardized before downstream systems can consume it.

The company requires a lightweight integration solution that:

- Validates incoming order events
- Publishes events asynchronously
- Processes messages through a queue
- Normalizes customer and order data
- Converts records into a canonical company format
- Handles failures safely
- Provides operational visibility

## System Flow

1. A Node.js Lambda receives a customer order event.
2. The Node.js Lambda validates required fields.
3. The Node.js Lambda enriches the event with metadata and a correlation ID.
4. The Node.js Lambda publishes the event to SNS.
5. SNS forwards the event to SQS.
6. A Java Lambda consumes messages from SQS.
7. The Java Lambda transforms and normalizes the data into the canonical model.
8. The Java Lambda logs the processed canonical record.
9. Failed messages are automatically routed to a Dead Letter Queue.
10. CloudWatch logs capture processing activity and failures.

## Success Criteria

A user can:

- Submit a sample order event
- Validate incoming data
- Publish events through SNS
- Observe messages in SQS
- Process messages using Java Lambda
- View transformed canonical records
- Trigger DLQ behavior with invalid data
- View logs in CloudWatch
- Run automated tests
- Run CI pipeline through GitHub Actions
