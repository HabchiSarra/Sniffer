# coding=utf-8
import csv
import os

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


class CommitNumberIRDCsvWriter(CsvOutputWriter):
    PROJECT = "project"
    NUMBER_COMMITS = "number_commits"
    NUMBER_COMMITS_I = "number_commits_I"
    NUMBER_COMMITS_R = "number_commits_R"
    NUMBER_COMMITS_D = "number_commits_D"
    HEADER = (
        PROJECT, NUMBER_COMMITS, NUMBER_COMMITS_I, NUMBER_COMMITS_R, NUMBER_COMMITS_D
    )

    def __init__(self, out_path):
        """Creates a CSV counting the number of commits introducing, refactoring, and deleting smells per project.

        :param out_path: The CSV file output path.
        """
        super().__init__(self.HEADER, output_path=out_path)

    def add_project(self, name: str, nb_commits: int, nb_commits_i: int, nb_commits_r: int, nb_commits_d: int):
        self._add_line({
            CommitNumberIRDCsvWriter.PROJECT: name,
            CommitNumberIRDCsvWriter.NUMBER_COMMITS_I: nb_commits_i,
            CommitNumberIRDCsvWriter.NUMBER_COMMITS_R: nb_commits_r,
            CommitNumberIRDCsvWriter.NUMBER_COMMITS_D: nb_commits_d,
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
    csv_writer = CommitNumberIRDCsvWriter("/data/OutputRQ1.csv")
    for folder in os.listdir(input_metrics):
        file_path = os.path.join(input_metrics, folder, "metrics/metrics-perDev-perCommit-perSmell.csv")
        with open(file_path, 'r') as input_file:
            nb_introduction = 0
            nb_refactoring = 0
            nb_deletion = 0
            nb_commits = 0
            reader = csv.DictReader(input_file)
            for line in reader:
                nb_commits += 1
                values = list(line.values())
                if [x for x in values[3::3] if is_not_zero(x)]:
                    nb_introduction += 1
                if [x for x in values[4::3] if is_not_zero(x)]:
                    nb_refactoring += 1
                if [x for x in values[5::3] if is_not_zero(x)]:
                    nb_deletion += 1
            csv_writer.add_project(folder, nb_commits, nb_introduction, nb_refactoring, nb_deletion)
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


if __name__ == '__main__':
    metrics = "/data/tandoori-metrics/results"
    # merge_commits_by_ird(metrics)
    merge_ird_by_smell(metrics)
    # input_commits = "/data/tandoori-metrics/commits-analysis"
