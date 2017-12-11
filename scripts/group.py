#!/usr/bin/python3

# Merge _analyzed_ commits with tagged ones
# Python >3.5 needed

import csv
import os


def isTagged(entry):
    return (int(entry["feat"]) + int(entry["fix"]) + int(entry["docs"])
            + int(entry["style"]) + int(entry["refactor"]) + int(entry["perf"])
            + int(entry["tests"]) + int(entry["chores"])
            ) >= 1


def sumCommits(path):
    commitSmells = {}
    with open(path, 'r') as metricsFile:
        metrics = csv.reader(metricsFile)
        for row in metrics:
            # If we hit the first row or an empty one, we skip it
            if len(row) == 0 or row[0] == "commitNumber":
                continue
            # For each smell and dev we have the sequence I,R,D. Starting at third column
            introduction = sum(list(map(int, row[3::3])))
            refactor = sum(list(map(int, row[4::3])))
            deletion = sum(list(map(int, row[5::3])))
            # row[1] is commit sha
            commitSmells[row[1]] = {"I": introduction, "R": refactor, "D": deletion, "R_D": refactor + deletion}
    return commitSmells


csvPrefix = "output"
tagsSuffix = "-commits-results"
smellsSuffix = "metrics/metrics-perDev-perCommit-perSmell.csv"
taggedOutput = "completeTags-onlyTagged.csv"
output = "completeTags.csv"

header_line = ["project", "commit", "feat", "fix", "docs", "style", "refactor", "perf", "tests",
               "chores", "I", "R", "D", "R_D"]
with open(output, "w") as outputFile, open(taggedOutput, "w") as taggedOutputFile:
    writer = csv.DictWriter(outputFile, header_line)
    taggedWriter = csv.DictWriter(taggedOutputFile, header_line)

    writer.writeheader()
    taggedWriter.writeheader()

    for directory in os.listdir(csvPrefix):
        tagsPath = csvPrefix + "/" + directory + "/" + directory + tagsSuffix
        metricsPath = csvPrefix + "/" + directory + "/" + smellsSuffix
        if not (os.path.exists(tagsPath) and os.path.exists(metricsPath)):
            print("Skipping " + directory)
            continue

        commitSmells = sumCommits(metricsPath)

        # Counting occurrences of smells
        with open(tagsPath, 'r') as tagsfile:
            tags = csv.DictReader(tagsfile)
            nbCommits = 0
            nbTaggedCommits = 0
            for entry in tags:
                sha = entry["commit"]
                if sha in commitSmells:
                    smells = commitSmells[sha]
                    nbCommits += 1
                    writer.writerow({**entry, **smells})
                    if isTagged(entry):
                        taggedWriter.writerow({**entry, **smells})
                        nbTaggedCommits += 1

        print("Project: " + directory + " - " + str(nbCommits) + " commits analyzed, including  " + str(nbTaggedCommits) + " tagged")
print("Done")
