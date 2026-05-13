package com.handbagstore.bll;

import com.handbagstore.dal.AccountDAL;
import com.handbagstore.dal.SystemLogDAL;
import com.handbagstore.dto.AccountDTO;
import com.handbagstore.dto.SystemLogDTO;
import com.handbagstore.utils.PasswordUtils;
import com.handbagstore.utils.Validation;

import java.sql.SQLException;
import java.util.List;

public class AccountBLL {
    private final AccountDAL accountDAL = new AccountDAL();
    private final SystemLogDAL logDAL = new SystemLogDAL();

    // Tài khoản đang đăng nhập (session)
    private static AccountDTO currentUser;

    public static AccountDTO getCurrentUser() {
        return currentUser;
    }

    public static void setCurrentUser(AccountDTO user) {
        currentUser = user;
    }

    /** Đăng nhập — trả về AccountDTO nếu thành công, null nếu thất bại */
    public AccountDTO login(String username, String password) throws SQLException {
        AccountDTO account = accountDAL.getByUsername(username);
        if (account == null)
            return null;
        if (!account.isActive())
            throw new RuntimeException("Tài khoản đã bị khóa!");
        if (!PasswordUtils.verify(password, account.getPasswordHash()))
            return null;

        currentUser = account;
        logDAL.insert(new SystemLogDTO(account.getAccountId(), "ĐĂNG NHẬP", "Đăng nhập thành công"));
        return account;
    }

    /** Đăng xuất */
    public void logout() throws SQLException {
        if (currentUser != null) {
            logDAL.insert(new SystemLogDTO(currentUser.getAccountId(), "ĐĂNG XUẤT", "Đăng xuất"));
            currentUser = null;
        }
    }

    /** Tạo tài khoản staff (chỉ admin) - Tự động sinh username và password */
    public int createStaffAccount(String fullName) throws SQLException {
        if (fullName == null || fullName.trim().isEmpty())
            throw new RuntimeException("Họ tên không được để trống!");

        AccountDTO staff = new AccountDTO();
        staff.setUsername("temp_" + System.currentTimeMillis()); // Tạm thời
        staff.setPasswordHash("temp");
        staff.setFullName(fullName);
        staff.setRole("STAFF");
        staff.setActive(true);
        staff.setMustChangePassword(true);

        int id = accountDAL.insert(staff);
        if (id > 0) {
            String username = "staff" + String.format("%02d", id);
            accountDAL.updateStaffCredentials(id, username, PasswordUtils.hash(username));
            logDAL.insert(new SystemLogDTO(currentUser.getAccountId(), "TẠO TÀI KHOẢN",
                    "Tạo tài khoản staff: " + username));
        }
        return id;
    }

    /** Đổi mật khẩu chủ động */
    public void changePassword(String oldPassword, String newPassword, String confirmPassword) throws SQLException {
        if (!PasswordUtils.verify(oldPassword, currentUser.getPasswordHash()))
            throw new RuntimeException("Mật khẩu cũ không chính xác!");
        if (!Validation.isValidPassword(newPassword))
            throw new RuntimeException("Mật khẩu mới phải có ít nhất 6 ký tự!");
        if (!newPassword.equals(confirmPassword))
            throw new RuntimeException("Xác nhận mật khẩu mới không khớp!");

        accountDAL.changePassword(currentUser.getAccountId(), PasswordUtils.hash(newPassword));
        // Cập nhật session hiện tại
        currentUser.setPasswordHash(PasswordUtils.hash(newPassword));
        currentUser.setMustChangePassword(false);

        logDAL.insert(new SystemLogDTO(currentUser.getAccountId(), "ĐỔI MẬT KHẨU", "Đổi mật khẩu thành công"));
    }

    /** Reset mật khẩu staff */
    public void resetPassword(int accountId, String newPassword) throws SQLException {
        if (!Validation.isValidPassword(newPassword))
            throw new RuntimeException("Mật khẩu phải có ít nhất 6 ký tự!");
        accountDAL.resetPassword(accountId, PasswordUtils.hash(newPassword));
        logDAL.insert(new SystemLogDTO(currentUser.getAccountId(), "RESET MẬT KHẨU",
                "Reset mật khẩu cho account ID: " + accountId));
    }

    /** Khóa / mở khóa tài khoản */
    public void toggleActive(int accountId, boolean active) throws SQLException {
        accountDAL.toggleActive(accountId, active);
        logDAL.insert(new SystemLogDTO(currentUser.getAccountId(),
                active ? "MỞ KHÓA TÀI KHOẢN" : "KHÓA TÀI KHOẢN",
                "Account ID: " + accountId));
    }

    /** Lấy danh sách staff */
    public List<AccountDTO> getAllStaff() throws SQLException {
        return accountDAL.getAllStaff();
    }
}
