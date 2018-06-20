# coding=utf-8

import sys, os, unittest


def my_test_suite():
    test_loader = unittest.TestLoader()
    test_suite = test_loader.discover('src/tests', pattern='test_*.py')
    return test_suite


if __name__ == '__main__':
    sys.path.append(os.path.dirname(os.path.realpath(__file__)) + "/src")
    runner = unittest.TextTestRunner()
    runner.run(my_test_suite())
