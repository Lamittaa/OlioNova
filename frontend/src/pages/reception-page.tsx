import { useEffect, useMemo, useState } from "react";
import { Link } from "react-router-dom";
import { useAuth } from "../app/auth-context";
import { useI18n } from "../app/i18n-context";
import { useToastHelpers } from "../app/toast-context";
import { canAction } from "../app/policy";
import { ActionButton, FieldError, PermissionNote } from "../components/form-ui";
import { Card } from "../components/ui";
import { endpoints } from "../lib/endpoints";
import { apiRequest } from "../lib/http";
import { useActionState } from "../lib/use-action-state";
import { asErrorMessage, findOliveProductForCustomer, formatDateTime } from "../lib/utils";
import { getApiErrorMessage, getApiFieldErrors, type FieldErrors } from "../lib/validation";
import type { BatchTracking, City, Customer, CustomerInput, Order, OrderItemInput, Product } from "../types/models";

function emptyCustomerDraft(nationalId = ""): CustomerInput {
  return {
    nationalId,
    firstName: "",
    lastName: "",
    phoneNumber: "",
    cityId: undefined,
    isMember: false
  };
}

function customerName(customer: Customer): string {
  return `${customer.firstName} ${customer.lastName}`.trim();
}

function initials(customer: Customer): string {
  const first = customer.firstName?.[0] ?? "";
  const last = customer.lastName?.[0] ?? "";
  return `${first}${last}`.toUpperCase() || "C";
}

const OLIVE_TYPE_OPTIONS = {
  ar: ["بلدي", "فرنجي", "مخلوط"],
  en: ["Baladi", "Frenji", "Mixed"]
} as const;

function matchesCustomer(customer: Customer, term: string): boolean {
  const needle = term.trim().toLowerCase();
  if (!needle) {
    return false;
  }

  return [
    customer.nationalId,
    customer.phoneNumber,
    customer.firstName,
    customer.lastName,
    customerName(customer)
  ].some((value) => value?.toLowerCase().includes(needle));
}

export function ReceptionPage(): JSX.Element {
  const auth = useAuth();
  const { language } = useI18n();
  const { success, error } = useToastHelpers();
  const actionState = useActionState();
  const isArabic = language === "ar";
  const copy = isArabic
    ? {
        pageTitle: "استقبال العملاء",
        pageDescription: "تسجيل العملاء وإصدار طلبات العصر والتذاكر",
        customerArrival: "وصول العميل",
        searchPlaceholder: "ابحث برقم الهوية أو الهاتف أو الاسم...",
        newCustomer: "عميل جديد",
        noCustomerTitle: "اختر عميلا لبدء العمل",
        noCustomerText: "ابحث عن العميل أو سجله من نفس الشاشة.",
        selectedCustomer: "تم اختيار العميل",
        totalOrders: "إجمالي الطلبات",
        workflow: "مسار الاستقبال",
        createOrder: "إنشاء طلب جديد",
        latestBatch: "آخر دفعة",
        recentOrders: "الطلبات الأخيرة",
        noRecentOrders: "لا توجد طلبات محملة لهذا العميل.",
        registerCustomer: "تسجيل العميل",
        cancel: "إلغاء",
        customerRegistration: "تسجيل عميل جديد",
        firstName: "الاسم الأول",
        lastName: "اسم العائلة",
        nationalId: "رقم الهوية",
        phoneNumber: "رقم الهاتف",
        city: "المدينة",
        selectCity: "اختر المدينة",
        service: "الخدمة",
        quantity: "الوزن / الكمية",
        oliveType: "نوع الزيتون",
        bagsCount: "عدد الأكياس",
        issueProductionTicket: "إصدار تذكرة الإنتاج",
        issueAccountingTicket: "إصدار تذكرة المحاسبة",
        trackingCode: "كود التتبع",
        status: "الحالة",
        items: "العناصر",
        created: "تاريخ الإنشاء",
        member: "مساهم",
        standard: "غير مساهم",
        orders: "طلبات",
        noResults: "لا يوجد عميل مطابق. يمكنك تسجيله هنا.",
        pageLoadFailed: "فشل تحميل صفحة الاستقبال",
        customerSaved: "تم تسجيل العميل",
        customerSaveFailed: "فشل حفظ العميل",
        orderCreated: "تم إنشاء الطلب",
        batchCreationFailed: "فشل إنشاء الطلب",
        trackingReady: (code: string) => `كود التتبع: ${code}`,
        trackingPending: "كود التتبع غير جاهز بعد.",
        ticketIssued: "تم إصدار التذكرة",
        ticketFailed: "فشل إصدار التذكرة",
        creating: "جار الإنشاء...",
        saving: "جار الحفظ...",
        issuing: "جار الإصدار..."
      }
    : {
        pageTitle: "Customer Reception",
        pageDescription: "Register customers and issue queue tickets",
        customerArrival: "Customer Arrival",
        searchPlaceholder: "Search by National ID, Phone, or Name...",
        newCustomer: "New Customer",
        noCustomerTitle: "Search and select a customer",
        noCustomerText: "Search and select a customer to begin the reception workflow.",
        selectedCustomer: "Customer Selected",
        totalOrders: "Total Orders",
        workflow: "Reception Workflow",
        createOrder: "Create New Order",
        latestBatch: "Latest Batch",
        recentOrders: "Recent Orders",
        noRecentOrders: "No orders loaded for this customer.",
        registerCustomer: "Register Customer",
        cancel: "Cancel",
        customerRegistration: "New Customer Registration",
        firstName: "First Name",
        lastName: "Last Name",
        nationalId: "National ID",
        phoneNumber: "Phone Number",
        city: "City",
        selectCity: "Select city",
        service: "Service",
        quantity: "Weight / Quantity",
        oliveType: "Olive Type",
        bagsCount: "Bags Count",
        issueProductionTicket: "Issue Production Queue Ticket",
        issueAccountingTicket: "Issue Accounting Queue Ticket",
        trackingCode: "Tracking code",
        status: "Status",
        items: "Items",
        created: "Created",
        member: "Member",
        standard: "Non-member",
        orders: "orders",
        noResults: "No matching customer. Register them here.",
        pageLoadFailed: "Reception page load failed",
        customerSaved: "Customer registered",
        customerSaveFailed: "Customer save failed",
        orderCreated: "Order created",
        batchCreationFailed: "Batch creation failed",
        trackingReady: (code: string) => `Tracking code: ${code}`,
        trackingPending: "Tracking code is not ready yet.",
        ticketIssued: "Ticket issued",
        ticketFailed: "Ticket issue failed",
        creating: "Creating...",
        saving: "Saving...",
        issuing: "Issuing..."
      };

  const [products, setProducts] = useState<Product[]>([]);
  const [cities, setCities] = useState<City[]>([]);
  const [customers, setCustomers] = useState<Customer[]>([]);
  const [searchTerm, setSearchTerm] = useState("");
  const [selectedCustomer, setSelectedCustomer] = useState<Customer | null>(null);
  const [customerOrders, setCustomerOrders] = useState<Order[]>([]);
  const [showCustomerForm, setShowCustomerForm] = useState(false);
  const [quantity, setQuantity] = useState("");
  const [oliveType, setOliveType] = useState("");
  const [bagsCount, setBagsCount] = useState("");
  const [createdOrder, setCreatedOrder] = useState<Order | null>(null);
  const [createdTracking, setCreatedTracking] = useState<BatchTracking | null>(null);
  const [fieldErrors, setFieldErrors] = useState<FieldErrors>({});
  const [customerDraft, setCustomerDraft] = useState<CustomerInput>(() => emptyCustomerDraft());
  const [customerCityText, setCustomerCityText] = useState("");

  const orderPermission = canAction(auth, "ORDER_CREATE");
  const orderReadPermission = canAction(auth, "ORDER_READ");
  const customerReadPermission = canAction(auth, "CUSTOMER_READ");
  const customerCreatePermission = canAction(auth, "CUSTOMER_CREATE");
  const cityCreatePermission = canAction(auth, "CITY_CREATE");
  const productReadPermission = canAction(auth, "PRODUCT_READ");
  const cityReadPermission = canAction(auth, "CITY_READ");
  const canUsePage = orderPermission.allowed && productReadPermission.allowed && customerReadPermission.allowed;

  const pressingProduct = useMemo(
    () => findOliveProductForCustomer(products, selectedCustomer),
    [products, selectedCustomer]
  );

  const filteredCustomers = useMemo(
    () => customers.filter((customer) => matchesCustomer(customer, searchTerm)).slice(0, 8),
    [customers, searchTerm]
  );

  const cityNameById = (cityId: number): string => cities.find((city) => city.id === cityId)?.cityName ?? `#${cityId}`;
  const oliveTypeOptions = OLIVE_TYPE_OPTIONS[language];

  const updateCustomerCityText = (value: string): void => {
    const matchingCity = cities.find((city) => city.cityName.trim().toLowerCase() === value.trim().toLowerCase());
    setCustomerCityText(value);
    setCustomerDraft((current) => ({ ...current, cityId: matchingCity?.id }));
  };

  const resolveCustomerCityId = async (): Promise<number> => {
    const cityName = customerCityText.trim();

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

  useEffect(() => {
    async function load(): Promise<void> {
      const requests: Promise<void>[] = [];

      if (productReadPermission.allowed) {
        requests.push(apiRequest<Product[]>(endpoints.products.list).then(setProducts));
      }

      if (cityReadPermission.allowed) {
        requests.push(apiRequest<City[]>(endpoints.cities.list).then(setCities));
      }

      if (customerReadPermission.allowed) {
        requests.push(apiRequest<Customer[]>(endpoints.customers.list).then(setCustomers));
      }

      await Promise.all(requests);
    }

    void load().catch((requestError: unknown) => {
      error(copy.pageLoadFailed, asErrorMessage(requestError));
    });
  }, [productReadPermission.allowed, cityReadPermission.allowed, customerReadPermission.allowed]);

  const resetBatchForm = (): void => {
    setQuantity("");
    setOliveType("");
    setBagsCount("");
  };

  const loadOrdersForCustomer = async (customerId: number): Promise<void> => {
    if (!orderReadPermission.allowed) {
      setCustomerOrders([]);
      return;
    }

    try {
      setCustomerOrders(await apiRequest<Order[]>(endpoints.orders.byCustomer(customerId)));
    } catch {
      setCustomerOrders([]);
    }
  };

  const selectCustomer = (customer: Customer): void => {
    setSelectedCustomer(customer);
    setSearchTerm(customer.nationalId);
    setShowCustomerForm(false);
    setCustomerDraft(emptyCustomerDraft());
    setFieldErrors({});
    void loadOrdersForCustomer(customer.id);
  };

  const openCustomerCreation = (): void => {
    const trimmed = searchTerm.trim();
    setCustomerDraft(emptyCustomerDraft(/^\d+$/.test(trimmed) ? trimmed : ""));
    setCustomerCityText("");
    setShowCustomerForm(true);
    setFieldErrors({});
  };

  const buildBatchItem = (): OrderItemInput | null => {
    if (!pressingProduct) {
      setFieldErrors({ productId: "No olive pressing service product is configured." });
      return null;
    }

    return {
      productId: pressingProduct.id,
      quantity: Number(quantity),
      oliveType,
      bagsCount: Number(bagsCount)
    };
  };

  const validateReceptionOrder = (): FieldErrors => {
    const errors: FieldErrors = {};
    const parsedQuantity = Number(quantity);
    const parsedBagsCount = Number(bagsCount);

    if (!quantity.trim() || !Number.isFinite(parsedQuantity) || parsedQuantity <= 0) {
      errors["items.0.quantity"] = isArabic ? "وزن الزيتون مطلوب." : "Olive weight is required.";
    }
    if (!oliveType.trim()) {
      errors["items.0.oliveType"] = isArabic ? "نوع الزيتون مطلوب." : "Olive type is required.";
    }
    if (!bagsCount.trim() || !Number.isFinite(parsedBagsCount) || parsedBagsCount <= 0) {
      errors["items.0.bagsCount"] = isArabic ? "عدد الأكياس مطلوب." : "Bags count is required.";
    }

    return errors;
  };

  const validateReceptionCustomer = (): FieldErrors => {
    const errors: FieldErrors = {};

    if (!customerDraft.firstName?.trim()) {
      errors.firstName = isArabic ? "الاسم الأول مطلوب." : "First name is required.";
    }
    if (!customerDraft.lastName?.trim()) {
      errors.lastName = isArabic ? "اسم العائلة مطلوب." : "Last name is required.";
    }
    if (!customerDraft.nationalId?.trim()) {
      errors.nationalId = isArabic ? "رقم الهوية مطلوب." : "National ID is required.";
    }
    if (!customerDraft.phoneNumber?.trim()) {
      errors.phoneNumber = isArabic ? "رقم الهاتف مطلوب." : "Phone number is required.";
    }
    if (!customerCityText.trim()) {
      errors.cityId = isArabic ? "المدينة مطلوبة." : "City is required.";
    }

    return errors;
  };

  const prepareCustomerTracking = async (orderId: number): Promise<BatchTracking | null> => {
    try {
      await apiRequest(endpoints.production.batches.byOrder(orderId), { method: "POST" });
      return await apiRequest<BatchTracking>(endpoints.tracking.byOrder(orderId));
    } catch {
      return null;
    }
  };

  const createOrderForCustomer = async (customer: Customer): Promise<{ order: Order; tracking: BatchTracking | null }> => {
    const item = buildBatchItem();
    if (!item) {
      throw new Error("No olive pressing service product is configured.");
    }

    const order = await apiRequest<Order>(endpoints.orders.create, {
      method: "POST",
      body: {
        customerId: customer.id,
        items: [item]
      }
    });

    setCreatedOrder(order);
    setCreatedTracking(null);
    setCustomerOrders((current) => [order, ...current.filter((entry) => entry.id !== order.id)]);
    const tracking = await prepareCustomerTracking(order.id);
    setCreatedTracking(tracking);
    setFieldErrors({});
    resetBatchForm();
    success(copy.orderCreated, tracking?.trackingCode ? copy.trackingReady(tracking.trackingCode) : copy.trackingPending);
    return { order, tracking };
  };

  const createSelectedCustomerOrder = (): void => {
    if (!selectedCustomer) {
      return;
    }

    const nextErrors = validateReceptionOrder();
    setFieldErrors(nextErrors);
    if (Object.keys(nextErrors).length) {
      return;
    }

    void actionState.runAction("reception-create-batch", async () => {
      try {
        await createOrderForCustomer(selectedCustomer);
      } catch (requestError: unknown) {
        setFieldErrors(getApiFieldErrors(requestError));
        error(copy.batchCreationFailed, getApiErrorMessage(requestError));
      }
    });
  };

  const createCustomer = (): void => {
    const nextErrors = validateReceptionCustomer();
    setFieldErrors(nextErrors);
    if (Object.keys(nextErrors).length) {
      return;
    }

    void actionState.runAction("reception-create-customer", async () => {
      try {
        const cityId = await resolveCustomerCityId();
        const customer = await apiRequest<Customer>(endpoints.customers.create, {
          method: "POST",
          body: {
            ...customerDraft,
            cityId
          }
        });

        setCustomers((current) => [customer, ...current.filter((entry) => entry.id !== customer.id)]);
        setShowCustomerForm(false);
        setCustomerCityText("");
        selectCustomer(customer);
        success(copy.customerSaved, customerName(customer));
      } catch (requestError: unknown) {
        setFieldErrors(getApiFieldErrors(requestError));
        error(copy.customerSaveFailed, getApiErrorMessage(requestError));
      }
    });
  };

  return (
    <div className="reception-page">
      <header className="reception-header">
        <h1>{copy.pageTitle}</h1>
      </header>

      {canUsePage ? (
        <div className="reception-layout">
          <section className="reception-left">
            <div className="reception-card">
              <h2>{copy.customerArrival}</h2>
              <div className="reception-search">
                <span aria-hidden="true">⌕</span>
                <input
                  onChange={(event) => setSearchTerm(event.target.value)}
                  placeholder={copy.searchPlaceholder}
                  value={searchTerm}
                />
              </div>
              <button className="reception-primary-button" onClick={openCustomerCreation} type="button">
                <span>+</span>
                {copy.newCustomer}
              </button>

              {searchTerm.trim() && !showCustomerForm ? (
                <div className="reception-results">
                  {filteredCustomers.length ? (
                    filteredCustomers.map((customer) => (
                      <button
                        className={`reception-customer-row ${selectedCustomer?.id === customer.id ? "active" : ""}`}
                        key={customer.id}
                        onClick={() => selectCustomer(customer)}
                        type="button"
                      >
                        <span className="reception-customer-avatar">{initials(customer)}</span>
                        <span>
                          <strong>{customerName(customer)}</strong>
                          <small>{customer.nationalId}</small>
                          <small>{customer.phoneNumber}</small>
                          <small>{cityNameById(customer.cityId)}</small>
                        </span>
                        <em>{customer.isMember ? copy.member : copy.standard}</em>
                      </button>
                    ))
                  ) : null}
                </div>
              ) : null}
            </div>

            {showCustomerForm ? (
              <div className="reception-card">
                <h2>{copy.customerRegistration}</h2>
                <div className="form-grid two reception-form-grid">
                  <div className="field">
                    <label>{copy.firstName}</label>
                    <input
                      onChange={(event) => setCustomerDraft((current) => ({ ...current, firstName: event.target.value }))}
                      required
                      value={customerDraft.firstName ?? ""}
                    />
                    <FieldError errors={fieldErrors} name="firstName" />
                  </div>
                  <div className="field">
                    <label>{copy.lastName}</label>
                    <input
                      onChange={(event) => setCustomerDraft((current) => ({ ...current, lastName: event.target.value }))}
                      required
                      value={customerDraft.lastName ?? ""}
                    />
                    <FieldError errors={fieldErrors} name="lastName" />
                  </div>
                  <div className="field">
                    <label>{copy.nationalId}</label>
                    <input
                      onChange={(event) => setCustomerDraft((current) => ({ ...current, nationalId: event.target.value }))}
                      required
                      value={customerDraft.nationalId ?? ""}
                    />
                    <FieldError errors={fieldErrors} name="nationalId" />
                  </div>
                  <div className="field">
                    <label>{copy.phoneNumber}</label>
                    <input
                      onChange={(event) => setCustomerDraft((current) => ({ ...current, phoneNumber: event.target.value }))}
                      required
                      value={customerDraft.phoneNumber ?? ""}
                    />
                    <FieldError errors={fieldErrors} name="phoneNumber" />
                  </div>
                  <div className="field span-2">
                    <label>{copy.city}</label>
                    <input
                      list="reception-city-options"
                      onChange={(event) => updateCustomerCityText(event.target.value)}
                      placeholder={copy.selectCity}
                      required
                      value={customerCityText}
                    />
                    <datalist id="reception-city-options">
                      {cities.map((city) => (
                        <option key={city.id} value={city.cityName} />
                      ))}
                    </datalist>
                    <FieldError errors={fieldErrors} name="cityId" />
                  </div>
                </div>
                <div className="reception-split-actions">
                  <button className="btn ghost" onClick={() => setShowCustomerForm(false)} type="button">
                    {copy.cancel}
                  </button>
                  <ActionButton
                    disabled={!customerCreatePermission.allowed}
                    disabledReason={customerCreatePermission.reason}
                    busyLabel={copy.saving}
                    isBusy={actionState.isBusy("reception-create-customer")}
                    onClick={createCustomer}
                    type="button"
                  >
                    {copy.registerCustomer}
                  </ActionButton>
                </div>
                <PermissionNote allowed={customerCreatePermission.allowed} reason={customerCreatePermission.reason} />
              </div>
            ) : null}
          </section>

          <section className="reception-right">
            {selectedCustomer ? (
              <>
                <div className="reception-card">
                  <h2 className="reception-selected-title">
                    <span>✓</span>
                    {copy.selectedCustomer}
                  </h2>
                  <div className="reception-selected-card">
                    <strong>{customerName(selectedCustomer)}</strong>
                    <span>{selectedCustomer.nationalId}</span>
                    <span>{selectedCustomer.phoneNumber}</span>
                    <span>{cityNameById(selectedCustomer.cityId)}</span>
                    <footer>
                      {copy.totalOrders}: {customerOrders.length}
                    </footer>
                  </div>
                </div>

                <div className="reception-card">
                  <h2>{copy.workflow}</h2>
                  <div className="reception-order-form">
                    <div className="reception-order-title">
                      <strong>{copy.createOrder}</strong>
                      <span>+</span>
                    </div>
                    <div className="form-grid two reception-form-grid">
                      <div className="field">
                        <label>{copy.service}</label>
                        <input disabled value={isArabic ? "عصر زيتون" : "Olive pressing"} />
                        <FieldError errors={fieldErrors} name="productId" />
                        <FieldError errors={fieldErrors} name="items[0].productId" />
                        <FieldError errors={fieldErrors} name="items.0.productId" />
                      </div>
                      <div className="field">
                        <label>{copy.quantity}</label>
                        <input min={1} onChange={(event) => setQuantity(event.target.value)} required type="number" value={quantity} />
                        <FieldError errors={fieldErrors} name="items[0].quantity" />
                        <FieldError errors={fieldErrors} name="items.0.quantity" />
                      </div>
                      <div className="field">
                        <label>{copy.oliveType}</label>
                        <input
                          list="reception-olive-type-options"
                          onChange={(event) => setOliveType(event.target.value)}
                          required
                          value={oliveType}
                        />
                        <datalist id="reception-olive-type-options">
                          {oliveTypeOptions.map((type) => (
                            <option key={type} value={type} />
                          ))}
                        </datalist>
                        <FieldError errors={fieldErrors} name="items[0].oliveType" />
                        <FieldError errors={fieldErrors} name="items.0.oliveType" />
                      </div>
                      <div className="field">
                        <label>{copy.bagsCount}</label>
                        <input min={1} onChange={(event) => setBagsCount(event.target.value)} required type="number" value={bagsCount} />
                        <FieldError errors={fieldErrors} name="items[0].bagsCount" />
                        <FieldError errors={fieldErrors} name="items.0.bagsCount" />
                      </div>
                    </div>
                    <ActionButton
                      disabled={!orderPermission.allowed}
                      disabledReason={orderPermission.reason}
                      busyLabel={copy.creating}
                      isBusy={actionState.isBusy("reception-create-batch")}
                      onClick={createSelectedCustomerOrder}
                      type="button"
                    >
                      {copy.createOrder}
                    </ActionButton>
                  </div>

                </div>

                {createdOrder ? (
                  <div className="reception-card">
                    <h2>{copy.latestBatch}</h2>
                    <div className="reception-latest-grid">
                      <div>
                        <strong>Order #{createdOrder.id}</strong>
                        <span>{createdOrder.createdAt ? formatDateTime(createdOrder.createdAt) : "--"}</span>
                      </div>
                      <div><strong>{createdOrder.status}</strong><span>{copy.status}</span></div>
                      <div>
                        <strong>
                          {createdTracking?.trackingCode ? (
                            <Link to={`/track?code=${encodeURIComponent(createdTracking.trackingCode)}`}>
                              {createdTracking.trackingCode}
                            </Link>
                          ) : (
                            "--"
                          )}
                        </strong>
                        <span>{copy.trackingCode}</span>
                      </div>
                      <div><strong>{createdOrder.items.length}</strong><span>{copy.items}</span></div>
                    </div>
                  </div>
                ) : null}

                <div className="reception-card">
                  <h2>{copy.recentOrders}</h2>
                  {customerOrders.length ? (
                    <div className="reception-order-list">
                      {customerOrders.slice(0, 4).map((order) => (
                        <div className="reception-order-row" key={order.id}>
                          <span>
                            <strong>Order #{order.id}</strong>
                            <small>{order.createdAt ? formatDateTime(order.createdAt) : "--"}</small>
                          </span>
                          <em>{order.status}</em>
                        </div>
                      ))}
                    </div>
                  ) : (
                    <div className="reception-empty-small">{copy.noRecentOrders}</div>
                  )}
                </div>
              </>
            ) : (
              <div className="reception-card reception-empty-panel">
                <div aria-hidden="true" className="reception-empty-icon" />
                <h2>{copy.noCustomerTitle}</h2>
                <p>{copy.noCustomerText}</p>
              </div>
            )}
          </section>
        </div>
      ) : (
        <Card>
          <PermissionNote allowed={orderPermission.allowed} reason={orderPermission.reason} />
          <PermissionNote allowed={productReadPermission.allowed} reason={productReadPermission.reason} />
          <PermissionNote allowed={customerReadPermission.allowed} reason={customerReadPermission.reason} />
        </Card>
      )}
    </div>
  );
}
