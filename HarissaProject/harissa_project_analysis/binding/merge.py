# coding=utf-8

from binding.mergers.intro import merge_commits_by_ird
from binding.mergers.rq1 import merge_ird_by_smell
from binding.mergers.rq2 import merge_commits_with_smells


def all_merges(metrics: str, commits: str):
    merge_commits_by_ird(metrics)
    merge_ird_by_smell(metrics)
    merge_commits_with_smells(metrics, commits)


if __name__ == '__main__':
    metrics_dir = "/data/tandoori-metrics/results"
    commits_dir = "/data/tandoori-metrics/commits-analysis"
    # input_commits = "/data/tandoori-metrics/commits-analysis"
    # merge_commits_by_ird(metrics)
    # merge_ird_by_smell(metrics)
    merge_commits_with_smells(metrics_dir, commits_dir)
