-- User Table
CREATE TABLE users(
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    registration_number VARCHAR(100) NOT NULL UNIQUE,
    phone_number VARCHAR(20) NOT NULL
);

-- 2. Clients Table
CREATE TABLE clients (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    date_of_birth DATE NOT NULL,
    phone_number VARCHAR(20) NOT NULL
);

-- 3. Records Table
CREATE TABLE records(
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    client_id BIGINT NOT NULL,
    record_date TIMESTAMP NOT NULL,
    diagnosis VARCHAR(255) NOT NULL,
    status VARCHAR(50) NOT NULL,

    -- Foreign keys enforcing the relationships
    CONSTRAINT fk_record_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_record_client FOREIGN KEY (client_id) REFERENCES clients(id)
);

CREATE TABLE record_entries(
    id BIGSERIAL PRIMARY KEY,
    record_id BIGINT NOT NULL,
    entry_name VARCHAR(255) NOT NULL,
    quantity INT NOT NULL,
    frequency VARCHAR(100) NOT NULL,
    duration VARCHAR(100) NOT NULL,

    -- If a record is deleted, its entities are deleted(CASCADE)
    CONSTRAINT fk_entry_record FOREIGN KEY (record_id) REFERENCES records(id) ON DELETE CASCADE
);