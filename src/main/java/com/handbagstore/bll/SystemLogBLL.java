package com.handbagstore.bll;

import com.handbagstore.dal.SystemLogDAL;
import com.handbagstore.dto.SystemLogDTO;
import java.sql.SQLException;
import java.util.List;

public class SystemLogBLL {
    private final SystemLogDAL logDAL = new SystemLogDAL();

    public void log(int accountId, String action, String detail) {
        try {
            logDAL.insert(new SystemLogDTO(accountId, action, detail));
        } catch (SQLException e) {
            System.err.println("Lỗi ghi log: " + e.getMessage());
        }
    }

    public List<SystemLogDTO> getAll() throws SQLException {
        return logDAL.getAll();
    }

    public List<SystemLogDTO> getByAccount(int accountId) throws SQLException {
        return logDAL.getByAccount(accountId);
    }

    public List<SystemLogDTO> search(String keyword, java.sql.Date from, java.sql.Date to) throws SQLException {
        return logDAL.search(keyword, from, to);
    }
}
