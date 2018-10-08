# coding=utf-8
import unittest

from project_profiler.analysis.commits.computation import CommitHandler
from project_profiler.analysis.commits.output import CommitOutputWriter


class TestProjectHandler(unittest.TestCase):
    def setUp(self):
        # self.handler = ProjectHandler("repo/url")
        pass

    # TODO: Create test suite

    def test_project(self):
        self.assertTrue(True)


class TestCommitHandler(unittest.TestCase):

    def setUp(self):
        self.handler = CommitHandler(CommitOutputWriter())  # TODO: Mock output

    # TODO: Create test suite

    def test_commit_computation(self):
        self.assertTrue(True)


if __name__ == '__main__':
    unittest.main()
