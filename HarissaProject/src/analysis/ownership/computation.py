# coding=utf-8
import operator

from git import Repo, Blob, Tree

from analysis.computation import Analyzer
from analysis.ownership.output import OwnershipOutputWriter, CsvOwnershipWriter

__all__ = ["OwnershipAnalyzer"]


class FileHandler(object):
    OWNERSHIP_THRESHOLD = 0.75
    """
    Analyze the ownership of the given file.
    """

    def __init__(self, output_writer: OwnershipOutputWriter, repo: Repo):
        self.repo = repo
        self.output_writer = output_writer

    def analyze(self, path: str):
        """
        This method will count the number of commits for each developer on a given file.
        Then say that a developer is the author if it has at most 75% the commits.
        :param path: The file path.
        :return:
        """
        log = list(self.repo.iter_commits(self.repo.active_branch, path))
        nb_commits = len(log)
        authors = self._count_authored_commits(log)
        # Retrieving author with maximum count of commits
        owner = max(authors.items(), key=operator.itemgetter(1))[0]
        ownership = authors[owner] / float(nb_commits)
        if ownership > self.OWNERSHIP_THRESHOLD:
            self.output_writer.add_owner(owner, path, ownership)
        else:
            # Useful to count ownership in the whole project
            self.output_writer.add_owner("", path, 0)

    @staticmethod
    def _count_authored_commits(log):
        authors = {}
        for commit in log:
            if commit.author not in authors:
                authors[commit.author] = 1
            else:
                authors[commit.author] += 1
        return authors


class OwnershipAnalyzer(Analyzer):
    """
    Analyze ownership on project's files.
    """

    def __init__(self, output_writer: OwnershipOutputWriter = CsvOwnershipWriter()):
        super().__init__(output_writer)

    def _process(self, repo: Repo):
        for file in repo.tree(repo.active_branch):
            file_handler = FileHandler(self.output_writer, repo)
            if isinstance(file, Blob):
                self._check(file, file_handler)
            if isinstance(file, Tree):
                self._walk(file, file_handler)

    def _walk(self, tree: Tree, file_handler: FileHandler):
        for file in tree.blobs:
            self._check(file, file_handler)
        for subtree in tree.trees:
            self._walk(subtree, file_handler)

    def _check(self, blob: Blob, file_handler: FileHandler):
        if self._is_source(blob):
            file_handler.analyze(blob.path)

    def _is_source(self, file: Blob):
        return file.name.endswith(".java")
