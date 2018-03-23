# coding=utf-8
import csv
import os
from multiprocessing.pool import Pool

from analysis.commits.computation import CommitsAnalyzer
from analysis.commits.output import CsvCommitWriter
from analysis.computation import RemoteProjectHandler, LocalProjectHandler, ProjectHandler
from analysis.ownership.computation import OwnershipAnalyzer
from analysis.ownership.output import CsvOwnershipWriter


class AnalysisProcessing(object):
    def __init__(self, apps: csv.DictReader,
                 thread_count: int, local_repo_dir: str = None, output_dir: str = "./output"):
        """
        Start the analysis process, with the given configuration.

        :param apps: The dictionary of applications to process, containing the 'name' and github 'uri'
        :param thread_count: The number of threads to use in the computation.
        :param local_repo_dir: The directory where repositories are already cloned, if available.
        If this option is set to None, the analyzer will automatically clone the repository in a temporary directory.
        """
        self.output_dir = output_dir
        self.thread_count = thread_count

        # Choosing analysis origin
        if local_repo_dir is not None:
            self.method = self.analyze_local
            self.arguments = [os.path.join(local_repo_dir, x) for x in os.listdir(local_repo_dir) if
                              os.path.isdir(os.path.join(local_repo_dir, x))]
        else:
            self.method = self.analyze_remote
            self.arguments = apps

    def process(self):
        with Pool(self.thread_count) as p:
            p.map(self.method, self.arguments)

    def analyze_remote(self, app):
        """
        Analyze the remote repository, available on github, defined by a dictionary entry.
        :param app: A Dict containing the fields:
         - "uri": The user/repository available on GitHub.
         - "name": The project name to use in logging and output.
        :return: None
        """
        app_name = app["name"]
        app_url = "https://github.com/" + app["uri"]
        print("Handling project: " + app_name + " - " + app_url)
        self._analyze(app_name, RemoteProjectHandler(app_url))

    def analyze_local(self, repo: str):
        """
        Analyze a local repository from its path.

        :param repo: Path to the local repository.
        :return: None
        """
        print("Handling local project: " + repo)
        self._analyze(repo.split("/")[-1], LocalProjectHandler(repo))

    def _analyze(self, app_name: str, handler: ProjectHandler):
        # TODO: There must be a better way of assigning those writers.
        commit_writer = CsvCommitWriter(self._output_file("commits-" + app_name + ".csv"))
        # project_writer = CsvProjectWriter(self._output_file("project-" + app_name + ".csv"))
        ownership_writer = CsvOwnershipWriter(self._output_file("ownership-" + app_name + ".csv"))
        handler.add_analyzer(CommitsAnalyzer(commit_writer))
        # handler.add_analyzer(ProjectAnalyzer(project_writer))
        handler.add_analyzer(OwnershipAnalyzer(ownership_writer))
        handler.run()
        del handler

    def _output_file(self, file: str):
        return os.path.join(self.output_dir, file)
