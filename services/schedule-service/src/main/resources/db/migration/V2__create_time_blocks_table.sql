CREATE TABLE time_blocks (
    id BIGSERIAL PRIMARY KEY,
    uuid UUID DEFAULT gen_random_uuid() UNIQUE NOT NULL,
    professional_uuid UUID NOT NULL,
    start_date_time TIMESTAMP NOT NULL,
    end_date_time TIMESTAMP NOT NULL,
    reason VARCHAR(500),
    type VARCHAR(50) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_time_block_duration CHECK (end_date_time > start_date_time),
    CONSTRAINT chk_time_block_type CHECK (type IN ('UNAVAILABLE', 'VACATION', 'TRAINING', 'EMERGENCY'))
);

CREATE INDEX idx_time_blocks_professional_uuid ON time_blocks(professional_uuid);
CREATE INDEX idx_time_blocks_start_end_time ON time_blocks(start_date_time, end_date_time);
CREATE INDEX idx_time_blocks_professional_time ON time_blocks(professional_uuid, start_date_time, end_date_time);
