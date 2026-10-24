"""Install the GUI gallery and exact native GUI-mod dependency closure to jem sandbox.

This profile is independent of AutoModpack. ManagedPack/test jem/Server/stable are
read-only inputs or outside this install boundary. Existing profile bytes are saved
before replacement, and the game must be closed.
"""
from pathlib import Path
from zipfile import ZipFile
from datetime import datetime, timezone, timedelta
import hashlib
import json
import os
import shutil
import subprocess
import tomllib

MODULE=Path(__file__).resolve().parent
ROOT=MODULE.parent.parent
PROFILE=Path(os.environ["APPDATA"])/".minecraft/versions/jem sandbox"
NATIVE=("curios","sophisticatedbackpacks","sophisticatedcore","sophisticatedstorage","trashslot","jei","betteradvancements","ftbquests","spudaciousshops","patchouli")


def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def main():
    profile=PROFILE.resolve(strict=True)
    if profile.name!="jem sandbox" or profile.parent.name!="versions":
        raise RuntimeError("Unexpected sandbox profile path")
    shell=shutil.which("pwsh") or str(Path(os.environ["SystemRoot"])/"System32/WindowsPowerShell/v1.0/powershell.exe")
    ps=subprocess.run([shell,"-NoProfile","-Command",
        "Get-CimInstance Win32_Process -Filter \"name='javaw.exe' OR name='java.exe'\" | Where-Object { $_.CommandLine -match '--gameDir' -and $_.CommandLine -match 'jem sandbox' } | Select-Object -ExpandProperty ProcessId"],capture_output=True,text=True,check=True,creationflags=subprocess.CREATE_NO_WINDOW)
    if ps.stdout.strip():
        raise RuntimeError("Close jem sandbox before installation. Active process: "+ps.stdout.strip())
    if (profile/"automodpack").exists() or any((profile/"mods").glob("*bootstrap*.jar")):
        raise RuntimeError("Sandbox became AutoModpack-managed; use its native installation owner")
    installed={}
    for path in (ROOT/"Client/ManagedPack/mods").glob("*.jar"):
        with ZipFile(path) as jar:
            if "META-INF/mods.toml" not in jar.namelist():
                continue
            data=tomllib.loads(jar.read("META-INF/mods.toml").decode())
            for mod in data.get("mods",[]):
                installed[mod["modId"]]=(path,data)
    seen=set();queue=list(NATIVE);artifacts={}
    while queue:
        mod=queue.pop(0)
        if mod in seen or mod in ("minecraft","forge"):
            continue
        if mod not in installed:
            raise RuntimeError("Required native dependency missing: "+mod)
        seen.add(mod);path,data=installed[mod];artifacts[path.name]=path
        for dependencies in data.get("dependencies",{}).values():
            queue.extend(dep["modId"] for dep in dependencies if dep.get("mandatory"))
    built=MODULE/"build/libs/jem-gui-sandbox-1.0.0.jar"
    if not built.is_file():
        raise RuntimeError("Build the reobfuscated gallery artifact first")
    artifacts[built.name]=built
    renderer=profile/"mods/jem-races-1.1.0-sandbox.jar"
    if not renderer.is_file():
        raise RuntimeError("Required existing race preview renderer missing")
    local_now=datetime.now(timezone(timedelta(hours=5)))
    backup=profile/"visual-backups"/("gui-gallery-"+local_now.strftime("%Y%m%d-%H%M%S"))
    entries=[]
    for name,source in artifacts.items():
        target=profile/"mods"/name
        if target.exists() and sha(target)==sha(source):
            entries.append({"file":name,"sha256":sha(source),"action":"already_current"});continue
        if target.exists():
            backup.mkdir(parents=True,exist_ok=True);shutil.copy2(target,backup/name)
        shutil.copy2(source,target)
        if sha(source)!=sha(target):
            raise RuntimeError("Installed bytes differ: "+name)
        entries.append({"file":name,"sha256":sha(target),"action":"installed"})
    receipt={"profile":str(profile),"date":local_now.isoformat(),"artifacts":entries,
             "existing_preview_renderer":{"file":renderer.name,"sha256":sha(renderer)},
             "native_mod_ids":sorted(seen),"game_launched":False,"runtime_gui_review":"unobserved",
             "production_server":"unchanged","managed_pack":"unchanged","automodpack_publication":"unchanged"}
    out=ROOT/"Docs/gui-sandbox/delivery.json";out.parent.mkdir(parents=True,exist_ok=True)
    out.write_text(json.dumps(receipt,indent=2)+"\n",encoding="utf-8")
    print(json.dumps({"profile":str(profile),"artifacts":len(entries),"receipt":str(out)}))


if __name__=="__main__":
    main()
