"""Author policies from exact registry/capture evidence. Does not launch or test Minecraft.
Historical capture is explicitly labelled; unknown classes/recipes remain NEEDS_REVIEW.
"""
import csv,json,re,collections,sys
from pathlib import Path
ROOT=Path(__file__).resolve().parents[3]
DATA=ROOT/'Mods/JEMEconomy/src/main/resources/data/jem_economy/economy'
DOC=ROOT/'Docs/economy'
def read(p): return json.loads(p.read_text(encoding='utf-8'))
DECOR_CLASSES={
 'earth.terrarium.handcrafted.common.blocks.BenchBlock':('DECOR_BUILD',140,'Handcrafted3.0.6 BenchBlock/ModularSeatBlock: native seat/color/shape only; exact bytecode has no item/fluid/energy storage or block entity.'),
 'earth.terrarium.handcrafted.common.blocks.trims.CornerTrimBlock':('DECOR_BUILD',140,'Handcrafted3.0.6: SimpleBlock directional/waterlogging; only shape/state Hammerable methods; no inventory/BE/energy.'),
 'earth.terrarium.handcrafted.common.blocks.trims.PillarTrimBlock':('DECOR_BUILD',140,'Handcrafted3.0.6: directional/waterlogged shape trim; no inventory/BE/energy.'),
 'earth.terrarium.handcrafted.common.blocks.misc.PotBlock':('DECOR_BUILD',140,'Handcrafted3.0.6 exact class overrides only shape/size.'),
 'earth.terrarium.handcrafted.common.blocks.misc.StackableJarBlock':('DECOR_BUILD',140,'Handcrafted3.0.6 exact class overrides only native stacking/placement/shape.')}
def technical_class(row,policy,protected):
 i=row.get('id','');block=row.get('block')
 if i in protected:return 'PROGRESSION_PROTECTED','Exact permanent progression/service/trophy restriction'
 if any(row.get(k) for k in ['itemInventoryCapability','itemFluidCapability','itemEnergyCapability']):return 'FUNCTIONAL_RESOURCE_OR_MACHINE','Actual loaded ItemStack capability'
 if block:
  if block.get('hasBlockEntity') or block.get('entityBlock') or block.get('redstoneSource'):
   return 'FUNCTIONAL_RESOURCE_OR_MACHINE','Loaded block entity/registered BE type or redstone source; stricter functional class wins'
  if block.get('class') in DECOR_CLASSES:
   rank,price,why=DECOR_CLASSES[block['class']];return policy.get('building_class',rank) if policy else rank,why
 if policy and policy.get('outcome')!='NONE':
  if policy.get('demand_group')=='building_supply':
   return policy.get('building_class',('TERRAIN_FILL' if i in ['minecraft:dirt','minecraft:cobblestone','minecraft:stone','minecraft:sand','minecraft:gravel','minecraft:grass_block'] else 'COMMON_BUILD')),policy.get('notes','Exact reviewed building tier')
  return 'FUNCTIONAL_RESOURCE_OR_MACHINE',policy.get('notes','Reviewed commodity; no subsidized building inference')
 return 'NEEDS_REVIEW','Absent current loaded technical/recipe proof; no category or price inferred from namespace/name'

def main():
 capture=read(ROOT/'Server/local/economy-audit/loaded-registry.json')
 loaded={r['id']:r for r in capture['items']}
 atlas={r['registry_id']:r for f in (ROOT/'Catalog/registries/items').glob('*.json') for r in read(f)['records']}
 policies={r['item']:r for r in read(DATA/'item_policies.json')}
 applied_overrides={i:dict(r) for i,r in policies.items() if r.get('recipe_audit_adjusted')}
 protected=set(read(DATA/'trade_restrictions.json')['all_npc_items'])
 def policy(i,sell,buy=None,den=1,group='mining',family=None,units=1,player=256,glob=8192,notes=''):
  if i not in loaded or (i not in atlas and capture.get('schema')!=2) or i in protected: return
  policies[i]=dict(schema=2,item=i,outcome='BUY_AND_SELL' if buy else 'SELL_ONLY',sell_copper=sell,buy_copper=buy,
   sell_denominator=den,buy_denominator=den,daily_player_units=player,daily_global_units=glob,demand_group=group,
   quota_family=family or i,quota_units=units,notes=notes)
 # Explicit agreed resource anchors; reversible storage forms use exact recipe proof below.
 anchors={'oak_log':(15,70),'coal':(20,100),'copper_ingot':(40,200),'iron_ingot':(100,500),
 'gold_ingot':(140,700),'redstone':(40,200),'lapis_lazuli':(40,200),'quartz':(40,200),
 'amethyst_shard':(50,250),'diamond':(1000,3000)}
 for path,(sell,buy) in anchors.items():
  policy('minecraft:'+path,sell,buy,64,'forestry' if path=='oak_log' else 'mining',notes='Authorized batch anchor per64; exact rational settlement.')
 policy('create:zinc_ingot',60,300,64,notes='Create6.0.8 registered ingot; batch anchor.')
 policy('minecraft:netherite_ingot',600,notes='Progression purchase remains closed.')
 # Exact recipe compression/decompression, no suffix or namespace admission.
 recipes=capture['recipes']; reverse={}
 for r in recipes:
  ing=r.get('ingredients',[])
  if r.get('count')==9 and len(ing)==1 and len(ing[0])==1: reverse[(r['output'],ing[0][0])]=r
 for r in recipes:
  ing=r.get('ingredients',[])
  if r.get('count')!=1 or len(ing)!=9 or not all(len(x)==1 and x==ing[0] for x in ing):continue
  unit=ing[0][0]; block=r['output']; base=policies.get(unit)
  if (unit,block) not in reverse or not base or base.get('demand_group')!='mining':continue
  policy(block,base['sell_copper']*9,base.get('buy_copper')*9 if base.get('buy_copper') else None,
   base.get('sell_denominator',1),'mining',unit,9,base['daily_player_units'],base['daily_global_units'],
   'Exact reversible9:1 recipes '+r['id']+' / '+reverse[(unit,block)]['id'])
  # Sale and purchase denominators are independent after buyback adjustment.
  if block in policies:policies[block]['buy_denominator']=base.get('buy_denominator',1)
 # Proven ore tags: only known material family. Unknown ores remain review, no inferred price.
 materials={'coal':'minecraft:coal','copper':'minecraft:copper_ingot','iron':'minecraft:iron_ingot',
 'gold':'minecraft:gold_ingot','redstone':'minecraft:redstone','lapis':'minecraft:lapis_lazuli',
 'quartz':'minecraft:quartz','diamond':'minecraft:diamond','zinc':'create:zinc_ingot'}
 for i,row in loaded.items():
  if (i not in atlas and capture.get('schema')!=2) or i in protected: continue
  tags=' '.join(row.get('tags',[]))
  matched=next((base for mat,base in materials.items() if 'forge:ores/'+mat+']' in tags or 'forge:raw_materials/'+mat+']' in tags),None)
  if matched and matched in policies:
   base=policies[matched]
   policy(i,base['sell_copper'],None,base.get('sell_denominator',1),'mining',matched,1,
    base['daily_player_units'],base['daily_global_units'],'Exact Forge material tag; conservative base yield, no NPC ore purchase.')
  if row.get('class')=='net.minecraft.world.item.RecordItem' or 'minecraft:music_discs]' in tags:
   policy(i,10000,None,1,'music_discs','jem_economy:music_discs',1,1,40,'Historical loaded RecordItem/jukebox tag; fixed price, shared disc quota.')
   if i in policies: policies[i]['fixed_price']=True
  elif 'nutrition' in row:
   if i not in policies or policies[i].get('outcome')=='NONE': policy(i,max(1,min(8,row['nutrition']//2)),None,1,'cooking',player=64,glob=2048,notes='Observed FoodProperties in loaded capture; arbitrary NBT denied.')
  elif any(tag in tags for tag in ('forge:seeds]', 'minecraft:seeds]', 'forge:crops]')):
   if i not in policies:policy(i,1,4,1,'crops',player=128,glob=4096,notes='Exact crop/seed tag from loaded capture.')
 # Seeds also buyable when FoodProperties caused the previous branch.
 for i,row in loaded.items():
  if any(tag in ' '.join(row.get('tags',[])) for tag in ('forge:seeds]', 'minecraft:seeds]')) and i in policies:
   r=policies[i];r['outcome']='BUY_AND_SELL';r['buy_copper']=max(4,r['sell_copper']*4);r['buy_denominator']=r.get('sell_denominator',1)
 for i,sell in [('minecraft:cod',3),('minecraft:salmon',4),('minecraft:beef',4),('minecraft:leather',3)]:
  policy(i,sell,max(sell*4,12),1,'fishing' if i.endswith(('cod','salmon')) else 'animal_products',player=32 if i.endswith(('cod','salmon')) else 64,glob=1280 if i.endswith(('cod','salmon')) else 2048,notes='Exact approved income anchor.')
 # Exact agrarian batch anchors replace legacy per-item inflation.
 for i,sell,buy,den in [('minecraft:wheat',8,40,64),('minecraft:carrot',6,30,64),('minecraft:potato',6,30,64),
 ('minecraft:apple',8,40,64),('minecraft:sweet_berries',5,20,64),('minecraft:glow_berries',6,25,64),
 ('minecraft:bread',1,4,1),('minecraft:cooked_beef',2,8,1),('minecraft:cooked_porkchop',2,8,1),
 ('minecraft:cooked_chicken',1,6,1),('minecraft:cooked_mutton',1,6,1),('minecraft:cooked_cod',1,6,1),('minecraft:cooked_salmon',2,8,1)]:
  policy(i,sell,buy,den,'cooking' if den==1 else 'crops',player=128,glob=4096,notes='Authorized agrarian/food batch anchor; native counts unchanged.')
 for i,row in loaded.items():
  if (i not in atlas and capture.get('schema')!=2) or i in protected:continue
  if any(tag in ' '.join(row.get('tags',[])) for tag in ('forge:seeds]', 'minecraft:seeds]')):
   policy(i,1,4,8,'crops',player=256,glob=8192,notes='Seed batch8 sell1/buy4; exact observed seed tag.')
 tiers={'dirt':(3,40),'cobblestone':(3,40),'stone':(4,60),'sand':(5,70),'gravel':(5,70),
 'grass_block':(5,80),'smooth_stone':(6,90),'stone_bricks':(7,100),'terracotta':(7,100),'glass':(7,100),'bricks':(12,180)}
 for path,(stack,bulk) in tiers.items():
  i='minecraft:'+path
  if i not in loaded or (i not in atlas and capture.get('schema')!=2):continue
  policies[i]=dict(schema=2,item=i,outcome='BUY_ONLY',sell_copper=None,buy_copper=stack,buy_denominator=64,
   bulk_1024=bulk,daily_player_units=65536,daily_global_units=262144,demand_group='building_supply',notes='Exact approved building tier; derivative graph review required before discounted bulk publication.')
 # Rebuild reversible families outward from immutable anchors. A legacy nugget must never reprice an ingot.
 family_anchors={'minecraft:'+k:v for k,v in anchors.items() if k!='oak_log'}
 family_anchors['create:zinc_ingot']=(60,300)
 for base_id,(sell,buy) in family_anchors.items():
  if base_id not in policies:continue
  policy(base_id,sell,buy,64,notes='Immutable approved resource anchor per64; exact recipe family.')
  nugget=next((r['output'] for r in recipes if r.get('count')==9 and r.get('ingredients')==[[base_id]]
       and (r['output'],base_id) in reverse and any(c.get('output')==base_id and len(c.get('ingredients',[]))==9
       and all(g==[r['output']] for g in c['ingredients']) for c in recipes)),None)
  scale=9 if nugget else 1
  base=policies[base_id];base.update(quota_family=base_id,quota_units=scale,daily_player_units=256*scale,daily_global_units=8192*scale)
  if nugget:
   policy(nugget,sell,buy,64*9,'mining',base_id,1,256*scale,8192*scale,'Exact9-nugget reciprocal recipe; anchor is the ingot, never the legacy nugget.')
  members={base_id}
  for item,row in loaded.items():
   tags=' '.join(row.get('tags',[]))
   if any(f'forge:{kind}/{mat}]' in tags for mat,canonical in materials.items() if canonical==base_id for kind in ('ores','raw_materials')):
    policy(item,sell,None,64,'mining',base_id,scale,256*scale,8192*scale,'Exact tagged material; conservative base yield and common family quota.')
    members.add(item)
  for recipe in recipes:
   groups=recipe.get('ingredients',[])
   if recipe.get('count')!=1 or len(groups)!=9 or not all(g==groups[0] and len(g)==1 for g in groups):continue
   unit=groups[0][0];block=recipe['output']
   if unit not in members or (unit,block) not in reverse:continue
   unit_policy=policies[unit]
   policy(block,sell*9,buy*9 if unit_policy.get('buy_copper') else None,64,'mining',base_id,scale*9,256*scale,8192*scale,
    'Exact reciprocal9:1 recipes; immutable material anchor and normalized shared quota.')
 # Nonreversible quartz/amethyst4:1 crafts are explicit distinct source-backed rules, never guessed9:1.
 for material,block in [('minecraft:quartz','minecraft:quartz_block'),('minecraft:amethyst_shard','minecraft:amethyst_block')]:
  recipe=next((r for r in recipes if r.get('output')==block and r.get('count')==1 and r.get('ingredients')==[[material]]*4),None)
  if recipe and material in policies:
   base=policies[material]
   policy(block,base['sell_copper']*4,None,base.get('sell_denominator',1),'mining',base.get('quota_family',material),base.get('quota_units',1)*4,
    base['daily_player_units'],base['daily_global_units'],'Exact one-way4:1 recipe '+recipe['id']+'; no invented reverse recipe or NPC block purchase.')
 # Actual Create crushed-material tag plus exact installed smelting output defines processed mineral admission.
 for i,row in loaded.items():
  if 'create:crushed_raw_materials]' not in ' '.join(row.get('tags',[])):continue
  recipe=next((r for r in recipes if r.get('type') in ['minecraft:smelting','minecraft:blasting']
    and r.get('ingredients')==[[i]] and r.get('output') in family_anchors and r.get('count',0)>0),None)
  if recipe:
   base=policies[recipe['output']];count=recipe['count']
   policy(i,base['sell_copper']*count,None,base.get('sell_denominator',1),'mining',base.get('quota_family',recipe['output']),base.get('quota_units',1)*count,
    base['daily_player_units'],base['daily_global_units'],'Actual create:crushed_raw_materials tag; exact smelting '+recipe['id']+' output '+str(count)+'; purchase remains closed.')
 # Exact current technical classes and actual recipe ingredients admit subsidized decor, never a mod namespace.
 if capture.get('schema')==2:
  native_shapes={'net.minecraft.world.level.block.Block','net.minecraft.world.level.block.SlabBlock',
   'net.minecraft.world.level.block.StairBlock','net.minecraft.world.level.block.WallBlock',
   'net.minecraft.world.level.block.GlassBlock','net.minecraft.world.level.block.HalfTransparentBlock',
   'net.minecraft.world.level.block.StainedGlassBlock','net.minecraft.world.level.block.StainedGlassPaneBlock',
   'net.minecraft.world.level.block.CarpetBlock','net.minecraft.world.level.block.WoolCarpetBlock',
   'net.minecraft.world.level.block.GlazedTerracottaBlock','net.minecraft.world.level.block.ConcretePowderBlock'}
  primitives={'minecraft:'+i for i in ['dirt','cobblestone','stone','andesite','diorite','granite','tuff','calcite',
   'clay_ball','brick','glass','sand','gravel','terracotta','string','stick','white_wool']}
  material_rank={i:100 for i in primitives}
  for i,row in loaded.items():
   tags=' '.join(row.get('tags',[]))
   if any(t in tags for t in ['minecraft:planks]','minecraft:logs]','minecraft:wooden_slabs]']):material_rank[i]=140
   if 'forge:dyes' in tags:material_rank[i]=180
   if policies.get(i,{}).get('demand_group')=='mining' and policies.get(i,{}).get('outcome')=='BUY_AND_SELL':material_rank[i]=400
  for stage in range(12):
   added=0
   for r in recipes:
    i=r.get('output');row=loaded.get(i,{});block=row.get('block',{});groups=r.get('ingredients',[])
    if not block or i in protected or i in family_anchors or i=='minecraft:oak_log' or i in ["minecraft:"+p for p in tiers]:continue
    if i in policies and policies[i].get('demand_group')=='building_supply' and policies[i].get('bulk_1024') is not None:continue
    cls=block.get('class');tags=' '.join(row.get('tags',[]))
    if cls not in native_shapes and cls not in DECOR_CLASSES:continue
    if block.get('hasBlockEntity') or block.get('entityBlock') or block.get('redstoneSource') or any(row.get(k) for k in ['itemInventoryCapability','itemFluidCapability','itemEnergyCapability']):continue
    if any(t in tags for t in ['forge:storage_blocks','forge:ores','minecraft:logs]','minecraft:planks]','minecraft:saplings]']):continue
    if not groups or any(not any(candidate in material_rank for candidate in g) for g in groups) or r.get('count',0)<=0:continue
    tier=max(min(material_rank[c] for c in g if c in material_rank) for g in groups)
    if cls in DECOR_CLASSES:tier=max(140,tier)
    rank='PREMIUM_DECOR' if tier>=400 else 'DECOR_BUILD' if tier>=140 else 'COMMON_BUILD'
    policies[i]=dict(schema=2,item=i,outcome='BUY_ONLY',sell_copper=None,buy_copper=(tier+15)//16,buy_denominator=64,
     bulk_1024=tier,daily_player_units=65536,daily_global_units=262144,demand_group='building_supply',building_class=rank,
     notes='Current loaded exact nonfunctional class '+cls+'; actual recipe '+r['id']+' output '+str(r['count'])+'; family material tier '+str(tier)+'. No BE/redstone/capability/progression inference from names.')
    material_rank[i]=tier;added+=1
   if not added:break
 if 'minecraft:oak_log' in policies:policies['minecraft:oak_log']['bulk_1024']=1120
 for item in ['minecraft:diamond','minecraft:diamond_block']:
  if item in policies:policies[item]['fixed_price']=True
 # Native planks use the existing reviewed wood material tier instead of per-item fallback.
 for i,row in loaded.items():
  block=row.get('block',{})
  if 'minecraft:planks]' not in ' '.join(row.get('tags',[])) or block.get('class')!='net.minecraft.world.level.block.Block':continue
  if block.get('hasBlockEntity') or block.get('entityBlock') or block.get('redstoneSource') or any(row.get(k) for k in ['itemInventoryCapability','itemFluidCapability','itemEnergyCapability']):continue
  policies[i]=dict(schema=2,item=i,outcome='BUY_ONLY',sell_copper=None,buy_copper=9,buy_denominator=64,bulk_1024=140,daily_player_units=65536,daily_global_units=262144,demand_group='building_supply',notes='Exact native plank tag and ordinary Block class; existing wood material tariff140/1024, no buyback.')
 # Reviewed current recipe decisions survive authoring; canonical rows remain the only price owner.
 for i,previous in applied_overrides.items():
  if i in ['minecraft:diamond','minecraft:diamond_block']:continue
  if i in policies:
   for key in ['sell_copper','sell_denominator','outcome','recipe_audit_adjusted']:
    if key in previous:policies[i][key]=previous[key]
 # Explicit2026-10-21 user decisions are immutable income anchors, applied after historical recipe adjustments.
 for i,(n,d) in {'minecraft:diamond':(1000,64),'minecraft:diamond_block':(9000,64),'minecraft:wheat':(10,64),'minecraft:carrot':(8,64),'minecraft:beetroot':(80,64)}.items():
  policies[i].update(sell_copper=n,sell_denominator=d,user_income_anchor=True)
  policies[i].pop('recipe_audit_adjusted',None)
 for i,p in policies.items():
  if p.get('quota_family')=='minecraft:gold_ingot':
   from fractions import Fraction
   price=Fraction(54*p.get('quota_units',1),64*9)
   p.update(sell_copper=price.numerator,sell_denominator=price.denominator,user_income_anchor=True)
   p.pop('recipe_audit_adjusted',None)
 if '--audit-only' in sys.argv:policies={r['item']:r for r in read(DATA/'item_policies.json')}
 else:(DATA/'item_policies.json').write_text(json.dumps(sorted(policies.values(),key=lambda r:r['item']),ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
 certificate=read(DOC/'shipping-monetary-potential-certificate.json') if (DOC/'shipping-monetary-potential-certificate.json').exists() else {}
 fields='mod_id,item_id,display_name,class,why,source_registry,source_recipe,recipe_output_count,base_material,effective_bulk_price_1024,price_x64,price_x256,price_x4096,buy_allowed,sell_allowed,arbitrage_status,requires_manual_review'.split(',')
 listings_by={r['stack']['item']:r for r in capture.get('marketListings',[])}
 recipes_by={}
 for r in recipes:recipes_by.setdefault(r.get('output'),[]).append(r)
 with (DOC/'shipping-policy-audit.csv').open('w',encoding='utf-8',newline='') as out:
  writer=csv.DictWriter(out,fieldnames=fields);writer.writeheader()
  audit_records=dict(atlas)
  for i,row in loaded.items():audit_records.setdefault(i,{'display_name_en_us':row.get('name',''),'current_capture_only':True})
  for i,record in sorted(audit_records.items()):
   p=policies.get(i); rs=recipes_by.get(i,[])
   listing=listings_by.get(i)
   if (not p or p.get('outcome')=='NONE') and listing and listing.get('outcome') in ['BUY_ONLY','BUY_AND_SELL']:
    p={'item':i,'outcome':listing['outcome'],'buy_copper':listing.get('buyNumerator'),'buy_denominator':listing.get('buyDenominator',1),
       'sell_copper':listing.get('sellNumerator'),'sell_denominator':listing.get('sellDenominator',1),
       'demand_group':listing.get('group',''),'notes':'Existing actual captured retail admission; no unreviewed bulk admission.'}
   known=p is not None and p.get('outcome')!='NONE'
   kind,classification_reason=technical_class(loaded.get(i,{'id':i}),p,protected)
   den=p.get('buy_denominator',1) if p else 1; buy=p.get('buy_copper') if p else None
   bulk=p.get('bulk_1024') if p else None
   def price(q):return (q*(bulk if bulk is not None and q>=256 else buy)+(1023 if bulk is not None and q>=256 else den-1))//(1024 if bulk is not None and q>=256 else den) if buy else ''
   writer.writerow(dict(mod_id=i.split(':')[0],item_id=i,display_name=record.get('display_name_en_us') or '',
    **{'class':kind},why=classification_reason,
    source_registry=('current loaded capture only; Atlas bootstrap refresh required' if record.get('current_capture_only') else 'Catalog/registries/items/'+i.split(':')[0]+'.json')+'; capture '+capture['capturedUtc'],
    source_recipe=';'.join(r['id'] for r in rs),recipe_output_count=';'.join(str(r['count']) for r in rs),
    base_material=p.get('quota_family',i) if p else '',effective_bulk_price_1024=price(1024),price_x64=price(64),price_x256=price(256),price_x4096=price(4096),
    buy_allowed=bool(i in loaded and buy and p['outcome']!='SELL_ONLY'),sell_allowed=bool(i in loaded and p and p.get('sell_copper') and p['outcome'] in ('SELL_ONLY','BUY_AND_SELL')),
    arbitrage_status='SUPPORTED_EXPECTED_RECIPE_POTENTIAL: bounded current purchase roots, all modeled coproduct chains; explicit handler/NBT gaps remain' if i in loaded and i in certificate.get('potential',{}) else 'NEEDS_REVIEW: outside supported monetary graph',requires_manual_review=kind=='NEEDS_REVIEW'))
 # Every actually registered identity has exactly one deterministic audit class; absent static observations are separate.
 mod_summary=collections.defaultdict(lambda:collections.Counter())
 for i,row in loaded.items():
  rank,reason=technical_class(row,policies.get(i),protected);mod_summary[i.split(':')[0]][rank]+=1
 summary={'capturedUtc':capture['capturedUtc'],'loadedItems':len(loaded),'staticAtlasObservations':len(atlas),
  'foodItems':sum('nutrition'in r for r in loaded.values()),
  'foodsSellable':sum('nutrition'in r and policies.get(i,{}).get('outcome') in ['SELL_ONLY','BUY_AND_SELL'] for i,r in loaded.items()),
  'approvedBulkSkus':sum(r.get('bulk_1024') is not None for r in policies.values()),
  'mods':{m:dict(sorted(counts.items())) for m,counts in sorted(mod_summary.items())},
  'requestedAbsentMods':[m for m in ['woodworks','quark','regions_unexplored'] if not any(i.startswith(m+':') for i in loaded)],
  'unknownAdmission':'Unknown classes do not acquire subsidized bulk. Existing retained retail admission is labelled separately in CSV.',
  'monetaryCertificate':{k:certificate.get(k) for k in ['status','recipes','nodes','constraints','unitPriceQuantumDenominator','maximumSaleRemainderDenominator','limits']}}
 (DOC/'shipping-policy-audit-summary.json').write_text(json.dumps(summary,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
 print('Authored',len(policies),'policies; actual loaded identities',len(loaded),'Atlas static observations',len(atlas),'capture',capture['capturedUtc'])
if __name__=='__main__':main()
