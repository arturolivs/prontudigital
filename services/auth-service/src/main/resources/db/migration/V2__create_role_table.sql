
CREATE TABLE roles (
    id UUID PRIMARY KEY,
    name VARCHAR(20) NOT NULL,
    description VARCHAR(200),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE INDEX idx_roles_name ON roles(name);