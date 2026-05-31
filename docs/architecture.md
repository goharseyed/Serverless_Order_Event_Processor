# Architecture

## Diagram

```
                    +-----------------+
                    |  Order Event    |
                    |  (JSON payload) |
                    +--------+--------+
                             |
                             v
                    +-----------------+
                    |  Node.js Lambda |  ← Validates & enriches
                    |  (handler.js)   |     Generates correlationId
                    +--------+--------+     Publishes to SNS
                             |
                             v
                    +-----------------+
                    |   SNS Topic     |  ← Publish/Subscribe fan-out
                    | OrderEventsTopic|
                    +--------+--------+
                             |
                             v
                    +-----------------+
                    |   SQS Queue     |  ← Decoupled message buffer
                    | OrderQueue      |
                    +--------+--------+
                      |           |
                      v           v (after max receives)
            +-------------+  +-----------------+
            | Java Lambda |  | Dead Letter     |
            | (OrderHandler|  | Queue (DLQ)     |
            |  .java)     |  | OrderQueueDLQ   |
            +------+------+  +-----------------+
                   |
                   v
            +-----------------+
            | CloudWatch Logs|  ← Structured JSON logs
            +-----------------+
```

## Components

### Node.js Lambda (`handler.js`)
- **Entry point** for order events
- Calls `validator.js` to enforce required fields and business rules
- Uses `snsPublisher.js` to publish validated, enriched events
- Uses AWS SDK v3 for SNS integration
- Generates UUID v4 correlation IDs

### SNS Topic (`OrderEventsTopic`)
- Decouples the publisher from consumers
- Enables future multi-subscriber scenarios (fan-out)
- Forwards messages to the SQS subscription

### SQS Queue (`OrderQueue`)
- Buffers messages for reliable processing
- Configured with a Dead Letter Queue for poison messages
- Max receive count triggers DLQ routing

### Java Lambda (`OrderHandler.java`)
- Consumes SQS messages as batch
- Calls `CanonicalMapper.java` to normalize and standardize data
- Logs canonical records as structured JSON

### Dead Letter Queue (`OrderQueueDLQ`)
- Automatically captures messages that exceed max receive attempts
- Provides operational visibility into processing failures

### CloudWatch Logs
- Captures all structured JSON log output from both Lambdas
- Enables correlation ID tracing across services

### IAM Roles
- Node.js Lambda: permission to publish to SNS
- Java Lambda: permission to read from SQS and write to CloudWatch Logs
