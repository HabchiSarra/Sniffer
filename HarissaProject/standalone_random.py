# coding=utf-8
import csv
import random
from typing import List, Dict, Tuple, IO


class OutputWriter(object):
    def write(self):
        """
        Write the result of the operation.
        :return:
        """
        raise NotImplementedError("Please Implement this method")


class CsvOutputWriter(OutputWriter):
    def __init__(self,
                 header: Tuple[str, ...],
                 output_path: str = None,
                 separator: str = None
                 ):
        """

        :param output_path:
        :param separator:
        """
        super().__init__()
        if output_path is None:
            output_path = "./output.csv"
        if separator is None:
            separator = ','
        self.header = header
        self.output_path = output_path
        self.separator = separator
        self.lines = []

    def _add_line(self, line: Dict):
        self.lines.append(line)

    def write(self):
        print("Writing file: " + self.output_path)

        with open(self.output_path, "w") as output_file:
            output = csv.DictWriter(output_file, fieldnames=self.header, delimiter=self.separator)
            # print("[DEBUG] printing rows: " + str(self.lines))
            output.writeheader()
            output.writerows(self.lines)


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
        # We assume that we will encounter lots of 0 smells commits
        lines = self._generate_random_lines(nb_selection * 10, file_lines)
        with open(self._commits_file, 'r') as commits_file:
            reader = csv.DictReader(commits_file)
            self._set_columns_name(reader.fieldnames)
            while selected_smells < nb_selection:
                line_to_select = lines.pop()
                line = self.find_commit(commits_file, reader, line_to_select)
                if line is None:
                    print("[WARNING] No line n°" + str(line_to_select) + " found (doc. size: " + str(file_lines) + ")")
                    continue
                line.update({
                    "introduction": self._sum_columns(line, self.introduction_cols),
                    "deletion": self._sum_columns(line, self.deletion_cols),
                    "refactoring": self._sum_columns(line, self.refactoring_cols),
                })
                result.append(line)
                added_smells = self._sum_columns(line, self.removal_cols)
                selected_smells += added_smells
        return result

    @staticmethod
    def find_commit(file: IO, reader: csv.DictReader, line_number: int) -> Dict:
        file.seek(0)
        current_line = 0
        # print("looking for line: " + str(line_number))
        for line in reader:
            if current_line == line_number:
                return line
            current_line += 1

    @staticmethod
    def _count_lines(file: str):
        """Count the number of available lines in the given file

        :param file: The file to count from
        :return:
        """
        with open(file) as opened_file:
            reader = csv.DictReader(opened_file)
            file_lines = sum(1 for _ in reader)
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
        theoretical_smells = {}
        found_smells = {}
        # Retrieve the project's commit to bind
        for commit in selection:
            commit_sha = commit["sha1"]
            binding[commit_sha] = []
            theoretical_smells[commit_sha] = commit["deletion"] + commit["refactoring"]
            found_smells[commit_sha] = 0

        # Retrieve the right lines from our commit files
        with open(self._smells_removal_file, 'r') as smells_file:
            reader = csv.DictReader(smells_file)
            for line in reader:
                commit_sha = line["sha"]
                if commit_sha in binding:
                    found_smells[commit_sha] += 1
                    line.pop("project")
                    # line.pop("sha")
                    binding[commit_sha].append(line)

        # Check consistency
        for sha, theoretical_nb_smells in theoretical_smells.items():
            if theoretical_nb_smells != found_smells[sha]:
                print("[WARNING] Smells count not matching! (sha: " + sha
                      + ", exp. " + str(theoretical_nb_smells)
                      + ", act. " + str(found_smells[sha]) + ")")
                if found_smells[sha] > 0:
                    print("Smells sha: " + str(binding[sha][0]["sha"]))
                # print("line: " + str(binding[sha]))
            else:
                print("[DEBUG] Smells count matching! (sha: " + sha + ", count: " + str(theoretical_nb_smells) + ")")

        # Create a line per smell with commit and smell data
        result = []
        for commit in selection:
            for smell in binding[commit["sha1"]]:
                if commit["sha1"] != smell["sha"]:
                    print("[WARNING] Inconsistent sha in line: " + smell)
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
    nb_selection = 30  # 1600
    selector = RandomSelector(commits_file, smells_removal_file, apps_csv)
    selection = selector.select_random_smells(nb_selection)
    write_csv(selection, output_path)


def check_generation():
    generated_file = "/data/tandoori-metrics/commits-selection.csv"
    with open(generated_file, "r") as file:
        reader = csv.DictReader(file)
        smells = 0
        lines = 0
        shas = []
        nb_types = {}
        for line in reader:
            lines += 1
            # Count smells per type
            smell_type = line["smell_type"]
            if smell_type not in nb_types.keys():
                nb_types[smell_type] = 1
            else:
                nb_types[smell_type] += 1

            # Count smells per "removal" count
            if line["sha1"] not in shas:
                shas.append(line["sha1"])
                smells += int(line["commit_removal"])
    print(str(nb_types) + " -> " + str(sum([v for v in nb_types.values()])))
    print("lines: " + str(lines))
    print("Smells count: " + str(smells))
    print("Number of commits: " + str(len(shas)))


if __name__ == '__main__':
    generate_random_commits()
    check_generation()
