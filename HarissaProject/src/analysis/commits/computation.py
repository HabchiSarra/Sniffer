# coding=utf-8
from typing import List

from git import Repo, Commit, NULL_TREE, TagReference

from analysis.commits.classification import CommitClassifier
from analysis.commits.output import *
from analysis.computation import Analyzer

__all__ = ["CommitsAnalyzer"]


class CommitsAnalyzer(Analyzer):
    """
    Analyze a project's commits.
    """

    def __init__(self, output_writer: CommitOutputWriter = CsvCommitWriter()):
        """
        Analyze each commit of a project.
        :param output_writer: The writer to use.
        """
        super().__init__(output_writer)

    def _process(self, repo: Repo):
        """
        Analyze each commit of the repository and write the result
        using the given output_writer.

        :return: None
        """
        commit_handler = CommitHandler(self.output_writer)
        commit_handler.set_tags(repo.tags)
        log = list(repo.iter_commits(repo.active_branch))

        new_commit = NULL_TREE
        for commit in reversed(log):
            old_commit = new_commit
            new_commit = commit
            commit_handler.analyze(new_commit=new_commit, old_commit=old_commit)


class CommitHandler(object):
    """
    Handle the analysis of commits for a project.
    """

    def __init__(self, output_writer: CommitOutputWriter):
        self.output_writer = output_writer
        self.classifier = CommitClassifier()
        self.tags = {}

    def analyze(self, new_commit: Commit, old_commit: Commit):
        """
        Analyze the given new_commit, using the old_commit to generate a diff.
        The analysis will then be added to the output_writer.

        :param new_commit: The commit to analyze.
        :param old_commit: The commit before the new_commit argument,
        please provide git.NULL_TREE if new_commit is the first.
        :return: None
        """
        diff = DiffHandler(new_commit, old_commit)
        classifications = self.classifier.classify(new_commit.message)

        self.output_writer.add_commit(
            author=new_commit.author,
            committer=new_commit.committer,
            date=new_commit.committed_datetime,
            sha1=new_commit.hexsha,
            addition=diff.additions(),
            deletion=diff.deletions(),
            classifications=classifications,
            message=new_commit.message,
            tags=self.get_tags(new_commit.hexsha)
        )

    def get_tags(self, sha: str):
        if sha in self.tags:
            return self.tags[sha]
        else:
            return []

    def set_tags(self, tags: List[TagReference]):
        for tag in tags:
            sha = tag.commit.hexsha
            if sha not in self.tags:
                self.tags[sha] = [tag.name]
            else:
                self.tags[sha].append(tag.name)


class DiffHandler(object):
    """
    Compute the metrics associated with a diff between two commits.
    """

    def __init__(self, new_commit: Commit, old_commit: Commit):
        # Since we can't call diff on NULL_TREE, we have to invert the first commit
        if old_commit == NULL_TREE:
            old_commit = new_commit
            new_commit = NULL_TREE
        self.diff = old_commit.diff(other=new_commit, create_patch=True)

    def additions(self):
        return self._count_changes("\n+")

    def deletions(self):
        return self._count_changes("\n-")

    def _count_changes(self, regex: str):
        count = 0
        for file in self.diff:
            file_diff = self._decode(file.diff)
            count += file_diff.count(regex)
        return count

    @staticmethod
    def _decode(diff: bytes):
        try:
            return diff.decode("utf-8")
        except UnicodeDecodeError:
            return diff.decode("iso-8859-1")
