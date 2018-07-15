-- Tandoori database definition in SQLite format.

CREATE TABLE IF NOT EXISTS `Project` (
  id   INTEGER PRIMARY KEY AUTOINCREMENT,
  name VARCHAR(256) NOT NULL,
  url         VARCHAR(256),
  UNIQUE (name)
);

CREATE TABLE IF NOT EXISTS `ProjectDeveloper` (
  developerId INTEGER NOT NULL,
  projectId   INTEGER NOT NULL,
  PRIMARY KEY (developerId, projectId),
  FOREIGN KEY (projectId) REFERENCES Project (id),
  FOREIGN KEY (developerId) REFERENCES Developer (id)
);

CREATE TABLE IF NOT EXISTS `Developer` (
  id        INTEGER PRIMARY KEY AUTOINCREMENT,
  username  VARCHAR(256)      NOT NULL,
  stars     INTEGER UNSIGNED,
  followers INTEGER UNSIGNED,
  UNIQUE (username)
);

CREATE TABLE IF NOT EXISTS `Languages` (
  id          INTEGER PRIMARY KEY AUTOINCREMENT,
  developerId INTEGER NOT NULL,
  language    VARCHAR(32)      NOT NULL,
  experience  INT              NOT NULL,
  UNIQUE (developerId, language),
  FOREIGN KEY (developerId) REFERENCES Developer (id)
);

CREATE TABLE IF NOT EXISTS `CommitEntry` (
  id          INTEGER PRIMARY KEY AUTOINCREMENT,
  projectId   INTEGER NOT NULL,
  developerId INTEGER NOT NULL,
  sha1        VARCHAR(40)      NOT NULL,
  ordinal     INTEGER UNSIGNED NOT NULL,
  additions   INTEGER UNSIGNED NOT NULL,
  deletions   INTEGER UNSIGNED NOT NULL,
  filesChanged INTEGER UNSIGNED NOT NULL,
  message      TEXT NOT NULL,
  date        DATE             NOT NULL,
  UNIQUE (projectId, sha1),
  FOREIGN KEY (projectId) REFERENCES Project (id),
  FOREIGN KEY (developerId) REFERENCES Developer (id)
);

CREATE TABLE IF NOT EXISTS `FileRename` (
  id          INTEGER PRIMARY KEY AUTOINCREMENT,
  projectId   INTEGER         NOT NULL,
  commitId    INTEGER         NOT NULL,
  oldFile     VARCHAR(256)    NOT NULL,
  newFile     VARCHAR(256)    NOT NULL,
  similarity  INT             NOT NULL,
  UNIQUE (projectId, commitId, oldFile),
  FOREIGN KEY (projectId) REFERENCES Project (id),
  FOREIGN KEY (commitId) REFERENCES CommitEntry (id)
);

CREATE TABLE IF NOT EXISTS `CommitEntryTag` (
  id       INTEGER PRIMARY KEY AUTOINCREMENT,
  commitId INTEGER NOT NULL,
  tag      VARCHAR(10),
  UNIQUE (commitId, tag),
  FOREIGN KEY (commitId) REFERENCES `CommitEntry` (id)
);

CREATE TABLE IF NOT EXISTS `Smell` (
  id          INTEGER PRIMARY KEY AUTOINCREMENT,
  projectId   INTEGER NOT NULL,
  instance    VARCHAR(256) NOT NULL,
  file        VARCHAR(256) NOT NULL,
  type        VARCHAR(5)   NOT NULL,
  renamedFrom INTEGER UNSIGNED
  FOREIGN KEY (projectId) REFERENCES Project (id),
  FOREIGN KEY (renamedFrom) REFERENCES Smell (id)
);

CREATE TABLE IF NOT EXISTS `SmellPresence` (
  id       INTEGER PRIMARY KEY AUTOINCREMENT,
  smellId  INTEGER NOT NULL,
  commitId INTEGER NOT NULL,
  UNIQUE (smellId, commitId),
  FOREIGN KEY (smellId) REFERENCES Smell (id),
  FOREIGN KEY (commitId) REFERENCES `CommitEntry` (id)
);

CREATE TABLE IF NOT EXISTS `SmellIntroduction` (
  id       INTEGER PRIMARY KEY AUTOINCREMENT,
  smellId  INTEGER NOT NULL,
  commitId INTEGER NOT NULL,
  ignored BOOLEAN NOT NULL DEFAULT FALSE,
  UNIQUE (smellId, commitId),
  FOREIGN KEY (smellId) REFERENCES Smell (id),
  FOREIGN KEY (commitId) REFERENCES `CommitEntry` (id)
);

CREATE TABLE IF NOT EXISTS `SmellRefactor` (
  id       INTEGER PRIMARY KEY AUTOINCREMENT,
  smellId  INTEGER NOT NULL,
  commitId INTEGER NOT NULL,
  ignored BOOLEAN NOT NULL DEFAULT FALSE,
  UNIQUE (smellId, commitId),
  FOREIGN KEY (smellId) REFERENCES Smell (id),
  FOREIGN KEY (commitId) REFERENCES `CommitEntry` (id)
);
