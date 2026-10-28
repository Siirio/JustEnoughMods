# Unofficial Luxium Compatibility

This is unofficial compatability mod - credits to the author of Luxium for an insanely optimized shaders

This project is not affiliated with or endorsed by Luxium or Vinlanx. Install the official unmodified [Luxium project](https://www.curseforge.com/minecraft/mc-mods/luxium) separately. This project never bundles, mirrors, patches, modifies, or redistributes Luxium code, shaders, assets, branding, or JAR content

`jem_luxium` is the stable technical mod ID for this client-only Forge 1.20.1 integration. It supports Luxium 2.8.0-pre-alpha, Embeddium 0.3.31, and Oculus 1.8.0 exactly until another version receives separate compatibility review

It adds a native `Luxium` page to Embeddium's video settings. The page contains one always-available Embeddium checkbox:

- on enables the exact installed Luxium 2.8.0 renderer and suspends Oculus shaders
- off disables Luxium and restores the Oculus shader enabled state that existed before Luxium was selected
- enabling Oculus shaders while Luxium is active disables Luxium and discards the temporary restore marker
- every renderer-owner change reloads Embeddium through its managed OpenGL context so Luxium's celestial shadow pipeline is rebuilt safely
- Luxium's installed-Oculus guard is bypassed only for its celestial shadow lifecycle because this adapter guarantees that the two renderers are never active together
- the Luxium mod is a required client dependency, so the checkbox never becomes a disabled placeholder

The integration does not patch Oculus, modify Luxium, replace Embeddium's screen, or own either renderer. Changes follow Embeddium's native Apply flow. Its remembered fallback is a personal client preference stored in the generated client config.

## Build

Run `./gradlew clean build` with Java 17. Public Maven coordinates supply compile-only API symbols and the output contains no nested third-party JARs
