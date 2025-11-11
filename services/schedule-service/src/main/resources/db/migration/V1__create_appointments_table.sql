CREATE TABLE appointments (
    id BIGSERIAL PRIMARY KEY,
    uuid UUID DEFAULT gen_random_uuid() UNIQUE NOT NULL,
    start_date_time TIMESTAMP NOT NULL,
    end_date_time TIMESTAMP NOT NULL,
    professional_uuid UUID NOT NULL,
    patient_uuid UUID NOT NULL,
    type VARCHAR(50),
    status VARCHAR(50) DEFAULT 'SCHEDULED',
    notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,

    -- Constraints
    CONSTRAINT chk_appointment_dates CHECK (end_date_time > start_date_time),
    CONSTRAINT chk_appointment_type CHECK (type IN ('CONSULTATION', 'SURGERY', 'EXAM', 'FOLLOW_UP', 'OTHER')),
    CONSTRAINT chk_appointment_status CHECK (status IN ('SCHEDULED', 'CONFIRMED', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED', 'NO_SHOW'))
);

-- Create indexes for better performance
CREATE INDEX idx_appointments_professional_uuid ON appointments(professional_uuid);
CREATE INDEX idx_appointments_uuid ON appointments(uuid);
CREATE INDEX idx_appointments_patient_uuid ON appointments(patient_uuid);
CREATE INDEX idx_appointments_start_date ON appointments(start_date_time);
CREATE INDEX idx_appointments_status ON appointments(status);
CREATE INDEX idx_appointments_professional_date ON appointments(professional_uuid, start_date_time);

