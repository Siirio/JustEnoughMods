"""Production economy authoring from an explicitly captured current loaded registry.
No game launch, tests, fixtures, or code analysis. Calculates finite money conversion
bounds from actual loaded recipes/roll outputs and emits reviewable price decisions.
Canonical item_policies.json is changed only with --apply. Report is never runtime input.
"""
import argparse,collections,csv,json,math,re,struct
from fractions import Fraction as F
from pathlib import Path
ROOT=Path(__file__).resolve().parents[3]
DATA=ROOT/'Mods/JEMCurrencyCore/src/main/resources/data/jem_economy/economy'
DOC=ROOT/'Docs/economy'
def read(p):return json.loads(p.read_text(encoding='utf-8'))
def write(p,x):p.write_text(json.dumps(x,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
def ceil(x):return -(-x.numerator//x.denominator)
def f32(value):return struct.unpack('!f',struct.pack('!f',value))[0]
def assembly_probabilities(outputs):
 # Exact Create6.0.8 rollResult: f32 sum/multiply, prefix f32 subtraction, first residual<0.
 weights=[f32(float(o.get('chance',1))) for o in outputs];total=f32(0)
 for w in weights:total=f32(total+w)
 grid=2**24;claimed=0;probabilities=[]
 for end in range(len(weights)):
  def negative(k):
   residual=f32(f32(k/grid)*total)
   for w in weights[:end+1]:residual=f32(residual-w)
   return residual<0
  low,high=0,grid
  while low<high:
   middle=(low+high)//2
   if negative(middle):low=middle+1
   else:high=middle
  boundary=low;probabilities.append(F(max(0,boundary-claimed),grid));claimed=max(claimed,boundary)
 return probabilities

def main():
 parser=argparse.ArgumentParser();parser.add_argument('--capture',type=Path,default=ROOT/'Server/local/economy-audit/loaded-registry.json');parser.add_argument('--apply',action='store_true');parser.add_argument('--potential',action='store_true');parser.add_argument('--exclude-recipe',action='append',default=[]);args=parser.parse_args()
 if args.apply and args.exclude_recipe:raise SystemExit('Candidate recipe exclusions cannot write production policy resources')
 capture=read(args.capture)
 if capture.get('schema')!=2:raise SystemExit('Current schema2 loaded capture required; refusing historical incomplete processing graph')
 policies={r['item']:r for r in read(DATA/'item_policies.json')};items={r['id']:r for r in capture['items']}
 native={}
 for filename in ['native_item_prices.json','profession_pools.json']:
  for r in read(DATA/filename):native.setdefault(r['item'],r)
 prices=dict(native)
 for i,p in policies.items():
  if p.get('outcome')!='NONE' or i not in prices:prices[i]=p
 def canonical_active(i):return i in policies and policies[i].get('outcome')!='NONE'
 def sale(i):
  p=prices.get(i,{})
  if p.get('outcome','BUY_AND_SELL') not in ['BUY_AND_SELL','SELL_ONLY'] or not p.get('sell_copper'):return F(0)
  return F(p['sell_copper'],p.get('sell_denominator',p.get('sell_units',1)))*(1 if p.get('fixed_price') or not canonical_active(i) else F(3,2))
 roots={};channels={};batches=collections.defaultdict(set);gaps=[]
 for r in capture.get('marketListings',[]):
  if r.get('outcome') not in ['BUY_ONLY','BUY_AND_SELL']:continue
  item=r['stack']['item'];options=[F(r['price'+str(q)],q) for q in [1,64,256,1024,4096] if r.get('price'+str(q),0)>0]
  if options:roots[item]=min(options);channels[item]='actual admitted Market full-order quote'
 for r in capture.get('nativeTrades',[]):
  if 'result' not in r:gaps.append({k:r.get(k) for k in ['profession','provider','sample_status']});continue
  a,b,out=[r.get(k,{}) for k in ['cost_a','cost_b','result']]
  good=a if out.get('item')=='minecraft:emerald' else out
  if good.get('item') in prices and good.get('count',0)>0:batches[good['item']].add(good['count'])
 for r in read(DATA/'profession_pools.json'):
  if r.get('count',0)>0:batches[r['item']].add(r['count'])
 # Conservative native-provider closure: any exact admitted merchandise may appear in any native legal batch.
 # Provider gaps cannot hide a cheaper monetary root: all64 possible counts are bounded, including absent offers.
 for i,p in prices.items():
  if i in native and p.get('buy_copper') and p.get('outcome','BUY_AND_SELL')!='SELL_ONLY':
   batches[i].update(range(1,min(64,items.get(i,{}).get('stackSize',64))+1))
 for i,counts in batches.items():
  p=prices[i]
  if not p.get('buy_copper') or p.get('outcome','BUY_AND_SELL')=='SELL_ONLY':continue
  unit=F(p['buy_copper'],p.get('buy_denominator',1));native_min=min(F(max(1,ceil(unit*q/2)),q) for q in counts)
  if i not in roots or native_min<roots[i]:roots[i]=native_min;channels[i]='actual native merchandise batch floor after bounded demand/racial discount'
 tags=collections.defaultdict(set);blocktags=collections.defaultdict(set)
 for item,row in items.items():
  for tag in row.get('tags',[]):tags[tag.split(' / ')[-1].rstrip(']')].add(item)
  for tag in row.get('block',{}).get('tags',[]):blocktags[tag.split(' / ')[-1].rstrip(']')].add(item)
 def ingredient(value):
  if isinstance(value,str):return [value] if value in items else sorted(tags.get(value.lstrip('#'),set()))
  if isinstance(value,list):return sorted({i for choice in value for i in ingredient(choice)})
  if not isinstance(value,dict):return []
  if 'item' in value:return [value['item']] if value['item'] in items else []
  if 'tag' in value:return sorted((blocktags if value.get('type')=='create:block_tag_ingredient' else tags).get(value['tag'],set()))
  if 'ingredient' in value:return ingredient(value['ingredient'])
  return []
 def fluid(value):
  name=re.search(r'FluidName:"([^"\n]+)"',value);amount=re.search(r'Amount:(\d+)',value)
  if not name or not amount:return None
  # Forge FluidStack writes a stable CompoundTag; keep complete NBT identity and remove amount only.
  key='fluid:'+re.sub(r'Amount:\d+,?','',value)
  return key,F(int(amount.group(1))),name.group(1)
 recipes=[];unmodeled=[]
 for r in capture['recipes']:
  groups=list(r.get('ingredients',[]));quantities=[F(1)]*len(groups);definition=r.get('definition',{})
  if not isinstance(definition,dict):definition={}
  if not groups:
   values=definition.get('ingredients')
   if values is None:
    values=[definition[key] for key in ['template','base','addition','ingredient'] if key in definition]
   for value in values or []:
    if isinstance(value,dict) and ('fluid' in value or 'fluidTag' in value):continue
    group=ingredient(value)
    groups.append(group);quantities.append(F(value.get('count',1) if isinstance(value,dict) else 1))
  # Create ItemApplicationRecipe holds ingredient1 when keepHeldItem is true: amortized catalyst, not consumed.
  if len(groups)>=2 and (definition.get('keepHeldItem') is True or r['type']=='create:deploying' and any(items.get(i,{}).get('durability',0)>0 for i in groups[1])):quantities[1]=F(0)
  sequence=r['type']=='create:sequenced_assembly'
  if sequence:
   transitional=definition.get('transitionalItem',{}).get('item');loops=definition.get('loops',1)
   for step in definition.get('sequence',[]):
    for ingredient_index,value in enumerate(step.get('ingredients',[])):
     if step.get('keepHeldItem') is True and ingredient_index==1:continue
     if isinstance(value,dict) and value.get('item')==transitional:continue
     if isinstance(value,dict) and ('fluid' in value or 'fluidTag'in value):continue
     group=ingredient(value)
     if group:
      tool=step.get('type')=='create:deploying' and ingredient_index==1 and any(items.get(i,{}).get('durability',0)>0 for i in group)
      groups.append(group);quantities.append(F(0) if tool else F(loops))
  for f in r.get('fluidInputs',[]):
   choices=[];amount=F(f.get('requiredAmount',f.get('amount',0)))
   for stack in f.get('resolvedStacks',[]):
    node=fluid(stack)
    if node:
     choices.append(node[0])
     if node[2] in ['minecraft:water','minecraft:lava']:roots[node[0]]=F(0);channels[node[0]]='Renewable native fluid conservative zero-cost bound'
   groups.append(choices);quantities.append(amount)
  outs=r.get('processingOutputs')
  if outs is None:
   outs=[{'item':r.get('output'),'count':r.get('count',0),'chance':'1'}]
   if isinstance(definition.get('results'),list):
    candidates=[x for x in definition['results'] if isinstance(x,dict) and isinstance(x.get('item'),str)]
    if candidates:outs=candidates
  outputs=collections.defaultdict(F);weight=sum((F(str(o.get('chance',1))) for o in outs),F(0)) if sequence else F(1)
  assembly_chances=assembly_probabilities(outs) if sequence else None
  def probability(o):
   if sequence:return assembly_chances[next(n for n,x in enumerate(outs) if x is o)]
   if 'processingOutputs' in r:
    value=struct.unpack('!f',struct.pack('!f',float(o.get('chance',1))))[0]
    p=F.from_float(value);grid=2**24
    return F(max(0,min(grid,(p*grid).numerator//(p*grid).denominator+1)),grid)
   return F(1) # Non-Create native recipe's declared deterministic result.

  for o in outs:
   if o.get('item') in items and o.get('count',1)>0 and probability(o)>0:
    outputs[o['item']]+=F(o.get('count',1))*probability(o)
  for stack in r.get('fluidOutputs',[]):
   node=fluid(stack)
   if node:outputs[node[0]]+=node[1]
  if not groups or any(not g for g in groups) or not outputs:
   unmodeled.append({'id':r['id'],'type':r['type'],'reason':'No exact resolved consumable groups or outputs; dynamic/NBT-specific conversion retained for explicit owner review'});continue
  recipes.append({'id':r['id'],'type':r['type'],'groups':groups,'quantities':quantities,'outputs':dict(outputs),'singleRemainderReturnProven':r['type']=='create:emptying' and r.get('class')=='com.simibubi.create.content.fluids.transfer.EmptyingRecipe','source':r.get('sourcePack','dynamic loaded recipe'),'fuel_cost':'zero lower bound (Create fan compatible)','chanceModel':'Exact Java float32 nextFloat24 grid: ProcessingOutput<=chance; assembly f32 weighted first-negative selection' if r['type'].startswith('create:') else 'Declared deterministic outputs; unknown non-Create stochastic serializer uses all positive outputs as conservative simultaneous upper bound'})
 # Replay current explicit admission as well as captured legacy fallback stock.
 # New reviewed Create policies can admit stock absent from the provisional capture.
 admitted={r['stack']['item'] for r in capture.get('marketListings',[]) if r.get('outcome') in ['BUY_ONLY','BUY_AND_SELL']}
 restrictions=read(DATA/'trade_restrictions.json')
 for i,p in policies.items():
  if i not in items:continue
  path=i.split(':')[1];row=items[i]
  exact_blocked=i in restrictions['all_npc_items'] or any(path.startswith(x) for x in restrictions['all_npc_path_prefixes']) or path.endswith('_trophy')
  sale_allowed=p.get('outcome') in ['BUY_AND_SELL','SELL_ONLY'] and p.get('sell_copper')
  item_tags={x.split(' / ')[-1].rstrip(']') for x in row.get('tags',[])}
  block_tags={x.split(' / ')[-1].rstrip(']') for x in row.get('block',{}).get('tags',[])}
  npc_blocked=exact_blocked or any(path.endswith(x) for x in restrictions['all_npc_path_suffixes']) or bool(item_tags&set(restrictions['all_npc_item_tags'])) or bool(block_tags&set(restrictions['all_npc_block_tags']))
  if (sale_allowed and not exact_blocked) or (p.get('outcome') in ['BUY_AND_SELL','BUY_ONLY'] and not npc_blocked):admitted.add(i)
 for i,p in policies.items():
  if i not in admitted or p.get('outcome') not in ['BUY_ONLY','BUY_AND_SELL'] or not p.get('buy_copper'):continue
  unit=F(p['buy_copper'],p.get('buy_denominator',1));quoted=min(F(ceil(unit*q),q) for q in range(1,65))
  if p.get('bulk_1024') is not None:quoted=min(quoted,F(p['bulk_1024'],1024))
  if i not in roots or quoted<roots[i]:roots[i]=quoted;channels[i]='current canonical full-order price over captured actual admission'
 remainder_items={r['id'] for r in capture['items'] if r.get('craftingRemainder')}
 supported=[]
 for r in recipes:
  if r['id']=='farm_and_charm:mincer/raw_copper_from_copper_ingot':
   unmodeled.append({'recipe':r['id'],'type':r['type'],'reason':'ACTIVE_USER_ALLOWED_AUTOMATION_EXEMPTION: user explicitly retains copper self-recovery and positive byproduct buyback; this active productive circuit is outside the no-arbitrage inequalities.'})
  elif r['id'] in args.exclude_recipe:
   unmodeled.append({'recipe':r['id'],'type':r['type'],'reason':'Candidate recipe removal only; NOT production active.'})
  elif any(i in remainder_items for g in r['groups'] for i in g) and r['type'] not in ['minecraft:crafting_shaped','minecraft:crafting_shapeless','create:emptying']:
   unmodeled.append({'recipe':r['id'],'type':r['type'],'reason':'Native container remainder consumption/return handler not yet proven; excluded from monetary certificate.'})
  else:supported.append(r)
 recipes=supported
 write(DOC/'shipping-recipe-flow.json',{'capturedUtc':capture['capturedUtc'],'roots':{i:str(v) for i,v in roots.items()},'recipes':[{**r,'quantities':[str(q) for q in r['quantities']],'outputs':{i:str(q) for i,q in r['outputs'].items()}} for r in recipes]})
 costs=dict(roots);routes={i:channels[i] for i in roots};passes=0;last_changed=set()
 for passes in range(1 if args.potential else 64):
  changed=False;last_changed=set()
  for r in recipes:
   choices=[min((costs[i] for i in group if i in costs),default=None) for group in r['groups']]
   if any(c is None for c in choices):continue
   total=sum((c*q for c,q in zip(choices,r['quantities'])),F(0))
   for i,q in r['outputs'].items():
    cost=total/q
    if i not in costs or cost<costs[i]:costs[i]=cost;routes[i]=r['id'];changed=True;last_changed.add(i)
  if not changed:break
 unsafe=[];multi=[]
 for i,p in prices.items():
  if i in costs and sale(i)>costs[i]:unsafe.append({'item':i,'maxSaleUnit':str(sale(i)),'minimumInputCost':str(costs[i]),'route':routes[i],
    'canonicalPolicy':i in policies,'fixedUserAnchor':p.get('fixed_price',False) or p.get('notes','').startswith('Immutable approved resource anchor')})
 for r in recipes:
  choices=[min((costs[i] for i in group if i in costs),default=None) for group in r['groups']]
  if any(c is None for c in choices):continue
  cost=sum((c*q for c,q in zip(choices,r['quantities'])),F(0));payout=sum((sale(i)*q for i,q in r['outputs'].items()),F(0))
  if payout>cost:multi.append({'recipe':r['id'],'maxAllOutputsSale':str(payout),'minimumInputCost':str(cost),'outputs':{i:str(q) for i,q in r['outputs'].items()}})
 report={'capture':str(args.capture.relative_to(ROOT)),'capturedUtc':capture['capturedUtc'],'artifacts':capture.get('artifacts',[]),
  'method':'Exact Fraction expected probabilities and Java float32 Create24-bit draw semantics; final certificate bounds arbitrary finite coproduct chains after approved dedicated guard. Preliminary minimum-cost passes are explanatory, not the acceptance gate.',
  'registeredItems':len(items),'loadedRecipes':len(capture['recipes']),'modeledItemRecipes':len(recipes),'passes':passes+1,'converged':not changed,
  'nonConvergedNodes':[{"id":i,"route":routes[i],"minimumCost":str(costs[i])} for i in sorted(last_changed)] if changed else [],
  'unmodeledRecipes':unmodeled,'nativeProviderGaps':gaps,'unsafeSingleOutputs':unsafe,'unsafeSimultaneousOutputs':multi,
  'nativeProviderPriceBound':'Exact native merchandise allowlist conservatively evaluated at every legal native batch1..64, so unsampled providers cannot introduce an unbounded cheaper root; arbitrary NBT only raises purchase premiums and never buyback.',
  'scopeLimits':['Growth/world loot are gameplay production with land/time, not free instantaneous conversion.','Randomized/native provider gaps remain explicit.','Dynamic serializers with conditional/NBT input counts require owning exact adapter; no fabricated closure.']}
 write(DOC/'shipping-conversion-audit.json',report)
 if args.potential:
  from monetary_potential import solve_and_author
  certificate=solve_and_author(capture,policies,native,prices,roots,recipes,args.apply)
  report['potentialCertificate']=certificate;write(DOC/'shipping-conversion-audit.json',report)
  print('Exact potential certificate',certificate['status'],'recipes',certificate['recipes'],'decisions',certificate.get('decisions',0));return
 if args.apply:
  fixed={'minecraft:cod','minecraft:salmon','minecraft:beef','minecraft:leather'}
  family={};factors={};multipliers={};base={};protected=set();reasons=collections.defaultdict(list)
  for i,p in prices.items():
   if sale(i)<=0:continue
   owner=p.get('quota_family',i) if canonical_active(i) else i;units=p.get('quota_units',1) if canonical_active(i) else 1
   family[i]=owner;factors[i]=F(units);multipliers[i]=F(1) if p.get('fixed_price') or not canonical_active(i) else F(3,2)
   current=F(p['sell_copper'],p.get('sell_denominator',p.get('sell_units',1)))/units
   base[owner]=min(base.get(owner,current),current)
   if i in fixed or p.get('fixed_price') or p.get('user_income_anchor'):protected.add(owner)
  margin=F(9,10)
  for i,cost in costs.items():
   if i not in family or family[i] in protected:continue
   owner=family[i];ceiling=cost*margin/(factors[i]*multipliers[i])
   if ceiling<base[owner]:base[owner]=ceiling;reasons[owner].append({'endpoint':i,'route':routes[i],'minimumInputCost':str(cost)})
  for correction_pass in range(128):
   changed_policy=False
   for r in recipes:
    choices=[min((costs[i] for i in group if i in costs),default=None) for group in r['groups']]
    if any(c is None for c in choices):continue
    cost=sum((c*q for c,q in zip(choices,r['quantities'])),F(0));locked=F(0);adjustable=F(0);affected=set()
    for i,q in r['outputs'].items():
     if i not in family:continue
     value=base[family[i]]*factors[i]*multipliers[i]*q
     if family[i] in protected:locked+=value
     else:adjustable+=value;affected.add(family[i])
    budget=cost*margin-locked
    if locked>cost:raise SystemExit('Immutable income/disc anchor conflicts with exact recipe '+r['id'])
    if adjustable>max(F(0),budget):
     ratio=max(F(0),budget)/adjustable
     for owner in affected:
      base[owner]*=ratio;reasons[owner].append({'recipe':r['id'],'coproductCost':str(cost),'fixedPayout':str(locked),'allocationRatio':str(ratio)})
     changed_policy=True
   if not changed_policy:break
  # Serialize a family ONCE. Every member multiplies this exact common fraction, preserving9:1 equality.
  for owner in base:
   max_units=max((factors[i] for i in family if family[i]==owner),default=F(1))
   den=min(1000000,max(1,int(F(999999999,max(F(1),base[owner]*max_units)))))
   amount=(base[owner]*den).numerator//(base[owner]*den).denominator
   base[owner]=F(amount,den)
  decisions=[]
  def apply_row(i,p,native_row=False):
   if i not in family or family[i] in protected:return
   target=base[family[i]]*factors[i]
   original=F(p['sell_copper'],p.get('sell_denominator',p.get('sell_units',1)))
   if target>=original:return
   # Target is already the single serialized family fraction times exact integer quota units.
   amount=target.numerator;den=target.denominator
   if amount>1000000000 or den>1000000000:raise SystemExit('Rational family exceeds checked parser bounds: '+i)
   if amount==0:
    if i in policies:
     p['outcome']='BUY_ONLY' if p.get('buy_copper') else 'NONE';p['sell_copper']=None
    else:
     # Extremely small but positive representation is not a proof for an actually zero-cost recipe.
     raise SystemExit('Native merchandise zero-cost conversion needs explicit admission decision: '+i)
   else:
    exact=F(amount,den);p['sell_copper']=exact.numerator;p['sell_units' if native_row else 'sell_denominator']=exact.denominator
   p['recipe_audit_adjusted']={'capturedUtc':capture['capturedUtc'],'decision':'User: preserve cheap building prices, adjust derived buyback','method':'90percent of conservative minimum funded input cost after maximum demand; summed expected byproducts share same budget','family':family[i]}
   decisions.append({'item':i,'before':str(original),'after':str(F(amount,den)) if amount else 'NO_POSITIVE_SAFE_BUYBACK','family':family[i],'reasons':reasons[family[i]]})
  for i,p in policies.items():
   if p.get('sell_copper'):apply_row(i,p)
  for filename in ['native_item_prices.json','profession_pools.json']:
   rows=read(DATA/filename)
   for p in rows:
    if p['item'] not in policies or policies[p['item']].get('outcome')=='NONE':
     if p.get('sell_copper'):apply_row(p['item'],p,True)
   write(DATA/filename,rows)
  write(DATA/'item_policies.json',sorted(policies.values(),key=lambda r:r['item']))
  report['appliedDecisions']=decisions;report['userDecision']='Preserve cheap building prices; adjust derived buyback; fixed disc/cod/salmon/beef/leather income unchanged'
  report['certificate']='Every modeled expected coproduct liquidation is bounded below90percent of the conservative input acquisition bound, excluding immutables; rational serialization rounds down. Full final rerun required.'
  write(DOC/'shipping-conversion-audit.json',report)
  print('Applied buyback decisions',len(decisions))
 print(json.dumps({k:report[k] for k in ['registeredItems','loadedRecipes','modeledItemRecipes','passes','converged']}));print('Unsafe endpoints',len(unsafe),'simultaneous outputs',len(multi),'unmodeled',len(unmodeled),'native gaps',len(gaps))
if __name__=='__main__':main()
