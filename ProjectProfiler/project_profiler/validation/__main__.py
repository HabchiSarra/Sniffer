import csv

import requests
from requests import HTTPError

from harissa_project_analysis.analysis.output import CsvOutputWriter


class ValidationOutputWriter:
    ID = "id"
    PROJECT_URL = "project_url"
    SHA1 = "sha1"
    FILE = "file"
    COMMIT_URL = "commit_url"
    INSTANCE = "instance"
    FILE_LINK = "file_link"
    VALID_SMELL = "Valid smell ?"
    COMMENT = "comment"

    def add_entry(self, entry: dict):
        raise NotImplementedError()


class UHAValidationOutputWriter(CsvOutputWriter, ValidationOutputWriter):
    # New columns
    DRAWPATH = "drawPath"
    DRAWPICTURE = "drawPicture"
    DRAWVERTICES = "drawVertices"
    DRAWPOSTEXT = "drawPosText"
    DRAWTEXTONPATH = "drawTextOnPath"
    SETLINEARTEXT = "setLinearText"
    SETMASKFILTER = "setMaskFilter"
    SETPATHEFFECT = "setPathEffect"
    SETRASTERIZER = "setRasterizer"
    SETSUBPIXELTEXT = "setSubpixelText"
    VIEW = "extends View"

    HEADER = (
        ValidationOutputWriter.ID,
        ValidationOutputWriter.PROJECT_URL,
        ValidationOutputWriter.SHA1,
        ValidationOutputWriter.FILE,
        ValidationOutputWriter.COMMIT_URL,
        ValidationOutputWriter.INSTANCE,
        ValidationOutputWriter.FILE_LINK,
        ValidationOutputWriter.VALID_SMELL,
        DRAWPATH,
        DRAWPICTURE,
        DRAWVERTICES,
        DRAWPOSTEXT,
        DRAWTEXTONPATH,
        SETLINEARTEXT,
        SETMASKFILTER,
        SETPATHEFFECT,
        SETRASTERIZER,
        SETSUBPIXELTEXT,
        VIEW,
        ValidationOutputWriter.COMMENT
    )

    def __init__(self, output_path: str = None):
        CsvOutputWriter.__init__(self, self.HEADER, output_path=output_path)
        self.owners = {}

    @property
    def methods_to_check(self):
        return [
            self.DRAWPATH,
            self.DRAWPICTURE,
            self.DRAWVERTICES,
            self.DRAWPOSTEXT,
            self.DRAWTEXTONPATH,
            self.SETLINEARTEXT,
            self.SETMASKFILTER,
            self.SETPATHEFFECT,
            self.SETRASTERIZER,
            self.SETSUBPIXELTEXT,
            self.VIEW
        ]

    def add_entry(self, entry: dict):
        self._add_line(entry)


class NLMRValidationOutputWriter(CsvOutputWriter):
    FILE_RENAMING = "File renaming happening"
    VALID_RENAMING = "Valid renaming ?"

    # New columns
    TRIM_MEMORY = "onTrimMemory"
    CACHE = "cache"

    HEADER = (
        ValidationOutputWriter.ID,
        ValidationOutputWriter.PROJECT_URL,
        ValidationOutputWriter.SHA1,
        ValidationOutputWriter.COMMIT_URL,
        ValidationOutputWriter.FILE,
        ValidationOutputWriter.INSTANCE,
        ValidationOutputWriter.FILE_LINK,
        ValidationOutputWriter.VALID_SMELL,
        FILE_RENAMING,
        VALID_RENAMING,
        TRIM_MEMORY,
        CACHE,
        ValidationOutputWriter.COMMENT
    )

    def __init__(self, output_path: str = None):
        CsvOutputWriter.__init__(self, self.HEADER, output_path=output_path)

    @property
    def methods_to_check(self):
        return [
            self.CACHE,
            self.TRIM_MEMORY
        ]

    def add_entry(self, entry: dict):
        self._add_line(entry)


def fetch_raw_java(url):
    # Fetching raw content.
    url = url \
        .replace("github.com", "raw.githubusercontent.com") \
        .replace("/blob/", "/").replace("/tree/", "/")
    print(f"Fetching file {url}...")
    result = requests.get(url)
    result.raise_for_status()
    return result.text


def main():
    csv_path = "/tmp/validations_nlmr.csv"
    output_csv = "/tmp/validation_out.csv"
    output_writer = NLMRValidationOutputWriter(output_path=output_csv)
    with open(csv_path, 'r') as validation_file:
        tags = csv.DictReader(validation_file)
        for entry in tags:
            url = entry[ValidationOutputWriter.FILE_LINK]
            try:
                java_class = fetch_raw_java(url)
            except HTTPError as exception:
                print(f"Unable to fetch file: {url} ({exception})")
                continue
            else:
                for method in output_writer.methods_to_check:
                    entry[method] = 1 if method in java_class else 0
                # entry.pop("View")  # We replaced it by 'extends View'
                output_writer.add_entry(entry)
        output_writer.write()


if __name__ == '__main__':
    main()
