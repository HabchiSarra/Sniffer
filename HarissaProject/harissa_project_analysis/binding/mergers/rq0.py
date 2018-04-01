# coding=utf-8

import csv
import os

from harissa_project_analysis.analysis.output import CsvOutputWriter


class IRDBySmellCsvWriter(CsvOutputWriter):
    SMELL = "smell_type"
    INTRODUCTION = "introduction"
    REFACTORING = "refactoring"
    DELETION = "deletion"
    REMOVAL = "removal"
    HEADER = (
        SMELL,
        INTRODUCTION,
        REFACTORING,
        DELETION,
        REMOVAL
    )

    def __init__(self, out_path):
        """Creates a CSV counting the number of commits introducing, refactoring, and deleting smells per project.

        :param out_path: The CSV file output path.
        """
        super().__init__(self.HEADER, output_path=out_path)
        self.smells = {}

    def add_project_smell(self, smell_name: int, i: int, r: int, d: int):
        if smell_name not in self.smells:
            self.smells[smell_name] = (i, r, d)
        else:
            self.smells[smell_name] = tuple(sum(x) for x in zip((i, r, d), self.smells[smell_name]))

    def write(self):
        for k, v in self.smells.items():
            self._add_line({
                IRDBySmellCsvWriter.SMELL: k,
                IRDBySmellCsvWriter.INTRODUCTION: v[0],
                IRDBySmellCsvWriter.REFACTORING: v[1],
                IRDBySmellCsvWriter.DELETION: v[2],
                IRDBySmellCsvWriter.REMOVAL: v[1] + v[2]
            })
        super().write()


def merge_ird_by_smell(input_metrics: str):
    csv_writer = IRDBySmellCsvWriter("/data/Output-IRD-persmell.csv")
    for folder in [x for x in os.listdir(input_metrics) if os.path.isdir(os.path.join(input_metrics, x))]:
        file_path = os.path.join(input_metrics, folder, "metrics/metrics-perDev-perSmell.csv")
        with open(file_path, 'r') as smell_count_file:
            reader = csv.DictReader(smell_count_file)
            for line in reader:
                values = list(line.values())
                smell_name = values[0]
                nb_i = sum(int(x) for x in values[1::3])
                nb_r = sum(int(x) for x in values[2::3])
                nb_d = sum(int(x) for x in values[3::3])
                csv_writer.add_project_smell(smell_name, nb_i, nb_r, nb_d)
    csv_writer.write()
