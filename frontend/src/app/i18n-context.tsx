import {
  createContext,
  useContext,
  useEffect,
  useMemo,
  useState,
  type PropsWithChildren
} from "react";

export type AppLanguage = "en" | "ar";
type AppDirection = "ltr" | "rtl";

type Translator = (value: string | null | undefined) => string;

interface I18nContextValue {
  language: AppLanguage;
  direction: AppDirection;
  setLanguage: (language: AppLanguage) => void;
  t: Translator;
}

const LANGUAGE_STORAGE_KEY = "ops.language.v2";

const translations: Record<string, string> = {
  "Loading session...": "جاري تحميل الجلسة...",
  Overview: "نظرة عامة",
  Profile: "الملف الشخصي",
  Customers: "العملاء",
  Cities: "المدن",
  Products: "المنتجات",
  Orders: "الطلبات",
  Invoice: "الفاتورة",
  Payments: "المدفوعات",
  Reports: "التقارير",
  Queue: "الطابور",
  "Queue Control": "التحكم بالأدوار",
  Production: "الإنتاج",
  "Admin Workspace": "مساحة الإدارة",
  "Olive Press Management": "إدارة معصرة الزيتون",
  "Gateway-first operations console aligned to the backend contracts.": "لوحة تشغيل تعتمد على البوابة ومتوافقة مع عقود الواجهة الخلفية.",
  "Unknown role": "دور غير معروف",
  "No profile loaded": "لم يتم تحميل الملف الشخصي",
  "Current Gateway": "البوابة الحالية",
  English: "English",
  Arabic: "العربية",
  Logout: "تسجيل الخروج",
  "Please correct the highlighted fields.": "يرجى تصحيح الحقول المميزة.",
  "Working...": "جارٍ التنفيذ...",
  "Sign In": "تسجيل الدخول",
  "Use the seeded demo accounts or your assigned employee credentials.": "استخدم الحسابات التجريبية المزروعة أو بيانات الموظف المخصصة لك.",
  Username: "اسم المستخدم",
  Password: "كلمة المرور",
  "Signing in...": "جارٍ تسجيل الدخول...",
  "Sign in": "تسجيل الدخول",
  "Seed users:": "المستخدمون التجريبيون:",
  "Need first-time setup?": "هل تحتاج إلى إعداد أول مرة؟",
  "Open set-password flow": "افتح مسار تعيين كلمة المرور",
  "Set Password": "تعيين كلمة المرور",
  "Complete first-time setup or password reset using a valid token.": "أكمل الإعداد الأولي أو إعادة تعيين كلمة المرور باستخدام رمز صالح.",
  Token: "الرمز",
  "New Password": "كلمة المرور الجديدة",
  "Confirm Password": "تأكيد كلمة المرور",
  "Submitting...": "جارٍ الإرسال...",
  Submit: "إرسال",
  "Back to sign in": "العودة إلى تسجيل الدخول",
  Workspace: "مساحة العمل",
  "Operations Overview": "نظرة عامة على العمليات",
  "A role-aware snapshot of the most important business flow touchpoints.": "لقطة سريعة واعية بالدور لأهم نقاط سير العمل.",
  "Signed-in Role": "الدور الحالي",
  Employee: "الموظف",
  Authorities: "الصلاحيات",
  "Derived from JWT claims": "مستخرجة من JWT",
  "Payment Signal": "مؤشر المدفوعات",
  "Latest finance activity visible to this role.": "أحدث نشاط مالي ظاهر لهذا الدور.",
  "Payments Loaded": "المدفوعات المحملة",
  "Latest Payment": "أحدث دفعة",
  "No payments yet": "لا توجد مدفوعات بعد",
  "No finance snapshot": "لا توجد لقطة مالية",
  "This role may not have payment visibility yet, or there are no payments in the system.": "قد لا يملك هذا الدور صلاحية رؤية المدفوعات بعد، أو لا توجد مدفوعات في النظام.",
  "Production Signal": "مؤشر الإنتاج",
  "Operational dashboard data routed from the production service.": "بيانات لوحة التشغيل الواردة من خدمة الإنتاج.",
  "Lines Tracked": "الخطوط المتابعة",
  "Average Queue": "متوسط الطابور",
  "Fastest Remaining Minutes": "أقل دقائق متبقية",
  "No production dashboard": "لا توجد لوحة إنتاج",
  "Technicians can still use the production page even when dashboard access is limited.": "يمكن للفنيين استخدام صفحة الإنتاج حتى عند تقييد الوصول إلى اللوحة.",
  "Profile Settings": "إعدادات الملف الشخصي",
  Shared: "مشترك",
  "Review and update your personal details, then rotate credentials safely.": "راجع بياناتك الشخصية وحدّثها ثم غيّر بيانات الاعتماد بأمان.",
  "My Profile": "ملفي الشخصي",
  "View and update the authenticated employee profile.": "اعرض ملف الموظف الحالي وحدّث بياناته.",
  "Loaded from /api/employees/profile with VIEW_PROFILE guard.": "تم التحميل من /api/employees/profile مع حماية VIEW_PROFILE.",
  "First Name": "الاسم الأول",
  "Last Name": "اسم العائلة",
  Email: "البريد الإلكتروني",
  "Phone Number": "رقم الهاتف",
  "Mobile Number": "رقم الجوال",
  City: "المدينة",
  "Select marital status": "اختر الحالة الاجتماعية",
  SINGLE: "أعزب",
  MARRIED: "متزوج",
  DIVORCED: "مطلق",
  WIDOWED: "أرمل",
  "Saving...": "جارٍ الحفظ...",
  "Save profile": "حفظ الملف الشخصي",
  "Password Change": "تغيير كلمة المرور",
  "Change Password": "تغيير كلمة المرور",
  "Requires current password plus a policy-compliant new password.": "يتطلب كلمة المرور الحالية وكلمة مرور جديدة متوافقة مع السياسة.",
  "Requires the current password and a new password of at least 8 characters.": "يتطلب كلمة المرور الحالية وكلمة مرور جديدة من 8 أحرف على الأقل.",
  "Current Password": "كلمة المرور الحالية",
  "Confirm New Password": "تأكيد كلمة المرور الجديدة",
  "Updating...": "جارٍ التحديث...",
  "Update password": "تحديث كلمة المرور",
  "City Registry": "سجل المدن",
  "Read, create, update, and delete cities according to role permissions.": "اقرأ المدن وأنشئها وحدّثها واحذفها حسب صلاحيات الدور.",
  "Create and manage city records.": "أنشئ المدن وأدر بياناتها.",
  "Create City": "إنشاء مدينة",
  Name: "الاسم",
  Create: "إنشاء",
  "City List": "قائمة المدن",
  "Customer Registry": "سجل العملاء",
  "Search, create, and maintain customer records before creating orders.": "ابحث عن العملاء وأنشئ سجلاتهم وحدّثها قبل إنشاء الطلبات.",
  "Create, find, and update customers with a simple national ID workflow.": "أنشئ العملاء وابحث عنهم وحدّث بياناتهم بمسار بسيط يعتمد على الرقم الوطني.",
  "Create Customer": "إنشاء عميل",
  "National ID": "الرقم الوطني",
  Search: "بحث",
  "Customer List": "قائمة العملاء",
  "Product Catalog": "كتالوج المنتجات",
  "Maintain products and inventory from the gateway-only contract surface.": "أدر المنتجات والمخزون من خلال عقد البوابة فقط.",
  "Create Product": "إنشاء منتج",
  Type: "النوع",
  Price: "السعر",
  Quantity: "الكمية",
  Inventory: "المخزون",
  Active: "نشط",
  Inactive: "غير نشط",
  "Order Flow": "سير الطلبات",
  "Resolve the customer, then create and manage the full order in one place.": "حدّد العميل ثم أنشئ الطلب وأدره بالكامل من مكان واحد.",
  "Find the customer, then create and manage the order in one place.": "حدّد العميل ثم أنشئ الطلب وأدره من مكان واحد.",
  "Create Order": "إنشاء طلب",
  "Front desk order intake.": "إدخال طلبات الاستقبال.",
  "Order entry.": "إدخال الطلب.",
  "Customer National ID": "الرقم الوطني للعميل",
  "Resolving...": "جارٍ تحديد العميل...",
  "Resolve customer": "تحديد العميل",
  "Customer resolved": "تم تحديد العميل",
  "Find an order by order number, customer number, or national ID.": "اعثر على الطلب برقم الطلب أو رقم العميل أو الرقم الوطني.",
  "Order ID": "رقم الطلب",
  "Customer ID": "معرف العميل",
  "National ID Search": "بحث بالرقم الوطني",
  "Search results": "نتائج البحث",
  "No orders found": "لم يتم العثور على طلبات",
  "Payment Operations": "عمليات الدفع",
  "Record payments and continue olive orders into the queue.": "سجّل المدفوعات وواصل طلبات الزيتون إلى الطابور.",
  "Record payments and continue olive orders.": "سجّل المدفوعات وواصل طلبات الزيتون.",
  "Record payment for an order and continue the workflow.": "سجّل دفعة للطلب وواصل سير العمل.",
  "Record payment for an order.": "سجّل دفعة لطلب.",
  "Queue update": "تحديث الطابور",
  "Some item actions are not available for your account.": "بعض إجراءات العناصر غير متاحة لحسابك.",
  "Queue Operations": "عمليات الطابور",
  "Monitor queue status and run the available actions.": "تابع حالة الطابور ونفّذ الإجراءات المتاحة.",
  "Queue Actions": "إجراءات الطابور",
  "Serving": "قيد الخدمة",
  "Waiting": "في الانتظار",
  "Average Wait": "متوسط الانتظار",
  "Idle": "لا يوجد",
  min: "دقيقة",
  "Advance queue": "تحريك الطابور",
  "No items loaded": "لا توجد عناصر محمّلة",
  "This order does not contain any visible items yet.": "لا يحتوي هذا الطلب على عناصر ظاهرة بعد.",
  "Order Items": "عناصر الطلب",
  "Update the order status or cancel the order.": "حدّث حالة الطلب أو ألغِه.",
  Product: "المنتج",
  Status: "الحالة",
  Qty: "الكمية",
  Bags: "الأكياس",
  Note: "ملاحظة",
  "Item Status": "حالة العنصر",
  "Add item": "إضافة عنصر",
  "Update item": "تحديث العنصر",
  "Update item status": "تحديث حالة العنصر",
  "Delete item": "حذف العنصر",
  "Select product": "اختر المنتج",
  "Order item added.": "تمت إضافة عنصر الطلب.",
  "Order item updated.": "تم تحديث عنصر الطلب.",
  "Order item status updated.": "تم تحديث حالة عنصر الطلب.",
  "Order item deleted.": "تم حذف عنصر الطلب.",
  "Payment Summary": "ملخص المدفوعات",
  "Quick accountant view.": "عرض سريع للمحاسبة.",
  "Quick view for accountants.": "عرض سريع للمحاسبين.",
  "Payments loaded": "المدفوعات المحمّلة",
  "Latest order": "أحدث طلب",
  "Recent Payments": "أحدث المدفوعات",
  "Search by payment or order number.": "ابحث برقم الدفعة أو رقم الطلب.",
  "Production Control": "التحكم بالإنتاج",
  "Start production, monitor the line, complete stages, and check ETA.": "ابدأ الإنتاج وتابع الخط وأنهِ المراحل وتحقق من الوقت المتوقع.",
  "Start Production": "بدء الإنتاج",
  "Start production": "ابدأ الإنتاج",
  "ETA Lookup": "البحث عن الوقت المتوقع",
  "Production Dashboard": "لوحة الإنتاج",
  Pipeline: "مسار العمل",
  "Order Stage Timeline": "تسلسل مراحل الطلب",
  "Search payments": "البحث في المدفوعات",
  "Payment ID or order ID": "رقم الدفعة أو رقم الطلب",
  Payment: "الدفعة",
  Order: "الطلب",
  Amount: "المبلغ",
  Method: "طريقة الدفع",
  Time: "الوقت",
  "Payment Method": "طريقة الدفع",
  "Cash Only": "نقداً فقط",
  "Print receipt": "طباعة الوصل",
  "No payments found": "لم يتم العثور على مدفوعات",
  "Try a different payment or order number.": "جرّب رقم دفعة أو رقم طلب آخر.",
  "No payments loaded": "لا توجد مدفوعات محمّلة",
  "Create a payment or refresh once the accounting user has activity.": "أنشئ دفعة أو حدّث الصفحة بعد وجود نشاط للمحاسبة.",
  "No employees loaded": "لا يوجد موظفون محمّلون",
  "Seed data should expose at least the four demo users.": "يجب أن تُظهر البيانات الأولية المستخدمين التجريبيين الأربعة على الأقل.",
  "No authorities assigned.": "لا توجد صلاحيات مخصّصة.",
  "Queue not loaded": "لم يتم تحميل الطابور",
  "Refresh the queue status or make sure the queue service is available.": "حدّث حالة الطابور أو تأكد من توفر خدمة الطابور.",
  "Accounting queue tickets are reserved for management.": "تذاكر طابور المحاسبة مخصّصة للإدارة.",
  "Use production tickets for orders ready to enter the line.": "استخدم تذاكر الإنتاج للطلبات الجاهزة للدخول إلى الخط.",
  "Refresh the queue to see the latest changes.": "حدّث الطابور لرؤية آخر التغييرات.",
  "Monitor queue status and advance work using role-safe controls.": "راقب حالة الطابور وحرّك العمل باستخدام عناصر تحكم آمنة حسب الدور.",
  "Production Operations": "عمليات الإنتاج",
  "Track dashboard metrics, inspect pipelines, and execute stage work.": "تابع مؤشرات اللوحة وافحص خطوط العمل ونفّذ مراحل الإنتاج.",
  Admin: "الإدارة",
  employees: "الموظفون",
  roles: "الأدوار",
  authorities: "الصلاحيات",
  ai: "الذكاء الاصطناعي",
  "Create Employee": "إنشاء موظف",
  Role: "الدور",
  Gender: "الجنس",
  "Marital Status": "الحالة الاجتماعية",
  "Employee List": "قائمة الموظفين",
  ID: "المعرّف",
  Enabled: "مفعّل",
  From: "من",
  To: "إلى",
  Total: "الإجمالي",
  Date: "التاريخ",
  Ticket: "التذكرة",
  "Estimated Wait": "الانتظار المتوقع",
  "Queue Type": "نوع الطابور",
  "Advance Action": "إجراء التحريك",
  "Order ID for Ticket Issue": "رقم الطلب لإصدار التذكرة",
  Line: "الخط",
  Stage: "المرحلة",
  Remaining: "المتبقي",
  Item: "العنصر",
  "Order Item ID": "رقم عنصر الطلب",
  "Create payment": "تسجيل دفعة",
  "Period report": "تقرير الفترة",
  Start: "بدء",
  Finish: "إنهاء",
  PRODUCTION: "الإنتاج",
  ACCOUNTING: "المحاسبة",
  NEXT: "التالي",
  SKIP: "تخطي",
  RECALL: "إعادة النداء",
  CLOSE: "إغلاق",
  "Now Serving": "الدور الحالي",
  "Next In Line": "التالي في الدور",
  "Queue is clear": "الطابور فارغ",
  "Open station": "فتح المحطة",
  "Close station": "إغلاق المحطة",
  "Open display": "فتح شاشة العرض",
  "Disable Employee ID": "رقم الموظف للتعطيل",
  "New Role Name": "اسم الدور الجديد",
  "Role ID to Inspect / Update": "رقم الدور للفحص أو التحديث",
  "Authorities to Assign": "الصلاحيات المراد إسنادها",
  "Remove Role Authority": "إزالة صلاحية من الدور",
  "Authority Name to Remove": "اسم الصلاحية المراد إزالتها",
  "Authority Name": "اسم الصلاحية",
  "Authority ID": "رقم الصلاحية",
  "Rename Authority To": "إعادة تسمية الصلاحية إلى",
  ADMIN: "الإدارة",
  ACCOUNTANT: "المحاسبة",
  RECEPTIONIST: "الاستقبال",
  TECHNICIAN: "الفني",
  ENABLED: "مفعّل",
  DISABLED: "معطّل",
  MALE: "ذكر",
  FEMALE: "أنثى",
  "Role Governance": "حوكمة الأدوار",
  "Authority Governance": "حوكمة الصلاحيات",
  "AI Operations": "عمليات الذكاء الاصطناعي",
  "Complete first-time activation or a reset token handoff.": "أكمل التفعيل الأولي أو تمرير إعادة تعيين كلمة المرور.",
  "Password set successfully. Redirecting to login...": "تم تعيين كلمة المرور بنجاح. جارٍ التحويل إلى تسجيل الدخول...",
  "Page Not Found": "الصفحة غير موجودة",
  "The route you requested is not registered in this frontend.": "المسار الذي طلبته غير مسجل في هذه الواجهة.",
  "Go to login": "الانتقال إلى تسجيل الدخول",
  "Profile Details": "تفاصيل الملف الشخصي",
  "Backed by `/api/employees/profile`.": "مدعوم من `/api/employees/profile`.",
  "View your account details.": "اعرض تفاصيل حسابك.",
  Phone: "الهاتف",
  "Old Password": "كلمة المرور القديمة",
  "Changing...": "جارٍ التغيير...",
  "Profile updated successfully.": "تم تحديث الملف الشخصي بنجاح.",
  "Password changed successfully.": "تم تغيير كلمة المرور بنجاح.",
  "Master Data": "البيانات المرجعية",
  "Read, create, update, and delete city records through the gateway.": "اقرأ سجلات المدن وأنشئها وحدّثها واحذفها عبر البوابة.",
  "Edit City #": "تعديل المدينة رقم",
  "Creating...": "جارٍ الإنشاء...",
  "City updated.": "تم تحديث المدينة.",
  "City created.": "تم إنشاء المدينة.",
  "Save changes": "حفظ التغييرات",
  Clear: "مسح",
  "City Directory": "دليل المدن",
  Actions: "الإجراءات",
  Edit: "تعديل",
  Delete: "حذف",
  "Deleting...": "جارٍ الحذف...",
  "Deleted city": "تم حذف المدينة",
  "No cities yet": "لا توجد مدن بعد",
  "Create the first city so customer intake can map to valid city IDs.": "أنشئ أول مدينة حتى يتمكن استقبال العملاء من الربط بمعرفات مدن صحيحة.",
  "Create the first city before registering customers.": "أنشئ أول مدينة قبل تسجيل العملاء.",
  "Customer Domain": "نطاق العملاء",
  "Supports intake, search, updates, membership checks, and national-ID resolution.": "يدعم الاستقبال والبحث والتحديثات وفحص العضوية وحل الرقم الوطني.",
  "Edit Customer #": "تعديل العميل رقم",
  "Select city": "اختر مدينة",
  Membership: "العضوية",
  Standard: "غير مساهم",
  Member: "مساهم",
  "Customer updated.": "تم تحديث العميل.",
  "Customer created.": "تم إنشاء العميل.",
  "Save customer": "حفظ العميل",
  "Lookup Utilities": "أدوات البحث",
  "Search by National ID": "البحث بالرقم الوطني",
  "Delete selected": "حذف المحدد",
  "Membership Lookup by Customer ID": "فحص العضوية بواسطة معرف العميل",
  "Checking...": "جارٍ الفحص...",
  "Check membership": "فحص العضوية",
  "Patch National ID for Selected Customer": "تعديل الرقم الوطني للعميل المحدد",
  "Customer national ID updated.": "تم تحديث الرقم الوطني للعميل.",
  "Patch national ID": "تعديل الرقم الوطني",
  "Membership result:": "نتيجة العضوية:",
  "Customer Directory": "دليل العملاء",
  "Customer Search": "بحث العملاء",
  "Find a customer by national ID and work with the selected record.": "ابحث عن عميل بواسطة الرقم الوطني واعمل على السجل المحدد.",
  "City ID": "معرف المدينة",
  "No customers yet": "لا يوجد عملاء بعد",
  "Create a customer to unlock the national-ID order flow.": "أنشئ عميلًا لفتح مسار الطلب باستخدام الرقم الوطني.",
  "Create a customer to start placing orders.": "أنشئ عميلًا لبدء تسجيل الطلبات.",
  Catalog: "الكتالوج",
  "Manage OLIVE, JIFT, and GALLON lookup products through the canonical `/api/products` path.": "أدر منتجات OLIVE وJIFT وGALLON عبر المسار القياسي `/api/products`.",
  "Edit Product #": "تعديل المنتج رقم",
  "Manage products used in orders and payments.": "أدر المنتجات المستخدمة في الطلبات والمدفوعات.",
  "Product Controls": "التحكم بالمنتج",
  "Update inventory": "تحديث المخزون",
  "Reactivate Product": "إعادة تفعيل المنتج",
  "Active Products": "المنتجات النشطة",
  "Create or reactivate products to start taking orders.": "أنشئ المنتجات أو أعد تفعيلها لبدء استقبال الطلبات.",
  Unit: "الوحدة",
  "Product updated.": "تم تحديث المنتج.",
  "Product created.": "تم إنشاء المنتج.",
  "Save product": "حفظ المنتج",
  "Create product": "إنشاء منتج",
  "Inventory + Activation Utilities": "أدوات المخزون والتفعيل",
  "Selected Product Inventory": "مخزون المنتج المحدد",
  "Patch inventory": "تعديل المخزون",
  "Deactivating...": "جارٍ التعطيل...",
  "Deactivate selected": "تعطيل المحدد",
  "Inventory updated.": "تم تحديث المخزون.",
  "Product deactivated.": "تم تعطيل المنتج.",
  "Activate Product by ID": "تفعيل المنتج بواسطة المعرف",
  "Activating...": "جارٍ التفعيل...",
  "Activate product": "تفعيل المنتج",
  "Active Product Catalog": "كتالوج المنتجات النشطة",
  "No active products": "لا توجد منتجات نشطة",
  "Create or reactivate products to support order intake and payments.": "أنشئ المنتجات أو أعد تفعيلها لدعم استقبال الطلبات والمدفوعات.",
  "Olive Type": "نوع الزيتون",
  "Bags Count": "عدد الأكياس",
  "Remove item": "إزالة العنصر",
  "Lookup by order ID, customer ID, or customer national ID.": "ابحث بواسطة رقم الطلب أو معرف العميل أو الرقم الوطني للعميل.",
  "Get order": "جلب الطلب",
  "Search by customer": "البحث حسب العميل",
  "Search by national ID": "البحث حسب الرقم الوطني",
  Items: "العناصر",
  Created: "تاريخ الإنشاء",
  "No orders loaded": "لم يتم تحميل أي طلبات",
  "Use one of the search tools or create a fresh order.": "استخدم إحدى أدوات البحث أو أنشئ طلبًا جديدًا.",
  "Selected Order #": "الطلب المحدد رقم",
  "Update status or cancel when backend rules allow it.": "حدّث الحالة أو ألغِ عندما تسمح قواعد الواجهة الخلفية بذلك.",
  "Canceling...": "جارٍ الإلغاء...",
  "Cancel order": "إلغاء الطلب",
  "Customer resolved:": "تم حل العميل:",
  "Governance tools, authority management, and AI operations.": "أدوات الحوكمة وإدارة الصلاحيات وعمليات الذكاء الاصطناعي.",
  "Create employee": "إنشاء موظف",
  "Employee created.": "تم إنشاء الموظف.",
  "Employee Directory": "دليل الموظفين",
  "Lookup by National ID": "البحث بالرقم الوطني",
  unknown: "غير معروف"
};

const extraTranslations: Record<string, string> = {
  English: "الإنجليزية",
  Arabic: "العربية",
  "Operations Platform": "منصة التشغيل",
  "Olive Press": "معصرة الزيتون",
  "Welcome back": "أهلاً بعودتك",
  "Arabic / English": "العربية / الإنجليزية",
  "Tablet-ready": "جاهز للأجهزة اللوحية",
  "Gateway-linked": "مرتبط بالبوابة",
  "Olive Press logo": "شعار معصرة الزيتون",
  "Trusted production workspace": "مساحة تشغيل موثوقة للإنتاج",
  "Cooperative olive pressing": "الجمعية التعاونية لعصر الزيتون",
  "Bilingual operations for intake, payments, and AI-assisted olive analysis.": "تشغيل ثنائي اللغة للاستقبال والمدفوعات وتحليل الزيتون المدعوم بالذكاء الاصطناعي.",
  "Orders, payments, and AI in one place.": "الطلبات والمدفوعات والذكاء الاصطناعي في مكان واحد.",
  "Smart tools for the modern olive press.": "أدوات ذكية لمعصرة الزيتون الحديثة.",
  "AI-ready": "جاهز للذكاء الاصطناعي",
  "Secure sign-in to your operations workspace.": "تسجيل دخول آمن إلى مساحة تشغيل المعصرة.",
  "Access your cooperative intelligence portal.": "ادخل إلى بوابة الذكاء التعاوني الخاصة بالمعصرة.",
  "Access your Cooperative Intelligence Portal.": "ادخل إلى بوابة الذكاء التعاوني الخاصة بالمعصرة.",
  "Use your work account.": "استخدم حساب العمل للمتابعة.",
  Welcome: "مرحباً",
  "Sign in to continue.": "سجّل الدخول للمتابعة.",
  "Username / Email": "اسم المستخدم / البريد الإلكتروني",
  "Enter your work email": "أدخل بريد العمل",
  "Enter your password": "أدخل كلمة المرور",
  "Forgot Password?": "هل نسيت كلمة المرور؟",
  "Need Help?": "تحتاج مساعدة؟",
  "Log In": "تسجيل الدخول",
  "OlioNova AI logo": "شعار OlioNova AI",
  "OlioNova AI": "OlioNova AI",
  "Transforming olive pressing into intelligence.": "تحويل عصر الزيتون إلى ذكاء تشغيلي.",
  "Professional operations login": "بوابة التشغيل الاحترافية",
  "Sign in to your workspace.": "سجّل الدخول إلى مساحة العمل الخاصة بك.",
  "A bilingual operations console for customers, orders, payments, production, and queue workflows through the central gateway.": "واجهة تشغيل ثنائية اللغة لإدارة العملاء والطلبات والمدفوعات والإنتاج والطوابير عبر البوابة المركزية.",
  "AI Prediction": "التنبؤ الذكي",
  "Request failed.": "فشل تنفيذ الطلب.",
  "Username is required.": "اسم المستخدم مطلوب.",
  "Password is required.": "كلمة المرور مطلوبة.",
  "Activation token is required.": "رمز التفعيل مطلوب.",
  "New password is required.": "كلمة المرور الجديدة مطلوبة.",
  "Password must be at least 8 characters.": "يجب أن تتكون كلمة المرور من 8 أحرف على الأقل.",
  "First name is required.": "الاسم الأول مطلوب.",
  "Last name is required.": "اسم العائلة مطلوب.",
  "Phone number is required.": "رقم الهاتف مطلوب.",
  "Email is required.": "البريد الإلكتروني مطلوب.",
  "Enter a valid email address.": "أدخل بريدًا إلكترونيًا صالحًا.",
  "Current password is required.": "كلمة المرور الحالية مطلوبة.",
  "New password must be at least 8 characters.": "يجب أن تتكون كلمة المرور الجديدة من 8 أحرف على الأقل.",
  "Confirm new password is required.": "تأكيد كلمة المرور الجديدة مطلوب.",
  "New passwords do not match.": "كلمتا المرور الجديدتان غير متطابقتين.",
  "Marital status is required.": "الحالة الاجتماعية مطلوبة.",
  "City name is required.": "اسم المدينة مطلوب.",
  "National ID is required.": "الرقم الوطني مطلوب.",
  "National ID must contain exactly 9 digits.": "يجب أن يتكون الرقم الوطني من 9 أرقام تمامًا.",
  "City is required.": "المدينة مطلوبة.",
  "Product name is required.": "اسم المنتج مطلوب.",
  "Product type is required.": "نوع المنتج مطلوب.",
  "Unit is required.": "الوحدة مطلوبة.",
  "Price must be greater than zero.": "يجب أن يكون السعر أكبر من صفر.",
  "Inventory cannot be negative.": "لا يمكن أن يكون المخزون سالبًا.",
  "Resolve a customer before creating the order.": "يجب تحديد العميل قبل إنشاء الطلب.",
  "At least one item is required.": "مطلوب عنصر واحد على الأقل.",
  "Choose a valid product.": "اختر منتجًا صالحًا.",
  "Quantity must be greater than zero.": "يجب أن تكون الكمية أكبر من صفر.",
  "Olive type is required.": "نوع الزيتون مطلوب.",
  "Bags count must be greater than zero.": "يجب أن يكون عدد الأكياس أكبر من صفر.",
  "Duplicate olive service item for the same olive type.": "يوجد عنصر خدمة زيتون مكرر لنفس نوع الزيتون.",
  "Duplicate purchase product in the same order.": "يوجد منتج شراء مكرر داخل نفس الطلب.",
  "Order ID is required.": "رقم الطلب مطلوب.",
  "Order ID must be a positive number.": "يجب أن يكون رقم الطلب رقمًا موجبًا.",
  "Role is required.": "الدور مطلوب.",
  "This field is required.": "هذا الحقل مطلوب.",
  "Batch ID is required.": "معرف الدفعة مطلوب.",
  "Batch ID may contain only letters, numbers, hyphens, and underscores (1-64 characters).": "يجب أن يحتوي معرف الدفعة على أحرف أو أرقام أو شرطات أو شرطات سفلية فقط، بطول من 1 إلى 64 حرفًا.",
  "Searching...": "جارٍ البحث...",
  "Find employee": "ابحث عن موظف",
  "Disable employee": "تعطيل الموظف",
  "Comma separated authorities": "صلاحيات مفصولة بفواصل",
  "Role ID": "رقم الدور",
  "Create role": "إنشاء دور",
  "Load role detail": "تحميل تفاصيل الدور",
  "Assign authorities": "إسناد الصلاحيات",
  "Remove authority": "إزالة الصلاحية",
  "Delete role": "حذف الدور",
  "Create authority": "إنشاء صلاحية",
  "Rename authority": "إعادة تسمية الصلاحية",
  "Delete authority": "حذف الصلاحية",
  "Select an order item first to update its status.": "اختر عنصر طلب أولاً لتحديث حالته.",
  "Enter an order item ID before loading the ETA.": "أدخل رقم عنصر الطلب قبل تحميل الوقت المتوقع.",
  "Enter an order item ID before loading stage details.": "أدخل رقم عنصر الطلب قبل تحميل تفاصيل المرحلة.",
  "Sign in required.": "يجب تسجيل الدخول أولاً.",
  "Payments are limited to accounting and admin users.": "المدفوعات متاحة للمحاسبة والإدارة فقط.",
  "Admin workspace only.": "هذه المساحة مخصصة للإدارة فقط.",
  "Page not available.": "الصفحة غير متاحة.",
  "Only admin or receptionist can create orders.": "يمكن للإدارة أو موظف الاستقبال فقط إنشاء الطلبات.",
  "Only admin or receptionist can cancel orders.": "يمكن للإدارة أو موظف الاستقبال فقط إلغاء الطلبات.",
  "Production start is limited to admin, accountant, or receptionist.": "بدء الإنتاج متاح للإدارة أو المحاسب أو موظف الاستقبال فقط.",
  "Dashboard is limited to admin and accountant.": "لوحة المتابعة متاحة للإدارة والمحاسب فقط.",
  "Pipeline is limited to admin and technician.": "مسار العمل متاح للإدارة والفني فقط.",
  "Stage execution is limited to admin and technician.": "تنفيذ المراحل متاح للإدارة والفني فقط.",
  "ETA is limited to receptionist and admin.": "عرض وقت الوصول المتوقع متاح لموظف الاستقبال والإدارة فقط.",
  "Queue advance is limited to admin, accountant, or technician.": "تحريك الطابور متاح للإدارة أو المحاسب أو الفني فقط.",
  "Accounting ticket issuance is admin-only in the current backend.": "إصدار تذكرة المحاسبة متاح للإدارة فقط في النسخة الحالية من الخلفية.",
  "Production tickets are limited to accounting and admin.": "تذاكر الإنتاج متاحة للمحاسبة والإدارة فقط.",
  "Action not available.": "الإجراء غير متاح."
};

const patternTranslations: Array<{ pattern: RegExp; translate: (...matches: string[]) => string }> = [
  {
    pattern: /^Resolved customer #(\d+)\.$/,
    translate: (id) => `تم تحديد العميل رقم ${id}.`
  },
  {
    pattern: /^Created order #(\d+)\.$/,
    translate: (id) => `تم إنشاء الطلب رقم ${id}.`
  },
  {
    pattern: /^Order #(\d+) canceled\.$/,
    translate: (id) => `تم إلغاء الطلب رقم ${id}.`
  },
  {
    pattern: /^Order #(\d+)$/,
    translate: (id) => `الطلب رقم ${id}`
  },
  {
    pattern: /^Issued production ticket for order #(\d+)\.$/,
    translate: (id) => `تم إصدار تذكرة إنتاج للطلب رقم ${id}.`
  },
  {
    pattern: /^Order #(\d+) loaded\.$/,
    translate: (id) => `تم تحميل الطلب رقم ${id}.`
  },
  {
    pattern: /^Employee #(\d+) disabled\.$/,
    translate: (id) => `تم تعطيل الموظف رقم ${id}.`
  },
  {
    pattern: /^Role #(\d+) loaded\.$/,
    translate: (id) => `تم تحميل الدور رقم ${id}.`
  },
  {
    pattern: /^Role #(\d+) deleted\.$/,
    translate: (id) => `تم حذف الدور رقم ${id}.`
  },
  {
    pattern: /^Customer resolved: (.+) \(#(\d+)\)$/,
    translate: (name, id) => `تم تحديد العميل: ${name} (#${id})`
  },
  {
    pattern: /^Payment recorded for order #(\d+)\.$/,
    translate: (id) => `تم تسجيل دفعة للطلب رقم ${id}.`
  },
  {
    pattern: /^Order #(\d+) was sent to the production queue\.$/,
    translate: (id) => `تم إرسال الطلب رقم ${id} إلى طابور الإنتاج.`
  },
  {
    pattern: /^Queue update for order #(\d+): (.+)$/,
    translate: (id, message) => `تحديث الطابور للطلب رقم ${id}: ${translatePhrase(message)}`
  },
  {
    pattern: /^Deleted city (.+)\.$/,
    translate: (name) => `تم حذف المدينة ${name}.`
  },
  {
    pattern: /^Deleted customer #(\d+)\.$/,
    translate: (id) => `تم حذف العميل رقم ${id}.`
  },
  {
    pattern: /^Role created\.$/,
    translate: () => "تم إنشاء الدور."
  },
  {
    pattern: /^Authorities assigned\.$/,
    translate: () => "تم إسناد الصلاحيات."
  },
  {
    pattern: /^Authority removed from role\.$/,
    translate: () => "تمت إزالة الصلاحية من الدور."
  },
  {
    pattern: /^Authority created\.$/,
    translate: () => "تم إنشاء الصلاحية."
  },
  {
    pattern: /^Authority updated\.$/,
    translate: () => "تم تحديث الصلاحية."
  },
  {
    pattern: /^Authority #(\d+) deleted\.$/,
    translate: (id) => `تم حذف الصلاحية رقم ${id}.`
  },
  {
    pattern: /^Activate request sent for product #(\d+)\.$/,
    translate: (id) => `تم إرسال طلب التفعيل للمنتج رقم ${id}.`
  },
  {
    pattern: /^Membership result: (.+)$/,
    translate: (value) => `نتيجة العضوية: ${translatePhrase(value)}`
  }
];

const extraPatternTranslations: Array<{ pattern: RegExp; translate: (...matches: string[]) => string }> = [
  {
    pattern: /^Requires (.+)\.$/,
    translate: (authority) => `يتطلب الصلاحية ${authority}.`
  }
];

function detectStoredLanguage(): AppLanguage {
  const stored = window.localStorage.getItem(LANGUAGE_STORAGE_KEY);
  if (stored === "ar" || stored === "en") {
    return stored;
  }

  const browserLanguage = window.navigator.language.toLowerCase();
  if (browserLanguage.startsWith("ar")) {
    return "ar";
  }

  return "ar";
}

function resolveDirection(language: AppLanguage): AppDirection {
  return language === "ar" ? "rtl" : "ltr";
}

function translatePhrase(value: string): string {
  for (const entry of [...extraPatternTranslations, ...patternTranslations]) {
    const match = value.match(entry.pattern);
    if (match) {
      return entry.translate(...match.slice(1));
    }
  }

  return extraTranslations[value] ?? translations[value] ?? value;
}

const I18nContext = createContext<I18nContextValue | undefined>(undefined);

export function I18nProvider({ children }: PropsWithChildren): JSX.Element {
  const [language, setLanguageState] = useState<AppLanguage>(() => detectStoredLanguage());

  const direction = useMemo(() => resolveDirection(language), [language]);

  const setLanguage = (nextLanguage: AppLanguage): void => {
    setLanguageState(nextLanguage);
    window.localStorage.setItem(LANGUAGE_STORAGE_KEY, nextLanguage);
  };

  useEffect(() => {
    document.documentElement.lang = language;
    document.documentElement.dir = direction;
  }, [direction, language]);

  const value = useMemo<I18nContextValue>(
    () => ({
      language,
      direction,
      setLanguage,
      t: (input) => {
        if (!input) {
          return "";
        }

        return language === "ar" ? translatePhrase(input) : input;
      }
    }),
    [direction, language]
  );

  return <I18nContext.Provider value={value}>{children}</I18nContext.Provider>;
}

export function useI18n(): I18nContextValue {
  const value = useContext(I18nContext);

  if (!value) {
    throw new Error("useI18n must be used within an I18nProvider.");
  }

  return value;
}
