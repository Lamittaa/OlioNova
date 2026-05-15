import { useEffect, useMemo, useState } from "react";
import { useSearchParams } from "react-router-dom";
import { useI18n } from "../app/i18n-context";
import { endpoints } from "../lib/endpoints";
import { apiRequest } from "../lib/http";
import type { PublicQueueDisplay, QueueType } from "../types/models";

const REFRESH_MS = 2000;
const NEXT_TICKET_COUNT = 6;

const QUEUE_COPY = {
  en: {
    title: "Factory Queue Display",
    nowServing: "Current Production Lines",
    nextInLine: "Next Turns",
    standby: "Please keep your ticket visible",
    today: "Today",
    waiting: "Waiting",
    avgWait: "Average Wait",
    lastUpdated: "Last updated",
    noTicket: "---",
    noQueue: "Queue is currently clear",
    waitInstruction: "Please wait until your number is called",
    instructionProduction: "Please proceed to the production area",
    instructionAccounting: "Please proceed to the cashier",
    production: "Production Turns",
    accounting: "Accounting Queue",
    lineA: "Production Line A",
    lineB: "Production Line B",
    order: "Order",
    remaining: "Remaining",
    live: "Live",
    loadError: "Unable to load queue display",
    minutes: (value: number) => `${value} min`
  },
  ar: {
    title: "شاشة أدوار المصنع",
    nowServing: "خطوط الإنتاج الحالية",
    nextInLine: "الأدوار التالية",
    standby: "يرجى إبقاء التذكرة ظاهرة",
    today: "اليوم",
    waiting: "بانتظار الدور",
    avgWait: "متوسط الانتظار",
    lastUpdated: "آخر تحديث",
    noTicket: "---",
    noQueue: "لا يوجد انتظار حالياً",
    waitInstruction: "يرجى الانتظار حتى يتم النداء على رقمك",
    instructionProduction: "يرجى التوجه إلى منطقة الإنتاج",
    instructionAccounting: "يرجى التوجه إلى المحاسب",
    production: "أدوار الإنتاج",
    accounting: "طابور المحاسبة",
    lineA: "خط الإنتاج A",
    lineB: "خط الإنتاج B",
    order: "الطلب",
    remaining: "المتبقي",
    live: "مباشر",
    loadError: "تعذر تحميل شاشة الأدوار",
    minutes: (value: number) => `${value} دقيقة`
  }
} as const;

function normalizeQueueType(value: string | null): QueueType {
  return value?.toUpperCase() === "ACCOUNTING" ? "ACCOUNTING" : "PRODUCTION";
}

function formatDisplayDate(date: Date, language: "en" | "ar"): string {
  return new Intl.DateTimeFormat(language === "ar" ? "ar" : "en", {
    day: "2-digit",
    month: "short",
    year: "numeric"
  }).format(date);
}

function formatDisplayTime(date: Date, language: "en" | "ar"): string {
  return new Intl.DateTimeFormat(language === "ar" ? "ar" : "en", {
    hour: "2-digit",
    minute: "2-digit"
  }).format(date);
}

function parseDisplayDate(value?: string | null): Date {
  const parsed = value ? new Date(value) : new Date();
  return Number.isNaN(parsed.getTime()) ? new Date() : parsed;
}

function fallbackDisplay(queueType: QueueType): PublicQueueDisplay {
  return {
    queueType,
    queueLabel: queueType === "PRODUCTION" ? "Production Queue" : "Accounting Queue",
    nowServing: null,
    nowServingLines: [],
    nextInLine: [],
    totalWaiting: 0,
    averageWaitMinutes: 0,
    instruction:
      queueType === "PRODUCTION"
        ? "Please proceed to the production area"
        : "Please proceed to the cashier",
    lastUpdated: new Date().toISOString()
  };
}

export function QueueDisplayPage(): JSX.Element {
  const { language, setLanguage } = useI18n();
  const [searchParams] = useSearchParams();
  const queueType = normalizeQueueType(searchParams.get("queue"));
  const requestedLanguage = searchParams.get("lang");
  const copy = QUEUE_COPY[language];
  const [display, setDisplay] = useState<PublicQueueDisplay>(() => fallbackDisplay(queueType));
  const [clock, setClock] = useState(() => new Date());
  const [error, setError] = useState<string | null>(null);

  const nextTickets = useMemo(() => {
    const tickets = display.nextInLine.slice(0, NEXT_TICKET_COUNT).map((ticket, index) => ({
      id: `${ticket.number}-${index}`,
      number: ticket.number,
      orderId: ticket.orderId ?? null,
      estimatedWaitMinutes: ticket.estimatedWaitMinutes ?? null,
      placeholder: false
    }));

    while (tickets.length < NEXT_TICKET_COUNT) {
      tickets.push({
        id: `empty-${tickets.length}`,
        number: copy.noTicket,
        orderId: null,
        estimatedWaitMinutes: null,
        placeholder: true
      });
    }

    return tickets;
  }, [copy.noTicket, display.nextInLine]);

  const currentLines = useMemo(() => {
    const source = display.nowServingLines?.length
      ? display.nowServingLines
      : display.nowServing
        ? [display.nowServing]
        : [];

    const hasAssignedLines = source.some((ticket) => ticket.productionLine);

    return [
      {
        label: copy.lineA,
        tickets: hasAssignedLines ? source.filter((ticket) => ticket.productionLine === "A") : source.slice(0, 1)
      },
      {
        label: copy.lineB,
        tickets: hasAssignedLines ? source.filter((ticket) => ticket.productionLine === "B") : source.slice(1, 2)
      }
    ];
  }, [copy.lineA, copy.lineB, display.nowServing, display.nowServingLines]);

  const queueLabel = display.queueType === "PRODUCTION" ? copy.production : copy.accounting;

  const instruction =
    display.nowServing || display.nowServingLines?.length
      ? display.queueType === "PRODUCTION"
        ? copy.instructionProduction
        : copy.instructionAccounting
      : display.totalWaiting > 0
        ? copy.waitInstruction
        : copy.noQueue;

  const lastUpdatedDate = parseDisplayDate(display.lastUpdated);

  useEffect(() => {
    if (requestedLanguage === "ar" || requestedLanguage === "en") {
      setLanguage(requestedLanguage);
    }
  }, [requestedLanguage, setLanguage]);

  useEffect(() => {
    setDisplay(fallbackDisplay(queueType));
  }, [queueType]);

  useEffect(() => {
    const timer = window.setInterval(() => setClock(new Date()), 1000);
    return () => window.clearInterval(timer);
  }, []);

  useEffect(() => {
    let cancelled = false;

    async function loadDisplay(): Promise<void> {
      try {
        const response = await apiRequest<PublicQueueDisplay>(`${endpoints.queues.display(queueType)}?ts=${Date.now()}`, {
          auth: false,
          headers: {
            "Cache-Control": "no-cache"
          }
        });
        if (!cancelled) {
          setDisplay(response);
          setError(null);
        }
      } catch {
        if (!cancelled) {
          setError(copy.loadError);
        }
      }
    }

    void loadDisplay();
    const timer = window.setInterval(() => {
      void loadDisplay();
    }, REFRESH_MS);

    return () => {
      cancelled = true;
      window.clearInterval(timer);
    };
  }, [copy.loadError, queueType]);

  return (
    <main className={`queue-display-screen queue-display-${display.queueType.toLowerCase()} ${language === "ar" ? "is-rtl" : ""}`}>
      <section className="queue-display-frame" aria-live="polite">
        <header className="queue-display-header">
          <div className="queue-display-brand">
            <img alt="" src="/opslogo.jpeg" />
            <span>{copy.live}</span>
          </div>
          <div className="queue-display-title">
            <h1>{copy.title}</h1>
            <span>{queueLabel}</span>
          </div>
          <div className="queue-display-language" role="group" aria-label="Arabic / English">
            <button className={language === "en" ? "active" : ""} onClick={() => setLanguage("en")} type="button">
              E
            </button>
            <button className={language === "ar" ? "active" : ""} onClick={() => setLanguage("ar")} type="button">
              ع
            </button>
          </div>
        </header>

        <div className="queue-display-board">
          <article className="queue-display-panel now">
            <h2>{copy.nowServing}</h2>
            <div className="queue-display-lines">
              {currentLines.map((line) => {
                const isEmpty = line.tickets.length === 0;

                return (
                  <div className="queue-display-line" key={line.label}>
                    <span>{line.label}</span>
                    {isEmpty ? (
                      <>
                        <strong>{copy.noTicket}</strong>
                        <small>{display.totalWaiting > 0 ? copy.waitInstruction : copy.noQueue}</small>
                      </>
                    ) : (
                      <div className="queue-display-line-orders">
                        {line.tickets.map((ticket, index) => (
                          <div className="queue-display-line-order" key={`${line.label}-${ticket.number}-${index}`}>
                            <strong>{ticket.number}</strong>
                            <small>{ticket.orderId ? `${copy.order} #${ticket.orderId}` : instruction}</small>
                          </div>
                        ))}
                      </div>
                    )}
                  </div>
                );
              })}
            </div>
          </article>

          <article className="queue-display-panel next">
            <h2>{copy.nextInLine}</h2>
            <ul>
              {nextTickets.map((ticket, index) => (
                <li className={ticket.placeholder ? "is-empty" : ""} key={ticket.id}>
                  <span>{String(index + 1).padStart(2, "0")}</span>
                  <div>
                    <strong>{ticket.number}</strong>
                    {!ticket.placeholder ? (
                      <small>
                        {ticket.orderId ? `${copy.order} #${ticket.orderId}` : ""}
                        {ticket.estimatedWaitMinutes !== null ? ` · ${copy.remaining}: ${copy.minutes(ticket.estimatedWaitMinutes)}` : ""}
                      </small>
                    ) : null}
                  </div>
                </li>
              ))}
            </ul>
          </article>
        </div>

        <div className="queue-display-stats">
          <span><small>{copy.waiting}</small><strong>{display.totalWaiting}</strong></span>
          <span><small>{copy.avgWait}</small><strong>{copy.minutes(display.averageWaitMinutes)}</strong></span>
          <span><small>{copy.today}</small><strong>{formatDisplayDate(clock, language)}</strong></span>
          <span><small>{copy.lastUpdated}</small><strong>{formatDisplayTime(lastUpdatedDate, language)}</strong></span>
        </div>

        <div className="queue-display-footer">
          <strong>{formatDisplayTime(clock, language)}</strong>
          <span>{copy.standby}</span>
        </div>

        {error ? <div className="queue-display-error">{error}</div> : null}
      </section>
    </main>
  );
}
