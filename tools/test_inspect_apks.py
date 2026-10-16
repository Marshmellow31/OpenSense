import hashlib
from pathlib import Path
import tempfile
import unittest
import zipfile
from inspect_apks import inspect_apk


class InspectorTest(unittest.TestCase):
    def test_synthetic_zip_is_not_treated_as_signed_or_publisher_verified(self):
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "synthetic.apk"
            with zipfile.ZipFile(path, "w") as archive:
                archive.writestr("classes.dex", b"BluetoothSocket 00001101-0000-1000-8000-00805f9b34fb")
            report = inspect_apk(path)
            self.assertEqual(hashlib.sha256(path.read_bytes()).hexdigest(), report["sha256"])
            self.assertEqual("unverified", report["signature"]["status"])
            self.assertEqual("needs-confirmation", report["provenance"])
            self.assertEqual(["00001101-0000-1000-8000-00805f9b34fb"], report["candidate_uuids"])

    def test_non_zip_is_rejected(self):
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "bad.apk"
            path.write_bytes(b"not-an-apk")
            with self.assertRaises(zipfile.BadZipFile):
                inspect_apk(path)

    def test_archive_size_budget(self):
        from unittest.mock import patch
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "large.apk"
            path.write_bytes(b"1234")
            with patch("inspect_apks.MAX_APK_BYTES", 3):
                with self.assertRaises(ValueError):
                    inspect_apk(path)


if __name__ == "__main__":
    unittest.main()
