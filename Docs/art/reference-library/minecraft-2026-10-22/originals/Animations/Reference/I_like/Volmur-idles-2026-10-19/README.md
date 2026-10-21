# Volmur — idle reference additions, 2026-10-19

Classification: **USER_LIKED**. The user added these three links and three Downloads files as idle-animation references, continuing the existing selection of whole-body expression, rhythm and secondary motion. Observations and technical adaptations below are agent analysis, not additional user instructions. No preference for the exact palette, character design or every depicted action is inferred.

## Source matching and originals

| Reference | Source | Archived file / state |
|---|---|---|
| IDLE01 Knight and Idle animation | [v0lmur / 2076377903516811621](https://x.com/v0lmur/status/2076377903516811621/video/1) | **Local video missing.** Browser main video duration ~4.523 s; poster/playback sample inspected. This post also quotes the elf video. |
| IDLE02 Mage with a book | [v0lmur / 2070519270698573974](https://x.com/v0lmur/status/2070519270698573974/video/1) | [Original MP4](<Originals/ssstwitter.com_1792391245386.mp4>), 9.429 s |
| IDLE03 Elf girl with a bow and bow stress tests | [v0lmur / 2070613354914185228](https://x.com/v0lmur/status/2070613354914185228/video/1) | [Original MP4](<Originals/ssstwitter.com_1792391231573.mp4>), 22.613 s; [identical second download](<Originals/ssstwitter.com_1792391257453.mp4>) |

The two elf files have identical SHA-256 `076c5a1e9dc28acf084db461062b8a7b90f7ac9f9cff888acc557b7c538aee31`. There are three supplied files but two unique recordings. The knight post's quoted video visually matches the duplicated elf file; do not assert the downloader's internal selection algorithm. Browser media download of the knight's main video failed, so it remains missing locally.

All three downloaded files are preserved byte-for-byte under their original names; Downloads remains untouched. Metadata and hashes: [manifest](<Review/manifest.json>). Both unique recordings are 640×360 at encoded 60 fps. This establishes neither source keyframe snapping nor an editable animation format. Original URLs also remain in adjacent `.url` shortcuts. Attribution is retained; asset-reuse permission is not implied.

## IDLE01 — knight: posture observation only

The browser shows an armoured humanoid with one arm raised onto the hilt of a tall, downward-pointing sword, while the other arm hangs low. Both feet are near the ground and the asymmetrical arm/weapon relationship carries the resting pose. A playing screenshot near 2.23 s supports this pose observation only. No local temporal board, measured weapon contact, breathing period, joint delay or seamless-loop claim is available.

Adaptation proposal: construct a resting pose around meaningful body/prop support rather than random asymmetry. A hand apparently resting on a sword needs deliberate grip and ground contact in production. This reference does not prove a particular rig hierarchy or actual force-bearing contact. Obtain the main knight recording before detailed motion calibration.

## IDLE02 — mage: quiet body, active prop

Evidence: [24 samples across the recording](<Review/MAGE_full.jpg>), [24 samples at 0.1-second intervals from 0–2.3 s](<Review/MAGE_pages_detail.jpg>).

Observed: the feet, long robe silhouette and supporting arm stay comparatively quiet. The open book stays in the supporting hand while page shapes rise and pass across it. In the detailed interval a page becomes conspicuously upright around 0.4–0.5 s, opens flatter around 0.6–1.0 s, rises again near 1.1–1.2 s, settles around 1.3–2.0 s, and another upright-page pose appears near 2.1–2.2 s. These are sampled shape changes, not recovered page-key timings. Small head/book position changes occur, but exact breathing amplitude, page pivot and hand lag are not measured.

Adaptation: an idle can communicate occupation through the held object while the main body remains restrained. Establish readable reading posture and hand/book contact first; animate the page as the focal action, then subtle body response if warranted. Do not move every limb just to advertise that the model is alive. Page geometry, interpolation and visibility tricks remain unknown. Do not assume a hand-driven page turn: the visible free arm remains down in these samples.

## IDLE03 — elf: baseline rest plus a bow-handling incident

Evidence: [32 full-recording samples](<Review/ELF_full.jpg>), [24 samples across 1.5–6.0 s](<Review/ELF_bow_detail.jpg>).

Observed: starts in a relaxed low-bow stance. Around 2.1–2.7 s the bow rotates upward and the other arm approaches. Around 2.9–3.7 s both arms are raised and head/torso incline toward the bow. Around 4.0–4.8 s one arm lowers while the bow remains raised in the other; around 5.2–5.6 s the bow returns low. By about 6.5 s the character is back in a quieter pose and remains comparatively quiet through roughly 12 s. A related handling sequence recurs around 14.5–17.5 s, followed by quiet stance through the end. This resembles a repeat with a long quiet region; the footage does not establish a numerical loop length or velocity-continuous boundary. No projectile or exact release frame is established by these samples.

Adaptation: separate the low-amplitude base idle from an occasional larger, character-specific prop action. Let the head/torso support the bow's action while the feet retain a stable stance. Sequence hand approach, shared interaction, free-hand withdrawal, and the held prop's return. Do not turn a showcase bow stress test into mandatory constant combat or a universal idle cadence. The bow's exact rig, draw mechanics and recoil remain unverified.

## Reusable idle lessons

These are scoped interpretations of the user's liked references:

1. A readable occupation or support relationship can define the idle: reading, handling a bow, resting beside a sword.
2. Choose a quiet supporting region and a focal moving region. Subtle idle does not require all bones to move together.
3. Separate sustained base motion from occasional larger incidents. Preserve quiet time after the incident instead of repeating the accent continuously.
4. Establish grip/support first, then primary motion, then justified secondary response. Never invent numerical lag from low-resolution footage.
5. A recording may contain multiple repetitions, author demonstrations or stress tests. Derive production clips and loop boundaries explicitly; do not copy video duration as animation duration.

Status: **THREE_LINKS_REGISTERED / THREE_FILES_PRESERVED / TWO_UNIQUE_LOCAL_VIDEOS_ANALYSED / KNIGHT_LOCAL_VIDEO_MISSING**. Analysis uses sampled decoded sequences, not claimed continuous playback or exhaustive per-frame review. No editable rig/curves or runtime compatibility was supplied. Four boards contain 104 sampled frames. No JEM code, gameplay assets, pack state or release baseline changed.
