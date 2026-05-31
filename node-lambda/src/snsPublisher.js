import { SNSClient, PublishCommand } from "@aws-sdk/client-sns";

const snsClient = new SNSClient({ region: process.env.AWS_REGION || "us-east-1" });

export async function publishToSns(topicArn, message, correlationId) {
  try {
    await snsClient.send(
      new PublishCommand({
        TopicArn: topicArn,
        Message: JSON.stringify(message),
        MessageAttributes: {
          correlationId: {
            DataType: "String",
            StringValue: correlationId
          }
        }
      })
    );
  } catch (error) {
    console.log(
      JSON.stringify({
        level: "ERROR",
        message: "SNS publish failed, retrying",
        correlationId,
        error: error.message,
        timestamp: new Date().toISOString()
      })
    );

    try {
      await snsClient.send(
        new PublishCommand({
          TopicArn: topicArn,
          Message: JSON.stringify(message),
          MessageAttributes: {
            correlationId: {
              DataType: "String",
              StringValue: correlationId
            }
          }
        })
      );
    } catch (retryError) {
      console.log(
        JSON.stringify({
          level: "ERROR",
          message: "SNS publish failed after retry",
          correlationId,
          error: retryError.message,
          timestamp: new Date().toISOString()
        })
      );
      throw new Error(`Failed to publish to SNS after retry: ${retryError.message}`);
    }
  }
}
