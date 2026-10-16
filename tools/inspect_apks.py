#!/usr/bin/env python3
"""Static, non-executing APK inventory. Output belongs in ignored private-analysis/."""
import argparse
import hashlib
import json
import os
from pathlib import Path
import re
import shutil
import subprocess
import zipfile

MAX_APK_BYTES = 256 * 1024 * 1024
MAX_EXPANDED_BYTES = 512 * 1024 * 1024
MAX_ENTRY_BYTES = 64 * 1024 * 1024
UUID = re.compile(rb"[0-9a-fA-F]{8}(?:-[0-9a-fA-F]{4}){3}-[0-9a-fA-F]{12}")
INDICATORS = (b"BluetoothSocket", b"BluetoothGatt", b"createRfcommSocket", b"listenUsingRfcomm",
              b"connectGatt", b"fetchUuidsWithSdp", b"SPP", b"RFCOMM", b"checksum", b"CRC")


def run_tool(command):
    """No shell interpolation of APK paths; batch tools on Windows use a restricted path set."""
    try:
        completed = subprocess.run([str(x) for x in command], capture_output=True, text=True,
                                   errors="replace", timeout=120, check=False)
        return {"status": "verified" if completed.returncode == 0 else "failed",
                "exit_code": completed.returncode, "stdout": completed.stdout[:100000],
                "stderr": completed.stderr[:10000]}
    except (OSError, subprocess.TimeoutExpired) as error:
        return {"status": "unavailable", "error_type": type(error).__name__}


def sdk_tools(sdk):
    if not sdk:
        return {}
    root = Path(sdk) / "build-tools"
    candidates = sorted((p for p in root.glob("*") if p.is_dir() and re.fullmatch(r"\d+(?:\.\d+)+", p.name)),
                        key=lambda p: tuple(map(int, p.name.split("."))), reverse=True)
    if not candidates:
        return {}
    suffix = ".exe" if os.name == "nt" else ""
    result = {"aapt2": candidates[0] / ("aapt2" + suffix)}
    # Invoke the signer JAR directly so no shell/batch parsing touches untrusted filenames.
    result["apksigner_jar"] = candidates[0] / "lib" / "apksigner.jar"
    return result


def inspect_apk(path, tools=None, java="java"):
    path = Path(path)
    if path.stat().st_size > MAX_APK_BYTES:
        raise ValueError("APK exceeds analysis size limit")
    digest = hashlib.sha256()
    with path.open("rb") as source:
        for chunk in iter(lambda: source.read(1024 * 1024), b""):
            digest.update(chunk)
    report = {"file": path.name, "sha256": digest.hexdigest(), "bytes": path.stat().st_size,
              "execution": "never executed or installed", "provenance": "needs-confirmation",
              "identity": "needs SDK badging inspection", "signature": {"status": "unverified"},
              "permissions": "needs SDK manifest inspection", "dependencies": "not a complete dependency inventory"}
    sidecar = path.with_suffix(path.suffix + ".provenance.json")
    if sidecar.exists():
        if sidecar.stat().st_size > 16384:
            raise ValueError("provenance sidecar too large")
        report["provenance"] = json.loads(sidecar.read_text(encoding="utf-8"))
    with zipfile.ZipFile(path) as archive:
        entries = archive.infolist()
        if len(entries) > 10000 or sum(x.file_size for x in entries) > MAX_EXPANDED_BYTES:
            raise ValueError("archive exceeds analysis limits")
        report["zip_entries"] = len(entries)
        report["native_libraries"] = [x.filename for x in entries if x.filename.startswith("lib/") and x.filename.endswith(".so")]
        report["signing_container_names"] = [x.filename for x in entries if x.filename.startswith("META-INF/") and x.filename.upper().endswith((".RSA", ".DSA", ".EC"))]
        report["candidate_uuids"] = []
        report["dex_indicators"] = []
        for entry in entries:
            if re.fullmatch(r"classes\d*\.dex", entry.filename):
                if entry.file_size > MAX_ENTRY_BYTES:
                    raise ValueError("DEX exceeds per-entry analysis limit")
                data = archive.read(entry)
                report["candidate_uuids"].extend(sorted({match.decode("ascii").lower() for match in UUID.findall(data)}))
                report["dex_indicators"].append({"file": entry.filename,
                    "present": [word.decode("ascii") for word in INDICATORS if word in data]})
        report["candidate_uuids"] = sorted(set(report["candidate_uuids"]))
    tools = tools or {}
    if tools.get("aapt2") and Path(tools["aapt2"]).exists():
        report["badging"] = run_tool([tools["aapt2"], "dump", "badging", path.resolve()])
        report["manifest_tree"] = run_tool([tools["aapt2"], "dump", "xmltree", path.resolve(), "--file", "AndroidManifest.xml"])
        if report["badging"]["status"] == "verified":
            badging = report["badging"]["stdout"]
            identity = re.search(r"package: name='([^']+)' versionCode='([^']+)' versionName='([^']+)'", badging)
            if identity:
                report["identity"] = dict(zip(("package", "version_code", "version_name"), identity.groups()))
            report["permissions"] = re.findall(r"uses-permission: name='([^']+)'", badging)
            report["sdk"] = {"minimum": re.search(r"minSdkVersion:'([^']+)'", badging).group(1)
                             if re.search(r"minSdkVersion:'([^']+)'", badging) else None,
                             "target": re.search(r"targetSdkVersion:'([^']+)'", badging).group(1)
                             if re.search(r"targetSdkVersion:'([^']+)'", badging) else None}
    if tools.get("apksigner_jar") and Path(tools["apksigner_jar"]).exists():
        report["signature"] = run_tool([java, "-jar", tools["apksigner_jar"], "verify", "--verbose", "--print-certs", path.resolve()])
    report["interpretation"] = "Strings and UUIDs are candidates only; no protocol behavior or publisher authenticity established. Signature validity alone does not establish publisher identity."
    return report


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--input", type=Path, default=Path("apk-input"))
    parser.add_argument("--output", type=Path, default=Path("private-analysis/apk-inventory.json"))
    parser.add_argument("--android-sdk", default=os.environ.get("ANDROID_HOME"))
    parser.add_argument("--java", default=shutil.which("java") or "java")
    args = parser.parse_args()
    results = []
    for path in sorted(args.input.glob("*.apk")):
        try:
            results.append(inspect_apk(path, sdk_tools(args.android_sdk), args.java))
        except (ValueError, OSError, zipfile.BadZipFile, RuntimeError) as error:
            results.append({"file": path.name, "status": "analysis_failed", "error_type": type(error).__name__})
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps({"schema": 1, "apk_count": len(results), "apks": results}, indent=2) + "\n", encoding="utf-8")
    print(f"Inventoried {len(results)} APK(s). Private report: {args.output}")
    if not results:
        print("No APK was analyzed. Supply authorized original APKs under apk-input/.")
    return 1 if any(x.get("status") == "analysis_failed" for x in results) else 0


if __name__ == "__main__":
    raise SystemExit(main())
