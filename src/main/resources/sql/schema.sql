-- =============================================
-- BagStoreDB - SQL Server Schema
-- Hệ thống Quản lý Kinh doanh Túi Xách
-- =============================================

-- Tạo Database
IF NOT EXISTS (SELECT name FROM sys.databases WHERE name = N'BagStoreDB')
BEGIN
    CREATE DATABASE BagStoreDB;
END
GO

USE BagStoreDB;
GO

-- =============================================
-- 1. Bảng Tài khoản (accounts)
-- =============================================
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'accounts')
BEGIN
    CREATE TABLE accounts (
        account_id    INT IDENTITY(1,1) PRIMARY KEY,
        username      NVARCHAR(50) NOT NULL UNIQUE,
        password_hash NVARCHAR(255) NOT NULL,
        full_name     NVARCHAR(100),
        role          NVARCHAR(10) NOT NULL CHECK (role IN ('ADMIN', 'STAFF')),
        is_active     BIT DEFAULT 1,
        created_at    DATETIME DEFAULT GETDATE()
    );
END
GO

-- =============================================
-- 2. Bảng Sản phẩm (products)
-- =============================================
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'products')
BEGIN
    CREATE TABLE products (
        product_id   INT IDENTITY(1,1) PRIMARY KEY,
        product_code NVARCHAR(20) NOT NULL UNIQUE,
        name         NVARCHAR(150) NOT NULL,
        brand        NVARCHAR(100),
        price        DECIMAL(15,0) NOT NULL,
        style        NVARCHAR(20) CHECK (style IN (N'Tote', N'Crossbody', N'Backpack', N'Clutch')),
        material     NVARCHAR(20) CHECK (material IN (N'Da thật', N'PU', N'Canvas')),
        color        NVARCHAR(50),
        image_path   NVARCHAR(255),
        status       NVARCHAR(10) DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'INACTIVE')),
        created_at   DATETIME DEFAULT GETDATE()
    );
END
GO

-- =============================================
-- 3. Bảng Nhà cung cấp (suppliers)
-- =============================================
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'suppliers')
BEGIN
    CREATE TABLE suppliers (
        supplier_id  INT IDENTITY(1,1) PRIMARY KEY,
        name         NVARCHAR(150) NOT NULL,
        phone        NVARCHAR(20),
        address      NVARCHAR(500)
    );
END
GO

-- =============================================
-- 4. Bảng Kho hàng (inventory)
-- =============================================
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'inventory')
BEGIN
    CREATE TABLE inventory (
        inventory_id  INT IDENTITY(1,1) PRIMARY KEY,
        product_id    INT NOT NULL,
        quantity      INT DEFAULT 0,
        reserved_qty  INT DEFAULT 0,
        cost_price    DECIMAL(15,0),
        updated_at    DATETIME DEFAULT GETDATE(),
        CONSTRAINT FK_inventory_product FOREIGN KEY (product_id) REFERENCES products(product_id)
    );
END
GO

-- =============================================
-- 5. Bảng Lô nhập kho (import_batches)
-- =============================================
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'import_batches')
BEGIN
    CREATE TABLE import_batches (
        batch_id     INT IDENTITY(1,1) PRIMARY KEY,
        product_id   INT NOT NULL,
        supplier_id  INT,
        quantity     INT NOT NULL,
        cost_price   DECIMAL(15,0),
        import_date  DATETIME NOT NULL,
        note         NVARCHAR(MAX),
        created_by   INT,
        CONSTRAINT FK_batch_product FOREIGN KEY (product_id) REFERENCES products(product_id),
        CONSTRAINT FK_batch_supplier FOREIGN KEY (supplier_id) REFERENCES suppliers(supplier_id),
        CONSTRAINT FK_batch_account FOREIGN KEY (created_by) REFERENCES accounts(account_id)
    );
END
GO

-- =============================================
-- 6. Bảng Khách hàng (customers)
-- =============================================
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'customers')
BEGIN
    CREATE TABLE customers (
        customer_id  INT IDENTITY(1,1) PRIMARY KEY,
        full_name    NVARCHAR(100),
        phone        NVARCHAR(20) NOT NULL UNIQUE,
        birthday     DATE,
        created_at   DATETIME DEFAULT GETDATE()
    );
END
GO

-- =============================================
-- 7. Bảng Mã giảm giá (discounts)
-- =============================================
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'discounts')
BEGIN
    CREATE TABLE discounts (
        discount_id   INT IDENTITY(1,1) PRIMARY KEY,
        code          NVARCHAR(50) NOT NULL UNIQUE,
        type          NVARCHAR(10) NOT NULL CHECK (type IN ('PERCENT', 'AMOUNT')),
        value         DECIMAL(15,0) NOT NULL,
        min_order_amt DECIMAL(15,0) DEFAULT 0,
        start_time    DATETIME,
        end_time      DATETIME,
        occasion      NVARCHAR(20) DEFAULT 'MANUAL' CHECK (occasion IN ('BIRTHDAY', 'SPECIAL', 'MANUAL')),
        is_active     BIT DEFAULT 1,
        created_by    INT,
        CONSTRAINT FK_discount_account FOREIGN KEY (created_by) REFERENCES accounts(account_id)
    );
END
GO

-- =============================================
-- 8. Bảng Hóa đơn (invoices)
-- =============================================
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'invoices')
BEGIN
    CREATE TABLE invoices (
        invoice_id      INT IDENTITY(1,1) PRIMARY KEY,
        invoice_code    NVARCHAR(20) NOT NULL UNIQUE,
        customer_id     INT,                                    -- NULL = khách vãng lai (guest)
        staff_id        INT NOT NULL,
        discount_id     INT,
        subtotal        DECIMAL(15,0) NOT NULL,
        discount_amount DECIMAL(15,0) DEFAULT 0,
        total           DECIMAL(15,0) NOT NULL,
        payment_method  NVARCHAR(10) DEFAULT 'CASH' CHECK (payment_method IN ('CASH', 'TRANSFER')),
        payment_received DECIMAL(15,0) DEFAULT 0,              -- Số tiền khách đưa (tiền mặt)
        change_amount   DECIMAL(15,0) DEFAULT 0,               -- Tiền thối lại
        status          NVARCHAR(10) DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'PAID', 'CANCELLED')),
        created_at      DATETIME DEFAULT GETDATE(),
        paid_at         DATETIME,
        expires_at      DATETIME,                               -- Thời điểm pending hết hạn
        CONSTRAINT FK_invoice_customer FOREIGN KEY (customer_id) REFERENCES customers(customer_id),
        CONSTRAINT FK_invoice_staff FOREIGN KEY (staff_id) REFERENCES accounts(account_id),
        CONSTRAINT FK_invoice_discount FOREIGN KEY (discount_id) REFERENCES discounts(discount_id)
    );
END
GO

-- =============================================
-- 9. Bảng Chi tiết hóa đơn (invoice_details)
-- =============================================
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'invoice_details')
BEGIN
    CREATE TABLE invoice_details (
        detail_id    INT IDENTITY(1,1) PRIMARY KEY,
        invoice_id   INT NOT NULL,
        product_id   INT NOT NULL,
        quantity     INT NOT NULL,
        unit_price   DECIMAL(15,0) NOT NULL,
        CONSTRAINT FK_detail_invoice FOREIGN KEY (invoice_id) REFERENCES invoices(invoice_id),
        CONSTRAINT FK_detail_product FOREIGN KEY (product_id) REFERENCES products(product_id)
    );
END
GO

-- =============================================
-- 10. Bảng Nhật ký hệ thống (system_logs)
-- =============================================
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'system_logs')
BEGIN
    CREATE TABLE system_logs (
        log_id       INT IDENTITY(1,1) PRIMARY KEY,
        account_id   INT,
        action       NVARCHAR(255),
        detail       NVARCHAR(MAX),
        created_at   DATETIME DEFAULT GETDATE(),
        CONSTRAINT FK_log_account FOREIGN KEY (account_id) REFERENCES accounts(account_id)
    );
END
GO

-- =============================================
-- 11. Bảng Cấu hình hệ thống (system_config)
-- Dùng để admin cấu hình pending timeout,...
-- =============================================
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'system_config')
BEGIN
    CREATE TABLE system_config (
        config_key   NVARCHAR(100) PRIMARY KEY,
        config_value NVARCHAR(255) NOT NULL,
        description  NVARCHAR(500)
    );
END
GO

-- Insert default config
IF NOT EXISTS (SELECT 1 FROM system_config WHERE config_key = 'pending.timeout.minutes')
BEGIN
    INSERT INTO system_config (config_key, config_value, description)
    VALUES ('pending.timeout.minutes', '5', N'Thời gian giữ đơn hàng pending (phút)');
END
GO

PRINT N'Schema created successfully!';
GO
