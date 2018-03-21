# coding=utf-8

import argparse
import csv
from multiprocessing import Pool
from typing import Dict

from analysis.commits.computation import ProjectAnalyzer
from analysis.commits.output import CsvOutputWriter
from analysis.computation import ProjectHandler, LocalProjectHandler, RemoteProjectHandler


def handle_args():
    parser = argparse.ArgumentParser(description='Process the commits of the given projects')
    parser.add_argument('csv', metavar='CSV',
                        help='A CSV file containing at least 2 columns, the project name and github uri')
    parser.add_argument('-t', '--threads', type=int, default=1,
                        help="Specify the number of thread to use")
    parser.add_argument('-l', '--local', type=str, default=None,
                        help="Specify the local repositories location")
    return parser.parse_args()


def generate_output_writer(app_name: str):
    return CsvOutputWriter("./output/output-" + app_name + ".csv")


class Processing(object):
    def __init__(self, apps: Dict, args):
        self.apps = apps
        if args.local is not None:
            self.method = self.analyze_local
            self.arguments = [args.local + x['name'] for x in apps]
        else:
            self.method = self.analyze_remote
            self.arguments = apps
        self.analyzers = (ProjectAnalyzer(),)


    def process(self):
        with Pool(args.threads) as p:
            p.map(self.method, self.arguments)

    def analyze_remote(self, app):
        app_name = app["name"]
        app_url = "https://github.com/" + app["uri"]
        print("Handling project: " + app_name + " - " + app_url)
        writer = generate_output_writer(app_name)
        handler = RemoteProjectHandler(app_url)
        self.analyze(handler)

    def analyze_local(self, repo: str):
        print("Handling local project: " + repo)
        writer = generate_output_writer(repo.split("/")[-1])
        handler = LocalProjectHandler(repo)
        self.analyze(handler)

    def analyze(self, handler: ProjectHandler):
        handler.add_analyzer(ProjectAnalyzer(output_writer=writer))
        handler.run()
        del handler


if __name__ == '__main__':
    args = handle_args()

    my_pool = Pool(args.threads)

    with open(args.csv, 'r') as appsfile:
        apps = csv.DictReader(appsfile, fieldnames=["name", "uri"])
        # Choosing analysis method
        if args.local is not None:
            method = analyze_local
            arguments = [args.local + x['name'] for x in apps]
        else:
            method = analyze_remote
            arguments = apps

        # Actual analysis
        with Pool(args.threads) as p:
            p.map(method, arguments)
    print("Done!")
