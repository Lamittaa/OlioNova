package com.project.payment.service;

import com.project.payment.model.Payment;
import com.project.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PaymentExcelExportService {

    private final PaymentRepository paymentRepository;

    // =========================================================
    // 📊 DAILY EXCEL
    // =========================================================
    public byte[] exportDaily(LocalDate date) {

        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.atTime(23, 59, 59);

        List<Payment> payments =
                paymentRepository.findByPaymentDateBetween(start, end);

        return buildExcel(
                payments,
                "Daily Payments - " + date
        );
    }

    // =========================================================
    // 📅 PERIOD EXCEL
    // =========================================================
    public byte[] exportPeriod(LocalDate from, LocalDate to) {

        if (from.isAfter(to)) {
            throw new IllegalArgumentException("From date must be before To date");
        }

        LocalDateTime start = from.atStartOfDay();
        LocalDateTime end = to.atTime(23, 59, 59);

        List<Payment> payments =
                paymentRepository.findByPaymentDateBetween(start, end);

        return buildExcel(
                payments,
                "Payments " + from + " to " + to
        );
    }

    // =========================================================
    // 🔧 Excel Builder
    // =========================================================
    private byte[] buildExcel(List<Payment> payments, String sheetName) {

        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            // ✅ FIX: تنظيف اسم الـ Sheet من الرموز الممنوعة
            String safeSheetName = sheetName.replace("/", "-");

            Sheet sheet = workbook.createSheet(safeSheetName);

            // ===== Header Style =====
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);

            // ===== Header Row =====
            Row header = sheet.createRow(0);
            String[] columns = {
                    "Payment ID",
                    "Order ID",
                    "Total Price",
                    "Payment Type",
                    "Payment Date",
                    "Employee ID"
            };

            for (int i = 0; i < columns.length; i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(columns[i]);
                cell.setCellStyle(headerStyle);
            }

            // ===== Data Rows =====
            int rowIdx = 1;
            for (Payment p : payments) {
                Row row = sheet.createRow(rowIdx++);

                row.createCell(0).setCellValue(p.getId());
                row.createCell(1).setCellValue(p.getOrderId());
                row.createCell(2).setCellValue(p.getTotalPrice().doubleValue());
                row.createCell(3).setCellValue(p.getPaymentType().name());
                row.createCell(4).setCellValue(p.getPaymentDate().toString());
                row.createCell(5).setCellValue(p.getUserId());
            }

            // ===== Auto-size =====
            for (int i = 0; i < columns.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return out.toByteArray();
        }
        catch (Exception e) {
            throw new RuntimeException("Failed to generate Excel file", e);
        }
    }
}
