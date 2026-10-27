# JEM Luxium

`jem_luxium` is a client-only Forge 1.20.1 integration for the exact installed Embeddium 0.3.31 and Oculus 1.8.0 stack.

It adds a native `Luxium` page to Embeddium's video settings. The page contains one standard Embeddium checkbox:

- on selects the installed Luxium shader pack through Oculus and enables shaders
- off restores the shader pack name and enabled state that Oculus had before Luxium was selected
- if no earlier state exists, off disables shaders through the normal Oculus API
- the option is unavailable when no valid shader pack whose filename starts with `Luxium ` exists

The integration does not patch Oculus, modify Luxium, replace Embeddium's screen, or own the shader renderer. Changes follow Embeddium's native Apply flow. Its remembered fallback is a personal client preference stored in the generated client config.
