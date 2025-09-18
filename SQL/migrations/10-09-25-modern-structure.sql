SET time_zone = '+01:00';
SET FOREIGN_KEY_CHECKS = 0;
START TRANSACTION;

DROP TABLE IF EXISTS rawpbm, rawpbm_x_class, rawpbmclass, pbm_x_pbm, logger;

ALTER TABLE category
    CHANGE category_id id INT(11) NOT NULL AUTO_INCREMENT,
    CHANGE parent_id parent_id INT(11) NOT NULL DEFAULT 0,
    CHANGE sap_naam sap_name VARCHAR(200),
    CHANGE naam name VARCHAR(70),
    CHANGE afbeelding image VARCHAR(120),
    CHANGE published published TINYINT(1) NOT NULL DEFAULT 0,
    CHANGE created_by created_by INT(11) NOT NULL,
    CHANGE created_on created_on DATETIME NOT NULL,
    CHANGE changed_by modified_by INT(11),
    CHANGE changed_on modified_on DATETIME,
    CHANGE lw last_modified TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP;

-- Add indexes
ALTER TABLE category
    ADD INDEX idx_parent (parent_id),
    ADD INDEX idx_published (published);

-- --------------------------

ALTER TABLE pbm
    CHANGE pbm_id id INT(11) NOT NULL AUTO_INCREMENT,
    CHANGE identificatie name VARCHAR(70),
    CHANGE merk brand VARCHAR(70),
    CHANGE pbm_type type VARCHAR(70),
    CHANGE omschrijving description TEXT,
    CHANGE beschermt_tegen protects_against TEXT,
    CHANGE beschermt_n_tegen does_not_protect_against TEXT,
    CHANGE aandachtspunten notes TEXT,
    CHANGE gebruiksaanwijzing usage_instructions TEXT,
    CHANGE verstrekking distribution TEXT,
    CHANGE normering standards TEXT,
    CHANGE afbeelding image VARCHAR(120),
    CHANGE published published TINYINT(1) NOT NULL DEFAULT 0,
    CHANGE hits hits INT(11) NOT NULL DEFAULT 0,
    CHANGE created_by created_by INT(11) NOT NULL,
    CHANGE created_on created_on DATETIME NOT NULL,
    CHANGE changed_by modified_by INT(11),
    CHANGE changed_on modified_on DATETIME,
    CHANGE lw last_modified TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP;

-- Add indexes
ALTER TABLE pbm
    ADD INDEX idx_published (published),
    ADD INDEX idx_name (name),
    ADD INDEX idx_brand (brand);

-- --------------------------
-- Normering table -> norm
-- --------------------------
ALTER TABLE normering
    CHANGE normering_id id INT(11) NOT NULL AUTO_INCREMENT,
    CHANGE naam name VARCHAR(70),
    CHANGE url file_path VARCHAR(255),
    CHANGE mimetype mime_type VARCHAR(50),
    CHANGE filesize file_size INT(11),
    CHANGE beschrijving description TEXT,
    CHANGE published published TINYINT(1) NOT NULL DEFAULT 0,
    CHANGE created_by created_by INT(11) NOT NULL,
    CHANGE created_on created_on DATETIME NOT NULL,
    CHANGE changed_by modified_by INT(11),
    CHANGE changed_on modified_on DATETIME,
    CHANGE lw last_modified TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP;

RENAME TABLE normering TO norm;

-- Add index
ALTER TABLE norm
    ADD INDEX idx_published (published);

-- --------------------------
-- Warehouse items (matnr)
-- --------------------------
ALTER TABLE matnr
    CHANGE matnr_id id INT(11) NOT NULL AUTO_INCREMENT,
    CHANGE pbm_id pbm_id INT(11) NOT NULL,
    CHANGE matnr warehouse_number VARCHAR(18),
    CHANGE varianttext variant_text VARCHAR(150),
    CHANGE created_on created_on DATETIME NOT NULL,
    CHANGE created_by created_by INT(11) NOT NULL,
    CHANGE changed_on modified_on DATETIME,
    CHANGE changed_by modified_by INT(11),
    CHANGE published published TINYINT(1) NOT NULL DEFAULT 0,
    CHANGE lw last_modified TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP;

RENAME TABLE matnr TO warehouse_item;

-- Add indexes and convert to InnoDB if needed
ALTER TABLE warehouse_item
    ENGINE=InnoDB,
    ADD INDEX idx_pbm (pbm_id),
    ADD INDEX idx_warehouse_number (warehouse_number),
    ADD INDEX idx_published (published);

-- --------------------------
-- Document table transformation
-- --------------------------
-- First, rename the existing pbmdocument table
ALTER TABLE pbmdocument
    CHANGE pbmdocument_id id INT(11) NOT NULL AUTO_INCREMENT,
    CHANGE pbmdocument_type document_type VARCHAR(50),
    CHANGE url file_path VARCHAR(255),
    ADD COLUMN mime_type VARCHAR(50) AFTER file_path,
    ADD COLUMN file_size INT(11) AFTER mime_type,
    ADD COLUMN description TEXT AFTER file_size,
    ADD COLUMN created_by INT(11) NOT NULL DEFAULT 1 AFTER description,
    ADD COLUMN created_on DATETIME NOT NULL DEFAULT NOW() AFTER created_by,
    ADD COLUMN modified_by INT(11) AFTER created_on,
    ADD COLUMN modified_on DATETIME AFTER modified_by;

RENAME TABLE pbmdocument TO document;

-- Convert to InnoDB
ALTER TABLE document ENGINE=InnoDB;

-- Add index
ALTER TABLE document
    ADD INDEX idx_pbm (pbm_id),
    ADD INDEX idx_document_type (document_type);

-- Create the linking table for many-to-many relationship
CREATE TABLE IF NOT EXISTS pbm_document (
                                            pbm_id INT(11) NOT NULL,
                                            document_id INT(11) NOT NULL,
                                            PRIMARY KEY (pbm_id, document_id),
                                            INDEX idx_pbm (pbm_id),
                                            INDEX idx_document (document_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Populate the linking table from existing data
INSERT INTO pbm_document (pbm_id, document_id)
SELECT pbm_id, id
FROM document
WHERE pbm_id IS NOT NULL AND pbm_id > 0;

-- Now remove the pbm_id column from document table
ALTER TABLE document DROP COLUMN pbm_id;

-- --------------------------
-- PBM x Category linking table
-- --------------------------
-- Convert to InnoDB first
ALTER TABLE pbm_x_category ENGINE=InnoDB;

ALTER TABLE pbm_x_category
    CHANGE pbm_id pbm_id INT(11) NOT NULL,
    CHANGE cat_id category_id INT(11) NOT NULL,
    ADD PRIMARY KEY IF NOT EXISTS (pbm_id, category_id),
    ADD INDEX idx_pbm (pbm_id),
    ADD INDEX idx_category (category_id);

RENAME TABLE pbm_x_category TO pbm_category;

-- --------------------------
-- PBM x Normering linking table
-- --------------------------
ALTER TABLE pbm_x_normering
    CHANGE pbm_id pbm_id INT(11) NOT NULL,
    CHANGE normering_id norm_id INT(11) NOT NULL,
    ADD INDEX idx_pbm (pbm_id),
    ADD INDEX idx_norm (norm_id);

RENAME TABLE pbm_x_normering TO pbm_norm;

-- --------------------------
-- User table
-- --------------------------
ALTER TABLE user
    CHANGE user_id id INT(11) NOT NULL AUTO_INCREMENT,
    CHANGE account_id account_id INT(11) NOT NULL DEFAULT 0,
    CHANGE login username VARCHAR(50) UNIQUE,
    CHANGE pwd password_hash VARCHAR(255), -- Increased size for modern hashing
    CHANGE userlevel user_level TINYINT(1) NOT NULL DEFAULT 0,
    CHANGE naam full_name VARCHAR(100),
    CHANGE blocked is_blocked BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN email VARCHAR(255) AFTER full_name,
    ADD COLUMN last_login DATETIME DEFAULT NULL AFTER is_blocked,
    ADD COLUMN login_attempts INT(11) NOT NULL DEFAULT 0 AFTER last_login,
    CHANGE created_by created_by INT(11) NOT NULL,
    CHANGE created_on created_on DATETIME NOT NULL,
    CHANGE changed_by modified_by INT(11),
    CHANGE changed_on modified_on DATETIME,
    CHANGE lw last_modified TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP;

RENAME TABLE user TO user_account;

-- Add indexes
ALTER TABLE user_account
    ADD INDEX idx_username (username),
    ADD INDEX idx_email (email),
    ADD INDEX idx_blocked (is_blocked);

-- --------------------------
ALTER TABLE category ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
ALTER TABLE pbm ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
ALTER TABLE norm ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
ALTER TABLE warehouse_item ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
ALTER TABLE document ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
ALTER TABLE pbm_category ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
ALTER TABLE pbm_norm ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
ALTER TABLE user_account ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


ALTER TABLE warehouse_item
    ADD CONSTRAINT fk_warehouse_pbm
        FOREIGN KEY (pbm_id) REFERENCES pbm(id) ON DELETE CASCADE;

ALTER TABLE pbm_category
    ADD CONSTRAINT fk_pbmc_pbm
        FOREIGN KEY (pbm_id) REFERENCES pbm(id) ON DELETE CASCADE,
    ADD CONSTRAINT fk_pbmc_category
        FOREIGN KEY (category_id) REFERENCES category(id) ON DELETE CASCADE;

ALTER TABLE pbm_norm
    ADD CONSTRAINT fk_pbmn_pbm
        FOREIGN KEY (pbm_id) REFERENCES pbm(id) ON DELETE CASCADE,
    ADD CONSTRAINT fk_pbmn_norm
        FOREIGN KEY (norm_id) REFERENCES norm(id) ON DELETE CASCADE;

ALTER TABLE pbm_document
    ADD CONSTRAINT fk_pbmd_pbm
        FOREIGN KEY (pbm_id) REFERENCES pbm(id) ON DELETE CASCADE,
    ADD CONSTRAINT fk_pbmd_document
        FOREIGN KEY (document_id) REFERENCES document(id) ON DELETE CASCADE;

ALTER TABLE category
    ADD CONSTRAINT fk_category_parent
        FOREIGN KEY (parent_id) REFERENCES category(id) ON DELETE CASCADE;

-- Add user relations (created_by, modified_by) foreign keys
ALTER TABLE category
    ADD CONSTRAINT fk_category_created_by FOREIGN KEY (created_by) REFERENCES user_account(id),
    ADD CONSTRAINT fk_category_modified_by FOREIGN KEY (modified_by) REFERENCES user_account(id);

ALTER TABLE pbm
    ADD CONSTRAINT fk_pbm_created_by FOREIGN KEY (created_by) REFERENCES user_account(id),
    ADD CONSTRAINT fk_pbm_modified_by FOREIGN KEY (modified_by) REFERENCES user_account(id);

ALTER TABLE norm
    ADD CONSTRAINT fk_norm_created_by FOREIGN KEY (created_by) REFERENCES user_account(id),
    ADD CONSTRAINT fk_norm_modified_by FOREIGN KEY (modified_by) REFERENCES user_account(id);

ALTER TABLE warehouse_item
    ADD CONSTRAINT fk_warehouse_created_by FOREIGN KEY (created_by) REFERENCES user_account(id),
    ADD CONSTRAINT fk_warehouse_modified_by FOREIGN KEY (modified_by) REFERENCES user_account(id);

ALTER TABLE document
    ADD CONSTRAINT fk_document_created_by FOREIGN KEY (created_by) REFERENCES user_account(id),
    ADD CONSTRAINT fk_document_modified_by FOREIGN KEY (modified_by) REFERENCES user_account(id);

ALTER TABLE user_account
    ADD CONSTRAINT fk_user_created_by FOREIGN KEY (created_by) REFERENCES user_account(id),
    ADD CONSTRAINT fk_user_modified_by FOREIGN KEY (modified_by) REFERENCES user_account(id);

ALTER TABLE pbm
    ADD CONSTRAINT chk_hits CHECK (hits >= 0);

ALTER TABLE user_account
    ADD CONSTRAINT chk_user_level CHECK (user_level BETWEEN 0 AND 10),
    ADD CONSTRAINT chk_login_attempts CHECK (login_attempts >= 0);

ALTER TABLE norm
    ADD CONSTRAINT chk_file_size CHECK (file_size >= 0);

SET FOREIGN_KEY_CHECKS = 1;
COMMIT;

-- --------------------------
-- Post-migration verification queries
-- --------------------------
-- Run these after migration to verify data integrity

-- Check for orphaned records
SELECT 'Orphaned warehouse items' as check_type, COUNT(*) as count
FROM warehouse_item w
         LEFT JOIN pbm p ON w.pbm_id = p.id
WHERE p.id IS NULL;

SELECT 'Orphaned pbm_category links' as check_type, COUNT(*) as count
FROM pbm_category pc
         LEFT JOIN pbm p ON pc.pbm_id = p.id
WHERE p.id IS NULL;

SELECT 'Orphaned category links' as check_type, COUNT(*) as count
FROM pbm_category pc
         LEFT JOIN category c ON pc.category_id = c.id
WHERE c.id IS NULL;

-- Display table summary
SELECT
    TABLE_NAME,
    ENGINE,
    TABLE_COLLATION,
    TABLE_ROWS
FROM information_schema.TABLES
WHERE TABLE_SCHEMA = DATABASE()
ORDER BY TABLE_NAME;

alter table document
    change lw timestamp timestamp default '0000-00-00 00:00:00' not null on update CURRENT_TIMESTAMP;
