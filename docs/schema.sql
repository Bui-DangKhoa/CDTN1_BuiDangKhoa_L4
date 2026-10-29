-- Schema DDL cho Smart CRM Technician Scheduling (MySQL 8.0)
-- Đảm bảo tính Idempotent (Tiêu chí C5): 100% câu lệnh dùng CREATE TABLE IF NOT EXISTS

SET NAMES utf8mb4;
SET CHARACTER SET utf8mb4;
SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0;
SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0;
SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION';

-- -----------------------------------------------------
-- Table `technician`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `technician` (
  `technician_id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NULL DEFAULT NULL,
  `employee_id` BIGINT NOT NULL,
  `full_name` VARCHAR(120) NOT NULL,
  `center_id` BIGINT NOT NULL,
  `level` VARCHAR(15) NOT NULL,
  `is_active` TINYINT(1) NOT NULL DEFAULT '1',
  PRIMARY KEY (`technician_id`),
  UNIQUE INDEX `uk_technician_user` (`user_id` ASC) VISIBLE,
  UNIQUE INDEX `employee_id` (`employee_id` ASC) VISIBLE,
  CONSTRAINT `fk_technician_user`
    FOREIGN KEY (`user_id`)
    REFERENCES `app_user` (`user_id`)
    ON DELETE SET NULL)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_unicode_ci;


-- -----------------------------------------------------
-- Table `issue_category`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `issue_category` (
  `category_id` INT NOT NULL AUTO_INCREMENT,
  `category_name` VARCHAR(60) NOT NULL,
  `default_priority` VARCHAR(10) NOT NULL,
  `is_active` TINYINT(1) NOT NULL DEFAULT '1',
  PRIMARY KEY (`category_id`),
  UNIQUE INDEX `category_name` (`category_name` ASC) VISIBLE)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_unicode_ci;


-- -----------------------------------------------------
-- Table `device`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `device` (
  `device_id` BIGINT NOT NULL AUTO_INCREMENT,
  `customer_id` BIGINT NOT NULL,
  `device_name` VARCHAR(100) NOT NULL,
  `serial_no` VARCHAR(50) NOT NULL,
  `purchase_date` DATE NULL DEFAULT NULL,
  `warranty_months` SMALLINT NOT NULL DEFAULT '12',
  PRIMARY KEY (`device_id`),
  UNIQUE INDEX `serial_no` (`serial_no` ASC) VISIBLE,
  INDEX `idx_device_customer` (`customer_id` ASC) VISIBLE)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_unicode_ci;


-- -----------------------------------------------------
-- Table `ticket`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `ticket` (
  `ticket_id` BIGINT NOT NULL AUTO_INCREMENT,
  `ticket_code` VARCHAR(20) NOT NULL,
  `customer_id` BIGINT NOT NULL,
  `customer_phone` VARCHAR(20) NULL DEFAULT NULL,
  `device_id` BIGINT NOT NULL,
  `center_id` BIGINT NOT NULL,
  `technician_id` BIGINT NULL DEFAULT NULL,
  `category_id` INT NOT NULL,
  `issue_desc` TEXT NOT NULL,
  `resolution` VARCHAR(500) NULL DEFAULT NULL,
  `priority` VARCHAR(10) NOT NULL,
  `status` VARCHAR(20) NOT NULL DEFAULT 'MOI',
  `received_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `due_date` DATETIME NOT NULL,
  `closed_at` DATETIME NULL DEFAULT NULL,
  PRIMARY KEY (`ticket_id`),
  UNIQUE INDEX `ticket_code` (`ticket_code` ASC) VISIBLE,
  INDEX `fk_ticket_device` (`device_id` ASC) VISIBLE,
  INDEX `fk_ticket_cat` (`category_id` ASC) VISIBLE,
  INDEX `idx_ticket_status_due` (`status` ASC, `due_date` ASC) VISIBLE,
  INDEX `idx_ticket_tech_due` (`technician_id` ASC, `due_date` ASC) VISIBLE,
  CONSTRAINT `fk_ticket_cat`
    FOREIGN KEY (`category_id`)
    REFERENCES `issue_category` (`category_id`),
  CONSTRAINT `fk_ticket_device`
    FOREIGN KEY (`device_id`)
    REFERENCES `device` (`device_id`),
  CONSTRAINT `fk_ticket_tech`
    FOREIGN KEY (`technician_id`)
    REFERENCES `technician` (`technician_id`))
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_unicode_ci;


-- -----------------------------------------------------
-- Table `appointment`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `appointment` (
  `appointment_id` BIGINT NOT NULL AUTO_INCREMENT,
  `ticket_id` BIGINT NOT NULL,
  `technician_id` BIGINT NOT NULL,
  `appointment_time` DATETIME NOT NULL,
  `status` VARCHAR(20) NOT NULL DEFAULT 'CHO_XAC_NHAN',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `active_tech_time` DATETIME GENERATED ALWAYS AS (if((`status` <> _utf8mb4'DA_HUY'),`appointment_time`,NULL)) VIRTUAL,
  PRIMARY KEY (`appointment_id`),
  UNIQUE INDEX `idx_appointment_tech_time` (`technician_id` ASC, `active_tech_time` ASC) VISIBLE,
  INDEX `fk_app_ticket` (`ticket_id` ASC) VISIBLE,
  CONSTRAINT `fk_app_tech`
    FOREIGN KEY (`technician_id`)
    REFERENCES `technician` (`technician_id`),
  CONSTRAINT `fk_app_ticket`
    FOREIGN KEY (`ticket_id`)
    REFERENCES `ticket` (`ticket_id`))
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_unicode_ci;


-- -----------------------------------------------------
-- Table `technician_skill`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `technician_skill` (
  `technician_id` BIGINT NOT NULL,
  `category_id` INT NOT NULL,
  `proficiency` SMALLINT NOT NULL,
  PRIMARY KEY (`technician_id`, `category_id`),
  INDEX `fk_skill_cat` (`category_id` ASC) VISIBLE,
  CONSTRAINT `fk_skill_cat`
    FOREIGN KEY (`category_id`)
    REFERENCES `issue_category` (`category_id`)
    ON DELETE CASCADE,
  CONSTRAINT `fk_skill_tech`
    FOREIGN KEY (`technician_id`)
    REFERENCES `technician` (`technician_id`)
    ON DELETE CASCADE)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_unicode_ci;


-- -----------------------------------------------------
-- Table `ticket_status_log`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `ticket_status_log` (
  `log_id` BIGINT NOT NULL AUTO_INCREMENT,
  `ticket_id` BIGINT NOT NULL,
  `from_status` VARCHAR(20) NULL DEFAULT NULL,
  `to_status` VARCHAR(20) NOT NULL,
  `changed_by` BIGINT NOT NULL,
  `change_reason` VARCHAR(500) NULL DEFAULT NULL,
  `changed_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`log_id`),
  INDEX `idx_log_ticket` (`ticket_id` ASC, `changed_at` ASC) VISIBLE,
  CONSTRAINT `fk_log_ticket`
    FOREIGN KEY (`ticket_id`)
    REFERENCES `ticket` (`ticket_id`))
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_unicode_ci;


-- -----------------------------------------------------
-- Table `app_user`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `app_user` (
  `user_id` BIGINT NOT NULL AUTO_INCREMENT,
  `username` VARCHAR(50) NOT NULL,
  `password` VARCHAR(255) NOT NULL,
  `full_name` VARCHAR(120) NOT NULL,
  `role` VARCHAR(30) NOT NULL,
  `center_id` BIGINT NOT NULL,
  `is_active` TINYINT(1) NOT NULL DEFAULT '1',
  PRIMARY KEY (`user_id`),
  UNIQUE INDEX `username` (`username` ASC) VISIBLE)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_unicode_ci;


SET SQL_MODE=@OLD_SQL_MODE;
SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS;
SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS;
