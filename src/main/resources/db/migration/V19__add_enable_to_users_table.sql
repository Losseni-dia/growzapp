-- V3__add_enabled_to_users.sql
ALTER TABLE users 
ADD COLUMN enabled TINYINT(1) NOT NULL DEFAULT 1 
AFTER sexe;


ALTER TABLE users 
MODIFY COLUMN enabled TINYINT(1) NOT NULL DEFAULT 1 
COMMENT 'Indique si le compte est actif (true) ou désactivé (false) par l''admin';