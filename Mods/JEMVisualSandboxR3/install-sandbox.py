"""Install a complete sandbox artifact only while its Minecraft profile is closed."""
from pathlib import Path
import datetime
import hashlib
import json
import os
import shutil
import subprocess
import tempfile
import zipfile


def require_closed(profile):
    command = "Get-CimInstance Win32_Process -Filter \"Name = 'javaw.exe' OR Name = 'java.exe'\" | Select-Object ProcessId,CommandLine | ConvertTo-Json -Compress"
    result = subprocess.run([str(Path(os.environ['SystemRoot']) / 'System32/WindowsPowerShell/v1.0/powershell.exe'), '-NoProfile', '-Command', command],
                            capture_output=True, text=True, check=True)
    rows = json.loads(result.stdout) if result.stdout.strip() else []
    if isinstance(rows, dict):
        rows = [rows]
    needle = str(profile).replace('\\', '/').lower()
    for row in rows:
        command_line = row.get('CommandLine')
        if command_line is None:
            raise SystemExit('Cannot inspect a Java process; installation refused.')
        if needle in command_line.replace('\\', '/').lower():
            raise SystemExit(f'Close jem sandbox first (Java PID {row["ProcessId"]}); installation refused.')


def main():
    module = Path(__file__).resolve().parent
    source = module / 'build/sandbox-profile/libs/jem-races-1.1.0-sandbox.jar'
    profile = Path(os.environ['APPDATA']) / '.minecraft/versions/jem sandbox'
    require_closed(profile)
    with zipfile.ZipFile(source) as archive:
        assert archive.testzip() is None, 'Corrupt artifact'
        assert 'JEM-Race-Profile: SANDBOX' in archive.read('META-INF/MANIFEST.MF').decode()
        for name in ['WorkbenchMesh', 'RaceOwnedEffects', 'RaceOwnedEffects$Grant',
                     'AbilityPreviewPresentation', 'RaceDevelopment', 'VisualRenderContext', 'RacePowerStages', 'SculkDomain']:
            assert f'com/siirio/jemvisualsandbox/workbench/{name}.class' in archive.namelist(), name
    with zipfile.ZipFile(source) as archive:
        for name in ['SculkPlayerScaleMixin', 'TemporarySculkGrowthMixin']:
            assert f'com/siirio/jemvisualsandbox/mixin/{name}.class' in archive.namelist(), name
    mods = profile / 'mods'
    previous = []
    for path in mods.glob('*.jar'):
        with zipfile.ZipFile(path) as archive:
            if 'META-INF/mods.toml' in archive.namelist():
                import tomllib
                entries = tomllib.loads(archive.read('META-INF/mods.toml').decode()).get('mods', [])
                if any(entry.get('modId') == 'jem_visual_sandbox' for entry in entries):
                    previous.append(path)
    backup = profile / 'visual-backups' / datetime.datetime.now().strftime('races-closed-install-%Y%m%d-%H%M%S-%f')
    backup.mkdir(parents=True)
    for path in previous:
        shutil.copy2(path, backup / path.name)
    target = mods / source.name
    descriptor, temporary = tempfile.mkstemp(prefix='.races-', suffix='.tmp', dir=mods)
    os.close(descriptor)
    try:
        shutil.copy2(source, temporary)
        require_closed(profile)
        os.replace(temporary, target)
        for path in previous:
            if path != target:
                path.unlink()
    finally:
        Path(temporary).unlink(missing_ok=True)
    digest = hashlib.sha256(target.read_bytes()).hexdigest()
    assert digest == hashlib.sha256(source.read_bytes()).hexdigest()
    print(json.dumps({'installed': str(target), 'sha256': digest, 'backup': str(backup),
                      'profileClosedAtInstall': True, 'atomicReplacement': True}))


if __name__ == '__main__':
    main()
