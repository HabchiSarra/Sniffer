# coding=utf-8
from git import Repo

from analysis.computation import Analyzer, OutputType
from analysis.projects.output import CsvOutputWriter, OutputWriter


class ProjectAnalyzer(Analyzer):
    """
    Clone and analyze a project from any git url.
    """

    def __init__(self, repo: Repo, project: str, output_writer: OutputWriter = CsvOutputWriter()):
        """

        :param repo:  The git.Repo instance to work with
        """
        super().__init__(project, OutputType.CSV)
        self.output_writer = output_writer
        self.repo = repo

    def analyze(self, repo: Repo):
        """
        Analyze each commit of the repository and write the result
        using the given output_writer.

        :return: None
        """

        # TODO: Implement analysis
        self.output_writer.write()
