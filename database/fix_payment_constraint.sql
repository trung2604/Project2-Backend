-- Fix payment_method constraint
ALTER TABLE payments DROP CONSTRAINT IF EXISTS payments_payment_method_check;

ALTER TABLE payments ADD CONSTRAINT payments_payment_method_check 
CHECK (payment_method IN ('COD', 'BANKING', 'CREDIT_CARD', 'DEBIT_CARD', 'WALLET'));

-- Kiểm tra cấu trúc bảng sau khi cập nhật
-- \d payments 