export const endpoints = {
  auth: {
    login: "/api/auth/login",
    refresh: "/api/auth/refresh",
    logout: "/api/auth/logout",
    setPassword: "/api/set-password"
  },
  profile: {
    me: "/api/employees/profile",
    update: "/api/employees/profile",
    changePassword: "/api/employees/profile/change-password"
  },
  employees: {
    list: "/api/employees",
    create: "/api/employees",
    byId: (id: number | string) => `/api/employees/${id}`,
    byNationalId: (nid: string) => `/api/employees/by-national-id/${nid}`
  },
  roles: {
    list: "/api/roles",
    create: "/api/roles",
    byId: (id: number | string) => `/api/roles/${id}`,
    addAuthorities: (id: number | string) => `/api/roles/${id}/authorities`,
    removeAuthority: (id: number | string, authority: string) => `/api/roles/${id}/authorities/${authority}`
  },
  authorities: {
    list: "/api/authorities",
    create: "/api/authorities",
    byId: (id: number | string) => `/api/authorities/${id}`
  },
  cities: {
    list: "/api/cities",
    create: "/api/cities",
    byId: (id: number | string) => `/api/cities/${id}`
  },
  customers: {
    list: "/api/customers",
    create: "/api/customers",
    byId: (id: number | string) => `/api/customers/${id}`,
    search: (nationalId: string) => `/api/customers/search/${nationalId}`,
    patchNationalId: (id: number | string) => `/api/customers/${id}/national-id`,
    membership: (id: number | string) => `/api/customers/${id}/membership`,
    byNationalId: (nationalId: string) => `/api/customers/by-national-id?nationalId=${encodeURIComponent(nationalId)}`
  },
  products: {
    list: "/api/products",
    create: "/api/products",
    byId: (id: number | string) => `/api/products/${id}`,
    inventory: (id: number | string) => `/api/products/${id}/inventory`,
    activate: (id: number | string) => `/api/products/${id}/activate`
  },
  orders: {
    create: "/api/orders",
    byId: (id: number | string) => `/api/orders/${id}`,
    byCustomer: (customerId: number | string) => `/api/orders?customerId=${customerId}`,
    byNationalId: (nationalId: string) =>
      `/api/orders/search/by-national-id?nationalId=${encodeURIComponent(nationalId)}`,
    status: (id: number | string) => `/api/orders/${id}/status`,
    items: (orderId: number | string) => `/api/orders/${orderId}/items`,
    item: (orderId: number | string, itemId: number | string) => `/api/orders/${orderId}/items/${itemId}`,
    itemStatus: (itemId: number | string) => `/api/orders/items/${itemId}/status`,
    statusValues: "/api/order-statuses/values"
  },
  payments: {
    list: "/api/payments",
    create: "/api/payments",
    byId: (id: number | string) => `/api/payments/${id}`,
    byOrder: (orderId: number | string) => `/api/payments/order/${orderId}`,
    dailyReport: "/api/payments/reports/daily/excel",
    periodReport: (from: string, to: string) =>
      `/api/payments/reports/period/excel?from=${encodeURIComponent(from)}&to=${encodeURIComponent(to)}`
  },
  queues: {
    issueAccounting: (orderId: number | string) => `/api/queues/accounting/tickets?orderId=${orderId}`,
    advance: (queueType: string, action: string) =>
      `/api/queues/${queueType}/advance?action=${encodeURIComponent(action)}`,
    status: (queueType: string) => `/api/queues/${queueType}/status`,
    display: (queueType: string) => `/api/public/queues/${queueType}/display`,
    issueProduction: (orderId: number | string) => `/api/queues/production/${orderId}`,
    ticketStatus: (ticketId: number | string, queueType: string, status: string, productionLine?: string) =>
      `/api/queues/tickets/${ticketId}/status?queueType=${encodeURIComponent(queueType)}&status=${encodeURIComponent(status)}${productionLine ? `&productionLine=${encodeURIComponent(productionLine)}` : ""}`,
    ticketStatusByOrder: (orderId: number | string, queueType: string, status: string, productionLine?: string) =>
      `/api/queues/tickets/status?orderId=${orderId}&queueType=${encodeURIComponent(queueType)}&status=${encodeURIComponent(status)}${productionLine ? `&productionLine=${encodeURIComponent(productionLine)}` : ""}`,
    tellerLogin: (queueType: string) => `/api/queues/teller/login?queueType=${encodeURIComponent(queueType)}`,
    tellerLogout: (queueType: string) => `/api/queues/teller/logout?queueType=${encodeURIComponent(queueType)}`
  },
  production: {
    start: "/api/production/start",
    changeStage: (itemId: number | string, stageType: string, container: string) =>
      `/api/production/items/${itemId}/change-stage?stageType=${encodeURIComponent(stageType)}&container=${encodeURIComponent(container)}`,
    orderStages: (orderItemId: number | string) => `/api/production/order/${orderItemId}`,
    pipeline: "/api/production/pipeline",
    stage: (id: number | string) => `/api/production/stage/${id}`,
    dashboard: "/api/production/dashboard",
    eta: (orderItemId: number | string) => `/api/production/eta/${orderItemId}`,
    batches: {
      create: "/api/production/batches",
      list: "/api/production/batches",
      byId: (batchId: string) => `/api/production/batches/${encodeURIComponent(batchId)}`,
      byOrderItem: (orderItemId: number | string) => `/api/production/batches/order-item/${orderItemId}`,
      byOrder: (orderId: number | string) => `/api/production/batches/order/${orderId}`
    }
  },
  tracking: {
    byBatch: (batchId: string) => `/api/tracking/batches/${encodeURIComponent(batchId)}`,
    byOrder: (orderId: number | string) => `/api/tracking/orders/${orderId}`,
    publicByCode: (trackingCode: string) => `/api/public/tracking/${encodeURIComponent(trackingCode)}`,
    status: (batchId: string) => `/api/tracking/batches/${encodeURIComponent(batchId)}/status`,
    tank: (batchId: string) => `/api/tracking/batches/${encodeURIComponent(batchId)}/tank`,
    line: (batchId: string) => `/api/tracking/batches/${encodeURIComponent(batchId)}/line`
  },
  ai: {
    predict: "/api/v1/predict",
    health: "/api/v1/health",
    uploadDataset: "/api/v1/dataset/images",
    updateYield: (batchId: string) => `/api/v1/dataset/${batchId}/yield`,
    train: "/api/v1/train",
    trainStatus: (jobId: string) => `/api/v1/train/${jobId}`
  }
} as const;
