package com.handbagstore.bll;

import com.handbagstore.dal.CustomerDAL;
import com.handbagstore.dal.InvoiceDAL;
import com.handbagstore.dto.CustomerDTO;
import com.handbagstore.dto.InvoiceDTO;
import com.handbagstore.utils.DateUtils;
import com.handbagstore.utils.Validation;

import java.sql.SQLException;
import java.util.List;

public class CustomerBLL {
    private final CustomerDAL customerDAL = new CustomerDAL();
    private final InvoiceDAL invoiceDAL = new InvoiceDAL();

    /** Tìm khách hàng theo SĐT + kiểm tra sinh nhật */
    public CustomerDTO getByPhone(String phone) throws SQLException {
        CustomerDTO customer = customerDAL.getByPhone(phone);
        if (customer != null && customer.getBirthday() != null) {
            customer.setBirthday(DateUtils.isBirthdayToday(customer.getBirthday()));
        }
        return customer;
    }

    public CustomerDTO getById(int customerId) throws SQLException {
        return customerDAL.getById(customerId);
    }

    public List<CustomerDTO> getAll() throws SQLException {
        return customerDAL.getAll();
    }

    public List<CustomerDTO> search(String keyword) throws SQLException {
        return customerDAL.search(keyword);
    }

    public int addCustomer(CustomerDTO customer) throws SQLException {
        if (!Validation.isValidPhone(customer.getPhone()))
            throw new RuntimeException("Số điện thoại không hợp lệ (10 số, bắt đầu 0)!");
        if (!Validation.isNotEmpty(customer.getFullName()))
            throw new RuntimeException("Họ tên không được để trống!");
        if (customerDAL.getByPhone(customer.getPhone()) != null)
            throw new RuntimeException("Số điện thoại đã tồn tại!");
        return customerDAL.insert(customer);
    }

    public void updateCustomer(CustomerDTO customer) throws SQLException {
        if (!Validation.isValidPhone(customer.getPhone()))
            throw new RuntimeException("Số điện thoại không hợp lệ!");
        customerDAL.update(customer);
    }

    /** Lấy lịch sử mua hàng */
    public List<InvoiceDTO> getPurchaseHistory(int customerId) throws SQLException {
        return invoiceDAL.getByCustomerId(customerId);
    }
}
