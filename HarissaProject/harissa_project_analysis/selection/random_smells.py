# coding=utf-8
import csv
from typing import List, Dict

import random

from harissa_project_analysis.analysis.output import CsvOutputWriter


class SmellSelectionCsvWriter(CsvOutputWriter):
    PROJECT = "project"
    COMMIT = "sha1"
    COMMIT_URL = "commit_url"
    COMMIT_MESSAGE = "commit_message"
    COMMIT_CLASSIFICATION = "commit_classification"
    COMMIT_INT = "commit_introduction"
    COMMIT_REF = "commit_refactoring"
    COMMIT_DEL = "commit_deletion"
    COMMIT_REM = "commit_removal"
    SMELL_INSTANCE = "smell_instance"
    SMELL_TYPE = "smell_type"
    SMELL_FILE = "smell_file"

    HEADER = (
        PROJECT,
        COMMIT,
        COMMIT_URL,
        COMMIT_CLASSIFICATION,
        COMMIT_INT,
        COMMIT_REF,
        COMMIT_DEL,
        COMMIT_REM,
        SMELL_INSTANCE,
        SMELL_TYPE,
        SMELL_FILE,
        COMMIT_MESSAGE
    )

    def __init__(self, output_path):
        super().__init__(self.HEADER, output_path)

    def add_smell(self, project: str, commit: str, classification: str,
                  introduction: int, refactoring: int, deletion: int,
                  instance: str, smell_type: str, smell_file: str, commit_message: str):
        self._add_line({
            self.PROJECT: project,
            self.COMMIT: commit,
            self.COMMIT_URL: "https://github.com/" + project + "/commit/" + commit,
            self.COMMIT_CLASSIFICATION: classification,
            self.COMMIT_INT: introduction,
            self.COMMIT_REF: refactoring,
            self.COMMIT_DEL: deletion,
            self.COMMIT_REM: refactoring + deletion,
            self.SMELL_INSTANCE: instance,
            self.SMELL_TYPE: smell_type,
            self.SMELL_FILE: smell_file,
            self.COMMIT_MESSAGE: commit_message
        })


def write_csv(smells_selection: List[Dict], output_path):
    writer = SmellSelectionCsvWriter(output_path)
    for smell in smells_selection:
        writer.add_smell(smell["project"], smell["sha1"], smell["categories"],
                         smell["introduction"], smell["refactoring"], smell["deletion"],
                         smell["instance"], smell["smell_type"], smell["file"], smell["message"])

    writer.write()


class RandomSelector(object):

    def __init__(self, commits_file: str, smells_removal_file: str, apps_csv: str):
        """Select randomly a number of smells from the removal in commits.

        :param commits_file: The csv file containing commits data (presented as in RQ1 output).
        :param smells_removal_file: The file referencing the smells ownership on removal.
        :param apps_csv: The applications name / uri binding in a csv file.
        """
        self._apps_csv = apps_csv
        self._smells_removal_file = smells_removal_file
        self._commits_file = commits_file

    def _set_columns_name(self, fieldnames):
        self.introduction_cols = [x for x in fieldnames if "int_" in x]
        self.refactoring_cols = [x for x in fieldnames if "ref_" in x]
        self.deletion_cols = [x for x in fieldnames if "del_" in x]
        self.removal_cols = [x for x in fieldnames if "rem_" in x]

    @staticmethod
    def _sum_columns(line: Dict, columns: List[str]):
        return sum([int(line[x]) for x in columns])

    def select_random_smells(self, nb_smells: int) -> List[Dict]:
        commits_selection = self._random_commits_selection(nb_smells)
        self._retrieve_projects_uri(commits_selection)
        smells_selection = self._enrich_with_smell(commits_selection)
        return smells_selection

    def _random_commits_selection(self, nb_selection: int) -> List[Dict]:
        """Randomly select commits in order to count nb_selection removed smells.

        :param nb_selection: The number of smell to select.
        :return: The selected lines.
        """
        result = []
        selected_smells = 0
        file_lines = self._count_lines(self._commits_file)
        lines = self._generate_random_lines(nb_selection, file_lines)
        current_line = 0
        with open(self._commits_file, 'r') as commits_file:
            reader = csv.DictReader(commits_file)
            self._set_columns_name(reader.fieldnames)
            for line in reader:
                if current_line in lines and selected_smells < nb_selection:
                    line.update({
                        "introduction": self._sum_columns(line, self.introduction_cols),
                        "deletion": self._sum_columns(line, self.deletion_cols),
                        "refactoring": self._sum_columns(line, self.refactoring_cols),
                    })
                    result.append(line)
                    selected_smells += self._sum_columns(line, self.removal_cols)
                current_line += 1
        return result

    @staticmethod
    def _count_lines(file: str):
        """Count the number of available lines in the given file

        :param file: The file to count from
        :return:
        """
        with open(file) as opened_file:
            file_lines = sum(1 for _ in opened_file)
        return file_lines

    @staticmethod
    def _generate_random_lines(nb_selection: int, upper_bound: int):
        return [random.randrange(0, stop=upper_bound) for _ in range(nb_selection)]

    def _enrich_with_smell(self, selection) -> List[Dict]:
        """Retrieve the smells definition from an ownership result file,
        then return a smell centered list of data commit selections with the smell informations.

        :param selection: The selected commits co
        :return: None.
        """
        binding = {}
        # Retrieve the project's commit to bind
        for commit in selection:
            binding[commit["sha1"]] = []

        # Retrieve the right lines from our commit files
        with open(self._smells_removal_file, 'r') as smells_file:
            reader = csv.DictReader(smells_file)
            for line in reader:
                commit_sha = line["sha"]
                if commit_sha in binding:
                    line.pop("project")
                    line.pop("sha")
                    binding[commit_sha].append(line)

        # Create a line per smell with commit and smell data
        result = []
        for commit in selection:
            for smell in binding[commit["sha1"]]:
                current_commit = commit
                current_commit.update(smell)
                result.append(current_commit)
        return result

    def _retrieve_projects_uri(self, commits_selection: List[Dict]):
        """Retrieve the GitHub projects' names linked to our local name,
        then update the "project" entry in each dictionary.

        :param commits_selection: The commits to update.
        :return: None.
        """
        binding = {}
        # Retrieve the project's name to bind
        for commit in commits_selection:
            binding[commit["project"]] = None

        # Actual binding project name / uri
        with open(self._apps_csv, 'r') as apps:
            reader = csv.DictReader(apps, fieldnames=["name", "uri"])
            for line in reader:
                project_name = line["name"]
                if project_name in binding.keys():
                    binding[project_name] = line["uri"]

        # Replace values from name to uri
        for commit in commits_selection:
            commit["project"] = binding[commit["project"]]


def generate_random_commits():
    commits_file = "/data/tandoori-metrics/rq1.csv"
    smells_removal_file = "/data/tandoori-metrics/ownership/ownership_removal.csv"
    apps_csv = "/data/tandoori-metrics/allApps.csv"
    output_path = "/data/tandoori-metrics/commits-selection.csv"
    nb_selection = 382
    selector = RandomSelector(commits_file, smells_removal_file, apps_csv)
    selection = selector.select_random_smells(nb_selection)
    write_csv(selection, output_path)


if __name__ == '__main__':
    generate_random_commits()
