# coding=utf-8
from harissa_project_analysis.binding.mergers.rq1 import merge_commits_with_smells
from harissa_project_analysis.binding.mergers.rq4 import merge_ownership_for_ird


def all_merges(metrics: str, repos: str = None, commits: str = None, logs:str = None, output: str = "./"):
    # merge_commits_by_ird(metrics)
    # merge_ird_by_smell(metrics)

    # if commits is None:
    #     print("Commits and logs values are needed for ownership merge!")
    # else:
    #     merge_commits_with_smells(metrics, commits)

    if repos is None or logs is None:
        print("Repository and logs values are needed for ownership merge!")
    else:
        merge_ownership_for_ird(metrics, repos, logs_dir=logs, output_path=output)


if __name__ == '__main__':
    metrics_dir = "/data/tandoori-metrics/results"
    # commits_dir = "/data/tandoori-metrics/commits-analysis"
    repos_dir = "/data/tandoori-repos"
    logs_dir = "/data/tandoori-metrics/logs"
    # input_commits = "/data/tandoori-metrics/commits-analysis"
    # merge_commits_by_ird(metrics)
    # merge_ird_by_smell(metrics)
    # merge_commits_with_smells(metrics_dir, commits_dir)
    merge_ownership_for_ird(metrics_dir, repos_dir, logs_dir=logs_dir, output_path="/data/tandoori-trials/ownership")
