DELIMITER $$

CREATE PROCEDURE fix_zero_dates()
BEGIN
    DECLARE done INT DEFAULT 0;
    DECLARE tbl  VARCHAR(64);
    DECLARE col  VARCHAR(64);
    DECLARE typ  VARCHAR(64);

    -- Cursor: every DATETIME or TIMESTAMP column in this database
    DECLARE cur CURSOR FOR
        SELECT TABLE_NAME, COLUMN_NAME, COLUMN_TYPE
        FROM INFORMATION_SCHEMA.COLUMNS
        WHERE TABLE_SCHEMA = 'pbmgids'
          AND DATA_TYPE IN ('datetime','timestamp');

    DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = 1;

    OPEN cur;
    read_loop: LOOP
        FETCH cur INTO tbl, col, typ;
        IF done THEN
            LEAVE read_loop;
        END IF;

        -- 1️⃣  Update existing rows: 0000-00-00 00:00:00 → NULL
        SET @u = CONCAT(
                'UPDATE `', tbl, '` ',
                'SET `', col, '` = NULL ',
                'WHERE `', col, "` = '0000-00-00 00:00:00'"
                 );
        PREPARE stmt FROM @u;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;

        -- 2️⃣  Make the default truly NULL
        SET @a = CONCAT(
                'ALTER TABLE `', tbl, '` ',
                'MODIFY COLUMN `', col, '` ', typ, ' NULL DEFAULT NULL'
                 );
        PREPARE stmt FROM @a;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;

    END LOOP;
    CLOSE cur;
END$$

DELIMITER ;

CALL fix_zero_dates();
DROP PROCEDURE fix_zero_dates;

UPDATE `pbm`
SET `created_on` = NULL
WHERE `created_on` = '0000-00-00 00:00:00';