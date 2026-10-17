# Market storefront r1

User direction (2026-10-17): match supplied user-reference.png, with a live mouse-following librarian replacing the painted character and one category dropdown replacing the category tabs. The user explicitly requested generation of missing artwork, then integration of the generated result. The generated plate is authorized for this task; no third-party pixel extraction is used.

Exact Taste Engine pair: C:/MyProjects/Coding/Solo/Ultimate-Skills/Taste-Engine/Pairs/Minecraft/GUI/Forge-1.20.1-jem-smp-screens/{TASTE,TECHNICAL}.md.

Source: storefront-r1.ora, an OpenRaster document with the original generated raster as one named editable plate layer. It does not claim separately hand-painted ornaments or reconstructed pixel-art layers. Its mergedimage.png and data/plate.png are byte-identical to the consumed storefront.png. Export is extraction of data/plate.png; native image dimensions1671×941, logical renderer canvas836×471. Generated using the built-in imagegen tool from the supplied reference; no new external assets.

Composition (logical coordinates): merchant alcove(65,69)–(220,255); title(271,43); balances start(505,47), stride53; search(270,101),158×16; dropdown(444,95),335×28; cards(245+137c,134+86r),4×3; buy(4,53),59×21 and sell(67,53),59×21 relative to card; pagination(424,403)/(544,403); parchment text(58,270). The entire composition scales uniformly and hit coordinates transform back into this canvas. The merchant scissor uses actual screen coordinates and balanced GuiGraphics scissor push/pop.

Live elements: librarian profession/level, localized labels/title, search, category names, exact server goods and names, wallet denominations, buy quotes, sell-price Coin tooltip, availability, page controls, errors and exchange. Goods/counts/quotes never come from baked art. Twelve server rows match twelve physical cards; empty/disabled slots dim their baked button surfaces. Plants is a query over admitted stock classified NATURE by the existing shared JEMCompat classifier, not a new policy/category enum or purchase-admission rule. Old category enum ordinals and packet framing remain unchanged.

Evidence: DESIGNED; generated source plate and runtime binding prepared. WAITING_FOR_REAL_GAME_CAPTURE: no gameplay client launch/capture is authorized by this delivery. Production build/server publication are technical evidence, not user visual acceptance or a claim of pixel-exact runtime identity. Final hashes/publication are recorded in delivery.json.
