import { useEffect, useMemo, useState } from "react";
import { useAuth } from "../app/auth-context";
import { useI18n } from "../app/i18n-context";
import { useToastHelpers } from "../app/toast-context";
import { canAction } from "../app/policy";
import { ActionButton, ErrorSummary, FieldError, PermissionNote } from "../components/form-ui";
import { Banner, Card, EmptyState, PageHeader, StatTile } from "../components/ui";
import { endpoints } from "../lib/endpoints";
import { apiRequest } from "../lib/http";
import { fetchProductionDashboard } from "../lib/production-api";
import { useActionState } from "../lib/use-action-state";
import { asErrorMessage, formatCurrency, isOliveProductType } from "../lib/utils";
import {
  getApiFieldErrors,
  getApiErrorMessage,
  type FieldErrors,
  validateCity,
  validateCustomer,
  validatePasswordChange,
  validateProduct,
  validateProfile
} from "../lib/validation";
import type {
  ChangePasswordRequest,
  City,
  CityInput,
  Customer,
  CustomerInput,
  Payment,
  Product,
  ProductInput,
  Order,
  ProductionBatch,
  ProductionDashboardItem,
  Profile,
  QueueStatus,
  UpdateProfileRequest
} from "../types/models";

type ToastNotice = { tone: "success" | "danger" | "neutral"; message: string };
type ToastNoticeValue = ToastNotice | null;
type PasswordChangeForm = ChangePasswordRequest & { confirmNewPassword: string };

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

function formatMetric(value: number, language: "ar" | "en", digits = 0): string {
  return new Intl.NumberFormat(language === "ar" ? "ar" : "en", {
    minimumFractionDigits: digits,
    maximumFractionDigits: digits
  }).format(value);
}

function ProductionBarChart({
  title,
  subtitle,
  items,
  valueKey,
  formatter,
  accentClass
}: {
  title: string;
  subtitle: string;
  items: ProductionDashboardItem[];
  valueKey: "throughputPerHour" | "queue" | "remainingMinutes";
  formatter: (item: ProductionDashboardItem) => string;
  accentClass: string;
}): JSX.Element {
  const maxValue = Math.max(...items.map((item) => item[valueKey]), 1);

  return (
    <Card title={title} subtitle={subtitle}>
      <div className="production-bars">
        {items.map((item) => {
          const width = `${Math.max((item[valueKey] / maxValue) * 100, 8)}%`;

          return (
            <div className="production-bar-row" key={`${title}-${item.line}`}>
              <div className="production-bar-meta">
                <strong>{item.line}</strong>
                <span>{item.stage}</span>
              </div>
              <div className="production-bar-track">
                <div className={`production-bar-fill ${accentClass}`} style={{ width }} />
              </div>
              <div className="production-bar-value">{formatter(item)}</div>
            </div>
          );
        })}
      </div>
    </Card>
  );
}

export function OverviewPage(): JSX.Element {
  const auth = useAuth();
  const { language, t } = useI18n();
  const [dashboard, setDashboard] = useState<ProductionDashboardItem[]>([]);
  const [payments, setPayments] = useState<Payment[]>([]);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    async function load(): Promise<void> {
      try {
        if (canAction(auth, "PAYMENT_READ").allowed) {
          setPayments(await apiRequest<Payment[]>(endpoints.payments.list));
        }
        if (canAction(auth, "PRODUCTION_DASHBOARD").allowed) {
          setDashboard(await fetchProductionDashboard());
        }
      } catch (requestError: unknown) {
        setError(asErrorMessage(requestError));
      }
    }

    void load();
  }, [auth.role]);

  const totalQueue = dashboard.reduce((sum, item) => sum + item.queue, 0);
  const totalThroughput = dashboard.reduce((sum, item) => sum + item.throughputPerHour, 0);
  const averageStageTime = dashboard.length ? dashboard.reduce((sum, item) => sum + item.avgStageTime, 0) / dashboard.length : 0;
  const averageEta = dashboard.length ? dashboard.reduce((sum, item) => sum + item.eta, 0) / dashboard.length : 0;
  const activeRuns = dashboard.filter((item) => item.status.toUpperCase().includes("PROGRESS") || item.status.toUpperCase().includes("ACTIVE")).length;
  const fastestLine = dashboard.length
    ? dashboard.reduce((best, item) => (item.remainingMinutes < best.remainingMinutes ? item : best), dashboard[0])
    : null;
  const isAccountant = auth.role === "ACCOUNTANT";
  const paymentSeries = useMemo(() => {
    const formatter = new Intl.DateTimeFormat(language === "ar" ? "ar" : "en", { weekday: "short" });
    const days = Array.from({ length: 7 }, (_, index) => {
      const date = new Date();
      date.setDate(date.getDate() - (6 - index));
      const key = date.toDateString();
      const total = payments
        .filter((payment) => payment.paymentDate && new Date(payment.paymentDate).toDateString() === key)
        .reduce((sum, payment) => sum + Number(payment.totalPrice ?? 0), 0);

      return { key, label: formatter.format(date), total };
    });

    return days;
  }, [language, payments]);
  const paymentMethodTotals = useMemo(() => {
    const totals = payments.reduce<Record<string, number>>((accumulator, payment) => {
      const method = payment.paymentType || "Cash";
      accumulator[method] = (accumulator[method] ?? 0) + Number(payment.totalPrice ?? 0);
      return accumulator;
    }, {});

    return Object.entries(totals).map(([method, total]) => ({ method, total }));
  }, [payments]);
  const maxPaymentSeries = Math.max(...paymentSeries.map((item) => item.total), 1);
  const totalPaymentsValue = payments.reduce((sum, payment) => sum + Number(payment.totalPrice ?? 0), 0);

  if (isAccountant) {
    return (
      <div className="content-grid">
        {error ? <Banner tone="danger" message={error} /> : null}
        <PageHeader
          eyebrow={language === "ar" ? "المحاسبة" : "Accounting"}
          title={language === "ar" ? "نظرة عامة على الإيرادات" : "Revenue Overview"}
          description={language === "ar" ? "مؤشر سريع لإيرادات المدفوعات خلال الأسبوع الحالي." : "A focused view of weekly payment revenue."}
        />
        <div className="accountant-weekly-revenue-card">
          <div className="accountant-chart-head">
            <div>
              <h2>{language === "ar" ? "الإيراد الأسبوعي" : "Weekly Revenue"} ({language === "ar" ? "₪" : "SAR"})</h2>
              <p>{language === "ar" ? "حرّك المؤشر فوق أي يوم لمعرفة الإيراد ونسبته من الأسبوع." : "Hover over any day to see revenue and its share of the week."}</p>
            </div>
            <strong>{formatCurrency(totalPaymentsValue)}</strong>
          </div>
          {payments.length ? (
            <div className="accountant-weekly-chart" role="img" aria-label={language === "ar" ? "الإيراد الأسبوعي" : "Weekly Revenue"}>
              {paymentSeries.map((item) => {
                const share = totalPaymentsValue > 0 ? (item.total / totalPaymentsValue) * 100 : 0;
                return (
                  <div className="accountant-weekly-item" key={item.key}>
                    <div className="accountant-weekly-bar-wrap">
                      <div className="accountant-weekly-hover">
                        <strong>{item.label}</strong>
                        <span>{language === "ar" ? "الإيراد" : "Revenue"}: {formatCurrency(item.total)}</span>
                        <span>{language === "ar" ? "النسبة" : "Share"}: {formatMetric(share, language, 1)}%</span>
                      </div>
                      <span style={{ height: `${Math.max((item.total / maxPaymentSeries) * 100, 8)}%` }} />
                    </div>
                    <small>{item.label}</small>
                  </div>
                );
              })}
            </div>
          ) : (
            <EmptyState
              title={language === "ar" ? "لا توجد مدفوعات بعد" : "No payments yet"}
              description={language === "ar" ? "سيظهر الشارت بعد تسجيل أول دفعة." : "The chart appears after the first payment is recorded."}
            />
          )}
        </div>
      </div>
    );
  }

  return (
    <div className="content-grid">
      {error ? <Banner tone="danger" message={error} /> : null}
      <div className="content-grid two">
        <StatTile label="Signed-in Role" value={t(auth.role ?? "Unknown")} />
        <StatTile label="Employee" value={auth.profile ? `${auth.profile.firstName} ${auth.profile.lastName}` : "--"} />
      </div>
      <div className="content-grid two">
        <Card title="Payment Signal" subtitle="Latest finance activity visible to this role.">
          {payments.length ? (
            <div className="content-grid">
              <StatTile label="Payments Loaded" value={payments.length} />
              <StatTile
                label="Latest Payment"
                value={payments[0] ? formatCurrency(payments[0].totalPrice) : "--"}
                hint={payments[0] ? `Order #${payments[0].orderId}` : "No payments yet"}
              />
            </div>
          ) : (
            <EmptyState
              title="No finance snapshot"
              description="This role may not have payment visibility yet, or there are no payments in the system."
            />
          )}
        </Card>
        <Card title="Production Signal" subtitle="Operational dashboard data routed from the production service.">
          {dashboard.length ? (
            <div className="content-grid">
              <StatTile label="Lines Tracked" value={dashboard.length} />
              <StatTile
                label="Average Queue"
                value={dashboard.length ? Math.round(dashboard.reduce((sum, item) => sum + item.queue, 0) / dashboard.length) : 0}
              />
              <StatTile
                label="Fastest Remaining Minutes"
                value={dashboard.length ? Math.min(...dashboard.map((item) => item.remainingMinutes)) : "--"}
              />
            </div>
          ) : (
            <EmptyState
              title="No production dashboard"
              description="Technicians can still use the production page even when dashboard access is limited."
            />
          )}
        </Card>
      </div>
      {isAccountant ? (
        <>
          <div className="accountant-overview-metrics">
            <StatTile
              label={language === "ar" ? "إجمالي المدفوعات" : "Total Payments"}
              value={formatCurrency(totalPaymentsValue)}
              hint={language === "ar" ? "كل الدفعات المحملة" : "All loaded payments"}
            />
            <StatTile
              label={language === "ar" ? "عدد الدفعات" : "Payment Count"}
              value={payments.length}
              hint={language === "ar" ? "سجلات مالية" : "Finance records"}
            />
            <StatTile
              label={language === "ar" ? "آخر دفعة" : "Latest Payment"}
              value={payments[0] ? formatCurrency(payments[0].totalPrice) : "--"}
              hint={payments[0] ? `#${payments[0].orderId}` : language === "ar" ? "لا يوجد" : "None"}
            />
          </div>
          <section className="admin-chart-grid">
            <div className="admin-chart-card">
              <h2>{language === "ar" ? "المدفوعات خلال آخر 7 أيام" : "Payments Over Last 7 Days"}</h2>
              <div className="admin-bar-chart">
                {paymentSeries.map((item) => (
                  <div className="admin-bar-item" key={item.key} title={formatCurrency(item.total)}>
                    <div className="admin-bar-track"><span style={{ height: `${Math.max((item.total / maxPaymentSeries) * 100, 8)}%` }} /></div>
                    <small>{item.label}</small>
                  </div>
                ))}
              </div>
            </div>
            <div className="admin-chart-card">
              <h2>{language === "ar" ? "توزيع طرق الدفع" : "Payment Method Mix"}</h2>
              {paymentMethodTotals.length ? (
                <div className="accountant-payment-trend">
                  {paymentMethodTotals.map((item) => (
                    <div className="accountant-payment-row" key={item.method}>
                      <strong>{item.method}</strong>
                      <div className="accountant-payment-track">
                        <div
                          className="accountant-payment-fill"
                          style={{ width: `${Math.max((item.total / Math.max(totalPaymentsValue, 1)) * 100, 8)}%` }}
                        />
                      </div>
                      <span>{formatCurrency(item.total)}</span>
                    </div>
                  ))}
                </div>
              ) : (
                <EmptyState title={language === "ar" ? "لا توجد مدفوعات" : "No payments"} description={language === "ar" ? "ستظهر الرسوم بعد تسجيل الدفعات." : "Charts appear after payments are recorded."} />
              )}
            </div>
          </section>
        </>
      ) : null}
      {canAction(auth, "PRODUCTION_DASHBOARD").allowed ? (
        dashboard.length ? (
          <>
            <Card
              title={language === "ar" ? "ملخص الإنتاج اليومي" : "Daily Production Overview"}
              subtitle={
                language === "ar"
                  ? "لوحة متابعة يومية مبنية على بيانات خطوط الإنتاج الحالية."
                  : "A live operational snapshot built from the current production lines."
              }
            >
              <div className="content-grid four">
                <StatTile
                  label={language === "ar" ? "الإنتاج بالساعة" : "Hourly Throughput"}
                  value={formatMetric(totalThroughput, language, 1)}
                  hint={language === "ar" ? "إجمالي جميع الخطوط" : "Combined across all lines"}
                />
                <StatTile
                  label={language === "ar" ? "إجمالي الطابور" : "Queue Backlog"}
                  value={formatMetric(totalQueue, language)}
                  hint={language === "ar" ? "طلبات قيد الانتظار" : "Orders waiting in line"}
                />
                <StatTile
                  label={language === "ar" ? "متوسط وقت المرحلة" : "Average Stage Time"}
                  value={`${formatMetric(averageStageTime, language, 1)} ${language === "ar" ? "د" : "min"}`}
                  hint={language === "ar" ? "متوسط المراحل الجارية" : "Across the tracked stages"}
                />
                <StatTile
                  label={language === "ar" ? "متوسط الوصول" : "Average ETA"}
                  value={`${formatMetric(averageEta, language, 1)} ${language === "ar" ? "د" : "min"}`}
                  hint={
                    fastestLine
                      ? language === "ar"
                        ? `أسرع خط ${fastestLine.line}`
                        : `Fastest line ${fastestLine.line}`
                      : undefined
                  }
                />
              </div>
            </Card>
            <div className="content-grid two">
              <ProductionBarChart
                accentClass="production-bar-fill-throughput"
                items={dashboard}
                subtitle={language === "ar" ? "مقارنة مباشرة بين الخطوط." : "Live comparison across production lines."}
                title={language === "ar" ? "الإنتاج لكل خط" : "Throughput by Line"}
                valueKey="throughputPerHour"
                formatter={(item) =>
                  language === "ar"
                    ? `${formatMetric(item.throughputPerHour, language, 1)} / ساعة`
                    : `${formatMetric(item.throughputPerHour, language, 1)} / hr`
                }
              />
              <ProductionBarChart
                accentClass="production-bar-fill-queue"
                items={dashboard}
                subtitle={language === "ar" ? "عدد الطلبات المنتظرة في كل خط." : "Waiting load on each line."}
                title={language === "ar" ? "الطابور لكل خط" : "Queue by Line"}
                valueKey="queue"
                formatter={(item) => formatMetric(item.queue, language)}
              />
            </div>
            <div className="content-grid two">
              <ProductionBarChart
                accentClass="production-bar-fill-time"
                items={dashboard}
                subtitle={language === "ar" ? "الوقت المتبقي حتى الخروج من المرحلة الحالية." : "Time remaining on the current stage."}
                title={language === "ar" ? "الوقت المتبقي لكل خط" : "Remaining Time by Line"}
                valueKey="remainingMinutes"
                formatter={(item) =>
                  language === "ar"
                    ? `${formatMetric(item.remainingMinutes, language)} د`
                    : `${formatMetric(item.remainingMinutes, language)} min`
                }
              />
              <Card
                title={language === "ar" ? "تدفق الخطوط" : "Line Flow"}
                subtitle={language === "ar" ? "حالة كل خط في الوقت الحالي." : "Current state of each production line."}
              >
                <div className="line-flow-grid">
                  {dashboard.map((item) => (
                    <div className="line-flow-card" key={`${item.line}-${item.stage}`}>
                      <div className="line-flow-header">
                        <strong>{item.line}</strong>
                        <span>{item.stage}</span>
                      </div>
                      <div className="line-flow-status-row">
                        <span className="line-flow-label">{language === "ar" ? "الحالة" : "Status"}</span>
                        <span className="line-flow-status">{item.status}</span>
                      </div>
                      <div className="line-flow-status-row">
                        <span className="line-flow-label">{language === "ar" ? "الوصول" : "ETA"}</span>
                        <span className="line-flow-status">
                          {language === "ar"
                            ? `${formatMetric(item.eta, language)} د`
                            : `${formatMetric(item.eta, language)} min`}
                        </span>
                      </div>
                    </div>
                  ))}
                  <div className="line-flow-card line-flow-card-highlight">
                    <div className="line-flow-header">
                      <strong>{language === "ar" ? "الخطوط النشطة" : "Active Runs"}</strong>
                      <span>{language === "ar" ? "اليوم" : "Today"}</span>
                    </div>
                    <div className="line-flow-big-number">{formatMetric(activeRuns, language)}</div>
                    <p>
                      {fastestLine
                        ? language === "ar"
                          ? `أفضل زمن حالي على ${fastestLine.line}.`
                          : `Fastest live turnaround is on ${fastestLine.line}.`
                        : language === "ar"
                          ? "لا توجد بيانات متاحة حالياً."
                          : "No live production data yet."}
                    </p>
                  </div>
                </div>
              </Card>
            </div>
          </>
        ) : (
          <Card
            title={language === "ar" ? "إحصاءات الإنتاج" : "Production Analytics"}
            subtitle={language === "ar" ? "ستظهر الرسوم هنا عند توفر بيانات من الخدمة." : "Charts will appear here once the production feed has data."}
          >
            <EmptyState
              title={language === "ar" ? "لا توجد بيانات إنتاج" : "No production data"}
              description={
                language === "ar"
                  ? "ابدأ أو حمّل خطوط الإنتاج حتى تظهر اللوحات اليومية."
                  : "Start or load production lines to populate the daily charts."
              }
            />
          </Card>
        )
      ) : null}
    </div>
  );
}

function LegacyAdminDashboardPage(): JSX.Element {
  const auth = useAuth();
  const { language, t } = useI18n();
  const [customers, setCustomers] = useState<Customer[]>([]);
  const [payments, setPayments] = useState<Payment[]>([]);
  const [dashboard, setDashboard] = useState<ProductionDashboardItem[]>([]);
  const [productionQueue, setProductionQueue] = useState<QueueStatus | null>(null);
  const [accountingQueue, setAccountingQueue] = useState<QueueStatus | null>(null);
  const [error, setError] = useState<string | null>(null);
  const isArabic = language === "ar";
  const copy = isArabic
    ? {
        title: "نظرة عامة على اللوحة",
        subtitle: "راقب عمليات معصرة الزيتون والأداء اليومي",
        todaysCustomers: "عملاء اليوم",
        liveCustomers: "سجلات العملاء الحالية",
        ordersCreatedToday: "الطلبات المنشأة اليوم",
        ordersUnavailable: "غير متاح من خدمة الطلبات الحالية",
        activeProductionBatches: "دفعات الإنتاج النشطة",
        completedOrdersHint: "طلبات مكتملة",
        pendingPayments: "مدفوعات معلقة",
        totalPaid: "إجمالي المدفوع",
        productionQueue: "طابور الإنتاج",
        accountingQueue: "طابور المحاسبة",
        avgWait: "متوسط الانتظار",
        oilOutputToday: "إنتاج الزيت اليوم",
        estimatedFromProduction: "تقدير من بيانات الإنتاج",
        completedOrders: "طلبات مكتملة",
        readyForPickup: "جاهزة للاستلام",
        ordersPerDay: "الطلبات حسب اليوم",
        weeklyOil: "إنتاج الزيت الأسبوعي (لتر)",
        alerts: "التنبيهات والإشعارات",
        tankAlert: "الخزان B ممتلئ بنسبة 85% - يفضل تفريغه قريبا",
        pickupDelay: "الطلب #127 متأخر - بانتظار استلام العميل",
        recentActivity: "النشاط الأخير",
        noPaymentActivity: "لا توجد حركة مدفوعات بعد",
        latestPayment: (orderId: number | string) => `تم تسجيل آخر دفعة للطلب #${orderId}`,
        paymentReceived: (orderId: number | string) => `تم استلام دفعة للطلب #${orderId}`,
        productionTicketsWaiting: (count: number) => `${formatMetric(count, language)} تذاكر بانتظار الإنتاج`,
        productionQueueUnavailable: "طابور الإنتاج غير متاح",
        min: "دقيقة",
        days: ["الإثنين", "الثلاثاء", "الأربعاء", "الخميس", "الجمعة", "السبت", "الأحد"],
        weeks: ["الأسبوع 1", "الأسبوع 2", "الأسبوع 3", "الأسبوع 4"],
        agoNow: "الآن",
        ago10: "قبل 10 دقائق",
        ago25: "قبل 25 دقيقة",
        ago15: "قبل 15 دقيقة",
        ago1h: "قبل ساعة",
        icons: ["ع", "ط", "إ", "د", "طإ", "طم", "ز", "م"]
      }
    : {
        title: "Dashboard Overview",
        subtitle: "Monitor your olive press operations and daily performance",
        todaysCustomers: "Today's Customers",
        liveCustomers: "Live customer records",
        ordersCreatedToday: "Orders Created Today",
        ordersUnavailable: "Not exposed by current order service",
        activeProductionBatches: "Active Production Batches",
        completedOrdersHint: "completed orders",
        pendingPayments: "Pending Payments",
        totalPaid: "total paid",
        productionQueue: "Production Queue",
        accountingQueue: "Accounting Queue",
        avgWait: "Avg wait",
        oilOutputToday: "Oil Output Today",
        estimatedFromProduction: "Estimated from production feed",
        completedOrders: "Completed Orders",
        readyForPickup: "Ready for pickup",
        ordersPerDay: "Orders Per Day",
        weeklyOil: "Weekly Oil Production (Liters)",
        alerts: "Alerts & Notifications",
        tankAlert: "Tank B is 85% full - consider emptying soon",
        pickupDelay: "Order #127 delayed - awaiting customer pickup",
        recentActivity: "Recent Activity",
        noPaymentActivity: "No payment activity yet",
        latestPayment: (orderId: number | string) => `Latest payment recorded for Order #${orderId}`,
        paymentReceived: (orderId: number | string) => `Payment received for Order #${orderId}`,
        productionTicketsWaiting: (count: number) => `${formatMetric(count, language)} production tickets waiting`,
        productionQueueUnavailable: "Production queue unavailable",
        min: "min",
        days: ["Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"],
        weeks: ["Week 1", "Week 2", "Week 3", "Week 4"],
        agoNow: "Now",
        ago10: "10 min ago",
        ago25: "25 min ago",
        ago15: "15 min ago",
        ago1h: "1 hour ago",
        icons: ["CU", "OR", "PR", "PA", "PQ", "AQ", "OI", "CO"]
      };

  useEffect(() => {
    async function load(): Promise<void> {
      const results = await Promise.allSettled([
        canAction(auth, "CUSTOMER_READ").allowed ? apiRequest<Customer[]>(endpoints.customers.list) : Promise.resolve([]),
        canAction(auth, "PAYMENT_READ").allowed ? apiRequest<Payment[]>(endpoints.payments.list) : Promise.resolve([]),
        canAction(auth, "PRODUCTION_DASHBOARD").allowed ? fetchProductionDashboard() : Promise.resolve([]),
        apiRequest<QueueStatus>(endpoints.queues.status("PRODUCTION")),
        apiRequest<QueueStatus>(endpoints.queues.status("ACCOUNTING"))
      ]);

      setCustomers(results[0].status === "fulfilled" ? results[0].value : []);
      setPayments(results[1].status === "fulfilled" ? results[1].value : []);
      setDashboard(results[2].status === "fulfilled" ? results[2].value : []);
      setProductionQueue(results[3].status === "fulfilled" ? results[3].value : null);
      setAccountingQueue(results[4].status === "fulfilled" ? results[4].value : null);

      const rejected = results.find((result) => result.status === "rejected");
      setError(rejected && rejected.status === "rejected" ? asErrorMessage(rejected.reason) : null);
    }

    void load().catch((requestError: unknown) => setError(asErrorMessage(requestError)));
  }, [auth.role]);

  const today = new Date().toDateString();
  const todaysPayments = payments.filter((payment) => payment.paymentDate && new Date(payment.paymentDate).toDateString() === today);
  const pendingPayments = 0;
  const completedOrders = 0;
  const activeBatches = dashboard.filter((item) => item.status.toUpperCase().includes("PROGRESS") || item.status.toUpperCase().includes("ACTIVE")).length;
  const estimatedOilLiters = dashboard.reduce((sum, item) => sum + item.throughputPerHour, 0);
  const paymentTotal = payments.reduce((sum, payment) => sum + Number(payment.totalPrice ?? 0), 0);
  const productionWaiting = productionQueue?.stats.totalWaiting ?? 0;
  const accountingWaiting = accountingQueue?.stats.totalWaiting ?? 0;
  const ordersPerDay = [44, 52, 38, 61, 55, 28, 22];
  const weeklyOil = [1420, 1680, 1550, Math.max(estimatedOilLiters, 1920)];
  const maxOrders = Math.max(...ordersPerDay, 1);
  const maxOil = Math.max(...weeklyOil, 1);
  const recentActivity = [
    todaysPayments[0]
      ? copy.paymentReceived(todaysPayments[0].orderId)
      : payments[0]
        ? copy.latestPayment(payments[0].orderId)
        : copy.noPaymentActivity,
    productionQueue ? copy.productionTicketsWaiting(productionWaiting) : copy.productionQueueUnavailable
  ];

  return (
    <div className="admin-dashboard-page">
      {error ? <Banner tone="danger" message={error} /> : null}
      <header className="admin-dashboard-header">
        <div>
          <h1>{copy.title}</h1>
        </div>
        <div className="admin-dashboard-date">
          <strong>{new Intl.DateTimeFormat(language === "ar" ? "ar" : "en", { weekday: "short", month: "short", day: "numeric" }).format(new Date())}</strong>
          <span>{t(auth.role ?? "Admin")}</span>
        </div>
      </header>

      <section className="admin-kpi-grid">
        <div className="admin-kpi-card"><div><span>{copy.todaysCustomers}</span><strong>{formatMetric(customers.length, language)}</strong><small className="positive">{copy.liveCustomers}</small></div><div className="admin-kpi-icon">{copy.icons[0]}</div></div>
        <div className="admin-kpi-card"><div><span>{copy.ordersCreatedToday}</span><strong>{formatMetric(0, language)}</strong><small>{copy.ordersUnavailable}</small></div><div className="admin-kpi-icon">{copy.icons[1]}</div></div>
        <div className="admin-kpi-card"><div><span>{copy.activeProductionBatches}</span><strong>{formatMetric(activeBatches, language)}</strong><small>{formatMetric(completedOrders, language)} {copy.completedOrdersHint}</small></div><div className="admin-kpi-icon">{copy.icons[2]}</div></div>
        <div className="admin-kpi-card"><div><span>{copy.pendingPayments}</span><strong>{formatMetric(pendingPayments, language)}</strong><small>{formatCurrency(paymentTotal)} {copy.totalPaid}</small></div><div className="admin-kpi-icon">{copy.icons[3]}</div></div>
        <div className="admin-kpi-card"><div><span>{copy.productionQueue}</span><strong>{formatMetric(productionWaiting, language)}</strong><small>{copy.avgWait}: {formatMetric(productionQueue?.stats.averageWaitTime ?? 0, language)} {copy.min}</small></div><div className="admin-kpi-icon">{copy.icons[4]}</div></div>
        <div className="admin-kpi-card"><div><span>{copy.accountingQueue}</span><strong>{formatMetric(accountingWaiting, language)}</strong><small>{copy.avgWait}: {formatMetric(accountingQueue?.stats.averageWaitTime ?? 0, language)} {copy.min}</small></div><div className="admin-kpi-icon">{copy.icons[5]}</div></div>
        <div className="admin-kpi-card"><div><span>{copy.oilOutputToday}</span><strong>{formatMetric(estimatedOilLiters || 0, language, 1)} L</strong><small>{copy.estimatedFromProduction}</small></div><div className="admin-kpi-icon success">{copy.icons[6]}</div></div>
        <div className="admin-kpi-card"><div><span>{copy.completedOrders}</span><strong>{formatMetric(completedOrders, language)}</strong><small>{copy.readyForPickup}</small></div><div className="admin-kpi-icon success">{copy.icons[7]}</div></div>
      </section>

      <section className="admin-chart-grid">
        <div className="admin-chart-card">
          <h2>{copy.ordersPerDay}</h2>
          <div className="admin-bar-chart">
            {ordersPerDay.map((value, index) => (
              <div className="admin-bar-item" key={index}>
                <div className="admin-bar-track"><span style={{ height: `${Math.max((value / maxOrders) * 100, 8)}%` }} /></div>
                <small>{copy.days[index]}</small>
              </div>
            ))}
          </div>
        </div>
        <div className="admin-chart-card">
          <h2>{copy.weeklyOil}</h2>
          <div className="admin-line-chart">
            <svg viewBox="0 0 520 210" role="img" aria-label={copy.weeklyOil}>
              <polyline fill="none" points={weeklyOil.map((value, index) => `${32 + index * 150},${190 - (value / maxOil) * 150}`).join(" ")} stroke="#6f813f" strokeWidth="3" />
              {weeklyOil.map((value, index) => <circle cx={32 + index * 150} cy={190 - (value / maxOil) * 150} fill="#6f813f" key={index} r="5" />)}
            </svg>
            <div className="admin-line-labels">{copy.weeks.map((week) => <span key={week}>{week}</span>)}</div>
          </div>
        </div>
      </section>

      <section className="admin-bottom-grid">
        <div className="admin-feed-card">
          <h2>{copy.alerts}</h2>
          <div className="admin-alert-list">
            <div><span className="admin-dot warning" /><strong>{copy.tankAlert}</strong><small>{copy.ago15}</small></div>
            <div><span className="admin-dot info" /><strong>{copy.pickupDelay}</strong><small>{copy.ago1h}</small></div>
          </div>
        </div>
        <div className="admin-feed-card">
          <h2>{copy.recentActivity}</h2>
          <div className="admin-activity-list">
            {recentActivity.map((activity, index) => (
              <div key={activity}><span className="admin-activity-icon">{formatMetric(index + 1, language)}</span><strong>{activity}</strong><small>{index === 0 ? copy.ago10 : index === 1 ? copy.ago25 : copy.agoNow}</small></div>
            ))}
          </div>
        </div>
      </section>
    </div>
  );
}

type AdminIconName = "cart" | "factory" | "list" | "drop" | "check";

function AdminKpiIcon({ name }: { name: AdminIconName }): JSX.Element {
  const paths: Record<AdminIconName, JSX.Element> = {
    cart: (
      <>
        <circle cx="9" cy="20" r="1.5" />
        <circle cx="18" cy="20" r="1.5" />
        <path d="M3 4h2l2.2 10.5a2 2 0 0 0 2 1.5h8.7a2 2 0 0 0 1.9-1.4L22 8H7" />
      </>
    ),
    factory: (
      <>
        <path d="M3 21V9l6 4V9l6 4V5h4v16" />
        <path d="M3 21h18" />
        <path d="M7 17h2M12 17h2M17 17h2" />
      </>
    ),
    list: (
      <>
        <path d="M8 6h13M8 12h13M8 18h13" />
        <path d="M3.5 6h.01M3.5 12h.01M3.5 18h.01" />
      </>
    ),
    drop: (
      <>
        <path d="M12 3s6 6.4 6 11a6 6 0 0 1-12 0c0-4.6 6-11 6-11z" />
        <path d="M10 17c1.8 1 4 .2 5-1.6" />
      </>
    ),
    check: (
      <>
        <circle cx="12" cy="12" r="9" />
        <path d="m8 12 2.6 2.6L16.5 9" />
      </>
    )
  };

  return (
    <div className={`admin-kpi-icon ${name === "drop" || name === "check" ? "success" : ""}`} aria-hidden="true">
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
        {paths[name]}
      </svg>
    </div>
  );
}

function isSameDay(first?: string | null, second = new Date()): boolean {
  if (!first) {
    return false;
  }

  const date = new Date(first);
  return !Number.isNaN(date.getTime()) && date.toDateString() === second.toDateString();
}

function orderDate(order: Order): string | null {
  return order.createdAt ?? order.updatedAt ?? null;
}

function isCompletedStatus(value?: string | null): boolean {
  return ["COMPLETED", "DONE", "READY_FOR_PICKUP"].some((status) => (value ?? "").toUpperCase().includes(status));
}

function oilLitersFromKg(value?: number | null): number {
  return value ? value / 0.915 : 0;
}

async function fetchOrdersForCustomers(customers: Customer[]): Promise<Order[]> {
  const orders = await Promise.all(
    customers.map((customer) =>
      apiRequest<Order[]>(endpoints.orders.byCustomer(customer.id)).catch(() => [])
    )
  );

  return orders.flat();
}

export function AdminDashboardPage(): JSX.Element {
  const auth = useAuth();
  const { language, t } = useI18n();
  const [customers, setCustomers] = useState<Customer[]>([]);
  const [orders, setOrders] = useState<Order[]>([]);
  const [batches, setBatches] = useState<ProductionBatch[]>([]);
  const [dashboard, setDashboard] = useState<ProductionDashboardItem[]>([]);
  const [productionQueue, setProductionQueue] = useState<QueueStatus | null>(null);
  const [error, setError] = useState<string | null>(null);
  const isArabic = language === "ar";
  const copy = isArabic
    ? {
        title: "نظرة عامة على اللوحة",
        subtitle: "راقب عمليات معصرة الزيتون والأداء اليومي",
        ordersToday: "طلبات اليوم",
        activeProductionBatches: "دفعات الإنتاج النشطة",
        completedOrdersHint: "طلبات مكتملة اليوم",
        productionQueue: "طابور الإنتاج",
        avgWait: "متوسط الانتظار",
        oilOutputToday: "إنتاج الزيت اليوم",
        estimatedFromProduction: "يتغير حسب بيانات الإنتاج",
        completedOrders: "طلبات مكتملة",
        readyForPickup: "جاهزة للاستلام",
        ordersPerDay: "الطلبات حسب اليوم",
        weeklyOil: "إنتاج الزيت الأسبوعي (لتر)",
        reportsSignal: "ملخص الحركة",
        recentActivity: "النشاط الأخير",
        noPaymentActivity: "لا توجد مدفوعات بعد",
        latestPayment: (orderId: number | string) => `آخر دفعة مسجلة للطلب #${orderId}`,
        productionTicketsWaiting: (count: number) => `${formatMetric(count, language)} تذاكر بانتظار الإنتاج`,
        productionQueueUnavailable: "طابور الإنتاج غير متاح",
        min: "دقيقة",
        orders: "طلبات",
        liters: "لتر",
        latest: "آخر تحديث",
        live: "مباشر",
        weeks: ["الأسبوع 1", "الأسبوع 2", "الأسبوع 3", "الأسبوع 4"]
      }
    : {
        title: "Dashboard Overview",
        subtitle: "Monitor your olive press operations and daily performance",
        ordersToday: "Today's Orders",
        activeProductionBatches: "Active Production Batches",
        completedOrdersHint: "completed today",
        productionQueue: "Production Queue",
        avgWait: "Avg wait",
        oilOutputToday: "Oil Output Today",
        estimatedFromProduction: "Changes with production data",
        completedOrders: "Completed Orders",
        readyForPickup: "Ready for pickup",
        ordersPerDay: "Orders Per Day",
        weeklyOil: "Weekly Oil Production (Liters)",
        reportsSignal: "Operations Signal",
        recentActivity: "Recent Activity",
        noPaymentActivity: "No payment activity yet",
        latestPayment: (orderId: number | string) => `Latest payment recorded for Order #${orderId}`,
        productionTicketsWaiting: (count: number) => `${formatMetric(count, language)} production tickets waiting`,
        productionQueueUnavailable: "Production queue unavailable",
        min: "min",
        orders: "orders",
        liters: "L",
        latest: "Latest",
        live: "Live",
        weeks: ["Week 1", "Week 2", "Week 3", "Week 4"]
      };

  useEffect(() => {
    let cancelled = false;

    async function load(): Promise<void> {
      const customerResult = canAction(auth, "CUSTOMER_READ").allowed
        ? await apiRequest<Customer[]>(endpoints.customers.list).catch(() => [])
        : [];

      const results = await Promise.allSettled([
        canAction(auth, "PRODUCTION_DASHBOARD").allowed ? fetchProductionDashboard() : Promise.resolve([]),
        apiRequest<QueueStatus>(endpoints.queues.status("PRODUCTION")),
        fetchOrdersForCustomers(customerResult),
        apiRequest<ProductionBatch[]>(endpoints.production.batches.list).catch(() => [])
      ]);

      if (cancelled) {
        return;
      }

      setCustomers(customerResult);
      setDashboard(results[0].status === "fulfilled" ? results[0].value : []);
      setProductionQueue(results[1].status === "fulfilled" ? results[1].value : null);
      setOrders(results[2].status === "fulfilled" ? results[2].value : []);
      setBatches(results[3].status === "fulfilled" ? results[3].value : []);

      const rejected = results.find((result) => result.status === "rejected");
      setError(rejected && rejected.status === "rejected" ? asErrorMessage(rejected.reason) : null);
    }

    void load().catch((requestError: unknown) => setError(asErrorMessage(requestError)));
    const timer = window.setInterval(() => {
      void load().catch((requestError: unknown) => setError(asErrorMessage(requestError)));
    }, 15000);

    return () => {
      cancelled = true;
      window.clearInterval(timer);
    };
  }, [auth.role]);

  const dayFormatter = new Intl.DateTimeFormat(language === "ar" ? "ar" : "en", { weekday: "short" });
  const todaysOrders = orders.filter((order) => isSameDay(orderDate(order)));
  const todaysBatches = batches.filter((batch) => isSameDay(batch.updatedAt ?? batch.createdAt));
  const completedOrders = orders.filter((order) => isCompletedStatus(order.status) || order.items.some((item) => isCompletedStatus(item.status))).length;
  const completedToday = orders.filter((order) => isCompletedStatus(order.status) && isSameDay(order.updatedAt ?? order.createdAt)).length;
  const activeBatches = batches.length
    ? batches.filter((batch) => !isCompletedStatus(batch.status)).length
    : dashboard.filter((item) => item.status.toUpperCase().includes("PROGRESS") || item.status.toUpperCase().includes("ACTIVE")).length;
  const oilOutputToday = todaysBatches.reduce((sum, batch) => sum + oilLitersFromKg(batch.predictedOilKg ?? null), 0)
    || dashboard.reduce((sum, item) => sum + item.throughputPerHour, 0);
  const productionWaiting = productionQueue?.stats.totalWaiting ?? 0;
  const lastSevenDays = Array.from({ length: 7 }, (_, index) => {
    const date = new Date();
    date.setDate(date.getDate() - (6 - index));
    const count = orders.filter((order) => isSameDay(orderDate(order), date)).length;
    return {
      key: date.toISOString(),
      label: dayFormatter.format(date),
      count
    };
  });
  const maxOrders = Math.max(...lastSevenDays.map((day) => day.count), 1);
  const fallbackWeeklyOil = dashboard.reduce((sum, item) => sum + item.throughputPerHour, 0) / 4;
  const weeklyOil = Array.from({ length: 4 }, (_, index) => {
    const weekStart = new Date();
    weekStart.setDate(weekStart.getDate() - ((3 - index) * 7 + 6));
    weekStart.setHours(0, 0, 0, 0);
    const weekEnd = new Date(weekStart);
    weekEnd.setDate(weekEnd.getDate() + 6);
    weekEnd.setHours(23, 59, 59, 999);

    const liters = batches
      .filter((batch) => {
        const date = new Date(batch.updatedAt ?? batch.createdAt ?? "");
        return !Number.isNaN(date.getTime()) && date >= weekStart && date <= weekEnd;
      })
      .reduce((sum, batch) => sum + oilLitersFromKg(batch.predictedOilKg ?? null), 0);

    return Math.round((liters || fallbackWeeklyOil) * 10) / 10;
  });
  const maxOil = Math.max(...weeklyOil, 1);
  const recentActivity = [
    productionQueue ? copy.productionTicketsWaiting(productionWaiting) : copy.productionQueueUnavailable,
    `${formatMetric(customers.length, language)} ${isArabic ? "زبائن مسجلين بالنظام" : "customers in the system"}`
  ];

  return (
    <div className="admin-dashboard-page">
      {error ? <Banner tone="danger" message={error} /> : null}
      <header className="admin-dashboard-header">
        <div>
          <h1>{copy.title}</h1>
          <p>{copy.subtitle}</p>
        </div>
        <div className="admin-dashboard-date">
          <strong>{new Intl.DateTimeFormat(language === "ar" ? "ar" : "en", { weekday: "short", month: "short", day: "numeric" }).format(new Date())}</strong>
          <span>{t(auth.role ?? "Admin")}</span>
        </div>
      </header>

      <section className="admin-kpi-grid compact">
        <div className="admin-kpi-card"><div><span>{copy.ordersToday}</span><strong>{formatMetric(todaysOrders.length, language)}</strong><small className="positive">{formatMetric(orders.length, language)} {copy.orders}</small></div><AdminKpiIcon name="cart" /></div>
        <div className="admin-kpi-card"><div><span>{copy.activeProductionBatches}</span><strong>{formatMetric(activeBatches, language)}</strong><small>{formatMetric(completedToday, language)} {copy.completedOrdersHint}</small></div><AdminKpiIcon name="factory" /></div>
        <div className="admin-kpi-card"><div><span>{copy.productionQueue}</span><strong>{formatMetric(productionWaiting, language)}</strong><small>{copy.avgWait}: {formatMetric(productionQueue?.stats.averageWaitTime ?? 0, language)} {copy.min}</small></div><AdminKpiIcon name="list" /></div>
        <div className="admin-kpi-card"><div><span>{copy.oilOutputToday}</span><strong>{formatMetric(oilOutputToday, language, 1)} {copy.liters}</strong><small>{copy.estimatedFromProduction}</small></div><AdminKpiIcon name="drop" /></div>
        <div className="admin-kpi-card"><div><span>{copy.completedOrders}</span><strong>{formatMetric(completedOrders, language)}</strong><small>{copy.readyForPickup}</small></div><AdminKpiIcon name="check" /></div>
      </section>

      <section className="admin-chart-grid">
        <div className="admin-chart-card">
          <h2>{copy.ordersPerDay}</h2>
          <div className="admin-bar-chart">
            {lastSevenDays.map((day) => (
              <div className="admin-bar-item has-tooltip" key={day.key}>
                <div className="admin-bar-track"><span style={{ height: `${Math.max((day.count / maxOrders) * 100, 8)}%` }} /></div>
                <small>{day.label}</small>
                <div className="admin-chart-tooltip">
                  <strong>{day.label}</strong>
                  <span>{copy.orders}: {formatMetric(day.count, language)}</span>
                </div>
              </div>
            ))}
          </div>
        </div>
        <div className="admin-chart-card">
          <h2>{copy.weeklyOil}</h2>
          <div className="admin-line-chart">
            <svg viewBox="0 0 520 210" role="img" aria-label={copy.weeklyOil}>
              <polyline fill="none" points={weeklyOil.map((value, index) => `${32 + index * 150},${190 - (value / maxOil) * 150}`).join(" ")} stroke="#6f813f" strokeWidth="3" />
              {weeklyOil.map((value, index) => (
                <circle cx={32 + index * 150} cy={190 - (value / maxOil) * 150} fill="#6f813f" key={index} r="5">
                  <title>{`${copy.weeks[index]}: ${formatMetric(value, language, 1)} ${copy.liters}`}</title>
                </circle>
              ))}
            </svg>
            <div className="admin-line-labels">{copy.weeks.map((week) => <span key={week}>{week}</span>)}</div>
          </div>
        </div>
      </section>

      <section className="admin-bottom-grid">
        <div className="admin-feed-card">
          <h2>{copy.reportsSignal}</h2>
          <div className="admin-alert-list">
            <div><span className="admin-dot info" /><strong>{formatMetric(todaysOrders.length, language)} {copy.ordersToday}</strong><small>{copy.live}</small></div>
            <div><span className="admin-dot warning" /><strong>{formatMetric(oilOutputToday, language, 1)} {copy.liters}</strong><small>{copy.oilOutputToday}</small></div>
          </div>
        </div>
        <div className="admin-feed-card">
          <h2>{copy.recentActivity}</h2>
          <div className="admin-activity-list">
            {recentActivity.map((activity, index) => (
              <div key={`${activity}-${index}`}><span className="admin-activity-icon">{formatMetric(index + 1, language)}</span><strong>{activity}</strong><small>{index === 0 ? copy.latest : copy.live}</small></div>
            ))}
          </div>
        </div>
      </section>
    </div>
  );
}

export function ProfilePage(): JSX.Element {
  const auth = useAuth();
  const { t } = useI18n();
  const { success, error } = useToastHelpers();
  const { notice, setNotice } = useNotice();
  const [profileForm, setProfileForm] = useState<UpdateProfileRequest>({
    firstName: auth.profile?.firstName ?? "",
    lastName: auth.profile?.lastName ?? "",
    phoneNumber: auth.profile?.phoneNumber ?? "",
    city: auth.profile?.city ?? "",
    maritalStatus: auth.profile?.maritalStatus ?? ""
  });
  const [passwordForm, setPasswordForm] = useState<PasswordChangeForm>({
    oldPassword: "",
    newPassword: "",
    confirmNewPassword: ""
  });
  const [profileErrors, setProfileErrors] = useState<FieldErrors>({});
  const [passwordErrors, setPasswordErrors] = useState<FieldErrors>({});
  const actionState = useActionState();
  const profilePermission = canAction(auth, "PROFILE_UPDATE");
  const passwordPermission = canAction(auth, "PASSWORD_CHANGE");

  useEffect(() => {
    setProfileForm({
      firstName: auth.profile?.firstName ?? "",
      lastName: auth.profile?.lastName ?? "",
      phoneNumber: auth.profile?.phoneNumber ?? "",
      city: auth.profile?.city ?? "",
      maritalStatus: auth.profile?.maritalStatus ?? ""
    });
  }, [auth.profile]);

  return (
    <div className="content-grid">
      <PageHeader eyebrow={t("Shared")} title={t("My Profile")} description={t("View and update the authenticated employee profile.")} />
      <NoticeBlock notice={notice} />
      <div className="content-grid two">
        <Card title={t("Profile Details")} subtitle={t("View your account details.")}>
          <ErrorSummary errors={profileErrors} />
          <div className="form-grid two">
            <div className="field">
              <label>{t("First Name")}</label>
              <input
                autoComplete="given-name"
                name="profile-first-name"
                onChange={(event) => setProfileForm((current) => ({ ...current, firstName: event.target.value }))}
                value={profileForm.firstName ?? ""}
              />
              <FieldError errors={profileErrors} name="firstName" />
            </div>
            <div className="field">
              <label>{t("Last Name")}</label>
              <input
                autoComplete="family-name"
                name="profile-last-name"
                onChange={(event) => setProfileForm((current) => ({ ...current, lastName: event.target.value }))}
                value={profileForm.lastName ?? ""}
              />
              <FieldError errors={profileErrors} name="lastName" />
            </div>
            <div className="field">
              <label>{t("Mobile Number")}</label>
              <input
                autoComplete="tel"
                name="profile-mobile-number"
                onChange={(event) => setProfileForm((current) => ({ ...current, phoneNumber: event.target.value }))}
                value={profileForm.phoneNumber ?? ""}
              />
              <FieldError errors={profileErrors} name="phoneNumber" />
            </div>
            <div className="field">
              <label>{t("Role")}</label>
              <input
                autoComplete="off"
                disabled
                name="profile-role"
                value={auth.profile?.role ? t(auth.profile.role) : ""}
              />
            </div>
            <div className="field">
              <label>{t("City")}</label>
              <input
                autoComplete="address-level2"
                name="profile-city"
                onChange={(event) => setProfileForm((current) => ({ ...current, city: event.target.value }))}
                value={profileForm.city ?? ""}
              />
            </div>
            <div className="field">
              <label>{t("Marital Status")}</label>
              <select
                autoComplete="off"
                name="profile-marital-status"
                onChange={(event) => setProfileForm((current) => ({ ...current, maritalStatus: event.target.value }))}
                value={profileForm.maritalStatus ?? ""}
              >
                <option value="">{t("Select marital status")}</option>
                <option value="SINGLE">{t("SINGLE")}</option>
                <option value="MARRIED">{t("MARRIED")}</option>
                <option value="DIVORCED">{t("DIVORCED")}</option>
                <option value="WIDOWED">{t("WIDOWED")}</option>
              </select>
              <FieldError errors={profileErrors} name="maritalStatus" />
            </div>
          </div>
          <div className="inline-actions">
            <ActionButton
              busyLabel="Saving..."
              disabled={!profilePermission.allowed}
              disabledReason={profilePermission.reason}
              isBusy={actionState.isBusy("save-profile")}
              onClick={() => {
                const nextErrors = validateProfile(profileForm);
                setProfileErrors(nextErrors);
                setNotice(null);

                if (Object.keys(nextErrors).length) {
                  return;
                }

                void actionState.runAction("save-profile", async () => {
                  try {
                    await apiRequest<Profile>(endpoints.profile.update, {
                      method: "PATCH",
                      body: profileForm
                    });
                    await auth.reloadProfile();
                    setNotice({ tone: "success", message: "Profile updated successfully." });
                    success(t("Profile updated"), t("Your profile changes were saved."));
                  } catch (requestError: unknown) {
                    setProfileErrors((current) => ({ ...current, ...getApiFieldErrors(requestError) }));
                    const message = getApiErrorMessage(requestError);
                    setNotice({ tone: "danger", message });
                    error(t("Profile update failed"), message);
                  }
                });
              }}
            >
              {t("Save profile")}
            </ActionButton>
          </div>
          <PermissionNote allowed={profilePermission.allowed} reason={profilePermission.reason} />
        </Card>
        <Card title={t("Password Change")} subtitle={t("Requires the current password and a new password of at least 8 characters.")}>
          <ErrorSummary errors={passwordErrors} />
          <div className="form-grid">
            <div className="field">
              <label>{t("Current Password")}</label>
              <input
                autoComplete="current-password"
                name="current-password"
                onChange={(event) => setPasswordForm((current) => ({ ...current, oldPassword: event.target.value }))}
                type="password"
                value={passwordForm.oldPassword}
              />
              <FieldError errors={passwordErrors} name="oldPassword" />
            </div>
            <div className="field">
              <label>{t("New Password")}</label>
              <input
                autoComplete="new-password"
                minLength={8}
                name="new-password"
                onChange={(event) => setPasswordForm((current) => ({ ...current, newPassword: event.target.value }))}
                type="password"
                value={passwordForm.newPassword}
              />
              <FieldError errors={passwordErrors} name="newPassword" />
            </div>
            <div className="field">
              <label>{t("Confirm New Password")}</label>
              <input
                autoComplete="new-password"
                minLength={8}
                name="confirm-new-password"
                onChange={(event) => setPasswordForm((current) => ({ ...current, confirmNewPassword: event.target.value }))}
                type="password"
                value={passwordForm.confirmNewPassword}
              />
              <FieldError errors={passwordErrors} name="confirmNewPassword" />
            </div>
          </div>
          <div className="inline-actions">
            <ActionButton
              className="secondary"
              disabled={!passwordPermission.allowed}
              disabledReason={passwordPermission.reason}
              busyLabel="Changing..."
              isBusy={actionState.isBusy("change-password")}
              onClick={() => {
                const nextErrors = validatePasswordChange(passwordForm);
                setPasswordErrors(nextErrors);
                setNotice(null);

                if (Object.keys(nextErrors).length) {
                  return;
                }

                void actionState.runAction("change-password", async () => {
                  try {
                    await apiRequest(endpoints.profile.changePassword, {
                      method: "POST",
                      body: {
                        oldPassword: passwordForm.oldPassword,
                        newPassword: passwordForm.newPassword
                      }
                    });
                    setPasswordForm({ oldPassword: "", newPassword: "", confirmNewPassword: "" });
                    setNotice({ tone: "success", message: "Password changed successfully." });
                    success(t("Password changed"), t("Your account password has been updated."));
                  } catch (requestError: unknown) {
                    setPasswordErrors((current) => ({ ...current, ...getApiFieldErrors(requestError) }));
                    const message = getApiErrorMessage(requestError);
                    setNotice({ tone: "danger", message });
                    error(t("Password change failed"), message);
                  }
                });
              }}
            >
              {t("Change Password")}
            </ActionButton>
          </div>
          <PermissionNote allowed={passwordPermission.allowed} reason={passwordPermission.reason} />
        </Card>
      </div>
    </div>
  );
}

export function CitiesPage(): JSX.Element {
  const auth = useAuth();
  const { t } = useI18n();
  const { notice, setNotice } = useNotice();
  const [cities, setCities] = useState<City[]>([]);
  const [cityName, setCityName] = useState("");
  const [selectedCity, setSelectedCity] = useState<City | null>(null);
  const [fieldErrors, setFieldErrors] = useState<FieldErrors>({});
  const actionState = useActionState();
  const createPermission = canAction(auth, "CITY_CREATE");
  const updatePermission = canAction(auth, "CITY_UPDATE");
  const deletePermission = canAction(auth, "CITY_DELETE");

  const loadCities = async (): Promise<void> => {
    setCities(await apiRequest<City[]>(endpoints.cities.list));
  };

  useEffect(() => {
    void loadCities().catch((requestError: unknown) => setNotice({ tone: "danger", message: asErrorMessage(requestError) }));
  }, []);

  return (
    <div className="content-grid">
      <PageHeader
        eyebrow={t("Master Data")}
        title={t("Cities")}
        description={t("Read, create, update, and delete city records through the gateway.")}
      />
      <NoticeBlock notice={notice} />
      <div className="content-grid two">
        <Card title={selectedCity ? `${t("Edit City #")}${selectedCity.id}` : t("Create City")}>
          <ErrorSummary errors={fieldErrors} />
          <div className="form-grid">
            <div className="field">
              <label>{t("Name")}</label>
              <input onChange={(event) => setCityName(event.target.value)} value={cityName} />
              <FieldError errors={fieldErrors} name="cityName" />
            </div>
          </div>
          <div className="inline-actions">
            <ActionButton
              busyLabel={selectedCity ? t("Saving...") : t("Creating...")}
              disabled={!(selectedCity ? updatePermission.allowed : createPermission.allowed)}
              disabledReason={selectedCity ? updatePermission.reason : createPermission.reason}
              isBusy={actionState.isBusy("save-city")}
              onClick={() => {
                const nextErrors = validateCity({ cityName });
                setFieldErrors(nextErrors);
                setNotice(null);

                if (Object.keys(nextErrors).length) {
                  return;
                }

                const payload: CityInput = { cityName: cityName.trim() };
                const request = selectedCity
                  ? () => apiRequest<City>(endpoints.cities.byId(selectedCity.id), { method: "PUT", body: payload })
                  : () => apiRequest<City>(endpoints.cities.create, { method: "POST", body: payload });

                void actionState.runAction("save-city", async () => {
                  try {
                    await request();
                    await loadCities();
                    setCityName("");
                    setSelectedCity(null);
                    setFieldErrors({});
                    setNotice({ tone: "success", message: selectedCity ? t("City updated.") : t("City created.") });
                  } catch (requestError: unknown) {
                    setFieldErrors((current) => ({ ...current, ...getApiFieldErrors(requestError) }));
                    setNotice({ tone: "danger", message: getApiErrorMessage(requestError) });
                  }
                });
              }}
              type="button"
            >
              {selectedCity ? t("Save changes") : t("Create City")}
            </ActionButton>
            {selectedCity ? (
              <ActionButton
                className="ghost"
                onClick={() => {
                  setSelectedCity(null);
                  setCityName("");
                  setFieldErrors({});
                }}
                type="button"
              >
                {t("Clear")}
              </ActionButton>
            ) : null}
          </div>
          <PermissionNote
            allowed={selectedCity ? updatePermission.allowed : createPermission.allowed}
            reason={selectedCity ? updatePermission.reason : createPermission.reason}
          />
        </Card>
        <Card title={t("City Directory")}>
          {cities.length ? (
            <div className="table-wrap">
              <table className="data-table">
                <thead>
                  <tr>
                    <th>{t("ID")}</th>
                    <th>{t("Cities")}</th>
                    <th>{t("Actions")}</th>
                  </tr>
                </thead>
                <tbody>
                  {cities.map((city) => (
                    <tr key={city.id}>
                      <td>{city.id}</td>
                      <td>{city.cityName}</td>
                      <td>
                        <div className="inline-actions">
                          <ActionButton
                            className="ghost"
                            onClick={() => {
                              setSelectedCity(city);
                              setCityName(city.cityName);
                              setFieldErrors({});
                            }}
                            type="button"
                          >
                            {t("Edit")}
                          </ActionButton>
                          <ActionButton
                            className="danger"
                            disabled={!deletePermission.allowed}
                            disabledReason={deletePermission.reason}
                            busyLabel={t("Deleting...")}
                            isBusy={actionState.isBusy(`delete-city-${city.id}`)}
                            onClick={() => {
                              void actionState.runAction(`delete-city-${city.id}`, async () => {
                                try {
                                  await apiRequest(endpoints.cities.byId(city.id), { method: "DELETE" });
                                  await loadCities();
                                  setNotice({ tone: "success", message: `Deleted city ${city.cityName}.` });
                                } catch (requestError: unknown) {
                                  setNotice({ tone: "danger", message: getApiErrorMessage(requestError) });
                                }
                              });
                            }}
                            type="button"
                          >
                            {t("Delete")}
                          </ActionButton>
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          ) : (
            <EmptyState title="No cities yet" description="Create the first city before registering customers." />
          )}
        </Card>
      </div>
    </div>
  );
}

export function CustomersPage(): JSX.Element {
  const auth = useAuth();
  const { t } = useI18n();
  const { notice, setNotice } = useNotice();
  const [customers, setCustomers] = useState<Customer[]>([]);
  const [cities, setCities] = useState<City[]>([]);
  const [selectedCustomer, setSelectedCustomer] = useState<Customer | null>(null);
  const [nationalIdLookup, setNationalIdLookup] = useState("");
  const [cityText, setCityText] = useState("");
  const [form, setForm] = useState<CustomerInput>({
    nationalId: "",
    firstName: "",
    lastName: "",
    phoneNumber: "",
    cityId: undefined,
    isMember: false
  });
  const [formErrors, setFormErrors] = useState<FieldErrors>({});
  const [utilityErrors, setUtilityErrors] = useState<FieldErrors>({});
  const actionState = useActionState();
  const readPermission = canAction(auth, "CUSTOMER_READ");
  const createPermission = canAction(auth, "CUSTOMER_CREATE");
  const updatePermission = canAction(auth, "CUSTOMER_UPDATE");
  const searchPermission = canAction(auth, "CUSTOMER_SEARCH");
  const deletePermission = canAction(auth, "CUSTOMER_DELETE");
  const membershipEditAllowed = auth.role === "ADMIN";
  const cityCreatePermission = canAction(auth, "CITY_CREATE");
  const canSearchCustomers = searchPermission.allowed || readPermission.allowed;
  const searchDisabledReason = searchPermission.allowed
    ? undefined
    : readPermission.allowed
      ? undefined
      : searchPermission.reason ?? readPermission.reason;
  const isAccountant = auth.role === "ACCOUNTANT";

  const loadCustomers = async (): Promise<void> => {
    const cityPermission = canAction(auth, "CITY_READ");
    const [customerList, cityList] = await Promise.all([
      apiRequest<Customer[]>(endpoints.customers.list),
      cityPermission.allowed ? apiRequest<City[]>(endpoints.cities.list) : Promise.resolve([])
    ]);
    setCustomers(customerList);
    setCities(cityList);
  };

  const cityNameById = (cityId?: number): string => cities.find((city) => city.id === cityId)?.cityName ?? "";

  const setCustomerCityText = (value: string): void => {
    const matchingCity = cities.find((city) => city.cityName.trim().toLowerCase() === value.trim().toLowerCase());
    setCityText(value);
    setForm((current) => ({ ...current, cityId: matchingCity?.id }));
  };

  const resolveCustomerCityId = async (): Promise<number> => {
    const cityName = cityText.trim();

    if (!cityName) {
      throw new Error("City is required.");
    }

    const existingCity = cities.find((city) => city.cityName.trim().toLowerCase() === cityName.toLowerCase());

    if (existingCity) {
      return existingCity.id;
    }

    if (!cityCreatePermission.allowed) {
      throw new Error(cityCreatePermission.reason ?? "City is required.");
    }

    const createdCity = await apiRequest<City>(endpoints.cities.create, {
      method: "POST",
      body: { cityName }
    });
    setCities((current) => [createdCity, ...current.filter((city) => city.id !== createdCity.id)]);
    return createdCity.id;
  };

  const runCustomerNationalIdSearch = (): void => {
    const nextErrors: FieldErrors = {};
    if (!/^\d{9}$/.test(nationalIdLookup.trim())) {
      nextErrors.nationalIdLookup = "Enter a 9-digit national ID.";
    }
    setUtilityErrors(nextErrors);

    if (Object.keys(nextErrors).length) {
      return;
    }

    void actionState.runAction("resolve-customer", async () => {
      try {
        const customer = await apiRequest<Customer>(endpoints.customers.byNationalId(nationalIdLookup.trim()));
        setSelectedCustomer(customer);
        setForm(customer);
        setCityText(cityNameById(customer.cityId));
        setCustomers((current) => [customer, ...current.filter((entry) => entry.id !== customer.id)]);
        setUtilityErrors({});
        setNotice({ tone: "success", message: `Resolved customer #${customer.id}.` });
      } catch (requestError: unknown) {
        setUtilityErrors((current) => ({ ...current, ...getApiFieldErrors(requestError) }));
        setNotice({ tone: "danger", message: getApiErrorMessage(requestError) });
      }
    });
  };

  useEffect(() => {
    void loadCustomers().catch((requestError: unknown) => setNotice({ tone: "danger", message: asErrorMessage(requestError) }));
  }, []);

  if (isAccountant) {
    return (
      <div className="content-grid">
        <PageHeader
          eyebrow="Customer Domain"
          title="Customer Directory"
          description="Read-only customer directory for accounting lookup."
        />
        <NoticeBlock notice={notice} />
        <Card title="Customer Directory">
          {customers.length ? (
            <div className="table-wrap">
              <table className="data-table">
                <thead>
                  <tr>
                    <th>{t("ID")}</th>
                    <th>{t("Name")}</th>
                    <th>{t("National ID")}</th>
                    <th>{t("City")}</th>
                    <th>{t("Phone Number")}</th>
                    <th>{t("Membership")}</th>
                  </tr>
                </thead>
                <tbody>
                  {customers.map((customer) => (
                    <tr key={customer.id}>
                      <td>{customer.id}</td>
                      <td>{customer.firstName} {customer.lastName}</td>
                      <td>{customer.nationalId}</td>
                      <td>{cityNameById(customer.cityId) || customer.cityId}</td>
                      <td>{customer.phoneNumber}</td>
                      <td>{customer.isMember ? t("Member") : t("Standard")}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          ) : (
            <EmptyState title="No customers yet" description="Customer records will appear here once they are available." />
          )}
        </Card>
      </div>
    );
  }

  return (
    <div className="content-grid">
      <PageHeader
        eyebrow="Customer Domain"
        title="Customers"
        description="Create, find, and update customers with a simple national ID workflow."
      />
      <NoticeBlock notice={notice} />
      <div className="content-grid two">
        <Card title={selectedCustomer ? `Edit Customer #${selectedCustomer.id}` : "Create Customer"}>
          <ErrorSummary errors={formErrors} />
          <div className="form-grid two">
            <div className="field">
              <label>{t("National ID")}</label>
              <input
                onChange={(event) => setForm((current) => ({ ...current, nationalId: event.target.value }))}
                value={form.nationalId ?? ""}
              />
              <FieldError errors={formErrors} name="nationalId" />
            </div>
            <div className="field">
              <label>{t("City")}</label>
              <input
                list="admin-customer-city-options"
                onChange={(event) => setCustomerCityText(event.target.value)}
                placeholder={t("Select city")}
                value={cityText}
              />
              <datalist id="admin-customer-city-options">
                {cities.map((city) => (
                  <option key={city.id} value={city.cityName} />
                ))}
              </datalist>
              <FieldError errors={formErrors} name="cityId" />
            </div>
            <div className="field">
              <label>{t("First Name")}</label>
              <input
                onChange={(event) => setForm((current) => ({ ...current, firstName: event.target.value }))}
                value={form.firstName ?? ""}
              />
              <FieldError errors={formErrors} name="firstName" />
            </div>
            <div className="field">
              <label>{t("Last Name")}</label>
              <input
                onChange={(event) => setForm((current) => ({ ...current, lastName: event.target.value }))}
                value={form.lastName ?? ""}
              />
              <FieldError errors={formErrors} name="lastName" />
            </div>
            <div className="field">
              <label>{t("Phone Number")}</label>
              <input
                onChange={(event) => setForm((current) => ({ ...current, phoneNumber: event.target.value }))}
                value={form.phoneNumber ?? ""}
              />
              <FieldError errors={formErrors} name="phoneNumber" />
            </div>
            {membershipEditAllowed ? (
              <div className="field">
                <label>{t("Membership")}</label>
                <select
                  onChange={(event) => setForm((current) => ({ ...current, isMember: event.target.value === "true" }))}
                  value={String(form.isMember ?? false)}
                >
                  <option value="false">{t("Standard")}</option>
                  <option value="true">{t("Member")}</option>
                </select>
              </div>
            ) : null}
          </div>
          <div className="inline-actions">
            <ActionButton
              busyLabel={selectedCustomer ? "Saving..." : "Creating..."}
              disabled={!(selectedCustomer ? updatePermission.allowed : createPermission.allowed)}
              disabledReason={selectedCustomer ? updatePermission.reason : createPermission.reason}
              isBusy={actionState.isBusy("save-customer")}
              onClick={() => {
                const nextErrors = validateCustomer(
                  selectedCustomer
                    ? {
                        firstName: form.firstName,
                        lastName: form.lastName,
                        phoneNumber: form.phoneNumber,
                        cityId: cityText.trim() ? 1 : undefined
                      }
                    : {
                        nationalId: form.nationalId,
                        firstName: form.firstName,
                        lastName: form.lastName,
                        phoneNumber: form.phoneNumber,
                        cityId: cityText.trim() ? 1 : undefined
                      }
                );
                setFormErrors(nextErrors);
                setNotice(null);

                if (Object.keys(nextErrors).length) {
                  return;
                }

                void actionState.runAction("save-customer", async () => {
                  try {
                    const cityId = await resolveCustomerCityId();
                    const createBody = {
                      nationalId: form.nationalId,
                      firstName: form.firstName,
                      lastName: form.lastName,
                      phoneNumber: form.phoneNumber,
                      cityId,
                      ...(membershipEditAllowed ? { isMember: form.isMember } : {})
                    };
                    const updateBody = {
                      firstName: form.firstName,
                      lastName: form.lastName,
                      phoneNumber: form.phoneNumber,
                      cityId,
                      ...(membershipEditAllowed ? { isMember: form.isMember } : {})
                    };

                    if (selectedCustomer) {
                      await apiRequest<Customer>(endpoints.customers.byId(selectedCustomer.id), {
                        method: "PUT",
                        body: updateBody
                      });
                    } else {
                      await apiRequest<Customer>(endpoints.customers.create, { method: "POST", body: createBody });
                    }
                    await loadCustomers();
                    setSelectedCustomer(null);
                    setCityText("");
                    setFormErrors({});
                    setForm({
                      nationalId: "",
                      firstName: "",
                      lastName: "",
                      phoneNumber: "",
                      cityId: undefined,
                      isMember: false
                    });
                    setNotice({ tone: "success", message: selectedCustomer ? "Customer updated." : "Customer created." });
                  } catch (requestError: unknown) {
                    setFormErrors((current) => ({ ...current, ...getApiFieldErrors(requestError) }));
                    setNotice({ tone: "danger", message: getApiErrorMessage(requestError) });
                  }
                });
              }}
              type="button"
            >
              {selectedCustomer ? t("Save customer") : t("Create Customer")}
            </ActionButton>
            {selectedCustomer ? (
              <ActionButton
                className="ghost"
                onClick={() => {
                  setSelectedCustomer(null);
                  setFormErrors({});
                  setCityText("");
                  setForm({
                    nationalId: "",
                    firstName: "",
                    lastName: "",
                    phoneNumber: "",
                    cityId: undefined,
                    isMember: false
                  });
                }}
                type="button"
              >
                {t("Clear")}
              </ActionButton>
            ) : null}
          </div>
          <PermissionNote
            allowed={selectedCustomer ? updatePermission.allowed : createPermission.allowed}
            reason={selectedCustomer ? updatePermission.reason : createPermission.reason}
          />
        </Card>
        <Card title="Customer Search" subtitle="Find a customer by national ID and work with the selected record.">
          <ErrorSummary errors={utilityErrors} />
          <div
            className="form-grid"
            onKeyDown={(event) => {
              if (event.key === "Enter" && canSearchCustomers) {
                event.preventDefault();
                runCustomerNationalIdSearch();
              }
            }}
          >
            <div className="field">
              <label>{t("Search by National ID")}</label>
              <input onChange={(event) => setNationalIdLookup(event.target.value)} value={nationalIdLookup} />
              <FieldError errors={utilityErrors} name="nationalIdLookup" />
            </div>
            <div className="inline-actions">
              <ActionButton
                className="secondary"
                disabled={!canSearchCustomers}
                disabledReason={searchDisabledReason}
                busyLabel="Searching..."
                isBusy={actionState.isBusy("resolve-customer")}
                onClick={runCustomerNationalIdSearch}
                type="button"
              >
                {t("Search")}
              </ActionButton>
              <ActionButton
                className="ghost"
                disabled={!selectedCustomer || !deletePermission.allowed}
                disabledReason={deletePermission.reason}
                busyLabel="Deleting..."
                isBusy={actionState.isBusy("delete-customer")}
                onClick={() => {
                  if (!selectedCustomer) {
                    return;
                  }

                  void actionState.runAction("delete-customer", async () => {
                    try {
                      await apiRequest(endpoints.customers.byId(selectedCustomer.id), { method: "DELETE" });
                      await loadCustomers();
                      setSelectedCustomer(null);
                      setNotice({ tone: "success", message: `Deleted customer #${selectedCustomer.id}.` });
                    } catch (requestError: unknown) {
                      setNotice({ tone: "danger", message: getApiErrorMessage(requestError) });
                    }
                  });
                }}
                type="button"
              >
                {t("Delete selected")}
              </ActionButton>
            </div>
            <PermissionNote allowed={canSearchCustomers} reason={searchDisabledReason} />
          </div>
        </Card>
      </div>
      <Card title="Customer Directory">
        {customers.length ? (
          <div className="table-wrap">
            <table className="data-table">
              <thead>
                <tr>
                  <th>{t("ID")}</th>
                  <th>{t("Name")}</th>
                  <th>{t("National ID")}</th>
                  <th>{t("City")}</th>
                  <th>{t("Phone Number")}</th>
                  <th>{t("Membership")}</th>
                </tr>
              </thead>
              <tbody>
                {customers.map((customer) => (
                  <tr
                    key={customer.id}
                    onClick={() => {
                      setSelectedCustomer(customer);
                      setForm(customer);
                      setCityText(cityNameById(customer.cityId));
                      setFormErrors({});
                    }}
                  >
                    <td>{customer.id}</td>
                    <td>{customer.firstName} {customer.lastName}</td>
                    <td>{customer.nationalId}</td>
                    <td>{cityNameById(customer.cityId) || customer.cityId}</td>
                    <td>{customer.phoneNumber}</td>
                    <td>{customer.isMember ? t("Member") : t("Standard")}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ) : (
          <EmptyState title="No customers yet" description="Create a customer to start placing orders." />
        )}
      </Card>
    </div>
  );
}

function LegacyProductsPage(): JSX.Element {
  const auth = useAuth();
  const { t } = useI18n();
  const { notice, setNotice } = useNotice();
  const [products, setProducts] = useState<Product[]>([]);
  const [form, setForm] = useState<ProductInput>({
    productName: "",
    productType: "OLIVE",
    inventory: 0,
    price: 0.6,
    unit: "KG"
  });
  const [selectedProduct, setSelectedProduct] = useState<Product | null>(null);
  const [activationId, setActivationId] = useState("");
  const [inventoryPatch, setInventoryPatch] = useState("");
  const [formErrors, setFormErrors] = useState<FieldErrors>({});
  const [utilityErrors, setUtilityErrors] = useState<FieldErrors>({});
  const actionState = useActionState();
  const createPermission = canAction(auth, "PRODUCT_CREATE");
  const updatePermission = canAction(auth, "PRODUCT_UPDATE");
  const inventoryPermission = canAction(auth, "PRODUCT_UPDATE_INVENTORY");
  const deletePermission = canAction(auth, "PRODUCT_DELETE");

  const loadProducts = async (): Promise<void> => {
    setProducts(await apiRequest<Product[]>(endpoints.products.list));
  };

  useEffect(() => {
    void loadProducts().catch((requestError: unknown) => setNotice({ tone: "danger", message: asErrorMessage(requestError) }));
  }, []);

  return (
    <div className="content-grid">
      <PageHeader
        eyebrow="Catalog"
        title="Products"
        description="Manage products used in orders and payments."
      />
      <NoticeBlock notice={notice} />
      <div className="content-grid two">
        <Card title={selectedProduct ? `Edit Product #${selectedProduct.id}` : "Create Product"}>
          <ErrorSummary errors={formErrors} />
          <div className="form-grid two">
            <div className="field">
              <label>{t("Name")}</label>
              <input
                onChange={(event) => setForm((current) => ({ ...current, productName: event.target.value }))}
                value={form.productName}
              />
              <FieldError errors={formErrors} name="productName" />
            </div>
            <div className="field">
              <label>{t("Type")}</label>
              <select
                onChange={(event) => setForm((current) => ({ ...current, productType: event.target.value }))}
                value={form.productType}
              >
                <option value="OLIVE">OLIVE</option>
                  <option value="JIFT">JIFT</option>
                  <option value="GALLON">GALLON</option>
                </select>
              <FieldError errors={formErrors} name="productType" />
            </div>
            <div className="field">
              <label>{t("Inventory")}</label>
              <input
                onChange={(event) => setForm((current) => ({ ...current, inventory: Number(event.target.value) }))}
                type="number"
                value={form.inventory ?? 0}
              />
              <FieldError errors={formErrors} name="inventory" />
            </div>
            <div className="field">
              <label>{t("Unit")}</label>
              <select
                onChange={(event) => setForm((current) => ({ ...current, unit: event.target.value }))}
                value={form.unit}
              >
                <option value="KG">KG</option>
                  <option value="PCS">PCS</option>
                  <option value="LITER">LITER</option>
                </select>
              <FieldError errors={formErrors} name="unit" />
            </div>
            <div className="field">
              <label>{t("Price")}</label>
              <input
                onChange={(event) => setForm((current) => ({ ...current, price: Number(event.target.value) }))}
                step="0.01"
                type="number"
                value={form.price}
              />
              <FieldError errors={formErrors} name="price" />
            </div>
          </div>
          <div className="inline-actions">
            <ActionButton
              busyLabel={selectedProduct ? "Saving..." : "Creating..."}
              disabled={!(selectedProduct ? updatePermission.allowed : createPermission.allowed)}
              disabledReason={selectedProduct ? updatePermission.reason : createPermission.reason}
              isBusy={actionState.isBusy("save-product")}
              onClick={() => {
                const nextErrors = validateProduct(form);
                setFormErrors(nextErrors);
                setNotice(null);

                if (Object.keys(nextErrors).length) {
                  return;
                }

                const request = selectedProduct
                  ? () => apiRequest<Product>(endpoints.products.byId(selectedProduct.id), { method: "PUT", body: form })
                  : () => apiRequest<Product>(endpoints.products.create, { method: "POST", body: form });

                void actionState.runAction("save-product", async () => {
                  try {
                    await request();
                    await loadProducts();
                    setSelectedProduct(null);
                    setFormErrors({});
                    setForm({ productName: "", productType: "OLIVE", inventory: 0, price: 0.6, unit: "KG" });
                    setNotice({ tone: "success", message: selectedProduct ? "Product updated." : "Product created." });
                  } catch (requestError: unknown) {
                    setFormErrors((current) => ({ ...current, ...getApiFieldErrors(requestError) }));
                    setNotice({ tone: "danger", message: getApiErrorMessage(requestError) });
                  }
                });
              }}
              type="button"
            >
              {selectedProduct ? t("Save product") : t("Create product")}
            </ActionButton>
            {selectedProduct ? (
              <ActionButton
                className="ghost"
                onClick={() => {
                  setSelectedProduct(null);
                  setFormErrors({});
                  setForm({ productName: "", productType: "OLIVE", inventory: 0, price: 0.6, unit: "KG" });
                }}
                type="button"
              >
                {t("Clear")}
              </ActionButton>
            ) : null}
          </div>
          <PermissionNote
            allowed={selectedProduct ? updatePermission.allowed : createPermission.allowed}
            reason={selectedProduct ? updatePermission.reason : createPermission.reason}
          />
        </Card>
        <Card title="Product Controls">
          <ErrorSummary errors={utilityErrors} />
          <div className="form-grid">
            <div className="field">
              <label>{t("Inventory")}</label>
              <input onChange={(event) => setInventoryPatch(event.target.value)} type="number" value={inventoryPatch} />
              <FieldError errors={utilityErrors} name="inventoryPatch" />
            </div>
            <div className="inline-actions">
              <ActionButton
                className="secondary"
                disabled={!selectedProduct || !inventoryPermission.allowed}
                disabledReason={inventoryPermission.reason}
                busyLabel="Updating..."
                isBusy={actionState.isBusy("inventory")}
                onClick={() => {
                  if (!selectedProduct) {
                    return;
                  }

                  const parsed = Number(inventoryPatch);
                  const nextErrors: FieldErrors = {};
                  if (!(inventoryPatch.trim() && Number.isFinite(parsed) && parsed >= 0)) {
                    nextErrors.inventoryPatch = "Enter a non-negative inventory value.";
                  }
                  setUtilityErrors(nextErrors);

                  if (Object.keys(nextErrors).length) {
                    return;
                  }

                  void actionState.runAction("inventory", async () => {
                    try {
                      await apiRequest(endpoints.products.inventory(selectedProduct.id), {
                        method: "PATCH",
                        body: { inventory: Number(inventoryPatch) }
                      });
                      await loadProducts();
                      setUtilityErrors({});
                      setNotice({ tone: "success", message: "Inventory updated." });
                    } catch (requestError: unknown) {
                      setNotice({ tone: "danger", message: getApiErrorMessage(requestError) });
                    }
                  });
                }}
                type="button"
              >
                {t("Update inventory")}
              </ActionButton>
              <ActionButton
                className="danger"
                disabled={!selectedProduct || !deletePermission.allowed}
                disabledReason={deletePermission.reason}
                busyLabel="Deactivating..."
                isBusy={actionState.isBusy("deactivate-product")}
                onClick={() => {
                  if (!selectedProduct) {
                    return;
                  }

                  void actionState.runAction("deactivate-product", async () => {
                    try {
                      await apiRequest(endpoints.products.byId(selectedProduct.id), { method: "DELETE" });
                      await loadProducts();
                      setSelectedProduct(null);
                      setNotice({ tone: "success", message: "Product deactivated." });
                    } catch (requestError: unknown) {
                      setNotice({ tone: "danger", message: getApiErrorMessage(requestError) });
                    }
                  });
                }}
                type="button"
              >
                {t("Deactivate selected")}
              </ActionButton>
            </div>
            <PermissionNote allowed={inventoryPermission.allowed} reason={inventoryPermission.reason} />
            <div className="field">
              <label>{t("Reactivate Product")}</label>
              <input onChange={(event) => setActivationId(event.target.value)} value={activationId} />
              <FieldError errors={utilityErrors} name="activationId" />
            </div>
            <div className="inline-actions">
              <ActionButton
                className="ghost"
                disabled={!updatePermission.allowed}
                disabledReason={updatePermission.reason}
                busyLabel="Activating..."
                isBusy={actionState.isBusy("activate-product")}
                onClick={() => {
                  const parsed = Number(activationId);
                  const nextErrors: FieldErrors = {};
                  if (!(activationId.trim() && Number.isFinite(parsed) && parsed > 0)) {
                    nextErrors.activationId = "Enter a valid product ID.";
                  }
                  setUtilityErrors(nextErrors);

                  if (Object.keys(nextErrors).length) {
                    return;
                  }

                  void actionState.runAction("activate-product", async () => {
                    try {
                      await apiRequest(endpoints.products.activate(activationId.trim()), { method: "PATCH" });
                      await loadProducts();
                      setNotice({ tone: "success", message: `Activate request sent for product #${activationId}.` });
                    } catch (requestError: unknown) {
                      setNotice({ tone: "danger", message: getApiErrorMessage(requestError) });
                    }
                  });
                }}
                type="button"
              >
                {t("Activate product")}
              </ActionButton>
            </div>
          </div>
        </Card>
        <Card title="Active Products">
          {products.length ? (
            <div className="table-wrap">
              <table className="data-table">
                <thead>
                  <tr>
                  <th>{t("ID")}</th>
                  <th>{t("Name")}</th>
                  <th>{t("Type")}</th>
                  <th>{t("Unit")}</th>
                  <th>{t("Inventory")}</th>
                  <th>{t("Price")}</th>
                  </tr>
                </thead>
                <tbody>
                  {products.map((product) => (
                    <tr key={product.id} onClick={() => {
                      setSelectedProduct(product);
                      setForm(product);
                      setInventoryPatch(String(product.inventory ?? 0));
                      setFormErrors({});
                    }}>
                      <td>{product.id}</td>
                      <td>{product.productName}</td>
                      <td>{product.productType}</td>
                      <td>{product.unit}</td>
                      <td>{product.inventory ?? "--"}</td>
                      <td>{formatCurrency(product.price)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          ) : (
            <EmptyState title="No active products" description="Create or reactivate products to start taking orders." />
          )}
        </Card>
      </div>
    </div>
  );
}

const PRODUCT_TYPE_OPTIONS = [
  { value: "OLIVE", ar: "عصر زيتون", en: "Olive pressing", unit: "KG", price: 0.6 },
  { value: "GALLON", ar: "جالونات", en: "Gallons", unit: "PCS", price: 1 },
  { value: "JIFT", ar: "جفت", en: "Jift", unit: "PCS", price: 1 }
];

const UNIT_OPTIONS = ["KG", "LITER", "PCS", "كغ", "لتر", "قطعة", "جالون"];

const CATALOG_PRODUCT_OPTIONS = [
  { value: "OLIVE_MEMBER", productType: "OLIVE", ar: "زيتون للمساهم", en: "Member olive", unit: "KG", price: 0.4 },
  { value: "OLIVE_STANDARD", productType: "OLIVE", ar: "زيتون لغير المساهم", en: "Non-member olive", unit: "KG", price: 0.6 },
  { value: "JIFT", productType: "JIFT", ar: "الجفت", en: "Pomace", unit: "PCS", price: 12 },
  { value: "GALLON", productType: "GALLON", ar: "الجالونات", en: "Gallons", unit: "PCS", price: 15 }
];

function findProductTypeOption(value: string): (typeof CATALOG_PRODUCT_OPTIONS)[number] | undefined {
  const normalized = value.trim().toLowerCase();
  return CATALOG_PRODUCT_OPTIONS.find(
    (option) =>
      option.value.toLowerCase() === normalized ||
      option.productType.toLowerCase() === normalized ||
      option.ar.toLowerCase() === normalized ||
      option.en.toLowerCase() === normalized
  );
}

function normalizeProductTypeForApi(value: string): string {
  return findProductTypeOption(value)?.productType ?? value.trim().toUpperCase();
}

function productTypeLabel(value: string, language: "ar" | "en"): string {
  const option = findProductTypeOption(value);
  if (!option) {
    return value;
  }
  return language === "ar" ? option.ar : option.en;
}

function productNameFromType(value: string): string {
  return findProductTypeOption(value)?.ar ?? value.trim();
}

function hasBrokenProductName(product: Product): boolean {
  return !product.productName?.trim() || product.productName.includes("?") || product.productName.includes("�");
}

function isAccidentalOliveProduct(product: Product): boolean {
  return isOliveProductType(product.productType) && (
    hasBrokenProductName(product) ||
    product.productName.trim() === "عصير زيتون" ||
    product.productName.toLowerCase().trim() === "olive pressing"
  );
}

function displayProductName(product: Product): string {
  const price = Number(product.price);
  const type = product.productType?.toUpperCase();
  const name = product.productName?.trim() ?? "";

  if (type === "JIFT" || price === 12) {
    return "جفت";
  }
  if (type === "GALLON" || price === 15) {
    return "جالونات";
  }
  if (isOliveProductType(type)) {
    if (price === 0.4 || name.includes("للمساهم")) {
      return "زيتون للمساهم";
    }
    if (price === 0.6 || name.includes("غير")) {
      return "زيتون لغير المساهم";
    }
  }

  return name;
}

function defaultProductForm(language: "ar" | "en"): ProductInput {
  const option = CATALOG_PRODUCT_OPTIONS[0];
  return {
    productName: option.ar,
    productType: language === "ar" ? option.ar : option.en,
    inventory: 0,
    price: option.price,
    unit: option.unit
  };
}

function validateProductCatalog(values: ProductInput): FieldErrors {
  const errors: FieldErrors = {};
  if (!values.productType.trim()) {
    errors.productType = "Product type is required.";
  }
  if (!values.unit.trim()) {
    errors.unit = "Unit is required.";
  }
  if (!Number.isFinite(values.price) || values.price <= 0) {
    errors.price = "Price must be greater than zero.";
  }
  return errors;
}

export function ProductsPage(): JSX.Element {
  const auth = useAuth();
  const { language, t } = useI18n();
  const { notice, setNotice } = useNotice();
  const [products, setProducts] = useState<Product[]>([]);
  const [form, setForm] = useState<ProductInput>(() => defaultProductForm(language));
  const [selectedProduct, setSelectedProduct] = useState<Product | null>(null);
  const [formErrors, setFormErrors] = useState<FieldErrors>({});
  const actionState = useActionState();
  const createPermission = canAction(auth, "PRODUCT_CREATE");
  const updatePermission = canAction(auth, "PRODUCT_UPDATE");
  const deletePermission = canAction(auth, "PRODUCT_DELETE");
  const isArabic = language === "ar";
  const visibleProducts = useMemo(
    () => products
      .filter((product) => !isAccidentalOliveProduct(product))
      .sort((first, second) => first.id - second.id),
    [products]
  );

  const loadProducts = async (): Promise<void> => {
    setProducts(await apiRequest<Product[]>(endpoints.products.list));
  };

  useEffect(() => {
    void loadProducts().catch((requestError: unknown) => setNotice({ tone: "danger", message: asErrorMessage(requestError) }));
  }, []);

  const resetForm = (): void => {
    setSelectedProduct(null);
    setFormErrors({});
    setForm(defaultProductForm(language));
  };

  const saveProduct = (): void => {
    const productType = normalizeProductTypeForApi(form.productType);
    const payload = {
      productName: productNameFromType(form.productType),
      productType,
      unit: form.unit.trim(),
      price: Number(form.price),
      inventory: 0,
      inventoryTotalQuantity: 0
    };
    const nextErrors = validateProductCatalog(payload);
    setFormErrors(nextErrors);

    if (Object.keys(nextErrors).length) {
      return;
    }

    const request = selectedProduct
      ? () => apiRequest<Product>(endpoints.products.byId(selectedProduct.id), { method: "PUT", body: payload })
      : () => apiRequest<Product>(endpoints.products.create, { method: "POST", body: payload });

    void actionState.runAction("save-product", async () => {
      try {
        await request();
        await loadProducts();
        resetForm();
        setNotice({ tone: "success", message: selectedProduct ? (isArabic ? "تم تحديث السعر." : "Price updated.") : (isArabic ? "تم إنشاء المنتج." : "Product created.") });
      } catch (requestError: unknown) {
        setFormErrors((current) => ({ ...current, ...getApiFieldErrors(requestError) }));
        setNotice({ tone: "danger", message: getApiErrorMessage(requestError) });
      }
    });
  };

  return (
    <div className="content-grid">
      <PageHeader title={isArabic ? "المنتجات" : "Products"} />
      <NoticeBlock notice={notice} />

      <div className="content-grid two">
        <Card title={selectedProduct ? (isArabic ? `تحديث المنتج #${selectedProduct.id}` : `Update Product #${selectedProduct.id}`) : (isArabic ? "إنشاء منتج" : "Create Product")}>
          <ErrorSummary errors={formErrors} />
          <div className="form-grid two">
            <div className="field">
              <label>{t("Type")}</label>
              <input
                list="product-type-options"
                onChange={(event) => {
                  const productType = event.target.value;
                  const option = findProductTypeOption(productType);
                  setForm((current) => ({
                    ...current,
                    productType,
                    productName: productNameFromType(productType),
                    unit: option?.unit ?? current.unit,
                    price: selectedProduct ? current.price : (option?.price ?? current.price)
                  }));
                }}
                value={form.productType}
              />
              <datalist id="product-type-options">
                {CATALOG_PRODUCT_OPTIONS.map((option) => (
                  <option key={option.value} value={isArabic ? option.ar : option.en} />
                ))}
              </datalist>
              <FieldError errors={formErrors} name="productType" />
            </div>
            <div className="field">
              <label>{t("Unit")}</label>
              <input
                list="product-unit-options"
                onChange={(event) => setForm((current) => ({ ...current, unit: event.target.value }))}
                value={form.unit}
              />
              <datalist id="product-unit-options">
                {UNIT_OPTIONS.map((unit) => (
                  <option key={unit} value={unit} />
                ))}
              </datalist>
              <FieldError errors={formErrors} name="unit" />
            </div>
            <div className="field">
              <label>{t("Price")}</label>
              <input
                onChange={(event) => setForm((current) => ({ ...current, price: Number(event.target.value) }))}
                step="0.01"
                type="number"
                value={form.price}
              />
              <FieldError errors={formErrors} name="price" />
            </div>
          </div>
          <div className="inline-actions">
            <ActionButton
              busyLabel={isArabic ? "جار الحفظ..." : "Saving..."}
              disabled={!(selectedProduct ? updatePermission.allowed : createPermission.allowed)}
              disabledReason={selectedProduct ? updatePermission.reason : createPermission.reason}
              isBusy={actionState.isBusy("save-product")}
              onClick={saveProduct}
              type="button"
            >
              {selectedProduct ? (isArabic ? "تحديث السعر" : "Update price") : (isArabic ? "إنشاء المنتج" : "Create product")}
            </ActionButton>
            {selectedProduct ? (
              <ActionButton className="ghost" onClick={resetForm} type="button">
                {t("Clear")}
              </ActionButton>
            ) : null}
          </div>
          <PermissionNote
            allowed={selectedProduct ? updatePermission.allowed : createPermission.allowed}
            reason={selectedProduct ? updatePermission.reason : createPermission.reason}
          />
        </Card>

        <Card title={isArabic ? "المنتجات والتسعير" : "Products and Pricing"}>
          {visibleProducts.length ? (
            <div className="table-wrap">
              <table className="data-table">
                <thead>
                  <tr>
                    <th>{t("ID")}</th>
                    <th>{t("Type")}</th>
                    <th>{t("Unit")}</th>
                    <th>{t("Price")}</th>
                    <th>{isArabic ? "إجراء" : "Action"}</th>
                  </tr>
                </thead>
                <tbody>
                  {visibleProducts.map((product) => (
                    <tr key={product.id} onClick={() => {
                      setSelectedProduct(product);
                      setForm({
                        productName: displayProductName(product),
                        productType: displayProductName(product),
                        inventory: 0,
                        price: product.price,
                        unit: product.unit
                      });
                      setFormErrors({});
                    }}>
                      <td>{product.id}</td>
                      <td>{displayProductName(product)}</td>
                      <td>{product.unit}</td>
                      <td>{formatCurrency(product.price)}</td>
                      <td>
                        <button
                          className="btn ghost danger"
                          disabled={!deletePermission.allowed}
                          title={!deletePermission.allowed ? deletePermission.reason : undefined}
                          onClick={(event) => {
                            event.stopPropagation();
                            void actionState.runAction(`delete-product-${product.id}`, async () => {
                              try {
                                await apiRequest(endpoints.products.byId(product.id), { method: "DELETE" });
                                await loadProducts();
                                if (selectedProduct?.id === product.id) {
                                  resetForm();
                                }
                                setNotice({ tone: "success", message: isArabic ? "تم حذف المنتج." : "Product deleted." });
                              } catch (requestError: unknown) {
                                setNotice({ tone: "danger", message: getApiErrorMessage(requestError) });
                              }
                            });
                          }}
                          type="button"
                        >
                          {actionState.isBusy(`delete-product-${product.id}`) ? (isArabic ? "جار الحذف..." : "Deleting...") : (isArabic ? "حذف" : "Delete")}
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          ) : (
            <EmptyState title={isArabic ? "لا توجد منتجات" : "No products"} description={isArabic ? "أنشئ منتجاً ليظهر في القائمة." : "Create a product to populate the list."} />
          )}
        </Card>
      </div>
    </div>
  );
}
