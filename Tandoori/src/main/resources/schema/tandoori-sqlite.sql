-- Tandoori database definition in SQLite format.

CREATE TABLE IF NOT EXISTS `Project` (
  id   INTEGER UNSIGNED PRIMARY KEY AUTOINCREMENT,
  name VARCHAR(256) NOT NULL,
  PRIMARY KEY (id),
  UNIQUE (name)
);

CREATE TABLE IF NOT EXISTS `ProjectDeveloper` (
  developerId INTEGER UNSIGNED PRIMARY KEY AUTOINCREMENT,
  projectId   INTEGER UNSIGNED NOT NULL,
  PRIMARY KEY (developerId, projectId),
  FOREIGN KEY (projectId) REFERENCES Project (id),
  FOREIGN KEY (developerId) REFERENCES Developer (id)
);

CREATE TABLE IF NOT EXISTS `Developer` (
  id        INTEGER UNSIGNED PRIMARY KEY AUTOINCREMENT,
  username  VARCHAR(40)      NOT NULL,
  stars     INTEGER UNSIGNED NOT NULL,
  followers INTEGER UNSIGNED NOT NULL,
  UNIQUE (username)
);

CREATE TABLE IF NOT EXISTS `Languages` (
  id          INTEGER UNSIGNED PRIMARY KEY AUTOINCREMENT,
  developerId INTEGER UNSIGNED NOT NULL,
  language    VARCHAR(32)      NOT NULL,
  experience  INT              NOT NULL,
  UNIQUE (developerId, language),
  FOREIGN KEY (developerId) REFERENCES Developer (id)

);

CREATE TABLE IF NOT EXISTS `Commit` (
  id          INTEGER UNSIGNED PRIMARY KEY AUTOINCREMENT,
  projectId   INTEGER UNSIGNED NOT NULL,
  developerId INTEGER UNSIGNED NOT NULL,
  sha1        VARCHAR(40)      NOT NULL,
  ordinal     INTEGER UNSIGNED NOT NULL,
  date        DATE             NOT NULL,
  UNIQUE (projectId, sha1),
  FOREIGN KEY (projectId) REFERENCES Project (id),
  FOREIGN KEY (developerId) REFERENCES Developer (id)
);

CREATE TABLE IF NOT EXISTS `CommitTag` (
  id       INTEGER UNSIGNED PRIMARY KEY AUTOINCREMENT,
  commitId INTEGER UNSIGNED NOT NULL,
  tag      VARCHAR(10),
  UNIQUE (commitId, tag),
  FOREIGN KEY (commitId) REFERENCES `Commit` (id)
);

CREATE TABLE IF NOT EXISTS `Smell` (
  id       INTEGER UNSIGNED PRIMARY KEY AUTOINCREMENT,
  instance VARCHAR(256) NOT NULL,
  type     VARCHAR(5)   NOT NULL,
  UNIQUE (instance)
);

CREATE TABLE IF NOT EXISTS `SmellIntroduction` (
  id       INTEGER UNSIGNED PRIMARY KEY AUTOINCREMENT,
  smellId  INTEGER UNSIGNED NOT NULL,
  commitId INTEGER UNSIGNED NOT NULL,
  UNIQUE (smellId, commitId),
  FOREIGN KEY (smellId) REFERENCES Smell (id),
  FOREIGN KEY (commitId) REFERENCES `Commit` (id)
);

CREATE TABLE IF NOT EXISTS `SmellRefactor` (
  id       INTEGER UNSIGNED PRIMARY KEY AUTOINCREMENT,
  smellId  INTEGER UNSIGNED,
  commitId INTEGER UNSIGNED,
  UNIQUE (smellId, commitId),
  FOREIGN KEY (smellId) REFERENCES Smell (id),
  FOREIGN KEY (commitId) REFERENCES `Commit` (id)
);

CREATE TABLE IF NOT EXISTS `SmellDeletion` (
  id       INTEGER UNSIGNED PRIMARY KEY AUTOINCREMENT,
  smellId  INTEGER UNSIGNED,
  commitId INTEGER UNSIGNED,
  UNIQUE (smellId, commitId),
  FOREIGN KEY (smellId) REFERENCES Smell (id),
  FOREIGN KEY (commitId) REFERENCES `Commit` (id)
);

