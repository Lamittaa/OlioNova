import { endpoints } from "./endpoints";
import { apiRequest } from "./http";
import type { ApiError, ProductionDashboardItem, ProductionStage } from "../types/models";

interface LegacyDashboardRow {
  orderId: number;
  status: string;
  itemsCount: number;
  completedItems: number;
  queueNumber?: number | null;
  storage?: string[];
}

interface LineOverviewResponse {
  line: string;
  stages: Array<{
    stageId: number;
    stageType: string;
    containerName: string;
    stageStatus: string;
    item?: {
      orderId?: number;
      orderItemId?: number;
    } | null;
  }>;
}

function isApiError(error: unknown): error is ApiError {
  return typeof error === "object" && error !== null && "status" in error;
}

function normalizeLegacyDashboard(rows: LegacyDashboardRow[]): ProductionDashboardItem[] {
  return rows.map((row) => {
    const remainingUnits = Math.max(row.itemsCount - row.completedItems, 0);
    const remainingMinutes = remainingUnits * 15;

    return {
      line: `Order #${row.orderId}`,
      stage: row.storage?.length ? `Storage ${row.storage.join(", ")}` : `${row.completedItems}/${row.itemsCount} items`,
      status: row.status,
      queue: row.queueNumber ?? 0,
      remainingMinutes,
      eta: remainingMinutes,
      avgStageTime: row.itemsCount > 0 ? 15 : 0,
      throughputPerHour: row.completedItems
    };
  });
}

function normalizePipeline(lines: LineOverviewResponse[]): ProductionStage[] {
  return lines.flatMap((lineOverview) =>
    lineOverview.stages.map((stage, index) => ({
      id: stage.stageId,
      name: `${stage.stageType} (${stage.containerName})`,
      stageType: stage.stageType,
      container: stage.containerName,
      orderId: stage.item?.orderId ?? 0,
      orderItemId: stage.item?.orderItemId ?? 0,
      line: lineOverview.line,
      stageOrder: index + 1,
      currentStatus: stage.stageStatus
    }))
  );
}

export async function fetchProductionDashboard(): Promise<ProductionDashboardItem[]> {
  try {
    const response = await apiRequest<ProductionDashboardItem[]>(endpoints.production.dashboard);
    if (response.length && "line" in response[0] && "stage" in response[0]) {
      return response;
    }
    return normalizeLegacyDashboard(response as unknown as LegacyDashboardRow[]);
  } catch (error: unknown) {
    if (!isApiError(error) || error.status !== 404) {
      throw error;
    }

    const fallback = await apiRequest<LegacyDashboardRow[]>("/api/production/orders");
    return normalizeLegacyDashboard(fallback);
  }
}

export async function fetchProductionPipeline(): Promise<ProductionStage[]> {
  try {
    const response = await apiRequest<ProductionStage[] | LineOverviewResponse[]>(endpoints.production.pipeline);
    if (response.length && "id" in response[0]) {
      return response as ProductionStage[];
    }
    return normalizePipeline(response as LineOverviewResponse[]);
  } catch (error: unknown) {
    if (!isApiError(error) || error.status !== 404) {
      throw error;
    }

    const fallback = await apiRequest<LineOverviewResponse[]>("/api/production/lines/overview");
    return normalizePipeline(fallback);
  }
}
