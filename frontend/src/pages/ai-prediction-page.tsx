import { useEffect, useState } from "react";
import { useI18n } from "../app/i18n-context";
import { useToastHelpers } from "../app/toast-context";
import { CameraCaptureInput } from "../components/CameraCaptureInput";
import { ActionButton, ErrorSummary, FieldError } from "../components/form-ui";
import { Card, PageHeader, StatusBadge } from "../components/ui";
import { endpoints } from "../lib/endpoints";
import { apiRequest } from "../lib/http";
import { useActionState } from "../lib/use-action-state";
import { formatNumber } from "../lib/utils";
import { getApiErrorMessage, getApiFieldErrors, type FieldErrors, validatePositiveId } from "../lib/validation";
import type { AiPredictionResponse, ProductionBatch } from "../types/models";

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

export function AiPredictionPage(): JSX.Element {
  const { language } = useI18n();
  const { success, error } = useToastHelpers();
  const actionState = useActionState();
  const isArabic = language === "ar";
  const [predictionOrderId, setPredictionOrderId] = useState("");
  const [selectedBatch, setSelectedBatch] = useState<ProductionBatch | null>(null);
  const [recentBatches, setRecentBatches] = useState<ProductionBatch[]>([]);
  const [predictionErrors, setPredictionErrors] = useState<FieldErrors>({});
  const [predictFile, setPredictFile] = useState<File | null>(null);
  const [predictedYieldPercent, setPredictedYieldPercent] = useState<number | null>(null);
  const [predictedOilKg, setPredictedOilKg] = useState<number | null>(null);

  const selectedBatchSummary = selectedBatch
    ? `${isArabic ? "عنصر الطلب" : "Item"} #${selectedBatch.orderItemId} - ${formatNumber(selectedBatch.oliveWeightKg)} ${isArabic ? "كغ" : "kg"}`
    : isArabic ? "لم يتم اختيار أي دفعة بعد" : "No batch selected";
  const predictedOilLiters = toOilLiters(predictedOilKg);
  const selectedBatchOilLiters = toOilLiters(selectedBatch?.predictedOilKg ?? null);

  const applySelectedBatch = (batch: ProductionBatch): void => {
    setSelectedBatch(batch);
    setPredictionOrderId(batch.orderId ? String(batch.orderId) : "");
    setPredictedYieldPercent(batch.predictedYieldPercent ?? null);
    setPredictedOilKg(batch.predictedOilKg ?? null);
  };

  const loadRecentBatches = async (): Promise<void> => {
    try {
      setRecentBatches(await apiRequest<ProductionBatch[]>(endpoints.production.batches.list));
    } catch {
      setRecentBatches([]);
    }
  };

  useEffect(() => {
    void loadRecentBatches();
  }, []);

  const loadBatchByOrder = (): void => {
    const nextErrors = validatePositiveId("predictionOrderId", predictionOrderId);
    setPredictionErrors(nextErrors);
    if (Object.keys(nextErrors).length) {
      return;
    }

    void actionState.runAction("ai-load-batch-by-order", async () => {
      try {
        const response = await apiRequest<ProductionBatch>(endpoints.production.batches.byOrder(predictionOrderId.trim()), {
          method: "POST"
        });
        applySelectedBatch(response);
        setPredictionErrors({});
        await loadRecentBatches();
        success(
          isArabic ? `تم تحميل دفعة الطلب ${response.batchId}` : `Loaded order batch ${response.batchId}`,
          isArabic ? "يمكن الآن رفع صورة العينة." : "You can now upload the sample image."
        );
      } catch (requestError: unknown) {
        setPredictionErrors(getApiFieldErrors(requestError));
        error(isArabic ? "فشل تحميل دفعة الطلب" : "Batch load failed", getApiErrorMessage(requestError));
      }
    });
  };

  const runPrediction = (): void => {
    const nextErrors: FieldErrors = {};

    if (!selectedBatch) {
      nextErrors.selectedBatch = isArabic ? "حمّل دفعة الطلب أولاً." : "Load the order batch first.";
    }
    if (!predictFile || predictFile.size === 0) {
      nextErrors.predictFile = isArabic ? "التقط أو اختر صورة واضحة أولاً." : "Capture or choose a clear image first.";
    }

    setPredictionErrors(nextErrors);
    if (Object.keys(nextErrors).length) {
      return;
    }

    void actionState.runAction("reception-ai-predict", async () => {
      try {
        const body = new FormData();
        body.append("file", predictFile as File);
        body.append("batchId", selectedBatch?.batchId ?? "");

        const response = await apiRequest<AiPredictionResponse>(endpoints.ai.predict, {
          method: "POST",
          body
        });

        const refreshedBatch = await apiRequest<ProductionBatch>(endpoints.production.batches.byId(response.batchId));
        applySelectedBatch(refreshedBatch);
        setPredictedYieldPercent(response.predictedYieldPercent ?? null);
        setPredictedOilKg(response.predictedOilKg ?? refreshedBatch.predictedOilKg ?? null);
        setPredictionErrors({});
        await loadRecentBatches();
        success(
          isArabic ? "تم تنفيذ التنبؤ بنجاح" : "Prediction completed successfully",
          isArabic ? "تم ربط النتيجة بالدفعة." : "The result is linked to the batch."
        );
      } catch (requestError: unknown) {
        setPredictionErrors(getApiFieldErrors(requestError));
        error(isArabic ? "فشل التنبؤ" : "Prediction failed", getApiErrorMessage(requestError));
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

  return (
    <div className="content-grid">
      <PageHeader
        eyebrow={isArabic ? "التنبؤ الذكي" : "AI Prediction"}
        title={isArabic ? "تنبؤ مردود الزيت" : "Oil Yield Prediction"}
        description={
          isArabic
            ? "حمّل دفعة الطلب ثم ارفع صورة العينة للحصول على كمية الزيت المتوقعة."
            : "Load an order batch, upload the sample image, and get the expected oil output."
        }
      />

      <div className="content-grid two">
        <Card
          title={isArabic ? "دفعة الطلب" : "Order Batch"}
          subtitle={
            isArabic
              ? "أدخل رقم الطلب وسيتم تجهيز الدفعة من بياناته الحالية."
              : "Enter the order ID and prepare the batch from its current data."
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
          <div className="inline-actions">
            <ActionButton
              busyLabel={isArabic ? "جار التحميل..." : "Loading..."}
              isBusy={actionState.isBusy("ai-load-batch-by-order")}
              onClick={loadBatchByOrder}
              type="button"
            >
              {isArabic ? "تحميل دفعة الطلب" : "Load order batch"}
            </ActionButton>
          </div>

          {selectedBatch ? (
            <div className="ai-batch-summary">
              <div className="ai-batch-summary-head">
                <span>{isArabic ? "الدفعة المختارة" : "Selected batch"}</span>
                <strong>{selectedBatch.batchId}</strong>
              </div>
              <div className="ai-batch-summary-grid">
                <div className="featured">
                  <span>{isArabic ? "الزيت المتوقع" : "Expected oil"}</span>
                  <strong>{formatLiters(selectedBatchOilLiters, language)} {isArabic ? "لتر" : "L"}</strong>
                </div>
                <div>
                  <span>{isArabic ? "عنصر الطلب" : "Order item"}</span>
                  <strong>#{selectedBatch.orderItemId}</strong>
                </div>
                <div>
                  <span>{isArabic ? "الوزن" : "Weight"}</span>
                  <strong>{formatNumber(selectedBatch.oliveWeightKg)} {isArabic ? "كغ" : "kg"}</strong>
                </div>
                <div>
                  <span>{isArabic ? "نسبة الزيت" : "Yield rate"}</span>
                  <strong>{selectedBatch.predictedYieldPercent == null ? "--" : formatPercent(selectedBatch.predictedYieldPercent, language)}</strong>
                </div>
              </div>
            </div>
          ) : null}
        </Card>

        <Card
          title={isArabic ? "الصورة والنتيجة" : "Image And Result"}
          subtitle={
            isArabic
              ? "بعد تحميل الدفعة، ارفع صورة واضحة وشغل التنبؤ."
              : "After loading the batch, upload a clear image and run prediction."
          }
        >
          <ErrorSummary errors={predictionErrors.selectedBatch ? { selectedBatch: predictionErrors.selectedBatch } : {}} />
          <div className="form-grid">
            <div className="field">
              <label>{isArabic ? "الدفعة المختارة" : "Selected Batch"}</label>
              <input disabled value={selectedBatchSummary} />
            </div>
            <div className="field">
              <label>{isArabic ? "صورة العينة" : "Sample Image"}</label>
              <CameraCaptureInput
                file={predictFile}
                id="reception-predict-file"
                language={language}
                onFileChange={updatePredictionImage}
              />
              <FieldError errors={predictionErrors} name="predictFile" />
            </div>
          </div>

          <div className="inline-actions">
            <ActionButton
              busyLabel={isArabic ? "جار تنفيذ التنبؤ..." : "Predicting..."}
              disabled={!selectedBatch}
              disabledReason={selectedBatch ? undefined : isArabic ? "حمّل دفعة الطلب أولاً." : "Load the order batch first."}
              isBusy={actionState.isBusy("reception-ai-predict")}
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
              <span>{isArabic ? "عنصر الطلب" : "Order item"}: <strong>{selectedBatch?.orderItemId ? `#${selectedBatch.orderItemId}` : "--"}</strong></span>
              <span>
                {isArabic ? "وزن الزيتون" : "Olive weight"}:{" "}
                <strong>{selectedBatch?.oliveWeightKg ? `${formatNumber(selectedBatch.oliveWeightKg)} ${isArabic ? "كغ" : "kg"}` : "--"}</strong>
              </span>
              <span>{isArabic ? "نسبة الزيت المتوقعة" : "Yield rate"}: <strong>{predictedYieldPercent === null ? "--" : formatPercent(predictedYieldPercent, language)}</strong></span>
            </div>
          </div>
        </Card>
      </div>

      {recentBatches.length ? (
        <Card title={isArabic ? "دفعات حديثة" : "Recent Batches"}>
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
                  <tr key={batch.batchId} onClick={() => applySelectedBatch(batch)} style={{ cursor: "pointer" }}>
                    <td>{formatLiters(toOilLiters(batch.predictedOilKg ?? null), language)} {isArabic ? "لتر" : "L"}</td>
                    <td>{batch.orderItemId}</td>
                    <td>{formatNumber(batch.oliveWeightKg)} {isArabic ? "كغ" : "kg"}</td>
                    <td><StatusBadge value={batch.status} /></td>
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
