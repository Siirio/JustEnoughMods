# Isolated test handoff — 20 October 2026

User authorizes the implementation plans, production artifacts, ordinary native AutoModpack application to stopped `test jem`, and one prepared MineRENT test deployment/restart. Original cutover requires a separate command. Keep the original world, its auth/player data, stable profile and immutable release baseline unchanged.

## Current evidence

Implementation and editor previews are prepared. Final artifact synchronization, ordinary publication/application and hosted deployment are **pending** until their exact receipts are recorded here. A build or preview does not establish player acceptance. No automated tests or Minecraft client launch were performed.

[Profile binding](profile-binding.json) and its two matching property templates bind only `jem-test-2026-10-20/world`, identity `aedf27f7-5f5f-4635-90f4-64c9428659b9`, epoch `handoff-2026-10-20-v1`. Native authentication remains mandatory. Source owners apply first choice, finite once-only wallet grant, initial creative and temporary OP2 after authentication; `force-gamemode=false` preserves later mode choices. Restore original admins through the private native-OP journal. Credentials, journal contents, worlds and personal state do not enter this release record.

## Matching production artifacts

| Owner | Full distributable | Protocol consequence |
| --- | --- | --- |
| Identity | `jem-identity-1.0.0.jar` | Shared profile/time APIs |
| CurrencyCore | Embedded in Economy and Settlements | Currency channel5; exact rational price/container/order contracts |
| Economy | `jem-economy-1.3.0.jar` | Shipping registry, rendering and native container synchronization |
| Settlements | `jem-settlements-2.1.0.jar` | Settlements channel11; native page materials and linked storage |
| Roles | `jem-roles-2.0.0.jar` | Roles channel4; readiness/test/selection state |
| Races | `jem-races-1.1.0.jar` | Skills channel7; selection nonce and queued presentation |
| Compat | `jemcompat-3.7.0.jar` | Native Backpacked augment codecs, furnace slots/process and Auto Feed |
| PackCore | `jem-pack-core-2.0.0-dev.jar` | Exact Nature's Compass guide identity |
| Server | `jem-server-2.0.0-dev.jar` | Dedicated test lifecycle |
| Bootstrap | `jem-bootstrap.jar`, revision jem.8 | Standalone native recovery revision; wire remains5.0.0-rc.1 |

CurrencyCore is an embedded dependency, not a new standalone installed JAR. Full embedded copies must match the fresh Core output. Rebuilt full distributables retain MenuSupport1.1.1. Slim/sandbox artifacts are excluded.

## Backpacked preview before host update

The user requested a preview before updating the test server. [Native source-layout composition](../../backpack/ui-preview-20261020/README.md) was displayed before any hosted update/restart. It shows Workbench, Furnace and native augment toggles. The native3D furnace icon and runtime interaction remain unobserved; the image is explicitly not a gameplay screenshot.

## Rollback scope

The private host rollback bundles contain the nine replaced JARs and root `config`, `defaultconfigs`, `kubejs`. They contain no world snapshot. Existing original-world backup history is separate; a new current original backup belongs only to the future separately authorized production cutover.

Preserve the host's native AutoModpack identity, credentials and journal. Publish a fresh host generation from canonical `Client/ManagedPack` bytes; never upload local history or local test state. Record ordinary local publication, installed client token/no pending transaction and hosted publication/startup separately.
# Historical preparation — superseded target

The user subsequently authorized the [original-world release](../2026-10-21-original-handoff/README.md), skipped the remaining hosted test phase and requested deletion of the disposable test save. The templates below were not installed for this final release and must not be applied to original.

