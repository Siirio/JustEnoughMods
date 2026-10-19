"""Labelled offline art composition only. Does not emulate game state or capture runtime."""
from pathlib import Path
from PIL import Image
import importlib.util, sys
sys.dont_write_bytecode=True

ROOT=Path(__file__).resolve().parent
spec=importlib.util.spec_from_file_location('existing_art_export',ROOT.parent/'control-family-r1/export_family.py')
module=importlib.util.module_from_spec(spec);spec.loader.exec_module(module)
text=module.NativeText(sys.argv[1])
OUT=ROOT.parents[4]/'Mods/JEMSettlements/src/main/resources/assets/jem_settlements/textures/gui/dashboard'
sprites={p.stem:Image.open(p).convert('RGBA') for p in OUT.glob('*.png')}
canvas=Image.new('RGBA',(600,382),'#1c2028')
def surface(name,x,y,w,h):canvas.alpha_composite(module.tiled_resize(sprites[name],(w,h),5),(x,y))
def label(s,x,y,color='#342b26'):text.draw(canvas,s,x,y,color=color)
surface('stone_frame',8,34,584,334)
surface('paper_frame',130,41,455,319)
surface('wood_frame',115,14,400,48)
surface('paper_frame',166,19,298,38)
for x in [145,469]:canvas.alpha_composite(sprites['banner'],(x,22))
for x in [124,490]:canvas.alpha_composite(sprites['lantern'],(x,25))
for x in [110,482]:canvas.alpha_composite(sprites['vine'],(x,0))
label('Поселение',258,32)
for i,s in enumerate(['Поселение','Жители','Счастье','Сундуки']):
    surface('control_selected' if i==0 else 'control',14,68+i*44,110,40)
    label(s,28,84+i*44,'#fff4dd')
for i,(s,v,mood) in enumerate([('Жители','4 / 10','happy'),('Счастье','19 / 100','sad'),('Красота','40 / 100','neutral')]):
    surface('card',140+i*144,70,140,64)
    canvas.alpha_composite(sprites['mood_'+mood],(149+i*144,85))
    label(s,171+i*144,82);label(v,171+i*144,103)
surface('dark_frame',140,143,432,83)
label('Казна поселения',155,154,'#fff0ce')
label('Текущий баланс',155,177,'#b9bbc9');label('257',155,194,'#f6cb5d')
label('Доход от жителей',330,177,'#b9bbc9');label('+32 / день',330,194,'#a7d978')
surface('paper_frame',140,235,432,83)
label('Обустройство',155,246)
for i,s in enumerate(['Зелень','Дорожки','Освещение']):
    surface('card',149+i*138,261,132,48);label(s,160+i*138,272);label('3 / 4',160+i*138,289)
surface('control',140,327,432,26);label('Название поселения',155,336,'#fff2dd')
label('OFFLINE MATERIAL COMPOSITION / NO GAME CAPTURE',12,372,'#ffffff')
canvas.save(ROOT/'materials-native1x.png')
canvas.resize((1200,764),Image.Resampling.NEAREST).save(ROOT/'materials-nearest2x.png')
print('Authored labelled offline material composition; no runtime evidence.')
