-- Transactions table
CREATE TABLE IF NOT EXISTS transactions (
    id VARCHAR(255) PRIMARY KEY,
    value DECIMAL(19,2) NOT NULL,
    description VARCHAR(500),
    payment_method VARCHAR(20) NOT NULL,
    card_number VARCHAR(4) NOT NULL,
    merchant_name VARCHAR(255) NOT NULL,
    customer_name VARCHAR(255) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for transactions
CREATE INDEX IF NOT EXISTS idx_transactions_created_at ON transactions(created_at);
CREATE INDEX IF NOT EXISTS idx_transactions_status ON transactions(status);


-- Receivables table
CREATE TABLE IF NOT EXISTS receivables (
    id VARCHAR(255) PRIMARY KEY,
    transaction_id VARCHAR(255) NOT NULL,
    status VARCHAR(20) NOT NULL,
    create_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    payment_date TIMESTAMP NOT NULL,
    subtotal DECIMAL(19,2) NOT NULL,
    discount DECIMAL(19,2) NOT NULL,
    total DECIMAL(19,2) NOT NULL,
    CONSTRAINT fk_receivables_transaction
        FOREIGN KEY (transaction_id) REFERENCES transactions(id)
        ON DELETE CASCADE
);

-- Indexes for receivables
CREATE INDEX IF NOT EXISTS idx_receivables_status ON receivables(status);
CREATE INDEX IF NOT EXISTS idx_receivables_payment_date ON receivables(payment_date);
CREATE INDEX IF NOT EXISTS idx_receivables_transaction_id ON receivables(transaction_id);