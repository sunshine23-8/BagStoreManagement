package com.handbagstore.bll;

import com.handbagstore.dal.DiscountDAL;
import com.handbagstore.dto.DiscountDTO;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

public class DiscountBLL {
    private final DiscountDAL discountDAL = new DiscountDAL();

    public List<DiscountDTO> getAll() throws SQLException {
        return discountDAL.getAll();
    }

    public List<DiscountDTO> getActive() throws SQLException {
        return discountDAL.getActive();
    }

    /** Lấy mã giảm giá sinh nhật đang hoạt động */
    public List<DiscountDTO> getActiveBirthdayDiscounts() throws SQLException {
        return discountDAL.getActiveBirthdayDiscounts();
    }

    public DiscountDTO getByCode(String code) throws SQLException {
        return discountDAL.getByCode(code);
    }

    /** Validate và lấy thông tin mã giảm giá */
    public DiscountDTO validateCode(String code) throws SQLException {
        DiscountDTO discount = discountDAL.getByCode(code);
        if (discount == null) throw new RuntimeException("Mã giảm giá không tồn tại!");
        if (!discount.isActive()) throw new RuntimeException("Mã giảm giá đã bị vô hiệu hóa!");

        LocalDateTime now = LocalDateTime.now();
        if (discount.getStartTime() != null && now.isBefore(discount.getStartTime()))
            throw new RuntimeException("Mã giảm giá chưa có hiệu lực!");
        if (discount.getEndTime() != null && now.isAfter(discount.getEndTime()))
            throw new RuntimeException("Mã giảm giá đã hết hạn!");

        return discount;
    }

    /**
     * Tính số tiền được giảm.
     * @return số tiền giảm (luôn >= 0)
     */
    public BigDecimal calculateDiscount(DiscountDTO discount, BigDecimal subtotal) {
        if (discount == null) return BigDecimal.ZERO;

        // Kiểm tra min_order_amt
        if (discount.getMinOrderAmt() != null &&
            subtotal.compareTo(discount.getMinOrderAmt()) < 0) {
            return BigDecimal.ZERO; // Chưa đạt ngưỡng tối thiểu
        }

        BigDecimal discountAmt;
        if (discount.isPercentType()) {
            discountAmt = subtotal.multiply(discount.getValue())
                    .divide(BigDecimal.valueOf(100), 0, RoundingMode.FLOOR);
        } else {
            discountAmt = discount.getValue();
        }

        // Không giảm quá tổng tiền
        if (discountAmt.compareTo(subtotal) > 0) {
            discountAmt = subtotal;
        }

        return discountAmt;
    }

    /**
     * Kiểm tra subtotal có đạt ngưỡng tối thiểu không.
     * @return số tiền cần mua thêm (0 nếu đã đạt)
     */
    public BigDecimal getAmountNeeded(DiscountDTO discount, BigDecimal subtotal) {
        if (discount.getMinOrderAmt() == null) return BigDecimal.ZERO;
        BigDecimal diff = discount.getMinOrderAmt().subtract(subtotal);
        return diff.compareTo(BigDecimal.ZERO) > 0 ? diff : BigDecimal.ZERO;
    }

    public int createDiscount(DiscountDTO discount) throws SQLException {
        if (discount.getCode() == null || discount.getCode().isEmpty())
            throw new RuntimeException("Mã giảm giá không được để trống!");
        if (discountDAL.getByCode(discount.getCode()) != null)
            throw new RuntimeException("Mã giảm giá đã tồn tại!");
        return discountDAL.insert(discount);
    }

    public void updateDiscount(DiscountDTO discount) throws SQLException {
        discountDAL.update(discount);
    }

    public void deactivate(int discountId) throws SQLException {
        discountDAL.deactivate(discountId);
    }

    public void toggleActive(int discountId, boolean active) throws SQLException {
        DiscountDTO d = discountDAL.getById(discountId);
        if (d == null) return;
        d.setActive(active);
        discountDAL.update(d);
    }
}
