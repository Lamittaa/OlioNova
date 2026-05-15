import { FormEvent, useEffect, useMemo, useState } from "react";
import { useSearchParams } from "react-router-dom";
import { useI18n } from "../app/i18n-context";
import { useToastHelpers } from "../app/toast-context";
import { endpoints } from "../lib/endpoints";
import { publicAsset } from "../lib/assets";
import { apiRequest } from "../lib/http";
import { useActionState } from "../lib/use-action-state";
import { getApiErrorMessage } from "../lib/validation";
import type { BatchTracking, ProductionStage, PublicQueueDisplay, TrackingStatus } from "../types/models";

type PublicTrackCopy = {
  actionsTitle: string;
  actionHistory: string;
  actionNotifications: string;
  actionSupport: string;
  brandLine: string;
  eyebrow: string;
  title: string;
  greeting: string;
  lookupTitle: string;
  lookupLabel: string;
  lookupPlaceholder: string;
  lookupButton: string;
  lookupBusy: string;
  emptyTitle: string;
  emptyText: string;
  enterCode: string;
  orderId: string;
  dated: string;
  stepReceived: string;
  stepReceivedDetail: string;
  stepProcessing: string;
  stepProcessingDetail: string;
  stepPressing: string;
  stepPressingDetail: string;
  stepQuality: string;
  stepQualityDetail: string;
  stepReady: string;
  stepReadyDetail: string;
  orderDetails: string;
  totalWeight: string;
  remaining: string;
  status: string;
  tank: string;
  readyForPickup: string;
  inProgress: string;
  registered: string;
  done: string;
  minutesRemaining: (minutes: number) => string;
  phase: (phaseNumber: number) => string;
  currentTank: (tank: string) => string;
  otherTank: (tank: string) => string;
  visualAlt: string;
  noValue: string;
};

const COPY: Record<"en" | "ar", PublicTrackCopy> = {
  en: {
    actionsTitle: "Quick Actions",
    actionHistory: "Order History",
    actionNotifications: "Notifications",
    actionSupport: "Support",
    brandLine: "Olive batch tracking",
    eyebrow: "OlioNova customer tracking",
    title: "Track Your OlioNova Olive Press Order",
    greeting: "Hello, track your olive processing and pickup status below.",
    lookupTitle: "Find Your Order",
    lookupLabel: "Tracking code",
    lookupPlaceholder: "e.g., OPS7F2K9QA",
    lookupButton: "Track status",
    lookupBusy: "Checking...",
    emptyTitle: "Tracking details will appear here.",
    emptyText: "Use the secure code from reception after your batch is registered.",
    enterCode: "Enter a tracking code.",
    orderId: "Order ID",
    dated: "Dated",
    stepReceived: "Received",
    stepReceivedDetail: "Booking confirmed",
    stepProcessing: "Processing",
    stepProcessingDetail: "Olives are being washed and milled",
    stepPressing: "Pressing & Extraction",
    stepPressingDetail: "Oil extraction in progress",
    stepQuality: "Quality Check & Bottling",
    stepQualityDetail: "Final checks before pickup",
    stepReady: "Ready for Pickup",
    stepReadyDetail: "Oil is ready",
    orderDetails: "Order Details",
    totalWeight: "Total Weight",
    remaining: "Est. Remaining",
    status: "Status",
    tank: "Tank",
    readyForPickup: "Ready for pickup",
    inProgress: "In progress",
    registered: "Registered",
    done: "Done",
    minutesRemaining: (minutes) => `${minutes} min`,
    phase: (phaseNumber) => `Phase ${phaseNumber}`,
    currentTank: (tank) => `Your oil is in Tank ${tank}`,
    otherTank: (tank) => `Tank ${tank}`,
    visualAlt: "Olive oil production at OlioNova",
    noValue: "--"
  },
  ar: {
    actionsTitle: "إجراءات سريعة",
    actionHistory: "سجل الطلبات",
    actionNotifications: "الإشعارات",
    actionSupport: "الدعم",
    brandLine: "تتبع دفعات الزيتون",
    eyebrow: "تتبع عملاء OlioNova",
    title: "تتبع طلب عصر الزيتون في OlioNova",
    greeting: "مرحباً، تابع حالة عصر الزيتون وموعد الاستلام هنا.",
    lookupTitle: "ابحث عن طلبك",
    lookupLabel: "رمز التتبع",
    lookupPlaceholder: "مثال: OPS7F2K9QA",
    lookupButton: "تتبع الحالة",
    lookupBusy: "جارٍ التحقق...",
    emptyTitle: "ستظهر تفاصيل التتبع هنا.",
    emptyText: "استخدم الرمز الآمن الذي تحصل عليه من الاستقبال بعد تسجيل الدفعة.",
    enterCode: "أدخل رمز التتبع.",
    orderId: "رقم الطلب",
    dated: "تاريخ التسجيل",
    stepReceived: "تم الاستلام",
    stepReceivedDetail: "تم تأكيد الحجز",
    stepProcessing: "قيد المعالجة",
    stepProcessingDetail: "غسل وطحن الزيتون",
    stepPressing: "العصر والاستخلاص",
    stepPressingDetail: "استخلاص الزيت جارٍ",
    stepQuality: "فحص الجودة والتعبئة",
    stepQualityDetail: "الفحص الأخير قبل الاستلام",
    stepReady: "جاهز للاستلام",
    stepReadyDetail: "الزيت جاهز",
    orderDetails: "تفاصيل الطلب",
    totalWeight: "الوزن الكلي",
    remaining: "الوقت المتبقي",
    status: "الحالة",
    tank: "الخزان",
    readyForPickup: "جاهز للاستلام",
    inProgress: "قيد التنفيذ",
    registered: "مسجل",
    done: "تم",
    minutesRemaining: (minutes) => `${minutes} دقيقة`,
    phase: (phaseNumber) => `المرحلة ${phaseNumber}`,
    currentTank: (tank) => `زيتك في الخزان ${tank}`,
    otherTank: (tank) => `الخزان ${tank}`,
    visualAlt: "إنتاج زيت الزيتون في OlioNova",
    noValue: "--"
  }
};

const ABOUT_COPY = {
  en: {
    label: "OlioNova",
    cards: [
      {
        title: "About us",
        paragraphs: [
          "We are the OlioNova team, developing smart and modern solutions for managing olive presses in a simpler, more organized, and more efficient way. Our idea began from the need to transform traditional work inside olive presses into an integrated digital system that helps owners and employees follow daily operations accurately and quickly while preserving service quality and organized data. We aim for OlioNova to be a platform that connects technology with the agricultural sector to deliver a more advanced and professional work experience."
        ]
      },
      {
        title: "Our mission",
        paragraphs: [
          "Our mission is to support olive press owners by providing an intelligent system that simplifies order management, production follow-up, inventory organization, accounting, and employee management with high efficiency. We believe technology can improve workflow, reduce errors, and save time and effort, which helps provide better service to farmers and customers."
        ]
      },
      {
        title: "Our vision",
        paragraphs: [
          "We aim for OlioNova to become one of the leading digital transformation solutions for the olive press sector in Palestine and the region, and to help develop this sector through smart systems that combine simplicity, accuracy, and innovation. Our vision is to build a future where olive press management is more efficient, sustainable, and powered by modern technology."
        ]
      },
      {
        title: "What we offer",
        paragraphs: ["OlioNova provides an integrated olive press management system that includes:"],
        items: [
          "Customer order management and production organization.",
          "Tracking olive oil, olives, and inventory quantities.",
          "Accounting, invoices, and payment management.",
          "Employee permissions for admin, accountant, technician, and reception roles.",
          "Reports and statistics that support better decisions.",
          "An easy-to-use interface that speeds up work and reduces errors."
        ],
        closing: "Our goal is to provide a modern practical experience that makes olive press management easier and more professional."
      }
    ]
  },
  ar: {
    label: "OlioNova",
    cards: [
      {
        title: "من نحن",
        paragraphs: [
          "نحن فريق OlioNova، نعمل على تطوير حلول ذكية وحديثة لإدارة معاصر الزيتون بطريقة أكثر سهولة وتنظيماً وكفاءة. انطلقت فكرتنا من الحاجة إلى تحويل العمل التقليدي داخل المعاصر إلى نظام رقمي متكامل يساعد أصحاب المعاصر والموظفين على متابعة العمليات اليومية بدقة وسرعة، مع الحفاظ على جودة الخدمة وتنظيم البيانات. نسعى لأن تكون OlioNova منصة تجمع بين التكنولوجيا والقطاع الزراعي لتقديم تجربة عمل أكثر تطوراً واحترافية."
        ]
      },
      {
        title: "رسالتنا",
        paragraphs: [
          "تتمثل رسالتنا في دعم أصحاب معاصر الزيتون من خلال توفير نظام ذكي يسهّل إدارة الطلبات، متابعة عمليات العصر، تنظيم المخزون، وإدارة الحسابات والموظفين بكفاءة عالية. نؤمن بأن التكنولوجيا قادرة على تحسين سير العمل وتقليل الأخطاء وتوفير الوقت والجهد، مما يساهم في تقديم خدمة أفضل للمزارعين والعملاء."
        ]
      },
      {
        title: "رؤيتنا",
        paragraphs: [
          "نسعى لأن تصبح OlioNova من الحلول الرائدة في التحول الرقمي لقطاع معاصر الزيتون في فلسطين والمنطقة، وأن نساهم في تطوير هذا القطاع من خلال أنظمة ذكية تجمع بين البساطة، الدقة، والابتكار. رؤيتنا هي بناء مستقبل تصبح فيه إدارة المعاصر أكثر كفاءة واستدامة واعتماداً على التكنولوجيا الحديثة."
        ]
      },
      {
        title: "ما نقدمه",
        paragraphs: ["توفر OlioNova نظاماً متكاملاً لإدارة أعمال المعصرة يشمل:"],
        items: [
          "إدارة طلبات العملاء وتنظيم عمليات العصر.",
          "متابعة كميات الزيت والزيتون والمخزون.",
          "إدارة الحسابات والفواتير والمدفوعات.",
          "تحديد صلاحيات الموظفين مثل الأدمن، المحاسب، الفني والاستقبال.",
          "تقارير وإحصائيات تساعد على اتخاذ قرارات أفضل.",
          "واجهة سهلة الاستخدام تساعد على تسريع العمل وتقليل الأخطاء."
        ],
        closing: "هدفنا هو تقديم تجربة عملية حديثة تجعل إدارة المعصرة أكثر سهولة واحترافية."
      }
    ]
  }
} as const;

const DEMO_TRACKING_CODE = "OPSDEMO2026";

function clampPercent(value: number): number {
  return Math.min(Math.max(value, 0), 100);
}

function normalizeTrackingCode(value: string): string {
  return value.trim().toUpperCase();
}

function visualStageIndex(status: TrackingStatus, progress: number): number {
  if (status === "DONE") {
    return 2;
  }
  if (status === "REGISTERED") {
    return 0;
  }
  return 1;
}

function localizedStatus(status: TrackingStatus, copy: PublicTrackCopy): string {
  switch (status) {
    case "REGISTERED":
      return copy.registered;
    case "IN_PROGRESS":
      return copy.inProgress;
    case "DONE":
      return copy.done;
  }
}

function localizedMessage(tracking: BatchTracking, copy: PublicTrackCopy): string {
  switch (tracking.status) {
    case "REGISTERED":
      return copy.stepReceivedDetail;
    case "IN_PROGRESS":
      return copy.minutesRemaining(tracking.estimatedRemainingMinutes);
    case "DONE":
      return copy.currentTank(String(tracking.tankCode));
  }
}

function localizedLookupError(error: unknown, language: "en" | "ar"): string {
  const message = getApiErrorMessage(error);
  if (language === "ar" && message.toLowerCase().includes("tracking code")) {
    return "لم يتم العثور على رمز التتبع.";
  }

  return message;
}

function formatDate(value: string | null | undefined, language: "en" | "ar"): string {
  if (!value) {
    return COPY[language].noValue;
  }

  return new Intl.DateTimeFormat(language === "ar" ? "ar" : "en", {
    day: "numeric",
    month: "short",
    year: "numeric"
  }).format(new Date(value));
}

function formatWeight(value: number | null | undefined, language: "en" | "ar", fallback: string): string {
  if (value === null || value === undefined || Number.isNaN(value)) {
    return fallback;
  }

  const unit = language === "ar" ? "كغ" : "kg";
  return `${new Intl.NumberFormat(language === "ar" ? "ar" : "en", {
    maximumFractionDigits: 2
  }).format(value)} ${unit}`;
}

function getAutoRefreshLabel(language: "en" | "ar", enabled: boolean): string {
  if (language === "ar") {
    return enabled ? "تحديث تلقائي: تشغيل" : "تحديث تلقائي: إيقاف";
  }

  return enabled ? "Auto refresh: ON" : "Auto refresh: OFF";
}

function formatUpdatedTime(value: string | null | undefined, language: "en" | "ar", fallback: string): string {
  if (!value) {
    return fallback;
  }

  const locale = language === "ar" ? "ar" : "en";
  return new Intl.DateTimeFormat(locale, { hour: "2-digit", minute: "2-digit" }).format(new Date(value));
}

function createDemoTracking(): BatchTracking {
  return {
    batchId: "DEMO-BATCH-2026",
    trackingCode: DEMO_TRACKING_CODE,
    orderId: 897321,
    orderItemId: 3021,
    oliveWeightKg: 1450,
    predictedOilKg: 285,
    status: "IN_PROGRESS",
    statusLabel: "In progress",
    progressPercent: 58,
    estimatedTotalMinutes: 90,
    estimatedRemainingMinutes: 38,
    tankCode: "B",
    tankLabel: "Tank B",
    productionLine: "A",
    friendlyMessage: "Your oil is moving through production. About 38 minutes remaining.",
    tanks: [
      { code: "A", label: "Tank A", current: false },
      { code: "B", label: "Tank B", current: true },
      { code: "C", label: "Tank C", current: false },
      { code: "D", label: "Tank D", current: false }
    ],
    registeredAt: "2026-05-04T09:00:00",
    startedAt: "2026-05-04T09:12:00",
    estimatedDoneAt: "2026-05-04T10:42:00",
    completedAt: null,
    updatedAt: "2026-05-04T09:35:00"
  };
}

export function CustomerTrackingPage(): JSX.Element {
  const { language, setLanguage } = useI18n();
  const { error: toastError } = useToastHelpers();
  const [searchParams, setSearchParams] = useSearchParams();
  const initialCode = searchParams.get("code") ?? "";
  const [trackingCode, setTrackingCode] = useState(initialCode);
  const [tracking, setTracking] = useState<BatchTracking | null>(null);
  const [lineLabel, setLineLabel] = useState<string>("--");
  const [autoRefresh, setAutoRefresh] = useState(true);
  const actionState = useActionState();
  const copy = COPY[language];
  const aboutCopy = ABOUT_COPY[language];
  const autoRefreshLabel = getAutoRefreshLabel(language, autoRefresh);
  const updatedAtLabel = formatUpdatedTime(tracking?.updatedAt, language, copy.noValue);

  const progressPercent = tracking ? clampPercent(tracking.progressPercent) : 0;
  const activeStageIndex = useMemo(
    () => (tracking ? visualStageIndex(tracking.status, progressPercent) : 0),
    [progressPercent, tracking]
  );
  const stages = [
    { label: language === "ar" ? "تم الاستلام" : "Received", detail: copy.stepReceivedDetail },
    { label: language === "ar" ? "قيد التنفيذ" : "In progress", detail: copy.stepProcessingDetail },
    { label: language === "ar" ? "تعبئة وانتهاء" : "Filling and done", detail: copy.stepReadyDetail }
  ];

  const resolveLineAndStage = async (orderItemId: number | null | undefined): Promise<void> => {
    if (!orderItemId) {
      setLineLabel(copy.noValue);
      return;
    }

    try {
      const stages = await apiRequest<ProductionStage[]>(endpoints.production.orderStages(orderItemId), { auth: false });
      const activeStage = stages.find((item) => item.currentStatus.toUpperCase().includes("IN_PROGRESS")) ?? stages[0];
      setLineLabel(activeStage?.line ?? copy.noValue);
    } catch {
      setLineLabel(copy.noValue);
    }
  };

  const resolveLineFromQueueDisplay = async (orderId: number | null | undefined): Promise<string | null> => {
    if (!orderId) {
      return null;
    }

    try {
      const display = await apiRequest<PublicQueueDisplay>(endpoints.queues.display("PRODUCTION"), { auth: false });
      const publicTickets = [
        ...(display.nowServingLines ?? []),
        ...(display.nowServing ? [display.nowServing] : []),
        ...display.nextInLine
      ];
      const matchingTicket = publicTickets.find((ticket) => Number(ticket.orderId) === Number(orderId));
      return matchingTicket?.productionLine ?? null;
    } catch {
      return null;
    }
  };

  const fetchTracking = async (normalizedCode: string, shouldSyncQuery: boolean): Promise<void> => {
    const response = await apiRequest<BatchTracking>(endpoints.tracking.publicByCode(normalizedCode), {
      auth: false
    });
    setTracking(response);
    if (response.productionLine) {
      setLineLabel(response.productionLine);
    } else {
      const queueLine = await resolveLineFromQueueDisplay(response.orderId ?? null);
      if (queueLine) {
        setLineLabel(queueLine);
      } else {
        await resolveLineAndStage(response.orderItemId ?? null);
      }
    }

    if (shouldSyncQuery) {
      setSearchParams({ code: response.trackingCode ?? normalizedCode });
    }
  };

  const lookupTracking = (code = trackingCode): void => {
    const normalizedCode = normalizeTrackingCode(code);
    setTrackingCode(normalizedCode);

    if (!normalizedCode) {
      setTracking(null);
      toastError(copy.enterCode);
      return;
    }

    if (import.meta.env.DEV && normalizedCode === DEMO_TRACKING_CODE) {
      const demoTracking = createDemoTracking();
      setTracking(demoTracking);
      setLineLabel("A");
      setSearchParams({ code: DEMO_TRACKING_CODE });
      return;
    }

    void actionState.runAction("public-tracking-lookup", async () => {
      try {
        await fetchTracking(normalizedCode, true);
      } catch (requestError: unknown) {
        setTracking(null);
        setLineLabel(copy.noValue);
        toastError(localizedLookupError(requestError, language));
      }
    });
  };

  useEffect(() => {
    if (initialCode) {
      lookupTracking(initialCode);
    }
  }, []);

  useEffect(() => {
    if (!autoRefresh || !tracking) {
      return;
    }

    const code = tracking.trackingCode ?? trackingCode;
    if (!code) {
      return;
    }

    const intervalId = globalThis.setInterval(() => {
      const normalizedCode = normalizeTrackingCode(code);
      void fetchTracking(normalizedCode, false).catch(() => undefined);
    }, 15000);

    return () => globalThis.clearInterval(intervalId);
  }, [autoRefresh, tracking?.batchId, tracking?.trackingCode, trackingCode]);

  return (
    <main className={`customer-track-page ${language === "ar" ? "is-rtl" : ""}`}>
      <section className="customer-track-dashboard">
        <header className="customer-track-topbar">
          <div>
            <span className="eyebrow">{copy.eyebrow}</span>
            <h1>{copy.title}</h1>
            <p>{copy.greeting}</p>
          </div>

          <div className="customer-track-top-actions">
            <button className={`btn ghost ${autoRefresh ? "active" : ""}`} onClick={() => setAutoRefresh((current) => !current)} type="button">
              {autoRefreshLabel}
            </button>
            <div className="language-toggle" aria-label="Arabic / English">
              <button
                className={language === "en" ? "active" : ""}
                onClick={() => setLanguage("en")}
                type="button"
              >
                E
              </button>
              <button
                className={language === "ar" ? "active" : ""}
                onClick={() => setLanguage("ar")}
                type="button"
              >
                ع
              </button>
            </div>
          </div>
        </header>

        <form
          className="customer-track-search"
          onSubmit={(event: FormEvent<HTMLFormElement>) => {
            event.preventDefault();
            lookupTracking();
          }}
        >
          <h2>{copy.lookupTitle}</h2>
          <label htmlFor="public-tracking-code">{copy.lookupLabel}</label>
          <div className="customer-track-search-row">
            <input
              autoComplete="off"
              id="public-tracking-code"
              onChange={(event) => setTrackingCode(event.target.value)}
              placeholder={copy.lookupPlaceholder}
              value={trackingCode}
            />
            <button className="btn" disabled={actionState.isBusy("public-tracking-lookup")} type="submit">
              {actionState.isBusy("public-tracking-lookup") ? copy.lookupBusy : copy.lookupButton}
            </button>
          </div>
        </form>

        {tracking ? (
          <section className="customer-track-order-card" aria-live="polite">
            <div className="customer-track-order-main">
              <div className="customer-track-order-head">
                <div>
                  <h2>
                    {copy.orderId}: #{tracking.orderId ?? tracking.trackingCode ?? tracking.batchId}
                  </h2>
                  <p>
                    {copy.dated}: {formatDate(tracking.registeredAt, language)}
                  </p>
                </div>
                <strong className="customer-track-code">{tracking.trackingCode ?? tracking.batchId}</strong>
              </div>

              <div className={`customer-track-stage-row stage-${activeStageIndex}`}>
                {stages.map((stage, index) => (
                  <div
                    className={`customer-track-stage ${index < activeStageIndex ? "complete" : ""} ${
                      index === activeStageIndex ? "active" : ""
                    }`}
                    key={stage.label}
                  >
                    <span className="customer-track-stage-dot">{index < activeStageIndex ? "✓" : index + 1}</span>
                    <strong>{stage.label}</strong>
                    <small>{stage.detail}</small>
                  </div>
                ))}
              </div>

              <div className="customer-track-detail-title">{copy.orderDetails}</div>
              <div className="customer-track-metrics">
                <div>
                  <span aria-hidden="true">kg</span>
                  <small>{copy.totalWeight}</small>
                  <strong>{formatWeight(tracking.oliveWeightKg, language, copy.noValue)}</strong>
                </div>
                <div>
                  <span aria-hidden="true">m</span>
                  <small>{copy.remaining}</small>
                  <strong>
                    {tracking.status === "DONE"
                      ? copy.readyForPickup
                      : copy.minutesRemaining(tracking.estimatedRemainingMinutes)}
                  </strong>
                </div>
                <div>
                  <span aria-hidden="true">i</span>
                  <small>{copy.status}</small>
                  <strong>{localizedStatus(tracking.status, copy)}</strong>
                </div>
                <div>
                  <span aria-hidden="true">L</span>
                  <small>{language === "ar" ? "الخط" : "Line"}</small>
                  <strong>{lineLabel}</strong>
                </div>
                <div>
                  <span aria-hidden="true">T</span>
                  <small>{copy.tank}</small>
                  <strong>{tracking.tankLabel}</strong>
                </div>
                <div>
                  <span aria-hidden="true">⟳</span>
                  <small>{language === "ar" ? "آخر تحديث" : "Updated"}</small>
                  <strong>{updatedAtLabel}</strong>
                </div>
              </div>
            </div>

            <div className="customer-track-tank-row">
              {tracking.tanks.map((tank) => (
                <div className={`customer-track-tank-tile ${tank.current ? "active" : ""}`} key={tank.code}>
                  <strong>{tank.code}</strong>
                  <span>{tank.current ? copy.currentTank(String(tank.code)) : copy.otherTank(String(tank.code))}</span>
                </div>
              ))}
            </div>
          </section>
        ) : (
          <section className="customer-track-empty-state">
            <img alt="" src={publicAsset("app-icon.png")} />
            <div>
              <strong>{copy.emptyTitle}</strong>
              <span>{copy.emptyText}</span>
            </div>
          </section>
        )}

        <section className="customer-track-about" aria-labelledby="customer-track-about-title">
          <div className="customer-track-about-heading">
            <img alt="" src={publicAsset("app-icon.png")} />
            <span>{aboutCopy.label}</span>
          </div>

          <div className="customer-track-about-stack">
            {aboutCopy.cards.map((card, index) => (
              <article key={card.title}>
                <h2 id={index === 0 ? "customer-track-about-title" : undefined}>{card.title}</h2>
                {card.paragraphs.map((paragraph) => (
                  <p key={paragraph}>{paragraph}</p>
                ))}
                {"items" in card ? (
                  <ul>
                    {card.items.map((item) => (
                      <li key={item}>{item}</li>
                    ))}
                  </ul>
                ) : null}
                {"closing" in card ? <p>{card.closing}</p> : null}
              </article>
            ))}
          </div>
        </section>
      </section>
    </main>
  );
}
