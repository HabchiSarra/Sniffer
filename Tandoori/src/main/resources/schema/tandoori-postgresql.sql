CREATE SCHEMA IF NOT EXISTS tandoori;
SET search_path TO tandoori;

CREATE TABLE IF NOT EXISTS Project (
  id   SERIAL NOT NULL PRIMARY KEY,
  name VARCHAR(256) NOT NULL,
  url         VARCHAR(256),
  UNIQUE (name)
);

CREATE TABLE IF NOT EXISTS Developer (
  id        SERIAL NOT NULL PRIMARY KEY,
  username  VARCHAR(256)      NOT NULL,
  stars     INTEGER,
  followers INTEGER,
  UNIQUE (username)
);

CREATE TABLE IF NOT EXISTS project_developer (
  id           SERIAL NOT NULL PRIMARY KEY,
  developer_id INTEGER NOT NULL,
  project_id   INTEGER NOT NULL,
  UNIQUE (developer_id, project_id),
  FOREIGN KEY (project_id) REFERENCES Project (id),
  FOREIGN KEY (developer_id) REFERENCES Developer (id)
);


CREATE TABLE IF NOT EXISTS Languages (
  id          SERIAL NOT NULL PRIMARY KEY,
  developer_id INTEGER NOT NULL,
  language    VARCHAR(32)      NOT NULL,
  experience  INT              NOT NULL,
  UNIQUE (developer_id, language),
  FOREIGN KEY (developer_id) REFERENCES Developer (id)
);

CREATE TABLE IF NOT EXISTS commit_entry (
  id          SERIAL NOT NULL PRIMARY KEY,
  project_id   INTEGER NOT NULL,
  developer_id INTEGER NOT NULL,
  sha1        VARCHAR(40)      NOT NULL,
  ordinal     INTEGER NOT NULL,
  additions   INTEGER NOT NULL,
  deletions   INTEGER NOT NULL,
  files_changed INTEGER NOT NULL,
  message      TEXT NOT NULL,
  date        DATE             NOT NULL,
  merged_commit_id INTEGER,
  UNIQUE (project_id, sha1),
  FOREIGN KEY (project_id) REFERENCES Project (id),
  FOREIGN KEY (developer_id) REFERENCES Developer (id),
  FOREIGN KEY (merged_commit_id) REFERENCES commit_entry (id)
);

CREATE TABLE IF NOT EXISTS Branch (
  id            SERIAL NOT NULL PRIMARY KEY,
  project_id    INTEGER NOT NULL,
  ordinal       INTEGER NOT NULL,
  parent_commit INTEGER,
  merged_into   INTEGER,
  UNIQUE (project_id, ordinal),
  FOREIGN KEY (project_id) REFERENCES Project (id),
  FOREIGN KEY (parent_commit) REFERENCES commit_entry (id),
  FOREIGN KEY (merged_into) REFERENCES commit_entry (id)
);

CREATE TABLE IF NOT EXISTS branch_commit (
  id         SERIAL NOT NULL PRIMARY KEY,
  branch_id   INTEGER NOT NULL,
  commit_id   INTEGER NOT NULL,
  ordinal    INTEGER NOT NULL,
  UNIQUE (branch_id, commit_id),
  UNIQUE (branch_id, ordinal),
  FOREIGN KEY (branch_id) REFERENCES Branch (id),
  FOREIGN KEY (commit_id) REFERENCES commit_entry (id)
);

CREATE TABLE IF NOT EXISTS file_rename (
  id          SERIAL NOT NULL PRIMARY KEY,
  project_id   INTEGER         NOT NULL,
  commit_id    INTEGER         NOT NULL,
  old_file     VARCHAR(256)    NOT NULL,
  new_file     VARCHAR(256)    NOT NULL,
  similarity  INT             NOT NULL,
  UNIQUE (project_id, commit_id, old_file),
  FOREIGN KEY (project_id) REFERENCES Project (id),
  FOREIGN KEY (commit_id) REFERENCES commit_entry (id)
);

CREATE TABLE IF NOT EXISTS commit_entry_tag (
  id       SERIAL NOT NULL PRIMARY KEY,
  commit_id INTEGER NOT NULL,
  tag      VARCHAR(10),
  UNIQUE (commit_id, tag),
  FOREIGN KEY (commit_id) REFERENCES commit_entry (id)
);
CREATE TABLE IF NOT EXISTS Smell (
  id       SERIAL NOT NULL PRIMARY KEY,
  project_id INTEGER NOT NULL,
  instance VARCHAR(256) NOT NULL,
  file     VARCHAR(256) NOT NULL,
  type     VARCHAR(5)   NOT NULL,
  renamed_from INTEGER,
  FOREIGN KEY (project_id) REFERENCES Project (id),
  FOREIGN KEY (renamed_from) REFERENCES Smell (id)
);
-- Creating unicity index for smells with renamed_from entry filled
CREATE UNIQUE INDEX IF NOT EXISTS smell__instance_file_type_project_id_renamed_from
ON Smell (instance, file, type, project_id, renamed_from)
WHERE renamed_from IS NOT NULL;

-- Creating another unicity index for smells with null renamed_from entry
CREATE UNIQUE INDEX IF NOT EXISTS smell__instance_file_type_project_id
ON Smell (instance, file, type, project_id)
WHERE renamed_from IS NULL;


CREATE TABLE IF NOT EXISTS smell_presence (
  id       SERIAL NOT NULL PRIMARY KEY,
  smell_id  INTEGER NOT NULL,
  project_id INTEGER NOT NULL,
  commit_id INTEGER NOT NULL,
  UNIQUE (smell_id, commit_id),
  FOREIGN KEY (smell_id) REFERENCES Smell (id),
  FOREIGN KEY (commit_id) REFERENCES commit_entry (id),
  FOREIGN KEY (project_id) REFERENCES Project (id)
);

CREATE TABLE IF NOT EXISTS smell_introduction (
  id       SERIAL NOT NULL PRIMARY KEY,
  smell_id  INTEGER NOT NULL,
  project_id INTEGER NOT NULL,
  ignored BOOLEAN NOT NULL DEFAULT FALSE,
  commit_id INTEGER NOT NULL,
  UNIQUE (smell_id, commit_id),
  FOREIGN KEY (smell_id) REFERENCES Smell (id),
  FOREIGN KEY (commit_id) REFERENCES commit_entry (id),
  FOREIGN KEY (project_id) REFERENCES Project (id)
);

CREATE TABLE IF NOT EXISTS smell_refactoring (
  id       SERIAL NOT NULL PRIMARY KEY,
  smell_id  INTEGER NOT NULL,
  project_id INTEGER NOT NULL,
  commit_id INTEGER NOT NULL,
  ignored BOOLEAN NOT NULL DEFAULT FALSE,
  UNIQUE (smell_id, commit_id),
  FOREIGN KEY (smell_id) REFERENCES Smell (id),
  FOREIGN KEY (commit_id) REFERENCES commit_entry (id),
  FOREIGN KEY (project_id) REFERENCES Project (id)
);

CREATE TABLE IF NOT EXISTS lost_smell_introduction (
  id       SERIAL NOT NULL PRIMARY KEY,
  smell_id  INTEGER NOT NULL,
  project_id INTEGER NOT NULL,
  since    INTEGER NOT NULL,
  until    INTEGER NOT NULL,
  FOREIGN KEY (smell_id) REFERENCES Smell (id),
  FOREIGN KEY (project_id) REFERENCES Project (id)
);

CREATE TABLE IF NOT EXISTS lost_smell_refactoring (
  id       SERIAL NOT NULL PRIMARY KEY,
  smell_id  INTEGER NOT NULL,
  project_id INTEGER NOT NULL,
  since    INTEGER NOT NULL,
  until    INTEGER NOT NULL,
  FOREIGN KEY (smell_id) REFERENCES Smell (id),
  FOREIGN KEY (project_id) REFERENCES Project (id)
);
