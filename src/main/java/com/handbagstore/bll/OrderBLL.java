package com.handbagstore.bll;

import com.handbagstore.dal.*;
import com.handbagstore.dto.*;
import com.handbagstore.utils.Config;
import com.handbagstore.utils.OrderTimerManager;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

/**
 * BLL trung tâm — xử lý toàn bộ luồng tạo đơn hàng, thanh toán, hủy đơn.
 */
public class OrderBLL {
    private final InvoiceDAL invoiceDAL = new InvoiceDAL();
    private final InvoiceDetailDAL detailDAL = new InvoiceDetailDAL();
    private final InventoryDAL inventoryDAL = new InventoryDAL();
    private final SystemLogDAL logDAL = new SystemLogDAL();

    /**
     * Tạo đơn hàng mới (PENDING).
     * 1. Insert invoice status=PENDING, expiresAt=now+timeout
     * 2. Reserve quantity cho từng sản phẩm
     * 3. Insert invoice details
     * 4. Schedule auto-cancel sau timeout
     *
     * @param invoice     thông tin hóa đơn (chưa cần payment info)
     * @param details     danh sách sản phẩm + số lượng
     * @param uiCallback  callback để UI cập nhật khi auto-cancel (chạy trên thread khác!)
     * @return invoice ID đã tạo
     */
    public int createPendingOrder(InvoiceDTO invoice, List<InvoiceDetailDTO> details,
                                  Runnable uiCallback) throws SQLException {
        // Validate tồn kho
        for (InvoiceDetailDTO d : details) {
            int available = inventoryDAL.getAvailableQuantity(d.getProductId());
            if (available < d.getQuantity()) {
                ProductDAL productDAL = new ProductDAL();
                ProductDTO p = productDAL.getById(d.getProductId());
                throw new RuntimeException("Sản phẩm " + (p != null ? p.getName() : d.getProductId()) +
                        " chỉ còn " + available + " trong kho!");
            }
        }

        // Generate invoice code
        String invoiceCode = invoiceDAL.generateInvoiceCode();
        invoice.setInvoiceCode(invoiceCode);
        invoice.setStatus("PENDING");

        // Set timeout
        int timeoutMinutes = Config.getInstance().getPendingTimeoutMinutes();
        invoice.setExpiresAt(LocalDateTime.now().plusMinutes(timeoutMinutes));

        // Insert invoice
        int invoiceId = invoiceDAL.insert(invoice);

        // Set invoice_id cho details và insert
        for (InvoiceDetailDTO d : details) {
            d.setInvoiceId(invoiceId);
        }
        detailDAL.insertBatch(details);

        // Reserve quantity
        for (InvoiceDetailDTO d : details) {
            inventoryDAL.reserveQuantity(d.getProductId(), d.getQuantity());
        }

        // Schedule auto-cancel
        long delayMs = timeoutMinutes * 60L * 1000L;
        final int fInvoiceId = invoiceId;
        OrderTimerManager.getInstance().scheduleCancelTask(invoiceId, () -> {
            try {
                autoCancelOrder(fInvoiceId);
                if (uiCallback != null) uiCallback.run();
            } catch (SQLException e) {
                System.err.println("Auto-cancel failed for invoice " + fInvoiceId + ": " + e.getMessage());
            }
        }, delayMs);

        // Log
        logDAL.insert(new SystemLogDTO(invoice.getStaffId(), "TẠO ĐƠN PENDING",
                "Mã HĐ: " + invoiceCode + ", Tổng: " + invoice.getTotal()));

        return invoiceId;
    }

    /**
     * Thanh toán ngay (không qua pending).
     */
    public int createAndPayOrder(InvoiceDTO invoice, List<InvoiceDetailDTO> details) throws SQLException {
        // Validate tồn kho
        for (InvoiceDetailDTO d : details) {
            int available = inventoryDAL.getAvailableQuantity(d.getProductId());
            if (available < d.getQuantity()) {
                throw new RuntimeException("Không đủ hàng trong kho!");
            }
        }

        String invoiceCode = invoiceDAL.generateInvoiceCode();
        invoice.setInvoiceCode(invoiceCode);
        invoice.setStatus("PAID");
        invoice.setPaidAt(LocalDateTime.now());

        int invoiceId = invoiceDAL.insert(invoice);

        for (InvoiceDetailDTO d : details) {
            d.setInvoiceId(invoiceId);
        }
        detailDAL.insertBatch(details);

        // Trừ kho thật (không reserve)
        for (InvoiceDetailDTO d : details) {
            inventoryDAL.updateQuantity(d.getProductId(), -d.getQuantity());
        }

        logDAL.insert(new SystemLogDTO(invoice.getStaffId(), "THANH TOÁN NGAY",
                "Mã HĐ: " + invoiceCode + ", Tổng: " + invoice.getTotal()));
        return invoiceId;
    }

    /**
     * Thanh toán đơn PENDING (nhân viên bấm nút thanh toán).
     * - Cancel timer
     * - Update status PAID
     * - Trừ kho thật + giải phóng reserved
     */
    public void payPendingOrder(int invoiceId, String paymentMethod,
                                BigDecimal received, BigDecimal change) throws SQLException {
        InvoiceDTO invoice = invoiceDAL.getById(invoiceId);
        if (invoice == null) throw new RuntimeException("Không tìm thấy hóa đơn!");
        if (!invoice.isPending()) throw new RuntimeException("Hóa đơn không ở trạng thái PENDING!");

        // Cancel auto-cancel timer
        OrderTimerManager.getInstance().cancelTask(invoiceId);

        // Update payment info
        invoiceDAL.updatePaymentInfo(invoiceId, paymentMethod, received, change);

        // Trừ kho thật + giải phóng reserve
        List<InvoiceDetailDTO> details = detailDAL.getByInvoiceId(invoiceId);
        for (InvoiceDetailDTO d : details) {
            inventoryDAL.confirmSold(d.getProductId(), d.getQuantity());
        }

        logDAL.insert(new SystemLogDTO(invoice.getStaffId(), "THANH TOÁN ĐƠN PENDING",
                "Mã HĐ: " + invoice.getInvoiceCode()));
    }

    /**
     * Hủy đơn PENDING (nhân viên bấm nút hủy thủ công).
     */
    public void cancelPendingOrder(int invoiceId) throws SQLException {
        InvoiceDTO invoice = invoiceDAL.getById(invoiceId);
        if (invoice == null) throw new RuntimeException("Không tìm thấy hóa đơn!");
        if (!invoice.isPending()) throw new RuntimeException("Hóa đơn không ở trạng thái PENDING!");

        // Cancel timer
        OrderTimerManager.getInstance().cancelTask(invoiceId);

        // Update status
        invoiceDAL.updateStatus(invoiceId, "CANCELLED");

        // Release reserve → trả lại kho
        List<InvoiceDetailDTO> details = detailDAL.getByInvoiceId(invoiceId);
        for (InvoiceDetailDTO d : details) {
            inventoryDAL.releaseReserve(d.getProductId(), d.getQuantity());
        }

        int staffId = AccountBLL.getCurrentUser() != null ? AccountBLL.getCurrentUser().getAccountId() : invoice.getStaffId();
        logDAL.insert(new SystemLogDTO(staffId, "HỦY ĐƠN THỦ CÔNG",
                "Mã HĐ: " + invoice.getInvoiceCode()));
    }

    /**
     * Auto-cancel khi hết thời gian pending (gọi bởi OrderTimerManager).
     */
    private void autoCancelOrder(int invoiceId) throws SQLException {
        InvoiceDTO invoice = invoiceDAL.getById(invoiceId);
        if (invoice == null || !invoice.isPending()) return;

        invoiceDAL.updateStatus(invoiceId, "CANCELLED");

        List<InvoiceDetailDTO> details = detailDAL.getByInvoiceId(invoiceId);
        for (InvoiceDetailDTO d : details) {
            inventoryDAL.releaseReserve(d.getProductId(), d.getQuantity());
        }

        logDAL.insert(new SystemLogDTO(invoice.getStaffId(), "AUTO-CANCEL",
                "Mã HĐ: " + invoice.getInvoiceCode() + " - Hết thời gian pending"));
    }

    /** Lấy danh sách đơn PENDING đang chờ */
    public List<InvoiceDTO> getPendingOrders() throws SQLException {
        return invoiceDAL.getPendingOrders();
    }

    /** Lấy chi tiết hóa đơn */
    public List<InvoiceDetailDTO> getInvoiceDetails(int invoiceId) throws SQLException {
        return detailDAL.getByInvoiceId(invoiceId);
    }

    /** Lấy hóa đơn theo ID */
    public InvoiceDTO getInvoiceById(int invoiceId) throws SQLException {
        return invoiceDAL.getById(invoiceId);
    }
}
