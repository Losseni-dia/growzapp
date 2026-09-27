ALTER TABLE contact_messages ALTER COLUMN user_id DROP NOT NULL;
ALTER TABLE contact_messages ADD COLUMN email_visiteur VARCHAR(191);
