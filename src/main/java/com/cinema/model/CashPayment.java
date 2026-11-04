package com.cinema.model;

/**
 * Phương thức thanh toán bằng tiền mặt.
 * Thanh toán trực tiếp tại quầy, luôn thành công nếu số tiền hợp lệ.
 *
 * <p>Polymorphism: Thực thi interface {@link PaymentMethod}.</p>
 */
public class CashPayment implements PaymentMethod {

    /**
     * Thực hiện thanh toán bằng tiền mặt.
     * Thanh toán thành công nếu số tiền lớn hơn 0.
     *
     * @param amount số tiền cần thanh toán (VND)
     * @return true nếu số tiền hợp lệ (> 0), false nếu không
     */
    @Override
    public boolean pay(double amount) {
        if (amount <= 0) {
            System.out.println("[CashPayment] Số tiền không hợp lệ: " + amount);
            return false;
        }
        System.out.println("=== THANH TOÁN TIỀN MẶT ===");
        System.out.printf("Số tiền cần thanh toán: %,.0f VND%n", amount);
        System.out.println("Khách hàng đã thanh toán tiền mặt tại quầy.");
        System.out.printf("Thanh toán thành công: %,.0f VND%n", amount);
        System.out.println("============================");
        return true;
    }

    /**
     * Lấy tên phương thức thanh toán.
     *
     * @return "Tiền mặt"
     */
    @Override
    public String getMethodName() {
        return "Tiền mặt";
    }

    /**
     * Lấy mã phương thức thanh toán.
     *
     * @return "CASH"
     */
    @Override
    public String getMethodCode() {
        return "CASH";
    }

    @Override
    public String toString() {
        return "CashPayment{method='Tiền mặt'}";
    }
}

