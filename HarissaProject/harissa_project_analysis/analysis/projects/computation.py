# coding=utf-8
from git import Repo

from harissa_project_analysis.analysis.computation import Analyzer
from harissa_project_analysis.analysis.projects.output import CsvProjectWriter, ProjectOutputWriter

__all__ = ["ProjectAnalyzer"]


class ProjectAnalyzer(Analyzer):
    """
    Analyze a project's developers and miscellaneous indices.
    """

    def __init__(self, output_writer: ProjectOutputWriter = CsvProjectWriter()):
        super().__init__(output_writer)

    def _process(self, repo: Repo):
        # TODO: Implement analysis
        pass
