# coding=utf-8
import unittest
from project_profiler.analysis.commits.classification import CommitClassifier


class TestClassification(unittest.TestCase):

    def setUp(self):
        self.classifier = CommitClassifier()

    def test_chore(self):
        self.assertEqual(self.classifier.classify("doing chores in a commit"), ["chores"])

    def test_nothing(self):
        self.assertEqual(self.classifier.classify("no keyword"), [])

    def test_only_fix(self):
        self.assertEqual(self.classifier.classify("fixed changelog"), ["fix"])
    # TODO: Enhance test suite!


if __name__ == '__main__':
    unittest.main()
