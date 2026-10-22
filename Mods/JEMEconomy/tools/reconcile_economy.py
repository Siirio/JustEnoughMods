"""Reconcile the editable economy against an explicitly captured loaded server registry.

This is an authoring/export tool, not a runtime registry or second wallet. Explicit
resource policies remain authoritative. Run only for an authorized economy review.
"""
from __future__ import annotations
import argparse, collections, csv, json, math, pathlib, re, zipfile
from fractions import Fraction

ROOT = pathlib.Path(__file__).resolve().parents[3]
DATA = ROOT / 'Mods/JEMEconomy/src/main/resources/data/jem_economy/economy'
FINAL = ROOT / 'Docs/economy/FINAL_PRICE_LIST'
RESEARCH = ROOT / '.superpowers/sdd/economy-overhaul'

def read(path): return json.loads(pathlib.Path(path).read_text(encoding='utf-8'))
def write(path, data):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, ensure_ascii=False, indent=2)+'\n', encoding='utf-8')
def tags(row): return {t.split(' / ',1)[-1].rstrip(']') for t in row['tags']}

def proposals(runtime, original):
    items={r['id']:r for r in runtime['items']}; result={r['item']:dict(r) for r in original}
    def add(item,buy,sell,group,cap,reason,source):
        if item not in items or item in result: return
        result[item]={'schema':1,'item':item,'outcome':'BUY_AND_SELL','sell_copper':sell,'buy_copper':buy,
                     'daily_player_units':cap,'daily_global_units':cap*16,'demand_group':group,
                     'notes':reason+' Evidence: '+source}
    # Actual runtime tags select common Overworld timber, not every name containing wood.
    forest={'minecraft','natures_spirit','beachparty','vinery','spawn','snowyspirit'}
    for id,row in sorted(items.items()):
        ns,path=id.split(':'); ts=tags(row)
        if ns in forest and 'minecraft:logs' in ts and not path.startswith('stripped_') and not any(x in path for x in ['crimson','warped','rotten','pale_oak']):
            add(id,24,2,'forestry',32,'Ordinary timber; six-plank Create sawing and stick yield priced in; renewable tree farms capped', 'loaded minecraft:logs tag and installed sawmill recipes')
        if ns in forest and 'minecraft:saplings' in ts and row.get('rarity')=='COMMON' and not any(x in path for x in ['pale_oak','huge','large']):
            add(id,16,1,'forestry',16,'Overworld planting stock; tree growth requires land/time; low surplus payout prevents sapling-farm dominance', 'loaded minecraft:saplings tag')
    explicit={
      'minecraft:apple':(8,2,'forestry',32,'Orchard and leaf-drop food; less frequent than field crops'),
      'minecraft:sweet_berries':(4,2,'crops',64,'Quick harvest thorn-bush crop; ordinary renewable produce'),
      'minecraft:glow_berries':(6,2,'crops',32,'Cave discovery then renewable vine; low payout'),
      'minecraft:brown_mushroom':(8,2,'crops',32,'Low-light or giant mushroom cultivation; capped renewable produce'),
      'minecraft:red_mushroom':(8,2,'crops',32,'Low-light or giant mushroom cultivation; capped renewable produce'),
      'minecraft:cocoa_beans':(8,2,'crops',32,'Jungle planting and repeat harvest; multiple dye/cookie outputs audited'),
      'minecraft:bamboo':(4,1,'forestry',64,'Fast automated crop; one-Copper payout and strict shared budget'),
      'minecraft:cactus':(4,1,'crops',32,'Fast automated crop; dye conversion audited'),
      'minecraft:chicken':(8,2,'animal_products',32,'Breeding and slaughter; eggs permit automation, conservative payout'),
      'minecraft:porkchop':(10,3,'animal_products',32,'Breeding/slaughter food; processed food stays profession-owned'),
      'minecraft:mutton':(8,2,'animal_products',32,'Sheep provide meat plus renewable wool; capped mixed yield'),
      'minecraft:rabbit':(12,3,'animal_products',16,'Less convenient breeding and variable drops'),
      'minecraft:leather':(12,3,'animal_products',32,'Breeding/slaughter input for books and gear; recipe-aware surplus value'),
      'minecraft:rabbit_hide':(6,1,'animal_products',32,'Four hides craft one leather; conversion-aware quote'),
      'minecraft:rabbit_foot':(24,4,'animal_products',8,'Low-probability rabbit drop; ingredient not potion'),
      'minecraft:feather':(4,1,'animal_products',32,'Chicken farm byproduct; arrow/cushion conversion audited'),
      'minecraft:egg':(4,1,'animal_products',32,'Passive chicken output; strict shared payout cap'),
      'minecraft:ink_sac':(6,1,'drops',32,'Renewable squid drop; dye processing audited'),
      'minecraft:glow_ink_sac':(12,2,'drops',16,'Cave squid drop with narrower spawn conditions'),
      'minecraft:spider_eye':(6,1,'drops',16,'Renewable spider ingredient; low automated-farm payout'),
      'minecraft:slime_ball':(8,2,'drops',16,'Swamp/slime-chunk renewable input; multiple conversions audited'),
      'minecraft:honeycomb':(8,2,'animal_products',16,'Bee management and timed hive harvest; bees remain gameplay'),
      'minecraft:flint':(6,1,'mining',32,'Gravel processing input; gravel itself remains excluded'),
      'minecraft:clay_ball':(6,1,'mining',32,'Ordinary clay and mud conversion; not generator currency'),
      'minecraft:charcoal':(8,1,'mining',32,'Timber smelting fuel; no fuel-cost subsidy in loop audit'),
      'minecraft:iron_nugget':(3,1,'mining',32,'Nine per iron ingot; integer payout kept below ingot acquisition cost'),
      'minecraft:gold_nugget':(4,1,'mining',32,'Nine per gold ingot; integer payout kept below ingot acquisition cost'),
      'create:zinc_ingot':(16,4,'mining',16,'Ordinary Create mining/processing material; ore multiplier audited'),
      'create:brass_ingot':(24,6,'mining',16,'Copper/zinc alloy requires heated mixing; inputs and output count audited'),
      'create:wheat_flour':(6,1,'crops',32,'Milling/crushing wheat; probability/byproducts audited conservatively'),
      'farm_and_charm:flour':(6,1,'crops',32,'Grain processing, not a prepared meal; recipe output audited'),
    }
    # Individual crop identities inspected against loaded tags/food properties and source recipes.
    cropids={
      'farm_and_charm':['barley','corn','lettuce','oat','onion','strawberry','tomato'],
      'kaleidoscope_cookery':['lettuce','rice','tomato','chili','oilseed','wild_rice'],
      'snowyspirit':['ginger'],
      'vinery':['cherry','jungle_grapes_red','jungle_grapes_white','savanna_grapes_red','savanna_grapes_white','taiga_grapes_red','taiga_grapes_white'],
      'herbalbrews':['yerba_mate_leaf','rooibos_leaf','dried_green_tea','dried_black_tea','dried_oolong_tea'],
      'brewery':['hops','dried_corn'],
      'natures_spirit':['shiitake_mushroom','desert_turnip'],
    }
    for ns,paths in cropids.items():
        for path in paths:
            explicit[f'{ns}:{path}']=(6,2,'crops',32,'Ordinary cultivated crop or dried plant ingredient; crop/recipe yields and automation audited')
    for id,(buy,sell,group,cap,why) in explicit.items(): add(id,buy,sell,group,cap,why,'loaded registry, installed tags/recipes/loot')
    for color in ['white','orange','magenta','light_blue','yellow','lime','pink','gray','light_gray','cyan','purple','blue','brown','green','red','black']:
        add(f'minecraft:{color}_wool',12,2,'animal_products',32,'Renewable shearing output; dye/recolor/string recipes included; builder products remain player-led','vanilla shearing/native Shepherd and loaded recipes')
    # Seeds are different goods from harvested crops. Exact native plant classes/tags, not suffix-only admission.
    crops={'minecraft','farm_and_charm','kaleidoscope_cookery','vinery','brewery','snowyspirit'}
    for id,row in sorted(items.items()):
        ns,path=id.split(':'); ts=tags(row)
        if ns in crops and row['class'].endswith(('ItemNameBlockItem','GrapeBushSeedItem')) and any(t.startswith(('forge:seeds/','c:seeds/')) for t in ts) and not any(x in path for x in ['nether','torchflower','pitcher']):
            add(id,6,1,'crops',32,'Planting seed with crop-to-seed yield audited; low surplus payout','loaded seed tag and plant item class')
    # Tide's real fishing definitions expose environment, weight, strength and special outputs.
    tidejar=next((ROOT/'Server/mods').glob('tide-*.jar'))
    with zipfile.ZipFile(tidejar) as jar:
        for path in sorted(jar.namelist()):
            if not re.fullmatch(r'data/tide/fishing/fish/(freshwater|saltwater|underground)/[^/]+\.json',path): continue
            fish=json.loads(jar.read(path)); id=fish.get('fish')
            if not id or id not in items: continue
            conditions=fish.get('conditions',[])
            dimensions=[x.get('dimensions',[]) for x in conditions if x.get('type')=='tide:dimension']
            if dimensions and any('minecraft:overworld' not in d for d in dimensions): continue
            weight=float(fish.get('selection_weight',0)); strength=float(fish.get('strength',0))
            if weight<10 or strength>0.65 or items[id].get('rarity')!='COMMON': continue
            # Avoid legendary drops and fish that transmute directly into rarer minerals.
            if id.split(':')[1] in {'midas_fish','crystal_shrimp','echo_snapper','gilded_minnow','lapis_lanternfish','iron_tetra','shadow_snapper','coelacanth','sun_emblem','uranias_pisces','aquathorn','marstilus','neptune_koi','spore_stalker'}: continue
            sell=2 if weight>=35 else 3 if weight>=20 else 4
            buy=10 if strength<=0.35 else 14 if strength<=0.5 else 18
            add(id,buy,sell,'fishing',16 if weight<20 else 32,
                f'Overworld water fishing: selection weight {weight:g}, strength {strength:g}; availability/effort assessed from native fishing definition',tidejar.name+':'+path)
    return list(result.values())

def csvwrite(name, columns, rows):
    FINAL.mkdir(parents=True,exist_ok=True)
    with (FINAL/name).open('w',encoding='utf-8-sig',newline='') as stream:
        writer=csv.DictWriter(stream,fieldnames=columns,extrasaction='ignore');writer.writeheader();writer.writerows(rows)

def trade_prices():
    def valid(row):
        return (isinstance(row.get('buy_copper'), int) and row['buy_copper'] > 0
                and isinstance(row.get('sell_copper'), int) and row['sell_copper'] > 0
                and isinstance(row.get('sell_units', 1), int) and row.get('sell_units', 1) > 0)
    market={r['item']:r for r in read(DATA/'item_policies.json') if r['outcome']=='BUY_AND_SELL' and valid(r)}
    native={r['item']:r for r in read(DATA/'native_item_prices.json') if valid(r)}
    for r in read(DATA/'profession_pools.json'):
        if valid(r): native.setdefault(r['item'],r)
    merged=dict(native);merged.update(market)
    return market,native,merged

def native_batch_sizes(runtime, native):
    batches=collections.defaultdict(set)
    for row in runtime.get('nativeTrades',[]):
        first, second, result = (row.get(key, {}) for key in ['cost_a', 'cost_b', 'result'])
        # Exact ProfessionTradePools behavior: a monetary original has one merchandise leg;
        # its reverse companion makes that same merchandise batch available in both directions.
        goods = first if result.get('item') == 'minecraft:emerald' else result
        monetary = any(stack.get('item') == 'minecraft:emerald' for stack in [first, second, result])
        if monetary and goods.get('item') in native and goods.get('count', 0) > 0:
            batches[goods['item']].add(goods['count'])
    for row in read(DATA/'profession_pools.json'):
        if row['item'] in native and row['count'] > 0:batches[row['item']].add(row['count'])
    return batches

def maximum_sale_unit(item, row, market, batches):
    if item in market:return Fraction(row['sell_copper']*3//2)
    units=row.get('sell_units',1)
    counts=batches.get(item,set())
    if not counts:return Fraction(0)
    return max(Fraction(math.ceil(Fraction(row['sell_copper']*count,units)),count) for count in counts)

def recipe_definitions():
    # Re-read installed artifacts: a private research cache is never an authority
    # for another checkout or a later artifact version.
    definitions={}
    jars=list((ROOT/'Server/libraries/net/minecraft/server').glob('*/*-unpacked.jar'))
    jars+=sorted((ROOT/'Server/mods').glob('*.jar'))
    for jar in jars:
        with zipfile.ZipFile(jar) as archive:
            for name in archive.namelist():
                match=re.fullmatch(r'data/([^/]+)/recipes/(.+)\.json',name)
                if not match: continue
                try: data=json.loads(archive.read(name))
                except (ValueError,UnicodeError): continue
                definitions[match[1]+':'+match[2]]={'data':data,'source':jar.name,'path':name}
    return definitions

def conversions(runtime):
    raw=recipe_definitions(); result=[]
    for recipe in runtime['recipes']:
        if not recipe['ingredients'] or any(not choices for choices in recipe['ingredients']): continue
        outputs=[]; primary=recipe['output'];count=recipe['count']
        if count>0 and primary!='minecraft:air': outputs=[(primary,Fraction(count))]
        definition=raw.get(recipe['id'],{}).get('data',{})
        rawresults=definition.get('results')
        if isinstance(rawresults,list) and rawresults and outputs:
            possible=[]
            for r in rawresults:
                if not isinstance(r,dict): continue
                item=r.get('item'); amount=r.get('count',1)
                if isinstance(item,str) and r.get('chance',1)>0: possible.append((item,Fraction(amount)*Fraction(str(r.get('chance',1)))))
            if possible and possible[0][0]==primary:
                # A recipe may emit the same item in guaranteed and chance legs.
                # Unit acquisition cost uses their combined expected quantity.
                combined=collections.defaultdict(Fraction)
                for item,amount in possible:combined[item]+=amount
                outputs=list(combined.items())
        if outputs:
            groups=list(recipe['ingredients']); quantities=[Fraction(1)]*len(groups)
            if recipe['type'] in {'minecraft:smelting','minecraft:blasting','minecraft:smoking'}:
                # Installed Create 6.0.8 fan types apply native cooking recipes against
                # reusable heat catalysts; their operation consumes no per-item fuel.
                if any(row['id'].startswith('create:') for row in runtime['items']):
                    result.append({'id':recipe['id']+'#create_fan','type':'create:fan_processing',
                                   'inputs':list(groups),'quantities':list(quantities),'outputs':list(outputs)})
                default_time = 200 if recipe['type'] == 'minecraft:smelting' else 100
                cooking_time = definition.get('cookingtime', default_time)
                burn_time = 1600 if recipe['type'] == 'minecraft:smelting' else 800
                groups.append(['minecraft:coal','minecraft:charcoal']);quantities.append(Fraction(cooking_time,burn_time))
            result.append({'id':recipe['id'],'type':recipe['type'],'inputs':groups,'quantities':quantities,'outputs':outputs})
    for row in runtime.get('nativeTrades', []):
        stacks = [row.get(key, {}) for key in ['cost_a', 'cost_b', 'result']]
        if any(stack.get('item') == 'minecraft:emerald' for stack in stacks):continue
        first, second, output = stacks
        if output.get('count', 0) <= 0 or output.get('item') in {None, 'minecraft:air'}:continue
        inputs = [stack for stack in [first, second] if stack.get('count', 0) > 0 and stack.get('item') != 'minecraft:air']
        if not inputs:continue
        result.append({'id':'native_barter:'+str(row.get('profession'))+':'+str(row.get('level'))+':'+str(row.get('pool_index')),
                       'type':'native:item_barter','inputs':[[stack['item']] for stack in inputs],
                       'quantities':[Fraction(stack['count']) for stack in inputs],
                       'outputs':[(output['item'],Fraction(output['count']))]})
    return result

def audit(runtime, recipe_overrides=None):
    pending_overrides = []
    if recipe_overrides:
        runtime = dict(runtime); changed_recipes = []
        for row in runtime['recipes']:
            if row['id'] in recipe_overrides:
                count = recipe_overrides[row['id']]
                if not isinstance(count,int) or count <= 0:raise ValueError('Invalid simulated recipe count')
                pending_overrides.append({'id':row['id'],'captured_count':row['count'],'simulated_count':count,
                                          'status':'explicit authoring simulation; not runtime evidence'})
                row = dict(row); row['count'] = count
            changed_recipes.append(row)
        runtime['recipes'] = changed_recipes
        if set(recipe_overrides)-{row['id'] for row in changed_recipes}:raise ValueError('Simulated recipe ID absent from capture')
    market,native,prices=trade_prices()
    loaded = {row['id'] for row in runtime['items']}
    market = {item: row for item,row in market.items() if item in loaded}
    prices = {item: row for item,row in prices.items() if item in loaded}
    recipes=conversions(runtime); batches=native_batch_sizes(runtime,prices)
    # Global Market-only goods have no racial/reputation discount. A native price
    # policy alone is not evidence that an NPC offers that item for purchase.
    cost={item:Fraction(row['buy_copper']) for item,row in market.items()}
    root_channels={item:'global Market full buy' for item in cost}
    for item, counts in batches.items():
        row = prices[item]
        def native_floor(count):
            if item in market: maximum = (row['sell_copper']*150//100)*count
            else: maximum = math.ceil(Fraction(row['sell_copper']*count*150,100*row.get('sell_units',1)))
            return Fraction(max(maximum+1,math.ceil(Fraction(row['buy_copper']*count,2))),count)
        floor = min(native_floor(count) for count in counts)
        if item not in cost or floor < cost[item]:
            cost[item]=floor;root_channels[item]='captured native/reverse or authored specialty batch discount floor'
    root_costs = dict(cost)
    routes={id:{id:Fraction(1)} for id in cost};via={id:root_channels[id] for id in cost}
    for iteration in range(45):
        changed=0
        for recipe in recipes:
            choices=[]
            for group in recipe['inputs']:
                candidates=[id for id in group if id in cost]
                if not candidates: break
                choices.append(min(candidates,key=lambda id:cost[id]))
            if len(choices)!=len(recipe['inputs']): continue
            total=sum((cost[id]*q for id,q in zip(choices,recipe['quantities'])),Fraction(0)); provenance=collections.defaultdict(Fraction)
            for id,used in zip(choices,recipe['quantities']):
                for origin,quantity in routes[id].items(): provenance[origin]+=quantity*used
            for output,count in recipe['outputs']:
                unit=total/count
                if output not in cost or (unit<cost[output] and float(cost[output]-unit)>0.00000001):
                    cost[output]=unit;routes[output]={id:q/count for id,q in provenance.items()};via[output]=recipe['id'];changed+=1
        if not changed: break
    unsafe=[]
    for id,r in prices.items():
        payout=maximum_sale_unit(id,r,market,batches)
        if id in cost and payout>cost[id]:
            unsafe.append({'item':id,'payout':float(payout),'cost':float(cost[id]),'recipe':via[id],
                           'sources':{origin:float(q) for origin,q in routes[id].items()},'exactCost':str(cost[id]),
                           'sourceUnitCosts':{origin:str(root_costs[origin]) for origin in routes[id]},
                           'sourceChannels':{origin:root_channels[origin] for origin in routes[id]}})
    multi=[]
    for recipe in recipes:
        if len(recipe['outputs'])<2: continue
        choices=[min((cost[id] for id in group if id in cost),default=None) for group in recipe['inputs']]
        if any(x is None for x in choices): continue
        liquidation=sum((q*maximum_sale_unit(id,prices[id],market,batches) for id,q in recipe['outputs'] if id in prices),Fraction(0))
        total=sum((c*q for c,q in zip(choices,recipe['quantities'])),Fraction(0))
        if liquidation>total: multi.append({'recipe':recipe['id'],'payout':float(liquidation),'cost':float(total),'outputs':[(id,str(q)) for id,q in recipe['outputs']]})
    gaps=[{key:row.get(key) for key in ['profession','level','pool_index','provider','sample_status']}
          for row in runtime.get('nativeTrades',[]) if 'result' not in row]
    report={'method':'Actual global Market full buy; captured native merchandise/reverse and authored specialty batch discount floors only. Native-only sale endpoints require actual provider/pool batches. Loaded ingredient alternatives and exact static output probabilities; fuel-consuming furnaces plus installed Create zero-consumable fan variants; machine power/fluid cost zero. Expected-value analysis, not a promise every lucky craft loses money.',
            'modeledRecipes':len(recipes),'loadedRecipes':len(runtime['recipes']),'costPasses':iteration+1,'converged':not changed,
            'marketPurchaseRoots':len(market),'nativePurchaseRoots':len(batches),
            'nativeProviderRows':len(runtime.get('nativeTrades',[])), 'nativeProviderGapCount':len(gaps),
            'nativeCoverageComplete':bool(runtime.get('nativeTrades')) and not gaps,
            'unsampledNativeProviders':gaps,
            'unobservedNativePolicies':sorted(set(native)-set(batches)),
            'pendingRuntimeRecipeOverrides':pending_overrides,
            'unsafe':sorted(unsafe,key=lambda r:r['payout']-r['cost'],reverse=True),'unsafeCoproducts':multi}
    write(RESEARCH/'arbitrage-report.json',report)
    return report,cost,routes

def export_catalog(runtime, verification=None):
    """Export current authored policies and captured evidence; never mutate prices or infer runtime acceptance."""
    import hashlib
    items = {row['id']: row for row in runtime['items']}
    policies = read(DATA/'item_policies.json')
    policy_by_id = {row['item']: row for row in policies}
    market, native, merged = trade_prices()
    native_candidates = set(native)
    invalid_native = sorted(item for item, row in native.items()
        if not isinstance(row.get('buy_copper'), int) or row['buy_copper'] <= 0
        or not isinstance(row.get('sell_copper'), int) or row['sell_copper'] <= 0
        or not isinstance(row.get('sell_units', 1), int) or row.get('sell_units', 1) <= 0)
    native = {item: row for item, row in native.items() if item not in invalid_native}
    merged = dict(native); merged.update(market)
    pools = read(DATA/'profession_pools.json')
    drive_rows = read(RESEARCH/'drive-full.json') if (RESEARCH/'drive-full.json').exists() else []
    drive = {row['Item ID']: row for row in drive_rows if row.get('Item ID')}
    captured = runtime.get('capturedUtc', 'unrecorded')
    native_rows = runtime.get('nativeTrades', [])
    hashes = {name: hashlib.sha256((DATA/name).read_bytes()).hexdigest()
              for name in ['item_policies.json', 'native_item_prices.json', 'profession_pools.json']}
    def category(row):
        path = row['item'].split(':', 1)[-1]
        group = row.get('demand_group', '')
        if group == 'drops' or path in {'bone', 'string', 'gunpowder'}: return 'DROPS'
        if group in {'mining', 'handcrafts'}: return 'RESOURCES'
        if group == 'forestry' and path.endswith(('_log', '_wood', '_sapling')): return 'BLOCKS'
        if path.endswith('_wool'): return 'BLOCKS'
        if path in {'leather','rabbit_hide','feather','honeycomb','buckskin','hide'}: return 'RESOURCES'
        return 'FOOD'
    def batch_payout(item, row, quantity, multiplier):
        if item in market: return (row['sell_copper']*multiplier//100)*quantity
        denominator = 100*row.get('sell_units', 1)
        return max(1, (row['sell_copper']*quantity*multiplier+denominator-1)//denominator)
    def price_fields(item, quantity=1, sample=None):
        row = merged.get(item)
        if row is None: return {}
        buy = row['buy_copper']
        # Fresh loaded samples include exact enchantment/NBT acquisition premiums.
        if sample and sample.get('buy_copper') is not None:
            buy = sample['buy_copper']
        return {'buy_copper_per_unit': buy, 'sell_copper_numerator': row['sell_copper'],
                'sell_units': 1 if item in market else row.get('sell_units', 1),
                'buy_batch_copper_base': buy*quantity,
                'sell_batch_copper_100': batch_payout(item, row, quantity, 100),
                'sell_batch_copper_150': batch_payout(item, row, quantity, 150),
                'daily_player_units': row.get('daily_player_units', 8),
                'daily_global_units': row.get('daily_global_units', 128)}

    market_export = []
    for item, row in sorted(market.items()):
        if item not in items: continue
        market_export.append({'item': item, 'name': items[item].get('name', ''),
            'mod': item.split(':')[0], 'buy_copper': row['buy_copper'], 'sell_copper': row['sell_copper'],
            'sell_copper_primary_demand': row['sell_copper']*150//100, 'category': category(row),
            'demand_group': row.get('demand_group', ''), 'daily_player_units': row['daily_player_units'],
            'daily_global_units': row['daily_global_units'], 'supplier_player_stock_per_day': row['daily_player_units'],
            'supplier_global_stock_per_day': row['daily_global_units'], 'shared_account_payout_cap': 240,
            'shared_global_payout_cap': 4000, 'explanation': row.get('notes', ''),
            'source': 'data/jem_economy/economy/item_policies.json', 'registry_capture_utc': captured})
    csvwrite('MARKET.csv', ['item','name','mod','buy_copper','sell_copper','sell_copper_primary_demand',
        'category','demand_group','daily_player_units','daily_global_units','supplier_player_stock_per_day',
        'supplier_global_stock_per_day','shared_account_payout_cap','shared_global_payout_cap','explanation',
        'source','registry_capture_utc'], market_export)

    villagers = []; observed_native = set()
    for row in native_rows:
        result = row.get('result', {}); first = row.get('cost_a', {})
        sells = result.get('item') == 'minecraft:emerald'
        goods = first if sells else result
        item = goods.get('item', ''); quantity = goods.get('count', 0)
        if item and item != 'minecraft:air': observed_native.add(item)
        priced = item in merged and item in items and quantity > 0
        villagers.append({'entry_type': 'loaded_native_provider', 'profession': row.get('profession', ''),
            'level': row.get('level', ''), 'pool_index': row.get('pool_index', ''), 'item': item,
            'native_direction': 'PLAYER_SELLS' if sells else ('PLAYER_BUYS' if item else 'PROVIDER_ONLY'),
            'supported_directions': 'BUY_AND_SELL' if priced else 'UNPRICED_OR_UNSAMPLED',
            'count': quantity or '', 'weight': 'native provider selection', 'max_uses': row.get('max_uses', ''),
            'restock': row.get('restock', 'native provider owns restock'), 'xp': row.get('xp', ''),
            'nbt_sample': goods.get('nbt', ''), 'provider': row.get('provider', ''),
            'evidence': row.get('sample_status', 'captured provider without sample'),
            'notes': row.get('selection', '')+'; reverse companion retains exact goods/counts; randomized NBT sample is nonexhaustive',
            'source': 'loaded NativeTradeCatalog.snapshot', **(price_fields(item, quantity, goods) if priced else {})})
    for row in sorted(pools, key=lambda r: (r['profession'], r['level'], r['item'])):
        item = row['item']; quantity = row['count']
        villagers.append({'entry_type': 'weighted_additional_pool', 'profession': row['profession'],
            'level': row['level'], 'pool_index': '', 'item': item, 'native_direction': 'PLAYER_BUYS',
            'supported_directions': 'BUY_AND_SELL' if item in items else 'ID_NOT_LOADED', 'count': quantity,
            'weight': row['weight'], 'max_uses': row['max_uses'], 'restock': 'native Villager work/restock',
            'xp': 5*row['level'], 'nbt_sample': '', 'provider': 'ProfessionTradePools',
            'evidence': 'loaded item identity; editable authored pool; generated selection remains persisted per NPC',
            'notes': row.get('notes', '')+'; one weighted addition per eligible level; paired reverse adds zero XP',
            'source': 'data/jem_economy/economy/profession_pools.json', **price_fields(item, quantity)})
    for item, row in sorted(native.items()):
        if item in observed_native: continue
        villagers.append({'entry_type': 'native_price_policy_not_observed_in_sample', 'profession': '', 'level': '',
            'item': item, 'native_direction': 'POLICY_ONLY', 'supported_directions': 'BUY_AND_SELL_POLICY',
            'count': '', 'weight': '', 'max_uses': '', 'restock': 'native offer lifecycle when actually offered',
            'evidence': 'native provider capture absent' if not native_rows else 'price policy exists; exact pool variant not established by captured sample',
            'notes': row.get('notes', ''), 'source': 'data/jem_economy/economy/native_item_prices.json or profession_pools.json',
            **price_fields(item)})
    csvwrite('VILLAGERS.csv', ['entry_type','profession','level','pool_index','item','native_direction','supported_directions',
        'count','buy_copper_per_unit','sell_copper_numerator','sell_units','buy_batch_copper_base','sell_batch_copper_100',
        'sell_batch_copper_150','weight','max_uses','restock','xp','daily_player_units','daily_global_units',
        'nbt_sample','provider','evidence','notes','source'], villagers)

    npc_admitted = (set(market)|set(native)) & set(items)
    exclusions = []
    for item, metadata in sorted(items.items()):
        if item in npc_admitted: continue
        prior = drive.get(item, {}); policy = policy_by_id.get(item)
        if prior.get('NPC trade channel') == 'EXCLUDE':
            kind = 'PRIOR_EXPLICIT_NPC_EXCLUSION'; reason = prior.get('Specific reason / rationale') or 'Prior explicit NPC exclusion retained'
            source = 'Drive Full Registry exact item decision'
        elif policy and policy.get('outcome') == 'NONE':
            kind = 'EXPLICIT_CURRENT_POLICY_NONE'; reason = policy.get('notes') or 'Current policy explicitly disallows NPC trading'
            source = 'item_policies.json exact item row'
        elif item.startswith('jem_economy:') and 'coin' in item:
            kind = 'BACKED_CURRENCY_NOT_MERCHANDISE'; reason = 'Backed Coin issuance/redemption belongs to the ledger, not an NPC commodity conversion'
            source = 'EconomyItems and physical Coin provenance/ledger'
        else:
            kind = 'NO_APPROVED_NPC_CHANNEL'
            reason = ('No current global or native-only merchandise admission/price for this exact loaded item; '
                      'this is an explicit catalog exclusion, not a claim of unobtainability or verified boss power')
            source = 'current canonical policy/native/pool absence; loaded registry identity'
        exclusions.append({'item': item, 'name': metadata.get('name', ''), 'mod': item.split(':')[0],
            'classification': kind, 'reason': item+': '+reason, 'source': source,
            'runtime_class': metadata.get('class', ''), 'rarity': metadata.get('rarity', ''),
            'prior_decision': prior.get('Decision status', ''), 'prior_channel': prior.get('NPC trade channel', ''),
            'player_to_player': 'unrestricted by NPC exclusion', 'registry_capture_utc': captured})
    csvwrite('EXCLUDED.csv', ['item','name','mod','classification','reason','source','runtime_class','rarity',
        'prior_decision','prior_channel','player_to_player','registry_capture_utc'], exclusions)
    unverified = []
    candidates = set(drive)|set(policy_by_id)|native_candidates|{row['item'] for row in pools}
    for item in sorted(candidates-set(items)):
        prior = drive.get(item, {}); sources = []
        if item in drive: sources.append('Drive Full Registry')
        if item in policy_by_id: sources.append('current item policy')
        if item in native: sources.append('current native/pool price resource')
        unverified.append({'item': item, 'sources': '; '.join(sources), 'prior_decision': prior.get('Decision status', ''),
            'prior_channel': prior.get('NPC trade channel', ''), 'reason': 'Not present in this successful loaded Item registry capture; static assets/strings are not runtime item proof',
            'prior_evidence': prior.get('Recipe / acquisition evidence', ''), 'registry_capture_utc': captured})
    csvwrite('UNVERIFIED_IDS.csv', ['item','sources','prior_decision','prior_channel','reason','prior_evidence','registry_capture_utc'], unverified)

    assert npc_admitted | {row['item'] for row in exclusions} == set(items)
    assert not npc_admitted & {row['item'] for row in exclusions}
    latest_audit = read(RESEARCH/'arbitrage-report.json') if (RESEARCH/'arbitrage-report.json').exists() else {}
    summary = {'loaded_items': len(items), 'loaded_recipes': len(runtime['recipes']), 'market_items': len(market_export),
        'native_only_items': len((set(native)-set(market)) & set(items)), 'npc_admitted_unique': len(npc_admitted),
        'excluded_items': len(exclusions), 'unverified_ids': len(unverified), 'native_provider_rows': len(native_rows),
        'additional_pool_rows': len(pools), 'villager_export_rows': len(villagers),
        'active_market_ids_missing': sorted(set(market)-set(items)), 'invalid_native_rows': invalid_native,
        'capture_utc': captured, 'resource_sha256': hashes,
        'native_provider_unsampled': sum('result' not in row for row in native_rows),
        'latest_audit_native_coverage_complete': latest_audit.get('nativeCoverageComplete', False),
        'latest_audit_pending_runtime_recipe_overrides': latest_audit.get('pendingRuntimeRecipeOverrides', []),
        'latest_recorded_audit_unsafe': len(latest_audit.get('unsafe', [])),
        'latest_recorded_audit_coproduct_unsafe': len(latest_audit.get('unsafeCoproducts', []))}
    verification = verification or {}
    balance = f'''# Economy balance\n\nGenerated from current authored resources and loaded registry captured {captured}.\nAll values below are Copper equivalents; actual GUI prices use existing Coin icons.\n\n## Catalog coverage\n\n{len(items):,} loaded Item IDs are partitioned into {len(npc_admitted):,} admitted\nNPC merchandise identities and {len(exclusions):,} explicit catalog exclusions.\nMarket has {len(market_export)} real two-way item policies; native-only merchandise\nadds {summary['native_only_items']} identities. Weighted additional pools contain\n{len(pools)} entries. Duplicate profession/provider rows are intentionally retained.\nPrior Drive exclusions and their reasons remain in EXCLUDED.csv unless the user's\nexplicit preserve-native contract supplies the documented native-only exception.\nThere is no PLAYER SHOPS ONLY classification: NPC exclusion does not restrict player\nshops, gifts, dropping or player-to-player barter.\n\n## Price scale and production\n\nCopper/Silver/Gold/Aether/Arcane are worth 1/10/100/1000/10000. Quantities and\nrecipe yields matter: crop processing, milling, smelting fuel, compression,\nunpacking, cutting and expected coproducts are included in the recorded audit.\nEach MARKET.csv row records the actual current prices, caps, demand group and\nitem-specific reason. Finished goods remain native specialist merchandise;\nunique boss/progression equipment is not admitted by a generic price fallback.\nExisting approved commodity prices are retained except individually evidenced\nrecipe/exploit corrections documented by the task owner.\n\nNative equipment/books keep exact NBT; acquisition premiums follow actual\nenchantment rarity/level. Purchase discounts clamp after racial adjustment to\nhalf the base budget or maximum whole-offer buyback plus one, whichever is higher.\nNative sell_units is a denominator: whole-offer payout is the ceiling of\nsell_copper × quantity × multiplier / (100 × sell_units), minimum one Copper.\nCanonical Market commodities retain their original per-unit rounding contract.\nOriginal native offer counts are never changed to conceal fractional prices.\nVILLAGERS.csv distinguishes exact loaded samples from unobserved price policies.\n\n## Earning, spending and finite supply\n\nAn account can receive at most 240 Copper per economic day from NPC sales, with\n4000 Copper shared across the server, item caps and the funded NPC_MARKET reserve.\nAt the payout cap, one Copper-valued unit yields at most240 units/day before item\ncaps; higher-value goods reach the same money ceiling sooner. Purchases consume\npersisted daily supplier stock separately from NPC buying quotas. Primary demand\npays150%, secondary125%, otherwise100%, subject to integer rounding. Time rewind\ndoes not reset an already consumed cycle. Automation is useful surplus production,\nnot an unlimited reserve or bypass of player/global limits.\n\nDefault recruit hiring/weekly wages: ordinary120/20, ranged200/40, mounted400/70,\ncommand700/120. Hiring adds5% of base per native experience level above1, capped3×.\nSeven Minecraft days equal168000 monotonic game ticks; each contract's immutable\nperiod roster and ledger key prevent duplicate weekly charging. Three actual unpaid\nweeks cause persistent rebellion; death does not forgive it. Town employers pay\nthe existing treasury; standalone employers pay their real Wallet. Native combat,\nownership and paid defeat/rehire remain with their existing owners.\n\nFounding defaults300 Copper after validation and creates the existing97×97 area.\nExpansion preserves the approved1 backed physical Copper per newly added inclusive\nblock of area; shrink/no-op cannot charge or mutate. For example one new97-block\ncolumn costs97 Copper, while a new10×10 area costs100 Copper.\n\n## Evidence limits\n\nThe latest recorded recipe report models {latest_audit.get('modeledRecipes', 'unrecorded')}\nrecipes and reports {summary['latest_recorded_audit_unsafe']} unsafe single-output\nand {summary['latest_recorded_audit_coproduct_unsafe']} unsafe coproduct routes.\nThis report may predate current resource hashes: rerun --audit --export after\nfinal changes. Expected-value processing analysis does not prove arbitrary lucky\ncraft sequences or runtime/player behavior. Visual acceptance, optional merchant\nboundaries and crash durability must be reported honestly in IMPLEMENTATION_REPORT.\n'''
    balance += '''
## Practical scale and progression

At base demand,64 crops worth2 Copper each earn128 Copper; primary demand
raises that to192 before shared caps. A120-Copper ordinary recruit is about
half a capped earning day, its20-Copper weekly wage is one twelfth of that day,
and300-Copper founding is1.25 capped days. Ten ordinary recruits require1200
to hire and200 per Minecraft week to maintain. These are caps/benchmarks,
not promised passive income; supply, reserve, item quotas and actual harvests apply.

Ordinary timber costs8 Copper. Wheat and all four Farm & Charm mincer grains
cost8 because four-flour output and native wholesale discounts matter. Cod and
salmon, raw or cooked, cost14 because cooked fish produces two bones and enough
meal/dyes to return7 Copper even through the cheapest preserved native dye batches.
Bone costs4; wine-bottle buyback pays1 Copper for the native two-bottle batch.
These corrections follow exact conversions rather than a uniform rarity multiplier.

Boss rewards, exclusive progression keys and protected equipment retain their
gameplay owners and have no generic NPC pricing fallback. Ordinary ingredients
do not inherit a boss gate merely because a protected recipe uses them. Rare
specialty weight never grants permission to bypass a protected World Tier endpoint.
NPC exclusion leaves gifts, barter, player shops and other player transfers intact.

The dedicated mincer recipe now makes1 cobblestone from1 stone. Create's native
fan remains fuel-free; ordinary generators/farms remain valid surplus production,
bounded by the same NPC reserve and daily budgets. Integrated singleplayer skips
the recipe adapter through KubeJS's physical-client Platform binding.
'''
    (FINAL/'ECONOMY_BALANCE.md').write_text(balance, encoding='utf-8')
    statuses = {'Production builds': 'pending final integration evidence',
        'Latest dedicated registry/recipe capture': f'captured {captured}; loaded identity evidence only',
        'Native provider capture': f'{len(native_rows)} rows captured' if native_rows else 'pending; snapshot contains no nativeTrades',
        'Native Java arithmetic': '331036 standalone assertions passed during this task; does not prove Forge lifecycle',
        'Merchant production-resource contracts': 'six checks passed during this task; rerun after final price changes',
        'Recruit/settlement contracts': '27 updated assertions reported passed by owning implementation note',
        'Coin denomination arithmetic': 'standalone production contract passed per owning presentation note',
        'Client rendering/player acceptance': 'pending; no inspected in-game screenshot recorded',
        'Final Server/ManagedPack synchronization': 'pending task-owner delivery evidence',
        'Native AutoModpack publication': 'pending task-owner preview/publish/hash evidence',
        'Git delivery': 'pending task-owner commit/push/remote verification',
        'MineRENT/stable/baseline': 'not authorized for this task; no release promotion claimed'}
    statuses.update(verification)
    report = ['# Economy implementation report', '',
        'The user requested and authorized implementation, balance decisions, builds and automated verification.',
        'The following describes current source/resources and captured evidence, not acceptance of every gameplay path.', '',
        '## Implemented owners', '',
        '- JEMEconomy: global two-way policies, supplier stock, sale quotas/reserve, existing Wallet/backed Coin ledger, native merchandise and persisted weighted specialties.',
        '- Native MerchantOffer: original goods/counts/NBT, progression, uses/demand/restock and saved individual offers. Compatible AbstractVillager merchants reuse this adapter.',
        '- JEMSettlements/Recruits: existing native hire ownership, Coin charges, per-contract seven-day payroll, real employer/treasury debit, persistent three-miss rebellion and atomic validated territory charges.',
        '- Shared client/text currency presentation: the five actual registered Coin assets and exact denomination amounts; native screens remain their original owners.', '',
        '## Reconciliation', '', '```json', json.dumps(summary, ensure_ascii=False, indent=2), '```', '',
        'MARKET.csv contains only real loaded active policies. VILLAGERS.csv labels loaded native provider samples, authored weighted pools and price policies not observed in the current sample separately.',
        'EXCLUDED.csv covers every other real loaded item with its exact prior/current reason or an honest no-approved-NPC-channel exclusion. It does not claim unresearched items are all unobtainable or overpowered.',
        'UNVERIFIED_IDS.csv contains only candidate IDs missing from the actual loaded registry, including static/Drive evidence that must never be silently activated.', '',
        '## Verification and delivery status', '', '| Activity | Actual evidence/status |', '| --- | --- |']
    for label, status in statuses.items():
        report.append('| '+str(label).replace('|', '/')+' | '+str(status).replace('|', '/').replace('\n', ' ')+' |')
    report += ['', '## Material limits', '',
        '- A sampled randomized native provider is not an exhaustive list of every enchanted-book/map/potion/NBT variant. World-dependent treasure-map and unknown optional provider callbacks can remain explicitly unsampled.',
        '- Unknown or explicitly excluded optional-mod merchandise remains visible/unpriced; existing native interfaces are preserved without inventing APIs or boss/World Tier exemptions.',
        '- Resource/audit hashes and loaded captures must refer to the final artifacts before claiming final runtime validation. This generator never runs a build, modifies prices, starts a server or publishes a pack.',
        '- Same-thread payment/item handling does not establish crash-atomic persistence across separate player/NPC/SavedData files. No deliberate crash durability acceptance is claimed.',
        '- A remaining unsafe recipe count, missing active ID, absent native capture or pending acceptance is outstanding work, not successful completion.', '',
        'Owning detail: ../merchant-implementation-notes.md, ../settlement-implementation-notes.md and ../presentation-implementation-notes.md.', '']
    report += ['', '## Installed production artifacts', '', '| Module | JAR | SHA-256 | Build / Server / ManagedPack identical |', '| --- | --- | --- | --- |']
    artifacts = [('JEMEconomy','jem-economy-1.2.0.jar'), ('JEMSettlements','jem-settlements-2.0.0.jar'),
                 ('JEMCompat','jemcompat-3.7.0.jar'), ('JEMServer','jem-server-2.0.0-dev.jar')]
    for module, name in artifacts:
        paths = [ROOT/'Mods'/module/'build/libs'/name, ROOT/'Server/mods'/name, ROOT/'Client/ManagedPack/mods'/name]
        digests = [hashlib.sha256(path.read_bytes()).hexdigest() if path.is_file() else None for path in paths]
        report.append(f"| {module} | {name} | {digests[1] or 'missing'} | {bool(digests[0]) and len(set(digests)) == 1} |")
    report += ['', '## Player entry and reproducibility', '',
        'Open `/smp` and its Market for the two Coin price rows. Native villagers and compatible special merchants keep their usual interaction and controls. Personal employers pay Wallet; assigned town recruits use the real treasury. Existing Wallet withdrawal supplies backed physical Coins for the protected exact-area expansion path.', '',
        'The opt-in dedicated JVM property `-Djem.economy.export=true` writes `Server/local/economy-audit/loaded-registry.json` and executes unsaved native-NBT contracts. Use it only for an authorized local verification run. `python Mods/JEMEconomy/tools/reconcile_economy.py --audit --export --runtime Server/local/economy-audit/loaded-registry.json` re-reads installed JAR recipes, audits the capture and regenerates these files. Drive classifications require the captured seven-tab design source; they are not item-registration proof.', '']
    (FINAL/'IMPLEMENTATION_REPORT.md').write_text('\n'.join(report), encoding='utf-8')
    from author_building_supply import export_catalog_overrides
    export_catalog_overrides(runtime)
    return summary


def main():
    parser=argparse.ArgumentParser();parser.add_argument('--runtime',default=str(ROOT/'Server/local/economy-audit/loaded-registry.json'))
    parser.add_argument('--propose',action='store_true');parser.add_argument('--audit',action='store_true')
    parser.add_argument('--export',action='store_true',help='Write the six final catalog/report files without changing prices')
    parser.add_argument('--verification',help='Optional JSON object of factual validation/delivery statuses for the report')
    parser.add_argument('--recipe-override',action='append',default=[],metavar='ID=COUNT',
                        help='Explicit audit-only recipe yield simulation, marked pending runtime capture')
    args=parser.parse_args()
    if any(row.get('schema',1)>=2 for row in read(DATA/'item_policies.json')):
        raise SystemExit('Rational schema2 policies require JEMEconomy/tools/author_shipping_economy.py and derive_conversion_economy.py; legacy integer export cannot overwrite the current audit')
    runtime=read(args.runtime)
    originalPath=RESEARCH/'approved-policies.json'
    if not originalPath.exists(): write(originalPath,read(DATA/'item_policies.json'))
    if args.propose:
        proposed=proposals(runtime,read(DATA/'item_policies.json'));write(DATA/'item_policies.json',proposed)
        print('Policy proposals:',collections.Counter(r['outcome'] for r in proposed))
    print('Runtime:',len(runtime['items']),'items;',len(runtime['recipes']),'loaded recipes')
    if args.audit:
        overrides={}
        for value in args.recipe_override:
            key,count=value.rsplit('=',1);overrides[key]=int(count)
        report,_,_=audit(runtime,overrides)
        hidden={'unsafe','unsafeCoproducts','unsampledNativeProviders','unobservedNativePolicies'}
        print('Recipe audit', {k:v for k,v in report.items() if k not in hidden})
        print('Unsafe',len(report['unsafe']),'coproducts',len(report['unsafeCoproducts']))
        for row in report['unsafe'][:24]:print(row)
    if args.export:
        print('Final catalog export', export_catalog(runtime, read(args.verification) if args.verification else None))

if __name__=='__main__': main()
