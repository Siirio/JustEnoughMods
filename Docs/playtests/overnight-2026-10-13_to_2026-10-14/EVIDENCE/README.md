# Evidence ownership

The local folder contains the complete run evidence and population reference ledger.
The Git snapshot includes compact reports, exact cases, curated native traces and
a selected set of critical unedited screenshots. `.gitignore` retains the remaining
actual PNGs, generated concept PNGs and the 32,281-row population JSONL locally.
Their hashes and paths stay in the indexes; links to omitted files need this local
folder. The compact repository report is not a remote backup of every raw artifact.

Private original-world/config/auth snapshots, raw server console/command queues,
temporary QA credential and complete thread dumps live only in the ignored
`build/overnight-qa` workspace. No world save, credential or trust key is committed.

`screenshots/actual` contains real observed frames; `screenshots/concepts` is
generated and unapproved. Native F2 is distinguished from desktop window capture.
`traces` and `logs` separate bytecode/source, read-only native snapshots and actual
transaction observations. Generated boards never count as played/fixed evidence.

See `evidence-index.jsonl`, `traces/player-cases.jsonl` and root `TIMELINE.md`.
