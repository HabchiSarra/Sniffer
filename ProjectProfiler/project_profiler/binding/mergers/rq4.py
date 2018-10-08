# coding=utf-8
import csv
import glob
import os

from git import Repo

from project_profiler.analysis.ownership.computation import FileOwnershipHandler
from project_profiler.analysis.ownership.output import OwnershipOutputWriter
from project_profiler.binding.mergers.rq1 import SmellType, SmellEntry
from project_profiler.binding.smells import FileFinder
from project_profiler.analysis.output import CsvOutputWriter


class SmellOwnershipCsvWriter(CsvOutputWriter):
    PROJECT = "project"
    COMMIT_SHA = "sha"
    SMELL_INSTANCE = "instance"
    SMELL_FILE = "file"
    SMELL_TYPE = "smell_type"
    DEVELOPER = "developer"
    IS_OWNER = "is_owner"

    HEADER = (
        PROJECT,
        COMMIT_SHA,
        SMELL_INSTANCE,
        SMELL_FILE,
        SMELL_TYPE,
        DEVELOPER,
        IS_OWNER
    )

    def __init__(self, output_path: str):
        super().__init__(self.HEADER, output_path)

    def add_instance(self, project: str, sha: str, smell_type: str, instance: str, file: str,
                     dev: str, is_owner: bool):
        self._add_line({
            self.PROJECT: project,
            self.COMMIT_SHA: sha,
            self.SMELL_INSTANCE: instance,
            self.SMELL_FILE: file,
            self.SMELL_TYPE: smell_type,
            self.DEVELOPER: dev,
            self.IS_OWNER: 1 if is_owner else 0
        })


class AnalyzeOutputWriter(OwnershipOutputWriter):
    def __init__(self):
        self.owner = None

    def add_owner(self, name: str, file_path: str, ownership: float):
        self.owner = name


class SmellInstance(object):
    def __init__(self, instance: str, file: str, dev: str, is_owner: bool):
        self.instance = instance
        self.file = file
        self.dev = dev
        self.is_owner = is_owner

    @staticmethod
    def from_instances(instances: set, repo: Repo, commit: str) -> set:
        """

        :param instances:
        :param repo:
        :param commit:
        :return: a set of SmellInstance
        """
        result = set()
        tree = repo.tree(commit)
        writer = AnalyzeOutputWriter()
        ownership_handler = FileOwnershipHandler(writer, repo)
        # print("[DEBUG] Finding owner for smell:" + str(instances))
        for instance in instances:
            java_file = FileFinder.find(instance, tree)
            owner = None
            is_owner = None
            if java_file is not None:
                ownership_handler.analyze(java_file, commit)
                owner = writer.owner
                is_owner = owner is not None and str(owner) != ""
            # else:
            #     print("[" + repo.git_dir + "] Couldn't find file for smell: " + instance)
            result.add(SmellInstance(instance, java_file, owner, is_owner))
        return result


def retrieve_smell_instances(previous_sha: str, commit_sha: str, smell_file: str, repo: Repo):
    """
    Returns a Tuple containing the set of introduced smells, then the set or removed smells.
    :param previous_sha: The commit before our analysed commit.
    :param commit_sha: The analyzed commit.
    :param smell_file: The file to look into.
    :param repo: The project repository.
    :return: A Tuple like: (($introduced, ...), ($removed, ...))
    """
    with open(smell_file, 'r') as smells:
        reader = csv.DictReader(smells)
        if previous_sha is None:
            old_smells = set()
        else:
            smells.seek(0)
            old_smells = retrieve_smells(reader, previous_sha)
        smells.seek(0)
        new_smells = retrieve_smells(reader, commit_sha)

        introduced = SmellInstance.from_instances(new_smells.difference(old_smells), repo, commit_sha)
        removed = SmellInstance.from_instances(old_smells.difference(new_smells), repo, previous_sha)
        # print("[DEBUG] Old (" + (previous_sha if previous_sha is not None else "None") + "): " + str(old_smells))
        # print("[DEBUG] New (" + commit_sha + "): " + str(new_smells))
        # print("[DEBUG] Introduced: " + str([x.instance for x in introduced]))
        # print("[DEBUG] Removed: " + str([x.instance for x in removed]))
        return introduced, removed


def retrieve_smells(reader: csv.DictReader, commit_sha: str):
    """
    Retrieve the smells present for the given commit.
    :param reader:  a CSV reader, with fields: ['commit_number', 'key', 'instance', 'commit_status', 'id']
    :param commit_sha: The commit to analyze
    :return:
    """
    #
    result = set()
    found = False
    for line in reader:
        if line['key'] == commit_sha:
            found = True
            result.add(line['instance'])
        elif found:
            # We already found our commit, and they are grouped together
            break
    return result


def retrieve_smell_file(metrics_dir: str, project: str, smell_type: str):
    return glob.glob(os.path.join(metrics_dir, project, "smells", "*" + smell_type + ".csv"))[0]


def _find_previous_commit(commit_sha: str, logs_file: str) -> str:
    result = None
    with open(logs_file, 'r') as logs:
        for commit in logs:
            current = commit.strip()
            if current == commit_sha:
                return result
            result = current
    return None


def merge_ownership_for_ird(metrics: str, repos: str, logs_dir: str, output_path: str):
    out_introduction = SmellOwnershipCsvWriter(os.path.join(output_path, "ownership_introductions.csv"))
    out_refactor = SmellOwnershipCsvWriter(os.path.join(output_path, "ownership_refactor.csv"))
    out_deletion = SmellOwnershipCsvWriter(os.path.join(output_path, "ownership_deletion.csv"))
    out_removal = SmellOwnershipCsvWriter(os.path.join(output_path, "ownership_removal.csv"))
    out_unsure = SmellOwnershipCsvWriter(os.path.join(output_path, "ownership_removal_unsure.csv"))

    for project in [x for x in os.listdir(metrics) if os.path.isdir(os.path.join(metrics, x))]:
        file_path = os.path.join(metrics, project, "metrics/metrics-perDev-perCommit-perSmell.csv")
        repo = Repo(os.path.join(repos, project))
        logs_file = os.path.join(logs_dir, project + ".logs")
        print("[" + project + "] Starting Analysis")

        with open(file_path, 'r') as smell_count_file:
            reader = csv.DictReader(smell_count_file)
            smell_entries = [SmellEntry(smell, reader.fieldnames) for smell in SmellType]

            for line in reader:
                commit_sha = line["sha"]
                previous_sha = _find_previous_commit(commit_sha, logs_file)

                # For each smell type
                for smell in smell_entries:
                    smell.reset()
                    smell.parse(line)
                    smell_type = smell.type.name
                    smell_file = retrieve_smell_file(metrics, project, smell_type)

                    # If we have any modification
                    if smell.introductions > 0 or smell.removal > 0:
                        # print("[" + project + "] Parsing (" + commit_sha + ") "
                        #       + str(smell.introductions) + " introductions and "
                        #       + str(smell.removal) + " removals (" + smell.type.name + ")")
                        smell_instances = retrieve_smell_instances(previous_sha, commit_sha, smell_file, repo)

                        # We state the introduced smells
                        for instance in smell_instances[0]:
                            out_introduction.add_instance(project, commit_sha, smell_type,
                                                          instance.instance, instance.file, instance.dev,
                                                          instance.is_owner)
                        # We state the removed smells
                        for instance in smell_instances[1]:
                            out_removal.add_instance(project, commit_sha, smell_type,
                                                     instance.instance, instance.file, instance.dev,
                                                     instance.is_owner)
                            if not smell.refactoring:
                                out_deletion.add_instance(project, commit_sha, smell_type,
                                                          instance.instance, instance.file, instance.dev,
                                                          instance.is_owner)
                            if not smell.deletion:
                                out_refactor.add_instance(project, commit_sha, smell_type,
                                                          instance.instance, instance.file, instance.dev,
                                                          instance.is_owner)
                            if smell.refactoring and smell.deletion:
                                out_unsure.add_instance(project, commit_sha, smell_type,
                                                        instance.instance, instance.file, instance.dev,
                                                        instance.is_owner)
                previous_sha = commit_sha
        print("[" + project + "] Ending Analysis")

    out_introduction.write()
    out_refactor.write()
    out_deletion.write()
    out_removal.write()
    out_unsure.write()
