"""Package the saved Java item export and direct Aseprite PNG without pixel conversion."""
from pathlib import Path
import argparse
import json
import zipfile

parser = argparse.ArgumentParser()
parser.add_argument('--output', required=True, type=Path)
args = parser.parse_args()
source = Path(__file__).resolve().parent
entries = {
    'pack.mcmeta': json.dumps({'pack': {'pack_format': 15, 'description': 'JEM: volumetric Building But Better copper hammer'}}).encode(),
    'assets/bbb/models/item/hammer.json': (source/'hammer-final.json').read_bytes(),
    'assets/bbb/textures/item/hammer_3d.png': (source/'hammer.png').read_bytes(),
}
args.output.parent.mkdir(parents=True, exist_ok=True)
with zipfile.ZipFile(args.output, 'w') as archive:
    for name, content in entries.items():
        entry = zipfile.ZipInfo(name, date_time=(2026, 10, 21, 0, 0, 0))
        entry.compress_type = zipfile.ZIP_DEFLATED
        archive.writestr(entry, content)
