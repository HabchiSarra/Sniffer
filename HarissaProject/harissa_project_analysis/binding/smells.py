# coding=utf-8
import csv
import os
from multiprocessing import Pool
from typing import Dict

from git import Repo, Tree

from harissa_project_analysis.analysis.output import CsvOutputWriter
from harissa_project_analysis.analysis.ownership.computation import FileOwnershipHandler
from harissa_project_analysis.analysis.ownership.output import OwnershipOutputWriter

__all__ = ["OwnershipProcessing"]


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
        """
        Retrieve the compilation unit linked to the smell instance.
        We may not find any file in the case of a package protected class
        sharing a compilation unit with a public class.

        :param smell_instance: The smell instance definition.
        :param lookup_path: The path to look into for the file.
        :return: The file if found, None if nothing matches.
        """
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


class SmellsAnalyzer(object):
    def __init__(self, analyzer: FileOwnershipHandler, repo: Repo, writer: SmellOwnershipWriter):
        """
        Analyze all the smells of a given repository.
        :param analyzer: The smell analyzer set on the right repository.
        :param repo: The git repository in which smells are located.
        :param writer: The output on which results should be written.
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


class OwnershipProcessing(object):
    def __init__(self, input_dir: str, repo_dir: str,
                 thread_count: int, output_dir: str = "./output"):
        """
        Creates a smell binder to check the ownership of
        :param input_dir: Projects results directory containing smells.
        :param repo_dir: Projects git repositories under the same name as in 'input_dir'.
        :param thread_count: The number of available threads.
        :param output_dir: The output for all files per project.
        """
        self.output_dir = output_dir
        self.input_dir = input_dir
        self.repo_dir = repo_dir
        self.thread_count = thread_count

    def process(self):
        with Pool(self.thread_count) as p:
            p.map(self.analyze_project_smells, os.listdir(self.input_dir))
        print("Binding Done!")

    def analyze_project_smells(self, project: str):
        print("Analyzing project: " + project)
        writer = SmellOwnershipWriter(self._output_file(project))
        repo = Repo(os.path.join(self.repo_dir, project))
        handler = FileOwnershipHandler(writer, repo)
        analyzer = SmellsAnalyzer(handler, repo, writer)
        analyzer.analyze_smells_ownership(self._smells_dir(project))
        writer.write()

    def _output_file(self, project: str):
        return os.path.join(self.output_dir, project + "-smell-commit-ownership.csv")

    def _smells_dir(self, project: str):
        return os.path.join(self.input_dir, project, "smells")
