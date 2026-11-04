package com.cinema.utils;

import java.util.regex.Pattern;

/**
 * Lớp tiện ích kiểm tra (validate) dữ liệu đầu vào của người dùng.
 * Đảm bảo dữ liệu hợp lệ trước khi xử lý nghiệp vụ hoặc lưu vào database.
 */
public class Validator {

    // Regex kiểm tra email hợp lệ
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    // Regex kiểm tra số điện thoại Việt Nam (10 số, bắt đầu bằng 0)
    private static final Pattern PHONE_PATTERN =
            Pattern.compile("^0[0-9]{9}$");

    // Regex kiểm tra mã sinh viên (dạng SVyyyyXXXX, ví dụ: SV20210001)
    private static final Pattern STUDENT_ID_PATTERN =
            Pattern.compile("^SV[0-9]{8,}$");

    // Regex kiểm tra tên hợp lệ (chỉ chứa chữ cái, khoảng trắng, dấu tiếng Việt)
    private static final Pattern NAME_PATTERN =
            Pattern.compile("^[\\p{L} ]{2,100}$");

    // Constructor private để ngăn tạo instance
    private Validator() {
    }

    /**
     * Kiểm tra chuỗi có rỗng hoặc null không.
     *
     * @param value chuỗi cần kiểm tra
     * @return true nếu chuỗi null, rỗng hoặc chỉ chứa khoảng trắng
     */
    public static boolean isNullOrEmpty(String value) {
        return value == null || value.trim().isEmpty();
    }

    /**
     * Kiểm tra chuỗi KHÔNG rỗng.
     *
     * @param value chuỗi cần kiểm tra
     * @return true nếu chuỗi có nội dung
     */
    public static boolean isNotEmpty(String value) {
        return !isNullOrEmpty(value);
    }

    /**
     * Kiểm tra email có hợp lệ không.
     *
     * @param email địa chỉ email cần kiểm tra
     * @return true nếu email hợp lệ
     */
    public static boolean isValidEmail(String email) {
        if (isNullOrEmpty(email)) {
            return false;
        }
        return EMAIL_PATTERN.matcher(email.trim()).matches();
    }

    /**
     * Kiểm tra số điện thoại có hợp lệ không.
     * Quy tắc: 10 chữ số, bắt đầu bằng 0.
     *
     * @param phone số điện thoại cần kiểm tra
     * @return true nếu số điện thoại hợp lệ
     */
    public static boolean isValidPhone(String phone) {
        if (isNullOrEmpty(phone)) {
            return false;
        }
        return PHONE_PATTERN.matcher(phone.trim()).matches();
    }

    /**
     * Kiểm tra mã số sinh viên có hợp lệ không.
     * Quy tắc: Bắt đầu bằng "SV" theo sau bởi ít nhất 8 chữ số.
     *
     * @param studentId mã sinh viên cần kiểm tra
     * @return true nếu mã sinh viên hợp lệ
     */
    public static boolean isValidStudentId(String studentId) {
        if (isNullOrEmpty(studentId)) {
            return false;
        }
        return STUDENT_ID_PATTERN.matcher(studentId.trim()).matches();
    }

    /**
     * Kiểm tra tên người dùng có hợp lệ không.
     * Quy tắc: Chỉ chứa chữ cái (bao gồm tiếng Việt) và khoảng trắng,
     * độ dài từ 2 đến 100 ký tự.
     *
     * @param name tên cần kiểm tra
     * @return true nếu tên hợp lệ
     */
    public static boolean isValidName(String name) {
        if (isNullOrEmpty(name)) {
            return false;
        }
        return NAME_PATTERN.matcher(name.trim()).matches();
    }

    /**
     * Kiểm tra số nguyên dương.
     *
     * @param value giá trị cần kiểm tra
     * @return true nếu value > 0
     */
    public static boolean isPositiveInteger(int value) {
        return value > 0;
    }

    /**
     * Kiểm tra số thực dương (dùng cho giá tiền).
     *
     * @param value giá trị cần kiểm tra
     * @return true nếu value > 0
     */
    public static boolean isPositiveDouble(double value) {
        return value > 0;
    }

    /**
     * Kiểm tra loại khách hàng có hợp lệ không.
     * Các giá trị hợp lệ: REGULAR, STUDENT, VIP (không phân biệt hoa thường).
     *
     * @param customerType loại khách hàng cần kiểm tra
     * @return true nếu loại khách hàng hợp lệ
     */
    public static boolean isValidCustomerType(String customerType) {
        if (isNullOrEmpty(customerType)) {
            return false;
        }
        String upper = customerType.trim().toUpperCase();
        return upper.equals("REGULAR") || upper.equals("STUDENT") || upper.equals("VIP");
    }

    /**
     * Kiểm tra loại ghế có hợp lệ không.
     * Các giá trị hợp lệ: REGULAR, DOUBLE, VIP.
     *
     * @param seatType loại ghế cần kiểm tra
     * @return true nếu loại ghế hợp lệ
     */
    public static boolean isValidSeatType(String seatType) {
        if (isNullOrEmpty(seatType)) {
            return false;
        }
        String upper = seatType.trim().toUpperCase();
        return upper.equals("REGULAR") || upper.equals("DOUBLE") || upper.equals("VIP");
    }

    /**
     * Kiểm tra phương thức thanh toán có hợp lệ không.
     * Các giá trị hợp lệ: CASH, CREDIT_CARD, E_WALLET, BANK_TRANSFER.
     *
     * @param paymentMethod phương thức thanh toán cần kiểm tra
     * @return true nếu phương thức hợp lệ
     */
    public static boolean isValidPaymentMethod(String paymentMethod) {
        if (isNullOrEmpty(paymentMethod)) {
            return false;
        }
        String upper = paymentMethod.trim().toUpperCase();
        return upper.equals("CASH") || upper.equals("CREDIT_CARD")
                || upper.equals("E_WALLET") || upper.equals("BANK_TRANSFER");
    }

    /**
     * Kiểm tra lựa chọn menu (số nguyên) có nằm trong phạm vi cho phép không.
     *
     * @param choice giá trị người dùng nhập
     * @param min    giá trị nhỏ nhất cho phép
     * @param max    giá trị lớn nhất cho phép
     * @return true nếu choice nằm trong [min, max]
     */
    public static boolean isValidMenuChoice(int choice, int min, int max) {
        return choice >= min && choice <= max;
    }

    /**
     * Validate và ném BusinessException nếu chuỗi rỗng.
     *
     * @param value     giá trị cần kiểm tra
     * @param fieldName tên trường (để hiển thị trong thông báo lỗi)
     * @throws BusinessException nếu giá trị rỗng
     */
    public static void requireNotEmpty(String value, String fieldName) throws BusinessException {
        if (isNullOrEmpty(value)) {
            throw new BusinessException(BusinessException.INVALID_INPUT,
                    fieldName + " không được để trống.");
        }
    }

    /**
     * Validate email và ném BusinessException nếu không hợp lệ.
     *
     * @param email email cần kiểm tra
     * @throws BusinessException nếu email không hợp lệ
     */
    public static void requireValidEmail(String email) throws BusinessException {
        if (!isValidEmail(email)) {
            throw new BusinessException(BusinessException.INVALID_INPUT,
                    "Email không hợp lệ: " + email);
        }
    }

    /**
     * Validate số điện thoại và ném BusinessException nếu không hợp lệ.
     *
     * @param phone số điện thoại cần kiểm tra
     * @throws BusinessException nếu số điện thoại không hợp lệ
     */
    public static void requireValidPhone(String phone) throws BusinessException {
        if (!isValidPhone(phone)) {
            throw new BusinessException(BusinessException.INVALID_INPUT,
                    "Số điện thoại không hợp lệ (phải 10 số, bắt đầu bằng 0): " + phone);
        }
    }

    /**
     * Validate mã sinh viên và ném BusinessException nếu không hợp lệ.
     *
     * @param studentId mã sinh viên cần kiểm tra
     * @throws BusinessException nếu mã sinh viên không hợp lệ
     */
    public static void requireValidStudentId(String studentId) throws BusinessException {
        if (!isValidStudentId(studentId)) {
            throw new BusinessException(BusinessException.INVALID_STUDENT_ID,
                    "Mã sinh viên không hợp lệ (định dạng SVxxxxxxxx): " + studentId);
        }
    }
}

