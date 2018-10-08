# coding=utf-8

import argparse
import csv

from project_profiler import __version__
from project_profiler.analysis.processing import AnalysisProcessing
from project_profiler.binding.smells import OwnershipProcessing

from project_profiler.binding.merge import all_merges


def handle_args():
    parser = argparse.ArgumentParser(description='Process the commits of the given projects')

    # common arguments
    # TODO: [args] output type - CSV, SQL, ...
    parser.add_argument('--version', action='version', version='%(prog)s ' + __version__)
    parser.add_argument('-t', '--threads', type=int, default=1,
                        help="Specify the number of thread to use")
    parser.add_argument("-o", "--output", type=str, default="./output",
                        help="Output directory for the results.")

    subparsers = parser.add_subparsers(dest="command",
                                       description="Available commands on ProjectProfiler analysis software")

    # Project analysis arguments
    analysis_parser = subparsers.add_parser('analysis')
    analysis_input_group = analysis_parser.add_mutually_exclusive_group(required=True)
    analysis_input_group.add_argument("-r", "--remote", type=str, default=None,
                                      help="Remote analysis. You must provide a CSV with the project name and uri.")
    analysis_input_group.add_argument('-l', '--local', type=str, default=None,
                                      help="Specify the local repositories location, will analyse the subdirectories.")
    # TODO: [args] analysis type - Ownership, Commits, Project (switch args)

    # Project binding with external data arguments
    binding_parser = subparsers.add_parser('binding')
    binding_parser.add_argument("-o", "--smells-ownership", action="store_true", dest="smells_ownership",
                                help="Analyse the ownership of the host file for each project smell instance")
    binding_parser.add_argument("-i", "--input", type=str, required=True,
                                help="Projects' smells structured as $input/$project/smells/*.csv")
    binding_parser.add_argument("-r", "--repo", type=str, required=True,
                                help="Projects' git repositories structured as $input/$project/.git")

    merge_parser = subparsers.add_parser('merge')
    merge_parser.add_argument("-i", "--input", type=str, required=True,
                              help="Projects' smells structured as $input/$project/smells/*.csv")
    merge_parser.add_argument("-r", "--repo", type=str, required=False,
                              help="Projects' git repositories structured as $repo/$project/.git")
    merge_parser.add_argument("-c", "--commits", type=str, required=False,
                              help="Projects' commits analysis structured as $commits/commits-$project.csv")
    merge_parser.add_argument("-l", "--logs", type=str, required=False,
                              help="Projects' logs in topological order as $logs/$project.logs")
    # TODO: [args] single project - possible via local? Give directly the repo?
    return parser.parse_args()


def analysis_command(args):
    if args.remote is not None:
        with open(args.remote, 'r') as appsfile:
            apps_csv = csv.DictReader(appsfile, fieldnames=["name", "uri"])
            AnalysisProcessing(apps_csv, args.threads, args.local, args.output).process()
    else:
        AnalysisProcessing(None, args.threads, args.local, args.output).process()


def binding_command(args):
    if args.smells_ownership:
        OwnershipProcessing(args.input, args.repo, args.threads, args.output).process()
    else:
        print("No binding defined!")


def merge_command(args):
    all_merges(metrics=args.input, repos=args.repo, commits=args.commits, output=args.output, logs=args.logs)


if __name__ == '__main__':
    args = handle_args()

    if args.command == "analysis":
        analysis_command(args)
    elif args.command == "binding":
        binding_command(args)
    elif args.command == "merge":
        merge_command(args)
    print("Done!")
