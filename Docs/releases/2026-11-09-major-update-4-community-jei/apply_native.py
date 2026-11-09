from pathlib import Path
import hashlib
import json
import os
import subprocess


root = Path(__file__).resolve().parents[3]
server = root / "Server"
profile = Path("C:/Users/user/AppData/Roaming/.minecraft/versions/test jem")
canonical = root / "Client/ManagedPack"
full_expected_operations = {
    "config/ambientsounds-client.json": "PRESENT",
    "config/blockrunner.json": "PRESENT",
    "config/fastbench.cfg": "DELETE",
    "config/immersive_storms.json5": "PRESENT",
    "config/paxi/resourcepack_load_order.json": "PRESENT",
    "config/paxi/resourcepacks/Bibliophilia 1.17 (1.20.1).zip": "PRESENT",
    "config/paxi/resourcepacks/shureee's Potions CIT.zip": "PRESENT",
    "config/weatherglass.properties": "PRESENT",
    "config/windy-client.toml": "PRESENT",
    "jem-pack/manifest.json": "PRESENT",
    "mods/AmbientSounds_FORGE_v6.3.11_mc1.20.1.jar": "PRESENT",
    "mods/BlockRunner-v8.0.4-1.20.1-Forge.jar": "PRESENT",
    "mods/FastWorkbench-1.20.1-8.0.4.jar": "DELETE",
    "mods/Structory_1.20.x_v1.3.5.jar": "PRESENT",
    "mods/TreeChop-1.20.1-forge-0.19.0-fixed.jar.url": "DELETE",
    "mods/VisualWorkbench-v8.0.1-1.20.1-Forge.jar": "PRESENT",
    "mods/citresewn-1.20.1-5.jar": "PRESENT",
    "mods/do_a_barrel_roll-forge-3.5.6+1.20.1.jar": "PRESENT",
    "mods/immersive_storms-1.0.0-forge.jar": "PRESENT",
    "mods/jem-advancements-5.3.2.jar": "PRESENT",
    "mods/jemcompat-3.10.3.jar": "PRESENT",
    "mods/ocean_lily_pad_village-1.0.1-forge-1.20.1.jar": "PRESENT",
    "mods/rosegoldequipment-1.7.6.jar": "PRESENT",
    "mods/spear-backport-forge-1.4.7-1.20.1.jar": "PRESENT",
    "mods/weatherglass-0.5.0-1.20.1-forge.jar": "PRESENT",
    "mods/windy-forge-1.20.1.jar": "PRESENT",
}
incremental_expected_operations = {
    "jem-pack/manifest.json": "PRESENT",
    "mods/jem-advancements-5.3.2.jar": "PRESENT",
}
previous_content_token = "db6012daab947a47c1d1b53123c4e1f953fbd952"
full_source_token = "715a0513839d76b04c99fd52ac316e223c18390d"


def sha256(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


processes = subprocess.run(
    [
        "pwsh",
        "-NoProfile",
        "-Command",
        "Get-CimInstance Win32_Process -Filter \"Name='java.exe' OR Name='javaw.exe'\" | "
        "Where-Object { $_.CommandLine -and $_.CommandLine.Contains('versions\\test jem') } | "
        "Select-Object -ExpandProperty ProcessId",
    ],
    capture_output=True,
    text=True,
    check=True,
)
if processes.stdout.strip():
    raise SystemExit("Close test jem before native installation: " + processes.stdout.strip())

active_before = json.loads((profile / "automodpack/client/active-state.json").read_text())
if active_before["contentToken"] == previous_content_token:
    expected_operations = incremental_expected_operations
elif active_before["contentToken"] == full_source_token:
    expected_operations = full_expected_operations
else:
    raise SystemExit("Native installation source token is not a reviewed Major Update 4 predecessor")

libraries = json.loads((profile / "test jem.json").read_text())["libraries"]
dependencies = []
for library in libraries:
    group, name, version = library["name"].split(":")[:3]
    path = profile.parent.parent / "libraries" / group.replace(".", "/") / name / version / f"{name}-{version}.jar"
    if path.is_file():
        dependencies.append(path)

bootstrap = profile / "mods/jem-bootstrap.jar"
java_expected = ",\n   ".join(
    f'Map.entry("{relative.replace(chr(92), chr(92) * 2).replace(chr(34), chr(92) + chr(34))}","{state}")'
    for relative, state in expected_operations.items()
)
source = f'''
import java.nio.file.*;
import java.util.*;
import java.lang.reflect.*;
import pl.skidam.automodpack_core.Constants;
import pl.skidam.automodpack_core.client.*;
import pl.skidam.automodpack_core.config.*;
import pl.skidam.automodpack_core.update.*;
import pl.skidam.automodpack_core.modpack.group.*;
class ApprovedMajorUpdateInstall {{
 static Object invoke(Object object,String name,Class<?>[] types,Object... values)throws Exception {{Method method=object.getClass().getDeclaredMethod(name,types);method.setAccessible(true);return method.invoke(object,values);}}
 static String sha(Path path)throws Exception{{return java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-1").digest(Files.readAllBytes(path)));}}
 static void run()throws Exception{{
  Constants.LOADER="forge";Constants.MC_VERSION="1.20.1";Constants.LOADER_VERSION="47.4.10";Constants.preload=false;
  Constants.MODPACK_LOADER=new pl.skidam.automodpack_loader_core_forge.mods.ModpackLoader();
  var storage=ClientStorage.open(Path.of(System.getProperty("user.dir")));
  Constants.clientConfig=ReconfConfigs.read(storage.clientConfigFile(),ClientConfigJsons.ClientConfigFieldsV3.class).orElseThrow();
  var active=storage.readActiveState();
  Path serverRoot=Path.of("C:/MyProjects/Minecraft/JustEnoughMods/Server/automodpack/server");
  Path fetchedHead=Files.createTempFile("major-update-head-", ".json");
  Path fetchedJournal=Files.createTempFile("major-update-journal-", ".jsonl");
  Files.copy(serverRoot.resolve("current-projection.json"),fetchedHead,StandardCopyOption.REPLACE_EXISTING);
  Files.copy(serverRoot.resolve("journal.jsonl"),fetchedJournal,StandardCopyOption.REPLACE_EXISTING);
  new HeadMirror(storage).replaceFrom(fetchedHead);
  var mirror=new JournalMirror(storage);mirror.replaceFrom(active.modpackId,fetchedJournal);
  var entries=mirror.entries(active.modpackId);var latest=entries.get(entries.size()-1);
  var hashes=new HashSet<String>();
  for(var entry:entries){{hashes.add(entry.policySha1());for(var change:entry.changes())if(change.toSha1()!=null&&!change.toSha1().isBlank())hashes.add(change.toSha1());}}
  for(String hash:hashes){{Path object=serverRoot.resolve("data/objects").resolve(hash.substring(0,2)).resolve(hash.substring(2));if(Files.isRegularFile(object))ClientObjectStore.storeObject(storage,hash,Files.readAllBytes(object));}}
  var document=new ClientGenerationStore(storage).document(active.modpackId,latest);
  var target=SelectedModpackTarget.prepare(document,new ClientSelectionStore(storage.selectionFile()),ClientPlatform.WINDOWS);
  Path canonical=Path.of("C:/MyProjects/Minecraft/JustEnoughMods/Client/ManagedPack");
  Map<String,String> expected=Map.ofEntries(
   {java_expected});
  try(var updater=new ModpackUpdater(target,null,null,storage)){{
   Object session=invoke(updater,"beginUpdateAttempt",new Class<?>[0]);
   invoke(session,"prepare",new Class<?>[]{{boolean.class,boolean.class}},false,true);
   Object prepared=invoke(session,"prepared",new Class<?>[0]);
   UpdatePlan plan=(UpdatePlan)invoke(prepared,"plan",new Class<?>[0]);
   var reviewed=new HashSet<String>();
   for(var operation:plan.operations()){{
    String relative=operation.relativePath();
    System.out.println("Native operation: "+operation.operation()+" "+operation.root()+" "+relative);
    if(!expected.containsKey(relative))throw new IllegalStateException("Unreviewed native mutation: "+relative);
    boolean deletion=operation.operation()==UpdatePlan.OperationType.DELETE;
    if(deletion != expected.get(relative).equals("DELETE"))throw new IllegalStateException("Unexpected operation for "+relative);
    if(!deletion && !sha(canonical.resolve(relative)).equals(operation.expectedObjectHash()))throw new IllegalStateException("Target differs from canonical source: "+relative);
    if(deletion && Files.exists(canonical.resolve(relative)))throw new IllegalStateException("Deleted path still exists in canonical source: "+relative);
    reviewed.add(relative);
   }}
   if(!plan.conflicts().isEmpty())throw new IllegalStateException("Native plan contains conflicts");
   if(!reviewed.equals(expected.keySet()))throw new IllegalStateException("Native plan differs from reviewed transition: "+reviewed);
   System.out.println("Approved publication: "+plan.packTarget().contentToken());
   ((UpdateAttempt)session).approve();((UpdateAttempt)session).commit();
   if(!new ClientGenerationStore(storage).headMatchesActive(active.modpackId,plan.packTarget().contentToken()))throw new IllegalStateException("Native installed head does not match");
   ClientStorageMutation.run(storage, () -> {{ storage.setDetached(active.modpackId,false); return null; }});
   if(storage.isDetached(active.modpackId))throw new IllegalStateException("Native installation remains detached");
  }}
 }}
}}
try {{ ApprovedMajorUpdateInstall.run(); }} catch(Throwable failure){{failure.printStackTrace();}}
/exit
'''

jshell_script = server / "local/major-update-4-community-jei-native.jsh"
jshell_log = server / "local/major-update-4-community-jei-native.log"
jshell_script.write_text(source, encoding="utf-8")
with jshell_log.open("w", encoding="utf-8") as log:
    result = subprocess.run(
        [
            "C:/Users/user/.jdks/ms-17.0.15/bin/jshell.exe",
            "--class-path",
            os.pathsep.join(str(path) for path in [bootstrap, *dependencies]),
            "--feedback",
            "concise",
            str(jshell_script),
        ],
        cwd=profile,
        stdout=log,
        stderr=subprocess.STDOUT,
    )
if result.returncode != 0:
    raise RuntimeError(f"Native installation command failed with exit code {result.returncode}")

log_text = jshell_log.read_text(encoding="utf-8", errors="replace")
print(log_text[-8000:])
terminal_markers = ("\njava.lang.IllegalStateException:", "\njava.lang.RuntimeException:", "\njava.lang.Error:")
if any(marker in log_text for marker in terminal_markers):
    raise RuntimeError("Native installation log contains a failure")

active_after = json.loads((profile / "automodpack/client/active-state.json").read_text())
published = json.loads((server / "automodpack/server/current-projection.json").read_text())
if active_after["contentToken"] != published["contentToken"]:
    raise RuntimeError("Native active token differs from published head")
if (profile / "automodpack/client/update-transaction.json").exists():
    raise RuntimeError("Native transaction remains pending")

active_root = profile / "automodpack/client/active"
installed_hashes = {}
for relative, state in expected_operations.items():
    installed = active_root / relative
    if state == "DELETE":
        if installed.exists():
            raise RuntimeError("Retired managed path remains active: " + relative)
    else:
        if sha256(installed) != sha256(canonical / relative):
            raise RuntimeError("Installed file differs from canonical source: " + relative)
        if relative.endswith(".jar"):
            installed_hashes[relative] = sha256(installed)

receipt = {
    "journalSequence": published["journalHead"],
    "contentToken": active_after["contentToken"],
    "installedJarSha256": installed_hashes,
    "deleted": sorted(relative for relative, state in expected_operations.items() if state == "DELETE"),
    "pendingTransaction": False,
    "runtimeVerified": False,
    "userApproved": False,
}
(Path(__file__).with_name("native-install-receipt.json")).write_text(
    json.dumps(receipt, indent=2) + "\n", encoding="utf-8"
)

print("Exact native Major Update 4 installation complete with matching token and no pending transaction")
