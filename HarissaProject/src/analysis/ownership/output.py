# coding=utf-8
import csv
from typing import Dict

from analysis.output import CsvOutputWriter, OutputWriter

__all__ = ["OwnershipOutputWriter", "CsvOwnershipWriter"]


class OwnershipOutputWriter(OutputWriter):
    FILE = "file"
    OWNER = "owner"
    PERCENTAGE = "percentage"

    def add_owner(self, name: str, file_path: str, ownership: float):
        """
        Add a file with a specified owner.

        :param name: The owner name.
        :param file_path: The file path.
        :param ownership: The ownership percentage.
        :return: None
        """
        raise NotImplementedError("Please Implement this method")


class CsvOwnershipWriter(CsvOutputWriter, OwnershipOutputWriter):
    HEADER = (
        OwnershipOutputWriter.FILE,
        OwnershipOutputWriter.OWNER,
        OwnershipOutputWriter.PERCENTAGE
    )

    def __init__(self, output_path: str = None, separator: str = None):
        """

        :param output_path:
        :param separator:
        """
        CsvOutputWriter.__init__(self, self.HEADER, output_path, separator)
        OwnershipOutputWriter.__init__(self)

    def add_owner(self, name: str, file_path: str, ownership: float):
        self._add_line({
            OwnershipOutputWriter.FILE: file_path,
            OwnershipOutputWriter.OWNER: name,
            OwnershipOutputWriter.PERCENTAGE: ownership
        })

    def _add_line(self, line: Dict):
        self.lines.append(line)

    def write(self):
        print("Writing file: " + self.output_path)

        with open(self.output_path, "w") as output_file:
            output = csv.DictWriter(output_file, fieldnames=self.header, delimiter=self.separator)
            # print("[DEBUG] printing rows: " + str(self.lines))
            output.writeheader()
            output.writerows(self.lines)
