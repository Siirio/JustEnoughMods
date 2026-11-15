# JEM Security 1.0.7 production receipt

## Release boundary

- MineRENT server `0618360f` / `JustEnough`
- Runtime artifact `/mods/jem_security-1.0.7.jar`
- AutoModpack artifact `/automodpack/host-modpack/main/mods/jem_security-1.0.7.jar`
- Retired runtime artifact `/local/retired/jem_security-1.0.5-before-1.0.7.jar`
- Retired AutoModpack artifact `/local/retired/host-pack-jem_security-1.0.5.jar`

## Reviewed hashes

- JEM Security 1.0.7 SHA-256 `8614637B96854C7D2831CF28521A2513D5FF512E47237426B8576D7487C6E5D2`
- JEM manifest SHA-256 `6E22EC975E79B7997ADE38CE48551E3D3B557A20311645F9281E5068A5E7F987`
- MineRENT projection SHA-256 `7B13726E054137077F5F64AC61C92E35E7FF025D1760071860BBB0498995C130`
- MineRENT journal SHA-256 `4FADEF365E70E2D17B00BF13F7F40A8DEF0F0B445AF4DDD28099BADB9A2E6FDC`

## Runtime evidence

- MineRENT loaded `jem_security` version 1.0.7 and reached `Done (1.699s)` at 2026-10-31 11:49 Asia/Yekaterinburg
- Native AutoModpack preview reported only the manifest change, removal of JEM Security 1.0.5 and addition of JEM Security 1.0.7
- Native AutoModpack published content token `b3b16bde689df823f19c00ad9b18a4ad8f733aa7`
- The post-publish preview reported `Diff: +0 ~0 -0` with the two intentional exclusions for bootstrap and `options.txt`
- MineRENT remained running after publication

## Stable client state

- Stable `JustEnoughMods` contains the exact reviewed JEM Security 1.0.7 and manifest bytes
- The production head is applied only through native AutoModpack using `apply_native.py`
- Player acceptance remains `in testing` until the running stable client completes its native restart and MineRENT records `client_attested`
