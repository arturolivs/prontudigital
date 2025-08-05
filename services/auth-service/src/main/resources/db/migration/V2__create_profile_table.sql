
CREATE TABLE profiles (
    id UUID PRIMARY KEY,
    name VARCHAR(20) NOT NULL,
    description VARCHAR(200),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE INDEX idx_profiles_name ON profiles(name);