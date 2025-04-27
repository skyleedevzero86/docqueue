CREATE TABLE receipt_print_logs (
                                    id VARCHAR(255) PRIMARY KEY,
                                    receipt_id VARCHAR(255) NOT NULL,
                                    file_name VARCHAR(255) NOT NULL,
                                    file_path VARCHAR(255) NOT NULL,
                                    printed_at TIMESTAMP NOT NULL,
                                    user_id VARCHAR(255)
);