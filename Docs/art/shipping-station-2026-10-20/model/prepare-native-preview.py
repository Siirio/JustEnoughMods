"""Restore native reference textures from the user's own Minecraft1.20.1 JAR.

Usage: python prepare-native-preview.py ABSOLUTE_CLIENT_JAR
Writes only ignored local reference inputs and an ignored Blockbench preview.
No game launch, no gameplay/runtime change, no painting or resampling.
"""
from pathlib import Path
import base64,json,sys,zipfile

root=Path(__file__).resolve().parent
assets={
    'diamond':'item/diamond', 'cod':'item/cod', 'salmon':'item/salmon',
    'block_oak_log':'block/oak_log', 'oak_log_top':'block/oak_log_top',
}
with zipfile.ZipFile(Path(sys.argv[1])) as jar:
    model=json.loads((root/'shipping-station-goods-preview.bbmodel').read_text(encoding='utf-8'))
    (root/'native-items').mkdir(exist_ok=True)
    for tex in model['textures']:
        name=tex['name']
        if name not in assets:continue
        data=jar.read('assets/minecraft/textures/'+assets[name]+'.png')
        path=root/'native-items'/(name+'.png')
        path.write_bytes(data)
        tex['path']=str(path)
        tex['source']='data:image/png;base64,'+base64.b64encode(data).decode('ascii')
    (root/'shipping-station-goods-local.bbmodel').write_text(json.dumps(model,indent=2)+'\n',encoding='utf-8')
print('Native local preview prepared. Open shipping-station-goods-local.bbmodel in Blockbench.')
