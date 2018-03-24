# coding=utf-8
import operator

from git import Repo, Blob, Tree, Reference

from harissa_project_analysis.analysis.computation import Analyzer
from harissa_project_analysis.analysis.ownership.output import OwnershipOutputWriter, CsvOwnershipWriter

__all__ = ["OwnershipAnalyzer", "FileOwnershipHandler"]


class FileOwnershipHandler(object):
    OWNERSHIP_THRESHOLD = 0.75
    """
    Analyze the ownership of the given file.
    """

    def __init__(self, output_writer: OwnershipOutputWriter, repo: Repo):
        """
        Analyze smells location in the given repository, for the given revision.

        :param output_writer: The output on which results should be written.
        :param repo: The git repository in which smells are located.
        """
        self.repo = repo
        self.output_writer = output_writer

    def analyze(self, path: str, revision: Reference):
        """
        This method will count the number of commits for each developer on a given file.
        Then say that a developer is the author if it has at most 75% the commits.
        :param path: The file path.
        :param revision: The git revision in which the smell should be looked for.
        :return:
        """
        log = list(self.repo.iter_commits(revision, path))
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
        revision = repo.active_branch
        for file in repo.tree(revision):
            file_handler = FileOwnershipHandler(self.output_writer, repo)
            if isinstance(file, Blob):
                self._check(file, file_handler, revision)
            if isinstance(file, Tree):
                self._walk(file, file_handler, revision)

    def _walk(self, tree: Tree, file_handler: FileOwnershipHandler, revision: Reference):
        for file in tree.blobs:
            self._check(file, file_handler, revision)
        for subtree in tree.trees:
            self._walk(subtree, file_handler, revision)

    def _check(self, blob: Blob, file_handler: FileOwnershipHandler, revision: Reference):
        if self._is_source(blob):
            file_handler.analyze(blob.path, revision)

    @staticmethod
    def _is_source(file: Blob):
        return file.name.endswith(".java")
