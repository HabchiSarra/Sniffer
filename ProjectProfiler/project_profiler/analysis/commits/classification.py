# coding=utf-8

from typing import Tuple

__all__ = ["CommitClassifier"]


class KeywordClassifier(object):
    """
    Classify the given commit within the given keyword as the given category
    """

    def __init__(self, category: str, keywords: Tuple[str, ...]):
        self.category = category
        self.keywords = keywords

    def classify(self, message: str):
        """
        Tells if the message can be classified into the current category.

        The algorithm will look for a set of keywords inside the message,
        if the message contains at least one of these keywords,
        it will be classified as the corresponding category.

        :param message: str - The message to analyze
        :return: The name of the classified category if found, None otherwise.
        """
        for keyword in self.keywords:
            # We are looking for specific words, or beginning of words
            # thus the leading space avoid finding substrings in existing words (e.g. hang in changelog)
            # So we also add a leading space to the message to match the first word if needed.
            if " " + keyword in " " + message.lower():
                return self.category
        return None


class ChoresClassifier(KeywordClassifier):
    def __init__(self):
        super().__init__("chores", ("chore",))


class DocumentationClassifier(KeywordClassifier):
    def __init__(self):
        super().__init__("documentation", ("docs", "documentation"))


class FeatureClassifier(KeywordClassifier):
    def __init__(self):
        super().__init__(
            "feature",
            ("implement", "add", "test", "request", "new", "start",
             "includ", "initial", "introduc", "creat", "increas")
        )


class FixClassifier(KeywordClassifier):
    def __init__(self):
        super().__init__(
            "fix",
            ("error", "bug", "fix", "issue", "mistake",
             "incorrect", "fault", "defect", "flaw")
        )


class PerformanceClassifier(KeywordClassifier):
    def __init__(self):
        super().__init__(
            "performance",
            ("wait", "slow", "fast", "lag", "tim", "minor",
             "stuck", "instant", "respons", "react", "speed",
             "latenc", "perform", "throughput", " hang", "memory", "leak")
        )


class RefactorClassifier(KeywordClassifier):
    def __init__(self):
        super().__init__(
            "refactor",
            ("refactor", "restruct", "clean",
             "not used", "unused", "reformat", "import ",
             "remove", "replace", "split", "reorg", "rename", "move")
        )


class StyleClassifier(KeywordClassifier):
    def __init__(self):
        super().__init__("style", ("style",))


class TestClassifier(KeywordClassifier):
    def __init__(self):
        super().__init__("tests", ("test",))


class CommitClassifier(object):
    CLASSIFIERS_ALL: Tuple[KeywordClassifier] = (
        ChoresClassifier(),
        DocumentationClassifier(),
        FeatureClassifier(),
        FixClassifier(),
        PerformanceClassifier(),
        RefactorClassifier(),
        StyleClassifier(),
        TestClassifier()
    )

    def classify(self, commit_message: str):
        """
        Use a set of classifiers to tag the given commit message.

        :param commit_message: the message to classify
        :return: A list of classifications found on the message.
        """
        classifications = []
        for classifier in self.CLASSIFIERS_ALL:
            classify = classifier.classify(commit_message)
            if classify is not None:
                classifications.append(classify)
        return classifications
