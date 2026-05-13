package com.handbagstore.bll;

import com.handbagstore.dal.StatisticDAL;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

public class StatisticBLL {
    private final StatisticDAL statisticDAL = new StatisticDAL();

    public BigDecimal getDailyRevenue(java.sql.Date date) throws SQLException {
        return statisticDAL.getDailyRevenue(date);
    }

    public BigDecimal getMonthlyRevenue(int year, int month) throws SQLException {
        return statisticDAL.getMonthlyRevenue(year, month);
    }

    public Map<Integer, BigDecimal> getDailyRevenueInMonth(int year, int month) throws SQLException {
        return statisticDAL.getDailyRevenueInMonth(year, month);
    }

    public List<Object[]> getTopProducts(java.sql.Date from, java.sql.Date to, int limit) throws SQLException {
        return statisticDAL.getTopProducts(from, to, limit);
    }

    public Map<String, BigDecimal> getRevenueByDateRange(java.sql.Date from, java.sql.Date to) throws SQLException {
        return statisticDAL.getRevenueByDateRange(from, to);
    }

    public Map<Integer, BigDecimal> getRevenueByMonthsInYear(int year) throws SQLException {
        return statisticDAL.getRevenueByMonthsInYear(year);
    }

    public Map<Integer, BigDecimal> getRevenueByRecentYears(int limit) throws SQLException {
        return statisticDAL.getRevenueByRecentYears(limit);
    }

    public List<Object[]> getInventoryStock() throws SQLException {
        return statisticDAL.getInventoryStock();
    }
}
