CREATE TABLE waiting_list (
    id BIGSERIAL PRIMARY KEY,
    uuid UUID DEFAULT gen_random_uuid() UNIQUE NOT NULL,
    patient_uuid UUID NOT NULL,
    professional_uuid UUID NOT NULL,
    preferred_type VARCHAR(50),
    preferred_date TIMESTAMP,
    priority INTEGER DEFAULT 0,
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_waiting_list_priority CHECK (priority >= 0),
    CONSTRAINT chk_waiting_list_status CHECK (status IN ('ACTIVE', 'NOTIFIED', 'CANCELLED', 'FULFILLED')),
    CONSTRAINT chk_waiting_list_type CHECK (preferred_type IN ('CONSULTATION', 'PROCEDURE', 'URGENT', 'FOLLOW_UP', 'OTHER'))
);

CREATE INDEX idx_waiting_list_patient_uuid ON waiting_list(patient_uuid);
CREATE INDEX idx_waiting_list_professional_uuid ON waiting_list(professional_uuid);
CREATE INDEX idx_waiting_list_status ON waiting_list(status);
CREATE INDEX idx_waiting_list_priority ON waiting_list(priority DESC);
CREATE INDEX idx_waiting_list_professional_status ON waiting_list(professional_uuid, status);
CREATE INDEX idx_waiting_list_created_at ON waiting_list(created_at);
CREATE UNIQUE INDEX uk_patient_professional_active ON waiting_list(patient_uuid, professional_uuid, status);

