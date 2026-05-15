import { useEffect, useMemo, useState } from "react";
import { useI18n } from "../app/i18n-context";
import { useToastHelpers } from "../app/toast-context";
import { ActionButton } from "../components/form-ui";
import { Card, EmptyState, PageHeader, StatusBadge } from "../components/ui";
import { endpoints } from "../lib/endpoints";
import { apiRequest } from "../lib/http";
import { useActionState } from "../lib/use-action-state";
import { asErrorMessage, formatCurrency, formatDateTime, formatNumber } from "../lib/utils";
import { getApiErrorMessage } from "../lib/validation";
import type { Customer, Order, Payment, Product, ProductionBatch } from "../types/models";

type ToastNotice = { tone: "success" | "danger" | "neutral"; message: string } | null;

function useNotice() {
  const { success, error, neutral } = useToastHelpers();

  return {
    notice: null as ToastNotice,
    setNotice: (notice: ToastNotice) => {
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

function NoticeBlock({ notice }: { notice: ToastNotice }): JSX.Element | null {
  void notice;
  return null;
}

function productInventory(product: Product): number {
  return product.inventory ?? product.inventoryAvailabilityQuantity ?? product.inventoryTotalQuantity ?? 0;
}

const PRICING_ROWS = [
  { key: "olive-member", nameAr: "زيتون للمساهم", nameEn: "Member olive", productName: "زيتون للمساهم", productType: "OLIVE", unit: "KG", defaultPrice: 0.4 },
  { key: "olive-standard", nameAr: "زيتون لغير المساهم", nameEn: "Non-member olive", productName: "زيتون لغير المساهم", productType: "OLIVE", unit: "KG", defaultPrice: 0.6 },
  { key: "jift", nameAr: "الجفت", nameEn: "Pomace", productName: "الجفت", productType: "JIFT", unit: "PCS", defaultPrice: 12 },
  { key: "gallon", nameAr: "الجالونات", nameEn: "Gallons", productName: "الجالونات", productType: "GALLON", unit: "PCS", defaultPrice: 15 }
] as const;

function findPricingProduct(products: Product[], row: (typeof PRICING_ROWS)[number]): Product | null {
  const byName = products.find((product) => product.productName?.trim() === row.productName);
  if (byName) {
    return byName;
  }

  if (row.productType === "JIFT" || row.productType === "GALLON") {
    return products.find((product) => product.productType?.toUpperCase() === row.productType) ?? null;
  }

  return null;
}

export function PricingPage(): JSX.Element {
  const { language } = useI18n();
  const { notice, setNotice } = useNotice();
  const actionState = useActionState();
  const [products, setProducts] = useState<Product[]>([]);
  const [drafts, setDrafts] = useState<Record<string, string>>({});
  const isArabic = language === "ar";

  const loadProducts = async (): Promise<void> => {
    const result = await apiRequest<Product[]>(endpoints.products.list);
    setProducts(result);
    setDrafts(Object.fromEntries(PRICING_ROWS.map((row) => {
      const product = findPricingProduct(result, row);
      return [row.key, String(product?.price ?? row.defaultPrice)];
    })));
  };

  useEffect(() => {
    void loadProducts().catch((requestError: unknown) => setNotice({ tone: "danger", message: asErrorMessage(requestError) }));
  }, []);

  const savePrice = (row: (typeof PRICING_ROWS)[number]): void => {
    const price = Number(drafts[row.key] ?? row.defaultPrice);
    if (!Number.isFinite(price) || price < 0) {
      setNotice({ tone: "danger", message: isArabic ? "أدخل سعراً صحيحاً." : "Enter a valid price." });
      return;
    }

    const existing = findPricingProduct(products, row);
    const body = {
      productName: row.productName,
      productType: row.productType,
      price,
      unit: row.unit,
      inventoryTotalQuantity: existing ? productInventory(existing) : 0
    };

    void actionState.runAction(`price-${row.key}`, async () => {
      try {
        if (existing) {
          await apiRequest<Product>(endpoints.products.byId(existing.id), { method: "PUT", body });
        } else {
          await apiRequest<Product>(endpoints.products.create, { method: "POST", body });
        }
        await loadProducts();
        setNotice({ tone: "success", message: isArabic ? "تم تثبيت السعر." : "Price saved." });
      } catch (requestError: unknown) {
        setNotice({ tone: "danger", message: getApiErrorMessage(requestError) });
      }
    });
  };

  return (
    <div className="content-grid">
      <PageHeader
        eyebrow={isArabic ? "التسعير" : "Pricing"}
        title={isArabic ? "التسعير" : "Pricing"}
        description={isArabic ? "تحديد أسعار المنتجات من الإدارة فقط، وتستخدم هذه الأسعار في الطلبات والفواتير." : "Set product prices from admin only. These prices are reused in orders and invoices."}
      />
      <NoticeBlock notice={notice} />
      <Card title={isArabic ? "أسعار المنتجات" : "Product Prices"}>
        <div className="table-wrap">
          <table className="data-table">
            <thead>
              <tr>
                <th>{isArabic ? "المنتج" : "Product"}</th>
                <th>{isArabic ? "النوع" : "Type"}</th>
                <th>{isArabic ? "الوحدة" : "Unit"}</th>
                <th>{isArabic ? "السعر الثابت" : "Fixed price"}</th>
                <th>{isArabic ? "الحالة" : "Status"}</th>
                <th>{isArabic ? "إجراء" : "Action"}</th>
              </tr>
            </thead>
            <tbody>
              {PRICING_ROWS.map((row) => {
                const product = findPricingProduct(products, row);
                return (
                  <tr key={row.key}>
                    <td>{isArabic ? row.nameAr : row.nameEn}</td>
                    <td>{row.productType}</td>
                    <td>{row.unit}</td>
                    <td>
                      <input
                        className="table-input"
                        onChange={(event) => setDrafts((current) => ({ ...current, [row.key]: event.target.value }))}
                        step="0.01"
                        type="number"
                        value={drafts[row.key] ?? row.defaultPrice}
                      />
                    </td>
                    <td><StatusBadge value={product ? (isArabic ? "موجود" : "ACTIVE") : (isArabic ? "غير منشأ" : "NEW")} /></td>
                    <td>
                      <ActionButton
                        className="secondary"
                        busyLabel={isArabic ? "جارٍ الحفظ..." : "Saving..."}
                        isBusy={actionState.isBusy(`price-${row.key}`)}
                        onClick={() => savePrice(row)}
                        type="button"
                      >
                        {isArabic ? "حفظ السعر" : "Save price"}
                      </ActionButton>
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      </Card>
    </div>
  );
}

export function AdminQueueDisplayPreviewPage(): JSX.Element {
  const { language } = useI18n();
  const isArabic = language === "ar";
  const displayUrl = `/queue-display?queue=PRODUCTION&lang=${language}`;

  return (
    <div className="content-grid">
      <PageHeader
        eyebrow={isArabic ? "العرض العام" : "Public Display"}
        title={isArabic ? "معاينة شاشة الأدوار" : "Queue Display Preview"}
        description={isArabic ? "شاشة العرض كما يراها الزبائن في منطقة الانتظار." : "The display screen exactly as customers see it in the waiting area."}
      />
      <Card
        title={isArabic ? "شاشة الأدوار" : "Queue Display"}
        actions={<a className="btn secondary" href={displayUrl} target="_blank" rel="noreferrer">{isArabic ? "فتح بنافذة جديدة" : "Open display"}</a>}
      >
        <iframe className="admin-preview-frame" src={displayUrl} title={isArabic ? "معاينة شاشة الأدوار" : "Queue display preview"} />
      </Card>
    </div>
  );
}

async function fetchOrdersForCustomers(customers: Customer[]): Promise<Order[]> {
  const orders = await Promise.all(customers.map((customer) => apiRequest<Order[]>(endpoints.orders.byCustomer(customer.id)).catch(() => [])));
  return orders.flat();
}

function customerName(customer?: Customer): string {
  return customer ? `${customer.firstName} ${customer.lastName}`.trim() : "--";
}

function paymentForOrder(payments: Payment[], orderId: number): Payment | null {
  return payments.find((payment) => Number(payment.orderId) === Number(orderId)) ?? null;
}

function orderOilItem(order: Order) {
  return order.items.find((item) => ["OLIVE", "SERVICE"].includes((item.productType ?? "").toUpperCase())) ?? order.items[0] ?? null;
}

function batchForOrder(batches: ProductionBatch[], order: Order): ProductionBatch | null {
  return batches.find((batch) => Number(batch.orderId) === Number(order.id)) ?? null;
}

function oilLiters(value?: number | null): string {
  if (!value) {
    return "--";
  }
  return formatNumber(value / 0.915);
}

export function AdminOrderManagementPage(): JSX.Element {
  const { language } = useI18n();
  const { notice, setNotice } = useNotice();
  const [customers, setCustomers] = useState<Customer[]>([]);
  const [orders, setOrders] = useState<Order[]>([]);
  const [payments, setPayments] = useState<Payment[]>([]);
  const [batches, setBatches] = useState<ProductionBatch[]>([]);
  const [query, setQuery] = useState("");
  const isArabic = language === "ar";

  const load = async (): Promise<void> => {
    const customerResult = await apiRequest<Customer[]>(endpoints.customers.list).catch(() => []);
    const [orderResult, paymentResult, batchResult] = await Promise.all([
      fetchOrdersForCustomers(customerResult),
      apiRequest<Payment[]>(endpoints.payments.list).catch(() => []),
      apiRequest<ProductionBatch[]>(endpoints.production.batches.list).catch(() => [])
    ]);
    setCustomers(customerResult);
    setOrders(orderResult);
    setPayments(paymentResult);
    setBatches(batchResult);
  };

  useEffect(() => {
    void load().catch((requestError: unknown) => setNotice({ tone: "danger", message: asErrorMessage(requestError) }));
    const timer = window.setInterval(() => {
      void load().catch(() => undefined);
    }, 15000);
    return () => window.clearInterval(timer);
  }, []);

  const visibleOrders = useMemo(() => {
    const normalized = query.trim().toLowerCase();
    const enriched = orders.map((order) => {
      const customer = customers.find((entry) => entry.id === order.customerId);
      return { order, customer };
    });

    if (!normalized) {
      return enriched.slice(0, 40);
    }

    return enriched.filter(({ order, customer }) =>
      [
        order.id,
        order.status,
        customer?.nationalId,
        customerName(customer)
      ].join(" ").toLowerCase().includes(normalized)
    ).slice(0, 40);
  }, [customers, orders, query]);

  return (
    <div className="content-grid">
      <PageHeader
        eyebrow={isArabic ? "إدارة الطلبات" : "Order Management"}
        title={isArabic ? "متابعة الطلبات والزبائن" : "Order and Customer Tracking"}
        description={isArabic ? "عرض حالة الدفع، الانتظار، العصر، ونسبة الزيت لكل طلب." : "View payment, waiting, pressing, and oil-yield status for every order."}
      />
      <NoticeBlock notice={notice} />
      <Card title={isArabic ? "فلترة الطلبات" : "Filter Orders"}>
        <div className="field">
          <label>{isArabic ? "بحث" : "Search"}</label>
          <input onChange={(event) => setQuery(event.target.value)} placeholder={isArabic ? "رقم الطلب أو اسم الزبون أو الهوية" : "Order ID, customer, or national ID"} value={query} />
        </div>
      </Card>
      <Card title={isArabic ? "حالة الطلبات" : "Order Status"}>
        {visibleOrders.length ? (
          <div className="table-wrap">
            <table className="data-table">
              <thead>
                <tr>
                  <th>{isArabic ? "الطلب" : "Order"}</th>
                  <th>{isArabic ? "الزبون" : "Customer"}</th>
                  <th>{isArabic ? "الدفع" : "Payment"}</th>
                  <th>{isArabic ? "حالة الطلب" : "Order Status"}</th>
                  <th>{isArabic ? "الإنتاج" : "Production"}</th>
                  <th>{isArabic ? "نسبة الزيت" : "Yield"}</th>
                  <th>{isArabic ? "اللترات" : "Liters"}</th>
                  <th>{isArabic ? "آخر تحديث" : "Updated"}</th>
                </tr>
              </thead>
              <tbody>
                {visibleOrders.map(({ order, customer }) => {
                  const payment = paymentForOrder(payments, order.id);
                  const batch = batchForOrder(batches, order);
                  const oilItem = orderOilItem(order);
                  return (
                    <tr key={order.id}>
                      <td>#{order.id}</td>
                      <td>{customerName(customer)}</td>
                      <td><StatusBadge value={payment ? (isArabic ? "مدفوع" : "PAID") : (isArabic ? "غير مدفوع" : "UNPAID")} /></td>
                      <td><StatusBadge value={order.status} /></td>
                      <td><StatusBadge value={oilItem?.status ?? batch?.status ?? "--"} /></td>
                      <td>{batch?.predictedYieldPercent ? `${formatNumber(batch.predictedYieldPercent)}%` : "--"}</td>
                      <td>{oilLiters(batch?.predictedOilKg ?? null)}</td>
                      <td>{formatDateTime(order.updatedAt ?? order.createdAt)}</td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        ) : (
          <EmptyState title={isArabic ? "لا توجد طلبات" : "No orders"} description={isArabic ? "ستظهر الطلبات بعد التسجيل." : "Orders appear after registration."} />
        )}
      </Card>
    </div>
  );
}
