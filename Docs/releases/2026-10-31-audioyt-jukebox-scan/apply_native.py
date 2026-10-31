from pathlib import Path
import hashlib
import json
import os
import subprocess


root = Path(__file__).resolve().parents[3]
server = root / "Server"
profile = Path("C:/Users/user/AppData/Roaming/.minecraft/versions/test jem")
published_source = server / "automodpack/host-modpack/main"
expected_jar_sha256 = "59362e3ecea6929a8321ebefb2014dffcf3575b747e992df4d1ce86c4236e133"

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

libraries = json.loads((profile / "test jem.json").read_text())["libraries"]
dependencies = []
for library in libraries:
    group, name, version = library["name"].split(":")[:3]
    path = profile.parent.parent / "libraries" / group.replace(".", "/") / name / version / f"{name}-{version}.jar"
    if path.is_file():
        dependencies.append(path)

bootstrap = profile / "mods/jem-bootstrap.jar"
source = r'''
import java.nio.file.*;
import java.util.*;
import java.lang.reflect.*;
import pl.skidam.automodpack_core.Constants;
import pl.skidam.automodpack_core.client.*;
import pl.skidam.automodpack_core.config.*;
import pl.skidam.automodpack_core.update.*;
import pl.skidam.automodpack_core.modpack.group.*;
class ApprovedAudioYtInstall {
 static Object invoke(Object object,String name,Class<?>[] types,Object... values)throws Exception {Method method=object.getClass().getDeclaredMethod(name,types);method.setAccessible(true);return method.invoke(object,values);}
 static String sha(Path path)throws Exception{return java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-1").digest(Files.readAllBytes(path)));}
 static void run()throws Exception{
  Constants.LOADER="forge";Constants.MC_VERSION="1.20.1";Constants.LOADER_VERSION="47.4.10";Constants.preload=false;
  Constants.MODPACK_LOADER=new pl.skidam.automodpack_loader_core_forge.mods.ModpackLoader();
  var storage=ClientStorage.open(Path.of(System.getProperty("user.dir")));
  Constants.clientConfig=ReconfConfigs.read(storage.clientConfigFile(),ClientConfigJsons.ClientConfigFieldsV3.class).orElseThrow();
  var active=storage.readActiveState();
  Path serverRoot=Path.of("C:/MyProjects/Minecraft/JustEnoughMods/Server/automodpack/server");
  Path fetchedHead=Files.createTempFile("audioyt-head-", ".json"), fetchedJournal=Files.createTempFile("audioyt-journal-", ".jsonl");
  Files.copy(serverRoot.resolve("current-projection.json"),fetchedHead,StandardCopyOption.REPLACE_EXISTING);
  Files.copy(serverRoot.resolve("journal.jsonl"),fetchedJournal,StandardCopyOption.REPLACE_EXISTING);
  new HeadMirror(storage).replaceFrom(fetchedHead);
  var mirror=new JournalMirror(storage);mirror.replaceFrom(active.modpackId,fetchedJournal);
  var entries=mirror.entries(active.modpackId);var latest=entries.get(entries.size()-1);
  var hashes=new HashSet<String>();
  for(var entry:entries){hashes.add(entry.policySha1());for(var change:entry.changes())if(change.toSha1()!=null&&!change.toSha1().isBlank())hashes.add(change.toSha1());}
  for(String hash:hashes){Path object=serverRoot.resolve("data/objects").resolve(hash.substring(0,2)).resolve(hash.substring(2));if(Files.isRegularFile(object))ClientObjectStore.storeObject(storage,hash,Files.readAllBytes(object));}
  var document=new ClientGenerationStore(storage).document(active.modpackId,latest);
  var target=SelectedModpackTarget.prepare(document,new ClientSelectionStore(storage.selectionFile()),ClientPlatform.WINDOWS);
  Path staged=Path.of("C:/MyProjects/Minecraft/JustEnoughMods/Server/automodpack/host-modpack/main");
  try(var updater=new ModpackUpdater(target,null,null,storage)){
   Object session=invoke(updater,"beginUpdateAttempt",new Class<?>[0]);
   invoke(session,"prepare",new Class<?>[]{boolean.class,boolean.class},false,true);
   Object prepared=invoke(session,"prepared",new Class<?>[0]);
   UpdatePlan plan=(UpdatePlan)invoke(prepared,"plan",new Class<?>[0]);
   var deleted=new HashSet<String>();var installed=new HashSet<String>();
   for(var operation:plan.operations()){
    String path=operation.relativePath();
    System.out.println("Native operation: "+operation.operation()+" "+operation.root()+" "+path);
    Path reviewed=staged.resolve(path).normalize();
    if(!reviewed.startsWith(staged))throw new IllegalStateException("Native path escapes reviewed publication: "+path);
    if(operation.operation()==UpdatePlan.OperationType.DELETE){deleted.add(path);if(Files.exists(reviewed))throw new IllegalStateException("Delete contradicts reviewed publication: "+path);}
    else {installed.add(path);if(!Files.isRegularFile(reviewed)||!sha(reviewed).equals(operation.expectedObjectHash()))throw new IllegalStateException("Target differs from reviewed publication source: "+path);}
   }
   if(!plan.conflicts().isEmpty())throw new IllegalStateException("Native plan contains conflicts");
   if(!installed.contains("mods/jemcompat-3.10.1.jar"))throw new IllegalStateException("Native plan does not install JEMCompat 3.10.1");
   if(!deleted.contains("mods/jemcompat-3.10.0.jar"))throw new IllegalStateException("Native plan does not retire JEMCompat 3.10.0");
   System.out.println("Approved publication: "+plan.packTarget().contentToken());
   ((UpdateAttempt)session).approve();((UpdateAttempt)session).commit();
   if(!new ClientGenerationStore(storage).headMatchesActive(active.modpackId,plan.packTarget().contentToken()))throw new IllegalStateException("Native installed head does not match");
   ClientStorageMutation.run(storage, () -> { storage.setDetached(active.modpackId,false); return null; });
   if(storage.isDetached(active.modpackId))throw new IllegalStateException("Native installation remains detached");
  }
 }
}
try { ApprovedAudioYtInstall.run(); } catch(Throwable failure){failure.printStackTrace();}
/exit
'''

jshell_script = server / "local/audioyt-native.jsh"
jshell_log = server / "local/audioyt-native.log"
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
print(log_text[-5000:])
if "Exception" in log_text or "Error" in log_text:
    raise RuntimeError("Native installation log contains a failure")

active_after = json.loads((profile / "automodpack/client/active-state.json").read_text())
published = json.loads((server / "automodpack/server/current-projection.json").read_text())
if active_after["contentToken"] != published["contentToken"]:
    raise RuntimeError("Native active state differs from the reviewed published head")
if (profile / "automodpack/client/update-transaction.json").exists():
    raise RuntimeError("Native transaction remains pending")

compat_jars = list((profile / "automodpack/client/active/mods").glob("jemcompat-*.jar"))
if [path.name for path in compat_jars] != ["jemcompat-3.10.1.jar"]:
    raise RuntimeError("Unexpected active JEMCompat files: " + ", ".join(path.name for path in compat_jars))
if hashlib.sha256(compat_jars[0].read_bytes()).hexdigest() != expected_jar_sha256:
    raise RuntimeError("Installed JEMCompat 3.10.1 differs from the approved artifact")

installed_manifest = profile / "automodpack/client/active/jem-pack/manifest.json"
published_manifest = published_source / "jem-pack/manifest.json"
if hashlib.sha256(installed_manifest.read_bytes()).hexdigest() != hashlib.sha256(published_manifest.read_bytes()).hexdigest():
    raise RuntimeError("Installed JEM manifest differs from the reviewed publication")

receipt = {
    "contentToken": active_after["contentToken"],
    "jemCompatSha256": expected_jar_sha256,
    "installed": "mods/jemcompat-3.10.1.jar",
    "removed": "mods/jemcompat-3.10.0.jar",
    "pendingTransaction": False,
}
(Path(__file__).with_name("native-install-receipt.json")).write_text(
    json.dumps(receipt, indent=2) + "\n", encoding="utf-8"
)

print("Exact native AudioYT fix installation complete with matching token, files and no pending transaction")
