package com.cinema.model;

/**
 * Chính sách giá vé cho khách hàng thường.
 * Không áp dụng giảm giá (0%).
 * Giá cuối = giá cơ bản + phụ phí ghế.
 *
 * <p>Polymorphism: Thực thi interface {@link TicketPricePolicy}.</p>
 */
public class NormalCustomerPricePolicy implements TicketPricePolicy {

    /** Tỷ lệ giảm giá: 0% */
    private static final double DISCOUNT_PERCENT = 0.0;

    /**
     * Tính giá vé cho khách hàng thường.
     * Công thức: giá cuối = giá cơ bản + phụ phí ghế (không giảm giá).
     *
     * @param basePrice     giá vé cơ bản (VND)
     * @param seatSurcharge phụ phí ghế (VND)
     * @return giá vé cuối cùng (VND)
     */
    @Override
    public double calculateFinalPrice(double basePrice, double seatSurcharge) {
        double totalBeforeDiscount = basePrice + seatSurcharge;
        double discount = totalBeforeDiscount * DISCOUNT_PERCENT;
        return totalBeforeDiscount - discount;
    }

    /**
     * Lấy tên chính sách giá.
     *
     * @return "Khách thường (0% giảm)"
     */
    @Override
    public String getPolicyName() {
        return "Khách thường (0% giảm)";
    }

    /**
     * Lấy phần trăm giảm giá.
     *
     * @return 0.0
     */
    @Override
    public double getDiscountPercent() {
        return DISCOUNT_PERCENT;
    }

    @Override
    public String toString() {
        return "NormalCustomerPricePolicy{discount=0%}";
    }
}

