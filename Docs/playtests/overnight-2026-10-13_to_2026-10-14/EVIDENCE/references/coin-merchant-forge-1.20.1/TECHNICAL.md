# Exact pair: JEM Coin merchant screen, Forge 1.20.1

Status: READY for a task-local concept and objective layout critique. Production
needs native GUI implementation and actual replay; a generated PNG is not a GUI.

Current owner: `CoinMerchantMenu` provides server-owned adapted native offers and
commits through slot clicks; `CoinMerchantScreen` owns presentation. Native source
uses176×186GUI units,18display slots, regular36inventory slots and `GuiGraphics`.
GUI4 with2560×1406framebuffer was actually inspected. Pixel layout must use
real `font.split` heights, bounded width, native tooltip placement and valid slot
hitboxes. Never reinterpret the tagged display icon as the original trade policy.

The proposal must project input→output, direction, quantity, authoritative quote,
affordability and rejection reason without creating client money/stock authority.
The exact stock repair delegates `uses` to native `notifyTrade` once; actor/refund
problems remain separate. No giant fixture, generated screenshot substitution or
raw copy into active AutoModpack gameDir is permitted.

Concept export: opaque PNG in `EVIDENCE/concepts`, original authored image,
explicit CONCEPT label and preserved prompt. Validate Russian text and observed
numbers; assess at ordinary viewing size. Do not deploy generated art as a final
native screen or pixel-perfect176×186texture without exact-format production.
