"""Snapshot authored Blockbench work; never copy tutorial/reference artwork."""
from pathlib import Path
import hashlib
import json
import shutil

HERE = Path(__file__).resolve().parent
PROJECT = HERE.parent.parent
ENGINE = Path('C:/MyProjects/Coding/Solo/Ultimate-Skills/Taste-Engine')
DEST = PROJECT / 'Docs/races/assets/sandbox-workbench-sources'
roots = [ENGINE/'blockbench/models',
         ENGINE/'Pairs/Minecraft/Models/Reference/Experiments/Giant-gnome-2026-10-17/Models',
         ENGINE/'Pairs/Minecraft/VFX/Reference/I_dont_like/Rejected-R4-2026-10-17/Editor-studies']
models = sorted({p for root in roots for p in root.rglob('*.bbmodel')})
models += sorted(p for p in (PROJECT/'Docs/races/assets').rglob('*.bbmodel') if DEST not in p.parents)
chosen = set(models)
for model in models:
    for suffix in ('*.ora','*.png'):
        for file in model.parent.glob(suffix):
            if suffix == '*.ora' or not any(word in file.stem.lower() for word in ('desktop','front','back','left','right','hero','top','bottom','neutral','contact','comparison','montage','preview','render','sheet','frame','board')):
                chosen.add(file)
    for layers in model.parent.glob('*[Ll]ayers*'):
        if layers.is_dir(): chosen.update(layers.rglob('*.png'))
rows=[]
for source in sorted(chosen):
    prefix='Taste-Engine' if ENGINE in source.parents else 'JEM'
    relative=source.relative_to(ENGINE if prefix=='Taste-Engine' else PROJECT)
    target=DEST/prefix/relative
    target.parent.mkdir(parents=True,exist_ok=True)
    shutil.copy2(source,target)
    rows.append({'file':str(target.relative_to(DEST)).replace('\\','/'),'sha256':hashlib.sha256(source.read_bytes()).hexdigest(),'bytes':source.stat().st_size})
(DEST/'manifest.json').write_text(json.dumps({'models':len(models),'files':rows},ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
(DEST/'README.md').write_text('''# Editable sandbox source archive

Snapshot of authored Blockbench work, with embedded textures, external PNGs and
available layered ORA sources. Original tutorials and reference images are not
copied or changed. The manifest records every included file hash.

Active main skills: Ancestors shield R7; Void R6; Bloom coffer R4;
Sculk crawl R4; Nether R6. Bloom grasp R3 remains an additional study.
All other models are archived studies. In particular the ancestor fan, rejected
R4 and old shield paintings retain their rejection; bundling is not approval.

Open any .bbmodel in Blockbench to edit geometry/UV/animation. Edit .ora externally
and import its exported PNG; painting inside Blockbench remains prohibited.
The JAR embeds this archive at assets/jem_visual_sandbox/sources.zip.
''',encoding='utf-8')
print(json.dumps({'models':len(models),'files':len(rows),'bytes':sum(r['bytes'] for r in rows)}))
