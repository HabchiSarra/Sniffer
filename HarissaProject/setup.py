# coding=utf-8
import os

from setuptools import setup, find_packages


def read(fname):
    """Utility function to read the README file.
         Used for the long_description.  It's nice, because now:
         1 ) we have a top level README file and
         2) it's easier to type in the README file than to put a raw string in below ...
    :param fname:
    :return:
    """
    return open(os.path.join(os.path.dirname(__file__), fname)).read()


setup(
    name="harissa_project_analysis",
    version="1.0.0",
    author="Antoine Veuiller",
    author_email="aveuiller@gmail.com",
    description=("Static analysis of github projects, "
                 "this project aims to compute data for Android smell analysis."),
    license="MIT",
    keywords="Android smell analysis",
    url="http://packages.python.org/harissa_project_analysis",
    packages=find_packages(),
    long_description=read('README.md'),
    classifiers=[
        "Development Status :: 3 - Alpha",
        "Programming Language :: Python :: 3.6",
        "Topic :: Utilities",
        "License :: OSI Approved :: MIT License"
    ],
)
