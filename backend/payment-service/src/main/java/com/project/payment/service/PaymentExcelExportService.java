package com.project.payment.service;

import com.project.payment.client.EmployeeClient;
import com.project.payment.client.OrderClient;
import com.project.payment.client.ProductClient;
import com.project.payment.dto.OrderItemSummary;
import com.project.payment.dto.OrderSummaryResponse;
import com.project.payment.dto.ProductResponse;
import com.project.payment.model.Payment;
import com.project.payment.repository.PaymentRepository;

import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.*;

import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentExcelExportService {

    private final PaymentRepository paymentRepository;
    private final EmployeeClient    employeeClient;
    private final OrderClient       orderClient;
    private final ProductClient     productClient;

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // =========================================================
    // DAILY EXCEL
    // =========================================================
    public byte[] exportDaily(LocalDate date) {

        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end   = date.atTime(23, 59, 59);

        List<Payment> payments =
                paymentRepository.findByPaymentDateBetween(start, end);

        return buildExcel(
                payments,
                "Daily Report - " + date,
                "Daily Payments — " + date
        );
    }

    // =========================================================
    // PERIOD EXCEL
    // =========================================================
    public byte[] exportPeriod(LocalDate from, LocalDate to) {

        if (from.isAfter(to)) {
            throw new IllegalArgumentException(
                    "From date must be before To date");
        }

        LocalDateTime start = from.atStartOfDay();
        LocalDateTime end   = to.atTime(23, 59, 59);

        List<Payment> payments =
                paymentRepository.findByPaymentDateBetween(start, end);

        return buildExcel(
                payments,
                "Payments " + from + " to " + to,
                "Payments Report — " + from + " to " + to
        );
    }

    // =========================================================
    // EXCEL BUILDER
    // =========================================================
    private byte[] buildExcel(List<Payment> payments,
                               String sheetName,
                               String reportTitle) {

        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            String safeName = sheetName
                    .replace("/", "-")
                    .replace(":", "-");

            // trim اسم الـ sheet — Excel يقبل 31 حرف فقط
            if (safeName.length() > 31) {
                safeName = safeName.substring(0, 31);
            }

            XSSFSheet sheet = workbook.createSheet(safeName);

            // ===== Column widths =====
            sheet.setColumnWidth(0, 4000);   // Payment ID
            sheet.setColumnWidth(1, 4000);   // Order ID
            sheet.setColumnWidth(2, 5000);   // Total Price
            sheet.setColumnWidth(3, 5000);   // Payment Type
            sheet.setColumnWidth(4, 8000);   // Payment Date
            sheet.setColumnWidth(5, 8000);   // Employee Name

            // ===== Styles =====
            XSSFCellStyle titleStyle   = createTitleStyle(workbook);
            XSSFCellStyle headerStyle  = createHeaderStyle(workbook);
            XSSFCellStyle dataStyle    = createDataStyle(workbook);
            XSSFCellStyle amountStyle  = createAmountStyle(workbook);
            XSSFCellStyle summaryStyle = createSummaryStyle(workbook);
            XSSFCellStyle summaryAmountStyle = createSummaryAmountStyle(workbook);
            XSSFCellStyle altDataStyle = createAltDataStyle(workbook);
            XSSFCellStyle altAmountStyle = createAltAmountStyle(workbook);

            int rowIdx = 0;

            // ===== Title Row =====
            Row titleRow = sheet.createRow(rowIdx++);
            titleRow.setHeight((short) 800);

            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue(reportTitle);
            titleCell.setCellStyle(titleStyle);
            sheet.addMergedRegion(
                    new CellRangeAddress(0, 0, 0, 5));

            // ===== Empty row =====
            sheet.createRow(rowIdx++);

            // ===== Header Row =====
            Row headerRow = sheet.createRow(rowIdx++);
            headerRow.setHeight((short) 600);

            String[] columns = {
                "Payment ID",
                "Order ID",
                "Total Price (₪)",
                "Payment Type",
                "Payment Date",
                "Employee Name"
            };

            for (int i = 0; i < columns.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(columns[i]);
                cell.setCellStyle(headerStyle);
            }

            // ===== Data Rows =====
            double grandTotal = 0;
            boolean alternate = false;

            for (Payment p : payments) {

                Row row = sheet.createRow(rowIdx++);
                row.setHeight((short) 450);

                XSSFCellStyle currentData   = alternate ? altDataStyle   : dataStyle;
                XSSFCellStyle currentAmount = alternate ? altAmountStyle  : amountStyle;
                alternate = !alternate;

                // Payment ID
                Cell idCell = row.createCell(0);
                idCell.setCellValue(p.getId());
                idCell.setCellStyle(currentData);

                // Order ID
                Cell orderCell = row.createCell(1);
                orderCell.setCellValue(p.getOrderId());
                orderCell.setCellStyle(currentData);

                // Total Price
                BigDecimal resolvedTotal = resolvePaymentTotal(p);
                Cell priceCell = row.createCell(2);
                priceCell.setCellValue(
                        resolvedTotal.doubleValue());
                priceCell.setCellStyle(currentAmount);
                grandTotal += resolvedTotal.doubleValue();

                // Payment Type
                Cell typeCell = row.createCell(3);
                typeCell.setCellValue(p.getPaymentType().name());
                typeCell.setCellStyle(currentData);

                // Payment Date
                Cell dateCell = row.createCell(4);
                dateCell.setCellValue(
                        p.getPaymentDate().format(DATE_FMT));
                dateCell.setCellStyle(currentData);

                // Employee Name
                Cell empCell = row.createCell(5);
                empCell.setCellValue(
                        fetchEmployeeName(p.getUserId()));
                empCell.setCellStyle(currentData);
            }

            // ===== Empty row before summary =====
            sheet.createRow(rowIdx++);

            // ===== Summary Row =====
            Row summaryRow = sheet.createRow(rowIdx);
            summaryRow.setHeight((short) 550);

            Cell labelCell = summaryRow.createCell(0);
            labelCell.setCellValue(
                    "Total Payments: " + payments.size());
            labelCell.setCellStyle(summaryStyle);
            sheet.addMergedRegion(
                    new CellRangeAddress(rowIdx, rowIdx, 0, 1));

            // خلية فارغة للـ merge
            summaryRow.createCell(1).setCellStyle(summaryStyle);

            Cell totalCell = summaryRow.createCell(2);
            totalCell.setCellValue(grandTotal);
            totalCell.setCellStyle(summaryAmountStyle);

            // باقي الخلايا في صف الـ summary
            for (int i = 3; i <= 5; i++) {
                summaryRow.createCell(i)
                          .setCellStyle(summaryStyle);
            }

            workbook.write(out);
            return out.toByteArray();

        }
        catch (Exception e) {
            throw new RuntimeException(
                    "Failed to generate Excel file", e);
        }
    }

    // =========================================================
    // FETCH EMPLOYEE NAME
    // =========================================================
    private String fetchEmployeeName(Long userId) {
        try {
            return employeeClient
                    .getEmployee(userId)
                    .getFullName();
        }
        catch (FeignException e) {
            log.warn("Could not fetch employee name for userId={}",
                    userId);
            return "ID: " + userId;
        }
    }

    private BigDecimal resolvePaymentTotal(Payment payment) {
        if (payment.getTotalPrice() != null
                && payment.getTotalPrice().compareTo(BigDecimal.ZERO) > 0) {
            return payment.getTotalPrice();
        }

        try {
            OrderSummaryResponse order =
                    orderClient.getOrderById(payment.getOrderId());
            return calculateOrderTotal(order);
        }
        catch (Exception e) {
            log.warn(
                    "Could not recalculate total for paymentId={}, orderId={}",
                    payment.getId(),
                    payment.getOrderId(),
                    e);
            return payment.getTotalPrice() == null
                    ? BigDecimal.ZERO
                    : payment.getTotalPrice();
        }
    }

    private BigDecimal calculateOrderTotal(OrderSummaryResponse order) {
        BigDecimal total = BigDecimal.ZERO;

        for (OrderItemSummary item : order.getItems()) {
            ProductResponse product =
                    productClient.getProduct(item.getProductId());
            String productType = product.getProductType() == null
                    ? ""
                    : product.getProductType();

            if (isOlivePressingProduct(productType)) {
                total = total.add(
                        calculateKgPrice(item.getQuantity(), order.isMember()));
            }
            else if (isPurchaseProduct(productType)) {
                if ("KG".equalsIgnoreCase(product.getUnit())) {
                    total = total.add(
                            calculateKgPrice(item.getQuantity(), order.isMember()));
                }
                else {
                    total = total.add(product.getPrice()
                            .multiply(BigDecimal.valueOf(item.getQuantity())));
                }
            }
        }

        return total;
    }

    private BigDecimal calculateKgPrice(double weight, boolean isMember) {
        double rate;

        if (isMember) {
            rate = 0.4;
        }
        else {
            rate = (weight >= 100) ? 0.6 : 0.8;
        }

        return BigDecimal.valueOf(weight * rate);
    }

    private boolean isOlivePressingProduct(String productType) {
        return "SERVICE".equalsIgnoreCase(productType)
                || "OLIVE".equalsIgnoreCase(productType);
    }

    private boolean isPurchaseProduct(String productType) {
        return "PURCHASE".equalsIgnoreCase(productType)
                || "JIFT".equalsIgnoreCase(productType)
                || "GALLON".equalsIgnoreCase(productType);
    }

    // =========================================================
    // STYLES
    // =========================================================

    // Title — أخضر غامق
    private XSSFCellStyle createTitleStyle(XSSFWorkbook wb) {
        XSSFCellStyle style = wb.createCellStyle();
        XSSFFont font = wb.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 16);
        font.setColor(new XSSFColor(
                new byte[]{(byte)255, (byte)255, (byte)255}, null));
        style.setFont(font);
        style.setFillForegroundColor(
                new XSSFColor(new byte[]{
                        (byte)34, (byte)85, (byte)51}, null));
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        return style;
    }

    // Header — أخضر متوسط
    private XSSFCellStyle createHeaderStyle(XSSFWorkbook wb) {
        XSSFCellStyle style = wb.createCellStyle();
        XSSFFont font = wb.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 11);
        font.setColor(new XSSFColor(
                new byte[]{(byte)255, (byte)255, (byte)255}, null));
        style.setFont(font);
        style.setFillForegroundColor(
                new XSSFColor(new byte[]{
                        (byte)46, (byte)117, (byte)70}, null));
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        setBorders(style, BorderStyle.THIN,
                new XSSFColor(new byte[]{
                        (byte)255,(byte)255,(byte)255}, null));
        return style;
    }

    // Data — أبيض
    private XSSFCellStyle createDataStyle(XSSFWorkbook wb) {
        XSSFCellStyle style = wb.createCellStyle();
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        setBorders(style, BorderStyle.THIN,
                new XSSFColor(new byte[]{
                        (byte)198,(byte)224,(byte)180}, null));
        return style;
    }

    // Data — أخضر فاتح جداً (alternate)
    private XSSFCellStyle createAltDataStyle(XSSFWorkbook wb) {
        XSSFCellStyle style = wb.createCellStyle();
        style.setFillForegroundColor(
                new XSSFColor(new byte[]{
                        (byte)235,(byte)246,(byte)228}, null));
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        setBorders(style, BorderStyle.THIN,
                new XSSFColor(new byte[]{
                        (byte)198,(byte)224,(byte)180}, null));
        return style;
    }

    // Amount — أبيض مع format
    private XSSFCellStyle createAmountStyle(XSSFWorkbook wb) {
        XSSFCellStyle style = wb.createCellStyle();
        style.setDataFormat(
                wb.createDataFormat().getFormat("#,##0.00"));
        style.setAlignment(HorizontalAlignment.RIGHT);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        setBorders(style, BorderStyle.THIN,
                new XSSFColor(new byte[]{
                        (byte)198,(byte)224,(byte)180}, null));
        return style;
    }

    // Amount alternate
    private XSSFCellStyle createAltAmountStyle(XSSFWorkbook wb) {
        XSSFCellStyle style = wb.createCellStyle();
        style.setFillForegroundColor(
                new XSSFColor(new byte[]{
                        (byte)235,(byte)246,(byte)228}, null));
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setDataFormat(
                wb.createDataFormat().getFormat("#,##0.00"));
        style.setAlignment(HorizontalAlignment.RIGHT);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        setBorders(style, BorderStyle.THIN,
                new XSSFColor(new byte[]{
                        (byte)198,(byte)224,(byte)180}, null));
        return style;
    }

    // Summary label
    private XSSFCellStyle createSummaryStyle(XSSFWorkbook wb) {
        XSSFCellStyle style = wb.createCellStyle();
        XSSFFont font = wb.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 11);
        style.setFont(font);
        style.setFillForegroundColor(
                new XSSFColor(new byte[]{
                        (byte)198,(byte)224,(byte)180}, null));
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        setBorders(style, BorderStyle.MEDIUM,
                new XSSFColor(new byte[]{
                        (byte)46,(byte)117,(byte)70}, null));
        return style;
    }

    // Summary amount
    private XSSFCellStyle createSummaryAmountStyle(XSSFWorkbook wb) {
        XSSFCellStyle style = wb.createCellStyle();
        XSSFFont font = wb.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 12);
        font.setColor(new XSSFColor(
                new byte[]{(byte)34,(byte)85,(byte)51}, null));
        style.setFont(font);
        style.setFillForegroundColor(
                new XSSFColor(new byte[]{
                        (byte)198,(byte)224,(byte)180}, null));
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setDataFormat(
                wb.createDataFormat().getFormat("#,##0.00"));
        style.setAlignment(HorizontalAlignment.RIGHT);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        setBorders(style, BorderStyle.MEDIUM,
                new XSSFColor(new byte[]{
                        (byte)46,(byte)117,(byte)70}, null));
        return style;
    }

    private void setBorders(XSSFCellStyle style,
                             BorderStyle borderStyle,
                             XSSFColor color) {
        style.setBorderTop(borderStyle);
        style.setBorderBottom(borderStyle);
        style.setBorderLeft(borderStyle);
        style.setBorderRight(borderStyle);
        style.setTopBorderColor(color);
        style.setBottomBorderColor(color);
        style.setLeftBorderColor(color);
        style.setRightBorderColor(color);
    }
}
