# Сборщик — reference extraction и отзывы

20.10.2026. Исходный `ai-weighing-block-reference.png` — пользовательский reference формы весовой станции: открытый ящик, тёмная балка, две чаши, тёплая древесина и латунные соединения. AI pixels никогда не использовались как production texture/cutout. Почтовая тема ранее отвергнута. Пользователь отдельно уточнил native diamonds/fish/oak logs вместо нарисованных товаров/досок, по3–5 выступающих экземпляров.

## Repaint R2

Перед переделкой снова открыты original station reference и фактический R1 render. Также открыты exact native reference PNG из собственных установленных JAR:

- Minecraft1.20.1: `assets/minecraft/textures/block/barrel_side.png`, `anvil.png`. Извлечены крупный рисунок дерева, спокойные железные массы и ограниченное число значимых пикселей на тонкой кромке.
- Create1.20.1-6.0.8: `assets/create/textures/block/andesite_casing.png`, `depot_side.png`, `depot_top.png`. Извлечены читаемое дерево/железо, отдельная роль небольших креплений и тихая центральная плоскость. [Официальный upstream](https://github.com/Creators-of-Create/Create); это reference, не источник скопированных production пикселей.

Оригинальные native bitmap bytes остаются вне Git в ignored `Server/local/collector-repaint-20261020`. Рисунок atlas полностью authored Aseprite на целевой64×64 canvas, не скопирован и не downsampled из этих references.

**R1 USER_REJECTED:** пользователь назвал дерево/железо плохими и сжатыми; целый16×16 рисунок был сжат на узкие рейки/обручи. Он явно потребовал вместо множества поперечных пикселей1–2 там, где этого достаточно.

**R2 USER_APPROVED_ART:** «вооооо - approved» после реальных native Blockbench views. Принимается конкретный collector repaint R2: face-sized UV, крупные тёплые wood clusters, тёмное тихое железо, сохранённые geometry/native goods. Это не универсальный запрет на сложную текстуру в других стилях и не runtime/gameplay approval. Точная версия: `../model/art-revision.json`.
