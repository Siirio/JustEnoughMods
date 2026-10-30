from pathlib import Path


template = (
    Path(__file__).resolve().parents[1]
    / "2026-10-30-jalloween-production"
    / "apply_native.py"
).read_text(encoding="utf-8")

replacements = {
    'profile = Path("C:/Users/user/AppData/Roaming/.minecraft/versions/test jem")':
        'profile = Path("C:/Users/user/AppData/Roaming/.minecraft/versions/JustEnoughMods")',
    '''new_files = {
    "mods/jem-advancements-5.2.2.jar",
    "mods/jem_holidays-1.0.5.jar",
    "kubejs/client_scripts/jem/jalloween_emf_ownership.js",
}''': '''new_files = {
    "mods/jem-races-2.0.1.jar",
    "mods/jem_holidays-1.0.6.jar",
    "mods/jem_security-1.0.5.jar",
}''',
    '''old_files = {
    "mods/jem-advancements-5.2.1.jar",
    "mods/jem_holidays-1.0.4.jar",
}''': '''old_files = {
    "mods/jem_holidays-1.0.5.jar",
    "mods/jem_security-1.0.4.jar",
}''',
    "CommandLine.Contains('versions\\\\test jem')":
        "CommandLine.Contains('versions\\\\JustEnoughMods')",
    'raise SystemExit("Close test jem before native installation: " + processes.stdout.strip())':
        'raise SystemExit("Close JustEnoughMods before native installation: " + processes.stdout.strip())',
    'libraries = json.loads((profile / "test jem.json").read_text())["libraries"]':
        'libraries = json.loads((profile / "JustEnoughMods.json").read_text())["libraries"]',
    'Path serverRoot=Path.of("C:/MyProjects/Minecraft/JustEnoughMods/Server/automodpack/server");':
        'Path serverRoot=Path.of("C:/MyProjects/Minecraft/JustEnoughMods/Server/local/remote-production-2f43a3b");',
    'Set<String> allowed=Set.of("jem-pack/manifest.json","kubejs/client_scripts/jem/jalloween_emf_ownership.js","mods/jem-advancements-5.2.1.jar","mods/jem-advancements-5.2.2.jar","mods/jem_holidays-1.0.4.jar","mods/jem_holidays-1.0.5.jar");':
        'Set<String> allowed=Set.of("jem-pack/manifest.json","mods/jem-races-2.0.1.jar","mods/jem_holidays-1.0.5.jar","mods/jem_holidays-1.0.6.jar","mods/jem_security-1.0.4.jar","mods/jem_security-1.0.5.jar");',
    'jshell_script = server / "local/jalloween-production-native.jsh"':
        'jshell_script = server / "local/login-hotfix-native.jsh"',
    'jshell_log = server / "local/jalloween-production-native.log"':
        'jshell_log = server / "local/login-hotfix-native.log"',
    '(server / "automodpack/server/current-projection.json").read_text()':
        '(server / "local/remote-production-2f43a3b/current-projection.json").read_text()',
    'Exact native Jalloween installation complete with matching token, files and no pending transaction':
        'Exact native production hotfix installation complete with matching token, files and no pending transaction',
}

for old, new in replacements.items():
    if template.count(old) != 1:
        raise RuntimeError("Native installer template drift: " + old)
    template = template.replace(old, new)

exec(compile(template, str(Path(__file__)), "exec"), {"__file__": str(Path(__file__))})
