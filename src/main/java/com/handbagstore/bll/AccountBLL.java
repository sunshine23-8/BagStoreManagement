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

    public static AccountDTO getCurrentUser() { return currentUser; }
    public static void setCurrentUser(AccountDTO user) { currentUser = user; }

    /** Đăng nhập — trả về AccountDTO nếu thành công, null nếu thất bại */
    public AccountDTO login(String username, String password) throws SQLException {
        AccountDTO account = accountDAL.getByUsername(username);
        if (account == null) return null;
        if (!account.isActive()) throw new RuntimeException("Tài khoản đã bị khóa!");
        if (!PasswordUtils.verify(password, account.getPasswordHash())) return null;

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

    /** Tạo tài khoản staff (chỉ admin) */
    public int createStaffAccount(String username, String password, String fullName) throws SQLException {
        if (!Validation.isValidUsername(username))
            throw new RuntimeException("Username không hợp lệ (3-50 ký tự, chữ + số)!");
        if (!Validation.isValidPassword(password))
            throw new RuntimeException("Mật khẩu phải có ít nhất 6 ký tự!");
        if (accountDAL.getByUsername(username) != null)
            throw new RuntimeException("Username đã tồn tại!");

        AccountDTO staff = new AccountDTO();
        staff.setUsername(username);
        staff.setPasswordHash(PasswordUtils.hash(password));
        staff.setFullName(fullName);
        staff.setRole("STAFF");
        staff.setActive(true);

        int id = accountDAL.insert(staff);
        logDAL.insert(new SystemLogDTO(currentUser.getAccountId(), "TẠO TÀI KHOẢN",
                "Tạo tài khoản staff: " + username));
        return id;
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
