DROP TABLE receipt_print_logs;
CREATE TABLE receipt_print_logs (
id VARCHAR(255) PRIMARY KEY DEFAULT gen_random_uuid(),
receipt_id VARCHAR(255) NOT NULL,
file_name VARCHAR(255) NOT NULL,
file_path VARCHAR(255) NOT NULL,
user_id VARCHAR(255) NOT NULL,
printed_at TIMESTAMP NOT NULL
);