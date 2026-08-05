ALTER TABLE documents ADD COLUMN description TEXT;

ALTER TABLE documents
ADD COLUMN statut VARCHAR(20) NOT NULL DEFAULT 'APPROUVE';

ALTER TABLE documents ADD COLUMN date_validation TIMESTAMP;

ALTER TABLE documents
ADD COLUMN uploade_par_id BIGINT REFERENCES users (id);