# First End portal glitch

`Timeline Glitch 1.mov` is the user-supplied production source. It is 2048 by 1080 pixels, 24 frames per second, and 3.14 seconds long. Its audio track is silent

The 75 PNG files in `src/main/resources/assets/jem_twelve_eyes/textures/secret_ending` are direct native-resolution frame exports. `SecretEndingClip` decodes at most four frames ahead and reuses one GPU texture. The screen keeps the source aspect ratio and adds black bars when required

To remake the frames from the source movie, run FFmpeg from the project root with `-i "Mods/JEMTwelveEyes/production/secret-ending/Timeline Glitch 1.mov" -vf fps=24 -start_number 0 "Mods/JEMTwelveEyes/src/main/resources/assets/jem_twelve_eyes/textures/secret_ending/frame_%03d.png"`. Do not resize, recolor, or add generated frames

The existing first-portal trigger and 12-second vanilla phase remain. The clip replaces only the following glitch phase. The server still owns completion and reward delivery
