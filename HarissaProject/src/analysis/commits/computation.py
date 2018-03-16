# coding=utf-8
import os
import shutil
import tempfile
import time
from typing import List

from git import Repo, Commit, NULL_TREE, TagReference

from analysis.commits.classification import CommitClassifier
from analysis.commits.output import *

__all__ = ["LocalProjectHandler", "RemoteProjectHandler"]


def generate_temp_dir():
    return tempfile.mkdtemp(prefix="projectAnalyzer", suffix=str(time.time()))


class ProjectHandler(object):
    """
    Clone and analyze a project from any git url.
    """

    def __init__(self, repo: Repo,
                 output_writer: OutputWriter = CsvOutputWriter()):
        """

        :param repo:  The git.Repo instance to work with
        :param output_writer: The writer to use
        """
        self.output_writer = output_writer
        self.commit_handler = CommitHandler(self.output_writer)
        self.commit_handler.set_tags(repo.tags)
        self.repo = repo

    def analyze(self):
        """
        Analyze each commit of the repository and write the result
        using the given output_writer.

        :return: None
        """
        log = list(self.repo.iter_commits(self.repo.active_branch))

        new_commit = NULL_TREE
        for commit in reversed(log):
            old_commit = new_commit
            new_commit = commit
            self.commit_handler.analyze(new_commit=new_commit, old_commit=old_commit)

        self.output_writer.write()


class RemoteProjectHandler(ProjectHandler):
    def __init__(self, repo_url: str,
                 working_dir: os.DirEntry = None,
                 output_writer: OutputWriter = CsvOutputWriter()):
        # Making the default parameter mutable...
        if working_dir is None:
            working_dir = generate_temp_dir()
        self.working_dir = working_dir
        repo = Repo().clone_from(repo_url, working_dir)
        super().__init__(repo, output_writer)

    def __del__(self):
        """
        Remove the working directory on object destruction.
        :return: None
        """
        if isinstance(self.working_dir, str):
            to_remove = self.working_dir
        else:
            to_remove = self.working_dir.path

        # Deleting git repository content
        print("Removing dir: " + to_remove)
        shutil.rmtree(to_remove)


class LocalProjectHandler(ProjectHandler):
    def __init__(self, repo_path: str,
                 output_writer: OutputWriter = CsvOutputWriter()):
        super().__init__(Repo(repo_path), output_writer)


class CommitHandler(object):
    """
    Handle the analysis of commits for a project.
    """

    def __init__(self, output_writer: OutputWriter):
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
