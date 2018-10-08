# coding=utf-8
import unittest

from project_profiler.binding.smells import FileFinder


class TestFileFinder(unittest.TestCase):
    def test_infer_class_with_method_call(self):
        smell_instance = "getApps#dev.ukanth.ufirewall.Api"
        expected = "dev/ukanth/ufirewall/Api"
        self.assertEqual(FileFinder._infer_java_file(smell_instance), expected)

    def test_infer_class_with_subclass(self):
        smell_instance = "eu.chainfire.libsuperuser.Shell$OnCommandResultListener"
        expected = "eu/chainfire/libsuperuser/Shell"
        self.assertEqual(FileFinder._infer_java_file(smell_instance), expected)

    def test_infer_class_with_method_call_and_subclass(self):
        smell_instance = "cbFunc#dev.ukanth.ufirewall.RootShell$RootCommand$Callback"
        expected = "dev/ukanth/ufirewall/RootShell"
        self.assertEqual(FileFinder._infer_java_file(smell_instance), expected)

    def test_infer_class_without_method_call_or_subclass(self):
        smell_instance = "dev.ukanth.ufirewall.DataDumpActivity"
        expected = "dev/ukanth/ufirewall/DataDumpActivity"
        self.assertEqual(FileFinder._infer_java_file(smell_instance), expected)


if __name__ == '__main__':
    unittest.main()
