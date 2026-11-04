package com.cinema.model;

/**
 * Chính sách giá vé cho sinh viên.
 * Giảm 20% trên tổng giá (giá cơ bản + phụ phí ghế).
 * Giá cuối = (giá cơ bản + phụ phí ghế) × 0.8.
 *
 * <p>Polymorphism: Thực thi interface {@link TicketPricePolicy}.</p>
 */
public class StudentPricePolicy implements TicketPricePolicy {

    /** Tỷ lệ giảm giá: 20% */
    private static final double DISCOUNT_PERCENT = 0.2;

    /**
     * Tính giá vé cho sinh viên.
     * Công thức: giá cuối = (giá cơ bản + phụ phí ghế) × (1 - 0.2).
     *
     * @param basePrice     giá vé cơ bản (VND)
     * @param seatSurcharge phụ phí ghế (VND)
     * @return giá vé cuối cùng sau giảm 20% (VND)
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
     * @return "Sinh viên (giảm 20%)"
     */
    @Override
    public String getPolicyName() {
        return "Sinh viên (giảm 20%)";
    }

    /**
     * Lấy phần trăm giảm giá.
     *
     * @return 0.2
     */
    @Override
    public double getDiscountPercent() {
        return DISCOUNT_PERCENT;
    }

    @Override
    public String toString() {
        return "StudentPricePolicy{discount=20%}";
    }
}

