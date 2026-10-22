"""Exact monetary-potential certificate for production economy authoring.
LP proposes a bounded value assignment; Fraction inequalities are the acceptance gate.
No runtime catalog, game actions, tests, or code inspection.
"""
from collections import defaultdict
from fractions import Fraction as F
from pathlib import Path
import json,hashlib
import numpy as np
from scipy.optimize import linprog
from scipy.sparse import coo_matrix
ROOT=Path(__file__).resolve().parents[3]
DATA=ROOT/'Mods/JEMEconomy/src/main/resources/data/jem_economy/economy'
def write(p,x):p.write_text(json.dumps(x,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
def read(p):return json.loads(p.read_text(encoding='utf-8'))
def solve_and_author(capture,policies,native,prices,roots,recipes,apply):
 items={r['id']:r for r in capture['items']};remainders={i:r['craftingRemainder'] for i,r in items.items() if r.get('craftingRemainder')}
 fixed={'minecraft:cod','minecraft:salmon','minecraft:beef','minecraft:leather'}
 def active(i):return i in policies and policies[i].get('outcome')!='NONE'
 family={};factor={};demand={};desired={};protected=set()
 for i,p in prices.items():
  if p.get('outcome','BUY_AND_SELL') not in ['BUY_AND_SELL','SELL_ONLY'] or not p.get('sell_copper'):continue
  owner=p.get('quota_family',i) if active(i) else i;units=p.get('quota_units',1) if active(i) else 1
  multi=F(1) if p.get('fixed_price') or not active(i) else F(3,2)
  family[i]=owner;factor[i]=F(units);demand[i]=multi
  raw=F(p['sell_copper'],p.get('sell_denominator',p.get('sell_units',1)))/units
  desired[owner]=min(desired.get(owner,raw),raw)
  if i in fixed or p.get('fixed_price') or p.get('user_income_anchor'):protected.add(owner)
 nodes=set(roots)|set(family)|set(remainders.values())
 for r in recipes:
  nodes.update(r['outputs'])
  for g in r['groups']:nodes.update(g)
 nodes=sorted(nodes);index={i:n for n,i in enumerate(nodes)}
 def group_keys(r):
  consumed=[(g,q) for g,q in zip(r['groups'],r['quantities']) if q>0]
  result=[]
  for g,q in zip(r['groups'],r['quantities']):
   returns={remainders.get(i) for i in g};deduct=True
   # Vanilla default remainders are added to explicit outputs; retaining that upper
   # bound for other handlers is conservative until their caller is proven exact.
   if len(consumed)==1 and len(returns)==1 and None not in returns:
    remainder=next(iter(returns))
    if r['outputs'].get(remainder,F(0))>=q and r.get('singleRemainderReturnProven'):deduct=False
   result.append((tuple(sorted(g)),deduct))
  return result
 for r in recipes:r['potentialGroups']=group_keys(r)
 groups={key for r in recipes for key in r['potentialGroups']};groups=sorted(groups);gindex={g:len(index)+n for n,g in enumerate(groups)}
 fkeys=sorted(desired);findex={f:len(index)+len(groups)+n for n,f in enumerate(fkeys)}
 count=len(index)+len(groups)+len(fkeys);bounds=[(0,float(roots[i]) if i in roots else None) for i in nodes]
 # A maximum-sum objective may sacrifice a valid family entirely. Require a
 # representable positive price in the constraints, rather than inventing a
 # floor after certification. The final downward quantization remains safe.
 bounds.extend((None,None) for _ in groups);bounds.extend((1,1) if f in protected or not apply else (float(min(F(1),F(2,1000000)/desired[f])),1) for f in fkeys)
 objective=np.zeros(count)
 for f in fkeys:objective[findex[f]]=-1
 rows=[];cols=[];values=[];b=[];labels=[]
 def constraint(terms,label='ingredient'):
  labels.append(label)
  n=len(b);b.append(0.0)
  for c,v in terms.items():
   if v:rows.append(n);cols.append(c);values.append(float(v))
 for key in groups:
  g,deduct=key
  for i in g:
   terms=defaultdict(F);terms[gindex[key]]+=1;terms[index[i]]-=1
   if deduct and i in remainders:terms[index[remainders[i]]]+=1
   constraint(terms)
 for r in recipes:
  terms=defaultdict(F)
  for i,q in r['outputs'].items():terms[index[i]]+=q
  for key,q in zip(r['potentialGroups'],r['quantities']):terms[gindex[key]]-=q
  constraint(terms,r['id'])
 for i,f in family.items():constraint({findex[f]:desired[f]*factor[i]*demand[i],index[i]:F(-1)})
 matrix=coo_matrix((values,(rows,cols)),shape=(len(b),count)).tocsr()
 prior_path=ROOT/'Docs/economy/shipping-monetary-potential-certificate.json'
 def input_fingerprint(price_rows):
  return hashlib.sha256(json.dumps({'roots':{i:str(v) for i,v in sorted(roots.items())},'recipes':recipes,'prices':price_rows},sort_keys=True,default=str).encode()).hexdigest()
 fingerprint=input_fingerprint(prices)
 prior=read(prior_path) if not apply and prior_path.exists() else {}
 # A previously certified exact assignment is a proposal only: all current
 # recipes, roots and actual stored buy/sell rows are rechecked below.
 proposal={i:F(prior['potential'][i]) for i in nodes} if all(i in prior.get('potential',{}) for i in nodes) else None
 def current_witness(candidate):
  if candidate is None or any(value<0 for value in candidate.values()):return False
  if any(candidate[i]>cost for i,cost in roots.items()):return False
  for i in family:
   row=prices[i];sale=F(row['sell_copper'],row.get('sell_denominator',row.get('sell_units',1)))
   if sale*demand[i]>candidate[i]:return False
  for recipe in recipes:
   available=sum((min(candidate[i]-(candidate.get(remainders.get(i),F(0)) if deduct else F(0)) for i in g)*q for (g,deduct),q in zip(recipe['potentialGroups'],recipe['quantities'])),F(0))
   if sum((candidate[i]*q for i,q in recipe['outputs'].items()),F(0))>available:return False
  return True
 # Price lowering changes the input fingerprint without invalidating a witness.
 # Exact current inequalities, rather than fingerprint identity, decide reuse.
 if current_witness(proposal):
  potential=proposal;iteration=0;projection_zero=[]
  print('Revalidating prior exact assignment against current recipe/root/serialized terms',flush=True)
 else:
  print('Solving monetary potential',len(nodes),'nodes',len(b),'constraints',flush=True)
  result=linprog(objective,A_ub=matrix,b_ub=np.array(b),bounds=bounds,method='highs',options={'primal_feasibility_tolerance':1e-9,'dual_feasibility_tolerance':1e-9})
  print('Monetary LP result',result.message,flush=True)
  if not result.success:
   relaxed=list(bounds)
   for f in fkeys:
    if f not in protected:relaxed[findex[f]]=(0,1)
   diagnostic=np.zeros(count);diagnostic[findex['minecraft:clay_ball']]=-1
   probe=linprog(diagnostic,A_ub=matrix,b_ub=np.array(b),bounds=relaxed,method='highs')
   evidence={'status':probe.message,'maximumClayRetention':float(probe.x[findex['minecraft:clay_ball']]) if probe.success else None,'support':[{'constraint':labels[n],'dual':float(x)} for n,x in enumerate(probe.ineqlin.marginals) if x] if probe.success else []}
   write(ROOT/'Docs/economy/shipping-potential-diagnostic.json',evidence)
   raise SystemExit('Monetary price constraints require explicit decision: '+result.message)
  # Rational reconstruction followed by an exact monotone feasibility projection.
  grid=10**9
  potential={i:F(str(max(0.0,float(result.x[index[i]])))).limit_denominator(grid) for i in nodes}
  print('Reconstruction core', {i:str(potential[i]) for i in ['minecraft:netherrack','minecraft:magma_cream','minecraft:magma_block','minecraft:gravel','netherexp:smooth_netherrack']},flush=True)
  for i,cost in roots.items():potential[i]=min(potential[i],cost)
  # Reconstruct the active LP face over exact rationals. Independent rounding of
  # coupled equalities otherwise produces an endless sequence of tiny reductions.
  equations=[]
  active_minimum={i:(desired[family[i]] if family[i] in protected or not apply else min(desired[family[i]],F(2,1000000)))*factor[i]*demand[i] for i in family}
  for i in nodes:
   if potential[i]==0:equations.append(({i:F(1)},F(0)))
   elif i in roots and abs(float(potential[i]-roots[i]))<1e-10:equations.append(({i:F(1)},roots[i]))
   elif i in active_minimum and abs(float(potential[i]-active_minimum[i]))<1e-10:equations.append(({i:F(1)},active_minimum[i]))
  for r in recipes:
   terms=defaultdict(F)
   for i,q in r['outputs'].items():terms[i]+=q
   for (g,deduct),q in zip(r['potentialGroups'],r['quantities']):
    chosen=min(g,key=lambda i:potential[i]-(potential.get(remainders.get(i),F(0)) if deduct else F(0)))
    terms[chosen]-=q
    if deduct and chosen in remainders:terms[remainders[chosen]]+=q
   terms={i:q for i,q in terms.items() if q}
   if terms and abs(float(sum((q*potential[i] for i,q in terms.items()),F(0))))<1e-10:equations.append((terms,F(0)))
  pivots={};order=[]
  for terms,rhs in equations:
   terms=dict(terms)
   while terms:
    pivot=min(terms,key=lambda i:index[i])
    if pivot not in pivots:
     coefficient=terms[pivot];terms={i:q/coefficient for i,q in terms.items()};rhs/=coefficient
     pivots[pivot]=(terms,rhs);order.append(pivot);break
    row,value=pivots[pivot];coefficient=terms.pop(pivot);rhs-=coefficient*value
    for i,q in row.items():
     if i==pivot:continue
     value=terms.get(i,F(0))-coefficient*q
     if value:terms[i]=value
     else:terms.pop(i,None)
  for pivot in sorted(pivots,key=lambda i:index[i],reverse=True):
   terms,rhs=pivots[pivot];potential[pivot]=rhs-sum((q*potential[i] for i,q in terms.items() if i!=pivot),F(0))
  print('Exact active-face reconstruction',len(pivots),'pivots',flush=True)
  minimum=active_minimum
  for i,value in minimum.items():potential[i]=max(potential[i],value)
  # Collapse exact two-way single-material transformations before rational projection.
  # Otherwise independent rounding can make9:1 compression oscillate forever despite a feasible exact price.
  parent={i:i for i in nodes};scale={i:F(1) for i in nodes}
  def find(i):
   if parent[i]!=i:
    old=parent[i];parent[i],factor_to_root=find(old);scale[i]*=factor_to_root
   return parent[i],scale[i]
  def union(a,b,ratio):
   ra,wa=find(a);rb,wb=find(b)
   if ra!=rb:parent[rb]=ra;scale[rb]=ratio*wa/wb
  edges=defaultdict(set)
  for r in recipes:
   combined=defaultdict(F)
   for g,q in zip(r['groups'],r['quantities']):
    if q>0:combined[tuple(g)]+=q
   consumed=[(g,q) for g,q in combined.items()]
   if len(consumed)!=1 or len(r['outputs'])!=1:continue
   out,outq=next(iter(r['outputs'].items()));g,qty=consumed[0]
   for i in g:
    if i not in remainders:edges[(i,out)].add(qty/outq)
  for (source,target),ratios in edges.items():
   for ratio in ratios:
    if 1/ratio in edges.get((target,source),set()):union(source,target,ratio)
  components=defaultdict(list)
  for i in nodes:
   owner,w=find(i);components[owner].append((i,w))
  component_value={owner:min(potential[i]/w for i,w in members) for owner,members in components.items()}
  component_floor={owner:max((minimum.get(i,F(0))/w for i,w in members),default=F(0)) for owner,members in components.items()}
  for owner in component_value:component_value[owner]=max(component_value[owner],component_floor[owner])
  def refresh():
   for owner,members in components.items():
    for i,w in members:potential[i]=component_value[owner]*w
  refresh()
  projection_zero=[]
  for iteration in range(256):
   violations=[]
   for r in recipes:
    net=defaultdict(F)
    for i,q in r['outputs'].items():
     owner,w=find(i);net[owner]+=q*w
    for (g,deduct),q in zip(r['potentialGroups'],r['quantities']):
     i=min(g,key=lambda x:potential[x]-(potential.get(remainders.get(x),F(0)) if deduct else F(0)))
     owner,w=find(i);net[owner]-=q*w
     if deduct and i in remainders:
      owner,w=find(remainders[i]);net[owner]+=q*w
    net={owner:q for owner,q in net.items() if q}
    balance=sum((q*component_value[owner] for owner,q in net.items()),F(0))
    if balance<=0:continue
    available=-sum((q*component_value[owner] for owner,q in net.items() if q<0),F(0))
    locked=sum((q*component_floor[owner] for owner,q in net.items() if q>0),F(0))
    flexible=sum((q*(component_value[owner]-component_floor[owner]) for owner,q in net.items() if q>0),F(0))
    if locked>available:raise SystemExit('Exact fixed-income certificate blocked by '+r['id'])
    ratio=(available-locked)/flexible if flexible else F(0)
    if ratio==0:projection_zero.append({'recipe':r['id'],'available':str(available),'nodes':[i for owner,q in net.items() if q>0 for i,w in components[owner]]})
    for owner,q in net.items():
     if q>0:
      component_value[owner]=component_floor[owner]+(component_value[owner]-component_floor[owner])*ratio
      for i,w in components[owner]:potential[i]=component_value[owner]*w
    violations.append(r['id'])
   if iteration%8==0:print('Exact component projection',iteration+1,'remaining recipe violations',len(violations),violations[:20],flush=True)
   if not violations:break
  else:raise SystemExit('Exact component projection did not converge: '+str(violations[:8]))
  for i,cost in roots.items():
   if potential[i]>cost:raise SystemExit('Potential exceeds funded purchase bound: '+i)
  for i,value in potential.items():
   if value<0:raise SystemExit('Negative monetary potential: '+i)
 for i,value in potential.items():
  if value<0:raise SystemExit('Negative monetary potential: '+i)
 for i,cost in roots.items():
  if potential[i]>cost:raise SystemExit('Potential exceeds current funded purchase bound: '+i)
 targets={}
 for f in fkeys:
  target=min([desired[f]]+[potential[i]/(factor[i]*demand[i]) for i in family if family[i]==f])
  maxunits=max((factor[i] for i in family if family[i]==f),default=F(1))
  den=1000000
  targets[f]=F((target*den).numerator//(target*den).denominator,den) if f not in protected else desired[f]
 certificates=[]
 for r in recipes:
  available=sum((min(potential[i]-(potential.get(remainders.get(i),F(0)) if deduct else F(0)) for i in g)*q for (g,deduct),q in zip(r['potentialGroups'],r['quantities'])),F(0))
  outputs=sum((potential[i]*q for i,q in r['outputs'].items()),F(0))
  if outputs>available:raise SystemExit('Rational certificate failed '+r['id'])
  certificates.append({'recipe':r['id'],'inputPotential':str(available),'outputPotential':str(outputs),'expectedItemOutputs':{i:str(q) for i,q in r['outputs'].items()},'defaultRemaindersDeducted':True,'chanceModel':r.get('chanceModel','')})
 decisions=[]
 def update(i,row,native_row=False):
  if i not in family or family[i] in protected:return
  value=targets[family[i]]*factor[i];before=F(row['sell_copper'],row.get('sell_denominator',row.get('sell_units',1)))
  if value>=before and 1000000%before.denominator==0:return
  if value>before:value=F((before*1000000).numerator//(before*1000000).denominator,1000000)
  if value==0:
   if native_row:
    row['sell_copper']=0;row['buyback_blocked_reason']='No positive potential under the conservatively modeled current recipe graph; purchase remains admitted.'
   else:
    row['sell_copper']=None;row['outcome']='BUY_ONLY' if row.get('buy_copper') else 'NONE'
    row['buyback_blocked_reason']='No positive potential under the conservatively modeled current recipe graph.'
  else:
   row['sell_copper']=value.numerator;row['sell_units' if native_row else 'sell_denominator']=value.denominator
  row['recipe_audit_adjusted']={'capturedUtc':capture['capturedUtc'],'decision':'User: preserve cheap construction, adjust derived buyback','certificate':'Exact expected-recipe monetary potentials; arbitrary-length conversion and coproduct composition','family':family[i]}
  decisions.append({'item':i,'before':str(before),'after':str(value),'family':family[i]})
 if apply:
  for i,p in policies.items():
   if p.get('sell_copper'):update(i,p)
  prepared=[]
  for file in ['native_item_prices.json','profession_pools.json']:
   rows=read(DATA/file)
   for row in rows:
    if not active(row['item']) and row.get('sell_copper'):update(row['item'],row,True)
    elif row.get('sell_copper'):
     # Inactive fallback rows still obey the same residual denominator bound,
     # including if a future canonical directional policy stops admitting them.
     value=F(row['sell_copper'],row.get('sell_units',1));value=F((value*1000000).numerator//(value*1000000).denominator,1000000)
     row['sell_copper']=value.numerator;row['sell_units']=value.denominator
   prepared.append((DATA/file,rows))
  # Exact post-serialization acceptance uses the actual active resource rows.
  for i,row in policies.items():
   if row.get('sell_copper'):
    value=F(row['sell_copper'],row.get('sell_denominator',1))
    if 1000000%value.denominator or value*demand[i]>potential[i]:raise SystemExit('Serialized canonical proof failed: '+i)
  for path,rows in prepared:
   for row in rows:
    if not row.get('sell_copper'):continue
    value=F(row['sell_copper'],row.get('sell_units',1))
    if 1000000%value.denominator:raise SystemExit('Native fallback denominator not bounded: '+row['item'])
    if not active(row['item']) and row['item'] in potential and value>potential[row['item']]:raise SystemExit('Serialized native proof failed: '+row['item'])
 # Bind the certificate to BOTH directions of the actual stored resources.
 # A stale/mis-copied purchase denominator must fail here, before deployment.
 stored=policies if apply else {r['item']:r for r in read(DATA/'item_policies.json')}
 root_terms=[]
 for i,row in stored.items():
  outcome=row.get('outcome','NONE');sell=row.get('sell_copper');buy=row.get('buy_copper')
  for key in ['sell_denominator','buy_denominator','quota_units']:
   if not 0<row.get(key,1)<=1000000000:raise SystemExit('Stored policy field out of runtime range: '+i+'/'+key)
  if outcome not in ['NONE','BUY_ONLY','SELL_ONLY','BUY_AND_SELL']:raise SystemExit('Invalid stored policy outcome: '+i)
  if outcome in ['BUY_ONLY','BUY_AND_SELL'] and (not buy or not 0<buy<=1000000000):raise SystemExit('Stored buy numerator out of runtime range: '+i)
  if outcome in ['BUY_AND_SELL','SELL_ONLY']:
   if not sell or row.get('daily_player_units',0)<=0 or row.get('daily_global_units',0)<=0:raise SystemExit('Invalid stored sale admission: '+i)
   value=F(sell,row.get('sell_denominator',1))
   if not 0<sell<=1000000000000000 or -(-value.numerator//value.denominator)>1000000000 or value<=0 or 1000000%value.denominator or i not in potential or value*demand[i]>potential[i]:raise SystemExit('Stored sale proof failed: '+i)
  if outcome in ['BUY_AND_SELL','BUY_ONLY']:
   if not buy or buy<=0:raise SystemExit('Invalid stored purchase admission: '+i)
   value=F(buy,row.get('buy_denominator',1))
   if outcome=='BUY_AND_SELL' and value<=F(sell,row.get('sell_denominator',1))*F(3,2):raise SystemExit('Unsafe stored rational spread: '+i)
   retail=min(F(-(-value.numerator*q//value.denominator),q) for q in range(1,65))
   cost=min(retail,F(row['bulk_1024'],1024)) if row.get('bulk_1024') else retail
   if i in roots and i in potential and potential[i]>cost:raise SystemExit('Stored admitted purchase price below potential: '+i)
   root_terms.append({'item':i,'buyNumerator':buy,'buyDenominator':row.get('buy_denominator',1),'bulk1024':row.get('bulk_1024'),'retailMinimumUnit':str(retail),'minimumUnit':str(cost)})
 native_stored={}
 for file in ['native_item_prices.json','profession_pools.json']:
  rows=next(rows for path,rows in prepared if path.name==file) if apply else read(DATA/file)
  for row in rows:
   previous=native_stored.setdefault(row['item'],row)
   if (previous['buy_copper'],previous['sell_copper'],previous.get('sell_units',1))!=(row['buy_copper'],row['sell_copper'],row.get('sell_units',1)):raise SystemExit('Conflicting stored native definitions: '+row['item'])
 for i,row in native_stored.items():
  native_sell=F(row['sell_copper'],row.get('sell_units',1))
  if row['buy_copper']<=0 or native_sell<0 or row['buy_copper']<=2*native_sell or not 0<row.get('sell_units',1)<=2147483647:raise SystemExit('Unsafe stored native parser/spread terms: '+i)
  effective=stored[i] if i in stored and stored[i].get('outcome')!='NONE' else row
  if effective.get('outcome','BUY_AND_SELL')=='SELL_ONLY' or not effective.get('buy_copper'):continue
  unit=F(effective['buy_copper'],effective.get('buy_denominator',1))
  cost=min(F(max(1,-(-(unit*q/2).numerator//(unit*q/2).denominator)),q) for q in range(1,min(64,items.get(i,{}).get('stackSize',64))+1))
  if i in potential and potential[i]>cost:raise SystemExit('Stored native purchase floor below potential: '+i)
  root_terms.append({'item':i,'nativeConservativeHalfPriceMinimumUnit':str(cost)})
 if apply:
  # No resource is written until both-direction and native root gates accept
  # the complete prepared proposal.
  for path,rows in prepared:write(path,rows)
  write(DATA/'item_policies.json',sorted(policies.values(),key=lambda r:r['item']))
  persisted_prices={}
  for filename in ['native_item_prices.json','profession_pools.json']:
   for row in read(DATA/filename):persisted_prices.setdefault(row['item'],row)
  for row in read(DATA/'item_policies.json'):
   if row.get('outcome')!='NONE' or row['item'] not in persisted_prices:persisted_prices[row['item']]=row
  fingerprint=input_fingerprint(persisted_prices)
 report={'status':'EXACT_EXPECTED_RECIPE_POTENTIAL_CERTIFIED','capturedUtc':capture['capturedUtc'],'recipes':len(recipes),'nodes':len(nodes),
  'unitPriceQuantumDenominator':1000000,'maximumSaleRemainderDenominator':100000000,'constraints':len(b),'projectionPasses':iteration+1,'decisions':len(decisions),'applied':apply,
  'proof':'Each consumed ingredient deducts its captured default crafting remainder; retained catalysts consume zero. Each purchase has cost>=potential; each expected recipe output sum<=minimum input sum for every alternative; each maximum-demand sell payout<=potential. Therefore any finite composition of modeled conversions has non-positive expected monetary gain, including coproducts. Funds/quotas/replay are separate runtime gates.',
  'limits':['ACTIVE_USER_ALLOWED_AUTOMATION_EXEMPTION: copper ingot to raw copper mincer remains active with positive byproduct buyback; no no-arbitrage claim covers compositions using that productive circuit.','Expected probabilities, not a guarantee every lucky finite craft loses money.','Only current captured and explicitly modeled recipes; dynamic/NBT/progression and unproven native container remainder handlers remain explicitly excluded.','Default crafting remainders are conservatively returned; GenericItemEmptying exact native caller returns only the explicit single output, with no extra remainder.','No runtime/player/crash acceptance.'],
  'storedResourceBuyAndSellCertified':True,'purchaseRootTerms':root_terms,
  'resourceSha256':{file:hashlib.sha256((DATA/file).read_bytes()).hexdigest() for file in ['item_policies.json','native_item_prices.json','profession_pools.json']},
  'inputFingerprint':fingerprint,'fixedIncomes':{i:str(desired[family[i]]*factor[i]) for i in family if family[i] in protected},'priceDecisions':decisions,'projectionZeroCauses':projection_zero,
  'potential':{i:str(value) for i,value in potential.items()},'recipeCertificates':certificates}
 write(ROOT/'Docs/economy/shipping-monetary-potential-certificate.json',report)
 return {k:v for k,v in report.items() if k not in ['potential','recipeCertificates','priceDecisions']}
