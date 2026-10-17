"""Package the source-owned standalone mod and install only into jem sandbox."""
from pathlib import Path
import argparse,datetime,hashlib,json,os,shutil,subprocess
HERE=Path(__file__).resolve().parent

def digest(path):return hashlib.sha256(path.read_bytes()).hexdigest()

def compose():
    source=HERE/'build/libs/jem-races-1.1.0.jar'
    target=HERE/'build/distributions/jem-races-1.1.0.jar'
    target.parent.mkdir(parents=True,exist_ok=True)
    shutil.copy2(source,target)
    return target

def install(target,profile):
    if profile.name!='jem sandbox' or not (profile/'jem sandbox.json').is_file():raise ValueError('Only the explicitly authorized existing jem sandbox profile can be updated')
    shell=shutil.which('pwsh') or str(Path(os.environ['SystemRoot'])/'System32/WindowsPowerShell/v1.0/powershell.exe')
    processes=subprocess.run([shell,'-NoProfile','-Command',"Get-CimInstance Win32_Process | Where-Object { $_.Name -match '^javaw?\\.exe$' } | Select-Object -ExpandProperty CommandLine"],capture_output=True,text=True,check=True).stdout
    if str(profile).lower() in processes.lower() or 'jem sandbox' in processes.lower():raise RuntimeError('Close the running jem sandbox before replacing its mod')
    backup=profile/'visual-backups'/('races-complete-'+datetime.datetime.now().astimezone().strftime('%Y%m%d-%H%M%S'))
    backup.mkdir(parents=True,exist_ok=False)
    old=profile/'mods/jem-visual-sandbox-1.0.0.jar'
    installed=profile/'mods/jem-races-1.1.0.jar'
    previous=[]
    for p in [old,installed]:
        if p.exists():
            previous.append({'file':str(p),'sha256':digest(p)});shutil.move(str(p),str(backup/p.name))
    staging=installed.with_suffix('.jar.staged')
    shutil.copy2(target,staging);os.replace(staging,installed)
    record={'profile':str(profile),'installed':str(installed),'previous':previous,'backup':str(backup),'sha256':digest(installed),'installedAt':datetime.datetime.now().astimezone().isoformat(),'sourceOwner':'Mods/JEMVisualSandboxR3','gameLaunched':False,'testsRun':False,'checksRun':False,'scope':'Standalone JEM Races sandbox only; no canonical pack or dedicated server installation'}
    (backup/'installation.json').write_text(json.dumps(record,indent=2)+'\n',encoding='utf-8')
    (HERE/'build/installation.json').write_text(json.dumps(record,indent=2)+'\n',encoding='utf-8')
    return record

if __name__=='__main__':
    parser=argparse.ArgumentParser(description=__doc__);parser.add_argument('--profile',type=Path)
    args=parser.parse_args();artifact=compose();print(json.dumps(install(artifact,args.profile.resolve()) if args.profile else {'artifact':str(artifact),'sha256':digest(artifact)},indent=2))
