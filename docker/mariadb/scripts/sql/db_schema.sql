ALTER DATABASE CHARACTER SET 'utf8mb4' COLLATE 'utf8mb4_unicode_ci';

CREATE TABLE IF NOT EXISTS persistence_metadata (
  persistence_key BIGINT NOT NULL AUTO_INCREMENT,
  persistence_id VARCHAR(255) NOT NULL,
  sequence_nr BIGINT NOT NULL,
  PRIMARY KEY (persistence_key),
  UNIQUE (persistence_id)
) CHARACTER SET utf8 COLLATE = 'utf8mb4_unicode_ci';

CREATE TABLE IF NOT EXISTS persistence_journal (
  persistence_key BIGINT NOT NULL,
  sequence_nr BIGINT NOT NULL,
  message MEDIUMBLOB NOT NULL,
  PRIMARY KEY (persistence_key, sequence_nr),
  FOREIGN KEY (persistence_key) REFERENCES persistence_metadata (persistence_key)
) CHARACTER SET utf8 COLLATE = 'utf8mb4_unicode_ci';

CREATE TABLE IF NOT EXISTS persistence_snapshot (
  persistence_key BIGINT NOT NULL,
  sequence_nr BIGINT NOT NULL,
  created_at BIGINT NOT NULL,
  snapshot MEDIUMBLOB NOT NULL,
  PRIMARY KEY (persistence_key, sequence_nr),
  FOREIGN KEY (persistence_key) REFERENCES persistence_metadata (persistence_key)
) CHARACTER SET utf8 COLLATE = 'utf8mb4_unicode_ci';

CREATE TABLE IF NOT EXISTS DBSchema (
  id BIGINT NOT NULL,
  revision BIGINT NOT NULL,
  status varchar(50) NOT NULL,
  erstelldat DATETIME NOT NULL,
  ersteller BIGINT NOT NULL,
  modifidat DATETIME NOT NULL,
  modifikator BIGINT NOT NULL,
  PRIMARY KEY (id)
) CHARACTER SET utf8 COLLATE = 'utf8mb4_unicode_ci';

CREATE TABLE IF NOT EXISTS PersistenceEventState  (
  id BIGINT not null,
  persistence_id varchar(100) not null,
  last_transaction_nr BIGINT default 0,
  last_sequence_nr BIGINT default 0,
  erstelldat datetime not null,
  ersteller BIGINT not null,
  modifidat datetime not null,
  modifikator BIGINT not null
) CHARACTER SET utf8 COLLATE = 'utf8mb4_unicode_ci';
