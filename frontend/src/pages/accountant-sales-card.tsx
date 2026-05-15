import { useEffect, useMemo, useState } from "react";
import { useAuth } from "../app/auth-context";
import { useI18n } from "../app/i18n-context";
import { useToastHelpers } from "../app/toast-context";
import { canAction } from "../app/policy";
import { ActionButton, ErrorSummary, FieldError, PermissionNote } from "../components/form-ui";
import { Banner, Card } from "../components/ui";
import { endpoints } from "../lib/endpoints";
import { apiRequest } from "../lib/http";
import { useActionState } from "../lib/use-action-state";
import { formatCurrency, isOliveProductType } from "../lib/utils";
import { getApiErrorMessage, getApiFieldErrors, type FieldErrors } from "../lib/validation";
import type { Customer, Order, Payment, Product } from "../types/models";

function isAccountantSaleProduct(product: Product): boolean {
  const text = `${product.productName} ${product.productType} ${product.unit}`.toLowerCase();
  return !isOliveProductType(product.productType) && (
    text.includes("pomace") ||
    text.includes("gallon") ||
    text.includes("جفت") ||
    text.includes("جالون") ||
    text.includes("جالونات")
  );
}

function customerName(customer: Customer): string {
  return `${customer.firstName} ${customer.lastName}`.trim();
}

export function AccountantSalesCard(props: {
  onPaymentCreated?: () => Promise<void>;
}): JSX.Element {
  const auth = useAuth();
  const { language } = useI18n();
  const { success, error } = useToastHelpers();
  const isArabic = language === "ar";
  const actionState = useActionState();
  const [products, setProducts] = useState<Product[]>([]);
  const [customerLookup, setCustomerLookup] = useState("");
  const [resolvedCustomer, setResolvedCustomer] = useState<Customer | null>(null);
  const [productId, setProductId] = useState("");
  const [quantity, setQuantity] = useState("");
  const [fieldErrors, setFieldErrors] = useState<FieldErrors>({});
  const orderPermission = canAction(auth, "ORDER_CREATE");
  const paymentPermission = canAction(auth, "PAYMENT_CREATE");
  const customerReadPermission = canAction(auth, "CUSTOMER_READ");
  const customerSearchPermission = canAction(auth, "CUSTOMER_SEARCH");
  const productReadPermission = canAction(auth, "PRODUCT_READ");

  const saleProducts = useMemo(() => products.filter(isAccountantSaleProduct), [products]);
  const selectedProduct = saleProducts.find((product) => product.id === Number(productId));

  useEffect(() => {
    if (!productReadPermission.allowed) {
      return;
    }

    void apiRequest<Product[]>(endpoints.products.list)
      .then(setProducts)
      .catch((requestError: unknown) => error(getApiErrorMessage(requestError)));
  }, [productReadPermission.allowed]);

  const resolveCustomer = (): void => {
    if (!customerLookup.trim()) {
      setFieldErrors({ customerLookup: "Customer ID or National ID is required." });
      return;
    }

    void actionState.runAction("accounting-sale-customer", async () => {
      try {
        const lookup = customerLookup.trim();
        const customer = /^\d{9}$/.test(lookup)
          ? await apiRequest<Customer>(endpoints.customers.byNationalId(lookup))
          : await apiRequest<Customer>(endpoints.customers.byId(lookup));

        setResolvedCustomer(customer);
        setFieldErrors({});
        success(`Customer resolved: ${customerName(customer)} (#${customer.id}).`);
      } catch (requestError: unknown) {
        setResolvedCustomer(null);
        error(getApiErrorMessage(requestError));
      }
    });
  };

  const createSale = (): void => {
    if (!resolvedCustomer) {
      setFieldErrors({ customerLookup: "Resolve a customer before creating the sale." });
      return;
    }

    void actionState.runAction("accounting-product-sale", async () => {
      try {
        const order = await apiRequest<Order>(endpoints.orders.create, {
          method: "POST",
          body: {
            customerId: resolvedCustomer.id,
            items: [
              {
                productId: Number(productId),
                quantity: Number(quantity)
              }
            ]
          }
        });
        const payment = await apiRequest<Payment>(endpoints.payments.create, {
          method: "POST",
          body: { orderId: order.id }
        });

        await props.onPaymentCreated?.();
        setFieldErrors({});
        setProductId("");
        setQuantity("");
        success(
          isArabic
            ? `تم بيع المنتج للطلب #${order.id} وتسجيل دفعة بقيمة ${formatCurrency(payment.totalPrice)}.`
            : `Sale order #${order.id} was paid for ${formatCurrency(payment.totalPrice)}.`
        );
      } catch (requestError: unknown) {
        setFieldErrors(getApiFieldErrors(requestError));
        error(getApiErrorMessage(requestError));
      }
    });
  };

  return (
    <Card
      title={isArabic ? "بيع الجفت والجالونات" : "Pomace and Gallon Sales"}
      subtitle={isArabic ? "بيع المنتجات غير المرتبطة بدفعة الزيتون من شاشة المحاسب." : "Sell non-batch products from the accountant workspace."}
    >
      <ErrorSummary errors={fieldErrors} />
      <div className="form-grid two">
        <div className="field">
          <label>{isArabic ? "رقم العميل / الهوية" : "Customer ID / Identity Number"}</label>
          <input onChange={(event) => setCustomerLookup(event.target.value)} value={customerLookup} />
          <FieldError errors={fieldErrors} name="customerLookup" />
        </div>
        <div className="inline-actions">
          <ActionButton
            className="secondary"
            disabled={!customerReadPermission.allowed || !customerSearchPermission.allowed}
            disabledReason={customerReadPermission.reason ?? customerSearchPermission.reason}
            busyLabel={isArabic ? "جارٍ البحث..." : "Looking up..."}
            isBusy={actionState.isBusy("accounting-sale-customer")}
            onClick={resolveCustomer}
            type="button"
          >
            {isArabic ? "بحث" : "Lookup"}
          </ActionButton>
        </div>
        <div className="field">
          <label>{isArabic ? "المنتج" : "Product"}</label>
          <select onChange={(event) => setProductId(event.target.value)} value={productId}>
            <option value="">{isArabic ? "اختر المنتج" : "Select product"}</option>
            {saleProducts.map((product) => (
              <option key={product.id} value={product.id}>
                {product.productName}
              </option>
            ))}
          </select>
          <FieldError errors={fieldErrors} name="items[0].productId" />
          <FieldError errors={fieldErrors} name="items.0.productId" />
        </div>
        <div className="field">
          <label>{isArabic ? "الكمية" : "Quantity"}</label>
          <input onChange={(event) => setQuantity(event.target.value)} type="number" value={quantity} />
          <FieldError errors={fieldErrors} name="items[0].quantity" />
          <FieldError errors={fieldErrors} name="items.0.quantity" />
        </div>
      </div>
      {resolvedCustomer ? (
        <Banner tone="success" message={`${customerName(resolvedCustomer)} (#${resolvedCustomer.id})`} detail={resolvedCustomer.nationalId} />
      ) : null}
      {selectedProduct ? (
        <p className="helper-text">
          {isArabic ? "السعر الحالي" : "Current price"}: {formatCurrency(selectedProduct.price)} / {selectedProduct.unit}
        </p>
      ) : null}
      <div className="inline-actions">
        <ActionButton
          disabled={!orderPermission.allowed || !paymentPermission.allowed || !resolvedCustomer}
          disabledReason={!resolvedCustomer ? "Resolve a customer before creating the sale." : orderPermission.reason ?? paymentPermission.reason}
          busyLabel={isArabic ? "جارٍ البيع..." : "Selling..."}
          isBusy={actionState.isBusy("accounting-product-sale")}
          onClick={createSale}
          type="button"
        >
          {isArabic ? "بيع وتسجيل دفعة" : "Sell and record payment"}
        </ActionButton>
      </div>
      <PermissionNote allowed={orderPermission.allowed} reason={orderPermission.reason} />
      <PermissionNote allowed={paymentPermission.allowed} reason={paymentPermission.reason} />
      <PermissionNote allowed={productReadPermission.allowed} reason={productReadPermission.reason} />
    </Card>
  );
}
