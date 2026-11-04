package com.cinema.model;

/**
 * Interface định nghĩa chính sách tính giá vé.
 * Áp dụng tính đa hình (Polymorphism): mỗi loại khách hàng có một
 * chính sách tính giá vé riêng biệt.
 *
 * <p>Các class thực thi:
 * <ul>
 *   <li>{@link NormalCustomerPricePolicy} - Khách thường (0% giảm)</li>
 *   <li>{@link StudentPricePolicy} - Sinh viên (giảm 20%)</li>
 *   <li>{@link VipPricePolicy} - Khách VIP (giảm 15%)</li>
 * </ul>
 */
public interface TicketPricePolicy {

    /**
     * Tính giá vé cuối cùng dựa trên giá vé cơ bản và phụ phí ghế.
     *
     * @param basePrice     giá vé cơ bản của suất chiếu (VND)
     * @param seatSurcharge phụ phí loại ghế (VND) - 0 cho ghế thường,
     *                      30000 cho ghế đôi, 50000 cho ghế VIP
     * @return giá vé cuối cùng sau khi áp dụng chính sách (VND)
     */
    double calculateFinalPrice(double basePrice, double seatSurcharge);

    /**
     * Lấy tên chính sách giá.
     *
     * @return tên chính sách
     */
    String getPolicyName();

    /**
     * Lấy phần trăm giảm giá.
     *
     * @return phần trăm giảm (0.0 - 1.0)
     */
    double getDiscountPercent();
}

