import { useEffect, useMemo, useState } from "react";
import { useAuth } from "../app/auth-context";
import { useI18n } from "../app/i18n-context";
import { useToastHelpers } from "../app/toast-context";
import { canAction } from "../app/policy";
import { CameraCaptureInput } from "../components/CameraCaptureInput";
import { ActionButton, ErrorSummary, FieldError, PermissionNote } from "../components/form-ui";
import { Banner, Card, PageHeader, StatusBadge } from "../components/ui";
import { endpoints } from "../lib/endpoints";
import { apiRequest } from "../lib/http";
import { fetchProductionDashboard } from "../lib/production-api";
import { useActionState } from "../lib/use-action-state";
import { asErrorMessage, formatNumber } from "../lib/utils";
import { getApiErrorMessage, getApiFieldErrors, type FieldErrors, validatePositiveId } from "../lib/validation";
import type {
  AiPredictionResponse,
  BatchTracking,
  ProductionBatch,
  ProductionDashboardItem,
  ProductionEta,
  ProductionStage,
  QueueStatus,
  QueueTicket
} from "../types/models";

type ToastNotice = { tone: "success" | "danger" | "neutral"; message: string };
type ToastNoticeValue = ToastNotice | null;

let initialProductionLoadPromise: Promise<void> | null = null;

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

function toOilLiters(oilKg: number | null): number | null {
  if (oilKg === null || Number.isNaN(oilKg)) {
    return null;
  }

  return oilKg / 0.915;
}

function formatLiters(value: number | null, language: "ar" | "en"): string {
  if (value === null || Number.isNaN(value)) {
    return "--";
  }

  return new Intl.NumberFormat(language === "ar" ? "ar" : "en", {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
  }).format(value);
}

function formatProductionTicketNumber(number?: string | null): string {
  if (!number) {
    return "--";
  }

  const trimmed = number.trim();
  return trimmed.toUpperCase().startsWith("P") ? trimmed : `P-${trimmed}`;
}

function minutesSince(value?: string | null): number | null {
  if (!value) {
    return null;
  }

  const startedAt = new Date(value).getTime();
  if (Number.isNaN(startedAt)) {
    return null;
  }

  return Math.max(0, Math.round((Date.now() - startedAt) / 60000));
}

function getLineToneClass(line: string): string {
  const normalized = line.trim().toUpperCase();
  if (normalized.startsWith("A")) {
    return "line-a";
  }
  if (normalized.startsWith("B")) {
    return "line-b";
  }
  if (normalized.startsWith("C")) {
    return "line-c";
  }
  if (normalized.startsWith("D")) {
    return "line-d";
  }
  return "line-default";
}

export function ProductionPage(): JSX.Element {
  const auth = useAuth();
  const { language, t } = useI18n();
  const { success, error, neutral } = useToastHelpers();
  const { notice, setNotice } = useNotice();
  const actionState = useActionState();
  const isArabic = language === "ar";

  const [dashboard, setDashboard] = useState<ProductionDashboardItem[]>([]);
  const [eta, setEta] = useState<ProductionEta | null>(null);

  const [startOrderId, setStartOrderId] = useState("");
  const [startOrderItemId, setStartOrderItemId] = useState("");
  const [stageItemId, setStageItemId] = useState("");

  const [predictionOrderId, setPredictionOrderId] = useState("");
  const [selectedBatch, setSelectedBatch] = useState<ProductionBatch | null>(null);
  const [recentBatches, setRecentBatches] = useState<ProductionBatch[]>([]);
  const [predictionErrors, setPredictionErrors] = useState<FieldErrors>({});
  const [trackingOrderId, setTrackingOrderId] = useState("");
  const [tracking, setTracking] = useState<BatchTracking | null>(null);
  const [trackedOrderStages, setTrackedOrderStages] = useState<ProductionStage[]>([]);
  const [productionQueue, setProductionQueue] = useState<QueueStatus | null>(null);
  const [stationReady, setStationReady] = useState(false);
  const [finishTankDialogOpen, setFinishTankDialogOpen] = useState(false);

  const [predictFile, setPredictFile] = useState<File | null>(null);
  const [predictedYieldPercent, setPredictedYieldPercent] = useState<number | null>(null);
  const [predictedOilKg, setPredictedOilKg] = useState<number | null>(null);

  const startPermission = canAction(auth, "PRODUCTION_START");
  const etaPermission = canAction(auth, "ETA_VIEW");
  const dashboardPermission = canAction(auth, "PRODUCTION_DASHBOARD");
  const queueAdvancePermission = canAction(auth, "QUEUE_ADVANCE");
  const predictionPermission = canAction(auth, "STAGE_START").allowed;
  const currentProductionTicket = productionQueue?.serving[0]?.ticket ?? null;
  const currentServingStartedAt = productionQueue?.serving[0]?.startedAt ?? null;
  const nextProductionTicket = productionQueue?.waiting[0] ?? null;
  const waitingProductionTickets = productionQueue?.waiting ?? [];
  const visibleProductionOrderId = currentProductionTicket?.orderId ?? nextProductionTicket?.orderId ?? null;
  const servingMinutes = minutesSince(currentServingStartedAt);

  const selectedBatchSummary = selectedBatch
    ? `${isArabic ? "عنصر الطلب" : "Item"} #${selectedBatch.orderItemId} - ${formatNumber(selectedBatch.oliveWeightKg)} ${isArabic ? "كغ" : "kg"}`
    : isArabic ? "لم يتم اختيار أي عنصر بعد" : "No item selected";
  const predictedOilLiters = toOilLiters(predictedOilKg);
  const selectedBatchOilLiters = toOilLiters(selectedBatch?.predictedOilKg ?? null);
  const currentTrackedStage = useMemo(() => {
    if (!trackedOrderStages.length) {
      return null;
    }

    return trackedOrderStages.find((stage) => stage.currentStatus.toUpperCase().includes("IN_PROGRESS")) ?? trackedOrderStages[0];
  }, [trackedOrderStages]);
  const trackedLineLabel = currentTrackedStage?.line ?? (tracking?.status === "DONE" ? (isArabic ? "منجز" : "Completed") : "--");
  const canCallNext = queueAdvancePermission.allowed && stationReady;
  const canSetInProgress = Boolean(tracking && tracking.status === "REGISTERED");
  const canMarkDone = Boolean(tracking && tracking.status === "IN_PROGRESS");
  const canAssignTank = Boolean(tracking && tracking.status === "IN_PROGRESS");
  const canRunStartProduction = Boolean(startPermission.allowed && tracking && tracking.status === "IN_PROGRESS");

  const applySelectedBatch = (batch: ProductionBatch): void => {
    setSelectedBatch(batch);
    setPredictionOrderId(batch.orderId ? String(batch.orderId) : "");
    setStartOrderId(batch.orderId ? String(batch.orderId) : "");
    setStartOrderItemId(String(batch.orderItemId));
    setStageItemId(String(batch.orderItemId));
    setPredictedYieldPercent(batch.predictedYieldPercent ?? null);
    setPredictedOilKg(batch.predictedOilKg ?? null);
    if (batch.orderId) {
      setTrackingOrderId(String(batch.orderId));
    }
  };

  const loadProductionQueue = async (): Promise<void> => {
    try {
      setProductionQueue(await apiRequest<QueueStatus>(endpoints.queues.status("PRODUCTION")));
    } catch {
      setProductionQueue(null);
    }
  };

  const loadTrackedOrderStages = async (orderItemId: number | null | undefined): Promise<void> => {
    if (!orderItemId) {
      setTrackedOrderStages([]);
      return;
    }

    try {
      const stages = await apiRequest<ProductionStage[]>(endpoints.production.orderStages(orderItemId));
      setTrackedOrderStages(stages);
    } catch {
      setTrackedOrderStages([]);
    }
  };

  const loadTracking = (orderId = trackingOrderId): void => {
    const trimmedOrderId = orderId.trim();
    if (!trimmedOrderId) {
      setNotice({
        tone: "danger",
        message: isArabic ? "أدخل رقم الطلب لعرض التتبع." : "Enter an order ID to view tracking."
      });
      neutral(isArabic ? "أدخل رقم الطلب" : "Enter order ID", isArabic ? "ثم حاول مرة أخرى." : "Then try again.");
      return;
    }

    void actionState.runAction("load-tracking", async () => {
      try {
        const response = await apiRequest<BatchTracking>(endpoints.tracking.byOrder(trimmedOrderId));
        setTracking(response);
        if (response.orderItemId) {
          setStartOrderItemId(String(response.orderItemId));
          setStageItemId(String(response.orderItemId));
          void loadTrackedOrderStages(response.orderItemId);
        } else {
          setTrackedOrderStages([]);
        }
      } catch (requestError: unknown) {
        const message = getApiErrorMessage(requestError);
        setNotice({ tone: "danger", message });
        error(isArabic ? "فشل تحميل التتبع" : "Tracking load failed", message);
      }
    });
  };

  const updateTrackingStatus = (status: BatchTracking["status"]): void => {
    if (!tracking) {
      return;
    }

    void actionState.runAction(`tracking-status-${status}`, async () => {
      try {
        const updatedTracking = await apiRequest<BatchTracking>(endpoints.tracking.status(tracking.batchId), {
          method: "PUT",
          body: { status }
        });

        const visibleTrackingOrderId = Number(trackingOrderId);
        const completedOrderId =
          updatedTracking.orderId
          ?? tracking.orderId
          ?? (Number.isFinite(visibleTrackingOrderId) && visibleTrackingOrderId > 0 ? visibleTrackingOrderId : null);
        if (status === "DONE" && completedOrderId && currentProductionTicket?.orderId === completedOrderId) {
          await apiRequest(endpoints.queues.ticketStatusByOrder(completedOrderId, "PRODUCTION", "COMPLETED"), {
            method: "PUT"
          });
          await loadProductionQueue();
        }

        setTracking(updatedTracking);
        await loadTrackedOrderStages(updatedTracking.orderItemId ?? null);
      } catch (requestError: unknown) {
        const message = getApiErrorMessage(requestError);
        setNotice({ tone: "danger", message });
        error(isArabic ? "فشل تحديث الحالة" : "Status update failed", message);
      }
    });
  };

  const updateTrackingTank = (tankCode: string): void => {
    if (!tracking) {
      return;
    }

    void actionState.runAction(`tracking-tank-${tankCode}`, async () => {
      try {
        const updatedTracking = await apiRequest<BatchTracking>(endpoints.tracking.tank(tracking.batchId), {
          method: "PUT",
          body: { tankCode }
        });
        setTracking(updatedTracking);
      } catch (requestError: unknown) {
        const message = getApiErrorMessage(requestError);
        setNotice({ tone: "danger", message });
        error(isArabic ? "فشل تحديث الخزان" : "Tank update failed", message);
      }
    });
  };

  const finishTrackingWithTank = (tankCode: string): void => {
    if (!tracking) {
      return;
    }

    void actionState.runAction(`tracking-finish-${tankCode}`, async () => {
      try {
        let updatedTracking = await apiRequest<BatchTracking>(endpoints.tracking.tank(tracking.batchId), {
          method: "PUT",
          body: { tankCode }
        });

        updatedTracking = await apiRequest<BatchTracking>(endpoints.tracking.status(updatedTracking.batchId), {
          method: "PUT",
          body: { status: "DONE" }
        });

        const visibleTrackingOrderId = Number(trackingOrderId);
        const completedOrderId =
          updatedTracking.orderId
          ?? tracking.orderId
          ?? (Number.isFinite(visibleTrackingOrderId) && visibleTrackingOrderId > 0 ? visibleTrackingOrderId : null);
        if (completedOrderId && currentProductionTicket?.orderId === completedOrderId) {
          await apiRequest(endpoints.queues.ticketStatusByOrder(completedOrderId, "PRODUCTION", "COMPLETED"), {
            method: "PUT"
          });
          await loadProductionQueue();
        }

        setTracking(updatedTracking);
        setFinishTankDialogOpen(false);
        await loadTrackedOrderStages(updatedTracking.orderItemId ?? null);
        success(isArabic ? `تم إنهاء الطلب وتعبئة الزيت في الخزان ${tankCode}.` : `Finished and assigned Tank ${tankCode}.`);
      } catch (requestError: unknown) {
        const message = getApiErrorMessage(requestError);
        setNotice({ tone: "danger", message });
        error(isArabic ? "فشل إنهاء الطلب" : "Finish failed", message);
      }
    });
  };

  const setInProgressWithTank = (tankCode: string): void => {
    if (!tracking) {
      return;
    }

    void actionState.runAction(`tracking-preset-${tankCode}`, async () => {
      try {
        let updatedTracking = tracking;

        if (updatedTracking.status !== "IN_PROGRESS") {
          updatedTracking = await apiRequest<BatchTracking>(endpoints.tracking.status(updatedTracking.batchId), {
            method: "PUT",
            body: { status: "IN_PROGRESS" }
          });
        }

        updatedTracking = await apiRequest<BatchTracking>(endpoints.tracking.tank(updatedTracking.batchId), {
          method: "PUT",
          body: { tankCode }
        });

        setTracking(updatedTracking);
        await loadTrackedOrderStages(updatedTracking.orderItemId ?? null);
        success(
          isArabic ? `تم نقل الطلب إلى قيد التنفيذ وتعيين الخزان ${tankCode}.` : `Set to in progress and assigned tank ${tankCode}.`
        );
      } catch (requestError: unknown) {
        const message = getApiErrorMessage(requestError);
        setNotice({ tone: "danger", message });
        error(isArabic ? "فشل تطبيق الإعداد السريع" : "Quick preset failed", message);
      }
    });
  };

  const loadPredictionWorkspace = async (): Promise<void> => {
    if (!predictionPermission) {
      return;
    }

    try {
      setRecentBatches(await apiRequest<ProductionBatch[]>(endpoints.production.batches.list));
    } catch {
      setRecentBatches([]);
    }
  };

  const loadPage = async (): Promise<void> => {
    await loadProductionQueue();
    if (dashboardPermission.allowed) {
      setDashboard(await fetchProductionDashboard());
    }
    await loadPredictionWorkspace();
  };

  const loadInitialPage = (): Promise<void> => {
    if (!initialProductionLoadPromise) {
      initialProductionLoadPromise = loadPage().catch((requestError: unknown) => {
        initialProductionLoadPromise = null;
        throw requestError;
      });
    }

    return initialProductionLoadPromise;
  };

  useEffect(() => {
    void loadInitialPage().catch((requestError: unknown) => {
      setNotice({ tone: "danger", message: asErrorMessage(requestError) });
    });

    const timer = window.setInterval(() => {
      void loadProductionQueue();
    }, 20000);

    return () => window.clearInterval(timer);
  }, []);

  useEffect(() => {
    if (!trackingOrderId.trim()) {
      return;
    }

    const intervalId = window.setInterval(() => {
      loadTracking(trackingOrderId);
    }, 15000);

    return () => window.clearInterval(intervalId);
  }, [trackingOrderId, tracking?.batchId]);

  const createBatch = (): void => {
    const nextErrors = validatePositiveId("predictionOrderId", predictionOrderId);
    setPredictionErrors(nextErrors);
    if (Object.keys(nextErrors).length) {
      return;
    }

    void actionState.runAction("create-production-batch", async () => {
      try {
        const response = await apiRequest<ProductionBatch>(endpoints.production.batches.byOrder(predictionOrderId.trim()), {
          method: "POST"
        });

        applySelectedBatch(response);
        setPredictionErrors({});
        await loadPredictionWorkspace();
        setNotice({
          tone: "success",
          message: isArabic
            ? `تم تجهيز الدفعة ${response.batchId} ويمكنك الآن رفع الصورة مباشرة.`
            : `Batch ${response.batchId} is ready. You can upload the image now.`
        });
        success(
          isArabic ? `تم تجهيز الدفعة ${response.batchId}` : `Batch ${response.batchId} is ready`,
          isArabic ? "يمكنك الآن رفع الصورة مباشرة." : "You can upload the image now."
        );
      } catch (requestError: unknown) {
        setPredictionErrors(getApiFieldErrors(requestError));
        const message = getApiErrorMessage(requestError);
        setNotice({ tone: "danger", message });
        error(isArabic ? "فشل تجهيز الدفعة" : "Batch preparation failed", message);
      }
    });
  };

  const loadBatchByOrderItem = (): void => {
    const nextErrors = validatePositiveId("predictionOrderId", predictionOrderId);
    setPredictionErrors(nextErrors);
    if (Object.keys(nextErrors).length) {
      return;
    }

    void actionState.runAction("load-batch-by-order", async () => {
      try {
        const response = await apiRequest<ProductionBatch>(endpoints.production.batches.byOrder(predictionOrderId.trim()), {
          method: "POST"
        });
        applySelectedBatch(response);
        setPredictionErrors({});
        await loadPredictionWorkspace();
        setNotice({
          tone: "success",
          message: isArabic
            ? `تم تحميل الدفعة ${response.batchId} لهذا الطلب.`
            : `Loaded batch ${response.batchId} for this order.`
        });
        success(
          isArabic ? `تم تحميل الدفعة ${response.batchId}` : `Loaded batch ${response.batchId}`,
          isArabic ? "تم ربطها بالطلب الحالي." : "It is now linked to the current order."
        );
      } catch (requestError: unknown) {
        setPredictionErrors(getApiFieldErrors(requestError));
        const message = getApiErrorMessage(requestError);
        setNotice({ tone: "danger", message });
        error(isArabic ? "فشل تحميل الدفعة" : "Batch load failed", message);
      }
    });
  };

  const runPrediction = (): void => {
    const nextErrors: FieldErrors = {};

    if (!selectedBatch) {
      nextErrors.selectedBatch = isArabic ? "أنشئ أو حمّل دفعة أولاً." : "Create or load a batch first.";
    }
    if (!predictFile || predictFile.size === 0) {
      nextErrors.predictFile = isArabic ? "التقط أو اختر صورة واضحة أولاً." : "Capture or choose a clear image first.";
    }

    setPredictionErrors(nextErrors);
    if (Object.keys(nextErrors).length) {
      return;
    }

    void actionState.runAction("technician-ai-predict-v2", async () => {
      try {
        const body = new FormData();
        body.append("file", predictFile as File);
        body.append("orderId", predictionOrderId.trim());

        const response = await apiRequest<AiPredictionResponse>(endpoints.ai.predict, {
          method: "POST",
          body
        });

        const refreshedBatch = await apiRequest<ProductionBatch>(
          endpoints.production.batches.byId(response.batchId)
        );

        applySelectedBatch(refreshedBatch);
        setPredictedYieldPercent(response.predictedYieldPercent ?? null);
        setPredictedOilKg(response.predictedOilKg ?? refreshedBatch.predictedOilKg ?? null);
        setPredictionErrors({});
        await loadPredictionWorkspace();
        setNotice({
          tone: "success",
          message: isArabic
            ? "تم تنفيذ التنبؤ وربطه بالدفعة بنجاح."
            : "Prediction completed and linked to the production batch."
        });
        success(
          isArabic ? "تم تنفيذ التنبؤ بنجاح" : "Prediction completed successfully",
          isArabic ? "تم ربط النتيجة بالدفعة." : "The result is linked to the batch."
        );
      } catch (requestError: unknown) {
        setPredictionErrors(getApiFieldErrors(requestError));
        const message = getApiErrorMessage(requestError);
        setNotice({ tone: "danger", message });
        error(isArabic ? "فشل التنبؤ" : "Prediction failed", message);
      }
    });
  };

  const loadEta = (): void => {
    if (!stageItemId.trim()) {
      setNotice({
        tone: "danger",
        message: isArabic ? "أدخل رقم عنصر الطلب أولاً." : "Enter an order item ID first."
      });
      neutral(isArabic ? "أدخل رقم عنصر الطلب" : "Enter order item ID");
      return;
    }

    void actionState.runAction("load-eta", async () => {
      try {
        setEta(await apiRequest<ProductionEta>(endpoints.production.eta(stageItemId.trim())));
      } catch (requestError: unknown) {
        const message = getApiErrorMessage(requestError);
        setNotice({ tone: "danger", message });
        error(isArabic ? "فشل تحميل الوقت المتوقع" : "ETA load failed", message);
      }
    });
  };

  const startProduction = (): void => {
    if (!startOrderId.trim() || !startOrderItemId.trim()) {
      setNotice({
        tone: "danger",
        message: isArabic ? "أدخل رقم الطلب ورقم عنصر الطلب." : "Enter both order ID and order item ID."
      });
      neutral(isArabic ? "أدخل رقم الطلب ورقم العنصر" : "Enter order and item IDs");
      return;
    }

    void actionState.runAction("start-production", async () => {
      try {
        await apiRequest(endpoints.production.start, {
          method: "POST",
          body: {
            orderId: Number(startOrderId),
            orderItemId: Number(startOrderItemId)
          }
        });
        await loadPage();
        setNotice({
          tone: "success",
          message: isArabic
            ? `تم بدء الإنتاج للعنصر ${startOrderItemId}.`
            : `Production started for item ${startOrderItemId}.`
        });
        success(
          isArabic ? `تم بدء الإنتاج للعنصر ${startOrderItemId}` : `Production started for item ${startOrderItemId}`
        );
      } catch (requestError: unknown) {
        const message = getApiErrorMessage(requestError);
        setNotice({ tone: "danger", message });
        error(isArabic ? "فشل بدء الإنتاج" : "Start production failed", message);
      }
    });
  };

  const openProductionStation = (): void => {
    void actionState.runAction("production-station-login", async () => {
      try {
        await apiRequest(endpoints.queues.tellerLogin("PRODUCTION"), {
          method: "POST",
          responseType: "text"
        });
        await loadProductionQueue();
        setStationReady(true);
        success(
          isArabic ? "تم فتح محطة الإنتاج" : "Production station opened",
          isArabic ? "المحطة جاهزة الآن." : "The station is ready now."
        );
      } catch (requestError: unknown) {
        setStationReady(false);
        const message = getApiErrorMessage(requestError);
        error(isArabic ? "فشل فتح محطة الإنتاج" : "Open station failed", message);
      }
    });
  };

  const callNextProductionOrder = (): void => {
    if (!nextProductionTicket?.number) {
      neutral(
        isArabic ? "لا يوجد طلب بانتظار الإنتاج" : "No production orders are waiting",
        isArabic ? "الطابور فارغ حالياً." : "The production queue is empty right now."
      );
      return;
    }

    void actionState.runAction("production-call-next", async () => {
      try {
        const ticket = await apiRequest<QueueTicket>(endpoints.queues.advance("PRODUCTION", "NEXT"), {
          method: "POST"
        });

        await loadProductionQueue();
        if (ticket.orderId) {
          const nextOrderId = String(ticket.orderId);
          setTrackingOrderId(nextOrderId);
          setStartOrderId(nextOrderId);

          const trackingResponse = await apiRequest<BatchTracking>(endpoints.tracking.byOrder(nextOrderId));
          setTracking(trackingResponse);
          if (trackingResponse.orderItemId) {
            setStartOrderItemId(String(trackingResponse.orderItemId));
            setStageItemId(String(trackingResponse.orderItemId));
            await loadTrackedOrderStages(trackingResponse.orderItemId);
          } else {
            setTrackedOrderStages([]);
          }
        }

        setNotice({
          tone: "success",
          message: isArabic ? "تم نداء طلب الإنتاج التالي وتحميل التتبع." : "Next production order was called and tracking was loaded."
        });
        success(
          isArabic ? "تم نداء الطلب التالي" : "Next order called",
          isArabic ? "تم تحميل التتبع تلقائياً." : "Tracking was loaded automatically."
        );
      } catch (requestError: unknown) {
        const message = getApiErrorMessage(requestError);
        if (message.toLowerCase().includes("no waiting tickets")) {
          neutral(
            isArabic ? "لا يوجد طلب بانتظار الإنتاج" : "No production orders are waiting",
            isArabic ? "الطابور فارغ حالياً." : "The production queue is empty right now."
          );
          return;
        }

        error(isArabic ? "فشل نداء الطلب التالي" : "Call next failed", message);
      }
    });
  };

  const updatePredictionImage = (file: File | null): void => {
    setPredictFile(file);
    if (file) {
      setPredictionErrors((currentErrors) => {
        const nextErrors = { ...currentErrors };
        delete nextErrors.predictFile;
        return nextErrors;
      });
    }
  };

  const loadVisibleQueueTracking = (): void => {
    if (!visibleProductionOrderId) {
      setNotice({
        tone: "danger",
        message: isArabic ? "لا يوجد طلب ظاهر في طابور الإنتاج حالياً." : "There is no visible order in the production queue right now."
      });
      neutral(
        isArabic ? "لا يوجد طلب ظاهر حالياً" : "No visible order right now",
        isArabic ? "جرّب التحديث أو النداء التالي." : "Try refresh or call next."
      );
      return;
    }

    const nextOrderId = String(visibleProductionOrderId);
    setTrackingOrderId(nextOrderId);
    setStartOrderId(nextOrderId);
    loadTracking(nextOrderId);
  };

  return (
    <div className="content-grid">
      <PageHeader
        eyebrow={isArabic ? "الإنتاج" : "Production"}
        title={isArabic ? "تشغيل الإنتاج" : "Production Control"}
        description={
          isArabic
            ? "جهّز الدفعة، ارفع صورة العينة، ثم راقب نتيجة التنبؤ وخطوات الإنتاج."
            : "Create a batch quickly, upload a sample image, and monitor production stages."
        }
      />
      <NoticeBlock notice={notice} />

      {finishTankDialogOpen && tracking ? (
        <div className="modal-backdrop" role="presentation">
          <Card title={isArabic ? "اختيار خزان التعبئة" : "Select filling tank"}>
            <p className="helper-text">
              {isArabic ? "اختر في أي خزان سيتم تعبئة الزيت فيه." : "Choose the tank where the oil will be filled."}
            </p>
            <div className="tracking-tanks">
              {tracking.tanks.map((tank) => (
                <ActionButton
                  className={tank.current ? "secondary" : "ghost"}
                  isBusy={actionState.isBusy(`tracking-finish-${tank.code}`)}
                  key={`finish-${tank.code}`}
                  onClick={() => finishTrackingWithTank(tank.code)}
                  type="button"
                >
                  {isArabic ? `الخزان ${tank.code}` : `Tank ${tank.code}`}
                </ActionButton>
              ))}
            </div>
            <div className="inline-actions">
              <button className="btn ghost" onClick={() => setFinishTankDialogOpen(false)} type="button">
                {isArabic ? "إلغاء" : "Cancel"}
              </button>
            </div>
          </Card>
        </div>
      ) : null}

      <section className="technician-workbench" aria-label={isArabic ? "مساحة عمل الفني" : "Technician workbench"}>
        <div className="technician-queue-card">
          <div className="technician-queue-head">
            <div className="technician-queue-title">
              <span className="technician-queue-icon" aria-hidden="true">▥</span>
              <div>
                <h2>{isArabic ? "طابور الإنتاج" : "Production Queue"}</h2>
                <p>
                  {formatNumber(productionQueue?.stats.totalWaiting ?? 0)} {isArabic ? "بانتظار الدور" : "waiting"}
                  {" · "}
                  {isArabic ? "متوسط" : "avg"} {formatNumber(productionQueue?.stats.averageWaitTime ?? 0)} {isArabic ? "دقيقة" : "min"}
                </p>
              </div>
            </div>
            <ActionButton
              className="secondary technician-open-station"
              disabled={!queueAdvancePermission.allowed}
              disabledReason={queueAdvancePermission.reason}
              busyLabel={isArabic ? "جارٍ فتح المحطة..." : "Opening..."}
              isBusy={actionState.isBusy("production-station-login")}
              onClick={openProductionStation}
              type="button"
            >
              + {isArabic ? "فتح المحطة" : "Open Station"}
            </ActionButton>
          </div>

          <div className="technician-queue-actions">
            <ActionButton
              className="secondary"
              disabled={!canCallNext}
              disabledReason={!queueAdvancePermission.allowed
                ? queueAdvancePermission.reason
                : (isArabic ? "افتح محطة الإنتاج أولاً." : "Open production station first.")}
              busyLabel={isArabic ? "جارٍ النداء..." : "Calling..."}
              isBusy={actionState.isBusy("production-call-next")}
              onClick={callNextProductionOrder}
              type="button"
            >
              {isArabic ? "نداء التالي" : "Call next"}
            </ActionButton>
            <ActionButton
              disabled={!canMarkDone}
              disabledReason={isArabic ? "حمّل طلباً قيد الإنتاج أولاً." : "Load an in-progress order first."}
              onClick={() => setFinishTankDialogOpen(true)}
              type="button"
            >
              {isArabic ? "تعبئة وإنهاء" : "Fill and finish"}
            </ActionButton>
            <button className="btn ghost icon-only" onClick={() => void loadProductionQueue()} type="button" aria-label={isArabic ? "تحديث الطابور" : "Refresh queue"}>
              ↻
            </button>
          </div>

          <div className="technician-serving-card">
            <span className="technician-serving-state">
              <i />
              {isArabic ? "قيد الخدمة الآن" : "Now Serving"}
            </span>
            <div className="technician-serving-main">
              <strong>{formatProductionTicketNumber(currentProductionTicket?.number)}</strong>
              <span>
                {currentProductionTicket?.orderId
                  ? `${isArabic ? "طلب" : "Order"} #${currentProductionTicket.orderId}`
                  : isArabic ? "لا يوجد طلب قيد النداء" : "No order is being served"}
              </span>
              <em>
                {servingMinutes === null
                  ? "--"
                  : `${formatNumber(servingMinutes)} ${isArabic ? "دقيقة" : "min"}`}
              </em>
            </div>
          </div>

          <div className="technician-waiting-list">
            <h3>{isArabic ? `بانتظار الدور (${waitingProductionTickets.length})` : `Waiting (${waitingProductionTickets.length})`}</h3>
            {waitingProductionTickets.length ? (
              waitingProductionTickets.slice(0, 4).map((ticket, index) => (
                <button
                  className="technician-waiting-row"
                  key={ticket.id}
                  onClick={() => {
                    setTrackingOrderId(String(ticket.orderId));
                    setStartOrderId(String(ticket.orderId));
                    loadTracking(String(ticket.orderId));
                  }}
                  type="button"
                >
                  <span>{formatNumber(index + 1)}</span>
                  <strong>{formatProductionTicketNumber(ticket.number)}</strong>
                  <div>
                    <b>{isArabic ? "طلب" : "Order"} #{ticket.orderId}</b>
                    <small>{formatNumber(ticket.estimatedWaitMinutes)} {isArabic ? "دقيقة" : "min"}</small>
                  </div>
                </button>
              ))
            ) : (
              <div className="technician-waiting-empty">
                {isArabic ? "لا توجد طلبات بانتظار الإنتاج." : "No production tickets waiting."}
              </div>
            )}
          </div>

          <div className="technician-queue-metrics">
            <span><strong>{formatNumber(productionQueue?.serving.length ?? 0)}</strong>{isArabic ? "قيد الخدمة" : "Serving"}</span>
            <span><strong>{formatNumber(productionQueue?.stats.averageWaitTime ?? 0)}</strong>{isArabic ? "متوسط الانتظار" : "Avg Wait"}</span>
            <span><strong>{formatNumber(productionQueue?.stats.totalWaiting ?? 0)}</strong>{isArabic ? "في الطابور" : "In Queue"}</span>
          </div>
        </div>

        <div className="workbench-panel">
          <span className="eyebrow">{isArabic ? "التتبع" : "Tracking"}</span>
          <h2>{tracking ? tracking.statusLabel : isArabic ? "غير محمل" : "Not loaded"}</h2>
          <p>{tracking ? tracking.friendlyMessage : isArabic ? "حمّل طلباً من الطابور أو أدخل رقم الطلب في الأسفل." : "Load an order from the queue or enter an order ID below."}</p>
          <div className="workbench-mini-metrics">
            <span>{isArabic ? "التقدم" : "Progress"} <strong>{tracking ? `${tracking.progressPercent}%` : "--"}</strong></span>
            <span>{isArabic ? "الخط" : "Line"} <strong>{trackedLineLabel}</strong></span>
            <span>{isArabic ? "الخزان" : "Tank"} <strong>{tracking?.tankLabel ?? "--"}</strong></span>
          </div>
        </div>

        <div className="workbench-panel workbench-flow-panel">
          <span className="eyebrow">{isArabic ? "خطوات الفني" : "Technician Flow"}</span>
          <h2>{isArabic ? "اتبع الترتيب الصحيح" : "Follow the right order"}</h2>
          <ol>
            <li>{isArabic ? "افتح محطة الإنتاج ونداء التالي." : "Open station and call next."}</li>
            <li>{isArabic ? "حمّل تتبع الطلب الحالي تلقائياً." : "Load tracking for the visible order."}</li>
            <li>{isArabic ? "حدّث حالة الطلب حسب المرحلة." : "Update status with the active stage."}</li>
            <li>{isArabic ? "اختر الخزان الصحيح A/B/C/D للطلب." : "Assign the right tank A/B/C/D."}</li>
          </ol>
          <button className="btn secondary" onClick={loadVisibleQueueTracking} type="button">
            {isArabic ? "تطبيق الخطوات الآن" : "Apply flow now"}
          </button>
        </div>
      </section>

      {predictionPermission ? (
        <div className="content-grid two">
          <Card
            title={isArabic ? "دفعة التنبؤ" : "Prediction Batch"}
            subtitle={
              isArabic
                ? "أدخل رقم الطلب فقط وسيتم تجهيز الدفعة من بيانات الطلب."
                : "Enter the order ID only. The batch is prepared from the order details."
            }
          >
            <ErrorSummary errors={predictionErrors} />

            <div className="form-grid">
              <div className="field">
                <label>{isArabic ? "رقم الطلب" : "Order ID"}</label>
                <input
                  onChange={(event) => setPredictionOrderId(event.target.value)}
                  placeholder={isArabic ? "مثال: 20" : "Example: 20"}
                  value={predictionOrderId}
                />
                <FieldError errors={predictionErrors} name="predictionOrderId" />
              </div>
            </div>

            <p className="helper-text">
              {isArabic
                ? "سيختار النظام عنصر الزيتون داخل الطلب ويستخدم الكمية المسجلة كوزن الدفعة."
                : "The backend selects the olive item in the order and uses its quantity as the batch weight."}
            </p>

            <div className="inline-actions">
              <ActionButton
                busyLabel={isArabic ? "جارٍ تجهيز الدفعة..." : "Preparing batch..."}
                isBusy={actionState.isBusy("create-production-batch")}
                onClick={createBatch}
                type="button"
              >
                {isArabic ? "تجهيز الدفعة" : "Prepare batch"}
              </ActionButton>

              <ActionButton
                className="secondary"
                busyLabel={isArabic ? "جارٍ التحميل..." : "Loading..."}
                isBusy={actionState.isBusy("load-batch-by-order")}
                onClick={loadBatchByOrderItem}
                type="button"
              >
                {isArabic ? "تحميل دفعة الطلب" : "Load order batch"}
              </ActionButton>

              <ActionButton
                className="ghost"
                disabled
                disabledReason={
                  isArabic
                    ? "يتم اختيار عنصر الزيتون والوزن تلقائياً من الطلب."
                    : "The olive item and weight are selected automatically from the order."
                }
                type="button"
              >
                {isArabic ? "اختيار تلقائي للدفعة" : "Batch selected automatically"}
              </ActionButton>
            </div>

            {selectedBatch ? (
              <div className="metric-strip">
                <div>
                  <strong>{isArabic ? "الزيت المتوقع" : "Latest Oil Output"}</strong>
                  <div>{formatLiters(selectedBatchOilLiters, language)} {isArabic ? "لتر" : "L"}</div>
                </div>
                <div>
                  <strong>{isArabic ? "عنصر الطلب" : "Order Item"}</strong>
                  <div>#{selectedBatch.orderItemId}</div>
                </div>
                <div>
                  <strong>{isArabic ? "الوزن" : "Weight"}</strong>
                  <div>
                    {formatNumber(selectedBatch.oliveWeightKg)} {isArabic ? "كغ" : "kg"}
                  </div>
                </div>
                <div>
                  <strong>{isArabic ? "نسبة الزيت" : "Yield Rate"}</strong>
                  <div>{selectedBatch.predictedYieldPercent === null || selectedBatch.predictedYieldPercent === undefined ? "--" : formatPercent(selectedBatch.predictedYieldPercent, language)}</div>
                </div>
              </div>
            ) : (
              <p className="helper-text">
                {isArabic
                  ? "ابدأ بإدخال رقم عنصر الطلب والوزن. هذا يكفي للوصول إلى التنبؤ."
                  : "Start with the order item ID and olive weight. That is enough to reach prediction."}
              </p>
            )}

            {recentBatches.length ? (
              <div className="table-wrap">
                <table className="data-table">
                  <thead>
                    <tr>
                      <th>{isArabic ? "الزيت المتوقع" : "Latest Oil"}</th>
                      <th>{isArabic ? "العنصر" : "Item"}</th>
                      <th>{isArabic ? "الوزن" : "Weight"}</th>
                      <th>{isArabic ? "الحالة" : "Status"}</th>
                    </tr>
                  </thead>
                  <tbody>
                    {recentBatches.slice(0, 5).map((batch) => (
                      <tr
                        key={batch.batchId}
                        onClick={() => applySelectedBatch(batch)}
                        style={{ cursor: "pointer" }}
                      >
                        <td>{formatLiters(toOilLiters(batch.predictedOilKg ?? null), language)} {isArabic ? "لتر" : "L"}</td>
                        <td>{batch.orderItemId}</td>
                        <td>
                          {formatNumber(batch.oliveWeightKg)} {isArabic ? "كغ" : "kg"}
                        </td>
                        <td>
                          <StatusBadge value={batch.status} />
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            ) : null}
          </Card>

          <Card
            title={isArabic ? "التنبؤ من الصورة" : "Image Prediction"}
            subtitle={
              isArabic
                ? "بعد تجهيز الدفعة، ارفع صورة واحدة واضحة لتحصل على كمية الزيت المتوقعة ونسبة الزيت."
                : "After preparing the batch, upload an image to see the latest expected oil output in liters."
            }
          >
            <ErrorSummary
              errors={predictionErrors.selectedBatch ? { selectedBatch: predictionErrors.selectedBatch } : {}}
            />
            <div className="form-grid">
              <div className="field">
                <label>{isArabic ? "العنصر المختار" : "Selected Item"}</label>
                <input
                  disabled
                  value={selectedBatchSummary}
                />
              </div>

              <div className="field">
                <label>{isArabic ? "صورة العينة" : "Sample Image"}</label>
                <CameraCaptureInput
                  file={predictFile}
                  id="technician-predict-file-v2"
                  language={language}
                  onFileChange={updatePredictionImage}
                />
                <FieldError errors={predictionErrors} name="predictFile" />
              </div>
            </div>

            <div className="inline-actions">
              <ActionButton
                busyLabel={isArabic ? "جارٍ تنفيذ التنبؤ..." : "Predicting..."}
                disabled={!selectedBatch}
                disabledReason={
                  selectedBatch ? undefined : isArabic ? "جهّز دفعة أو حمّل دفعة موجودة أولاً." : "Prepare or load a batch first."
                }
                isBusy={actionState.isBusy("technician-ai-predict-v2")}
                onClick={runPrediction}
                type="button"
              >
                {isArabic ? "تشغيل التنبؤ" : "Run prediction"}
              </ActionButton>
            </div>

            <div className="prediction-highlight">
              <div className="prediction-value">
                {formatLiters(predictedOilLiters, language)} {isArabic ? "لتر" : "L"}
              </div>
              <div className="prediction-meta">
                <span>
                  {isArabic ? "عنصر الطلب" : "Order item"}: <strong>{selectedBatch?.orderItemId ? `#${selectedBatch.orderItemId}` : "--"}</strong>
                </span>
                <span>
                  {isArabic ? "وزن الزيتون" : "Olive weight"}:{" "}
                  <strong>
                    {selectedBatch?.oliveWeightKg
                      ? `${formatNumber(selectedBatch.oliveWeightKg)} ${isArabic ? "كغ" : "kg"}`
                      : "--"}
                  </strong>
                </span>
                <span>
                  {isArabic ? "نسبة الزيت المتوقعة" : "Yield rate"}:{" "}
                  <strong>{predictedYieldPercent === null ? "--" : formatPercent(predictedYieldPercent, language)}</strong>
                </span>
              </div>
            </div>

            <p className="helper-text">
              {isArabic
                ? "كمية الزيت المعروضة هنا تقديرية ومحوّلة إلى لتر لتكون أوضح للفني أثناء العمل."
                : "Oil output is shown as an approximate liter value to keep the technician view clear."}
            </p>

            {predictedOilLiters === null ? (
              <p className="helper-text">
                {isArabic
                  ? "بعد تجهيز الدفعة، ارفع صورة واحدة واضحة ثم شغّل التنبؤ لتظهر كمية الزيت ونسبة الزيت المتوقعة."
                  : "Once the batch is ready, upload one sample image and run prediction to see the latest oil output."}
              </p>
            ) : null}
          </Card>
        </div>
      ) : null}

      <Card
        title={isArabic ? "تتبع الدفعة" : "Batch Tracking"}
        subtitle={
          isArabic
            ? "عرض بسيط لحالة الدفعة، الوقت التقريبي، والخزان الحالي للزيت."
            : "A simple view of the batch status, approximate time, and current oil tank."
        }
      >
        <div className="tracking-lookup">
          <div className="field">
            <label>{isArabic ? "رقم الطلب" : "Order ID"}</label>
            <input
              onChange={(event) => setTrackingOrderId(event.target.value)}
              placeholder={isArabic ? "مثال: 20" : "Example: 20"}
              value={trackingOrderId}
            />
          </div>
          <ActionButton
            className="secondary"
            busyLabel={isArabic ? "جارٍ تحميل التتبع..." : "Loading tracking..."}
            isBusy={actionState.isBusy("load-tracking")}
            onClick={() => loadTracking()}
            type="button"
          >
            {isArabic ? "عرض التتبع" : "View tracking"}
          </ActionButton>
        </div>

        {tracking ? (
          <div className="tracking-panel">
            <div className="tracking-main">
              <div>
                <span className="eyebrow">{tracking.batchId}</span>
                <h3>{isArabic ? "حالة الزيت الآن" : "Oil Status Now"}</h3>
                <p>{tracking.friendlyMessage}</p>
              </div>
              <StatusBadge value={tracking.statusLabel} />
            </div>

            <div className="tracking-progress" aria-label={isArabic ? "نسبة التقدم" : "Tracking progress"}>
              <div className="tracking-progress-bar">
                <span style={{ width: `${Math.min(Math.max(tracking.progressPercent, 0), 100)}%` }} />
              </div>
              <div className="tracking-progress-meta">
                <strong>{tracking.progressPercent}%</strong>
                <span>
                  {tracking.status === "DONE"
                    ? isArabic ? "جاهز للاستلام" : "Ready for pickup"
                    : `${tracking.estimatedRemainingMinutes} ${isArabic ? "دقيقة متبقية تقريباً" : "min approx. remaining"}`}
                </span>
              </div>
            </div>

            <div className="tracking-quick-presets">
              <strong>{isArabic ? "إعدادات سريعة" : "Quick presets"}</strong>
              <div className="tracking-quick-buttons">
                {tracking.tanks.map((tank) => (
                  <button
                    className="btn secondary"
                    disabled={!canSetInProgress}
                    key={`preset-${tank.code}`}
                    onClick={() => setInProgressWithTank(tank.code)}
                    type="button"
                  >
                    {isArabic ? `قيد التنفيذ + خزان ${tank.code}` : `IN_PROGRESS + Tank ${tank.code}`}
                  </button>
                ))}
              </div>
            </div>

            <div className="tracking-tanks">
              {tracking.tanks.map((tank) => (
                <button
                  className={`tank-button ${tank.current ? "active" : ""}`}
                  disabled={!canAssignTank}
                  key={tank.code}
                  onClick={() => updateTrackingTank(tank.code)}
                  title={isArabic ? `تعيين الخزان ${tank.code}` : `Set tank ${tank.code}`}
                  type="button"
                >
                  <span>{tank.code}</span>
                  <small>{tank.current ? (isArabic ? "زيت العميل هنا" : "Customer oil here") : tank.label}</small>
                </button>
              ))}
            </div>

            <div className="metric-strip">
              <div>
                <strong>{isArabic ? "الخط الحالي" : "Current line"}</strong>
                <div>{trackedLineLabel}</div>
              </div>
              <div>
                <strong>{isArabic ? "الخزان" : "Tank"}</strong>
                <div>{tracking.tankLabel}</div>
              </div>
              <div>
                <strong>{isArabic ? "عنصر الطلب" : "Order Item"}</strong>
                <div>{tracking.orderItemId ? `#${tracking.orderItemId}` : "--"}</div>
              </div>
              <div>
                <strong>{isArabic ? "الوزن" : "Weight"}</strong>
                <div>{tracking.oliveWeightKg ? `${formatNumber(tracking.oliveWeightKg)} ${isArabic ? "كغ" : "kg"}` : "--"}</div>
              </div>
              <div>
                <strong>{isArabic ? "الزيت المتوقع" : "Expected Oil"}</strong>
                <div>{formatLiters(toOilLiters(tracking.predictedOilKg ?? null), language)} {isArabic ? "لتر" : "L"}</div>
              </div>
              <div>
                <strong>{isArabic ? "آخر تحديث" : "Last update"}</strong>
                <div>{tracking.updatedAt ? new Intl.DateTimeFormat(language === "ar" ? "ar" : "en", { hour: "2-digit", minute: "2-digit" }).format(new Date(tracking.updatedAt)) : "--"}</div>
              </div>
            </div>

            {trackedOrderStages.length ? (
              <div className="tracking-line-timeline" aria-label={isArabic ? "مراحل الخط" : "Line stages"}>
                {trackedOrderStages.map((stage) => {
                  const stageStatus = stage.currentStatus.toUpperCase();
                  const isActive = stageStatus.includes("IN_PROGRESS");
                  const isDone = stageStatus.includes("DONE") || stageStatus.includes("FINISHED") || stageStatus.includes("COMPLETED");

                  const lineToneClass = getLineToneClass(stage.line);

                  return (
                    <div className={`line-stage ${lineToneClass} ${isActive ? "active" : ""} ${isDone ? "done" : ""}`} key={stage.id}>
                      <span className="line-stage-order">{stage.stageOrder}</span>
                      <div>
                        <strong>{stage.name}</strong>
                        <small>{isArabic ? "الخط" : "Line"}: {stage.line} - {stage.currentStatus}</small>
                      </div>
                    </div>
                  );
                })}
              </div>
            ) : null}

            <div className="tracking-actions">
              <button className="btn ghost" disabled type="button">
                {isArabic ? "مسجلة" : "Registered"}
              </button>
              <button
                className="btn secondary"
                disabled={!canSetInProgress}
                onClick={() => updateTrackingStatus("IN_PROGRESS")}
                type="button"
              >
                {isArabic ? "قيد التنفيذ" : "In progress"}
              </button>
              <button
                className="btn"
                disabled={!canMarkDone}
                onClick={() => setFinishTankDialogOpen(true)}
                type="button"
              >
                {isArabic ? "تعبئة وإنهاء" : "Fill and finish"}
              </button>
            </div>
          </div>
        ) : (
          <p className="helper-text">
            {isArabic
              ? "بعد تسجيل الدفعة، أدخل رقم الطلب وسيظهر للموظف أين وصلت الدفعة وأي خزان يحتوي زيت العميل."
              : "After a batch is registered, enter the order ID to see progress and which tank holds the customer's oil."}
          </p>
        )}
      </Card>

      <div
        className={
          startPermission.allowed && etaPermission.allowed
            ? "content-grid two"
            : "content-grid"
        }
      >
        {startPermission.allowed ? (
          <Card title={isArabic ? "بدء الإنتاج" : "Start Production"}>
            <div className="form-grid two">
              <div className="field">
                <label>{t("Order ID")}</label>
                <input onChange={(event) => setStartOrderId(event.target.value)} value={startOrderId} />
              </div>
              <div className="field">
                <label>{t("Order Item ID")}</label>
                <input onChange={(event) => setStartOrderItemId(event.target.value)} value={startOrderItemId} />
              </div>
            </div>
            <div className="inline-actions">
              <ActionButton
                disabled={!canRunStartProduction}
                disabledReason={!startPermission.allowed
                  ? startPermission.reason
                  : (isArabic ? "حمّل تتبع الطلب واجعله قيد التنفيذ أولاً." : "Load tracking and move it to IN_PROGRESS first.")}
                busyLabel={isArabic ? "جارٍ البدء..." : "Starting..."}
                isBusy={actionState.isBusy("start-production")}
                onClick={startProduction}
                type="button"
              >
                {isArabic ? "بدء الإنتاج" : "Start production"}
              </ActionButton>
            </div>
            <PermissionNote allowed={startPermission.allowed} reason={startPermission.reason} />
          </Card>
        ) : null}

        {etaPermission.allowed ? (
          <Card title={isArabic ? "الوقت المتوقع والمراحل" : "ETA and Stage Lookup"}>
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
                  busyLabel={isArabic ? "جارٍ التحميل..." : "Loading..."}
                  isBusy={actionState.isBusy("load-eta")}
                  onClick={loadEta}
                  type="button"
                >
                  {isArabic ? "تحميل الوقت المتوقع" : "Load ETA"}
                </ActionButton>
              </div>
              <PermissionNote allowed={etaPermission.allowed} reason={etaPermission.reason} />
              {eta ? (
                <Banner
                  tone="neutral"
                  message={
                    isArabic
                      ? `الخط ${eta.line}، المرحلة ${eta.currentStage}، الوقت المتوقع ${eta.eta} دقيقة.`
                      : `Line ${eta.line}, stage ${eta.currentStage}, ETA ${eta.eta} minutes.`
                  }
                />
              ) : null}
            </div>
          </Card>
        ) : null}
      </div>

      {dashboardPermission.allowed && dashboard.length ? (
        <Card title={isArabic ? "لوحة متابعة الإنتاج" : "Production Dashboard"}>
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
                    <td>
                      <StatusBadge value={item.status} />
                    </td>
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

    </div>
  );
}

