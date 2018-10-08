# coding=utf-8
import shutil
import tempfile
import time
from enum import Enum, auto
from typing import List

from git import Repo

from project_profiler.analysis.output import OutputWriter

__all__ = ["LocalProjectHandler", "RemoteProjectHandler", "ProjectHandler",
           "Analyzer", "OutputType"]


class OutputType(Enum):
    CSV = auto


class Analyzer(object):
    """
    Interface defining the project analyzers methods.
    """

    def __init__(self, output_writer: OutputWriter):
        self.output_writer = output_writer

    def analyze(self, repo: Repo):
        """
        Run the analysis on the given repository.
        :return: None
        """
        self._process(repo)
        self.output_writer.write()

    def _process(self, repo: Repo):
        """
        Actual analysis implementation.
        :return: None
        """
        raise NotImplementedError("Please Implement the analysis method")


class ProjectHandler(object):
    """
    Handle project analysis which will run the added analyzers onto the given project.
    """

    def __init__(self, repo: Repo, analyzers: List[Analyzer] = None):
        """
        Create a new project handler.
        :param repo: git.Repo object to process analysis onto.
        :param analyzers: List of analyzers to launch on the given project.
        """
        if analyzers is None:
            analyzers = []
        self.repo = repo
        self.analyzers = analyzers

    def add_analyzer(self, analyzer: Analyzer):
        self.analyzers.append(analyzer)

    def run(self):
        """
        Run project analysis
        :return: None
        """
        for analyzer in self.analyzers:
            analyzer.analyze(self.repo)


def generate_temp_dir():
    """
    Generate a temporary directory.

    :return: The created directory path.
    """
    return tempfile.mkdtemp(prefix="projectProfiler", suffix=str(time.time()))


class RemoteProjectHandler(ProjectHandler):
    """
    Handle a remote project by cloning it, then analyzing it.
    """

    def __init__(self, repo_url: str,
                 working_dir: str = None,
                 analyzers: List[Analyzer] = None):
        # Making the default parameter mutable...
        if working_dir is None:
            working_dir = generate_temp_dir()
        self.working_dir = working_dir
        repo = Repo().clone_from(repo_url, working_dir)
        super().__init__(repo, analyzers)

    def __del__(self):
        """
        Remove the working directory on object destruction.
        :return: None
        """
        to_remove = self.working_dir
        print("Removing dir: " + to_remove)
        shutil.rmtree(to_remove)


class LocalProjectHandler(ProjectHandler):
    """
    Handle a project present locally on the system.
    """

    def __init__(self, repo_path: str):
        super().__init__(Repo(repo_path))
