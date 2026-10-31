from pathlib import Path


template = Path(__file__).resolve().parents[1] / "2026-10-31-audioyt-jukebox-scan/apply_native.py"
source = template.read_text(encoding="utf-8")
replacements = {
    "59362e3ecea6929a8321ebefb2014dffcf3575b747e992df4d1ce86c4236e133":
        "58e33853523dc4c71ca95a45d504d9921f037816e9e73953fe576288b5b54a77",
    "3.10.1": "3.10.2",
    "AudioYt": "SpawnerMagnet",
    "AudioYT": "spawner and Magnet",
    "audioyt": "spawner-magnet",
}
for old, new in replacements.items():
    source = source.replace(old, new)

exec(compile(source, str(Path(__file__).resolve()), "exec"), {
    "__file__": str(Path(__file__).resolve()),
    "__name__": "__main__",
})
