CREATE TABLE receipt_print_logs (
                                    id BIGSERIAL PRIMARY KEY,
                                    receipt_id VARCHAR(255) NOT NULL,
                                    file_name VARCHAR(255) NOT NULL,
                                    file_path VARCHAR(255) NOT NULL,
                                    user_id VARCHAR(255) NOT NULL,
                                    printed_at TIMESTAMP NOT NULL
);

ALTER TABLE receipt_print_logs
ALTER COLUMN id TYPE VARCHAR(255) USING (id::VARCHAR);

CREATE EXTENSION IF NOT EXISTS pgcrypto;

ALTER TABLE receipt_print_logs
ALTER COLUMN id SET DEFAULT gen_random_uuid();