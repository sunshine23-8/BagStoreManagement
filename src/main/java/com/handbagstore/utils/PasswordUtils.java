package com.handbagstore.utils;

import org.mindrot.jbcrypt.BCrypt;

/**
 * Tiện ích mã hóa / xác thực mật khẩu bằng BCrypt.
 */
public class PasswordUtils {
    private static final int BCRYPT_ROUNDS = 10;

    /** Tạo hash BCrypt từ mật khẩu plain text */
    public static String hash(String plainPassword) {
        return BCrypt.hashpw(plainPassword, BCrypt.gensalt(BCRYPT_ROUNDS));
    }

    /** So sánh mật khẩu plain text với hash */
    public static boolean verify(String plainPassword, String hashedPassword) {
        try {
            return BCrypt.checkpw(plainPassword, hashedPassword);
        } catch (Exception e) {
            return false;
        }
    }
}
