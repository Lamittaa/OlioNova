import { useEffect, useState } from "react";
import { useI18n } from "../app/i18n-context";
import { ActionButton, ErrorSummary, FieldError } from "../components/form-ui";
import { Card, EmptyState, PageHeader, StatusBadge } from "../components/ui";
import { endpoints } from "../lib/endpoints";
import { apiRequest } from "../lib/http";
import { useActionState } from "../lib/use-action-state";
import { asErrorMessage } from "../lib/utils";
import { getApiFieldErrors, getApiErrorMessage, type FieldErrors, validateEmployee } from "../lib/validation";
import type { CreateEmployeeRequest, Employee, EmployeeListItem, UpdateEmployeeRequest } from "../types/models";
import { useToastHelpers } from "../app/toast-context";

type ToastNotice = { tone: "success" | "danger" | "neutral"; message: string } | null;

function useNotice() {
  const { success, error, neutral } = useToastHelpers();

  return {
    notice: null as ToastNotice,
    setNotice: (notice: ToastNotice) => {
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

function NoticeBlock({ notice }: { notice: ToastNotice }): JSX.Element | null {
  void notice;
  return null;
}

const EMPTY_EMPLOYEE_FORM: CreateEmployeeRequest = {
  nationalId: "",
  firstName: "",
  lastName: "",
  phoneNumber: "",
  email: "",
  gender: "MALE",
  maritalStatus: "SINGLE",
  roleName: "RECEPTIONIST"
};

export function AdminPage(): JSX.Element {
  const { language, t } = useI18n();
  const { notice, setNotice } = useNotice();
  const actionState = useActionState();
  const [employees, setEmployees] = useState<EmployeeListItem[]>([]);
  const [employeeForm, setEmployeeForm] = useState<CreateEmployeeRequest>(EMPTY_EMPLOYEE_FORM);
  const [employeeErrors, setEmployeeErrors] = useState<FieldErrors>({});
  const [employeeEditErrors, setEmployeeEditErrors] = useState<FieldErrors>({});
  const [disableEmployeeId, setDisableEmployeeId] = useState("");
  const [employeeNationalIdSearch, setEmployeeNationalIdSearch] = useState("");
  const [employeeLookup, setEmployeeLookup] = useState<Employee | null>(null);
  const [employeeEditForm, setEmployeeEditForm] = useState<UpdateEmployeeRequest | null>(null);
  const isArabic = language === "ar";

  const loadEmployees = async (): Promise<void> => {
    setEmployees(await apiRequest<EmployeeListItem[]>(endpoints.employees.list));
  };

  const buildEmployeeRequest = (): CreateEmployeeRequest => {
    return {
      ...employeeForm,
      email: employeeForm.email.trim(),
      maritalStatus: employeeForm.maritalStatus || "SINGLE"
    };
  };

  const getEmployeeName = (employee: Employee | EmployeeListItem): string => {
    if ("fullName" in employee) {
      return employee.fullName;
    }

    return `${employee.firstName ?? ""} ${employee.lastName ?? ""}`.trim();
  };

  const buildEmployeeEditRequest = (): UpdateEmployeeRequest | null => {
    if (!employeeEditForm) {
      return null;
    }

    return {
      ...employeeEditForm,
      email: employeeEditForm.email.trim(),
      maritalStatus: employeeEditForm.maritalStatus || "SINGLE"
    };
  };

  useEffect(() => {
    void loadEmployees().catch((requestError: unknown) => setNotice({ tone: "danger", message: asErrorMessage(requestError) }));
  }, []);

  const createEmployee = (): void => {
    const employeeRequest = buildEmployeeRequest();
    const nextErrors = validateEmployee(employeeRequest);
    setEmployeeErrors(nextErrors);

    if (Object.keys(nextErrors).length) {
      return;
    }

    void actionState.runAction("create-employee", async () => {
      try {
        await apiRequest(endpoints.employees.create, { method: "POST", body: employeeRequest });
        await loadEmployees();
        setEmployeeForm(EMPTY_EMPLOYEE_FORM);
        setEmployeeErrors({});
        setNotice({ tone: "success", message: isArabic ? "تم إنشاء الموظف." : "Employee created." });
      } catch (requestError: unknown) {
        setEmployeeErrors((current) => ({ ...current, ...getApiFieldErrors(requestError) }));
        setNotice({ tone: "danger", message: getApiErrorMessage(requestError) });
      }
    });
  };

  const findEmployee = (): void => {
    const nationalId = employeeNationalIdSearch.trim();
    if (!nationalId) {
      setEmployeeLookup(null);
      setNotice({ tone: "neutral", message: isArabic ? "أدخل رقم الهوية أولاً." : "Enter a national ID first." });
      return;
    }

    void actionState.runAction("find-employee", async () => {
      try {
        const foundEmployee = await apiRequest<Employee>(endpoints.employees.byNationalId(nationalId));
        setEmployeeLookup(foundEmployee);
        setEmployeeEditForm(null);
        setEmployeeEditErrors({});
      } catch (requestError: unknown) {
        setEmployeeLookup(null);
        setEmployeeEditForm(null);
        setNotice({ tone: "danger", message: getApiErrorMessage(requestError) });
      }
    });
  };

  const openEmployeeEditor = (): void => {
    if (!employeeLookup) {
      return;
    }

    setEmployeeEditErrors({});
    setEmployeeEditForm({
      nationalId: employeeLookup.nationalId,
      firstName: employeeLookup.firstName,
      lastName: employeeLookup.lastName,
      phoneNumber: employeeLookup.phoneNumber,
      email: employeeLookup.email,
      gender: employeeLookup.gender,
      maritalStatus: employeeLookup.maritalStatus || "SINGLE",
      roleName: employeeLookup.role
    });
  };

  const updateEmployee = (): void => {
    if (!employeeLookup) {
      return;
    }

    const employeeRequest = buildEmployeeEditRequest();
    if (!employeeRequest) {
      return;
    }

    const nextErrors = validateEmployee(employeeRequest);
    setEmployeeEditErrors(nextErrors);

    if (Object.keys(nextErrors).length) {
      return;
    }

    void actionState.runAction("update-employee", async () => {
      try {
        const updatedEmployee = await apiRequest<Employee>(endpoints.employees.byId(employeeLookup.id), {
          method: "PATCH",
          body: employeeRequest
        });
        setEmployeeLookup(updatedEmployee);
        setEmployeeEditForm(null);
        setEmployeeEditErrors({});
        await loadEmployees();
        setNotice({ tone: "success", message: isArabic ? "تم تحديث بيانات الموظف." : "Employee data updated." });
      } catch (requestError: unknown) {
        setEmployeeEditErrors((current) => ({ ...current, ...getApiFieldErrors(requestError) }));
        setNotice({ tone: "danger", message: getApiErrorMessage(requestError) });
      }
    });
  };

  const disableEmployee = (): void => {
    if (!disableEmployeeId.trim()) {
      setNotice({ tone: "neutral", message: isArabic ? "أدخل رقم الموظف أولاً." : "Enter an employee ID first." });
      return;
    }

    void actionState.runAction("disable-employee", async () => {
      try {
        await apiRequest(endpoints.employees.byId(disableEmployeeId.trim()), { method: "DELETE" });
        await loadEmployees();
        setDisableEmployeeId("");
        setNotice({ tone: "success", message: isArabic ? "تم تعطيل الموظف." : "Employee disabled." });
      } catch (requestError: unknown) {
        setNotice({ tone: "danger", message: getApiErrorMessage(requestError) });
      }
    });
  };

  return (
    <div className="content-grid">
      <PageHeader
        eyebrow={isArabic ? "الإدارة" : "Admin"}
        title={isArabic ? "إدارة الموظفين" : "Employee Management"}
        description={isArabic ? "إنشاء الموظفين ومتابعة دليل الموظفين الحالي." : "Create employees and manage the current employee directory."}
      />
      <NoticeBlock notice={notice} />

      <div className="content-grid two">
        <Card title={isArabic ? "إنشاء موظف" : "Create Employee"}>
          <ErrorSummary errors={employeeErrors} />
          <div className="form-grid two">
            <div className="field">
              <label>{t("National ID")}</label>
              <input
                onChange={(event) => setEmployeeForm((current) => ({ ...current, nationalId: event.target.value }))}
                value={employeeForm.nationalId}
              />
              <FieldError errors={employeeErrors} name="nationalId" />
            </div>
            <div className="field">
              <label>{t("Role")}</label>
              <select
                onChange={(event) => setEmployeeForm((current) => ({ ...current, roleName: event.target.value }))}
                value={employeeForm.roleName}
              >
                <option value="ADMIN">{t("ADMIN")}</option>
                <option value="ACCOUNTANT">{t("ACCOUNTANT")}</option>
                <option value="RECEPTIONIST">{t("RECEPTIONIST")}</option>
                <option value="TECHNICIAN">{t("TECHNICIAN")}</option>
              </select>
            </div>
            <div className="field">
              <label>{t("First Name")}</label>
              <input
                onChange={(event) => setEmployeeForm((current) => ({ ...current, firstName: event.target.value }))}
                value={employeeForm.firstName}
              />
              <FieldError errors={employeeErrors} name="firstName" />
            </div>
            <div className="field">
              <label>{t("Last Name")}</label>
              <input
                onChange={(event) => setEmployeeForm((current) => ({ ...current, lastName: event.target.value }))}
                value={employeeForm.lastName}
              />
              <FieldError errors={employeeErrors} name="lastName" />
            </div>
            <div className="field">
              <label>{t("Phone Number")}</label>
              <input
                onChange={(event) => setEmployeeForm((current) => ({ ...current, phoneNumber: event.target.value }))}
                value={employeeForm.phoneNumber}
              />
              <FieldError errors={employeeErrors} name="phoneNumber" />
            </div>
            <div className="field">
              <label>{t("Email")}</label>
              <input
                autoComplete="email"
                onChange={(event) => setEmployeeForm((current) => ({ ...current, email: event.target.value }))}
                type="email"
                value={employeeForm.email}
              />
              <FieldError errors={employeeErrors} name="email" />
            </div>
            <div className="field">
              <label>{t("Gender")}</label>
              <select
                onChange={(event) => setEmployeeForm((current) => ({ ...current, gender: event.target.value }))}
                value={employeeForm.gender}
              >
                <option value="MALE">{t("MALE")}</option>
                <option value="FEMALE">{t("FEMALE")}</option>
              </select>
            </div>
          </div>
          <div className="inline-actions">
            <ActionButton
              busyLabel={isArabic ? "جارٍ الإنشاء..." : "Creating..."}
              isBusy={actionState.isBusy("create-employee")}
              onClick={createEmployee}
              type="button"
            >
              {isArabic ? "إنشاء الموظف" : "Create employee"}
            </ActionButton>
          </div>
        </Card>

        <Card title={isArabic ? "دليل الموظفين" : "Employee Directory"}>
          <div className="form-grid">
            <div className="field">
              <label>{t("Lookup by National ID")}</label>
              <input
                onChange={(event) => setEmployeeNationalIdSearch(event.target.value)}
                value={employeeNationalIdSearch}
              />
            </div>
            <div className="field">
              <label>{t("Disable Employee ID")}</label>
              <input onChange={(event) => setDisableEmployeeId(event.target.value)} value={disableEmployeeId} />
            </div>
            <div className="inline-actions">
              <ActionButton
                className="secondary"
                busyLabel={isArabic ? "جارٍ البحث..." : "Searching..."}
                isBusy={actionState.isBusy("find-employee")}
                onClick={findEmployee}
                type="button"
              >
                {t("Find employee")}
              </ActionButton>
              <ActionButton
                className="danger"
                busyLabel={isArabic ? "جارٍ التعطيل..." : "Disabling..."}
                isBusy={actionState.isBusy("disable-employee")}
                onClick={disableEmployee}
                type="button"
              >
                {t("Disable employee")}
              </ActionButton>
            </div>
            {employeeLookup ? (
              <>
                <div className="metric-strip">
                  <div><strong>{t("ID")}</strong><div>{employeeLookup.id}</div></div>
                  <div><strong>{t("Name")}</strong><div>{getEmployeeName(employeeLookup) || "-"}</div></div>
                  <div><strong>{t("Role")}</strong><div>{employeeLookup.role}</div></div>
                </div>
                <div className="inline-actions">
                  <ActionButton
                    className="secondary"
                    onClick={openEmployeeEditor}
                    type="button"
                  >
                    {isArabic ? "تحديث البيانات" : "Update data"}
                  </ActionButton>
                </div>
              </>
            ) : null}
            {employeeEditForm ? (
              <div className="content-grid">
                <ErrorSummary errors={employeeEditErrors} />
                <div className="form-grid two">
                  <div className="field">
                    <label>{t("National ID")}</label>
                    <input
                      onChange={(event) => setEmployeeEditForm((current) => current ? ({ ...current, nationalId: event.target.value }) : current)}
                      value={employeeEditForm.nationalId}
                    />
                    <FieldError errors={employeeEditErrors} name="nationalId" />
                  </div>
                  <div className="field">
                    <label>{t("Role")}</label>
                    <select
                      onChange={(event) => setEmployeeEditForm((current) => current ? ({ ...current, roleName: event.target.value }) : current)}
                      value={employeeEditForm.roleName}
                    >
                      <option value="ADMIN">{t("ADMIN")}</option>
                      <option value="ACCOUNTANT">{t("ACCOUNTANT")}</option>
                      <option value="RECEPTIONIST">{t("RECEPTIONIST")}</option>
                      <option value="TECHNICIAN">{t("TECHNICIAN")}</option>
                    </select>
                  </div>
                  <div className="field">
                    <label>{t("First Name")}</label>
                    <input
                      onChange={(event) => setEmployeeEditForm((current) => current ? ({ ...current, firstName: event.target.value }) : current)}
                      value={employeeEditForm.firstName}
                    />
                    <FieldError errors={employeeEditErrors} name="firstName" />
                  </div>
                  <div className="field">
                    <label>{t("Last Name")}</label>
                    <input
                      onChange={(event) => setEmployeeEditForm((current) => current ? ({ ...current, lastName: event.target.value }) : current)}
                      value={employeeEditForm.lastName}
                    />
                    <FieldError errors={employeeEditErrors} name="lastName" />
                  </div>
                  <div className="field">
                    <label>{t("Phone Number")}</label>
                    <input
                      onChange={(event) => setEmployeeEditForm((current) => current ? ({ ...current, phoneNumber: event.target.value }) : current)}
                      value={employeeEditForm.phoneNumber}
                    />
                    <FieldError errors={employeeEditErrors} name="phoneNumber" />
                  </div>
                  <div className="field">
                    <label>{t("Email")}</label>
                    <input
                      autoComplete="email"
                      onChange={(event) => setEmployeeEditForm((current) => current ? ({ ...current, email: event.target.value }) : current)}
                      type="email"
                      value={employeeEditForm.email}
                    />
                    <FieldError errors={employeeEditErrors} name="email" />
                  </div>
                  <div className="field">
                    <label>{t("Gender")}</label>
                    <select
                      onChange={(event) => setEmployeeEditForm((current) => current ? ({ ...current, gender: event.target.value }) : current)}
                      value={employeeEditForm.gender}
                    >
                      <option value="MALE">{t("MALE")}</option>
                      <option value="FEMALE">{t("FEMALE")}</option>
                    </select>
                  </div>
                </div>
                <div className="inline-actions">
                  <ActionButton
                    busyLabel={isArabic ? "جارٍ التحديث..." : "Updating..."}
                    isBusy={actionState.isBusy("update-employee")}
                    onClick={updateEmployee}
                    type="button"
                  >
                    {isArabic ? "حفظ التحديث" : "Save update"}
                  </ActionButton>
                  <ActionButton
                    className="secondary"
                    onClick={() => {
                      setEmployeeEditForm(null);
                      setEmployeeEditErrors({});
                    }}
                    type="button"
                  >
                    {isArabic ? "إلغاء" : "Cancel"}
                  </ActionButton>
                </div>
              </div>
            ) : null}
          </div>
        </Card>
      </div>

      <Card title={isArabic ? "الموظفون الحاليون" : "Current Employees"}>
        {employees.length ? (
          <div className="table-wrap">
            <table className="data-table">
              <thead>
                <tr>
                  <th>{t("ID")}</th>
                  <th>{t("Name")}</th>
                  <th>{t("Username")}</th>
                  <th>{t("Role")}</th>
                  <th>{t("Enabled")}</th>
                </tr>
              </thead>
              <tbody>
                {employees.map((employee) => (
                  <tr key={employee.id}>
                    <td>{employee.id}</td>
                    <td>{employee.fullName}</td>
                    <td>{employee.username}</td>
                    <td>{employee.role}</td>
                    <td><StatusBadge value={employee.enabled ? "ENABLED" : "DISABLED"} /></td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ) : (
          <EmptyState title={isArabic ? "لا يوجد موظفون" : "No employees loaded"} description={isArabic ? "أنشئ موظفاً أو تحقق من خدمة المصادقة." : "Create an employee or verify the auth service."} />
        )}
      </Card>
    </div>
  );
}
