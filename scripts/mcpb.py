#!/usr/bin/env python3
"""Build the Claude Desktop extensions (.mcpb) of the spring-vision MCP server.

usage: mcpb.py VERSION MACOS_JAR WINDOWS_JAR OUT_DIR

Writes OUT_DIR/spring-vision-VERSION-macos.mcpb (Apple Silicon) and
OUT_DIR/spring-vision-VERSION-windows.mcpb (x64), each with a copy named
spring-vision-latest-macos.mcpb / spring-vision-latest-windows.mcpb that always holds the last build. Each bundle holds the model-free MCP jar built
with that platform's native libraries and the unmodified Eclipse Temurin JRE pinned in
mcp/mcpb/runtimes.json, so the machine it is installed on needs nothing else. Models are
downloaded by the server on its first run.

On an Apple Silicon Mac the macOS bundle is started once over stdio before it is packed: the
build fails if it does not answer, and the tool list in both manifests comes from that run.
Format: https://github.com/modelcontextprotocol/mcpb/blob/main/MANIFEST.md
"""
import hashlib
import json
import os
import pathlib
import platform
import shutil
import subprocess
import sys
import tarfile
import tempfile
import urllib.request
import zipfile

ROOT = pathlib.Path(__file__).resolve().parent.parent
MCPB = ROOT / "mcp" / "mcpb"
JAR = "spring-vision-mcp.jar"
STDIO_ARGS = ["-Djava.awt.headless=true", "-Dfile.encoding=UTF-8"]


def sha256(path):
    digest = hashlib.sha256()
    with open(path, "rb") as f:
        for block in iter(lambda: f.read(1 << 20), b""):
            digest.update(block)
    return digest.hexdigest()


def fetch_runtime(spec, cache):
    """The runtime archive in the cache, downloaded when missing; refuses a checksum mismatch."""
    archive = cache / spec["url"].rsplit("/", 1)[-1]
    if not archive.exists() or sha256(archive) != spec["sha256"]:
        print(f"downloading {spec['release']} ({archive.name})")
        partial = archive.with_suffix(archive.suffix + ".part")
        with urllib.request.urlopen(spec["url"]) as response, open(partial, "wb") as out:
            shutil.copyfileobj(response, out)
        actual = sha256(partial)
        if actual != spec["sha256"]:
            partial.unlink()
            sys.exit(f"checksum mismatch for {archive.name}: expected {spec['sha256']}, got {actual}")
        partial.rename(archive)
    return archive


def unpack_without_top_folder(archive, target):
    """Unpacks the archive into target, dropping its single top-level folder (jdk-25…-jre/)."""
    with tempfile.TemporaryDirectory() as scratch:
        if archive.name.endswith(".zip"):
            with zipfile.ZipFile(archive) as z:
                z.extractall(scratch)
        else:
            with tarfile.open(archive) as t:
                t.extractall(scratch, filter="tar")
        (top,) = pathlib.Path(scratch).iterdir()
        shutil.copytree(top, target, symlinks=True)


def layout(name, jar, runtime_archive, out):
    """The bundle's folder: server/ with the jar and runtime, plus the macOS launcher."""
    folder = out / "mcpb" / name
    if folder.exists():
        shutil.rmtree(folder)
    server = folder / "server"
    server.mkdir(parents=True)
    shutil.copyfile(jar, server / JAR)
    unpack_without_top_folder(runtime_archive, server / "runtime")
    if name == "macos":
        launcher = server / "spring-vision"
        shutil.copyfile(MCPB / "spring-vision", launcher)
        launcher.chmod(0o755)
    return folder


def tools_of(macos_folder):
    """Starts the macOS bundle over stdio with an empty home and returns its tools."""
    with tempfile.TemporaryDirectory() as home:
        env = dict(os.environ, SPRING_VISION_LOG_HOME=str(pathlib.Path(home) / "logs"),
                   JAVA_TOOL_OPTIONS=f"-Duser.home={home}")
        requests = "\n".join(json.dumps(r) for r in [
            {"jsonrpc": "2.0", "id": 1, "method": "initialize",
             "params": {"protocolVersion": "2025-06-18", "capabilities": {},
                        "clientInfo": {"name": "mcpb-build", "version": "0"}}},
            {"jsonrpc": "2.0", "method": "notifications/initialized"},
            {"jsonrpc": "2.0", "id": 2, "method": "tools/list"},
        ]) + "\n"
        proc = subprocess.Popen([str(macos_folder / "server" / "spring-vision")], stdin=subprocess.PIPE,
                                stdout=subprocess.PIPE, stderr=subprocess.DEVNULL, env=env, text=True)
        proc.stdin.write(requests)
        proc.stdin.flush()
        tools = None
        for line in proc.stdout:
            message = json.loads(line)
            if message.get("id") == 2:
                tools = message["result"]["tools"]
                break
        proc.stdin.close()
        proc.wait(timeout=60)
    if not tools:
        sys.exit("the macOS bundle did not list its tools")
    return [{"name": t["name"], "description": t["description"].split(". ")[0].rstrip(".") + "."}
            for t in sorted(tools, key=lambda t: t["name"])]


def manifest(version, name, tools):
    if name == "macos":
        mcp_config = {"command": "${__dirname}/server/spring-vision", "args": []}
        platforms = ["darwin"]
    else:
        mcp_config = {
            "command": "${__dirname}/server/runtime/bin/java.exe",
            "args": STDIO_ARGS + ["-jar", "${__dirname}/server/" + JAR, "--spring.profiles.active=stdio"],
        }
        platforms = ["win32"]
    return {
        "manifest_version": "0.2",
        "name": "spring-vision",
        "display_name": "Spring Vision",
        "version": version,
        "description": "Computer vision on your own machine: OCR, identity documents and licence plates, faces, "
                       "emotions, hands, barcodes, image metadata and more.",
        "long_description": "Everything runs locally; no image leaves the machine. The models are downloaded once, "
                            "on the first run, into ~/.springvision/models. Until a tool's model is in place the tool "
                            "says so and does nothing; setup_status shows the progress. Requires "
                            + ("a Mac with Apple Silicon." if name == "macos" else "64-bit Windows."),
        "author": {"name": "Yilmaz Mustafa", "url": "https://github.com/codesapienbe"},
        "repository": {"type": "git", "url": "https://github.com/codesapienbe/spring-vision"},
        "license": "Apache-2.0",
        "keywords": ["vision", "ocr", "face", "barcode", "computer-vision"],
        "server": {"type": "binary", "entry_point": mcp_config["command"].replace("${__dirname}/", ""),
                   "mcp_config": mcp_config},
        "tools": tools,
        "compatibility": {"claude_desktop": ">=1.0.0", "platforms": platforms},
    }


def pack(folder, target, latest):
    """Zips the bundle folder with the zip tool, which keeps the executable bits, and copies it to latest."""
    target.unlink(missing_ok=True)
    subprocess.run(["zip", "-qrX", str(target), "manifest.json", "server"], cwd=folder, check=True)
    shutil.copyfile(target, latest)
    print(f"built {target.relative_to(ROOT)} ({target.stat().st_size // 1_048_576} MB), copied to {latest.name}")


def main():
    version, macos_jar, windows_jar, out = sys.argv[1], pathlib.Path(sys.argv[2]), pathlib.Path(sys.argv[3]), \
        pathlib.Path(sys.argv[4]).resolve()
    runtimes = json.loads((MCPB / "runtimes.json").read_text())
    cache = out / "cache"
    cache.mkdir(parents=True, exist_ok=True)
    macos = layout("macos", macos_jar, fetch_runtime(runtimes["macos"], cache), out)
    windows = layout("windows", windows_jar, fetch_runtime(runtimes["windows"], cache), out)
    if platform.system() != "Darwin" or platform.machine() != "arm64":
        sys.exit("build the extensions on an Apple Silicon Mac: the macOS bundle is started once as a check")
    tools = tools_of(macos)
    print(f"macOS bundle answered over stdio with {len(tools)} tools")
    for name, folder in (("macos", macos), ("windows", windows)):
        (folder / "manifest.json").write_text(json.dumps(manifest(version, name, tools), indent=2) + "\n")
        pack(folder, out / f"spring-vision-{version}-{name}.mcpb", out / f"spring-vision-latest-{name}.mcpb")


if __name__ == "__main__":
    main()
