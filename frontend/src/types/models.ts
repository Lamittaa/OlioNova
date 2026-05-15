export type Role = "ADMIN" | "ACCOUNTANT" | "RECEPTIONIST" | "TECHNICIAN";

export type PageKey =
  | "overview"
  | "profile"
  | "cities"
  | "customers"
  | "products"
  | "orders"
  | "ai"
  | "invoice"
  | "payments"
  | "reports"
  | "queue"
  | "queue-control"
  | "production"
  | "pricing"
  | "queue-display-admin"
  | "order-management"
  | "admin";

export type ActionKey =
  | "AUTH_LOGIN"
  | "PROFILE_VIEW"
  | "PROFILE_UPDATE"
  | "PASSWORD_CHANGE"
  | "CITY_READ"
  | "CITY_CREATE"
  | "CITY_UPDATE"
  | "CITY_DELETE"
  | "CUSTOMER_READ"
  | "CUSTOMER_SEARCH"
  | "CUSTOMER_CREATE"
  | "CUSTOMER_UPDATE"
  | "CUSTOMER_DELETE"
  | "CUSTOMER_UPDATE_NATIONAL_ID"
  | "PRODUCT_READ"
  | "PRODUCT_CREATE"
  | "PRODUCT_UPDATE"
  | "PRODUCT_UPDATE_INVENTORY"
  | "PRODUCT_DELETE"
  | "ORDER_CREATE"
  | "ORDER_READ"
  | "ORDER_CANCEL"
  | "ORDER_UPDATE_STATUS"
  | "ORDER_ITEM_READ"
  | "ORDER_ITEM_ADD"
  | "ORDER_ITEM_UPDATE"
  | "ORDER_ITEM_DELETE"
  | "ORDER_ITEM_UPDATE_STATUS"
  | "ORDER_STATUS_READ"
  | "PAYMENT_CREATE"
  | "PAYMENT_READ"
  | "PAYMENT_REPORT_EXPORT"
  | "PRODUCTION_START"
  | "PRODUCTION_DASHBOARD"
  | "PRODUCTION_PIPELINE"
  | "STAGE_START"
  | "STAGE_FINISH"
  | "ETA_VIEW"
  | "QUEUE_STATUS"
  | "QUEUE_ADVANCE"
  | "QUEUE_ISSUE_ACCOUNTING"
  | "QUEUE_ISSUE_PRODUCTION"
  | "ADMIN_MANAGE"
  | "AI_TOOLS";

export interface ApiFieldError {
  field?: string;
  message?: string;
  rejectedValue?: unknown;
}

export interface ApiError {
  status: number;
  message: string;
  error?: string;
  code?: string;
  path?: string;
  timestamp?: string;
  errors?: ApiFieldError[];
}

export interface JwtClaims {
  sub?: string;
  exp?: number;
  iat?: number;
  userId?: number;
  authorities?: string[];
}

export interface TokenBundle {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresIn: number;
  refreshExpiresIn: number;
  username: string;
  role: Role;
}

export interface LoginRequest {
  username: string;
  password: string;
}

export interface RefreshRequest {
  refreshToken: string;
}

export interface SetPasswordRequest {
  token: string;
  newPassword: string;
}

export interface Profile {
  id: number;
  nationalId: string;
  firstName: string;
  lastName: string;
  phoneNumber: string;
  email: string;
  city?: string;
  gender: string;
  maritalStatus: string;
  username: string;
  role: string;
}

export interface UpdateProfileRequest {
  firstName?: string;
  lastName?: string;
  phoneNumber?: string;
  email?: string;
  city?: string;
  gender?: string;
  maritalStatus?: string;
}

export interface ChangePasswordRequest {
  oldPassword: string;
  newPassword: string;
}

export interface EmployeeListItem {
  id: number;
  fullName: string;
  nationalId: string;
  email: string;
  phoneNumber: string;
  username: string;
  role: string;
  enabled: boolean;
}

export interface Employee {
  id: number;
  nationalId: string;
  firstName: string;
  lastName: string;
  phoneNumber: string;
  email: string;
  gender: string;
  maritalStatus: string;
  username: string;
  role: string;
  enabled: boolean;
}

export interface CreateEmployeeRequest {
  nationalId: string;
  firstName: string;
  lastName: string;
  phoneNumber: string;
  email: string;
  gender: string;
  maritalStatus: string;
  roleName: string;
}

export interface UpdateEmployeeRequest extends CreateEmployeeRequest {
  enabled?: boolean;
}

export interface RoleRecord {
  id: number;
  name: string;
  authorities: string[];
}

export interface RoleAuthoritiesResponse {
  roleId: number;
  roleName: string;
  authorities: string[];
  message?: string;
}

export interface AuthorityRecord {
  id: number;
  name: string;
}

export interface City {
  id: number;
  cityName: string;
}

export interface CityInput {
  cityName: string;
}

export interface Customer {
  id: number;
  nationalId: string;
  firstName: string;
  lastName: string;
  phoneNumber: string;
  cityId: number;
  isMember: boolean;
}

export interface CustomerInput {
  nationalId?: string;
  firstName?: string;
  lastName?: string;
  phoneNumber?: string;
  cityId?: number;
  isMember?: boolean;
}

export interface MembershipResponse {
  isMembership: boolean;
}

export interface Product {
  id: number;
  productName: string;
  productType: string;
  inventory: number | null;
  inventoryTotalQuantity?: number | null;
  inventoryAvailabilityQuantity?: number | null;
  price: number;
  unit: string;
}

export interface ProductInput {
  productName: string;
  productType: string;
  inventory: number | null;
  price: number;
  unit: string;
}

export interface InventoryInput {
  inventory: number;
}

export interface OrderItem {
  id: number;
  productId: number;
  productName: string;
  productType: string;
  quantity: number;
  status: string;
  oliveType?: string;
  bagsCount?: number;
  note?: string;
}

export interface Order {
  id: number;
  customerId: number;
  status: string;
  items: OrderItem[];
  member?: boolean;
  isMember?: boolean;
  createdAt?: string;
  updatedAt?: string;
}

export interface OrderItemInput {
  productId: number;
  quantity: number;
  oliveType?: string;
  bagsCount?: number;
  note?: string;
}

export interface CreateOrderRequest {
  customerId: number;
  items: OrderItemInput[];
}

export interface UpdateOrderStatusRequest {
  status: string;
}

export interface Payment {
  id: number;
  orderId: number;
  totalPrice: number;
  paymentType: string;
  paymentDate: string;
  userId: number;
}

export interface CreatePaymentRequest {
  orderId: number;
}

export interface QueueTicket {
  id?: string | number;
  number?: string;
  type?: string;
  orderId?: number;
  status?: string;
  checkInTime?: string;
  productionLine?: "A" | "B" | string | null;
}

export interface QueueServingTicket {
  ticket: QueueTicket;
  counter: string;
  startedAt: string;
}

export interface QueueWaitingTicket {
  id: string;
  number: string;
  type: string;
  orderId: number;
  checkInTime: string;
  estimatedWaitMinutes: number;
}

export interface QueueStats {
  totalWaiting: number;
  averageWaitTime: number;
  activeCounters: number;
}

export interface QueueStatus {
  serving: QueueServingTicket[];
  waiting: QueueWaitingTicket[];
  stats: QueueStats;
  lastUpdated: string;
}

export type QueueType = "PRODUCTION" | "ACCOUNTING";

export interface PublicQueueTicket {
  number: string;
  calledAt?: string | null;
  orderId?: number | null;
  estimatedWaitMinutes?: number | null;
  productionLine?: "A" | "B" | string | null;
}

export interface PublicQueueDisplay {
  queueType: QueueType;
  queueLabel: string;
  nowServing?: PublicQueueTicket | null;
  nowServingLines?: PublicQueueTicket[];
  nextInLine: PublicQueueTicket[];
  totalWaiting: number;
  averageWaitMinutes: number;
  instruction: string;
  lastUpdated: string;
}

export interface StartProductionRequest {
  orderId: number;
  orderItemId: number;
}

export interface ProductionStage {
  id: number;
  name: string;
  stageType: string;
  container?: string;
  orderId: number;
  orderItemId: number;
  line: string;
  stageOrder: number;
  currentStatus: string;
}

export interface ProductionDashboardItem {
  line: string;
  stage: string;
  status: string;
  remainingMinutes: number;
  queue: number;
  eta: number;
  avgStageTime: number;
  throughputPerHour: number;
}

export interface ProductionEta {
  orderItemId: number;
  line: string;
  currentStage: string;
  remainingMinutes: number;
  queue: number;
  eta: number;
}

export interface ProductionBatch {
  batchId: string;
  orderId?: number | null;
  orderItemId: number;
  oliveWeightKg: number;
  status: string;
  predictedYieldPercent?: number | null;
  predictedOilKg?: number | null;
  predictionConfidence?: number | null;
  modelVersion?: string | null;
  latestImageId?: number | null;
  lastPredictedAt?: string | null;
  createdAt?: string;
  updatedAt?: string;
}

export type TrackingStatus = "REGISTERED" | "IN_PROGRESS" | "DONE";
export type TankCode = "A" | "B" | "C" | "D";

export interface TankTracking {
  code: TankCode;
  label: string;
  current: boolean;
}

export interface BatchTracking {
  batchId: string;
  trackingCode?: string | null;
  orderId?: number | null;
  orderItemId?: number | null;
  oliveWeightKg?: number | null;
  predictedOilKg?: number | null;
  status: TrackingStatus;
  statusLabel: string;
  progressPercent: number;
  estimatedTotalMinutes: number;
  estimatedRemainingMinutes: number;
  tankCode: TankCode;
  tankLabel: string;
  productionLine?: string | null;
  friendlyMessage: string;
  tanks: TankTracking[];
  registeredAt?: string | null;
  startedAt?: string | null;
  estimatedDoneAt?: string | null;
  completedAt?: string | null;
  updatedAt?: string | null;
}

export interface CreateProductionBatchRequest {
  orderId?: number;
  orderItemId: number;
  oliveWeightKg: number;
}

export interface StartStageRequest {
  stageId: number;
  userId: number;
}

export interface FinishStageRequest {
  stageId: number;
  employeeId: number;
}

export interface AiPredictionResponse {
  imageId: number;
  batchId: string;
  predictedYieldPercent: number;
  oliveWeightKg?: number;
  predictedOilKg?: number;
  confidence: number;
  modelVersion: string;
  featureValues?: Record<string, number>;
  rMean?: number;
  gMean?: number;
  bMean?: number;
  rmean?: number;
  gmean?: number;
  bmean?: number;
}

export interface AiHealthResponse {
  status: string;
  activeModelVersion: string | null;
  trainingSampleCount: number;
  storedImageCount: number;
}

export interface AiDatasetImageResponse {
  imageId: number;
  batchId: string;
  captureTime: string;
  segmentationSuccess: boolean;
  actualYieldPercent: number | null;
  predictedYieldPercent: number | null;
  predictionConfidence: number | null;
  modelVersion: string | null;
  anomalyFlag: boolean | null;
  featureValues?: Record<string, number>;
}

export interface AiTrainingJobResponse {
  jobId: string;
  state: string;
  statusUrl: string;
}

export interface AiTrainingStatusResponse {
  jobId: string;
  state: string;
  createdAt?: string;
  startedAt?: string;
  completedAt?: string;
  metrics?: unknown;
  errorMessage?: string | null;
}

export interface AuthSession {
  tokens: TokenBundle | null;
  claims: JwtClaims | null;
  authorities: string[];
  role: Role | null;
  profile: Profile | null;
}
