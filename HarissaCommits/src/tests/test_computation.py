# coding=utf-8
import unittest

from commitsanalysis.computation import CommitHandler, ProjectHandler
from commitsanalysis.output import OutputWriter


class TestProjectHandler(unittest.TestCase):
    def setUp(self):
        # self.handler = ProjectHandler("repo/url")
        pass

    # TODO: Create test suite

    def test_project(self):
        self.assertTrue(True)


class TestCommitHandler(unittest.TestCase):

    def setUp(self):
        self.handler = CommitHandler(OutputWriter())  # TODO: Mock output

    # TODO: Create test suite

    def test_commit_computation(self):
        self.assertTrue(True)


if __name__ == '__main__':
    unittest.main()
