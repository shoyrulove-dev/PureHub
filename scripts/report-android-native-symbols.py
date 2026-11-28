"""List native library Build IDs and symbol metadata shipped in an AAB."""

import argparse
import re
import subprocess
import tempfile
from pathlib import Path
from zipfile import ZipFile


METADATA_PREFIX = "BUNDLE-METADATA/com.android.tools.build.debugsymbols/"


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("bundle", type=Path)
    parser.add_argument("--readelf", default="readelf")
    args = parser.parse_args()

    with ZipFile(args.bundle) as bundle, tempfile.TemporaryDirectory() as temporary:
        entries = bundle.namelist()
        libraries = sorted(name for name in entries if re.fullmatch(r"[^/]+/lib/[^/]+/[^/]+\.so", name))
        symbol_entries = sorted(name for name in entries if name.startswith(METADATA_PREFIX) and not name.endswith("/"))
        print(f"{args.bundle.name}: {len(libraries)} native libraries, {len(symbol_entries)} native debug metadata files")
        path = Path(temporary) / "library.so"
        for name in libraries:
            path.write_bytes(bundle.read(name))
            notes = subprocess.check_output([args.readelf, "-n", str(path)], text=True)
            match = re.search(r"Build ID:\s*([0-9a-fA-F]+)", notes)
            print(f"{name}\t{match.group(1) if match else 'Build ID unavailable'}")
        for name in symbol_entries:
            print(f"metadata\t{name}")
        if libraries and not symbol_entries:
            print("Native debug metadata is absent. Obtain matching symbols from the library provider.")


if __name__ == "__main__":
    main()
