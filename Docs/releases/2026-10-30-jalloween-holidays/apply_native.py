from pathlib import Path


template = (
    Path(__file__).resolve().parents[1]
    / "2026-10-30-test-jem-security-optimization"
    / "apply_native.py"
)
source = template.read_text(encoding="utf-8")

replacements = {
    '''installed_paths = {
    "jem-pack/manifest.json",
    "mods/jem-advancements-5.2.1.jar",
    "mods/jem_security-1.0.4.jar",
    "mods/jem-server-2.0.0-dev.jar",
    "mods/jem-settlements-2.1.0.jar",
    "mods/jem_adaptive_culling-1.5.0.jar",
}
removed_paths = {"mods/jem_adaptive_culling-1.4.1.jar"}''': '''installed_paths = {
    "jem-pack/manifest.json",
    "mods/AudioYT-1.0.1.jar",
    "mods/jem_holidays-1.0.3.jar",
    "mods/jem_luxium-1.0.9.jar",
    "resourcepacks/Halloween%20GUI%201.20.zip",
}
removed_paths = {
    "mods/jem_holidays-1.0.1.jar",
    "mods/jem_luxium-1.0.8.jar",
}''',
    '''Set<String> allowed=Set.of("jem-pack/manifest.json","mods/jem-advancements-5.2.1.jar","mods/jem_security-1.0.4.jar","mods/jem-server-2.0.0-dev.jar","mods/jem-settlements-2.1.0.jar","mods/jem_adaptive_culling-1.4.1.jar","mods/jem_adaptive_culling-1.5.0.jar");
  Set<String> removed=Set.of("mods/jem_adaptive_culling-1.4.1.jar");''': '''Set<String> allowed=Set.of("jem-pack/manifest.json","mods/jem_holidays-1.0.3.jar");
  Set<String> removed=Set.of();''',
    '''if(plan.operations().size()!=8)throw new IllegalStateException("Native plan operation count differs from reviewed preview");''': '''if(plan.operations().size()!=3)throw new IllegalStateException("Native plan operation count differs from reviewed preview");''',
    'jem-combined-head-': 'jem-jalloween-head-',
    'jem-combined-journal-': 'jem-jalloween-journal-',
    'local/jem-combined-native.jsh': 'local/jem-jalloween-native.jsh',
    'local/jem-combined-native.log': 'local/jem-jalloween-native.log',
    'Exact native combined installation complete': 'Exact native Jalloween installation complete',
}

for old, new in replacements.items():
    if old not in source:
        raise RuntimeError("Native installer template changed before Jalloween application")
    source = source.replace(old, new)

exec(compile(source, str(Path(__file__).resolve()), "exec"), {"__file__": __file__})
