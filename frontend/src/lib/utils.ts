import type { Customer, Product } from "../types/models";

export function cx(...values: Array<string | false | null | undefined>): string {
  return values.filter(Boolean).join(" ");
}

export function titleCase(value: string): string {
  return value
    .toLowerCase()
    .split(/[_\s-]+/)
    .filter(Boolean)
    .map((part) => part[0].toUpperCase() + part.slice(1))
    .join(" ");
}

function getActiveLocale(): string {
  if (typeof document === "undefined") {
    return "en";
  }

  const language = document.documentElement.lang || "en";
  return language.toLowerCase().startsWith("ar") ? "ar" : "en";
}

export function translateBackendMessage(message: string): string {
  const normalized = message.trim();

  if (!normalized || getActiveLocale() !== "ar") {
    return normalized;
  }

  const exactTranslations: Record<string, string> = {
    "Request failed.": "فشل تنفيذ الطلب.",
    "Request failed": "فشل تنفيذ الطلب.",
    "Something went wrong.": "حدث خطأ غير متوقع.",
    "Order must contain at least one item": "يجب أن يحتوي الطلب على عنصر واحد على الأقل.",
    "Duplicate purchase product": "يوجد منتج شراء مكرر داخل الطلب نفسه.",
    "Duplicate service with same oliveType": "يوجد عنصر عصر مكرر لنفس نوع الزيتون.",
    "Olive pressing item requires oliveType & bagsCount": "عنصر عصر الزيتون يتطلب نوع الزيتون وعدد الأكياس.",
    "Order service unavailable": "خدمة الطلبات غير متاحة حالياً.",
    "Product service unavailable": "خدمة المنتجات غير متاحة حالياً.",
    "Customer service is currently unavailable": "خدمة العملاء غير متاحة حالياً.",
    "Queue service must be running for pressing orders": "يجب أن تكون خدمة الطابور قيد التشغيل لطلبات العصر.",
    "userId missing in authentication token": "معرف المستخدم غير موجود في رمز التحقق.",
    "The request was interrupted before it finished.": "تم إيقاف الطلب قبل اكتماله.",
    "Production batch service is currently unavailable": "خدمة دفعات الإنتاج غير متاحة حالياً.",
    "Production batch must include a positive olive weight before prediction":
      "يجب أن تحتوي دفعة الإنتاج على وزن زيتون موجب قبل تنفيذ التنبؤ.",
    "batchId must be 1-64 characters and contain only letters, numbers, hyphens, or underscores":
      "يجب أن يتكون رقم الدفعة من 1 إلى 64 حرفاً أو رقماً، ويمكن أن يحتوي على الشرطة أو الشرطة السفلية فقط.",
    "oliveWeightKg must be greater than zero": "يجب أن يكون وزن الزيتون أكبر من صفر.",
    "oliveWeightKg is required": "وزن الزيتون بالكيلوغرام مطلوب.",
    "orderId is required": "رقم الطلب مطلوب.",
    "orderItemId is required": "رقم عنصر الطلب مطلوب."
  };

  if (exactTranslations[normalized]) {
    return exactTranslations[normalized];
  }

  const patternTranslations: Array<{ pattern: RegExp; translate: (...matches: string[]) => string }> = [
    {
      pattern: /^Order not found with id: (\d+)$/,
      translate: (id) => `لم يتم العثور على الطلب رقم ${id}.`
    },
    {
      pattern: /^Customer not found with id: (\d+)$/,
      translate: (id) => `لم يتم العثور على العميل رقم ${id}.`
    },
    {
      pattern: /^Customer not found with nationalId: (\d+)$/,
      translate: (id) => `لم يتم العثور على عميل بالرقم الوطني ${id}.`
    },
    {
      pattern: /^Product not found with id: (\d+)$/,
      translate: (id) => `لم يتم العثور على المنتج رقم ${id}.`
    },
    {
      pattern: /^Payment not found with id: (\d+)$/,
      translate: (id) => `لم يتم العثور على الدفعة رقم ${id}.`
    },
    {
      pattern: /^Payment not found for orderId: (\d+)$/,
      translate: (id) => `لم يتم العثور على دفعة للطلب رقم ${id}.`
    },
    {
      pattern: /^Payment already exists for orderId: (\d+)$/,
      translate: (id) => `توجد دفعة مسجلة مسبقاً للطلب رقم ${id}.`
    },
    {
      pattern: /^Order cannot be paid in status: (.+)$/,
      translate: (status) => `لا يمكن دفع الطلب وهو في الحالة ${status}.`
    },
    {
      pattern: /^Product out of stock: (.+)$/,
      translate: (name) => `المنتج غير متوفر في المخزون: ${name}.`
    },
    {
      pattern: /^Not enough stock for: (.+)\. Available: (.+), Requested: (.+)$/,
      translate: (name, available, requested) =>
        `المخزون غير كافٍ للمنتج ${name}. المتوفر: ${available}، المطلوب: ${requested}.`
    },
    {
      pattern: /^Invalid productId: (\d+)$/,
      translate: (id) => `معرف المنتج غير صالح: ${id}.`
    },
    {
      pattern: /^Unsupported product type: (.+)$/,
      translate: (productType) => `نوع المنتج غير مدعوم: ${productType}.`
    },
    {
      pattern: /^Unsupported product type for payment: (.+)$/,
      translate: (details) => `نوع المنتج غير مدعوم في الدفع: ${details}.`
    },
    {
      pattern: /^Production batch not found with id: (.+)$/,
      translate: (id) => `لم يتم العثور على دفعة الإنتاج بالرقم ${id}.`
    },
    {
      pattern: /^Production batch not found for order item: (\d+)$/,
      translate: (id) => `لم يتم العثور على دفعة إنتاج لعنصر الطلب رقم ${id}.`
    },
    {
      pattern: /^A production batch already exists for order item: (\d+)$/,
      translate: (id) => `توجد دفعة إنتاج مسجلة مسبقاً لعنصر الطلب رقم ${id}.`
    },
    {
      pattern: /^Order item (\d+) is not a valid olive-production item for order (\d+)$/,
      translate: (itemId, orderId) =>
        `عنصر الطلب رقم ${itemId} غير صالح كعنصر عصر زيتون ضمن الطلب رقم ${orderId}.`
    },
    {
      pattern: /^OrderStatus not found: (.+)$/,
      translate: (status) => `لم يتم العثور على حالة الطلب: ${status}.`
    },
    {
      pattern: /^Invalid order status transition: (.+)$/,
      translate: (details) => `انتقال حالة الطلب غير صالح: ${details}.`
    },
    {
      pattern: /^No static resource (.+)\.$/,
      translate: (path) => `المسار المطلوب غير متاح: ${path}.`
    },
    {
      pattern: /^Unable to reach the API\./,
      translate: () => "تعذر الوصول إلى الواجهة البرمجية. تأكد من تشغيل البوابة والخدمات المطلوبة."
    }
  ];

  for (const entry of patternTranslations) {
    const match = normalized.match(entry.pattern);
    if (match) {
      return entry.translate(...match.slice(1));
    }
  }

  return normalized;
}

export function formatDateTime(value?: string): string {
  if (!value) {
    return "--";
  }

  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return value;
  }

  return new Intl.DateTimeFormat(getActiveLocale(), {
    dateStyle: "medium",
    timeStyle: "short"
  }).format(date);
}

export function formatNumber(value?: number | null): string {
  if (value === null || value === undefined || Number.isNaN(value)) {
    return "--";
  }
  return new Intl.NumberFormat(getActiveLocale()).format(value);
}

export function formatCurrency(value?: number | null): string {
  if (value === null || value === undefined || Number.isNaN(value)) {
    return "--";
  }
  return new Intl.NumberFormat(getActiveLocale(), {
    style: "currency",
    currency: "ILS",
    maximumFractionDigits: 2
  }).format(value);
}

export function downloadBlob(blob: Blob, filename: string): void {
  const url = URL.createObjectURL(blob);
  const anchor = document.createElement("a");
  anchor.href = url;
  anchor.download = filename;
  anchor.click();
  URL.revokeObjectURL(url);
}

export function asErrorMessage(error: unknown): string {
  if (typeof error === "string") {
    return translateBackendMessage(error);
  }
  if (error && typeof error === "object" && "message" in error) {
    return translateBackendMessage(String(error.message));
  }
  return translateBackendMessage("Something went wrong.");
}

export function isOliveProductType(value?: string): boolean {
  return ["SERVICE", "OLIVE"].includes((value ?? "").toUpperCase());
}

export function isMemberOliveProduct(product: Product): boolean {
  const text = `${product.productName} ${product.productType}`.toLowerCase();
  return isOliveProductType(product.productType) && text.includes("مساهم") && !text.includes("غير");
}

export function isStandardOliveProduct(product: Product): boolean {
  const text = `${product.productName} ${product.productType}`.toLowerCase();
  return isOliveProductType(product.productType) && (text.includes("غير") || text.includes("standard") || text.includes("non"));
}

export function findOliveProductForCustomer(products: Product[], customer?: Partial<Pick<Customer, "isMember">> | null): Product | null {
  const preferred = products.find((product) => (customer?.isMember ? isMemberOliveProduct(product) : isStandardOliveProduct(product)));
  return preferred ?? products.find((product) => isOliveProductType(product.productType)) ?? null;
}

export function isPurchaseProductType(value?: string): boolean {
  return !isOliveProductType(value);
}
