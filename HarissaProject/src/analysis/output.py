# coding=utf-8
import csv
from typing import Tuple, Dict

__all__ = ["CsvOutputWriter"]


class OutputWriter(object):
    def write(self):
        """
        Write the result of the operation.
        :return:
        """
        raise NotImplementedError("Please Implement this method")


class CsvOutputWriter(OutputWriter):
    def __init__(self,
                 header: Tuple[str, ...],
                 output_path: str = None,
                 separator: str = None
                 ):
        """

        :param output_path:
        :param separator:
        """
        super().__init__()
        if output_path is None:
            output_path = "./output.csv"
        if separator is None:
            separator = 'ا'  # That's an alif
        self.header = header
        self.output_path = output_path
        self.separator = separator
        self.lines = []

    def _add_line(self, line: Dict):
        self.lines.append(line)

    def write(self):
        print("Writing file: " + self.output_path)

        with open(self.output_path, "w") as output_file:
            output = csv.DictWriter(output_file, fieldnames=self.header, delimiter=self.separator)
            # print("[DEBUG] printing rows: " + str(self.lines))
            output.writeheader()
            output.writerows(self.lines)
