CREATE DATABASE IF NOT EXISTS replace_me;

CREATE TABLE IF NOT EXISTS replace_me.persistence_metadata (
  persistence_key BIGINT NOT NULL AUTO_INCREMENT,
  persistence_id VARCHAR(255) NOT NULL,
  sequence_nr BIGINT NOT NULL,
  PRIMARY KEY (persistence_key),
  UNIQUE (persistence_id)
) CHARACTER SET utf8 COLLATE = 'utf8_unicode_ci';

CREATE TABLE IF NOT EXISTS replace_me.persistence_journal (
  persistence_key BIGINT NOT NULL,
  sequence_nr BIGINT NOT NULL,
  message MEDIUMBLOB NOT NULL,
  PRIMARY KEY (persistence_key, sequence_nr),
  FOREIGN KEY (persistence_key) REFERENCES persistence_metadata (persistence_key)
) CHARACTER SET utf8 COLLATE = 'utf8_unicode_ci';

CREATE TABLE IF NOT EXISTS replace_me.persistence_snapshot (
  persistence_key BIGINT NOT NULL,
  sequence_nr BIGINT NOT NULL,
  created_at BIGINT NOT NULL,
  snapshot MEDIUMBLOB NOT NULL,
  PRIMARY KEY (persistence_key, sequence_nr),
  FOREIGN KEY (persistence_key) REFERENCES persistence_metadata (persistence_key)
) CHARACTER SET utf8 COLLATE = 'utf8_unicode_ci';

CREATE TABLE IF NOT EXISTS replace_me.DBSchema (
  id BIGINT NOT NULL,
  revision BIGINT NOT NULL,
  status varchar(50) NOT NULL,
  erstelldat DATETIME NOT NULL,
  ersteller BIGINT NOT NULL,
  modifidat DATETIME NOT NULL,
  modifikator BIGINT NOT NULL,
  PRIMARY KEY (id)
) CHARACTER SET utf8 COLLATE = 'utf8_unicode_ci';

CREATE TABLE IF NOT EXISTS replace_me.PersistenceEventState  (
  id BIGINT not null,
  persistence_id varchar(100) not null,
  last_transaction_nr BIGINT default 0,
  last_sequence_nr BIGINT default 0,
  erstelldat datetime not null,
  ersteller BIGINT not null,
  modifidat datetime not null,
  modifikator BIGINT not null
) CHARACTER SET utf8 COLLATE = 'utf8_unicode_ci';

CREATE TABLE IF NOT EXISTS replace_me.event_journal (
  ordering SERIAL,
  deleted BOOLEAN DEFAULT false NOT NULL,
  persistence_id VARCHAR(255) NOT NULL,
  sequence_number BIGINT NOT NULL,
  writer TEXT NOT NULL,
  write_timestamp BIGINT NOT NULL,
  adapter_manifest TEXT NOT NULL,
  event_payload MEDIUMBLOB NOT NULL,
  event_ser_id INTEGER NOT NULL,
  event_ser_manifest TEXT NOT NULL,
  meta_payload MEDIUMBLOB,
  meta_ser_id INTEGER,meta_ser_manifest TEXT,
  PRIMARY KEY(persistence_id,sequence_number)
) CHARACTER SET utf8 COLLATE = 'utf8_unicode_ci';

CREATE TABLE IF NOT EXISTS replace_me.event_tag (
  event_id BIGINT UNSIGNED NOT NULL,
  tag VARCHAR(255) NOT NULL,
  PRIMARY KEY(event_id, tag),
  FOREIGN KEY (event_id)
      REFERENCES event_journal(ordering)
      ON DELETE CASCADE
) CHARACTER SET utf8 COLLATE = 'utf8_unicode_ci';

CREATE TABLE IF NOT EXISTS replace_me.snapshot (
  persistence_id VARCHAR(255) NOT NULL,
  sequence_number BIGINT NOT NULL,
  created BIGINT NOT NULL,
  snapshot_ser_id INTEGER NOT NULL,
  snapshot_ser_manifest TEXT NOT NULL,
  snapshot_payload MEDIUMBLOB NOT NULL,
  meta_ser_id INTEGER,
  meta_ser_manifest TEXT,
  meta_payload MEDIUMBLOB,
  PRIMARY KEY (persistence_id, sequence_number)
) CHARACTER SET utf8 COLLATE = 'utf8_unicode_ci';
