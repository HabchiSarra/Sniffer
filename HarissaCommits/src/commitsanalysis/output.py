# coding=utf-8
from typing import List

import csv

__all__ = ["OutputWriter", "CsvOutputWriter"]


class OutputWriter(object):
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

    def __init__(self):
        pass

    def add_commit(self, author: str, committer: str, sha1: str, date: str,
                   classifications: List[str], addition: int, deletion: int, message: str,
                   tags: List[str]):
        """

        :return:
        """
        raise NotImplementedError("Please Implement this method")

    def write(self):
        """

        :return:
        """
        raise NotImplementedError("Please Implement this method")


class CsvOutputWriter(OutputWriter):
    HEADER = (
        OutputWriter.AUTHOR,
        OutputWriter.COMMITTER,
        OutputWriter.SHA1,
        OutputWriter.DATE,
        OutputWriter.TAGS,
        OutputWriter.CLASSIFICATIONS,
        OutputWriter.ADDITION,
        OutputWriter.DELETION,
        OutputWriter.SIZE,
        OutputWriter.MESSAGE
    )

    def __init__(self, output_path: str = "./output.csv", separator='ا'):  # That's an alif
        """

        :param output_path:
        :param separator:
        """
        super().__init__()
        self.output_path = output_path
        self.separator = separator
        self.lines = []

    def add_commit(self, author: str, committer: str, sha1: str, date: str,
                   classifications: List[str], addition: int, deletion: int, message: str,
                   tags: List[str]):
        commit_line = {
            OutputWriter.AUTHOR: author,
            OutputWriter.COMMITTER: committer,
            OutputWriter.SHA1: sha1,
            OutputWriter.DATE: date,
            OutputWriter.CLASSIFICATIONS: classifications,
            OutputWriter.ADDITION: str(addition),
            OutputWriter.DELETION: str(deletion),
            OutputWriter.SIZE: str(addition + deletion),
            OutputWriter.MESSAGE: message,
            OutputWriter.TAGS: tags
        }
        self.lines.append(commit_line)

    def write(self):
        print("Writing file: " + self.output_path)
        with open(self.output_path, "w") as output_file:
            output = csv.DictWriter(output_file, fieldnames=self.HEADER, delimiter=self.separator)
            # print("[DEBUG] printing rows: " + str(self.lines))
            output.writeheader()
            output.writerows(self.lines)
