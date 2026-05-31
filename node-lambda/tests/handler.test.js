import { describe, it, expect, jest, beforeEach } from "@jest/globals";

const mockPublish = jest.fn();
jest.unstable_mockModule("../src/snsPublisher.js", () => ({
  publishToSns: mockPublish
}));

const { handler } = await import("../src/handler.js");

describe("handler", () => {
  const validEvent = {
    orderId: "ORD-001",
    customerId: "CUST-001",
    customerName: "John Smith",
    orderDate: "2025-01-15T10:30:00.000Z",
    items: [
      { itemId: "ITEM-001", quantity: 2, price: 29.99 }
    ]
  };

  beforeEach(() => {
    mockPublish.mockReset();
    process.env.SNS_TOPIC_ARN = "arn:aws:sns:us-east-1:123456789012:order-events";
  });

  it("should validate and publish a valid event", async () => {
    mockPublish.mockResolvedValueOnce(undefined);

    const result = await handler(validEvent);

    expect(result.statusCode).toBe(200);
    const body = JSON.parse(result.body);
    expect(body.message).toContain("validated and published");
    expect(body.correlationId).toBeDefined();
    expect(mockPublish).toHaveBeenCalledTimes(1);
  });

  it("should produce unique correlation IDs per invocation", async () => {
    mockPublish.mockResolvedValue(undefined);

    const results = await Promise.all([handler(validEvent), handler(validEvent)]);

    const id1 = JSON.parse(results[0].body).correlationId;
    const id2 = JSON.parse(results[1].body).correlationId;
    expect(id1).not.toBe(id2);
  });

  it("should handle a JSON string input", async () => {
    mockPublish.mockResolvedValueOnce(undefined);

    const result = await handler(JSON.stringify(validEvent));

    expect(result.statusCode).toBe(200);
    expect(mockPublish).toHaveBeenCalledTimes(1);
  });

  it("should return 400 for missing required fields", async () => {
    const result = await handler({ orderId: "ORD-002" });

    expect(result.statusCode).toBe(400);
    expect(JSON.parse(result.body).correlationId).toBeDefined();
    expect(mockPublish).not.toHaveBeenCalled();
  });

  it("should return 400 for empty customerName", async () => {
    const result = await handler({ ...validEvent, customerName: "" });

    expect(result.statusCode).toBe(400);
    expect(mockPublish).not.toHaveBeenCalled();
  });

  it("should return 400 for invalid date", async () => {
    const result = await handler({ ...validEvent, orderDate: "not-a-date" });

    expect(result.statusCode).toBe(400);
    expect(mockPublish).not.toHaveBeenCalled();
  });

  it("should return 400 for items with zero quantity", async () => {
    const result = await handler({
      ...validEvent,
      items: [{ itemId: "X", quantity: 0, price: 10 }]
    });

    expect(result.statusCode).toBe(400);
    expect(mockPublish).not.toHaveBeenCalled();
  });

  it("should return 500 when SNS publish fails after retry", async () => {
    mockPublish.mockRejectedValueOnce(new Error("SNS failure"));

    const result = await handler(validEvent);

    expect(result.statusCode).toBe(500);
    const body = JSON.parse(result.body);
    expect(body.correlationId).toBeDefined();
  });
});
