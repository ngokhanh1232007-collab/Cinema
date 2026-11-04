package com.cinema.model;

/**
 * Chính sách giá vé cho khách VIP.
 * Giảm 15% trên tổng giá (giá cơ bản + phụ phí ghế).
 * Giá cuối = (giá cơ bản + phụ phí ghế) × 0.85.
 *
 * <p>Polymorphism: Thực thi interface {@link TicketPricePolicy}.</p>
 */
public class VipPricePolicy implements TicketPricePolicy {

    /** Tỷ lệ giảm giá: 15% */
    private static final double DISCOUNT_PERCENT = 0.15;

    /**
     * Tính giá vé cho khách VIP.
     * Công thức: giá cuối = (giá cơ bản + phụ phí ghế) × (1 - 0.15).
     *
     * @param basePrice     giá vé cơ bản (VND)
     * @param seatSurcharge phụ phí ghế (VND)
     * @return giá vé cuối cùng sau giảm 15% (VND)
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
     * @return "Khách VIP (giảm 15%)"
     */
    @Override
    public String getPolicyName() {
        return "Khách VIP (giảm 15%)";
    }

    /**
     * Lấy phần trăm giảm giá.
     *
     * @return 0.15
     */
    @Override
    public double getDiscountPercent() {
        return DISCOUNT_PERCENT;
    }

    @Override
    public String toString() {
        return "VipPricePolicy{discount=15%}";
    }
}

