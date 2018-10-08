# coding=utf-8
import csv
import os
from enum import Enum
from typing import List, Dict

import datetime

from dateutil import parser

from project_profiler.analysis.commits.classification import CommitClassifier
from project_profiler.analysis.output import CsvOutputWriter


def merge_commits_with_smells(input_metrics: str, commits: str):
    csv_writer = CommitMergerCsvWriter("/data/Output-Commits-context-RQ1.csv")
    for project in [x for x in os.listdir(input_metrics) if os.path.isdir(os.path.join(input_metrics, x))]:
        file_path = os.path.join(input_metrics, project, "metrics/metrics-perDev-perCommit-perSmell.csv")

        # Load commit analysis file
        commit_entries = CommitLoader(os.path.join(commits, "commits-" + project + ".csv"))

        with open(file_path, 'r') as smell_count_file:
            reader = csv.DictReader(smell_count_file)
            for line in reader:
                smell_entries = [SmellEntry(smell, reader.fieldnames) for smell in SmellType]
                for smell_entry in smell_entries:
                    smell_entry.parse(line)
                commit_sha = line["sha"]
                commit_entry: CommitEntry = commit_entries.commits[commit_sha]
                csv_writer.add_commit(project=project, sha1=commit_sha,
                                      additions=commit_entry.additions, deletions=commit_entry.deletions,
                                      message=commit_entry.message, categories=commit_entry.categories,
                                      distance_release=commit_entry.distance_release,
                                      distance_startup=commit_entry.distance_startup,
                                      smells=smell_entries)
    csv_writer.write()


class AlifSeparatedValueDialect(csv.excel):
    delimiter = 'ا'


class CommitLoader(object):
    def __init__(self, file: str):
        self.commits = {}
        self._parse_entries(file)

    def _parse_entries(self, file: str):
        first_commit_date = None
        with open(file, 'r') as csv_file:
            reader = csv.DictReader(csv_file, dialect=AlifSeparatedValueDialect())
            # print("Parsing commits csv, with header: " + str(reader.fieldnames))
            for line in reader:
                if first_commit_date is None:
                    first_commit_date = parser.parse(line["date"])
                self._add_commit(line, first_commit_date)
                # Notifying our previous commits if we have a tag
                if line["tags"] != '[]':
                    # print("tags:" + str(line["tags"]))
                    for commit in self.commits.values():
                        commit.notify_release(parser.parse(line["date"]))

    def _add_commit(self, line: Dict, first_commit_date: datetime):
        sha = line["sha1"]
        commit_date = parser.parse(line["date"])
        distance_startup = (commit_date - first_commit_date).days
        self.commits[sha] = CommitEntry(sha, line["committer"], line["message"], line["addition"],
                                        line["deletion"],
                                        line["classifications"], distance_startup, commit_date)


class CommitEntry(object):
    def __init__(self, sha: str, author: str, message: str, additions: int, deletions: int,
                 categories: List[str], distance_startup: int, commit_date: datetime):
        self.sha = sha
        self.author = author
        self.message = message
        self.additions = additions
        self.deletions = deletions
        self.categories = categories
        self.distance_startup = distance_startup
        self.date = commit_date
        self.distance_release = -1

    def notify_release(self, release_date: datetime):
        if release_date >= self.date and self.distance_release == -1:
            self.distance_release = (release_date - self.date).days
            # print("setting distance for commit: " + self.sha + "("
            #     + str(self.date) + ") to release (" + str(release_date) + ")- " + str(self.distance_release))


class SmellType(Enum):
    HMU = "HMU",
    IOD = "IOD"
    IWR = "IWR"
    LIC = "LIC"
    MIM = "MIM"
    NLMR = "NLMR"
    UCS = "UCS"
    UHA = "UHA"
    UIO = "UIO"


class SmellEntry(object):
    def __init__(self, smell: SmellType, fields: List[str]):
        self.type = smell
        self.columns = self._get_fields(fields, smell)
        # print("[DEBUG][" + smell.name + "] Found columns:" + str(self.columns))
        # print("[DEBUG][" + smell.name + "] introduction col:" + str([header for header in self.columns[0::3]]))
        # print("[DEBUG][" + smell.name + "] refactoring col:" + str([header for header in self.columns[1::3]]))
        # print("[DEBUG][" + smell.name + "] deletion col:" + str([header for header in self.columns[2::3]]))
        self.introductions = 0
        self.refactoring = 0
        self.deletion = 0
        self.removal = 0

    @staticmethod
    def _get_fields(fields: List[str], smell: SmellType):
        return [field for field in fields if smell.name in field]

    def reset(self):
        self.introductions = 0
        self.refactoring = 0
        self.deletion = 0
        self.removal = 0

    def parse(self, line):
        """
        Fill the number of introduction, refactoring, deletion and removal in this CSV line.

        :param line: The line to analyze
        :return: None
        """
        self.introductions += sum(int(line[header]) for header in self.columns[0::3])
        self.refactoring += sum(int(line[header]) for header in self.columns[1::3])
        self.deletion += sum(int(line[header]) for header in self.columns[2::3])
        self.removal = self.refactoring + self.deletion


class CommitMergerCsvWriter(CsvOutputWriter):

    @staticmethod
    def get_int(smell: str):
        return "int_" + smell

    @staticmethod
    def get_ref(smell: str):
        return "ref_" + smell

    @staticmethod
    def get_del(smell: str):
        return "del_" + smell

    @staticmethod
    def get_rem(smell: str):
        return "rem_" + smell

    PROJECT = "project"
    SHA1 = "sha1"
    ADDITIONS = "additions"
    DELETIONS = "deletions"
    MESSAGE = "message"
    CATEGORIES = "categories"
    DISTANCE_RELEASE = "distance_to_release"
    DISTANCE_STARTUP = "distance_from_startup"

    @classmethod
    def get_headers(cls):
        smell_headers = [
            [cls.get_int(smell.name), cls.get_ref(smell.name), cls.get_del(smell.name), cls.get_rem(smell.name)] for
            smell in SmellType
        ]
        return (
            cls.PROJECT,
            cls.SHA1,
            cls.ADDITIONS,
            cls.DELETIONS,
            cls.CATEGORIES,
            *[header for headers in smell_headers for header in headers],
            cls.DISTANCE_RELEASE,
            cls.DISTANCE_STARTUP,
            cls.MESSAGE
        )

    def __init__(self, out_path):
        """Creates a CSV counting the number of commits introducing, refactoring, and deleting smells per project.

        :param out_path: The CSV file output path.
        """
        super().__init__(self.get_headers(), output_path=out_path)
        self.smells = {}

    def add_commit(self, project: str, sha1: str, additions: int, deletions: int,
                   message: str, categories: List[str],
                   distance_release: int, distance_startup: int,
                   smells: List[SmellEntry]):
        commit_line = {
            self.PROJECT: project,
            self.SHA1: sha1,
            self.ADDITIONS: additions,
            self.DELETIONS: deletions,
            self.CATEGORIES: categories,
            self.DISTANCE_RELEASE: distance_release,
            self.DISTANCE_STARTUP: distance_startup,
            self.MESSAGE: message
        }
        commit_line.update(self.build_dictionary(smells))
        self._add_line(commit_line)

    def build_dictionary(self, smells: List[SmellEntry]):
        smell_dictionary = {}
        for smell in smells:
            smell_name = smell.type.name
            smell_dictionary.update({
                self.get_int(smell_name): smell.introductions,
                self.get_ref(smell_name): smell.refactoring,
                self.get_del(smell_name): smell.deletion,
                self.get_rem(smell_name): smell.removal
            })
        return smell_dictionary


def reclassify(rq1_file: str, out_file: str):
    with open(rq1_file, "r") as input_f, open(out_file, 'w') as output:
        reader = csv.DictReader(input_f)
        writer = csv.DictWriter(output, reader.fieldnames)

        writer.writeheader()
        for line in reader:
            classification = CommitClassifier().classify(line["message"])
            line.update({"categories": classification})
            writer.writerow(line)


if __name__ == '__main__':
    in_file = "/data/Output-Commits-context-RQ1.csv.old"
    out_file = "/data/Output-Commits-context-RQ1.csv"
    reclassify(in_file, out_file)