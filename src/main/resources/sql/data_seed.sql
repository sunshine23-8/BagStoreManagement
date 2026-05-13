-- =============================================
-- Dữ liệu mẫu (Seed Data)
-- =============================================
USE BagStoreDB;
GO

-- 1. Tạo tài khoản Admin mặc định
-- Password: admin123 (BCrypt hash)
IF NOT EXISTS (SELECT 1 FROM accounts WHERE username = 'admin')
BEGIN
    INSERT INTO accounts (username, password_hash, full_name, role, is_active)
    VALUES ('admin', '$2a$10$0HXLpdyxdHQI0PxWAM3SF.0N5QFJiKRiG1ZxdF9JvccAaMHZLribG', N'Quản trị viên', 'ADMIN', 1);
END
GO

-- 2. Tạo tài khoản Staff mẫu
-- Password: staff123
IF NOT EXISTS (SELECT 1 FROM accounts WHERE username = 'staff01')
BEGIN
    INSERT INTO accounts (username, password_hash, full_name, role, is_active)
    VALUES ('staff01', '$2a$10$y9vXxAWR1CyZKlyMDLJg7.Q5ykswa5ANpUVRbX3IscsCebqD6jy2.', N'Nguyễn Văn A', 'STAFF', 1);
END
GO

-- 3. Thêm nhà cung cấp mẫu
INSERT INTO suppliers (name, phone, address) VALUES
(N'Công ty TNHH Da Sài Gòn', '0901234567', N'123 Nguyễn Huệ, Q.1, TP.HCM'),
(N'Xưởng túi Hà Nội', '0912345678', N'456 Trần Duy Hưng, Hà Nội');
GO

-- 4. Thêm sản phẩm mẫu
INSERT INTO products (product_code, name, brand, price, style, material, color, status) VALUES
('TX001', N'Túi Tote Da Bò Cao Cấp', N'Louis Bag', 1500000, 'Tote', N'Da thật', N'Đen', 'ACTIVE'),
('TX002', N'Túi Crossbody Mini PU', N'Zara Style', 450000, 'Crossbody', 'PU', N'Nâu', 'ACTIVE'),
('TX003', N'Balo Canvas Hàn Quốc', N'Korean Style', 350000, 'Backpack', 'Canvas', N'Xanh Navy', 'ACTIVE'),
('TX004', N'Clutch Dự Tiệc Ánh Kim', N'Dior Inspired', 800000, 'Clutch', N'Da thật', N'Vàng Gold', 'ACTIVE'),
('TX005', N'Túi Tote Vải Canvas Vintage', N'Retro Bag', 280000, 'Tote', 'Canvas', N'Kem', 'ACTIVE'),
('TX006', N'Túi Crossbody Da PU Thời Trang', N'Coach Style', 650000, 'Crossbody', 'PU', N'Đỏ', 'ACTIVE');
GO

-- 5. Thêm tồn kho tương ứng
INSERT INTO inventory (product_id, quantity, reserved_qty, cost_price) VALUES
(1, 20, 0, 900000),
(2, 35, 0, 200000),
(3, 15, 0, 180000),
(4, 8, 0, 500000),
(5, 50, 0, 120000),
(6, 25, 0, 350000);
GO

-- 6. Thêm khách hàng mẫu
INSERT INTO customers (full_name, phone, birthday) VALUES
(N'Trần Thị Bích', '0987654321', '1995-05-12'),
(N'Lê Minh Tuấn', '0976543210', '1990-08-20'),
(N'Phạm Thùy Dung', '0965432109', '1988-12-25');
GO

-- 7. Thêm mã giảm giá mẫu
INSERT INTO discounts (code, type, value, min_order_amt, start_time, end_time, occasion, is_active) VALUES
('WELCOME10', 'PERCENT', 10, 500000, '2026-01-01', '2026-12-31', 'MANUAL', 1),
('FLAT50K', 'AMOUNT', 50000, 300000, '2026-01-01', '2026-12-31', 'MANUAL', 1),
('BIRTHDAY20', 'PERCENT', 20, 0, '2026-01-01', '2026-12-31', 'BIRTHDAY', 1);
GO

PRINT N'Seed data inserted successfully!';
GO


--------------------------
USE BagStoreDB;
UPDATE accounts 
SET password_hash = '$2a$10$0HXLpdyxdHQI0PxWAM3SF.0N5QFJiKRiG1ZxdF9JvccAaMHZLribG' 
WHERE username = 'admin';

USE BagStoreDB;
UPDATE accounts 
SET password_hash = '$2a$10$y9vXxAWR1CyZKlyMDLJg7.Q5ykswa5ANpUVRbX3IscsCebqD6jy2.' 
WHERE username = 'staff01';