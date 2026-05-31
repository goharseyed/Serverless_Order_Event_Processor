import { describe, it, expect } from "@jest/globals";
import { validateOrder, ValidationError } from "../src/validator.js";

describe("validateOrder", () => {
  const validOrder = {
    orderId: "ORD-001",
    customerId: "CUST-001",
    customerName: "John Smith",
    orderDate: "2025-01-15T10:30:00.000Z",
    items: [
      { itemId: "ITEM-001", quantity: 2, price: 29.99 }
    ]
  };

  it("should pass for a valid order", () => {
    expect(validateOrder(validOrder)).toBe(true);
  });

  it("should throw for a null order", () => {
    expect(() => validateOrder(null)).toThrow(ValidationError);
  });

  it("should throw for an undefined order", () => {
    expect(() => validateOrder(undefined)).toThrow(ValidationError);
  });

  it("should throw when orderId is missing", () => {
    const { orderId, ...rest } = validOrder;
    expect(() => validateOrder(rest)).toThrow(ValidationError);
  });

  it("should throw when customerId is missing", () => {
    const { customerId, ...rest } = validOrder;
    expect(() => validateOrder(rest)).toThrow(ValidationError);
  });

  it("should throw when customerName is missing", () => {
    const { customerName, ...rest } = validOrder;
    expect(() => validateOrder(rest)).toThrow(ValidationError);
  });

  it("should throw when orderDate is missing", () => {
    const { orderDate, ...rest } = validOrder;
    expect(() => validateOrder(rest)).toThrow(ValidationError);
  });

  it("should throw when items is missing", () => {
    const { items, ...rest } = validOrder;
    expect(() => validateOrder(rest)).toThrow(ValidationError);
  });

  it("should throw when a required string field is empty", () => {
    expect(() => validateOrder({ ...validOrder, orderId: "" })).toThrow(ValidationError);
    expect(() => validateOrder({ ...validOrder, customerName: "  " })).toThrow(ValidationError);
  });

  it("should throw when orderDate is not valid ISO 8601", () => {
    expect(() => validateOrder({ ...validOrder, orderDate: "not-a-date" })).toThrow(ValidationError);
    expect(() => validateOrder({ ...validOrder, orderDate: "2025-13-01" })).toThrow(ValidationError);
  });

  it("should throw when items is not an array", () => {
    expect(() => validateOrder({ ...validOrder, items: "not-an-array" })).toThrow(ValidationError);
  });

  it("should throw when items array is empty", () => {
    expect(() => validateOrder({ ...validOrder, items: [] })).toThrow(ValidationError);
  });

  it("should throw when item quantity is <= 0", () => {
    expect(() => validateOrder({
      ...validOrder,
      items: [{ itemId: "X", quantity: 0, price: 10 }]
    })).toThrow(ValidationError);

    expect(() => validateOrder({
      ...validOrder,
      items: [{ itemId: "X", quantity: -1, price: 10 }]
    })).toThrow(ValidationError);
  });

  it("should throw when item price is <= 0", () => {
    expect(() => validateOrder({
      ...validOrder,
      items: [{ itemId: "X", quantity: 1, price: 0 }]
    })).toThrow(ValidationError);

    expect(() => validateOrder({
      ...validOrder,
      items: [{ itemId: "X", quantity: 1, price: -5 }]
    })).toThrow(ValidationError);
  });

  it("should include all validation errors in details", () => {
    try {
      validateOrder({ ...validOrder, orderId: "", orderDate: "bad" });
    } catch (error) {
      expect(error.details).toContain("Field cannot be empty: orderId");
      expect(error.details).toContain("orderDate must be a valid ISO 8601 date");
    }
  });
});
