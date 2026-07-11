-- Onmom MySQL 8 schema
-- Spring Boot / JPA friendly DDL using snake_case, BIGINT ids, and utf8mb4.
-- MVP-friendly version: keep PK/UNIQUE/INDEX, validate relations and code values in application code.

CREATE TABLE users (
  id BIGINT NOT NULL AUTO_INCREMENT,
  nickname VARCHAR(80) NULL,
  profile_image_url VARCHAR(500) NULL,
  primary_role VARCHAR(20) NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  deleted_at DATETIME(3) NULL,
  PRIMARY KEY (id),
  KEY idx_users_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE oauth_accounts (
  id BIGINT NOT NULL AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  provider VARCHAR(20) NOT NULL DEFAULT 'KAKAO',
  provider_user_id VARCHAR(120) NOT NULL,
  email VARCHAR(255) NULL,
  connected_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  UNIQUE KEY uk_oauth_provider_user (provider, provider_user_id),
  KEY idx_oauth_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE pregnancies (
  id BIGINT NOT NULL AUTO_INCREMENT,
  mother_user_id BIGINT NOT NULL,
  mother_display_name VARCHAR(80) NOT NULL,
  baby_nickname VARCHAR(80) NULL,
  pregnancy_week_start TINYINT UNSIGNED NULL,
  pregnancy_week_end TINYINT UNSIGNED NULL,
  due_date DATE NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  KEY idx_preg_mother_status (mother_user_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE family_connections (
  id BIGINT NOT NULL AUTO_INCREMENT,
  pregnancy_id BIGINT NOT NULL,
  mother_user_id BIGINT NOT NULL,
  family_user_id BIGINT NOT NULL,
  relationship VARCHAR(20) NOT NULL DEFAULT 'SPOUSE',
  status VARCHAR(20) NOT NULL DEFAULT 'INVITED',
  connected_at DATETIME(3) NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  UNIQUE KEY uk_family_connection (pregnancy_id, family_user_id),
  KEY idx_family_user_status (family_user_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE family_invite_codes (
  id BIGINT NOT NULL AUTO_INCREMENT,
  pregnancy_id BIGINT NOT NULL,
  inviter_user_id BIGINT NOT NULL,
  code VARCHAR(6) NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
  expires_at DATETIME(3) NOT NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  UNIQUE KEY uk_family_invite_code (code),
  KEY idx_family_invite_preg_status (pregnancy_id, status),
  KEY idx_family_invite_expires (expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE hospitals (
  id BIGINT NOT NULL AUTO_INCREMENT,
  external_provider VARCHAR(20) NOT NULL DEFAULT 'KAKAO',
  external_place_id VARCHAR(120) NULL,
  name VARCHAR(150) NOT NULL,
  phone VARCHAR(40) NULL,
  address VARCHAR(300) NULL,
  latitude DECIMAL(10,7) NULL,
  longitude DECIMAL(10,7) NULL,
  rating DECIMAL(2,1) NULL,
  review_count INT UNSIGNED NULL,
  cached_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  UNIQUE KEY uk_hospital_external (external_provider, external_place_id),
  KEY idx_hospital_location (latitude, longitude)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE chat_sessions (
  id BIGINT NOT NULL AUTO_INCREMENT,
  pregnancy_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  started_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  ended_at DATETIME(3) NULL,
  PRIMARY KEY (id),
  KEY idx_chat_session_preg (pregnancy_id, started_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE chat_messages (
  id BIGINT NOT NULL AUTO_INCREMENT,
  session_id BIGINT NOT NULL,
  sender_type VARCHAR(20) NOT NULL,
  message_type VARCHAR(40) NOT NULL DEFAULT 'TEXT',
  content TEXT NOT NULL,
  metadata JSON NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  KEY idx_chat_msg_session_time (session_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE emotion_records (
  id BIGINT NOT NULL AUTO_INCREMENT,
  pregnancy_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  record_date DATE NOT NULL,
  mood_score TINYINT UNSIGNED NOT NULL,
  mood_label VARCHAR(40) NOT NULL,
  note_text TEXT NULL,
  source VARCHAR(20) NOT NULL DEFAULT 'MANUAL',
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  UNIQUE KEY uk_emotion_daily (pregnancy_id, record_date),
  KEY idx_emotion_user_date (user_id, record_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE ai_reports (
  id BIGINT NOT NULL AUTO_INCREMENT,
  pregnancy_id BIGINT NOT NULL,
  emotion_record_id BIGINT NULL,
  report_type VARCHAR(40) NOT NULL,
  title VARCHAR(150) NULL,
  content TEXT NOT NULL,
  model_name VARCHAR(80) NULL,
  generated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  KEY idx_ai_report_preg_type (pregnancy_id, report_type, generated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE calendar_events (
  id BIGINT NOT NULL AUTO_INCREMENT,
  pregnancy_id BIGINT NOT NULL,
  created_by_user_id BIGINT NOT NULL,
  source_message_id BIGINT NULL,
  hospital_id BIGINT NULL,
  event_type VARCHAR(40) NOT NULL,
  title VARCHAR(150) NOT NULL,
  starts_at DATETIME(3) NOT NULL,
  ends_at DATETIME(3) NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'SUGGESTED',
  reminder_minutes_before INT UNSIGNED NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  KEY idx_event_preg_time (pregnancy_id, starts_at),
  KEY idx_event_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE wellness_checklists (
  id BIGINT NOT NULL AUTO_INCREMENT,
  pregnancy_id BIGINT NOT NULL,
  source_message_id BIGINT NULL,
  title VARCHAR(150) NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  KEY idx_checklist_preg_status (pregnancy_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE checklist_items (
  id BIGINT NOT NULL AUTO_INCREMENT,
  checklist_id BIGINT NOT NULL,
  content VARCHAR(255) NOT NULL,
  is_checked BOOLEAN NOT NULL DEFAULT FALSE,
  sort_order INT UNSIGNED NOT NULL DEFAULT 0,
  checked_at DATETIME(3) NULL,
  PRIMARY KEY (id),
  KEY idx_checklist_item_order (checklist_id, sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE emotion_translations (
  id BIGINT NOT NULL AUTO_INCREMENT,
  pregnancy_id BIGINT NOT NULL,
  source_user_id BIGINT NOT NULL,
  source_text TEXT NOT NULL,
  ai_interpretation TEXT NOT NULL,
  suggested_message TEXT NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  KEY idx_translation_preg_status (pregnancy_id, status, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE family_messages (
  id BIGINT NOT NULL AUTO_INCREMENT,
  pregnancy_id BIGINT NOT NULL,
  sender_user_id BIGINT NOT NULL,
  recipient_user_id BIGINT NULL,
  translation_id BIGINT NULL,
  message_type VARCHAR(40) NOT NULL,
  content TEXT NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'CREATED',
  sent_at DATETIME(3) NULL,
  read_at DATETIME(3) NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  KEY idx_family_msg_recipient (recipient_user_id, status, created_at),
  KEY idx_family_msg_preg (pregnancy_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE safety_alerts (
  id BIGINT NOT NULL AUTO_INCREMENT,
  pregnancy_id BIGINT NOT NULL,
  source_message_id BIGINT NULL,
  alert_type VARCHAR(40) NOT NULL,
  severity VARCHAR(20) NOT NULL DEFAULT 'MEDIUM',
  title VARCHAR(150) NOT NULL,
  description TEXT NULL,
  recommendation TEXT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  acknowledged_at DATETIME(3) NULL,
  resolved_at DATETIME(3) NULL,
  PRIMARY KEY (id),
  KEY idx_alert_preg_status (pregnancy_id, status, created_at),
  KEY idx_alert_severity (severity)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE notifications (
  id BIGINT NOT NULL AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  pregnancy_id BIGINT NULL,
  notification_type VARCHAR(40) NOT NULL,
  title VARCHAR(150) NOT NULL,
  body TEXT NULL,
  priority VARCHAR(20) NOT NULL DEFAULT 'NORMAL',
  ref_table VARCHAR(60) NULL,
  ref_id BIGINT NULL,
  read_at DATETIME(3) NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  KEY idx_notification_user_read (user_id, read_at, created_at),
  KEY idx_notification_preg (pregnancy_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
