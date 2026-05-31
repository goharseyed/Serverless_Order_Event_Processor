import { describe, it, expect, jest, beforeEach } from "@jest/globals";

const mockSend = jest.fn();
jest.unstable_mockModule("@aws-sdk/client-sns", () => ({
  SNSClient: jest.fn(() => ({ send: mockSend })),
  PublishCommand: jest.fn((params) => params)
}));

const { publishToSns } = await import("../src/snsPublisher.js");

describe("publishToSns", () => {
  const topicArn = "arn:aws:sns:us-east-1:123456789012:test-topic";
  const message = { orderId: "ORD-001" };
  const correlationId = "abc-123";

  beforeEach(() => {
    mockSend.mockReset();
  });

  it("should publish to SNS successfully", async () => {
    mockSend.mockResolvedValueOnce({ MessageId: "msg-001" });

    await expect(publishToSns(topicArn, message, correlationId)).resolves.toBeUndefined();
    expect(mockSend).toHaveBeenCalledTimes(1);
  });

  it("should retry once on first failure", async () => {
    mockSend
      .mockRejectedValueOnce(new Error("Network error"))
      .mockResolvedValueOnce({ MessageId: "msg-002" });

    await expect(publishToSns(topicArn, message, correlationId)).resolves.toBeUndefined();
    expect(mockSend).toHaveBeenCalledTimes(2);
  });

  it("should throw after retry also fails", async () => {
    mockSend
      .mockRejectedValueOnce(new Error("Network error"))
      .mockRejectedValueOnce(new Error("Network error again"));

    await expect(publishToSns(topicArn, message, correlationId)).rejects.toThrow(
      "Failed to publish to SNS after retry"
    );
    expect(mockSend).toHaveBeenCalledTimes(2);
  });
});
