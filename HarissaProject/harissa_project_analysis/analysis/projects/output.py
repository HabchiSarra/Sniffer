# coding=utf-8

from harissa_project_analysis.analysis.output import CsvOutputWriter, OutputWriter

__all__ = ["ProjectOutputWriter", "CsvProjectWriter"]


class ProjectOutputWriter(OutputWriter):
    MEMBERS = "members"
    CONTRIBUTORS = "contributors"

    def add_project(self):
        """
        #TODO: Project entry definition
        :return:
        """
        raise NotImplementedError("Please Implement this method")


class CsvProjectWriter(CsvOutputWriter, ProjectOutputWriter):
    HEADER = (
        ProjectOutputWriter.MEMBERS,
        ProjectOutputWriter.CONTRIBUTORS
    )

    def __init__(self, output_path: str = None, separator: str = None):
        """

        :param output_path:
        :param separator:
        """
        CsvOutputWriter.__init__(self, self.HEADER, output_path, separator)
        ProjectOutputWriter.__init__(self)

    def add_project(self):
        commit_line = {
            # TODO: Project entry definition
        }
        self._add_line(commit_line)
