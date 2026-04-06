ALTER TABLE appointments ADD COLUMN evaluation_id BIGINT;
ALTER TABLE appointments ADD COLUMN completed_at TIMESTAMP;
ALTER TABLE appointments ADD CONSTRAINT fk_evaluation FOREIGN KEY (evaluation_id) REFERENCES appointments(id);
