CREATE SCHEMA tandoori;
SET search_path TO tandoori;

CREATE TABLE IF NOT EXISTS Project (
  id   SERIAL NOT NULL PRIMARY KEY,
  name VARCHAR(256) NOT NULL,
  UNIQUE (name)
);

CREATE TABLE IF NOT EXISTS Developer (
  id        SERIAL NOT NULL PRIMARY KEY,
  username  VARCHAR(256)      NOT NULL,
  stars     INTEGER,
  followers INTEGER,
  UNIQUE (username)
);

CREATE TABLE IF NOT EXISTS ProjectDeveloper (
  developerId INTEGER NOT NULL,
  projectId   INTEGER NOT NULL,
  PRIMARY KEY (developerId, projectId),
  FOREIGN KEY (projectId) REFERENCES Project (id),
  FOREIGN KEY (developerId) REFERENCES Developer (id)
);


CREATE TABLE IF NOT EXISTS Languages (
  id          SERIAL NOT NULL PRIMARY KEY,
  developerId INTEGER NOT NULL,
  language    VARCHAR(32)      NOT NULL,
  experience  INT              NOT NULL,
  UNIQUE (developerId, language),
  FOREIGN KEY (developerId) REFERENCES Developer (id)
);

CREATE TABLE IF NOT EXISTS CommitEntry (
  id          SERIAL NOT NULL PRIMARY KEY,
  projectId   INTEGER NOT NULL,
  developerId INTEGER NOT NULL,
  sha1        VARCHAR(40)      NOT NULL,
  ordinal     INTEGER NOT NULL,
  additions   INTEGER NOT NULL,
  deletions   INTEGER NOT NULL,
  filesChanged INTEGER NOT NULL,
  date        DATE             NOT NULL,
  UNIQUE (projectId, sha1),
  FOREIGN KEY (projectId) REFERENCES Project (id),
  FOREIGN KEY (developerId) REFERENCES Developer (id)
);

CREATE TABLE IF NOT EXISTS FileRename (
  id          SERIAL NOT NULL PRIMARY KEY,
  projectId   INTEGER         NOT NULL,
  commitId    INTEGER         NOT NULL,
  oldFile     VARCHAR(256)    NOT NULL,
  newFile     VARCHAR(256)    NOT NULL,
  similarity  INT             NOT NULL,
  UNIQUE (projectId, commitId, oldFile),
  FOREIGN KEY (projectId) REFERENCES Project (id),
  FOREIGN KEY (commitId) REFERENCES CommitEntry (id)
);

CREATE TABLE IF NOT EXISTS CommitEntryTag (
  id       SERIAL NOT NULL PRIMARY KEY,
  commitId INTEGER NOT NULL,
  tag      VARCHAR(10),
  UNIQUE (commitId, tag),
  FOREIGN KEY (commitId) REFERENCES CommitEntry (id)
);

CREATE TABLE IF NOT EXISTS Smell (
  id       SERIAL NOT NULL PRIMARY KEY,
  projectId INTEGER NOT NULL,
  instance VARCHAR(256) NOT NULL,
  file     VARCHAR(256) NOT NULL,
  type     VARCHAR(5)   NOT NULL,
  renamedFrom INTEGER,
  FOREIGN KEY (projectId) REFERENCES Project (id),
  FOREIGN KEY (renamedFrom) REFERENCES Smell (id)
);

CREATE TABLE IF NOT EXISTS SmellPresence (
  id       SERIAL NOT NULL PRIMARY KEY,
  smellId  INTEGER NOT NULL,
  commitId INTEGER NOT NULL,
  UNIQUE (smellId, commitId),
  FOREIGN KEY (smellId) REFERENCES Smell (id),
  FOREIGN KEY (commitId) REFERENCES CommitEntry (id)
);

CREATE TABLE IF NOT EXISTS SmellIntroduction (
  id       SERIAL NOT NULL PRIMARY KEY,
  smellId  INTEGER NOT NULL,
  commitId INTEGER NOT NULL,
  UNIQUE (smellId, commitId),
  FOREIGN KEY (smellId) REFERENCES Smell (id),
  FOREIGN KEY (commitId) REFERENCES CommitEntry (id)
);

CREATE TABLE IF NOT EXISTS SmellRefactor (
  id       SERIAL NOT NULL PRIMARY KEY,
  smellId  INTEGER NOT NULL,
  commitId INTEGER NOT NULL,
  UNIQUE (smellId, commitId),
  FOREIGN KEY (smellId) REFERENCES Smell (id),
  FOREIGN KEY (commitId) REFERENCES CommitEntry (id)
);

CREATE TABLE IF NOT EXISTS SmellDeletion (
  id       SERIAL NOT NULL PRIMARY KEY,
  smellId  INTEGER NOT NULL,
  commitId INTEGER NOT NULL,
  UNIQUE (smellId, commitId),
  FOREIGN KEY (smellId) REFERENCES Smell (id),
  FOREIGN KEY (commitId) REFERENCES CommitEntry (id)
);

