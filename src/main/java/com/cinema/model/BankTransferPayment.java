package com.cinema.model;

/**
 * Phương thức thanh toán bằng chuyển khoản ngân hàng.
 * Mô phỏng quá trình chuyển khoản qua tài khoản ngân hàng.
 *
 * <p>Polymorphism: Thực thi interface {@link PaymentMethod}.</p>
 */
public class BankTransferPayment implements PaymentMethod {

    /** Tên ngân hàng thụ hưởng */
    private String bankName;

    /** Số tài khoản thụ hưởng */
    private String accountNumber;

    /** Tên chủ tài khoản thụ hưởng */
    private String accountHolder;

    /**
     * Constructor mặc định với thông tin ngân hàng mặc định.
     */
    public BankTransferPayment() {
        this.bankName = "VietinBank - CN SON LA - HOI SO";
        this.accountNumber = "102883583080";
        this.accountHolder = "NGO HOANG KHANH";
    }

    /**
     * Constructor với thông tin ngân hàng tùy chỉnh.
     *
     * @param bankName      tên ngân hàng
     * @param accountNumber số tài khoản
     * @param accountHolder tên chủ tài khoản
     */
    public BankTransferPayment(String bankName, String accountNumber, String accountHolder) {
        this.bankName = bankName;
        this.accountNumber = accountNumber;
        this.accountHolder = accountHolder;
    }

    /**
     * Thực hiện thanh toán bằng chuyển khoản ngân hàng.
     * Mô phỏng: hiển thị thông tin chuyển khoản và xác nhận thành công.
     *
     * @param amount số tiền cần thanh toán (VND)
     * @return true nếu số tiền hợp lệ (> 0), false nếu không
     */
    @Override
    public boolean pay(double amount) {
        if (amount <= 0) {
            System.out.println("[BankTransferPayment] Số tiền không hợp lệ: " + amount);
            return false;
        }
        System.out.println("=== THANH TOÁN CHUYỂN KHOẢN NGÂN HÀNG ===");
        System.out.println("Ngân hàng:        " + bankName);
        System.out.println("Số tài khoản:     " + accountNumber);
        System.out.println("Chủ tài khoản:    " + accountHolder);
        System.out.printf("Số tiền chuyển:    %,.0f VND%n", amount);
        System.out.println("Đang xử lý chuyển khoản...");
        System.out.printf("Chuyển khoản thành công: %,.0f VND%n", amount);
        System.out.println("==========================================");
        return true;
    }

    /**
     * Lấy tên phương thức thanh toán.
     *
     * @return "Chuyển khoản ngân hàng"
     */
    @Override
    public String getMethodName() {
        return "Chuyển khoản ngân hàng";
    }

    /**
     * Lấy mã phương thức thanh toán.
     *
     * @return "BANK_TRANSFER"
     */
    @Override
    public String getMethodCode() {
        return "BANK_TRANSFER";
    }

    // ===================== GETTER / SETTER =====================

    public String getBankName() {
        return bankName;
    }

    public void setBankName(String bankName) {
        this.bankName = bankName;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }

    public String getAccountHolder() {
        return accountHolder;
    }

    public void setAccountHolder(String accountHolder) {
        this.accountHolder = accountHolder;
    }

    @Override
    public String toString() {
        return "BankTransferPayment{" +
                "bankName='" + bankName + '\'' +
                ", accountNumber='" + accountNumber + '\'' +
                ", accountHolder='" + accountHolder + '\'' +
                '}';
    }
}

