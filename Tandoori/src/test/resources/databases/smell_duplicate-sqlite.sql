PRAGMA foreign_keys=OFF;
BEGIN TRANSACTION;
CREATE TABLE `Project` (
  id   INTEGER PRIMARY KEY AUTOINCREMENT,
  name VARCHAR(256) NOT NULL,
  url         VARCHAR(256),
  UNIQUE (name)
);
INSERT INTO Project VALUES(1,'project');
CREATE TABLE `project_developer` (
  developer_id INTEGER NOT NULL,
  project_id   INTEGER NOT NULL,
  PRIMARY KEY (developer_id, project_id),
  FOREIGN KEY (project_id) REFERENCES Project (id),
  FOREIGN KEY (developer_id) REFERENCES Developer (id)
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
  developer_id INTEGER NOT NULL,
  language    VARCHAR(32)      NOT NULL,
  experience  INT              NOT NULL,
  UNIQUE (developer_id, language),
  FOREIGN KEY (developer_id) REFERENCES Developer (id)
);
CREATE TABLE `commit_entry` (
  id          INTEGER PRIMARY KEY AUTOINCREMENT,
  project_id   INTEGER NOT NULL,
  developer_id INTEGER NOT NULL,
  sha1        VARCHAR(40)      NOT NULL,
  ordinal     INTEGER UNSIGNED NOT NULL,
  additions   INTEGER UNSIGNED NOT NULL,
  deletions   INTEGER UNSIGNED NOT NULL,
  files_changed INTEGER UNSIGNED NOT NULL,
  date        DATE             NOT NULL,
  UNIQUE (project_id, sha1),
  FOREIGN KEY (project_id) REFERENCES Project (id),
  FOREIGN KEY (developer_id) REFERENCES Developer (id)
);
INSERT INTO commit_entry VALUES(1,1,1,'first',1,0,0,0,'2018-01-01');
INSERT INTO commit_entry VALUES(2,1,1,'second',2,0,0,0,'2018-01-01');
INSERT INTO commit_entry VALUES(3,1,1,'third',3,0,0,0,'2018-01-01');
INSERT INTO commit_entry VALUES(4,1,1,'fourth',4,0,0,0,'2018-01-01');
CREATE TABLE `file_rename` (
  id          INTEGER PRIMARY KEY AUTOINCREMENT,
  project_id   INTEGER         NOT NULL,
  commit_id    INTEGER         NOT NULL,
  old_file     VARCHAR(256)    NOT NULL,
  new_file     VARCHAR(256)    NOT NULL,
  similarity  INT             NOT NULL,
  UNIQUE (project_id, commit_id, old_file),
  FOREIGN KEY (project_id) REFERENCES Project (id),
  FOREIGN KEY (commit_id) REFERENCES commit_entry (id)
);
INSERT INTO file_rename VALUES(1,1,2,'Fa','Fc',100);
INSERT INTO file_rename VALUES(2,1,2,'Fd','Fx',100);
INSERT INTO file_rename VALUES(3,1,2,'Fk','Fb',100);
INSERT INTO file_rename VALUES(4,1,3,'Fc','Fg',100);
CREATE TABLE `commit_entryTag` (
  id       INTEGER PRIMARY KEY AUTOINCREMENT,
  commit_id INTEGER NOT NULL,
  tag      VARCHAR(10),
  UNIQUE (commit_id, tag),
  FOREIGN KEY (commit_id) REFERENCES `commit_entry` (id)
);
CREATE TABLE `smell_presence` (
  id       INTEGER PRIMARY KEY AUTOINCREMENT,
  smell_id  INTEGER NOT NULL,
  commit_id INTEGER NOT NULL,
  UNIQUE (smell_id, commit_id),
  FOREIGN KEY (smell_id) REFERENCES Smell (id),
  FOREIGN KEY (commit_id) REFERENCES `commit_entry` (id)
);
INSERT INTO smell_presence VALUES(1,1,1);
INSERT INTO smell_presence VALUES(2,2,1);
INSERT INTO smell_presence VALUES(3,4,1);
INSERT INTO smell_presence VALUES(4,7,1);
INSERT INTO smell_presence VALUES(5,8,1);
INSERT INTO smell_presence VALUES(6,2,2);
INSERT INTO smell_presence VALUES(7,3,2);
INSERT INTO smell_presence VALUES(8,5,2);
INSERT INTO smell_presence VALUES(9,7,2);
INSERT INTO smell_presence VALUES(10,8,2);
INSERT INTO smell_presence VALUES(11,2,3);
INSERT INTO smell_presence VALUES(12,5,3);
INSERT INTO smell_presence VALUES(13,6,3);
INSERT INTO smell_presence VALUES(14,7,3);
INSERT INTO smell_presence VALUES(15,8,3);
INSERT INTO smell_presence VALUES(16,9,4);
INSERT INTO smell_presence VALUES(17,10,4);
CREATE TABLE `smell_introduction` (
  id       INTEGER PRIMARY KEY AUTOINCREMENT,
  smell_id  INTEGER NOT NULL,
  commit_id INTEGER NOT NULL,
  UNIQUE (smell_id, commit_id),
  FOREIGN KEY (smell_id) REFERENCES Smell (id),
  FOREIGN KEY (commit_id) REFERENCES `commit_entry` (id)
);
CREATE TABLE `smell_refactoring` (
  id       INTEGER PRIMARY KEY AUTOINCREMENT,
  smell_id  INTEGER NOT NULL,
  commit_id INTEGER NOT NULL,
  UNIQUE (smell_id, commit_id),
  FOREIGN KEY (smell_id) REFERENCES Smell (id),
  FOREIGN KEY (commit_id) REFERENCES `commit_entry` (id)
);
CREATE TABLE `SmellDeletion` (
  id       INTEGER PRIMARY KEY AUTOINCREMENT,
  smell_id  INTEGER NOT NULL,
  commit_id INTEGER NOT NULL,
  UNIQUE (smell_id, commit_id),
  FOREIGN KEY (smell_id) REFERENCES Smell (id),
  FOREIGN KEY (commit_id) REFERENCES `commit_entry` (id)
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
INSERT INTO sqlite_sequence VALUES('commit_entry',4);
INSERT INTO sqlite_sequence VALUES('Smell',10);
INSERT INTO sqlite_sequence VALUES('file_rename',4);
INSERT INTO sqlite_sequence VALUES('smell_presence',17);
COMMIT;