package com.cinema.model;

/**
 * Phương thức thanh toán bằng ví điện tử (E-Wallet).
 * Mô phỏng thanh toán qua các ví điện tử như MoMo, ZaloPay, VNPay...
 *
 * <p>Polymorphism: Thực thi interface {@link PaymentMethod}.</p>
 */
public class EWalletPayment implements PaymentMethod {

    /** Tên ví điện tử */
    private String walletName;

    /** Số điện thoại liên kết với ví */
    private String phoneNumber;

    /**
     * Constructor mặc định với ví MoMo.
     */
    public EWalletPayment() {
        this.walletName = "MoMo";
        this.phoneNumber = "";
    }

    /**
     * Constructor với thông tin ví tùy chỉnh.
     *
     * @param walletName  tên ví điện tử (MoMo, ZaloPay, VNPay...)
     * @param phoneNumber số điện thoại liên kết
     */
    public EWalletPayment(String walletName, String phoneNumber) {
        this.walletName = walletName;
        this.phoneNumber = phoneNumber;
    }

    /**
     * Thực hiện thanh toán bằng ví điện tử.
     * Mô phỏng: hiển thị thông tin ví và xác nhận thanh toán thành công.
     *
     * @param amount số tiền cần thanh toán (VND)
     * @return true nếu số tiền hợp lệ (> 0), false nếu không
     */
    @Override
    public boolean pay(double amount) {
        if (amount <= 0) {
            System.out.println("[EWalletPayment] Số tiền không hợp lệ: " + amount);
            return false;
        }
        System.out.println("=== THANH TOÁN VÍ ĐIỆN TỬ ===");
        System.out.println("Ví:              " + walletName);
        if (phoneNumber != null && !phoneNumber.isEmpty()) {
            System.out.println("SĐT liên kết:   " + phoneNumber);
        }
        System.out.printf("Số tiền:          %,.0f VND%n", amount);
        System.out.println("Đang xử lý thanh toán qua " + walletName + "...");
        System.out.printf("Thanh toán %s thành công: %,.0f VND%n", walletName, amount);
        System.out.println("==============================");
        return true;
    }

    /**
     * Lấy tên phương thức thanh toán.
     *
     * @return "Ví điện tử" kèm tên ví cụ thể
     */
    @Override
    public String getMethodName() {
        return "Ví điện tử (" + walletName + ")";
    }

    /**
     * Lấy mã phương thức thanh toán.
     *
     * @return "E_WALLET"
     */
    @Override
    public String getMethodCode() {
        return "E_WALLET";
    }

    // ===================== GETTER / SETTER =====================

    public String getWalletName() {
        return walletName;
    }

    public void setWalletName(String walletName) {
        this.walletName = walletName;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    @Override
    public String toString() {
        return "EWalletPayment{" +
                "walletName='" + walletName + '\'' +
                ", phoneNumber='" + phoneNumber + '\'' +
                '}';
    }
}

