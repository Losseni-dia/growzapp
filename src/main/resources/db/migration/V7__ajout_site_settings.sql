CREATE TABLE site_settings (
    id BIGINT PRIMARY KEY,
    default_language VARCHAR(5) NOT NULL DEFAULT 'fr'
);

INSERT INTO site_settings (id, default_language) VALUES (1, 'fr');
