package com.handbagstore.bll;

import com.handbagstore.dal.InventoryDAL;
import com.handbagstore.dal.SystemLogDAL;
import com.handbagstore.dto.ImportBatchDTO;
import com.handbagstore.dto.InventoryLogDTO;
import com.handbagstore.dto.SystemLogDTO;
import com.handbagstore.utils.Config;

import java.sql.SQLException;
import java.util.List;

public class InventoryBLL {
    private final InventoryDAL inventoryDAL = new InventoryDAL();
    private final SystemLogDAL logDAL = new SystemLogDAL();

    public List<InventoryLogDTO> getAll() throws SQLException {
        return inventoryDAL.getAll();
    }

    public InventoryLogDTO getByProductId(int productId) throws SQLException {
        return inventoryDAL.getByProductId(productId);
    }

    public int getAvailableQuantity(int productId) throws SQLException {
        return inventoryDAL.getAvailableQuantity(productId);
    }

    /** Lấy danh sách sản phẩm tồn kho thấp (≤ threshold từ config) */
    public List<InventoryLogDTO> getLowStockItems() throws SQLException {
        int threshold = Config.getInstance().getLowStockThreshold();
        return inventoryDAL.getLowStockItems(threshold);
    }

    /** Nhập kho — ghi lô hàng + cập nhật số lượng */
    public void importStock(ImportBatchDTO batch) throws SQLException {
        if (batch.getQuantity() <= 0) throw new RuntimeException("Số lượng nhập phải lớn hơn 0!");
        inventoryDAL.insertImportBatch(batch);

        if (AccountBLL.getCurrentUser() != null) {
            logDAL.insert(new SystemLogDTO(AccountBLL.getCurrentUser().getAccountId(),
                    "NHẬP KHO", "Product ID: " + batch.getProductId() + ", SL: " + batch.getQuantity()));
        }
    }

    /** Lấy lịch sử nhập kho */
    public List<ImportBatchDTO> getImportHistory() throws SQLException {
        return inventoryDAL.getImportHistory();
    }

    /** Kiểm tra đủ hàng trước khi tạo đơn */
    public boolean checkAvailability(int productId, int requestedQty) throws SQLException {
        int available = inventoryDAL.getAvailableQuantity(productId);
        return available >= requestedQty;
    }

    /** Cập nhật số lượng tồn kho (delta > 0: nhập/trả, delta < 0: xuất/bán) */
    public void updateQuantity(int productId, int delta) throws SQLException {
        inventoryDAL.updateQuantity(productId, delta);
    }
}
