import type { ApiError, OrderItemInput, Product } from "../types/models";
import { isOliveProductType, translateBackendMessage } from "./utils";

export type FieldErrors = Record<string, string>;

export function getApiErrorMessage(error: unknown): string {
  if (typeof error === "string") {
    return error;
  }

  if (error && typeof error === "object" && "message" in error) {
    return translateBackendMessage(String((error as { message?: unknown }).message ?? "Request failed."));
  }

  return translateBackendMessage("Request failed.");
}

export function getApiFieldErrors(error: unknown): FieldErrors {
  if (!error || typeof error !== "object" || !("errors" in error)) {
    return {};
  }

  const apiError = error as ApiError;
  const result: FieldErrors = {};

  for (const item of apiError.errors ?? []) {
    if (item.field && item.message) {
      result[item.field] = translateBackendMessage(item.message);
    }
  }

  return result;
}

export function validateLogin(values: { username: string; password: string }): FieldErrors {
  const errors: FieldErrors = {};

  if (!values.username.trim()) {
    errors.username = "Username is required.";
  }

  if (!values.password.trim()) {
    errors.password = "Password is required.";
  }

  return errors;
}

export function validateSetPassword(values: { token: string; newPassword: string }): FieldErrors {
  const errors: FieldErrors = {};

  if (!values.token.trim()) {
    errors.token = "Activation token is required.";
  }

  if (!values.newPassword.trim()) {
    errors.newPassword = "New password is required.";
  } else if (values.newPassword.length < 8) {
    errors.newPassword = "Password must be at least 8 characters.";
  }

  return errors;
}

export function validateProfile(values: {
  firstName?: string;
  lastName?: string;
  phoneNumber?: string;
  city?: string;
  maritalStatus?: string;
}): FieldErrors {
  const errors: FieldErrors = {};

  if (!String(values.firstName ?? "").trim()) {
    errors.firstName = "First name is required.";
  }

  if (!String(values.lastName ?? "").trim()) {
    errors.lastName = "Last name is required.";
  }

  if (!String(values.phoneNumber ?? "").trim()) {
    errors.phoneNumber = "Phone number is required.";
  }

  if (!String(values.maritalStatus ?? "").trim()) {
    errors.maritalStatus = "Marital status is required.";
  }

  return errors;
}

export function validatePasswordChange(values: {
  oldPassword: string;
  newPassword: string;
  confirmNewPassword?: string;
}): FieldErrors {
  const errors: FieldErrors = {};

  if (!values.oldPassword.trim()) {
    errors.oldPassword = "Current password is required.";
  }

  if (!values.newPassword.trim()) {
    errors.newPassword = "New password is required.";
  } else if (values.newPassword.length < 8) {
    errors.newPassword = "New password must be at least 8 characters.";
  }

  if (values.confirmNewPassword !== undefined) {
    if (!values.confirmNewPassword.trim()) {
      errors.confirmNewPassword = "Confirm new password is required.";
    } else if (values.confirmNewPassword !== values.newPassword) {
      errors.confirmNewPassword = "New passwords do not match.";
    }
  }

  return errors;
}

export function validateCity(values: { cityName: string }): FieldErrors {
  const errors: FieldErrors = {};

  if (!values.cityName.trim()) {
    errors.cityName = "City name is required.";
  }

  return errors;
}

export function validateCustomer(values: {
  nationalId?: string;
  firstName?: string;
  lastName?: string;
  phoneNumber?: string;
  cityId?: number;
}): FieldErrors {
  const errors: FieldErrors = {};

  if ("nationalId" in values && !String(values.nationalId ?? "").trim()) {
    errors.nationalId = "National ID is required.";
  }

  if ("nationalId" in values && values.nationalId && !/^\d{9}$/.test(values.nationalId)) {
    errors.nationalId = "National ID must contain exactly 9 digits.";
  }

  if (!String(values.firstName ?? "").trim()) {
    errors.firstName = "First name is required.";
  }

  if (!String(values.lastName ?? "").trim()) {
    errors.lastName = "Last name is required.";
  }

  if (!String(values.phoneNumber ?? "").trim()) {
    errors.phoneNumber = "Phone number is required.";
  }

  if (!values.cityId) {
    errors.cityId = "City is required.";
  }

  return errors;
}

export function validateProduct(values: {
  productName: string;
  productType: string;
  unit: string;
  price: number;
  inventory: number | null;
}): FieldErrors {
  const errors: FieldErrors = {};

  if (!values.productName.trim()) {
    errors.productName = "Product name is required.";
  }

  if (!values.productType.trim()) {
    errors.productType = "Product type is required.";
  }

  if (!values.unit.trim()) {
    errors.unit = "Unit is required.";
  }

  if (!(values.price > 0)) {
    errors.price = "Price must be greater than zero.";
  }

  if (values.inventory !== null && values.inventory < 0) {
    errors.inventory = "Inventory cannot be negative.";
  }

  return errors;
}

export function validateOrderDraft(
  resolvedCustomerId: number | null | undefined,
  items: OrderItemInput[],
  products: Product[]
): FieldErrors {
  const errors: FieldErrors = {};

  if (!resolvedCustomerId) {
    errors.customerId = "Resolve a customer before creating the order.";
  }

  if (!items.length) {
    errors.items = "At least one item is required.";
    return errors;
  }

  const seenPurchase = new Set<number>();
  const seenOlive = new Set<string>();

  items.forEach((item, index) => {
    const product = products.find((entry) => entry.id === item.productId);

    if (!product) {
      errors[`items.${index}.productId`] = "Choose a valid product.";
      return;
    }

    if (!(item.quantity > 0)) {
      errors[`items.${index}.quantity`] = "Quantity must be greater than zero.";
    }

    if (isOliveProductType(product.productType)) {
      if (!item.oliveType?.trim()) {
        errors[`items.${index}.oliveType`] = "Olive type is required.";
      }
      if (!item.bagsCount || item.bagsCount <= 0) {
        errors[`items.${index}.bagsCount`] = "Bags count must be greater than zero.";
      }
      const key = `${product.id}|${item.oliveType ?? ""}`;
      if (seenOlive.has(key)) {
        errors[`items.${index}.oliveType`] = "Duplicate olive service item for the same olive type.";
      }
      seenOlive.add(key);
    } else {
      if (seenPurchase.has(product.id)) {
        errors[`items.${index}.productId`] = "Duplicate purchase product in the same order.";
      }
      seenPurchase.add(product.id);
    }
  });

  return errors;
}

export function validatePayment(values: { orderId: string }): FieldErrors {
  const errors: FieldErrors = {};
  const parsed = Number(values.orderId);

  if (!values.orderId.trim()) {
    errors.orderId = "Order ID is required.";
  } else if (!Number.isFinite(parsed) || parsed <= 0) {
    errors.orderId = "Order ID must be a positive number.";
  }

  return errors;
}

export function validateEmployee(values: {
  nationalId: string;
  firstName: string;
  lastName: string;
  phoneNumber: string;
  email: string;
  roleName: string;
}): FieldErrors {
  const errors: FieldErrors = {};

  if (!/^\d{9}$/.test(values.nationalId.trim())) {
    errors.nationalId = "National ID must contain exactly 9 digits.";
  }

  if (!values.firstName.trim()) {
    errors.firstName = "First name is required.";
  }

  if (!values.lastName.trim()) {
    errors.lastName = "Last name is required.";
  }

  if (!values.phoneNumber.trim()) {
    errors.phoneNumber = "Phone number is required.";
  }

  if (!values.email.trim()) {
    errors.email = "Email is required.";
  } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(values.email.trim())) {
    errors.email = "Enter a valid email address.";
  }

  if (!values.roleName.trim()) {
    errors.roleName = "Role is required.";
  }

  return errors;
}

export function validateRequiredText(name: string, value: string): FieldErrors {
  return value.trim() ? {} : { [name]: "This field is required." };
}

export function validatePositiveId(name: string, value: string): FieldErrors {
  const parsed = Number(value);

  if (!value.trim()) {
    return { [name]: "This field is required." };
  }

  if (!Number.isFinite(parsed) || parsed <= 0) {
    return { [name]: "Enter a positive number." };
  }

  return {};
}

export function validateBatchId(name: string, value: string): FieldErrors {
  const normalized = value.trim();

  if (!normalized) {
    return { [name]: "Batch ID is required." };
  }

  if (!/^[A-Za-z0-9](?:[A-Za-z0-9_-]{0,63})$/.test(normalized)) {
    return {
      [name]: "Batch ID may contain only letters, numbers, hyphens, and underscores (1-64 characters)."
    };
  }

  return {};
}

export function validateProductionBatch(values: {
  orderId?: string;
  orderItemId: string;
  oliveWeightKg: string;
}): FieldErrors {
  const weight = Number(values.oliveWeightKg);

  return {
    ...(values.orderId?.trim() ? validatePositiveId("batchOrderId", values.orderId) : {}),
    ...validatePositiveId("batchOrderItemId", values.orderItemId),
    ...(!values.oliveWeightKg.trim()
      ? { batchOliveWeightKg: "This field is required." }
      : !Number.isFinite(weight) || weight <= 0
        ? { batchOliveWeightKg: "Enter a positive number." }
        : {})
  };
}
