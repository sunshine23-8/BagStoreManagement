package com.handbagstore.utils;

import java.io.File;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

import com.handbagstore.dto.CustomerDTO;
import com.handbagstore.dto.InvoiceDTO;
import com.handbagstore.dto.InvoiceDetailDTO;
import com.itextpdf.io.font.PdfEncodings;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;

/**
 * Xuất hóa đơn dạng PDF bằng iText 7.
 */
public class PdfExporter {

    private static final NumberFormat CURRENCY_FMT = NumberFormat.getInstance(new Locale("vi", "VN"));

    /**
     * Xuất hóa đơn ra file PDF.
     * 
     * @param outputPath đường dẫn file PDF output
     * @param invoice    thông tin hóa đơn
     * @param details    chi tiết sản phẩm
     * @param customer   khách hàng (nullable = guest)
     */
    public static void exportInvoice(String outputPath, InvoiceDTO invoice,
            List<InvoiceDetailDTO> details, CustomerDTO customer) throws Exception {
        // Tạo thư mục nếu chưa có
        File file = new File(outputPath);
        if (file.getParentFile() != null)
            file.getParentFile().mkdirs();

        PdfWriter writer = new PdfWriter(outputPath);
        PdfDocument pdf = new PdfDocument(writer);
        Document doc = new Document(pdf);

        // Font hỗ trợ tiếng Việt (Arial trên Windows)
        try {
            String fontPath = "C:/Windows/Fonts/arial.ttf";
            PdfFont font = PdfFontFactory.createFont(fontPath, PdfEncodings.IDENTITY_H);
            doc.setFont(font);
        } catch (Exception e) {
            System.err.println("Không thể load font Arial, dùng font mặc định: " + e.getMessage());
        }
        // Tiêu đề
        doc.add(new Paragraph("CỬA HÀNG TÚI XÁCH")
                .setTextAlignment(TextAlignment.CENTER)
                .setFontSize(18).setBold());
        doc.add(new Paragraph("HÓA ĐƠN BÁN HÀNG")
                .setTextAlignment(TextAlignment.CENTER)
                .setFontSize(14));
        doc.add(new Paragraph("\n"));

        // Thông tin hóa đơn
        doc.add(new Paragraph("Mã hóa đơn: " + invoice.getInvoiceCode()));
        doc.add(new Paragraph("Ngày: " + DateUtils.formatDateTime(invoice.getCreatedAt())));
        doc.add(new Paragraph("Nhân viên: " + (invoice.getStaffName() != null ? invoice.getStaffName() : "")));

        if (customer != null) {
            doc.add(new Paragraph("Khách hàng: " + customer.getFullName() + " - " + customer.getPhone()));
        } else {
            doc.add(new Paragraph("Khách hàng: Khách vãng lai"));
        }
        doc.add(new Paragraph("\n"));

        // Bảng chi tiết sản phẩm
        Table table = new Table(UnitValue.createPercentArray(new float[] { 1, 3, 2, 1, 2 }));
        table.setWidth(UnitValue.createPercentValue(100));

        // Header
        table.addHeaderCell(new Cell().add(new Paragraph("STT").setBold()));
        table.addHeaderCell(new Cell().add(new Paragraph("Sản phẩm").setBold()));
        table.addHeaderCell(new Cell().add(new Paragraph("Đơn giá").setBold()));
        table.addHeaderCell(new Cell().add(new Paragraph("SL").setBold()));
        table.addHeaderCell(new Cell().add(new Paragraph("Thành tiền").setBold()));

        int stt = 1;
        for (InvoiceDetailDTO d : details) {
            table.addCell(new Cell().add(new Paragraph(String.valueOf(stt++))));
            table.addCell(new Cell().add(new Paragraph(d.getProductName() != null ? d.getProductName() : "")));
            table.addCell(new Cell().add(new Paragraph(formatMoney(d.getUnitPrice()))));
            table.addCell(new Cell().add(new Paragraph(String.valueOf(d.getQuantity()))));
            table.addCell(new Cell().add(new Paragraph(formatMoney(d.getLineTotal()))));
        }
        doc.add(table);
        doc.add(new Paragraph("\n"));

        // Tổng tiền
        doc.add(new Paragraph("Tạm tính: " + formatMoney(invoice.getSubtotal()))
                .setTextAlignment(TextAlignment.RIGHT));

        if (invoice.getDiscountAmount() != null && invoice.getDiscountAmount().compareTo(BigDecimal.ZERO) > 0) {
            String discountLabel = "Giảm giá";
            if (invoice.getDiscountCode() != null && !invoice.getDiscountCode().isEmpty()) {
                discountLabel += " (" + invoice.getDiscountCode() + ")";
            }

            doc.add(new Paragraph(discountLabel + ": -" + formatMoney(invoice.getDiscountAmount()))
                    .setTextAlignment(TextAlignment.RIGHT));
        }

        doc.add(new Paragraph("TỔNG CỘNG: " + formatMoney(invoice.getTotal()))
                .setTextAlignment(TextAlignment.RIGHT)
                .setFontSize(14).setBold());

        // Phương thức thanh toán
        String pm = "CASH".equals(invoice.getPaymentMethod()) ? "Tiền mặt" : "Chuyển khoản";
        doc.add(new Paragraph("Phương thức: " + pm)
                .setTextAlignment(TextAlignment.RIGHT));

        if ("CASH".equals(invoice.getPaymentMethod()) && invoice.getPaymentReceived() != null) {
            doc.add(new Paragraph("Tiền nhận: " + formatMoney(invoice.getPaymentReceived()))
                    .setTextAlignment(TextAlignment.RIGHT));
            doc.add(new Paragraph("Tiền thối: " + formatMoney(invoice.getChangeAmount()))
                    .setTextAlignment(TextAlignment.RIGHT));
        }

        if (invoice.getDiscountCode() != null && invoice.getDiscountCode().contains("Sinh nhật")) {
            doc.add(new Paragraph("Chúc bạn một tuổi mới thật rạng rỡ nhé!")
                    .setTextAlignment(TextAlignment.CENTER).setBold());
        }
        doc.add(new Paragraph("\n"));
        doc.add(new Paragraph("Cảm ơn quý khách!")
                .setTextAlignment(TextAlignment.CENTER).setItalic());

        doc.close();
    }

    private static String formatMoney(BigDecimal amount) {
        if (amount == null)
            return "0";
        return CURRENCY_FMT.format(amount) + "đ";
    }
}
