# Changelog

## 1.0.11

- Reload native core entity shaders along with the GPU feature mask and terrain renderer when changing renderer ownership
- Log a bounded native shadow-state snapshot after entering a world or changing the master switch to diagnose remaining missing-shadow reports
- Runtime shadow acceptance remains pending

## 1.0.10

- Refresh Luxium's cached GPU feature mask immediately when the renderer owner changes, before rebuilding Embeddium
- Make an in-game Luxium enable apply celestial shadows, sky lighting and the complete Luxium render path without a Minecraft restart

## 1.0.9

- Persist Luxium's first-run warning acknowledgement before suppressing the warning screen, so new installations never retain the 200-tick gate

## 1.0.8 - in development

- Suppress Luxium 2.8.0's first-run alpha warning and ten-second Continue gate
- Preserve the native Luxium settings screen, Embeddium page, renderer toggle, Oculus arbitration and YouTube action

## 1.0.6 - in development

- Package the production Mixin refmap required to resolve Mojmap method targets in Forge runtime
- Preserve the direct Embeddium route and Luxium-tab controls introduced in 1.0.5

## 1.0.5 - in development

- Open Embeddium video settings directly instead of Luxium's intermediate hub
- Put the full Luxium settings action on the second Luxium row and keep the upstream YouTube artwork below it

## 1.0.4 - in development

- Prepare the existing compatibility switch for an independent public build

