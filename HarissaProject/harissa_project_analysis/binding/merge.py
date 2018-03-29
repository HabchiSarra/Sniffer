# coding=utf-8
import csv
import os
from datetime import datetime
from enum import Enum
from typing import List, Dict

from dateutil import parser

from harissa_project_analysis.analysis.output import CsvOutputWriter


class IRDBySmellCsvWriter(CsvOutputWriter):
    SMELL = "smell_type"
    INTRODUCTION = "introduction"
    REFACTORING = "refactoring"
    DELETION = "deletion"
    REMOVAL = "removal"
    HEADER = (
        SMELL,
        INTRODUCTION,
        REFACTORING,
        DELETION,
        REMOVAL
    )

    def __init__(self, out_path):
        """Creates a CSV counting the number of commits introducing, refactoring, and deleting smells per project.

        :param out_path: The CSV file output path.
        """
        super().__init__(self.HEADER, output_path=out_path)
        self.smells = {}

    def add_project_smell(self, smell_name: int, i: int, r: int, d: int):
        if smell_name not in self.smells:
            self.smells[smell_name] = (i, r, d)
        else:
            self.smells[smell_name] = tuple(sum(x) for x in zip((i, r, d), self.smells[smell_name]))

    def write(self):
        for k, v in self.smells.items():
            self._add_line({
                IRDBySmellCsvWriter.SMELL: k,
                IRDBySmellCsvWriter.INTRODUCTION: v[0],
                IRDBySmellCsvWriter.REFACTORING: v[1],
                IRDBySmellCsvWriter.DELETION: v[2],
                IRDBySmellCsvWriter.REMOVAL: v[1] + v[2]
            })
        super().write()


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
        self.smell = smell
        self.columns = self._get_fields(fields, smell)
        self.introductions = 0
        self.refactoring = 0
        self.deletion = 0
        self.removal = 0

    @staticmethod
    def _get_fields(fields: List[str], smell: SmellType):
        return [field for field in fields if smell.name in field]

    def parse(self, line):
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
            smell_name = smell.smell.name
            smell_dictionary.update({
                self.get_int(smell_name): smell.introductions,
                self.get_ref(smell_name): smell.refactoring,
                self.get_del(smell_name): smell.deletion,
                self.get_rem(smell_name): smell.removal
            })
        return smell_dictionary


class CommitNumberIRDCsvWriter(CsvOutputWriter):
    PROJECT = "project"
    NUMBER_COMMITS = "number_commits"
    NUMBER_COMMITS_I = "number_commits_I"
    NUMBER_COMMITS_R = "number_commits_R"
    NUMBER_COMMITS_D = "number_commits_D"
    NUMBER_COMMITS_I_AND_D = "number_commits_introducing_and_removing"
    HEADER = (
        PROJECT, NUMBER_COMMITS, NUMBER_COMMITS_I, NUMBER_COMMITS_R, NUMBER_COMMITS_D, NUMBER_COMMITS_I_AND_D
    )

    def __init__(self, out_path):
        """Creates a CSV counting the number of commits introducing, refactoring, and deleting smells per project.

        :param out_path: The CSV file output path.
        """
        super().__init__(self.HEADER, output_path=out_path)

    def add_project(self, name: str, nb_commits: int, nb_commits_i: int, nb_commits_r: int, nb_commits_d: int,
                    nb_i_and_d: int):
        self._add_line({
            CommitNumberIRDCsvWriter.PROJECT: name,
            CommitNumberIRDCsvWriter.NUMBER_COMMITS_I: nb_commits_i,
            CommitNumberIRDCsvWriter.NUMBER_COMMITS_R: nb_commits_r,
            CommitNumberIRDCsvWriter.NUMBER_COMMITS_D: nb_commits_d,
            CommitNumberIRDCsvWriter.NUMBER_COMMITS_I_AND_D: nb_i_and_d,
            CommitNumberIRDCsvWriter.NUMBER_COMMITS: nb_commits
        })


def is_not_zero(x):
    """
    Tells if the CSV entry is not a 0.

    :param x: The entry to check.
    :return: If the entry is a simple string and not a '0'.
    """
    return isinstance(x, str) and x != '0'


def merge_commits_by_ird(input_metrics: str):
    """Count the number of project introducing, refactoring or deleting smells in each project.

    :param input_metrics: The directory containing projects smells outputs.
    :return: None.
    """
    csv_writer = CommitNumberIRDCsvWriter("/data/OutputIntro.csv")
    for folder in os.listdir(input_metrics):
        file_path = os.path.join(input_metrics, folder, "metrics/metrics-perDev-perCommit-perSmell.csv")
        with open(file_path, 'r') as input_file:
            nb_introduction = 0
            nb_refactoring = 0
            nb_deletion = 0
            nb_commits = 0
            # Remove is delete or refactor
            introduce_and_remove = 0
            reader = csv.DictReader(input_file)
            for line in reader:
                nb_commits += 1
                values = list(line.values())
                introduced = False
                removed = False
                if [x for x in values[3::3] if is_not_zero(x)]:
                    nb_introduction += 1
                    introduced = True
                if [x for x in values[4::3] if is_not_zero(x)]:
                    nb_refactoring += 1
                    removed = True
                if [x for x in values[5::3] if is_not_zero(x)]:
                    nb_deletion += 1
                    removed = True
                if introduced and removed:
                    introduce_and_remove += 1
            csv_writer.add_project(folder, nb_commits, nb_introduction, nb_refactoring, nb_deletion,
                                   introduce_and_remove)
    csv_writer.write()


def merge_ird_by_smell(input_metrics: str):
    csv_writer = IRDBySmellCsvWriter("/data/Output-IRD-persmell.csv")
    for folder in [x for x in os.listdir(input_metrics) if os.path.isdir(os.path.join(input_metrics, x))]:
        file_path = os.path.join(input_metrics, folder, "metrics/metrics-perDev-perSmell.csv")
        with open(file_path, 'r') as smell_count_file:
            reader = csv.DictReader(smell_count_file)
            for line in reader:
                values = list(line.values())
                smell_name = values[0]
                nb_i = sum(int(x) for x in values[1::3])
                nb_r = sum(int(x) for x in values[2::3])
                nb_d = sum(int(x) for x in values[3::3])
                csv_writer.add_project_smell(smell_name, nb_i, nb_r, nb_d)
    csv_writer.write()


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


def all_merges(metrics: str, commits: str):
    merge_commits_by_ird(metrics)
    merge_ird_by_smell(metrics)
    merge_commits_with_smells(metrics, commits)


if __name__ == '__main__':
    metrics_dir = "/data/tandoori-metrics/results"
    commits_dir = "/data/tandoori-metrics/commits-analysis"
    # input_commits = "/data/tandoori-metrics/commits-analysis"
    # merge_commits_by_ird(metrics)
    # merge_ird_by_smell(metrics)
    merge_commits_with_smells(metrics_dir, commits_dir)
