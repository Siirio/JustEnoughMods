"""Package canonical Puffish data with explicit sandbox-only semantic icon substitutions."""
import json
from pathlib import Path
import sys
import re

MODULE = Path(__file__).resolve().parent
SOURCE = MODULE.parents[1] / 'Client/ManagedPack/config/paxi/datapacks/jem-profession-skills/data'
ICONS = MODULE / 'src/sandbox/resources/assets/jem_professions/sandbox-icons.json'
CONTRACT = MODULE / 'src/sandbox/resources/assets/jem_professions/sandbox-skills.json'


def adapt_icons(value, mapping):
    if isinstance(value, dict):
        if value.get('type') == 'item' and isinstance(value.get('data'), dict):
            original = value['data'].get('item')
            if original in mapping:
                value['data']['item'] = mapping[original]
        for child in value.values():
            adapt_icons(child, mapping)
    elif isinstance(value, list):
        for child in value:
            adapt_icons(child, mapping)


def main():
    output = Path(sys.argv[1]).resolve()
    contract = json.loads(CONTRACT.read_text(encoding='utf-8'))
    catalog = (MODULE / 'src/main/java/com/siirio/jemprofessions/tree/ProfessionCatalog.java').read_text(encoding='utf-8')
    levels = {node: int(level) for node, level in re.findall(r'n\("([^"]+)","[^"]+",\d,"[^"]*",(\d+),', catalog)}
    for source in SOURCE.rglob('*.json'):
        target = output / 'data' / source.relative_to(SOURCE)
        target.parent.mkdir(parents=True, exist_ok=True)
        value = json.loads(source.read_text(encoding='utf-8'))
        if source.name == 'definitions.json':
            for node_id, definition in value.items():
                skill = contract[node_id]
                definition['title'] = skill['name_ru']
                definition['description'] = skill['effect_ru']
                definition['extra_description'] = 'Опыт: ' + skill['xp_ru']
                # Native required_points means remaining points, not profession level.
                # Exact level/parent admission is the sandbox server's SkillAdmissionMixin.
                definition['required_points'] = 1
                definition['required_mods'] = skill['required_mods']
        target.write_text(json.dumps(value, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')


if __name__ == '__main__':
    main()
