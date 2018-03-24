# coding=utf-8
from typing import List

from harissa_project_analysis.analysis.output import CsvOutputWriter, OutputWriter

__all__ = ["CommitOutputWriter", "CsvCommitWriter"]


class CommitOutputWriter(OutputWriter):
    AUTHOR = "author"
    COMMITTER = "committer"
    SHA1 = "sha1"
    DATE = "date"
    CLASSIFICATIONS = "classifications"
    ADDITION = "addition"
    DELETION = "deletion"
    SIZE = "size"
    MESSAGE = "message"
    TAGS = "tags"

    def add_commit(self, author: str, committer: str, sha1: str, date: str,
                   classifications: List[str], addition: int, deletion: int, message: str,
                   tags: List[str]):
        """
        Add a commit to the final output.

        :param author: The registered Author.
        :param committer: The final committer.
        :param sha1: The SHA1 identifier.
        :param date: The commit creation date.
        :param classifications: The classification tags.
        :param addition: The number of added lines.
        :param deletion: The number of removed lines.
        :param message: The commit message.
        :param tags: The tags associated with this commit.
        :return:
        """
        raise NotImplementedError("Please Implement this method")


class CsvCommitWriter(CsvOutputWriter, CommitOutputWriter):
    HEADER = (
        CommitOutputWriter.AUTHOR,
        CommitOutputWriter.COMMITTER,
        CommitOutputWriter.SHA1,
        CommitOutputWriter.DATE,
        CommitOutputWriter.TAGS,
        CommitOutputWriter.CLASSIFICATIONS,
        CommitOutputWriter.ADDITION,
        CommitOutputWriter.DELETION,
        CommitOutputWriter.SIZE,
        CommitOutputWriter.MESSAGE
    )

    def __init__(self, output_path: str = None, separator: str = None):
        """

        :param output_path:
        :param separator:
        """
        CsvOutputWriter.__init__(self, self.HEADER, output_path, separator)
        CommitOutputWriter.__init__(self)

    def add_commit(self, author: str, committer: str, sha1: str, date: str,
                   classifications: List[str], addition: int, deletion: int, message: str,
                   tags: List[str]):
        commit_line = {
            CommitOutputWriter.AUTHOR: author,
            CommitOutputWriter.COMMITTER: committer,
            CommitOutputWriter.SHA1: sha1,
            CommitOutputWriter.DATE: date,
            CommitOutputWriter.CLASSIFICATIONS: classifications,
            CommitOutputWriter.ADDITION: str(addition),
            CommitOutputWriter.DELETION: str(deletion),
            CommitOutputWriter.SIZE: str(addition + deletion),
            CommitOutputWriter.MESSAGE: message,
            CommitOutputWriter.TAGS: tags
        }
        self._add_line(commit_line)
