PRAGMA foreign_keys=OFF;
BEGIN TRANSACTION;
CREATE TABLE `Project` (
  id   INTEGER PRIMARY KEY AUTOINCREMENT,
  name VARCHAR(256) NOT NULL,
  UNIQUE (name)
);
INSERT INTO Project VALUES(1,'project');
CREATE TABLE `ProjectDeveloper` (
  developerId INTEGER NOT NULL,
  projectId   INTEGER NOT NULL,
  PRIMARY KEY (developerId, projectId),
  FOREIGN KEY (projectId) REFERENCES Project (id),
  FOREIGN KEY (developerId) REFERENCES Developer (id)
);
CREATE TABLE `Developer` (
  id        INTEGER PRIMARY KEY AUTOINCREMENT,
  username  VARCHAR(256)      NOT NULL,
  stars     INTEGER UNSIGNED,
  followers INTEGER UNSIGNED,
  UNIQUE (username)
);
CREATE TABLE `Languages` (
  id          INTEGER PRIMARY KEY AUTOINCREMENT,
  developerId INTEGER NOT NULL,
  language    VARCHAR(32)      NOT NULL,
  experience  INT              NOT NULL,
  UNIQUE (developerId, language),
  FOREIGN KEY (developerId) REFERENCES Developer (id)
);
CREATE TABLE `CommitEntry` (
  id          INTEGER PRIMARY KEY AUTOINCREMENT,
  projectId   INTEGER NOT NULL,
  developerId INTEGER NOT NULL,
  sha1        VARCHAR(40)      NOT NULL,
  ordinal     INTEGER UNSIGNED NOT NULL,
  additions   INTEGER UNSIGNED NOT NULL,
  deletions   INTEGER UNSIGNED NOT NULL,
  filesChanged INTEGER UNSIGNED NOT NULL,
  date        DATE             NOT NULL,
  UNIQUE (projectId, sha1),
  FOREIGN KEY (projectId) REFERENCES Project (id),
  FOREIGN KEY (developerId) REFERENCES Developer (id)
);
INSERT INTO CommitEntry VALUES(1,1,1,'first',1,0,0,0,'2018-01-01');
INSERT INTO CommitEntry VALUES(2,1,1,'second',2,0,0,0,'2018-01-01');
INSERT INTO CommitEntry VALUES(3,1,1,'third',3,0,0,0,'2018-01-01');
INSERT INTO CommitEntry VALUES(4,1,1,'fourth',4,0,0,0,'2018-01-01');
CREATE TABLE `FileRename` (
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
INSERT INTO FileRename VALUES(1,1,2,'Fa','Fc',100);
INSERT INTO FileRename VALUES(2,1,2,'Fd','Fx',100);
INSERT INTO FileRename VALUES(3,1,2,'Fk','Fb',100);
INSERT INTO FileRename VALUES(4,1,3,'Fc','Fg',100);
CREATE TABLE `CommitEntryTag` (
  id       INTEGER PRIMARY KEY AUTOINCREMENT,
  commitId INTEGER NOT NULL,
  tag      VARCHAR(10),
  UNIQUE (commitId, tag),
  FOREIGN KEY (commitId) REFERENCES `CommitEntry` (id)
);
CREATE TABLE `SmellPresence` (
  id       INTEGER PRIMARY KEY AUTOINCREMENT,
  smellId  INTEGER NOT NULL,
  commitId INTEGER NOT NULL,
  UNIQUE (smellId, commitId),
  FOREIGN KEY (smellId) REFERENCES Smell (id),
  FOREIGN KEY (commitId) REFERENCES `CommitEntry` (id)
);
INSERT INTO SmellPresence VALUES(1,1,1);
INSERT INTO SmellPresence VALUES(2,2,1);
INSERT INTO SmellPresence VALUES(3,4,1);
INSERT INTO SmellPresence VALUES(4,7,1);
INSERT INTO SmellPresence VALUES(5,8,1);
INSERT INTO SmellPresence VALUES(6,2,2);
INSERT INTO SmellPresence VALUES(7,3,2);
INSERT INTO SmellPresence VALUES(8,5,2);
INSERT INTO SmellPresence VALUES(9,7,2);
INSERT INTO SmellPresence VALUES(10,8,2);
INSERT INTO SmellPresence VALUES(11,2,3);
INSERT INTO SmellPresence VALUES(12,5,3);
INSERT INTO SmellPresence VALUES(13,6,3);
INSERT INTO SmellPresence VALUES(14,7,3);
INSERT INTO SmellPresence VALUES(15,8,3);
INSERT INTO SmellPresence VALUES(16,9,4);
INSERT INTO SmellPresence VALUES(17,10,4);
CREATE TABLE `SmellIntroduction` (
  id       INTEGER PRIMARY KEY AUTOINCREMENT,
  smellId  INTEGER NOT NULL,
  commitId INTEGER NOT NULL,
  UNIQUE (smellId, commitId),
  FOREIGN KEY (smellId) REFERENCES Smell (id),
  FOREIGN KEY (commitId) REFERENCES `CommitEntry` (id)
);
CREATE TABLE `SmellRefactor` (
  id       INTEGER PRIMARY KEY AUTOINCREMENT,
  smellId  INTEGER NOT NULL,
  commitId INTEGER NOT NULL,
  UNIQUE (smellId, commitId),
  FOREIGN KEY (smellId) REFERENCES Smell (id),
  FOREIGN KEY (commitId) REFERENCES `CommitEntry` (id)
);
CREATE TABLE `SmellDeletion` (
  id       INTEGER PRIMARY KEY AUTOINCREMENT,
  smellId  INTEGER NOT NULL,
  commitId INTEGER NOT NULL,
  UNIQUE (smellId, commitId),
  FOREIGN KEY (smellId) REFERENCES Smell (id),
  FOREIGN KEY (commitId) REFERENCES `CommitEntry` (id)
);
CREATE TABLE `Smell` (
  id       INTEGER PRIMARY KEY AUTOINCREMENT,
  instance VARCHAR(256) NOT NULL,
  file     VARCHAR(256) NOT NULL,
  type     VARCHAR(5)   NOT NULL,
  UNIQUE (instance, type)
);
INSERT INTO Smell VALUES(1,'a','Fa','IOD');
INSERT INTO Smell VALUES(2,'b','Fb','IOD');
INSERT INTO Smell VALUES(3,'c','Fc','IOD');
INSERT INTO Smell VALUES(4,'d','Fd','IOD');
INSERT INTO Smell VALUES(5,'e','Fe','IOD');
INSERT INTO Smell VALUES(6,'g','Fg','IOD');
INSERT INTO Smell VALUES(7,'a','Fa','LIC');
INSERT INTO Smell VALUES(8,'c','Fc','HMU');
INSERT INTO Smell VALUES(9,'x','Fa','IOD');
INSERT INTO Smell VALUES(10,'z','Fc','IOD');
DELETE FROM sqlite_sequence;
INSERT INTO sqlite_sequence VALUES('Project',1);
INSERT INTO sqlite_sequence VALUES('CommitEntry',4);
INSERT INTO sqlite_sequence VALUES('Smell',10);
INSERT INTO sqlite_sequence VALUES('FileRename',4);
INSERT INTO sqlite_sequence VALUES('SmellPresence',17);
COMMIT;