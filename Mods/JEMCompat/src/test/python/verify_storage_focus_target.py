import argparse
import json
import re
import subprocess
from pathlib import Path


def bytecode(jar, class_name):
    return subprocess.check_output(["javap", "-p", "-c", "-classpath", str(jar), class_name], text=True, encoding="utf-8")


def method(code, name):
    matches = [block for block in re.split(r"\n(?=  (?:public|private|protected) )", code)
               if name + "(" in block.split("Code:")[0]]
    assert len(matches) == 1, (name, len(matches))
    return matches[0]


def installed(directory, pattern):
    jars = list(directory.glob(pattern))
    assert len(jars) == 1, (pattern, jars)
    return jars[0]


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--mods-dir", type=Path, required=True)
    args = parser.parse_args()
    root = Path(__file__).resolve().parents[3]
    source = (root / "src/main/java/com/siirio/jemcompat/mixin/client/StorageTerminalFocusMixin.java").read_text(encoding="utf-8")
    target = re.search(r'@Mixin\(targets = "([^"]+)"', source).group(1)
    invocation = re.search(r'target = "L([^;]+);([^\"]+)"', source)
    owner, descriptor = invocation.groups()
    terminal = bytecode(installed(args.mods_dir, "toms_storage-*.jar"), target)
    packet = method(terminal, "onPacket")
    method_name, signature = descriptor.split("(", 1)
    expected = "Method " + owner + "." + method_name + ":(" + signature
    assert packet.count(expected) == 1, expected
    assert "Field searchType:I" in packet and "iand" in packet, "Upstream autofocus condition changed"
    assert "m_7933_:(III)Z" in method(terminal, "m_7933_"), "Native edit-box key handling changed"
    assert "m_5534_:(CI)Z" in method(terminal, "m_5534_"), "Native edit-box character handling changed"
    assert "PlatformContainerScreen.m_6375_:(DDI)Z" in method(terminal, "m_6375_"), "Native child focus routing changed"
    menu = bytecode(installed(args.mods_dir, "toms_storage-*.jar"), "com.tom.storagemod.gui.StorageTerminalMenu")
    assert "java/lang/Runnable.run:()V" in method(menu, "receiveClientNBTPacket"), "Packet callback changed"
    overlay = bytecode(installed(args.mods_dir, "jei-*.jar"), "mezz.jei.api.runtime.IIngredientListOverlay")
    assert "boolean hasKeyboardFocus();" in overlay, "JEI focus API changed"
    config = json.loads((root / "src/main/resources/jemcompat.mixins.json").read_text(encoding="utf-8"))
    mixin = "client.StorageTerminalFocusMixin"
    assert mixin in config["client"] and mixin not in config["mixins"], "Focus mixin must be client-only"
    print("PASS: one exact installed onPacket focus invocation; native mouse/key/character routes; packet callback; JEI focus API; client-only registration.")


if __name__ == "__main__":
    main()
