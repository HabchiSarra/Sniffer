# coding=utf-8

from harissa_project_analysis.binding.mergers.rq3 import merge_ownership_for_ird


def all_merges(metrics: str, repos: str, output: str):
    # merge_commits_by_ird(metrics)
    # merge_ird_by_smell(metrics)
    # merge_commits_with_smells(metrics, commits)
    if repos is None:
        print("Repository value is needed for ownership merge!")
    else:
        merge_ownership_for_ird(metrics, repos, output)


if __name__ == '__main__':
    metrics_dir = "/data/tandoori-metrics/results"
    # metrics_dir = "/data/tandoori-metrics/results"
    commits_dir = "/data/tandoori-metrics/commits-analysis"
    repos_dir = "/data/tandoori-repos"
    # input_commits = "/data/tandoori-metrics/commits-analysis"
    # merge_commits_by_ird(metrics)
    # merge_ird_by_smell(metrics)
    # merge_commits_with_smells(metrics_dir, commits_dir)
    merge_ownership_for_ird(metrics_dir, repos_dir, "/data/ownership")
