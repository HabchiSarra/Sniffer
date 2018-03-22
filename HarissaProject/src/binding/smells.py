# coding=utf-8
import csv
import os
from multiprocessing import Pool
from typing import Dict

from git import Repo, Tree

from analysis.output import CsvOutputWriter
from analysis.ownership.computation import FileOwnershipHandler
from analysis.ownership.output import OwnershipOutputWriter


class SmellOwnershipWriter(OwnershipOutputWriter, CsvOutputWriter):
    SHA1 = "sha1"
    AUTHOR = "author"
    SMELL = "smell"
    FILE = "file"
    IS_OWNER = "isOwner"

    HEADER = (
        SHA1,
        AUTHOR,
        SMELL,
        FILE,
        IS_OWNER
    )

    def __init__(self, output_path: str = None):
        OwnershipOutputWriter.__init__(self)
        CsvOutputWriter.__init__(self, self.HEADER, output_path=output_path)
        self.owners = {}

    def add_owner(self, name: str, file_path: str, ownership: float):
        if name and ownership:
            self.owners[file_path] = (name, ownership)

    def add_smell_ownership(self, sha1: str, smell_instance: str, file_path: str, author: str):
        self._add_line({
            self.SHA1: sha1,
            self.IS_OWNER: file_path in self.owners,
            self.FILE: file_path,
            self.SMELL: smell_instance,
            self.AUTHOR: author
        })


class FileFinder(object):
    BINDING = {}

    @staticmethod
    def find(smell_instance: str, lookup_path: Tree):
        if smell_instance in FileFinder.BINDING:
            return FileFinder.BINDING[smell_instance]
        file_path = FileFinder._infer_java_file(smell_instance)
        path = FileFinder._find_java_file_path(file_path, lookup_path)
        FileFinder.BINDING[smell_instance] = path
        return path

    @staticmethod
    def _infer_java_file(smell_instance):
        """
        Retrieve the java file name from a smell instance,
        removing the method call (#) and subclass ($), then transforming classpath into path ('.' to '/')

        :param smell_instance: The smell instance denomination.
        :return: The java file containing the instance.
        """
        # Removing the method call (#) and subclass ($), then transforming classpath into path
        return smell_instance.split("#")[-1].split("$")[0].replace(".", "/")

    @staticmethod
    def _find_java_file_path(file_path: str, lookup_path: Tree):
        found_file = None
        for directory in lookup_path.trees:
            found_file = FileFinder._find_java_file_path(file_path, directory)
            if found_file is not None:
                return found_file
        for file in lookup_path.blobs:
            if file_path in file.path:
                return file.path
        return found_file


def analyze_project_smells(project: str):
    print("Analyzing project: " + project)
    writer = SmellOwnershipWriter(os.path.join(project, "smell-commit-ownership.csv"))
    repo = Repo(os.path.join(repo_dir, project))
    handler = FileOwnershipHandler(writer, repo)
    analyzer = SmellsAnalyzer(handler, repo, writer)
    analyzer.analyze_smells_ownership(os.path.join(input_dir, project, "smells"))
    writer.write()


class SmellsAnalyzer(object):
    def __init__(self, analyzer: FileOwnershipHandler, repo: Repo, writer: SmellOwnershipWriter):
        """

        :param writer:
        :param writer:
        :param analyzer: The smell analyzer set on the right repository.
        """
        self.analyzer = analyzer
        self.repo = repo
        self.writer = writer

    def analyze_smells_ownership(self, smell_dir: str):
        """
        Walk through smell files and start analysis on each of them.
        :param smell_dir: The directory containing smells files.
        :return: None
        """
        for _, _, files in os.walk(smell_dir):
            for smell_file in files:
                print("[" + os.path.basename(smell_dir) + "] Analyzing smell file: " + smell_file)
                self._analyze_smell_file(os.path.join(smell_dir, smell_file))

    def _analyze_smell_file(self, smell_file: str):
        """
        Analyze a file of smells.
        :param smell_file: Path of the file to analyze.
        :return: None
        """
        with open(smell_file, "r") as smells:
            smells_csv = csv.DictReader(smells, ["commit_number", "key", "instance", "commit_status", "id"])
            next(smells_csv, None)  # Skip header
            for smell in smells_csv:
                self._analyze_smell_instance(smell)

    def _analyze_smell_instance(self, smell: Dict):
        """
        Start analysis on a specific smell instance.
        :param smell: The smell instance name.
        :return: None
        """
        commit = smell["key"]
        smell_instance = smell["instance"]
        # print("[DEBUG] Analyzing smell: " + smell_instance + " - commit: " + commit)
        java_file = FileFinder.find(smell_instance, self.repo.tree(commit))
        if java_file is None:
            print("[WARNING] We couldn't find a satisfactory file on commit ("
                  + commit + ") for smell: " + smell_instance)
        else:
            self.analyzer.analyze(java_file, commit)
            author = self.repo.commit(commit).author
            self.writer.add_smell_ownership(commit, smell_instance, java_file, author)


if __name__ == '__main__':
    # TODO: [args] set input dir arg
    # TODO: [args] set output dir arg
    # TODO: [args] set number of threads

    input_dir = "/data/tandoori-metrics/one-result"
    repo_dir = "/data/tandoori-repos"
    thread_count = 3

    with Pool(thread_count) as p:
        p.map(analyze_project_smells, os.listdir(input_dir))
    print("Done!")
