"""Run with python3 src/test/check_teleport_access.py; requires cached Maven dependencies."""
import os
import sys
import re
import shutil
import subprocess
from pathlib import Path

root = Path(__file__).resolve().parents[2]
mvn = shutil.which("mvn") or "/Applications/IntelliJ IDEA.app/Contents/plugins/maven-plugin/lib/maven3/bin/mvn"
env = os.environ.copy()
if sys.platform == "darwin":
    env["JAVA_HOME"] = subprocess.check_output(["/usr/libexec/java_home"], text=True).strip()
result = subprocess.run([mvn, "-o", "-X", "test-compile"], cwd=root, env=env,
                        stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True)
if result.returncode:
    raise SystemExit(result.stdout[-4000:])
paths = re.search(r"\(f\) testPath = \[(.*?)\]", result.stdout).group(1).split(", ")
main = "work.alsace.buildercore.service.TeleportAccessCheck"
subprocess.run(["java", "-ea", "-cp", ":".join(paths), main], cwd=root, check=True)
# Also exercise disabled/missing MMC with its API completely absent from the classpath.
subprocess.run(["java", "-ea", "-cp", ":".join(p for p in paths if "mapmanager-api" not in p),
                main, "without-mmc"], cwd=root, check=True)
