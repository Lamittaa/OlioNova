import { useEffect, useMemo, useState } from "react";
import QRCode from "qrcode";
import { useAuth } from "../app/auth-context";
import { useI18n } from "../app/i18n-context";
import { useToastHelpers } from "../app/toast-context";
import { canAction } from "../app/policy";
import { ActionButton, ErrorSummary, FieldError, PermissionNote } from "../components/form-ui";
import { Banner, Card, EmptyState, PageHeader, StatusBadge } from "../components/ui";
import { publicAsset } from "../lib/assets";
import { endpoints } from "../lib/endpoints";
import { apiRequest } from "../lib/http";
import { fetchProductionDashboard, fetchProductionPipeline } from "../lib/production-api";
import { useActionState } from "../lib/use-action-state";
import { AccountantSalesCard } from "./accountant-sales-card";
import {
  asErrorMessage,
  downloadBlob,
  findOliveProductForCustomer,
  formatCurrency,
  formatDateTime,
  formatNumber,
  isOliveProductType
} from "../lib/utils";
import {
  getApiFieldErrors,
  getApiErrorMessage,
  type FieldErrors,
  validateBatchId,
  validateProductionBatch,
  validateOrderDraft,
  validatePayment
} from "../lib/validation";
import type {
  AiPredictionResponse,
  BatchTracking,
  CreateProductionBatchRequest,
  Customer,
  Order,
  OrderItemInput,
  Payment,
  Product,
  ProductionBatch,
  ProductionDashboardItem,
  ProductionEta,
  ProductionStage,
  QueueTicket,
  QueueStatus
} from "../types/models";

type ToastNotice = { tone: "success" | "danger" | "neutral"; message: string };
type ToastNoticeValue = ToastNotice | null;

function useNotice() {
  const { success, error, neutral } = useToastHelpers();

  return {
    notice: null as ToastNoticeValue,
    setNotice: (notice: ToastNoticeValue) => {
      if (!notice) {
        return;
      }

      if (notice.tone === "success") {
        success(notice.message);
        return;
      }

      if (notice.tone === "danger") {
        error(notice.message);
        return;
      }

      neutral(notice.message);
    }
  };
}

function NoticeBlock({
  notice
}: {
  notice: ToastNotice | null;
}): JSX.Element | null {
  return null;
}

function formatPercent(value: number, language: "ar" | "en"): string {
  return `${new Intl.NumberFormat(language === "ar" ? "ar" : "en", {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
  }).format(value)}%`;
}

function normalizeOrderForFrontend(order: Order): Order {
  const syncPaidItems =
    order.status.toUpperCase() === "PAID" &&
    order.items.every((item) => ["SUBMITTED", "PAID"].includes(item.status.toUpperCase()));

  return {
    ...order,
    member: order.member ?? order.isMember,
    isMember: order.isMember ?? order.member,
    items: order.items.map((item) => {
      const oliveProduct = isOliveProductType(item.productType);

      return {
        ...item,
        status: syncPaidItems && item.status.toUpperCase() === "SUBMITTED" ? "PAID" : item.status,
        oliveType: oliveProduct ? item.oliveType : undefined,
        bagsCount: oliveProduct ? item.bagsCount : undefined
      };
    })
  };
}

function normalizeOrdersForFrontend(orders: Order[]): Order[] {
  return orders.map(normalizeOrderForFrontend);
}

function buildCompatibleCreateItems(items: OrderItemInput[], products: Product[]): OrderItemInput[] {
  return items.map((item) => {
    const product = products.find((entry) => entry.id === item.productId);

    if (!product || isOliveProductType(product.productType)) {
      return item;
    }

    return {
      ...item,
      oliveType: item.oliveType?.trim() || product.productType,
      bagsCount: item.bagsCount && item.bagsCount > 0 ? item.bagsCount : Math.max(1, Math.ceil(item.quantity))
    };
  });
}

function readRecordValue(record: Record<string, unknown>, keys: string[]): string {
  for (const key of keys) {
    const value = record[key];
    if (value !== null && value !== undefined && value !== "") {
      return String(value);
    }
  }

  return "-";
}

function getPaymentIdentifier(payment: Payment): string {
  return readRecordValue(payment as unknown as Record<string, unknown>, ["id", "paymentId"]);
}

function getPaymentOrderIdentifier(payment: Payment): string {
  return readRecordValue(payment as unknown as Record<string, unknown>, ["orderId", "order_id", "orderNumber"]);
}

function getPaymentMethodLabel(payment: Payment): string {
  return readRecordValue(payment as unknown as Record<string, unknown>, ["paymentMethod", "method", "paymentType"]);
}

function getPaymentAmountLabel(payment: Payment): string {
  const value = readRecordValue(payment as unknown as Record<string, unknown>, ["amount", "totalAmount", "paidAmount"]);
  const amount = Number(value);
  return Number.isFinite(amount) ? formatCurrency(amount) : value;
}

function getPaymentTimeLabel(payment: Payment): string {
  const value = readRecordValue(payment as unknown as Record<string, unknown>, ["paidAt", "paymentDate", "createdAt", "timestamp"]);
  return value === "-" ? value : formatDateTime(value);
}

function escapeReceiptHtml(value?: string | number | null): string {
  return String(value ?? "")
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;")
    .replace(/'/g, "&#39;");
}

function qrSvg(text: string): string {
  const size = 29;
  const dataCodewords = 55;
  const eccCodewords = 15;
  const modules = Array.from({ length: size }, () => Array<boolean>(size).fill(false));
  const reserved = Array.from({ length: size }, () => Array<boolean>(size).fill(false));
  const set = (x: number, y: number, dark: boolean, isReserved = true): void => {
    if (x < 0 || y < 0 || x >= size || y >= size) {
      return;
    }
    modules[y][x] = dark;
    if (isReserved) {
      reserved[y][x] = true;
    }
  };
  const finder = (x: number, y: number): void => {
    for (let dy = -1; dy <= 7; dy += 1) {
      for (let dx = -1; dx <= 7; dx += 1) {
        const xx = x + dx;
        const yy = y + dy;
        const dark = dx >= 0 && dx <= 6 && dy >= 0 && dy <= 6 && (dx === 0 || dx === 6 || dy === 0 || dy === 6 || (dx >= 2 && dx <= 4 && dy >= 2 && dy <= 4));
        set(xx, yy, dark);
      }
    }
  };
  finder(0, 0);
  finder(size - 7, 0);
  finder(0, size - 7);
  for (let i = 8; i < size - 8; i += 1) {
    set(i, 6, i % 2 === 0);
    set(6, i, i % 2 === 0);
  }
  for (let dy = -2; dy <= 2; dy += 1) {
    for (let dx = -2; dx <= 2; dx += 1) {
      const distance = Math.max(Math.abs(dx), Math.abs(dy));
      set(22 + dx, 22 + dy, distance === 2 || distance === 0);
    }
  }
  set(8, 21, true);
  for (let i = 0; i <= 8; i += 1) {
    if (i !== 6) {
      set(8, i, false);
      set(i, 8, false);
    }
  }
  for (let i = 0; i < 8; i += 1) {
    set(size - 1 - i, 8, false);
    set(8, size - 1 - i, false);
  }

  const bytes = Array.from(new TextEncoder().encode(text.slice(0, 53)));
  const bits: number[] = [];
  const appendBits = (value: number, length: number): void => {
    for (let i = length - 1; i >= 0; i -= 1) {
      bits.push((value >>> i) & 1);
    }
  };
  appendBits(0x4, 4);
  appendBits(bytes.length, 8);
  bytes.forEach((byte) => appendBits(byte, 8));
  while (bits.length % 8 !== 0 && bits.length < dataCodewords * 8) {
    bits.push(0);
  }
  const data: number[] = [];
  for (let i = 0; i < bits.length; i += 8) {
    data.push(Number.parseInt(bits.slice(i, i + 8).join(""), 2));
  }
  for (let pad = 0; data.length < dataCodewords; pad += 1) {
    data.push(pad % 2 === 0 ? 0xec : 0x11);
  }

  const exp = new Array<number>(512).fill(0);
  const log = new Array<number>(256).fill(0);
  let value = 1;
  for (let i = 0; i < 255; i += 1) {
    exp[i] = value;
    log[value] = i;
    value <<= 1;
    if (value & 0x100) {
      value ^= 0x11d;
    }
  }
  for (let i = 255; i < 512; i += 1) {
    exp[i] = exp[i - 255];
  }
  const multiply = (a: number, b: number): number => (a && b ? exp[log[a] + log[b]] : 0);
  let generator = [1];
  for (let i = 0; i < eccCodewords; i += 1) {
    const next = new Array<number>(generator.length + 1).fill(0);
    generator.forEach((coefficient, index) => {
      next[index] ^= multiply(coefficient, exp[i]);
      next[index + 1] ^= coefficient;
    });
    generator = next;
  }
  const ecc = new Array<number>(eccCodewords).fill(0);
  data.forEach((byte) => {
    const factor = byte ^ ecc.shift()!;
    ecc.push(0);
    for (let i = 0; i < eccCodewords; i += 1) {
      ecc[i] ^= multiply(generator[i], factor);
    }
  });
  const allBits = [...data, ...ecc].flatMap((byte) => Array.from({ length: 8 }, (_, index) => (byte >>> (7 - index)) & 1));
  let bitIndex = 0;
  let upward = true;
  for (let x = size - 1; x >= 1; x -= 2) {
    if (x === 6) {
      x -= 1;
    }
    for (let step = 0; step < size; step += 1) {
      const y = upward ? size - 1 - step : step;
      for (let dx = 0; dx < 2; dx += 1) {
        const xx = x - dx;
        if (!reserved[y][xx]) {
          const masked = Boolean(allBits[bitIndex] ?? 0) !== ((xx + y) % 2 === 0);
          set(xx, y, masked, false);
          bitIndex += 1;
        }
      }
    }
    upward = !upward;
  }
  const formatData = (1 << 3) | 0;
  let remainder = formatData << 10;
  for (let i = 14; i >= 10; i -= 1) {
    if ((remainder >>> i) & 1) {
      remainder ^= 0x537 << (i - 10);
    }
  }
  const formatBits = ((formatData << 10) | remainder) ^ 0x5412;
  const bit = (index: number): boolean => Boolean((formatBits >>> index) & 1);
  for (let i = 0; i <= 5; i += 1) set(8, i, bit(i));
  set(8, 7, bit(6));
  set(8, 8, bit(7));
  set(7, 8, bit(8));
  for (let i = 9; i < 15; i += 1) set(14 - i, 8, bit(i));
  for (let i = 0; i < 8; i += 1) set(size - 1 - i, 8, bit(i));
  for (let i = 8; i < 15; i += 1) set(8, size - 15 + i, bit(i));

  const quiet = 4;
  const rects = modules
    .flatMap((row, y) => row.map((dark, x) => (dark ? `<rect x="${x + quiet}" y="${y + quiet}" width="1" height="1"/>` : "")))
    .join("");
  return `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 ${size + quiet * 2} ${size + quiet * 2}" shape-rendering="crispEdges"><rect width="100%" height="100%" fill="#fff"/>${rects}</svg>`;
}

function findProductByKeywords(products: Product[], keywords: string[]): Product | null {
  return (
    products.find((product) => {
      const haystack = `${product.productName} ${product.productType}`.toLowerCase();
      return keywords.some((keyword) => haystack.includes(keyword.toLowerCase()));
    }) ?? null
  );
}

function parseMoneyInput(value: string): number {
  const parsed = Number(value);
  return Number.isFinite(parsed) && parsed > 0 ? parsed : 0;
}

function getOrderOliveItem(order: Order | null): Order["items"][number] | null {
  return order?.items.find((item) => isOliveProductType(item.productType) || isOliveProductType(item.productName)) ?? null;
}

type InvoiceLine = {
  label: string;
  quantity: number;
  unitPrice: number;
  total: number;
  unit: string;
};

async function printInvoiceReceipt({
  language,
  payment,
  order,
  customer,
  tracking,
  lines,
  invoiceTotal
}: {
  language: "ar" | "en";
  payment: Payment;
  order: Order | null;
  customer: Customer | null;
  tracking: BatchTracking | null;
  lines: InvoiceLine[];
  invoiceTotal: number;
}): Promise<void> {
  const receiptWindow = window.open("", "_blank", "width=960,height=720");

  if (!receiptWindow) {
    throw new Error("Allow pop-ups to open the receipt window.");
  }

  const direction = language === "ar" ? "rtl" : "ltr";
  const customerName = customer ? `${customer.firstName} ${customer.lastName}`.trim() : "-";
  const receiptNumber = String(payment.id).padStart(5, "0");
  const orderLabel = order ? `#${order.id}` : `#${payment.orderId}`;
  const printedTotal = invoiceTotal > 0 ? invoiceTotal : payment.totalPrice;
  const trackingCode = tracking?.trackingCode ?? "";
  const trackingUrl = trackingCode ? `${window.location.origin}/track?code=${encodeURIComponent(trackingCode)}` : "";
  const logoUrl = new URL(publicAsset("press-logo-full.jpeg"), window.location.origin).toString();
  const qrMarkup = trackingUrl
    ? await QRCode.toString(trackingUrl, {
        errorCorrectionLevel: "M",
        margin: 2,
        type: "svg",
        width: 128
      })
    : "";

  receiptWindow.document.write(`
    <!doctype html>
    <html lang="${language}" dir="${direction}">
      <head>
        <meta charset="utf-8" />
        <title>فاتورة / إيصال</title>
        <style>
          * { box-sizing: border-box; }
          body { margin: 0; padding: 22px; background: #d8c9aa; color: #1f1b16; font-family: "Segoe UI", Tahoma, Arial, sans-serif; }
          .sheet { width: 190mm; min-height: 270mm; margin: 0 auto; background: #f8efd4; padding: 11mm 13mm; border: 1px solid #8f7c54; box-shadow: 0 18px 44px rgba(0,0,0,.18); }
          .head { display: grid; grid-template-columns: 1fr 150px 1fr; gap: 14px; align-items: center; border-bottom: 2px solid #111; padding-bottom: 12px; text-align: center; }
          .head .side { font-size: 14px; line-height: 1.45; font-weight: 600; }
          .logo { width: 148px; height: 118px; object-fit: contain; margin: 0 auto; display: block; background: transparent; }
          .serial { color: #9b2f2f; font-size: 28px; font-weight: 800; margin: 12px 0 4px; letter-spacing: 2px; text-align: left; direction: ltr; }
          h1 { margin: 4px 0 18px; text-align: center; font-size: 28px; text-decoration: underline; }
          .lines { display: grid; gap: 12px; margin: 18px 0; font-size: 18px; }
          .line { display: grid; grid-template-columns: auto 1fr auto 1fr; gap: 10px; align-items: end; }
          .fill { border-bottom: 1px dotted #111; min-height: 24px; padding: 0 8px; font-weight: 700; }
          table { width: 100%; border-collapse: collapse; margin-top: 18px; font-size: 17px; }
          th, td { border: 2px solid #151515; padding: 10px; text-align: start; vertical-align: top; }
          th { background: rgba(255,255,255,.16); text-align: center; font-weight: 800; }
          .amount { width: 135px; text-align: center; }
          .summary-row { display: grid; grid-template-columns: 1fr 120px; gap: 14px; align-items: start; margin-top: 18px; }
          .qr { border: 2px solid #111; background: #fff; padding: 8px; width: 120px; height: 120px; }
          .qr svg { width: 100%; height: 100%; display: block; }
          .tracking { font-size: 14px; text-align: center; margin-top: 6px; direction: ltr; }
          .sign { display: grid; grid-template-columns: 1fr 1fr; gap: 34px; margin-top: 34px; font-size: 18px; }
          .sign div { border-bottom: 1px dotted #111; min-height: 32px; display: flex; align-items: end; gap: 10px; }
          .footer { margin-top: 28px; text-align: center; font-size: 20px; font-weight: 800; }
          @page { size: A4; margin: 0; }
          @media print { body { background: #fff; padding: 0; } .sheet { width: auto; min-height: auto; border: 0; box-shadow: none; } }
        </style>
      </head>
      <body>
        <main class="sheet">
          <section class="head">
            <div class="side">
              <strong>The Cooperative Society For</strong><br />
              Pressing Olive Industrializing & Marketing Its Products<br />
              Bethlehem<br />
              Tel: 02 - 2742379<br />
              E-mail: olivecoop3@gmail.com
            </div>
            <img class="logo" alt="Press logo" src="${escapeReceiptHtml(logoUrl)}" />
            <div class="side">
              <strong>الجمعية التعاونية لعصر الزيتون</strong><br />
              وتصنيعه وتسويق منتجاته<br />
              محافظة بيت لحم - بيت جالا<br />
              تلفون: 02 - 2742379<br />
              بريد إلكتروني: olivecoop3@gmail.com
            </div>
          </section>
          <div class="serial">No. ${escapeReceiptHtml(receiptNumber)}</div>
          <h1>فاتورة / إيصال</h1>
          <section class="lines">
            <div class="line"><strong>وصلني من:</strong><span class="fill">${escapeReceiptHtml(customerName)}</span><strong>رقم العضوية:</strong><span class="fill">${escapeReceiptHtml(customer?.isMember ? customer.nationalId : "")}</span></div>
            <div class="line"><strong>عنوانه:</strong><span class="fill"></span><strong>الهاتف:</strong><span class="fill">${escapeReceiptHtml(customer?.phoneNumber ?? "")}</span></div>
            <div class="line"><strong>وذلك عن:</strong><span class="fill">${escapeReceiptHtml(lines.map((line) => `${line.label} ${line.quantity} ${line.unit}`).join(" / "))}</span><strong>رقم الوصل:</strong><span class="fill">${escapeReceiptHtml(receiptNumber)}</span></div>
            <div class="line"><strong>رمز التتبع:</strong><span class="fill">${escapeReceiptHtml(trackingCode || "-")}</span><strong>رقم الطلب:</strong><span class="fill">${escapeReceiptHtml(orderLabel)}</span></div>
          </section>
          <table>
            <thead>
              <tr>
                <th>البيان</th>
                <th class="amount">المبلغ / شيكل</th>
              </tr>
            </thead>
            <tbody>
              ${lines
                .map(
                  (line) => `
                    <tr>
                      <td>${escapeReceiptHtml(line.label)} - ${escapeReceiptHtml(`${line.quantity} ${line.unit}`)} × ${escapeReceiptHtml(formatCurrency(line.unitPrice))}</td>
                      <td class="amount">${escapeReceiptHtml(formatCurrency(line.total))}</td>
                    </tr>
                  `
                )
                .join("")}
              <tr>
                <td><strong>المجموع</strong></td>
                <td class="amount"><strong>${escapeReceiptHtml(formatCurrency(printedTotal))}</strong></td>
              </tr>
            </tbody>
          </table>
          <div class="summary-row">
            <div class="lines">
              <div class="line"><strong>رقم العضوية:</strong><span class="fill">${escapeReceiptHtml(customer?.isMember ? customer.nationalId : "")}</span><strong>الهاتف:</strong><span class="fill">${escapeReceiptHtml(customer?.phoneNumber ?? "")}</span></div>
              <div class="line"><strong>رقم الوصل:</strong><span class="fill">${escapeReceiptHtml(receiptNumber)}</span><strong>التاريخ:</strong><span class="fill">${escapeReceiptHtml(formatDateTime(payment.paymentDate))}</span></div>
            </div>
            <div>
              <div class="qr">${qrMarkup}</div>
              <div class="tracking">${escapeReceiptHtml(trackingUrl)}</div>
            </div>
          </div>
          <section class="sign">
            <div><strong>التوقيع</strong></div>
            <div><strong>المحاسب</strong></div>
          </section>
          <div class="footer">من الشجر إلى الحجر تحصل على زيت ذو جودة عالية</div>
        </main>
        <script>window.print();</script>
      </body>
    </html>
  `);
  receiptWindow.document.close();
}

export function InvoicePage(): JSX.Element {
  const auth = useAuth();
  const { language, t } = useI18n();
  const { notice, setNotice } = useNotice();
  const [products, setProducts] = useState<Product[]>([]);
  const [customerLookup, setCustomerLookup] = useState("");
  const [orderLookup, setOrderLookup] = useState("");
  const [matchingOrders, setMatchingOrders] = useState<Order[]>([]);
  const [selectedOrder, setSelectedOrder] = useState<Order | null>(null);
  const [selectedCustomer, setSelectedCustomer] = useState<Customer | null>(null);
  const [invoiceTracking, setInvoiceTracking] = useState<BatchTracking | null>(null);
  const [services, setServices] = useState({ olive: true, pomace: false, gallons: false });
  const [details, setDetails] = useState({
    oliveWeightKg: "",
    oliveUnitPrice: "",
    pomacePieces: "",
    pomaceUnitPrice: "",
    gallonsCount: "",
    gallonUnitPrice: ""
  });
  const [fieldErrors, setFieldErrors] = useState<FieldErrors>({});
  const [createdPayment, setCreatedPayment] = useState<Payment | null>(null);
  const [workflowResults, setWorkflowResults] = useState<string[]>([]);
  const actionState = useActionState();
  const paymentPermission = canAction(auth, "PAYMENT_CREATE");
  const productReadPermission = canAction(auth, "PRODUCT_READ");

  const loadInvoiceTracking = async (orderId: number | string): Promise<BatchTracking | null> => {
    try {
      const tracking = await apiRequest<BatchTracking>(endpoints.tracking.byOrder(orderId));
      setInvoiceTracking(tracking);
      return tracking;
    } catch {
      setInvoiceTracking(null);
      return null;
    }
  };

  useEffect(() => {
    if (!productReadPermission.allowed) {
      return;
    }

    void apiRequest<Product[]>(endpoints.products.list)
      .then((result) => {
        setProducts(result);
        const pomaceProduct = findProductByKeywords(result, ["jift", "pomace", "جفت"]);
        const gallonProduct = findProductByKeywords(result, ["gallon", "جالون", "galon"]);

        setDetails((current) => ({
          ...current,
          pomaceUnitPrice: current.pomaceUnitPrice || (pomaceProduct?.price ? String(pomaceProduct.price) : ""),
          gallonUnitPrice: current.gallonUnitPrice || (gallonProduct?.price ? String(gallonProduct.price) : "")
        }));
      })
      .catch(() => {
        setProducts([]);
      });
  }, [productReadPermission.allowed]);

  const selectOrder = async (order: Order): Promise<void> => {
    const normalizedOrder = normalizeOrderForFrontend(order);
    const oliveItem = getOrderOliveItem(normalizedOrder);

    setSelectedOrder(normalizedOrder);
    setOrderLookup(String(normalizedOrder.id));
    setCreatedPayment(null);
    setWorkflowResults([]);
    setInvoiceTracking(null);

    let customer: Customer | null = null;
    let isMember = Boolean(normalizedOrder.isMember ?? normalizedOrder.member);
    try {
      customer = await apiRequest<Customer>(endpoints.customers.byId(normalizedOrder.customerId));
      try {
        const membership = await apiRequest<{ isMembership: boolean }>(endpoints.customers.membership(normalizedOrder.customerId));
        isMember = Boolean(membership.isMembership);
      } catch {
        isMember = Boolean(customer.isMember ?? isMember);
      }
      setSelectedCustomer({ ...customer, isMember });
    } catch {
      setSelectedCustomer(null);
    }

    setDetails((current) => ({
      ...current,
      oliveWeightKg: oliveItem ? String(oliveItem.quantity) : current.oliveWeightKg,
      oliveUnitPrice: isMember ? "0.4" : "0.6"
    }));

    await loadInvoiceTracking(normalizedOrder.id);
  };

  const searchCustomerOrders = (): void => {
    const query = customerLookup.trim();

    if (!query) {
      setMatchingOrders([]);
      setNotice({ tone: "neutral", message: language === "ar" ? "أدخل رقم العميل أو الرقم الوطني أولاً." : "Enter a customer ID or national ID first." });
      return;
    }

    void actionState.runAction("invoice-customer-search", async () => {
      try {
        const endpoint = query.length >= 8 ? endpoints.orders.byNationalId(query) : endpoints.orders.byCustomer(query);
        const result = normalizeOrdersForFrontend(await apiRequest<Order[]>(endpoint));
        setMatchingOrders(result);

        if (result.length === 1) {
          await selectOrder(result[0]);
        }

        setNotice({
          tone: result.length ? "success" : "neutral",
          message: result.length
            ? language === "ar"
              ? "تم تحميل طلبات العميل."
              : "Customer orders loaded."
            : language === "ar"
              ? "لا توجد طلبات لهذا العميل."
              : "No orders found for this customer."
        });
      } catch (requestError: unknown) {
        setMatchingOrders([]);
        setNotice({ tone: "danger", message: getApiErrorMessage(requestError) });
      }
    });
  };

  const searchOrder = (): void => {
    const query = orderLookup.trim();

    if (!query) {
      setNotice({ tone: "neutral", message: language === "ar" ? "أدخل رقم الطلب أولاً." : "Enter the order number first." });
      return;
    }

    void actionState.runAction("invoice-order-search", async () => {
      try {
        const order = await apiRequest<Order>(endpoints.orders.byId(query));
        await selectOrder(order);
        setNotice({ tone: "success", message: language === "ar" ? "تم تحميل الطلب." : "Order loaded." });
      } catch (requestError: unknown) {
        setNotice({ tone: "danger", message: getApiErrorMessage(requestError) });
      }
    });
  };

  const invoiceLines = useMemo<InvoiceLine[]>(() => {
    const lines: InvoiceLine[] = [];
    const oliveWeight = parseMoneyInput(details.oliveWeightKg);
    const oliveUnitPrice = parseMoneyInput(details.oliveUnitPrice);
    const pomacePieces = parseMoneyInput(details.pomacePieces);
    const pomaceUnitPrice = parseMoneyInput(details.pomaceUnitPrice);
    const gallonsCount = parseMoneyInput(details.gallonsCount);
    const gallonUnitPrice = parseMoneyInput(details.gallonUnitPrice);

    if (services.olive) {
      lines.push({
        label: language === "ar" ? "عصر الزيتون" : "Olive pressing",
        quantity: oliveWeight,
        unitPrice: oliveUnitPrice,
        total: oliveWeight * oliveUnitPrice,
        unit: language === "ar" ? "كغ" : "kg"
      });
    }

    if (services.pomace) {
      lines.push({
        label: language === "ar" ? "جفت" : "Pomace",
        quantity: pomacePieces,
        unitPrice: pomaceUnitPrice,
        total: pomacePieces * pomaceUnitPrice,
        unit: language === "ar" ? "قطعة" : "piece"
      });
    }

    if (services.gallons) {
      lines.push({
        label: language === "ar" ? "جالونات" : "Gallons",
        quantity: gallonsCount,
        unitPrice: gallonUnitPrice,
        total: gallonsCount * gallonUnitPrice,
        unit: language === "ar" ? "جالون" : "gallon"
      });
    }

    return lines;
  }, [details, language, services]);

  const invoiceTotal = invoiceLines.reduce((sum, line) => sum + line.total, 0);
  const selectedOrderOliveItem = getOrderOliveItem(selectedOrder);

  const createPayment = (): void => {
    const nextErrors = validatePayment({ orderId: orderLookup });
    setFieldErrors(nextErrors);
    setWorkflowResults([]);

    if (Object.keys(nextErrors).length) {
      return;
    }

    void actionState.runAction("invoice-create-payment", async () => {
      try {
        const payment = await apiRequest<Payment>(endpoints.payments.create, {
          method: "POST",
          body: { orderId: Number(orderLookup) }
        });
        let nextTracking = invoiceTracking;

        try {
          await apiRequest<unknown>(endpoints.queues.issueProduction(payment.orderId), { method: "POST" });
        } catch (queueError: unknown) {
          void queueError;
        }

        try {
          await apiRequest(endpoints.production.batches.byOrder(payment.orderId), { method: "POST" });
        } catch {
          // The batch may already exist from reception.
        }
        nextTracking = await loadInvoiceTracking(payment.orderId);

        setCreatedPayment(payment);
        setWorkflowResults(nextTracking?.trackingCode ? [nextTracking.trackingCode] : []);
        setFieldErrors({});
        setNotice({
          tone: "success",
          message: language === "ar" ? "تم تسجيل الدفعة بنجاح." : "Payment recorded successfully."
        });
      } catch (requestError: unknown) {
        setFieldErrors((current) => ({ ...current, ...getApiFieldErrors(requestError) }));
        setNotice({ tone: "danger", message: getApiErrorMessage(requestError) });
      }
    });
  };

  return (
    <div className="content-grid">
      <PageHeader title={language === "ar" ? "الفاتورة" : "Invoice"} />
      <NoticeBlock notice={notice} />

      <div className="content-grid two">
        <Card title={language === "ar" ? "البحث عن العميل والطلب" : "Customer and Order Lookup"} subtitle={language === "ar" ? "ابحث برقم العميل أو رقم الطلب قبل تسجيل الفاتورة." : "Find the customer or order before creating the invoice."}>
          <div className="form-grid two">
            <div className="field">
              <label>{language === "ar" ? "رقم العميل أو الرقم الوطني" : "Customer ID or National ID"}</label>
              <input onChange={(event) => setCustomerLookup(event.target.value)} value={customerLookup} />
            </div>
            <div className="inline-actions align-end">
              <ActionButton
                busyLabel={t("Searching...")}
                isBusy={actionState.isBusy("invoice-customer-search")}
                onClick={searchCustomerOrders}
                type="button"
              >
                {language === "ar" ? "بحث عن طلبات العميل" : "Search customer orders"}
              </ActionButton>
            </div>
            <div className="field">
              <label>{t("Order ID")}</label>
              <input onChange={(event) => setOrderLookup(event.target.value)} value={orderLookup} />
              <FieldError errors={fieldErrors} name="orderId" />
            </div>
            <div className="inline-actions align-end">
              <ActionButton
                className="ghost"
                busyLabel={t("Searching...")}
                isBusy={actionState.isBusy("invoice-order-search")}
                onClick={searchOrder}
                type="button"
              >
                {language === "ar" ? "بحث عن الطلب" : "Search order"}
              </ActionButton>
            </div>
          </div>
          {matchingOrders.length ? (
            <div className="invoice-order-list">
              {matchingOrders.map((order) => (
                <button className="invoice-order-row" key={order.id} onClick={() => void selectOrder(order)} type="button">
                  <strong>#{order.id}</strong>
                  <span>{language === "ar" ? "العميل" : "Customer"} #{order.customerId}</span>
                  <small>{formatDateTime(order.createdAt)}</small>
                </button>
              ))}
            </div>
          ) : null}
        </Card>

        <Card title={language === "ar" ? "ملخص الطلب" : "Order Summary"} subtitle={language === "ar" ? "معلومات الطلب المختار قبل إنشاء الدفعة." : "Selected order details before payment."}>
          {selectedOrder ? (
            <div className="invoice-summary-card">
              <div>
                <span>{language === "ar" ? "رقم الطلب" : "Order"}</span>
                <strong>#{selectedOrder.id}</strong>
              </div>
              <div>
                <span>{language === "ar" ? "العميل" : "Customer"}</span>
                <strong>{selectedCustomer ? `${selectedCustomer.firstName} ${selectedCustomer.lastName}` : `#${selectedOrder.customerId}`}</strong>
              </div>
              <div>
                <span>{language === "ar" ? "عنصر الزيتون" : "Olive item"}</span>
                <strong>{selectedOrderOliveItem ? `#${selectedOrderOliveItem.id} - ${formatNumber(selectedOrderOliveItem.quantity)} kg` : "-"}</strong>
              </div>
              <div>
                <span>{language === "ar" ? "حالة الطلب" : "Status"}</span>
                <strong>{selectedOrder.status}</strong>
              </div>
            </div>
          ) : (
            <EmptyState title={language === "ar" ? "لم يتم اختيار طلب" : "No order selected"} description={language === "ar" ? "ابحث عن العميل أو الطلب حتى تظهر تفاصيل الفاتورة." : "Search for a customer or order to start the invoice."} />
          )}
        </Card>
      </div>

      <Card title={language === "ar" ? "تفاصيل الفاتورة" : "Invoice Details"} subtitle={language === "ar" ? "اختر الخدمات المطلوبة وأدخل كمياتها وأسعارها." : "Choose services and enter quantities and pricing."}>
        <div className="invoice-service-grid">
          <label className="invoice-service-toggle">
            <input checked={services.olive} onChange={(event) => setServices((current) => ({ ...current, olive: event.target.checked }))} type="checkbox" />
            <span>{language === "ar" ? "عصر الزيتون" : "Olive pressing"}</span>
          </label>
          <label className="invoice-service-toggle">
            <input checked={services.pomace} onChange={(event) => setServices((current) => ({ ...current, pomace: event.target.checked }))} type="checkbox" />
            <span>{language === "ar" ? "جفت" : "Pomace"}</span>
          </label>
          <label className="invoice-service-toggle">
            <input checked={services.gallons} onChange={(event) => setServices((current) => ({ ...current, gallons: event.target.checked }))} type="checkbox" />
            <span>{language === "ar" ? "الجالونات" : "Gallons"}</span>
          </label>
        </div>

        <div className="content-grid three">
          {services.olive ? (
            <div className="invoice-detail-panel">
              <h3>{language === "ar" ? "دفعة عصر الزيتون" : "Olive pressing batch"}</h3>
              <div className="form-grid">
                <div className="field">
                  <label>{language === "ar" ? "وزن الزيتون / كغ" : "Olive weight / kg"}</label>
                  <input readOnly value={details.oliveWeightKg} />
                </div>
                <div className="field">
                  <label>{language === "ar" ? "السعر الثابت / كغ" : "Fixed price / kg"}</label>
                  <input readOnly value={details.oliveUnitPrice} />
                </div>
              </div>
            </div>
          ) : null}
          {services.pomace ? (
            <div className="invoice-detail-panel">
              <h3>{language === "ar" ? "تفاصيل الجفت" : "Pomace details"}</h3>
              <div className="form-grid">
                <div className="field">
                  <label>{language === "ar" ? "كمية الجفت / قطعة" : "Pomace pieces"}</label>
                  <input onChange={(event) => setDetails((current) => ({ ...current, pomacePieces: event.target.value }))} value={details.pomacePieces} />
                </div>
                <div className="field">
                  <label>{language === "ar" ? "السعر الثابت / قطعة" : "Fixed price / piece"}</label>
                  <input readOnly value={details.pomaceUnitPrice} />
                </div>
              </div>
            </div>
          ) : null}
          {services.gallons ? (
            <div className="invoice-detail-panel">
              <h3>{language === "ar" ? "تفاصيل الجالونات" : "Gallons details"}</h3>
              <div className="form-grid">
                <div className="field">
                  <label>{language === "ar" ? "عدد الجالونات" : "Gallons count"}</label>
                  <input onChange={(event) => setDetails((current) => ({ ...current, gallonsCount: event.target.value }))} value={details.gallonsCount} />
                </div>
                <div className="field">
                  <label>{language === "ar" ? "السعر الثابت / جالون" : "Fixed price / gallon"}</label>
                  <input readOnly value={details.gallonUnitPrice} />
                </div>
              </div>
            </div>
          ) : null}
        </div>
      </Card>

      <div className="content-grid two">
        <Card title={language === "ar" ? "ملخص الحساب" : "Invoice Total"} subtitle={language === "ar" ? "مراجعة قبل تسجيل الدفعة." : "Review before recording payment."}>
          <div className="invoice-total-list">
            {invoiceLines.map((line) => (
              <div key={line.label}>
                <span>{line.label}</span>
                <small>{formatNumber(line.quantity)} {line.unit} × {formatCurrency(line.unitPrice)}</small>
                <strong>{formatCurrency(line.total)}</strong>
              </div>
            ))}
            <div className="invoice-grand-total">
              <span>{language === "ar" ? "الإجمالي" : "Total"}</span>
              <strong>{formatCurrency(invoiceTotal)}</strong>
            </div>
          </div>
          <ErrorSummary errors={fieldErrors} />
          <div className="inline-actions">
            <ActionButton
              disabled={!paymentPermission.allowed || !selectedOrder}
              disabledReason={paymentPermission.reason}
              busyLabel={language === "ar" ? "جار التسجيل..." : "Recording..."}
              isBusy={actionState.isBusy("invoice-create-payment")}
              onClick={createPayment}
              type="button"
            >
              {language === "ar" ? "تسجيل الدفعة وإدخالها للطابور" : "Record payment and queue"}
            </ActionButton>
            <PermissionNote allowed={paymentPermission.allowed} reason={paymentPermission.reason} />
          </div>
        </Card>

        <Card title={language === "ar" ? "النتيجة والطباعة" : "Result and Receipt"} subtitle={language === "ar" ? "اطبع الوصل بعد نجاح الدفع." : "Print the receipt after payment succeeds."}>
          {createdPayment ? (
            <div className="invoice-summary-card">
              <div>
                <span>{language === "ar" ? "تم تسجيل الدفعة" : "Payment recorded"}</span>
                <strong>#{createdPayment.id}</strong>
              </div>
              <div>
                <span>{language === "ar" ? "رقم الدفعة" : "Payment number"}</span>
                <strong>{createdPayment.id}</strong>
              </div>
              <div>
                <span>{language === "ar" ? "رمز التتبع" : "Tracking code"}</span>
                <strong>{workflowResults[0] ?? invoiceTracking?.trackingCode ?? "-"}</strong>
              </div>
              <ActionButton
                className="receipt-print-button"
                onClick={() => {
                  void printInvoiceReceipt({ language, payment: createdPayment, order: selectedOrder, customer: selectedCustomer, tracking: invoiceTracking, lines: invoiceLines, invoiceTotal });
                }}
                type="button"
              >
                {t("Print receipt")}
              </ActionButton>
            </div>
          ) : (
            <EmptyState title={language === "ar" ? "لم تسجل دفعة بعد" : "No payment yet"} description={language === "ar" ? "بعد تسجيل الدفعة يظهر زر الطباعة هنا." : "After recording payment, the print button appears here."} />
          )}
        </Card>
      </div>
    </div>
  );
}

export function OrdersPage(): JSX.Element {
  const auth = useAuth();
  const { language, t } = useI18n();
  const { notice, setNotice } = useNotice();
  const [products, setProducts] = useState<Product[]>([]);
  const [orderStatuses, setOrderStatuses] = useState<string[]>([]);
  const [resolvedCustomer, setResolvedCustomer] = useState<{ id: number; name: string; isMember?: boolean } | null>(null);
  const [customerNationalId, setCustomerNationalId] = useState("");
  const [items, setItems] = useState<OrderItemInput[]>([{ productId: 0, quantity: 1 }]);
  const [searchOrderId, setSearchOrderId] = useState("");
  const [searchCustomerId, setSearchCustomerId] = useState("");
  const [searchNationalId, setSearchNationalId] = useState("");
  const [orders, setOrders] = useState<Order[]>([]);
  const [selectedOrder, setSelectedOrder] = useState<Order | null>(null);
  const [selectedOrderItems, setSelectedOrderItems] = useState<Order["items"]>([]);
  const [itemDraft, setItemDraft] = useState<OrderItemInput>({ productId: 0, quantity: 1 });
  const [selectedItemId, setSelectedItemId] = useState<number | null>(null);
  const [itemStatusDraft, setItemStatusDraft] = useState("IN_PROGRESS");
  const [orderStatusDraft, setOrderStatusDraft] = useState("");
  const [createErrors, setCreateErrors] = useState<FieldErrors>({});
  const [paymentLikeErrors, setPaymentLikeErrors] = useState<FieldErrors>({});
  const actionState = useActionState();
  const createPermission = canAction(auth, "ORDER_CREATE");
  const cancelPermission = canAction(auth, "ORDER_CANCEL");
  const updateStatusPermission = canAction(auth, "ORDER_UPDATE_STATUS");
  const addItemPermission = canAction(auth, "ORDER_ITEM_ADD");
  const updateItemPermission = canAction(auth, "ORDER_ITEM_UPDATE");
  const updateItemStatusPermission = canAction(auth, "ORDER_ITEM_UPDATE_STATUS");
  const deleteItemPermission = canAction(auth, "ORDER_ITEM_DELETE");
  const customerSearchPermission = canAction(auth, "CUSTOMER_SEARCH");
  const productReadPermission = canAction(auth, "PRODUCT_READ");
  const orderStatusReadPermission = canAction(auth, "ORDER_STATUS_READ");
  const canCreateOrders = createPermission.allowed && customerSearchPermission.allowed && productReadPermission.allowed;
  const canEditOrderComposition = productReadPermission.allowed && (addItemPermission.allowed || updateItemPermission.allowed);
  const canManageItemStatus = updateItemStatusPermission.allowed;

  useEffect(() => {
    async function load(): Promise<void> {
      const requests: Promise<void>[] = [];

      if (canCreateOrders || canEditOrderComposition) {
        requests.push(
          apiRequest<Product[]>(endpoints.products.list).then((result) => {
            setProducts(result);
          })
        );
      }

      if (orderStatusReadPermission.allowed) {
        requests.push(
          apiRequest<string[]>(endpoints.orders.statusValues).then((result) => {
            setOrderStatuses(result);
          })
        );
      }

      await Promise.all(requests);
    }

    void load().catch((requestError: unknown) => setNotice({ tone: "danger", message: asErrorMessage(requestError) }));
  }, [canCreateOrders, canEditOrderComposition, orderStatusReadPermission.allowed]);

  const syncSelectedOrder = (incoming: Order): Order => {
    const normalized = normalizeOrderForFrontend(incoming);
    setSelectedOrder(normalized);
    setOrderStatusDraft(normalized.status);
    setSelectedOrderItems(normalized.items);
    return normalized;
  };

  return (
    <div className="content-grid">
      <PageHeader
        eyebrow="Order Flow"
        title="Orders"
        description="Find the customer, then create and manage the order in one place."
      />
      <NoticeBlock notice={notice} />
      <div className={canCreateOrders ? "content-grid two" : "content-grid"} id="prediction">
        {canCreateOrders ? (
        <Card title="Create Order" subtitle="Order entry.">
          <ErrorSummary errors={createErrors} />
          <div className="form-grid">
            <div className="field">
              <label>{t("Customer National ID")}</label>
              <input onChange={(event) => setCustomerNationalId(event.target.value)} value={customerNationalId} />
            </div>
            <div className="inline-actions">
              <ActionButton
                className="secondary"
                busyLabel="Resolving..."
                isBusy={actionState.isBusy("resolve-order-customer")}
                onClick={() => {
                  if (!/^\d{9}$/.test(customerNationalId.trim())) {
                    setCreateErrors((current) => ({
                      ...current,
                      customerId: "Enter a 9-digit national ID before resolving the customer."
                    }));
                    return;
                  }

                  void actionState.runAction("resolve-order-customer", async () => {
                    try {
                      const customer = await apiRequest<Customer>(
                        endpoints.customers.byNationalId(customerNationalId.trim())
                      );
                      setResolvedCustomer({ id: customer.id, name: `${customer.firstName} ${customer.lastName}`, isMember: customer.isMember });
                      setCreateErrors((current) => ({ ...current, customerId: "" }));
                      setNotice({ tone: "success", message: `Resolved customer #${customer.id}.` });
                    } catch (requestError: unknown) {
                      setNotice({ tone: "danger", message: getApiErrorMessage(requestError) });
                    }
                  });
                }}
                type="button"
              >
                {t("Resolve customer")}
              </ActionButton>
            </div>
            {resolvedCustomer ? <Banner tone="success" message={`Customer resolved: ${resolvedCustomer.name} (#${resolvedCustomer.id})`} /> : null}
            <FieldError errors={createErrors} name="customerId" />
            {items.map((item, index) => {
              const selectedProduct = products.find((product) => product.id === item.productId);
              const oliveProduct = selectedProduct ? isOliveProductType(selectedProduct.productType) : false;

              return (
                <div className="card" key={`${item.productId}-${index}`}>
                  <div className="form-grid two">
                    <div className="field">
                      <label>{t("Products")}</label>
                      <select
                        onChange={(event) => {
                        const productId = Number(event.target.value);
                          const nextProduct = products.find((product) => product.id === productId);
                          const nextOliveProduct = nextProduct ? isOliveProductType(nextProduct.productType) : false;
                          setItems((current) =>
                            current.map((entry, entryIndex) =>
                              entryIndex === index
                                ? {
                                    productId,
                                    quantity: entry.quantity,
                                    oliveType: nextOliveProduct ? entry.oliveType : "",
                                    bagsCount: nextOliveProduct ? entry.bagsCount : undefined,
                                    note: nextOliveProduct ? entry.note : ""
                                  }
                                : entry
                            )
                          );
                        }}
                        value={item.productId}
                      >
                        <option value={0}>{t("Select product")}</option>
                        {products.map((product) => (
                          <option key={product.id} value={product.id}>
                            {product.productName}
                          </option>
                        ))}
                      </select>
                      <FieldError errors={createErrors} name={`items.${index}.productId`} />
                    </div>
                    <div className="field">
                      <label>{t("Quantity")}</label>
                      <input
                        min={1}
                        onChange={(event) => {
                          const quantity = Number(event.target.value);
                          setItems((current) =>
                            current.map((entry, entryIndex) => (entryIndex === index ? { ...entry, quantity } : entry))
                          );
                        }}
                        type="number"
                        value={item.quantity}
                      />
                      <FieldError errors={createErrors} name={`items.${index}.quantity`} />
                    </div>
                    {oliveProduct ? (
                      <>
                        <div className="field">
                          <label>{t("Olive Type")}</label>
                          <input
                            onChange={(event) => {
                              const oliveType = event.target.value;
                              setItems((current) =>
                                current.map((entry, entryIndex) => (entryIndex === index ? { ...entry, oliveType } : entry))
                              );
                            }}
                            value={item.oliveType ?? ""}
                          />
                          <FieldError errors={createErrors} name={`items.${index}.oliveType`} />
                        </div>
                        <div className="field">
                          <label>{t("Bags Count")}</label>
                          <input
                            min={1}
                            onChange={(event) => {
                              const bagsCount = Number(event.target.value);
                              setItems((current) =>
                                current.map((entry, entryIndex) => (entryIndex === index ? { ...entry, bagsCount } : entry))
                              );
                            }}
                            type="number"
                            value={item.bagsCount ?? ""}
                          />
                          <FieldError errors={createErrors} name={`items.${index}.bagsCount`} />
                        </div>
                      </>
                    ) : null}
                    <div className="field">
                      <label>{t("Note")}</label>
                      <input
                        onChange={(event) => {
                          const note = event.target.value;
                          setItems((current) =>
                            current.map((entry, entryIndex) => (entryIndex === index ? { ...entry, note } : entry))
                          );
                        }}
                        value={item.note ?? ""}
                      />
                    </div>
                  </div>
                  <div className="inline-actions">
                    <button className="btn ghost" onClick={() => {
                      setItems((current) => current.filter((_, entryIndex) => entryIndex !== index));
                    }} type="button">
                      {t("Remove item")}
                    </button>
                  </div>
                </div>
              );
            })}
            <div className="inline-actions">
              <ActionButton className="ghost" onClick={() => setItems((current) => [...current, { productId: 0, quantity: 1 }])} type="button">
                {t("Add item")}
              </ActionButton>
              <ActionButton
                disabled={!createPermission.allowed}
                disabledReason={createPermission.reason}
                busyLabel="Creating..."
                isBusy={actionState.isBusy("create-order")}
                onClick={() => {
                  const nextErrors = validateOrderDraft(resolvedCustomer?.id, items, products);
                  setCreateErrors(nextErrors);
                  if (Object.keys(nextErrors).length) {
                    return;
                  }

                  void actionState.runAction("create-order", async () => {
                    try {
                      const order = await apiRequest<Order>(endpoints.orders.create, {
                        method: "POST",
                        body: {
                          customerId: resolvedCustomer?.id,
                          items: buildCompatibleCreateItems(items, products).map((entry) => {
                            const product = products.find((candidate) => candidate.id === entry.productId);
                            if (!product || !isOliveProductType(product.productType)) {
                              return entry;
                            }

                            const pricedOliveProduct = findOliveProductForCustomer(products, resolvedCustomer);
                            return pricedOliveProduct ? { ...entry, productId: pricedOliveProduct.id } : entry;
                          })
                        }
                      });
                      const normalizedOrder = syncSelectedOrder(order);
                      setOrders([normalizedOrder, ...orders]);
                      setItems([{ productId: 0, quantity: 1 }]);
                      setCreateErrors({});
                      setNotice({ tone: "success", message: `Created order #${order.id}.` });
                    } catch (requestError: unknown) {
                      setCreateErrors((current) => ({ ...current, ...getApiFieldErrors(requestError) }));
                      setNotice({ tone: "danger", message: getApiErrorMessage(requestError) });
                    }
                  });
                }}
                type="button"
              >
                {t("Create Order")}
              </ActionButton>
            </div>
            <PermissionNote allowed={createPermission.allowed} reason={createPermission.reason} />
          </div>
        </Card>
        ) : null}
        <Card title="Search Orders" subtitle="Find an order by order number, customer number, or national ID.">
          <div className="form-grid">
            <div className="field">
              <label>{t("Order ID")}</label>
              <input onChange={(event) => setSearchOrderId(event.target.value)} value={searchOrderId} />
            </div>
            <div className="field">
              <label>{t("Customer ID")}</label>
              <input onChange={(event) => setSearchCustomerId(event.target.value)} value={searchCustomerId} />
            </div>
            <div className="field">
              <label>{t("National ID")}</label>
              <input onChange={(event) => setSearchNationalId(event.target.value)} value={searchNationalId} />
            </div>
            <div className="inline-actions">
              <button
                className="btn secondary"
                onClick={() => {
                  if (!searchOrderId.trim()) {
                    setNotice({
                      tone: "danger",
                      message: language === "ar" ? "Ø£Ø¯Ø®Ù„ Ø±Ù‚Ù… Ø§Ù„Ø·Ù„Ø¨ Ø£ÙˆÙ„Ø§Ù‹." : "Enter an order ID first."
                    });
                    return;
                  }
                  void apiRequest<Order>(endpoints.orders.byId(searchOrderId))
                    .then((order) => {
                      const normalizedOrder = syncSelectedOrder(order);
                      setOrders([normalizedOrder]);
                    })
                    .catch((requestError: unknown) => setNotice({ tone: "danger", message: asErrorMessage(requestError) }));
                }}
                type="button"
              >
                {t("Get order")}
              </button>
              <button
                className="btn ghost"
                onClick={() => {
                  if (!searchCustomerId.trim()) {
                    setNotice({
                      tone: "danger",
                      message: language === "ar" ? "Ø£Ø¯Ø®Ù„ Ø±Ù‚Ù… Ø§Ù„Ø¹Ù…ÙŠÙ„ Ø£ÙˆÙ„Ø§Ù‹." : "Enter a customer ID first."
                    });
                    return;
                  }
                  void apiRequest<Order[]>(endpoints.orders.byCustomer(searchCustomerId))
                    .then((result) => setOrders(normalizeOrdersForFrontend(result)))
                    .catch((requestError: unknown) => setNotice({ tone: "danger", message: asErrorMessage(requestError) }));
                }}
                type="button"
              >
                {t("Search by customer")}
              </button>
              <button
                className="btn ghost"
                onClick={() => {
                  if (!searchNationalId.trim()) {
                    setNotice({
                      tone: "danger",
                      message: language === "ar" ? "Ø£Ø¯Ø®Ù„ Ø§Ù„Ø±Ù‚Ù… Ø§Ù„ÙˆØ·Ù†ÙŠ Ø£ÙˆÙ„Ø§Ù‹." : "Enter a national ID first."
                    });
                    return;
                  }
                  void apiRequest<Order[]>(endpoints.orders.byNationalId(searchNationalId))
                    .then((result) => setOrders(normalizeOrdersForFrontend(result)))
                    .catch((requestError: unknown) => setNotice({ tone: "danger", message: asErrorMessage(requestError) }));
                }}
                type="button"
              >
                {t("Search by national ID")}
              </button>
            </div>
          </div>
        </Card>
      </div>
      <Card title="Orders">
        {orders.length ? (
          <div className="table-wrap">
            <table className="data-table">
              <thead>
                <tr>
                  <th>{t("ID")}</th>
                  <th>{t("Customer ID")}</th>
                  <th>{t("Status")}</th>
                  <th>{t("Items")}</th>
                  <th>{t("Created")}</th>
                </tr>
              </thead>
              <tbody>
                {orders.map((order) => (
                  <tr key={order.id} onClick={() => syncSelectedOrder(order)}>
                    <td>{order.id}</td>
                    <td>{order.customerId}</td>
                    <td><StatusBadge value={order.status} /></td>
                    <td>{order.items.length}</td>
                    <td>{formatDateTime(order.createdAt)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ) : (
          <EmptyState title="No orders loaded" description="Use one of the search tools or create a fresh order." />
        )}
      </Card>
      {selectedOrder ? (
        <Card
          title={language === "ar" ? `Ø§Ù„Ø·Ù„Ø¨ Ø§Ù„Ù…Ø­Ø¯Ø¯ #${selectedOrder.id}` : `Selected Order #${selectedOrder.id}`}
          subtitle="Update the order status or cancel the order."
        >
          <div className="form-grid two">
            <div className="field">
              <label>{t("Status")}</label>
              {updateStatusPermission.allowed && orderStatuses.length ? (
                <select
                  value={orderStatusDraft}
                  onChange={(event) => {
                    const status = event.target.value;
                    setOrderStatusDraft(status);
                    void apiRequest<Order>(endpoints.orders.status(selectedOrder.id), {
                      method: "PUT",
                      body: { status }
                    })
                      .then((order) => {
                        const normalizedOrder = syncSelectedOrder(order);
                        setOrders((current) => current.map((entry) => (entry.id === normalizedOrder.id ? normalizedOrder : entry)));
                      })
                      .catch((requestError: unknown) => setNotice({ tone: "danger", message: asErrorMessage(requestError) }));
                  }}
                >
                  {orderStatuses.map((status) => (
                    <option key={status} value={status}>
                      {status}
                    </option>
                  ))}
                </select>
              ) : (
                <StatusBadge value={selectedOrder.status} />
              )}
            </div>
          </div>
          <PermissionNote allowed={updateStatusPermission.allowed} reason={updateStatusPermission.reason} />
          {cancelPermission.allowed ? (
            <div className="inline-actions">
              <ActionButton
                className="danger"
                disabledReason={cancelPermission.reason}
                busyLabel="Canceling..."
                isBusy={actionState.isBusy("cancel-order")}
                onClick={() => {
                  void actionState.runAction("cancel-order", async () => {
                    try {
                      await apiRequest(endpoints.orders.byId(selectedOrder.id), { method: "DELETE" });
                      const canceledOrder = normalizeOrderForFrontend({ ...selectedOrder, status: "CANCELED" });
                      syncSelectedOrder(canceledOrder);
                      setOrders((current) => current.map((entry) => (entry.id === canceledOrder.id ? canceledOrder : entry)));
                      setSelectedItemId(null);
                      setNotice({ tone: "success", message: `Order #${selectedOrder.id} canceled.` });
                    } catch (requestError: unknown) {
                      setNotice({ tone: "danger", message: getApiErrorMessage(requestError) });
                    }
                  });
                }}
                type="button"
              >
                {t("Cancel order")}
              </ActionButton>
            </div>
          ) : null}
          <div className="card">
            <strong>{t("Order Items")}</strong>
            {selectedOrderItems.length ? (
              <div className="table-wrap">
                <table className="data-table">
                  <thead>
                    <tr>
                      <th>{t("ID")}</th>
                      <th>{t("Product")}</th>
                      <th>{t("Status")}</th>
                      <th>{t("Qty")}</th>
                      <th>{t("Olive Type")}</th>
                      <th>{t("Bags")}</th>
                    </tr>
                  </thead>
                  <tbody>
                    {selectedOrderItems.map((item) => (
                      <tr key={item.id} onClick={() => {
                        setSelectedItemId(item.id);
                        setItemStatusDraft(item.status);
                        setItemDraft({
                          productId: item.productId,
                          quantity: item.quantity,
                          oliveType: item.oliveType,
                          bagsCount: item.bagsCount,
                          note: item.note
                        });
                      }}>
                        <td>{item.id}</td>
                        <td>{item.productName}</td>
                        <td><StatusBadge value={item.status} /></td>
                        <td>{item.quantity}</td>
                        <td>{item.oliveType ?? "-"}</td>
                        <td>{item.bagsCount ?? "-"}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            ) : (
              <EmptyState title="No items loaded" description="This order does not contain any visible items yet." />
            )}
          </div>
          {canEditOrderComposition || canManageItemStatus ? (
          <div className="card">
            <strong>{selectedItemId ? (language === "ar" ? `ØªØ¹Ø¯ÙŠÙ„ Ø§Ù„Ø¹Ù†ØµØ± #${selectedItemId}` : `Edit Item #${selectedItemId}`) : t("Add item")}</strong>
            <div className="form-grid two">
              {canEditOrderComposition ? (
                <>
              <div className="field">
                <label>{t("Product")}</label>
                <select
                  onChange={(event) => {
                    const productId = Number(event.target.value);
                    const nextProduct = products.find((product) => product.id === productId);
                    const olive = nextProduct ? isOliveProductType(nextProduct.productType) : false;
                    setItemDraft((current) => ({
                      ...current,
                      productId,
                      oliveType: olive ? current.oliveType : "",
                      bagsCount: olive ? current.bagsCount : undefined,
                      note: olive ? current.note : ""
                    }));
                  }}
                  value={itemDraft.productId}
                >
                  <option value={0}>{t("Select product")}</option>
                  {products.map((product) => (
                    <option key={product.id} value={product.id}>
                      {product.productName}
                    </option>
                  ))}
                </select>
              </div>
              <div className="field">
                <label>{t("Quantity")}</label>
                <input
                  min={1}
                  onChange={(event) => setItemDraft((current) => ({ ...current, quantity: Number(event.target.value) }))}
                  type="number"
                  value={itemDraft.quantity}
                />
              </div>
              {isOliveProductType(products.find((product) => product.id === itemDraft.productId)?.productType) ? (
                <>
                  <div className="field">
                    <label>{t("Olive Type")}</label>
                    <input
                      onChange={(event) => setItemDraft((current) => ({ ...current, oliveType: event.target.value }))}
                      value={itemDraft.oliveType ?? ""}
                    />
                  </div>
                  <div className="field">
                    <label>{t("Bags Count")}</label>
                    <input
                      min={1}
                      onChange={(event) => setItemDraft((current) => ({ ...current, bagsCount: Number(event.target.value) }))}
                      type="number"
                      value={itemDraft.bagsCount ?? ""}
                    />
                  </div>
                </>
              ) : null}
              <div className="field">
                <label>{t("Note")}</label>
                <input
                  onChange={(event) => setItemDraft((current) => ({ ...current, note: event.target.value }))}
                  value={itemDraft.note ?? ""}
                />
              </div>
                </>
              ) : null}
              {selectedItemId && canManageItemStatus ? (
                <div className="field">
                  <label>{t("Item Status")}</label>
                  <input
                    onChange={(event) => setItemStatusDraft(event.target.value)}
                    placeholder="IN_PROGRESS"
                    value={itemStatusDraft}
                  />
                </div>
              ) : null}
            </div>
            <div className="inline-actions">
              {canEditOrderComposition ? (
              <ActionButton
                className="secondary"
                disabled={!selectedOrder || !addItemPermission.allowed}
                disabledReason={addItemPermission.reason}
                busyLabel="Adding..."
                isBusy={actionState.isBusy("add-order-item")}
                onClick={() => {
                  if (!selectedOrder) {
                    return;
                  }

                  void actionState.runAction("add-order-item", async () => {
                    try {
                      const order = await apiRequest<Order>(endpoints.orders.items(selectedOrder.id), {
                        method: "POST",
                        body: itemDraft
                      });
                      const normalizedOrder = syncSelectedOrder(order);
                      setOrders((current) => current.map((entry) => (entry.id === normalizedOrder.id ? normalizedOrder : entry)));
                      setNotice({ tone: "success", message: "Order item added." });
                    } catch (requestError: unknown) {
                      setNotice({ tone: "danger", message: getApiErrorMessage(requestError) });
                    }
                  });
                }}
                type="button"
              >
                {t("Add item")}
              </ActionButton>
              ) : null}
              {canEditOrderComposition ? (
              <ActionButton
                className="ghost"
                disabled={!selectedOrder || !selectedItemId || !updateItemPermission.allowed}
                disabledReason={updateItemPermission.reason}
                busyLabel="Updating..."
                isBusy={actionState.isBusy("update-order-item")}
                onClick={() => {
                  if (!selectedOrder || !selectedItemId) {
                    return;
                  }

                  void actionState.runAction("update-order-item", async () => {
                    try {
                      const order = await apiRequest<Order>(endpoints.orders.item(selectedOrder.id, selectedItemId), {
                        method: "PUT",
                        body: itemDraft
                      });
                      const normalizedOrder = syncSelectedOrder(order);
                      setOrders((current) => current.map((entry) => (entry.id === normalizedOrder.id ? normalizedOrder : entry)));
                      setNotice({ tone: "success", message: "Order item updated." });
                    } catch (requestError: unknown) {
                      setNotice({ tone: "danger", message: getApiErrorMessage(requestError) });
                    }
                  });
                }}
                type="button"
              >
                {t("Update item")}
              </ActionButton>
              ) : null}
              {canManageItemStatus ? (
              <ActionButton
                className="ghost"
                disabled={!selectedOrder || !selectedItemId || !updateItemStatusPermission.allowed}
                disabledReason={updateItemStatusPermission.reason}
                busyLabel="Updating..."
                isBusy={actionState.isBusy("update-order-item-status")}
                onClick={() => {
                  if (!selectedOrder || !selectedItemId) {
                    return;
                  }

                  if (!itemStatusDraft.trim()) {
                    return;
                  }

                  void actionState.runAction("update-order-item-status", async () => {
                    try {
                      await apiRequest(endpoints.orders.itemStatus(selectedItemId), {
                        method: "PUT",
                        body: { status: itemStatusDraft.trim() }
                      });
                      const order = await apiRequest<Order>(endpoints.orders.byId(selectedOrder.id));
                      const normalizedOrder = syncSelectedOrder(order);
                      setOrders((current) => current.map((entry) => (entry.id === normalizedOrder.id ? normalizedOrder : entry)));
                      setNotice({ tone: "success", message: "Order item status updated." });
                    } catch (requestError: unknown) {
                      setNotice({ tone: "danger", message: getApiErrorMessage(requestError) });
                    }
                  });
                }}
                type="button"
              >
                {t("Update item status")}
              </ActionButton>
              ) : null}
              {canEditOrderComposition ? (
              <ActionButton
                className="danger"
                disabled={!selectedOrder || !selectedItemId || !deleteItemPermission.allowed}
                disabledReason={deleteItemPermission.reason}
                busyLabel="Deleting..."
                isBusy={actionState.isBusy("delete-order-item")}
                onClick={() => {
                  if (!selectedOrder || !selectedItemId) {
                    return;
                  }

                  void actionState.runAction("delete-order-item", async () => {
                    try {
                      await apiRequest(endpoints.orders.item(selectedOrder.id, selectedItemId), {
                        method: "DELETE"
                      });
                      const order = await apiRequest<Order>(endpoints.orders.byId(selectedOrder.id));
                      syncSelectedOrder(order);
                      setSelectedItemId(null);
                      setNotice({ tone: "success", message: "Order item deleted." });
                    } catch (requestError: unknown) {
                      setNotice({ tone: "danger", message: getApiErrorMessage(requestError) });
                    }
                  });
                }}
                type="button"
              >
                {t("Delete item")}
              </ActionButton>
              ) : null}
            </div>
            {!selectedItemId && canManageItemStatus && !canEditOrderComposition ? (
              <Banner tone="neutral" message={t("Select an order item first to update its status.")} />
            ) : null}
          </div>
          ) : null}
        </Card>
      ) : null}
    </div>
  );
}

export function PaymentsPage(): JSX.Element {
  const auth = useAuth();
  const { language, t } = useI18n();
  const { notice, setNotice } = useNotice();
  const [payments, setPayments] = useState<Payment[]>([]);
  const [ordersByNationalId, setOrdersByNationalId] = useState<Order[]>([]);
  const [orderLookupNationalId, setOrderLookupNationalId] = useState("");
  const [orderId, setOrderId] = useState("");
  const [paymentSearch, setPaymentSearch] = useState("");
  const [workflowResults, setWorkflowResults] = useState<string[]>([]);
  const [fieldErrors, setFieldErrors] = useState<FieldErrors>({});
  const actionState = useActionState();
  const paymentPermission = canAction(auth, "PAYMENT_CREATE");

  const loadPayments = async (): Promise<void> => {
    setPayments(await apiRequest<Payment[]>(endpoints.payments.list));
  };

  const searchOrdersByNationalId = (): void => {
    const nationalId = orderLookupNationalId.trim();

    if (!nationalId) {
      setOrdersByNationalId([]);
      setNotice({ tone: "neutral", message: t("Type a customer national ID to find the order number.") });
      return;
    }

    void actionState.runAction("lookup-payment-orders", async () => {
      try {
        const result = await apiRequest<Order[]>(endpoints.orders.byNationalId(nationalId));
        setOrdersByNationalId(result);

        if (!result.length) {
          setNotice({ tone: "neutral", message: t("No orders matched the national ID.") });
          return;
        }

        setNotice({ tone: "success", message: t("Order list loaded.") });
      } catch (requestError: unknown) {
        setOrdersByNationalId([]);
        setNotice({ tone: "danger", message: asErrorMessage(requestError) });
      }
    });
  };

  useEffect(() => {
    void loadPayments().catch((requestError: unknown) => setNotice({ tone: "danger", message: asErrorMessage(requestError) }));
  }, []);

  const visiblePayments = useMemo(() => {
    const query = paymentSearch.trim().toLowerCase();
    const source = [...payments];

    if (!query) {
      return source.slice(0, 8);
    }

    return source
      .filter((payment) =>
        [
          getPaymentIdentifier(payment),
          getPaymentOrderIdentifier(payment),
          getPaymentMethodLabel(payment),
          getPaymentAmountLabel(payment),
          getPaymentTimeLabel(payment)
        ]
          .join(" ")
          .toLowerCase()
          .includes(query)
      )
      .slice(0, 12);
  }, [paymentSearch, payments]);

  const latestPayment = payments[0] ?? null;

  const printReceipt = async (payment: Payment): Promise<void> => {
    const escapeHtml = (value?: string | number | null): string =>
      String(value ?? "")
        .replace(/&/g, "&amp;")
        .replace(/</g, "&lt;")
        .replace(/>/g, "&gt;")
        .replace(/"/g, "&quot;")
        .replace(/'/g, "&#39;");

    const order = await apiRequest<Order>(endpoints.orders.byId(payment.orderId));
    const customer = await apiRequest<Customer>(endpoints.customers.byId(order.customerId)).catch(() => null);
    const receiptWindow = window.open("", "_blank", "width=960,height=720");

    if (!receiptWindow) {
      throw new Error("Allow pop-ups to open the receipt window.");
    }

    const direction = language === "ar" ? "rtl" : "ltr";
    const receiptNumber = String(payment.id).padStart(5, "0");
    const customerName = customer ? `${customer.firstName} ${customer.lastName}`.trim() : "";
    const membershipNumber = customer?.isMember ? customer.nationalId : "";
    const phoneNumber = customer?.phoneNumber ?? "";
    const lineSummary = order.items
      .map((item) => {
        const details = [item.oliveType, item.bagsCount ? `${item.bagsCount} كيس` : "", `الكمية ${item.quantity}`]
          .filter(Boolean)
          .join(" / ");

        return details ? `${item.productName} - ${details}` : `${item.productName} - الكمية ${item.quantity}`;
      })
      .join("، ");
    const paymentDate = formatDateTime(payment.paymentDate);
    const totalLabel = formatCurrency(payment.totalPrice);
    const [shekelPart, agorotPart] = payment.totalPrice.toFixed(2).split(".");
    const developmentFee = "";
    const pressingFee = shekelPart;
    const totalShekel = shekelPart;

    receiptWindow.document.write(`
      <!doctype html>
      <html lang="${language}" dir="${direction}">
        <head>
          <meta charset="utf-8" />
          <title>فاتورة / إيصال</title>
          <style>
            :root { color-scheme: light; }
            * { box-sizing: border-box; }
            body {
              margin: 0;
              padding: 18px;
              background: #ececec;
              color: #181818;
              font-family: "Noto Naskh Arabic", "Segoe UI", Tahoma, Arial, sans-serif;
            }
            .sheet {
              width: 210mm;
              min-height: 297mm;
              margin: 0 auto;
              background: #fff;
              padding: 12mm 12mm 14mm;
              box-shadow: 0 18px 40px rgba(0, 0, 0, 0.12);
              border: 1px solid #d8d8d8;
            }
            .header {
              display: grid;
              grid-template-columns: 1fr auto 1fr;
              gap: 10px;
              align-items: start;
              text-align: center;
              border-bottom: 1px solid #6f6f6f;
              padding-bottom: 8mm;
            }
            .header-block {
              font-size: 12px;
              line-height: 1.7;
            }
            .header-block strong {
              display: block;
              font-size: 13px;
            }
            .header-center {
              display: grid;
              justify-items: center;
              gap: 6px;
              padding-top: 1mm;
            }
            .header-logo-frame {
              width: 190px;
              min-height: 112px;
              padding: 0;
              display: grid;
              place-items: center;
              background: transparent;
              border: 0;
              border-radius: 0;
              box-shadow: none;
              overflow: hidden;
            }
            .header-center img {
              width: 100%;
              max-width: none;
              height: 108px;
              max-height: none;
              object-fit: contain;
              object-position: center;
              display: block;
              transform: scale(1.18);
              transform-origin: center;
            }
            .header-center .mark {
              font-size: 11px;
              line-height: 1.35;
              max-width: 190px;
              font-weight: 700;
            }
            .receipt-head {
              display: grid;
              grid-template-columns: 1fr auto 1fr;
              align-items: center;
              margin: 9mm 0 10mm;
              font-size: 18px;
            }
            .receipt-title {
              text-align: center;
              font-size: 18px;
              font-weight: 700;
              text-decoration: underline;
              text-underline-offset: 5px;
            }
            .receipt-no {
              text-align: left;
              font-size: 17px;
              letter-spacing: 0.08em;
              direction: ltr;
            }
            .row-line {
              display: grid;
              grid-template-columns: auto 1fr auto 1fr;
              align-items: end;
              gap: 8px;
              margin-top: 9mm;
              font-size: 15px;
            }
            .line-fill {
              min-height: 22px;
              border-bottom: 1px dotted #909090;
              padding: 0 4px 2px;
              font-weight: 600;
            }
            .statement {
              margin-top: 9mm;
              font-size: 15px;
            }
            .statement .line-fill {
              display: inline-block;
              width: calc(100% - 70px);
              vertical-align: bottom;
            }
            .table-wrap {
              margin-top: 7mm;
              border: 1px solid #6f6f6f;
            }
            table {
              width: 100%;
              border-collapse: collapse;
              table-layout: fixed;
              font-size: 15px;
            }
            th, td {
              border: 1px solid #6f6f6f;
              padding: 10px 8px;
              vertical-align: top;
            }
            th {
              text-align: center;
              font-weight: 700;
            }
            .desc-col { width: 76%; }
            .amount-col { width: 14%; text-align: center; }
            .currency-col { width: 10%; text-align: center; }
            .notes {
              min-height: 132px;
            }
            .line-item {
              display: flex;
              justify-content: space-between;
              align-items: center;
              min-height: 38px;
              gap: 12px;
            }
            .line-item.total {
              font-weight: 700;
            }
            .meta-footer {
              display: grid;
              grid-template-columns: 1fr 1fr;
              gap: 26px;
              margin-top: 16mm;
              font-size: 15px;
            }
            .sign-line {
              display: grid;
              grid-template-columns: auto 1fr;
              gap: 10px;
              align-items: end;
            }
            .sign-line .line-fill {
              min-height: 24px;
            }
            .slogan {
              margin-top: 16mm;
              text-align: center;
              font-size: 18px;
              font-weight: 700;
            }
            @page { size: A4; margin: 0; }
            @media print {
              body { background: #fff; padding: 0; }
              .sheet { box-shadow: none; border: 0; margin: 0; width: auto; min-height: auto; }
            }
          </style>
        </head>
        <body>
          <div class="sheet">
            <div class="header">
              <div class="header-block" dir="ltr">
                <strong>The Cooperative Society For</strong>
                <div>Pressing Olive Industrializing &amp;</div>
                <div>Marketing Its Products</div>
                <div>Bethlehem</div>
                <div>Tel: 02-2742379</div>
                <div>Fax: 02-2777595</div>
                <div>Mob: 0592 632711</div>
                <div>E-mail: olivecoop3@gmail.com</div>
              </div>
              <div class="header-center">
                <div class="header-logo-frame">
                  <img alt="Press logo" src="${new URL(publicAsset("press-logo-badge.png"), window.location.origin).toString()}" />
                </div>
                <div class="mark">الجمعية التعاونية لعصر الزيتون</div>
              </div>
              <div class="header-block">
                <strong>الجمعية التعاونية لعصر الزيتون</strong>
                <div>وتصنيعه وتسويق منتجاته</div>
                <div>محافظة بيت لحم - بيت جالا</div>
                <div>تلفون: 02-2742379</div>
                <div>فاكس: 02-2777595</div>
                <div>جوال: 0592 632711</div>
                <div>بريد إلكتروني: olivecoop3@gmail.com</div>
              </div>
            </div>

            <div class="receipt-head">
              <div class="receipt-no">No. ${escapeHtml(receiptNumber)}</div>
              <div class="receipt-title">فاتورة / إيصال</div>
              <div></div>
            </div>

            <div class="row-line">
              <div>وصلني من</div>
              <div class="line-fill">${escapeHtml(customerName)}</div>
              <div>رقم العضوية</div>
              <div class="line-fill">${escapeHtml(membershipNumber)}</div>
            </div>

            <div class="row-line">
              <div>عنوانه</div>
              <div class="line-fill"></div>
              <div>الهاتف</div>
              <div class="line-fill">${escapeHtml(phoneNumber)}</div>
            </div>

            <div class="statement">
              وذلك عن <span class="line-fill">${escapeHtml(lineSummary)}</span>
            </div>

            <div class="table-wrap">
              <table>
                <thead>
                  <tr>
                    <th class="desc-col" rowspan="2">البيان</th>
                    <th colspan="2">المبلغ</th>
                  </tr>
                  <tr>
                    <th class="amount-col">شيكل</th>
                    <th class="currency-col">أغورة</th>
                  </tr>
                </thead>
                <tbody>
                  <tr>
                    <td class="notes">
                      <div class="line-item"><span>مساهمة تطوير</span><span></span></div>
                      <div class="line-item"><span>عمولة عصر</span><span>${escapeHtml(lineSummary)}</span></div>
                      <div class="line-item total"><span>المجموع</span><span>${escapeHtml(totalLabel)}</span></div>
                    </td>
                    <td class="amount-col">
                      <div class="line-item"><span>${escapeHtml(developmentFee)}</span></div>
                      <div class="line-item"><span>${escapeHtml(pressingFee)}</span></div>
                      <div class="line-item total"><span>${escapeHtml(totalShekel)}</span></div>
                    </td>
                    <td class="currency-col">
                      <div class="line-item"><span></span></div>
                      <div class="line-item"><span>${escapeHtml(agorotPart)}</span></div>
                      <div class="line-item total"><span>${escapeHtml(agorotPart)}</span></div>
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>

            <div class="meta-footer">
              <div class="sign-line">
                <div>التوقيع</div>
                <div class="line-fill"></div>
              </div>
              <div class="sign-line">
                <div>التاريخ</div>
                <div class="line-fill">${escapeHtml(paymentDate)}</div>
              </div>
            </div>

            <div class="slogan">من الشجر إلى الحجر نحصل على زيت ذو جودة عالية</div>
          </div>
          <script>window.print();</script>
        </body>
      </html>
    `);
    receiptWindow.document.close();
  };

  return (
    <div className="content-grid">
      <PageHeader title={language === "ar" ? "المدفوعات" : "Payments"} />
      <NoticeBlock notice={notice} />
      <div className="content-grid two">
        <Card title={t("Payment Summary")} subtitle={t("Quick view for accountants.")}>
          <div className="form-grid two">
            <div className="card">
              <strong>{t("Payments loaded")}</strong>
              <div>{payments.length}</div>
            </div>
            <div className="card">
              <strong>{t("Latest order")}</strong>
              <div>{latestPayment ? `#${getPaymentOrderIdentifier(latestPayment)}` : "-"}</div>
            </div>
          </div>
        </Card>
        <Card title={t("Recent Payments")} subtitle={t("Search by payment or order number.")}>
          <div className="form-grid">
            <div className="field">
              <label>{t("Search payments")}</label>
              <input
                onChange={(event) => setPaymentSearch(event.target.value)}
                placeholder={t("Payment ID or order ID")}
                value={paymentSearch}
              />
            </div>
          </div>
          {visiblePayments.length ? (
            <div className="table-wrap">
              <table className="data-table">
                <thead>
                  <tr>
                    <th>{t("Payment")}</th>
                    <th>{t("Order")}</th>
                    <th>{t("Amount")}</th>
                    <th>{t("Method")}</th>
                    <th>{t("Time")}</th>
                    <th>{t("Actions")}</th>
                  </tr>
                </thead>
                <tbody>
                  {visiblePayments.map((payment) => (
                    <tr key={`${getPaymentIdentifier(payment)}-${getPaymentOrderIdentifier(payment)}`}>
                      <td>{getPaymentIdentifier(payment)}</td>
                      <td>{getPaymentOrderIdentifier(payment)}</td>
                      <td>{getPaymentAmountLabel(payment)}</td>
                      <td>{getPaymentMethodLabel(payment)}</td>
                      <td>{getPaymentTimeLabel(payment)}</td>
                      <td>
                        <button
                          className="btn receipt-print-button"
                          onClick={() => {
                            void printReceipt(payment).catch((requestError: unknown) =>
                              setNotice({ tone: "danger", message: getApiErrorMessage(requestError) })
                            );
                          }}
                          type="button"
                        >
                          {t("Print receipt")}
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          ) : (
            <EmptyState title="No payments found" description="Try a different payment or order number." />
          )}
        </Card>
      </div>
      <div className="content-grid two">
        <AccountantSalesCard onPaymentCreated={loadPayments} />
        <Card title={t("Create Payment")} subtitle={t("Record payment for an order.")}>
          <div className="form-grid">
            <div className="field">
              <label>{t("Customer National ID")}</label>
              <input
                onChange={(event) => setOrderLookupNationalId(event.target.value)}
                placeholder={t("Use the customer national ID to find the order number.")}
                value={orderLookupNationalId}
              />
            </div>
            <div className="inline-actions">
              <ActionButton
                className="secondary"
                busyLabel={t("Searching...")}
                isBusy={actionState.isBusy("lookup-payment-orders")}
                onClick={searchOrdersByNationalId}
                type="button"
              >
                {t("Search orders")}
              </ActionButton>
            </div>
          </div>
          {ordersByNationalId.length ? (
            <div className="table-wrap">
              <table className="data-table">
                <thead>
                  <tr>
                    <th>{t("Order")}</th>
                    <th>{t("Customer ID")}</th>
                    <th>{t("Created")}</th>
                    <th>{t("Actions")}</th>
                  </tr>
                </thead>
                <tbody>
                  {ordersByNationalId.map((order) => (
                    <tr key={order.id}>
                      <td>#{order.id}</td>
                      <td>{order.customerId}</td>
                      <td>{formatDateTime(order.createdAt)}</td>
                      <td>
                        <button
                          className="btn ghost"
                          onClick={() => {
                            setOrderId(String(order.id));
                            setNotice({ tone: "success", message: t("Order ID copied to the payment field.") });
                          }}
                          type="button"
                        >
                          {t("Use order")}
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          ) : null}
          <ErrorSummary errors={fieldErrors} />
          <div className="form-grid">
            <div className="field">
              <label>{t("Order ID")}</label>
              <input onChange={(event) => setOrderId(event.target.value)} value={orderId} />
              <FieldError errors={fieldErrors} name="orderId" />
            </div>
            <div className="field">
              <label>{t("Payment Method")}</label>
              <input disabled value={t("Cash Only")} />
            </div>
            <div className="inline-actions">
              <ActionButton
                disabled={!paymentPermission.allowed}
                disabledReason={paymentPermission.reason}
                busyLabel="Creating..."
                isBusy={actionState.isBusy("create-payment")}
                onClick={() => {
                  const nextErrors = validatePayment({ orderId });
                  setFieldErrors(nextErrors);
                  setWorkflowResults([]);

                  if (Object.keys(nextErrors).length) {
                    return;
                  }

                  void actionState.runAction("create-payment", async () => {
                    try {
                      const payment = await apiRequest<Payment>(endpoints.payments.create, {
                        method: "POST",
                        body: { orderId: Number(orderId) }
                      });
                      await loadPayments();
                      setFieldErrors({});
                      setWorkflowResults([]);
                      setOrderId("");
                      setNotice({
                        tone: "success",
                        message:
                          language === "ar"
                            ? `ØªÙ… ØªØ³Ø¬ÙŠÙ„ Ø¯ÙØ¹Ø© Ø§Ù„Ø·Ù„Ø¨ #${payment.orderId} Ø¨Ù†Ø¬Ø§Ø­ Ø¨Ù‚ÙŠÙ…Ø© ${formatCurrency(payment.totalPrice)}ØŒ ÙˆØ£ØµØ¨Ø­ Ø§Ù„Ø·Ù„Ø¨ Ù…Ø¯ÙÙˆØ¹Ø§Ù‹.`
                            : `Payment for order #${payment.orderId} was recorded successfully for ${formatCurrency(payment.totalPrice)}. The order is now marked as paid.`
                      });
                    } catch (requestError: unknown) {
                      setFieldErrors((current) => ({ ...current, ...getApiFieldErrors(requestError) }));
                      setNotice({ tone: "danger", message: getApiErrorMessage(requestError) });
                    }
                  });
                }}
                type="button"
              >
                {t("Create payment")}
              </ActionButton>
            </div>
            <PermissionNote allowed={paymentPermission.allowed} reason={paymentPermission.reason} />
            {workflowResults.length ? (
              <div className="card">
                <strong>{t("Queue update")}</strong>
                <ul>
                  {workflowResults.map((line) => (
                    <li key={line}>{line}</li>
                  ))}
                </ul>
              </div>
            ) : null}
            {latestPayment ? (
              <div className="inline-actions">
                <ActionButton
                  className="receipt-print-button"
                  onClick={() => {
                    void printReceipt(latestPayment).catch((requestError: unknown) =>
                      setNotice({ tone: "danger", message: getApiErrorMessage(requestError) })
                    );
                  }}
                  type="button"
                >
                  {t("Print receipt")}
                </ActionButton>
              </div>
            ) : null}
          </div>
        </Card>
      </div>
      <Card title="Payment Ledger">
        {payments.length ? (
          <div className="table-wrap">
            <table className="data-table">
              <thead>
                <tr>
                  <th>{t("ID")}</th>
                  <th>{t("Order")}</th>
                  <th>{t("Total")}</th>
                  <th>{t("Type")}</th>
                  <th>{t("Date")}</th>
                  <th>{t("Actions")}</th>
                </tr>
              </thead>
              <tbody>
                {payments.map((payment) => (
                  <tr key={payment.id}>
                    <td>{payment.id}</td>
                    <td>{payment.orderId}</td>
                    <td>{formatCurrency(payment.totalPrice)}</td>
                    <td>{payment.paymentType}</td>
                    <td>{formatDateTime(payment.paymentDate)}</td>
                    <td>
                      <button
                        className="btn receipt-print-button"
                        onClick={() => {
                          void printReceipt(payment).catch((requestError: unknown) =>
                            setNotice({ tone: "danger", message: getApiErrorMessage(requestError) })
                          );
                        }}
                        type="button"
                      >
                        {t("Print receipt")}
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ) : (
          <EmptyState title="No payments loaded" description="Create a payment or refresh once the accounting user has activity." />
        )}
      </Card>
    </div>
  );
}

export function ReportsPage(): JSX.Element {
  const auth = useAuth();
  const { language, t } = useI18n();
  const { notice, setNotice } = useNotice();
  const [reportFrom, setReportFrom] = useState("");
  const [reportTo, setReportTo] = useState("");
  const actionState = useActionState();
  const reportPermission = canAction(auth, "PAYMENT_REPORT_EXPORT");

  return (
    <div className="content-grid">
      <PageHeader
        eyebrow={language === "ar" ? "المحاسبة" : "Accounting"}
        title={language === "ar" ? "التقارير" : "Reports"}
        description={language === "ar" ? "صدّر تقارير المدفوعات اليومية أو حسب فترة محددة." : "Export daily or period payment reports."}
      />
      <NoticeBlock notice={notice} />
      <div className="content-grid">
        <Card title={language === "ar" ? "تقرير حسب الفترة" : "Period Excel Report"} subtitle={language === "ar" ? "اختر تاريخ البداية والنهاية ثم حمّل الملف." : "Choose a start and end date, then export."}>
          <div className="form-grid">
            <div className="field">
              <label>{t("From")}</label>
              <input onChange={(event) => setReportFrom(event.target.value)} type="date" value={reportFrom} />
            </div>
            <div className="field">
              <label>{t("To")}</label>
              <input onChange={(event) => setReportTo(event.target.value)} type="date" value={reportTo} />
            </div>
            <div className="inline-actions">
              <ActionButton
                className="secondary"
                disabled={!reportPermission.allowed || !reportFrom || !reportTo}
                disabledReason={!reportFrom || !reportTo ? (language === "ar" ? "اختر تاريخ البداية والنهاية." : "Select both dates first.") : reportPermission.reason}
                busyLabel={language === "ar" ? "جار التجهيز..." : "Preparing..."}
                isBusy={actionState.isBusy("period-report")}
                onClick={() => {
                  void actionState.runAction("period-report", async () => {
                    try {
                      const blob = await apiRequest<Blob>(endpoints.payments.periodReport(reportFrom, reportTo), { responseType: "blob" });
                      downloadBlob(blob, `payments-${reportFrom}-${reportTo}.xlsx`);
                      setNotice({ tone: "success", message: language === "ar" ? "تم تجهيز تقرير الفترة." : "Period report prepared." });
                    } catch (requestError: unknown) {
                      setNotice({ tone: "danger", message: getApiErrorMessage(requestError) });
                    }
                  });
                }}
                type="button"
              >
                {t("Period report")}
              </ActionButton>
            </div>
            <PermissionNote allowed={reportPermission.allowed} reason={reportPermission.reason} />
          </div>
        </Card>
      </div>
    </div>
  );
}

export function QueuePage(): JSX.Element {
  const auth = useAuth();
  const { language, t } = useI18n();
  const { notice, setNotice } = useNotice();
  const [productionStatus, setProductionStatus] = useState<QueueStatus | null>(null);
  const [orderId, setOrderId] = useState("");
  const [action, setAction] = useState("NEXT");
  const actionState = useActionState();
  const advancePermission = canAction(auth, "QUEUE_ADVANCE");
  const productionTicketPermission = canAction(auth, "QUEUE_ISSUE_PRODUCTION");
  const canManageQueue = advancePermission.allowed || productionTicketPermission.allowed;
  const selectedQueueStatus = productionStatus;
  const selectedServingTicket = selectedQueueStatus?.serving[0]?.ticket ?? null;
  const selectedNextTicket = selectedQueueStatus?.waiting[0] ?? null;

  const loadQueues = async (): Promise<void> => {
    setProductionStatus(await apiRequest<QueueStatus>(endpoints.queues.status("PRODUCTION")));
  };

  useEffect(() => {
    void loadQueues().catch((requestError: unknown) => setNotice({ tone: "danger", message: asErrorMessage(requestError) }));
    const timer = window.setInterval(() => {
      void loadQueues().catch(() => undefined);
    }, 20000);
    return () => window.clearInterval(timer);
  }, []);

  const renderQueue = (title: string, status: QueueStatus | null): JSX.Element => (
    <Card title={title}>
      {status ? (
        <div className="queue-operator-card">
          <div className="queue-operator-hero">
            <span>{t("Now Serving")}</span>
            <strong>{status.serving[0]?.ticket.number ?? "--"}</strong>
            <small>
              {status.serving[0]?.ticket.orderId
                ? `${t("Order")} #${status.serving[0].ticket.orderId}`
                : t("Idle")}
            </small>
          </div>

          <div className="queue-operator-next">
            <div className="queue-operator-stats">
              <span>
                {t("Waiting")} <strong>{status.stats.totalWaiting}</strong>
              </span>
              <span>
                {t("Average Wait")} <strong>{status.stats.averageWaitTime} {t("min")}</strong>
              </span>
            </div>
            <div className="queue-ticket-strip">
              {status.waiting.slice(0, 5).map((ticket) => (
                <div className="queue-ticket-chip" key={ticket.id}>
                  <strong>{ticket.number}</strong>
                  <span>#{ticket.orderId}</span>
                </div>
              ))}
              {!status.waiting.length ? (
                <div className="queue-ticket-chip empty">
                  <strong>--</strong>
                  <span>{t("Queue is clear")}</span>
                </div>
              ) : null}
            </div>
          </div>
        </div>
      ) : (
        <EmptyState title="Queue not loaded" description="Refresh the queue status or make sure the queue service is available." />
      )}
    </Card>
  );

  return (
    <div className="content-grid">
      <PageHeader eyebrow="Queueing" title="Production Queue" description="Monitor the production queue and run the available actions." />
      <NoticeBlock notice={notice} />
      {canManageQueue ? (
      <div className="content-grid">
        <Card title="Queue Actions">
          <div className="form-grid">
            <div className="field">
              <label>{t("Advance Action")}</label>
              <select onChange={(event) => setAction(event.target.value)} value={action}>
                <option value="NEXT">{t("NEXT")}</option>
                <option value="SKIP">{t("SKIP")}</option>
              </select>
            </div>
            <div className="field">
              <label>{t("Order ID for Ticket Issue")}</label>
              <input onChange={(event) => setOrderId(event.target.value)} value={orderId} />
            </div>
            <div className="queue-action-summary">
              <div>
                <span>{t("Now Serving")}</span>
                <strong>{selectedServingTicket?.number ?? "--"}</strong>
                <small>{selectedServingTicket?.orderId ? `${t("Order")} #${selectedServingTicket.orderId}` : t("Idle")}</small>
              </div>
              <div>
                <span>{t("Next In Line")}</span>
                <strong>{selectedNextTicket?.number ?? "--"}</strong>
                <small>{selectedNextTicket?.orderId ? `${t("Order")} #${selectedNextTicket.orderId}` : t("Queue is clear")}</small>
              </div>
            </div>
            <div className="inline-actions">
              <ActionButton
                className="secondary"
                disabled={!advancePermission.allowed}
                disabledReason={advancePermission.reason}
                busyLabel="Opening..."
                isBusy={actionState.isBusy("queue-station-login")}
                onClick={() => {
                  void actionState.runAction("queue-station-login", async () => {
                    try {
                      await apiRequest(endpoints.queues.tellerLogin("PRODUCTION"), {
                        method: "POST",
                        responseType: "text"
                      });
                      await loadQueues();
                      setNotice({ tone: "success", message: "PRODUCTION station is open." });
                    } catch (requestError: unknown) {
                      setNotice({ tone: "danger", message: getApiErrorMessage(requestError) });
                    }
                  });
                }}
                type="button"
              >
                {t("Open station")}
              </ActionButton>
              <ActionButton
                disabled={!advancePermission.allowed}
                disabledReason={advancePermission.reason}
                busyLabel="Advancing..."
                isBusy={actionState.isBusy("advance-queue")}
                onClick={() => {
                  void actionState.runAction("advance-queue", async () => {
                    try {
                      await apiRequest(endpoints.queues.advance("PRODUCTION", action), { method: "POST" });
                      await loadQueues();
                      setNotice({
                        tone: "success",
                        message:
                          language === "ar"
                            ? `تم تحريك طابور الإنتاج بواسطة ${action}.`
                            : `PRODUCTION queue advanced with ${action}.`
                      });
                    } catch (requestError: unknown) {
                      setNotice({ tone: "danger", message: getApiErrorMessage(requestError) });
                    }
                  });
                }}
                type="button"
              >
                {t("Advance queue")}
              </ActionButton>
              <ActionButton
                className="ghost"
                disabled={!advancePermission.allowed}
                disabledReason={advancePermission.reason}
                busyLabel="Closing..."
                isBusy={actionState.isBusy("queue-station-logout")}
                onClick={() => {
                  void actionState.runAction("queue-station-logout", async () => {
                    try {
                      await apiRequest(endpoints.queues.tellerLogout("PRODUCTION"), {
                        method: "POST",
                        responseType: "text"
                      });
                      setNotice({ tone: "success", message: "PRODUCTION station is closed." });
                    } catch (requestError: unknown) {
                      setNotice({ tone: "danger", message: getApiErrorMessage(requestError) });
                    }
                  });
                }}
                type="button"
              >
                {t("Close station")}
              </ActionButton>
              <button
                className="btn ghost"
                onClick={() => window.open("/queue-display?queue=PRODUCTION", "_blank", "noopener,noreferrer")}
                type="button"
              >
                {t("Open display")}
              </button>
              <ActionButton
                className="secondary"
                disabled={!productionTicketPermission.allowed}
                disabledReason={productionTicketPermission.reason}
                busyLabel="Issuing..."
                isBusy={actionState.isBusy("issue-production-ticket")}
                onClick={() => {
                  void actionState.runAction("issue-production-ticket", async () => {
                    try {
                      await apiRequest(endpoints.queues.issueProduction(orderId), { method: "POST" });
                      await loadQueues();
                      setNotice({ tone: "success", message: `Issued production ticket for order #${orderId}.` });
                    } catch (requestError: unknown) {
                      setNotice({ tone: "danger", message: getApiErrorMessage(requestError) });
                    }
                  });
                }}
                type="button"
              >
                Issue production ticket
              </ActionButton>
            </div>
            <PermissionNote allowed={advancePermission.allowed} reason={advancePermission.reason} />
            <PermissionNote allowed={productionTicketPermission.allowed} reason={productionTicketPermission.reason} />
          </div>
        </Card>
      </div>
      ) : null}
      <div className="content-grid">
        {renderQueue("Production Queue", productionStatus)}
      </div>
    </div>
  );
}

type ProductionLineCode = "A" | "B";
type TechnicianProductionStep = "NEW" | "IN_PROGRESS" | "READY_FOR_PICKUP" | "COMPLETED";
type TechnicianActionStatus = "IN_PROGRESS" | "READY_FOR_PICKUP" | "COMPLETED";

const PRODUCTION_LINES: ProductionLineCode[] = ["A", "B"];
const QUEUE_CONTROL_REFRESH_MS = 2000;

function lineLabel(line: ProductionLineCode, language: "ar" | "en"): string {
  return language === "ar" ? `خط الإنتاج ${line}` : `Production Line ${line}`;
}

function statusLabel(status: TechnicianActionStatus, language: "ar" | "en"): string {
  if (language !== "ar") {
    return status === "IN_PROGRESS" ? "In progress" : status === "READY_FOR_PICKUP" ? "Filling" : "Done";
  }

  return status === "IN_PROGRESS" ? "قيد التنفيذ" : status === "READY_FOR_PICKUP" ? "تعبئة" : "انتهاء";
}

async function syncTrackingStatus(orderId: number, nextStatus: TechnicianActionStatus, tankCode?: string): Promise<void> {
  const tracking = await apiRequest<BatchTracking>(endpoints.tracking.byOrder(orderId));

  if (tankCode) {
    await apiRequest(endpoints.tracking.tank(tracking.batchId), {
      method: "PUT",
      body: { tankCode }
    });
  }

  if (nextStatus === "READY_FOR_PICKUP") {
    return;
  }

  await apiRequest(endpoints.tracking.status(tracking.batchId), {
    method: "PUT",
    body: { status: nextStatus === "COMPLETED" ? "DONE" : "IN_PROGRESS" }
  });
}

async function syncTrackingLine(orderId: number, productionLine: ProductionLineCode): Promise<void> {
  const tracking = await apiRequest<BatchTracking>(endpoints.tracking.byOrder(orderId));
  await apiRequest(endpoints.tracking.line(tracking.batchId), {
    method: "PUT",
    body: { productionLine }
  });
}

export function QueueControlPage(): JSX.Element {
  const { language, t } = useI18n();
  const { notice, setNotice } = useNotice();
  const [status, setStatus] = useState<QueueStatus | null>(null);
  const [fillingTicket, setFillingTicket] = useState<QueueTicket | null>(null);
  const [ticketSteps, setTicketSteps] = useState<Record<string, TechnicianProductionStep>>({});
  const actionState = useActionState();
  const isArabic = language === "ar";

  const loadQueue = async (): Promise<void> => {
    setStatus(await apiRequest<QueueStatus>(endpoints.queues.status("PRODUCTION")));
  };

  useEffect(() => {
    void loadQueue().catch((requestError: unknown) => setNotice({ tone: "danger", message: getApiErrorMessage(requestError) }));
    const timer = window.setInterval(() => {
      void loadQueue().catch(() => undefined);
    }, QUEUE_CONTROL_REFRESH_MS);
    return () => window.clearInterval(timer);
  }, []);

  const setTicketServing = (ticketId: string | number, line: ProductionLineCode): void => {
    void actionState.runAction(`line-${line}-serve-${ticketId}`, async () => {
      try {
        await apiRequest(endpoints.queues.ticketStatus(ticketId, "PRODUCTION", "SERVING", line), { method: "PUT" });
        const servedTicket = status?.waiting.find((ticket) => String(ticket.id) === String(ticketId));
        if (servedTicket?.orderId) {
          await syncTrackingLine(servedTicket.orderId, line).catch(() => undefined);
        }
        await loadQueue();
        setNotice({
          tone: "success",
          message: isArabic ? `تم نقل الطلب إلى ${lineLabel(line, language)}.` : `Ticket moved to ${lineLabel(line, language)}.`
        });
      } catch (requestError: unknown) {
        setNotice({ tone: "danger", message: getApiErrorMessage(requestError) });
      }
    });
  };

  const ticketStepKey = (ticket: QueueTicket): string => String(ticket.id ?? ticket.orderId ?? ticket.number);

  const getTicketStep = (ticket: QueueTicket): TechnicianProductionStep => ticketSteps[ticketStepKey(ticket)] ?? "NEW";

  const canUseProductionAction = (ticket: QueueTicket, nextStatus: TechnicianActionStatus): boolean => {
    const currentStep = getTicketStep(ticket);
    return (
      (currentStep === "NEW" && nextStatus === "IN_PROGRESS") ||
      (currentStep === "IN_PROGRESS" && nextStatus === "READY_FOR_PICKUP") ||
      (currentStep === "READY_FOR_PICKUP" && nextStatus === "COMPLETED")
    );
  };

  const productionActionClass = (ticket: QueueTicket, nextStatus: TechnicianActionStatus): string => {
    const currentStep = getTicketStep(ticket);
    const canUse = canUseProductionAction(ticket, nextStatus);
    const classes = [nextStatus === "COMPLETED" && canUse ? "queue-finish-button" : "ghost"];

    if (canUse) {
      classes.push("tech-status-button-active");
    } else {
      classes.push("tech-status-button-locked");
    }

    if (
      (currentStep === "IN_PROGRESS" && nextStatus === "IN_PROGRESS") ||
      (currentStep === "READY_FOR_PICKUP" && (nextStatus === "IN_PROGRESS" || nextStatus === "READY_FOR_PICKUP")) ||
      (currentStep === "COMPLETED")
    ) {
      classes.push("tech-status-button-done");
    }

    return classes.join(" ");
  };

  const updateProductionState = (ticket: QueueTicket, nextStatus: TechnicianActionStatus, tankCode?: string): void => {
    if (!ticket.orderId) {
      setNotice({ tone: "danger", message: isArabic ? "لا يوجد رقم طلب لهذه التذكرة." : "This ticket has no order number." });
      return;
    }

    if (!canUseProductionAction(ticket, nextStatus)) {
      setNotice({
        tone: "neutral",
        message: isArabic ? "يجب تنفيذ الخطوات بالترتيب." : "Follow the production steps in order."
      });
      return;
    }

    if (nextStatus === "READY_FOR_PICKUP" && !tankCode) {
      setFillingTicket(ticket);
      return;
    }

    const orderId = ticket.orderId;

    void actionState.runAction(`ticket-${ticket.id}-${nextStatus}${tankCode ? `-${tankCode}` : ""}`, async () => {
      try {
        const order = normalizeOrderForFrontend(await apiRequest<Order>(endpoints.orders.byId(orderId)));
        const oliveItem = getOrderOliveItem(order);

        if (oliveItem) {
          await apiRequest(endpoints.orders.itemStatus(oliveItem.id), {
            method: "PUT",
            body: { status: nextStatus }
          });
        } else {
          await apiRequest(endpoints.orders.status(orderId), {
            method: "PUT",
            body: { status: nextStatus }
          });
        }

        if (nextStatus === "COMPLETED" && ticket.id !== undefined) {
          await apiRequest(endpoints.queues.ticketStatus(ticket.id, "PRODUCTION", "COMPLETED"), { method: "PUT" });
        }

        if (ticket.productionLine === "A" || ticket.productionLine === "B") {
          await syncTrackingLine(orderId, ticket.productionLine).catch(() => undefined);
        }
        await syncTrackingStatus(orderId, nextStatus, tankCode).catch(() => undefined);
        setTicketSteps((current) => ({
          ...current,
          [ticketStepKey(ticket)]: nextStatus
        }));
        setFillingTicket(null);
        await loadQueue();
        setNotice({
          tone: "success",
          message: isArabic
            ? `تم تحديث حالة الطلب إلى ${statusLabel(nextStatus, language)}${tankCode ? ` في الخزان ${tankCode}` : ""}.`
            : `Order status updated to ${statusLabel(nextStatus, language)}${tankCode ? ` in Tank ${tankCode}` : ""}.`
        });
      } catch (requestError: unknown) {
        setNotice({ tone: "danger", message: getApiErrorMessage(requestError) });
      }
    });
  };

  const servingTickets = status?.serving ?? [];
  const hasAssignedProductionLines = servingTickets.some((entry) => entry.ticket.productionLine);
  const servingByLine = PRODUCTION_LINES.map((line) => ({
    line,
    serving: hasAssignedProductionLines
      ? servingTickets.filter((entry) => entry.ticket.productionLine === line)
      : servingTickets.filter((_, index) => PRODUCTION_LINES[index % PRODUCTION_LINES.length] === line)
  }));
  const waitingTickets = status?.waiting ?? [];
  const firstWaitingTicketId = waitingTickets[0]?.id;

  return (
    <div className="content-grid">
      <PageHeader
        eyebrow={isArabic ? "الفني" : "Technician"}
        title={isArabic ? "التحكم بالأدوار" : "Production Queue Control"}
        description={isArabic ? "حرّك طلبات عصر الزيتون بين الأدوار التالية وخطي الإنتاج." : "Move olive-pressing orders from the next turns into production lines."}
      />
      <NoticeBlock notice={notice} />

      {fillingTicket ? (
        <div className="modal-backdrop" role="presentation">
          <Card title={isArabic ? "اختيار خزان التعبئة" : "Select filling tank"}>
            <p className="helper-text">
              {isArabic
                ? `اختر الخزان الذي سيتم تخزين زيت الطلب #${fillingTicket.orderId ?? "--"} فيه.`
                : `Choose where to store the oil for order #${fillingTicket.orderId ?? "--"}.`}
            </p>
            <div className="tracking-tanks">
              {(["A", "B", "C", "D"] as const).map((tankCode) => (
                <ActionButton
                  className="ghost"
                  isBusy={actionState.isBusy(`ticket-${fillingTicket.id}-READY_FOR_PICKUP-${tankCode}`)}
                  key={tankCode}
                  onClick={() => updateProductionState(fillingTicket, "READY_FOR_PICKUP", tankCode)}
                  type="button"
                >
                  {isArabic ? `الخزان ${tankCode}` : `Tank ${tankCode}`}
                </ActionButton>
              ))}
            </div>
            <div className="inline-actions">
              <button className="btn ghost" onClick={() => setFillingTicket(null)} type="button">
                {isArabic ? "إلغاء" : "Cancel"}
              </button>
            </div>
          </Card>
        </div>
      ) : null}

      <section className="tech-queue-board">
        <div className="tech-queue-section">
          <div className="tech-queue-section-title">
            <h2>{t("Now Serving")}</h2>
            <span>{isArabic ? "خطا الإنتاج الأساسيان" : "Two active production lines"}</span>
          </div>
          <div className="tech-current-lines">
            {servingByLine.map(({ line, serving }) => (
              <article className="tech-line-card current" key={line}>
                <header>
                  <span>{lineLabel(line, language)}</span>
                  <strong>{serving.length}</strong>
                </header>
                {serving.length ? (
                  <div className="tech-serving-stack">
                    {serving.map((entry) => (
                      <div className="tech-serving-ticket" key={entry.ticket.id ?? `${line}-${entry.ticket.orderId}`}>
                        <div className="tech-ticket-meta">
                          <strong>{entry.ticket.number ?? "--"}</strong>
                          <span>{t("Order")} #{entry.ticket.orderId ?? "--"}</span>
                          <span>{isArabic ? "وقت التشغيل" : "Started"} {formatDateTime(entry.startedAt)}</span>
                        </div>
                        <div className="tech-status-actions">
                          {(["IN_PROGRESS", "READY_FOR_PICKUP", "COMPLETED"] as const).map((nextStatus) => (
                            <ActionButton
                              className={productionActionClass(entry.ticket, nextStatus)}
                              disabled={!canUseProductionAction(entry.ticket, nextStatus)}
                              disabledReason={isArabic ? "يجب تنفيذ الخطوات بالترتيب." : "Follow the steps in order."}
                              isBusy={actionState.isBusy(`ticket-${entry.ticket.id}-${nextStatus}`)}
                              key={nextStatus}
                              onClick={() => updateProductionState(entry.ticket, nextStatus)}
                              type="button"
                            >
                              {statusLabel(nextStatus, language)}
                            </ActionButton>
                          ))}
                        </div>
                      </div>
                    ))}
                  </div>
                ) : (
                  <EmptyState
                    title={isArabic ? "لا يوجد طلب على هذا الخط" : "No active order"}
                    description={isArabic ? "اختر طلباً من الأدوار التالية." : "Move a waiting order into this line."}
                  />
                )}
              </article>
            ))}
          </div>
        </div>

        <div className="tech-queue-section">
          <div className="tech-queue-section-title">
            <h2>{isArabic ? "الأدوار التالية" : "Next Turns"}</h2>
            <span>
              {isArabic
                ? "يجب إدخال أول طلب منتظر فقط، ثم تختار خط الإنتاج المناسب له."
                : "Only the first waiting order can be moved, then choose the production line."}
            </span>
          </div>
          <article className="tech-line-card">
            <header>
              <span>{isArabic ? "الانتظار حسب الدور" : "Waiting by turn"}</span>
              <strong>{waitingTickets.length}</strong>
            </header>
            <div className="tech-waiting-list">
              {waitingTickets.length ? waitingTickets.map((ticket) => {
                const canMoveTicket = ticket.id === firstWaitingTicketId;
                return (
                  <div className={`tech-waiting-ticket ${canMoveTicket ? "first" : "locked"}`} key={ticket.id}>
                    <div>
                      <strong>{ticket.number}</strong>
                      <span>{t("Order")} #{ticket.orderId}</span>
                    </div>
                    <span>{isArabic ? "الوقت المتبقي" : "Remaining"}: {ticket.estimatedWaitMinutes} {t("min")}</span>
                    <div className="tech-line-move-actions">
                      {PRODUCTION_LINES.map((line) => (
                        <ActionButton
                          className={canMoveTicket ? undefined : "ghost"}
                          disabled={!canMoveTicket}
                          disabledReason={
                            canMoveTicket
                              ? undefined
                              : (isArabic
                                ? `يجب إدخال الطلب رقم ${waitingTickets[0]?.number ?? ""} أولاً.`
                                : `Move ticket ${waitingTickets[0]?.number ?? ""} first.`)
                          }
                          isBusy={actionState.isBusy(`line-${line}-serve-${ticket.id}`)}
                          key={line}
                          onClick={() => setTicketServing(ticket.id, line)}
                          type="button"
                        >
                          {isArabic ? `إلى خط ${line}` : `To line ${line}`}
                        </ActionButton>
                      ))}
                    </div>
                  </div>
                );
              }) : (
                <EmptyState title={isArabic ? "لا يوجد انتظار" : "No waiting orders"} description={isArabic ? "ستظهر الطلبات المدفوعة هنا." : "Paid production tickets appear here."} />
              )}
            </div>
          </article>
        </div>
      </section>
    </div>
  );
}

export function LegacyProductionPage(): JSX.Element {
  const auth = useAuth();
  const { language, t } = useI18n();
  const { notice, setNotice } = useNotice();
  const [dashboard, setDashboard] = useState<ProductionDashboardItem[]>([]);
  const [pipeline, setPipeline] = useState<ProductionStage[]>([]);
  const [eta, setEta] = useState<ProductionEta | null>(null);
  const [orderStages, setOrderStages] = useState<ProductionStage[]>([]);
  const [orderId, setOrderId] = useState("");
  const [orderItemId, setOrderItemId] = useState("");
  const [stageItemId, setStageItemId] = useState("");
  const [batchOrderId, setBatchOrderId] = useState("");
  const [batchOrderItemId, setBatchOrderItemId] = useState("");
  const [batchOliveWeightKg, setBatchOliveWeightKg] = useState("");
  const [batchLookupId, setBatchLookupId] = useState("");
  const [selectedBatch, setSelectedBatch] = useState<ProductionBatch | null>(null);
  const [recentBatches, setRecentBatches] = useState<ProductionBatch[]>([]);
  const [predictBatchId, setPredictBatchId] = useState("");
  const [predictFile, setPredictFile] = useState<File | null>(null);
  const [predictPreviewUrl, setPredictPreviewUrl] = useState("");
  const [predictedYieldPercent, setPredictedYieldPercent] = useState<number | null>(null);
  const [predictedOilKg, setPredictedOilKg] = useState<number | null>(null);
  const [predictionConfidence, setPredictionConfidence] = useState<number | null>(null);
  const [predictionBatchLabel, setPredictionBatchLabel] = useState("");
  const [predictionModelVersion, setPredictionModelVersion] = useState("");
  const [predictionErrors, setPredictionErrors] = useState<FieldErrors>({});
  const actionState = useActionState();
  void [batchOrderId, batchOrderItemId, batchOliveWeightKg, batchLookupId, selectedBatch, recentBatches, predictedOilKg];
  const startPermission = canAction(auth, "PRODUCTION_START");
  const etaPermission = canAction(auth, "ETA_VIEW");
  const pipelinePermission = canAction(auth, "PRODUCTION_PIPELINE");
  const stageStartPermission = canAction(auth, "STAGE_START");
  const stageFinishPermission = canAction(auth, "STAGE_FINISH");
  const dashboardPermission = canAction(auth, "PRODUCTION_DASHBOARD");
  const predictionPermission = auth.role === "TECHNICIAN";

  const applySelectedBatch = (batch: ProductionBatch): void => {
    setSelectedBatch(batch);
    setBatchLookupId(batch.batchId);
    setBatchOrderId(String(batch.orderId));
    setBatchOrderItemId(String(batch.orderItemId));
    setBatchOliveWeightKg(String(batch.oliveWeightKg));
    setOrderId(String(batch.orderId));
    setOrderItemId(String(batch.orderItemId));
    setStageItemId(String(batch.orderItemId));
    setPredictedYieldPercent(batch.predictedYieldPercent ?? null);
    setPredictedOilKg(batch.predictedOilKg ?? null);
    setPredictionConfidence(batch.predictionConfidence ?? null);
    setPredictionModelVersion(batch.modelVersion ?? "");
  };

  const loadPredictionWorkspace = async (): Promise<void> => {
    if (!predictionPermission) {
      return;
    }
    setRecentBatches(await apiRequest<ProductionBatch[]>(endpoints.production.batches.list));
  };

  const loadPage = async (): Promise<void> => {
    if (dashboardPermission.allowed) {
      setDashboard(await fetchProductionDashboard());
    }
    if (pipelinePermission.allowed) {
      setPipeline(await fetchProductionPipeline());
    }
    await loadPredictionWorkspace();
  };

  useEffect(() => {
    void loadPage().catch((requestError: unknown) => setNotice({ tone: "danger", message: asErrorMessage(requestError) }));
  }, []);

  useEffect(() => {
    if (!predictFile) {
      setPredictPreviewUrl("");
      return;
    }

    const objectUrl = URL.createObjectURL(predictFile);
    setPredictPreviewUrl(objectUrl);
    return () => URL.revokeObjectURL(objectUrl);
  }, [predictFile]);

  const findNextPipelineStage = (stage: ProductionStage): ProductionStage | null =>
    pipeline
      .filter((candidate) => candidate.line === stage.line && candidate.stageOrder > stage.stageOrder)
      .sort((left, right) => left.stageOrder - right.stageOrder)[0] ?? null;

  const finishPipelineStage = (stage: ProductionStage): void => {
    const nextStage = findNextPipelineStage(stage);
    const nextContainer = nextStage?.container;
    if (!stage.orderItemId || !nextStage || !nextContainer) {
      setNotice({ tone: "danger", message: "No next stage is available for this item." });
      return;
    }

    void apiRequest(endpoints.production.changeStage(stage.orderItemId, nextStage.stageType, nextContainer), {
      method: "PUT"
    })
      .then(() => loadPage())
      .catch((requestError: unknown) => setNotice({ tone: "danger", message: asErrorMessage(requestError) }));
  };

  return (
    <div className="content-grid">
      <PageHeader eyebrow="Production" title="Production Control" description="Start production, monitor the line, complete stages, and check ETA." />
      <NoticeBlock notice={notice} />
      {predictionPermission ? (
      <div className="content-grid two">
        <Card
          title={language === "ar" ? "Ø§Ù„ØªÙ†Ø¨Ø¤ Ø¨Ø§Ù„ØµÙˆØ±" : "Image Prediction"}
          subtitle={
            language === "ar"
              ? "Ø§Ø±ÙØ¹ ØµÙˆØ±Ø© Ø§Ù„Ø¹ÙŠÙ†Ø© ÙˆØ­Ø¯Ø¯ Ø±Ù‚Ù… Ø§Ù„Ø¯ÙØ¹Ø© Ù„Ù„Ø­ØµÙˆÙ„ Ø¹Ù„Ù‰ Ù†Ø³Ø¨Ø© Ø§Ù„Ù…Ø±Ø¯ÙˆØ¯ Ø§Ù„Ù…ØªÙˆÙ‚Ø¹Ø©."
              : "Upload a sample image and batch ID to get the predicted yield."
          }
        >
          <ErrorSummary errors={predictionErrors} />
          <div className="form-grid">
            <div className="field">
              <label>{language === "ar" ? "Ø±Ù‚Ù… Ø§Ù„Ø¯ÙØ¹Ø©" : "Batch ID"}</label>
              <input onChange={(event) => setPredictBatchId(event.target.value)} value={predictBatchId} />
              <FieldError errors={predictionErrors} name="predictBatchId" />
            </div>
            <div className="field">
              <label>{language === "ar" ? "ØµÙˆØ±Ø© Ø§Ù„Ø¹ÙŠÙ†Ø©" : "Sample Image"}</label>
              <label className="upload-dropzone" htmlFor="technician-predict-file">
                <input
                  accept="image/*"
                  id="technician-predict-file"
                  onChange={(event) => setPredictFile(event.target.files?.[0] ?? null)}
                  type="file"
                />
                <span>{language === "ar" ? "Ø§Ø¶ØºØ· Ù„Ø§Ø®ØªÙŠØ§Ø± ØµÙˆØ±Ø© Ø£Ùˆ Ø§Ø³Ø­Ø¨Ù‡Ø§ Ù‡Ù†Ø§" : "Click to choose an image or drop it here"}</span>
                <small>{predictFile?.name ?? (language === "ar" ? "Ù„Ù… ÙŠØªÙ… Ø§Ø®ØªÙŠØ§Ø± Ù…Ù„Ù Ø¨Ø¹Ø¯" : "No file selected yet")}</small>
              </label>
              <FieldError errors={predictionErrors} name="predictFile" />
            </div>
          </div>
          <div className="image-preview-card compact">
            {predictPreviewUrl ? (
              <img alt="Prediction preview" className="image-preview" src={predictPreviewUrl} />
            ) : (
              <div className="image-preview empty">
                {language === "ar" ? "Ø³ØªØ¸Ù‡Ø± Ù…Ø¹Ø§ÙŠÙ†Ø© Ø§Ù„ØµÙˆØ±Ø© Ù‡Ù†Ø§" : "Image preview will appear here"}
              </div>
            )}
          </div>
          <div className="inline-actions">
            <ActionButton
              busyLabel={language === "ar" ? "Ø¬Ø§Ø±Ù Ø§Ù„ØªÙ†Ø¨Ø¤..." : "Predicting..."}
              isBusy={actionState.isBusy("technician-ai-predict")}
              onClick={() => {
                const nextErrors: FieldErrors = {
                  ...validateBatchId("predictBatchId", predictBatchId)
                };
                if (!predictFile) {
                  nextErrors.predictFile = language === "ar" ? "Ø§Ø®ØªØ± ØµÙˆØ±Ø© Ø£ÙˆÙ„Ø§Ù‹." : "Choose an image first.";
                }
                setPredictionErrors(nextErrors);
                if (Object.keys(nextErrors).length) {
                  return;
                }

                void actionState.runAction("technician-ai-predict", async () => {
                  try {
                    const body = new FormData();
                    body.append("file", predictFile as File);
                    body.append("batchId", predictBatchId.trim());

                    const response = await apiRequest<AiPredictionResponse>(endpoints.ai.predict, {
                      method: "POST",
                      body
                    });

                    setPredictedYieldPercent(response.predictedYieldPercent ?? null);
                    setPredictionConfidence(response.confidence ?? null);
                    setPredictionBatchLabel(response.batchId ?? predictBatchId.trim());
                    setPredictionModelVersion(response.modelVersion ?? "");
                    setPredictionErrors({});
                    setNotice({
                      tone: "success",
                      message: language === "ar" ? "ØªÙ… ØªÙ†ÙÙŠØ° Ø§Ù„ØªÙ†Ø¨Ø¤ Ø¨Ù†Ø¬Ø§Ø­." : "Prediction completed successfully."
                    });
                  } catch (requestError: unknown) {
                    setNotice({ tone: "danger", message: getApiErrorMessage(requestError) });
                  }
                });
              }}
              type="button"
            >
              {language === "ar" ? "ØªØ´ØºÙŠÙ„ Ø§Ù„ØªÙ†Ø¨Ø¤" : "Run prediction"}
            </ActionButton>
          </div>
        </Card>
        <Card
          title={language === "ar" ? "Ù†ØªÙŠØ¬Ø© Ø§Ù„ØªÙ†Ø¨Ø¤" : "Prediction Result"}
          subtitle={
            language === "ar"
              ? "Ù†ØªÙŠØ¬Ø© Ø§Ù„Ù…Ø±Ø¯ÙˆØ¯ Ø§Ù„Ù…ØªÙˆÙ‚Ø¹Ø© Ù…Ù† Ø§Ù„Ù†Ù…ÙˆØ°Ø¬ Ø§Ù„Ø­Ø§Ù„ÙŠ."
              : "Estimated yield from the current model."
          }
        >
          <div className="prediction-highlight">
            <div className="prediction-value">
              {predictedYieldPercent === null ? "--" : formatPercent(predictedYieldPercent, language)}
            </div>
            <div className="prediction-meta">
              <span>
                {language === "ar" ? "Ø±Ù‚Ù… Ø§Ù„Ø¯ÙØ¹Ø©" : "Batch ID"}: <strong>{predictionBatchLabel || "--"}</strong>
              </span>
              <span>
                {language === "ar" ? "Ø§Ù„Ø«Ù‚Ø©" : "Confidence"}:{" "}
                <strong>{predictionConfidence === null ? "--" : formatPercent(predictionConfidence * 100, language)}</strong>
              </span>
              <span>
                {language === "ar" ? "Ø¥ØµØ¯Ø§Ø± Ø§Ù„Ù†Ù…ÙˆØ°Ø¬" : "Model version"}: <strong>{predictionModelVersion || "--"}</strong>
              </span>
            </div>
          </div>
          {predictedYieldPercent === null ? (
            <p className="helper-text">
              {language === "ar" ? "Ù„Ø§ ØªÙˆØ¬Ø¯ Ù†ØªÙŠØ¬Ø© Ø¨Ø¹Ø¯. Ø§Ø±ÙØ¹ ØµÙˆØ±Ø© ÙˆØ´ØºÙ‘Ù„ Ø§Ù„ØªÙ†Ø¨Ø¤." : "No result yet. Upload an image and run prediction."}
            </p>
          ) : null}
        </Card>
      </div>
      ) : null}
      <div className={startPermission.allowed && (etaPermission.allowed || pipelinePermission.allowed) ? "content-grid two" : "content-grid"}>
        {startPermission.allowed ? (
        <Card title="Start Production">
          <div className="form-grid two">
            <div className="field">
              <label>{t("Order ID")}</label>
              <input onChange={(event) => setOrderId(event.target.value)} value={orderId} />
            </div>
            <div className="field">
              <label>{t("Order Item ID")}</label>
              <input onChange={(event) => setOrderItemId(event.target.value)} value={orderItemId} />
            </div>
          </div>
          <div className="inline-actions">
            <ActionButton
              disabled={!startPermission.allowed}
              disabledReason={startPermission.reason}
              busyLabel="Starting..."
              isBusy={actionState.isBusy("start-production")}
              onClick={() => {
                void actionState.runAction("start-production", async () => {
                  try {
                    await apiRequest(endpoints.production.start, {
                      method: "POST",
                      body: { orderId: Number(orderId), orderItemId: Number(orderItemId) }
                    });
                    await loadPage();
                    setNotice({
                      tone: "success",
                      message:
                        language === "ar"
                          ? `ØªÙ… Ø¨Ø¯Ø¡ Ø§Ù„Ø¥Ù†ØªØ§Ø¬ Ù„Ù„Ø¹Ù†ØµØ± Ø±Ù‚Ù… ${orderItemId}.`
                          : `Production started for item #${orderItemId}.`
                    });
                  } catch (requestError: unknown) {
                    setNotice({ tone: "danger", message: getApiErrorMessage(requestError) });
                  }
                });
              }}
              type="button"
              >
                {t("Start production")}
              </ActionButton>
          </div>
          <PermissionNote allowed={startPermission.allowed} reason={startPermission.reason} />
        </Card>
        ) : null}
        {etaPermission.allowed || pipelinePermission.allowed ? (
        <Card title="ETA Lookup">
          <div className="form-grid">
            <div className="field">
              <label>{t("Order Item ID")}</label>
              <input onChange={(event) => setStageItemId(event.target.value)} value={stageItemId} />
            </div>
            <div className="inline-actions">
              <ActionButton
                className="secondary"
                disabled={!etaPermission.allowed}
                disabledReason={etaPermission.reason}
                busyLabel="Loading..."
                isBusy={actionState.isBusy("load-eta")}
                onClick={() => {
                  if (!stageItemId.trim()) {
                    setNotice({ tone: "danger", message: t("Enter an order item ID before loading the ETA.") });
                    return;
                  }

                  void actionState.runAction("load-eta", async () => {
                    try {
                      const response = await apiRequest<ProductionEta>(endpoints.production.eta(stageItemId.trim()));
                      setEta(response);
                    } catch (requestError: unknown) {
                      setNotice({ tone: "danger", message: getApiErrorMessage(requestError) });
                    }
                  });
                }}
                type="button"
              >
                Load ETA
              </ActionButton>
              <ActionButton
                className="ghost"
                disabled={!pipelinePermission.allowed}
                disabledReason={pipelinePermission.reason}
                busyLabel="Loading..."
                isBusy={actionState.isBusy("load-order-stages")}
                onClick={() => {
                  if (!stageItemId.trim()) {
                    setNotice({ tone: "danger", message: t("Enter an order item ID before loading stage details.") });
                    return;
                  }

                  void actionState.runAction("load-order-stages", async () => {
                    try {
                      const response = await apiRequest<ProductionStage[]>(endpoints.production.orderStages(stageItemId.trim()));
                      setOrderStages(response);
                    } catch (requestError: unknown) {
                      setNotice({ tone: "danger", message: getApiErrorMessage(requestError) });
                    }
                  });
                }}
                type="button"
              >
                Load order stages
              </ActionButton>
            </div>
            <PermissionNote allowed={etaPermission.allowed} reason={etaPermission.reason} />
            <PermissionNote allowed={pipelinePermission.allowed} reason={pipelinePermission.reason} />
            {eta ? (
              <Banner
                tone="neutral"
                message={
                  language === "ar"
                    ? `Ø§Ù„Ø®Ø· ${eta.line}ØŒ Ø§Ù„Ù…Ø±Ø­Ù„Ø© ${eta.currentStage}ØŒ Ø§Ù„ÙˆÙ‚Øª Ø§Ù„Ù…ØªÙˆÙ‚Ø¹ ${eta.eta} Ø¯Ù‚ÙŠÙ‚Ø©.`
                    : `Line ${eta.line}, stage ${eta.currentStage}, ETA ${eta.eta} minutes.`
                }
              />
            ) : null}
          </div>
        </Card>
        ) : null}
      </div>
      {dashboardPermission.allowed && dashboard.length ? (
        <Card title="Production Dashboard">
          <div className="table-wrap">
            <table className="data-table">
              <thead>
                <tr>
                  <th>{t("Line")}</th>
                  <th>{t("Stage")}</th>
                  <th>{t("Status")}</th>
                  <th>{t("Queue")}</th>
                  <th>{t("Remaining")}</th>
                </tr>
              </thead>
              <tbody>
                {dashboard.map((item) => (
                  <tr key={`${item.line}-${item.stage}`}>
                    <td>{item.line}</td>
                    <td>{item.stage}</td>
                    <td><StatusBadge value={item.status} /></td>
                    <td>{item.queue}</td>
                    <td>
                      {item.remainingMinutes} {t("min")}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </Card>
      ) : null}
      {pipelinePermission.allowed && pipeline.length ? (
        <Card title="Pipeline">
          <div className="table-wrap">
            <table className="data-table">
              <thead>
                <tr>
                  <th>{t("Stage")}</th>
                  <th>{t("Order")}</th>
                  <th>{t("Item")}</th>
                  <th>{t("Line")}</th>
                  <th>{t("Status")}</th>
                  <th>{t("Actions")}</th>
                </tr>
              </thead>
              <tbody>
                {pipeline.map((stage) => (
                  <tr key={stage.id}>
                    <td>{stage.name}</td>
                    <td>{stage.orderId}</td>
                    <td>{stage.orderItemId}</td>
                    <td>{stage.line}</td>
                    <td><StatusBadge value={stage.currentStatus} /></td>
                    <td>
                      <div className="inline-actions">
                        <button
                          className="btn secondary"
                          disabled
                          title={t("Start production from the Start Production form above.")}
                          type="button"
                        >
                          {t("Start")}
                        </button>
                        <button
                          className="btn ghost"
                          disabled={!stageFinishPermission.allowed || !stage.orderItemId || !findNextPipelineStage(stage)}
                          onClick={() => finishPipelineStage(stage)}
                          type="button"
                        >
                          {t("Finish")}
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </Card>
      ) : null}
      {orderStages.length ? (
        <Card title="Order Stage Timeline">
          <div className="table-wrap">
            <table className="data-table">
              <thead>
                <tr>
                  <th>{t("Stage")}</th>
                  <th>{t("Line")}</th>
                  <th>{t("Order")}</th>
                  <th>{t("Status")}</th>
                </tr>
              </thead>
              <tbody>
                {orderStages.map((stage) => (
                  <tr key={stage.id}>
                    <td>{stage.name}</td>
                    <td>{stage.line}</td>
                    <td>{stage.orderId}</td>
                    <td><StatusBadge value={stage.currentStatus} /></td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </Card>
      ) : null}
    </div>
  );
}

export { ProductionPage } from "./production-page";
