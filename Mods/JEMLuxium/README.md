# JEM Luxium

`jem_luxium` is a client-only Forge 1.20.1 integration for the exact installed Embeddium 0.3.31 and Oculus 1.8.0 stack.

It adds a native `Luxium` page to Embeddium's video settings. The page contains one always-available Embeddium checkbox:

- on enables the exact installed Luxium 2.8.0 renderer and suspends Oculus shaders
- off disables Luxium and restores the Oculus shader enabled state that existed before Luxium was selected
- enabling Oculus shaders while Luxium is active disables Luxium and discards the temporary restore marker
- every renderer-owner change reloads Embeddium through its managed OpenGL context so Luxium's celestial shadow pipeline is rebuilt safely
- Luxium's installed-Oculus guard is bypassed only for its celestial shadow lifecycle because this adapter guarantees that the two renderers are never active together
- the Luxium mod is a required client dependency, so the checkbox never becomes a disabled placeholder

The integration does not patch Oculus, modify Luxium, replace Embeddium's screen, or own either renderer. Changes follow Embeddium's native Apply flow. Its remembered fallback is a personal client preference stored in the generated client config.
