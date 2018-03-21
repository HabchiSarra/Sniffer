# coding=utf-8

import argparse
import csv
from multiprocessing import Pool

from analysis.commits.computation import CommitsAnalyzer
from analysis.commits.output import CsvCommitWriter
from analysis.computation import ProjectHandler, LocalProjectHandler, RemoteProjectHandler
from analysis.ownership.computation import OwnershipAnalyzer
from analysis.ownership.output import CsvOwnershipWriter
from analysis.projects.computation import ProjectAnalyzer
from analysis.projects.output import CsvProjectWriter


def handle_args():
    parser = argparse.ArgumentParser(description='Process the commits of the given projects')
    parser.add_argument('csv', metavar='CSV',
                        help='A CSV file containing at least 2 columns, the project name and github uri')
    parser.add_argument('-t', '--threads', type=int, default=1,
                        help="Specify the number of thread to use")
    parser.add_argument('-l', '--local', type=str, default=None,
                        help="Specify the local repositories location")
    # TODO: [args] output dir
    # TODO: [args] output type
    # TODO: [args] single project
    # TODO: [args] analyze type
    return parser.parse_args()


class Processing(object):
    def __init__(self, apps: csv.DictReader, args):
        self.apps = apps
        self.thread_count = args.threads

        # Choosing analysis origin
        if args.local is not None:
            self.method = self.analyze_local
            self.arguments = [args.local + x['name'] for x in self.apps]
        else:
            self.method = self.analyze_remote
            self.arguments = apps

    def process(self):
        with Pool(self.thread_count) as p:
            p.map(self.method, self.arguments)

    @staticmethod
    def analyze_remote(app):
        """
        Analyze the remote repository, available on github, defined by a dictionary entry.
        :param app: A Dict containing the fields:
         - "uri": The user/repository available on GitHub.
         - "name": The project name to use in logging and output.
        :return: None
        """
        app_name = app["name"]
        app_url = "https://github.com/" + app["uri"]
        print("Handling project: " + app_name + " - " + app_url)
        Processing._analyze(app_name, RemoteProjectHandler(app_url))

    @staticmethod
    def analyze_local(repo: str):
        """
        Analyze a local repository from its path.

        :param repo: Path to the local repository.
        :return: None
        """
        print("Handling local project: " + repo)
        Processing._analyze(repo.split("/")[-1], LocalProjectHandler(repo))

    @staticmethod
    def _analyze(app_name: str, handler: ProjectHandler):
        # TODO: There must be a better way of assigning those writers.
        commit_writer = CsvCommitWriter("./output/commits-" + app_name + ".csv")
        # project_writer = CsvProjectWriter("./output/project-" + app_name + ".csv")
        ownership_writer = CsvOwnershipWriter("./output/ownership-" + app_name + ".csv")
        handler.add_analyzer(CommitsAnalyzer(commit_writer))
        # handler.add_analyzer(ProjectAnalyzer(project_writer))
        handler.add_analyzer(OwnershipAnalyzer(ownership_writer))
        handler.run()
        del handler

    @staticmethod
    def _generate_output_writer(app_name: str):
        return CsvCommitWriter("./output/output-" + app_name + ".csv")


if __name__ == '__main__':
    exec_args = handle_args()

    with open(exec_args.csv, 'r') as appsfile:
        apps_csv = csv.DictReader(appsfile, fieldnames=["name", "uri"])
        Processing(apps_csv, exec_args).process()
        print("Done!")
