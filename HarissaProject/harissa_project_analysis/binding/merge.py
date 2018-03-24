# coding=utf-8
import os, csv

from harissa_project_analysis.analysis.output import CsvOutputWriter


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


if __name__ == '__main__':
    metrics = "/data/tandoori-metrics/results"
    merge_commits_by_ird(metrics)
    # input_commits = "/data/tandoori-metrics/commits-analysis"
