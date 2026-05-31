export class ValidationError extends Error {
  constructor(message, details) {
    super(message);
    this.name = "ValidationError";
    this.details = details;
  }
}

function isValidISODate(dateString) {
  if (typeof dateString !== "string") return false;
  const date = new Date(dateString);
  return !isNaN(date.getTime()) && date.toISOString() === new Date(dateString).toISOString();
}

export function validateOrder(order) {
  const errors = [];

  if (!order) {
    throw new ValidationError("Order object is required", ["Order object is missing"]);
  }

  const requiredFields = ["orderId", "customerId", "customerName", "orderDate", "items"];

  for (const field of requiredFields) {
    if (!(field in order)) {
      errors.push(`Missing required field: ${field}`);
    } else if (typeof order[field] === "string" && order[field].trim() === "") {
      errors.push(`Field cannot be empty: ${field}`);
    }
  }

  if (order.orderDate && !isValidISODate(order.orderDate)) {
    errors.push("orderDate must be a valid ISO 8601 date");
  }

  if (order.items && Array.isArray(order.items)) {
    if (order.items.length === 0) {
      errors.push("items array cannot be empty");
    } else {
      for (let i = 0; i < order.items.length; i++) {
        const item = order.items[i];
        if (typeof item.quantity !== "number" || item.quantity <= 0) {
          errors.push(`items[${i}].quantity must be > 0`);
        }
        if (typeof item.price !== "number" || item.price <= 0) {
          errors.push(`items[${i}].price must be > 0`);
        }
      }
    }
  } else if (order.items !== undefined) {
    errors.push("items must be an array");
  }

  if (errors.length > 0) {
    throw new ValidationError("Order validation failed", errors);
  }

  return true;
}
