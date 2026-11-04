package com.cinema.model;

/**
 * Interface định nghĩa phương thức thanh toán.
 * Áp dụng tính đa hình (Polymorphism): mỗi phương thức thanh toán
 * có cách xử lý riêng biệt.
 *
 * <p>Các class thực thi:
 * <ul>
 *   <li>{@link CashPayment} - Thanh toán bằng tiền mặt</li>
 *   <li>{@link BankTransferPayment} - Thanh toán bằng chuyển khoản ngân hàng</li>
 *   <li>{@link EWalletPayment} - Thanh toán bằng ví điện tử</li>
 * </ul>
 */
public interface PaymentMethod {

    /**
     * Thực hiện thanh toán với số tiền cho trước.
     *
     * @param amount số tiền cần thanh toán (VND)
     * @return true nếu thanh toán thành công, false nếu thất bại
     */
    boolean pay(double amount);

    /**
     * Lấy tên phương thức thanh toán.
     *
     * @return tên phương thức thanh toán bằng tiếng Việt
     */
    String getMethodName();

    /**
     * Lấy mã phương thức thanh toán (để lưu vào DB).
     *
     * @return mã phương thức (CASH, BANK_TRANSFER, E_WALLET)
     */
    String getMethodCode();
}

