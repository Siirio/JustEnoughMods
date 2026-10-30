from pathlib import Path
import hashlib
import json
import os
import subprocess


root = Path(__file__).resolve().parents[3]
server = root / "Server"
profile = Path("C:/Users/user/AppData/Roaming/.minecraft/versions/test jem")
canonical = root / "Client/ManagedPack"
new_jar = "mods/jemcompat-3.9.2.jar"
old_jar = "mods/jemcompat-3.9.1.jar"
installed_paths = {new_jar}
allowed_paths = installed_paths | {old_jar, "jem-pack/manifest.json"}

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
class ApprovedJemCompatInstall {
 static Object invoke(Object object,String name,Class<?>[] types,Object... values)throws Exception {Method method=object.getClass().getDeclaredMethod(name,types);method.setAccessible(true);return method.invoke(object,values);}
 static String sha(Path path)throws Exception{return java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-1").digest(Files.readAllBytes(path)));}
 static void run()throws Exception{
  Constants.LOADER="forge";Constants.MC_VERSION="1.20.1";Constants.LOADER_VERSION="47.4.10";Constants.preload=false;
  Constants.MODPACK_LOADER=new pl.skidam.automodpack_loader_core_forge.mods.ModpackLoader();
  var storage=ClientStorage.open(Path.of(System.getProperty("user.dir")));
  Constants.clientConfig=ReconfConfigs.read(storage.clientConfigFile(),ClientConfigJsons.ClientConfigFieldsV3.class).orElseThrow();
  var active=storage.readActiveState();
  Path serverRoot=Path.of("C:/MyProjects/Minecraft/JustEnoughMods/Server/automodpack/server");
  Path fetchedHead=Files.createTempFile("jemcompat-head-", ".json"), fetchedJournal=Files.createTempFile("jemcompat-journal-", ".jsonl");
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
  Path canonical=Path.of("C:/MyProjects/Minecraft/JustEnoughMods/Client/ManagedPack");
  Set<String> allowed=Set.of("jem-pack/manifest.json","mods/jemcompat-3.9.1.jar","mods/jemcompat-3.9.2.jar");
  try(var updater=new ModpackUpdater(target,null,null,storage)){
   Object session=invoke(updater,"beginUpdateAttempt",new Class<?>[0]);
   invoke(session,"prepare",new Class<?>[]{boolean.class,boolean.class},false,true);
   Object prepared=invoke(session,"prepared",new Class<?>[0]);
   UpdatePlan plan=(UpdatePlan)invoke(prepared,"plan",new Class<?>[0]);
   for(var operation:plan.operations()){
    System.out.println("Native operation: "+operation.operation()+" "+operation.root()+" "+operation.relativePath());
    if(!allowed.contains(operation.relativePath()))throw new IllegalStateException("Unreviewed native mutation: "+operation.relativePath());
    if(operation.operation()==UpdatePlan.OperationType.DELETE){if(!operation.relativePath().equals("mods/jemcompat-3.9.1.jar"))throw new IllegalStateException("Unexpected delete: "+operation.relativePath());}
    else if(!sha(canonical.resolve(operation.relativePath())).equals(operation.expectedObjectHash()))throw new IllegalStateException("Target differs from canonical source: "+operation.relativePath());
   }
   if(!plan.conflicts().isEmpty())throw new IllegalStateException("Native plan contains conflicts");
   if(plan.operations().isEmpty())throw new IllegalStateException("Native plan contains no update");
   System.out.println("Approved publication: "+plan.packTarget().contentToken());
   ((UpdateAttempt)session).approve();((UpdateAttempt)session).commit();
   if(!new ClientGenerationStore(storage).headMatchesActive(active.modpackId,plan.packTarget().contentToken()))throw new IllegalStateException("Native installed head does not match");
   ClientStorageMutation.run(storage, () -> { storage.setDetached(active.modpackId,false); return null; });
   if(storage.isDetached(active.modpackId))throw new IllegalStateException("Native installation remains detached");
  }
 }
}
try { ApprovedJemCompatInstall.run(); } catch(Throwable failure){failure.printStackTrace();}
/exit
'''

jshell_script = server / "local/jemcompat-native.jsh"
jshell_log = server / "local/jemcompat-native.log"
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
    raise RuntimeError("Native active token differs from published head")
if (profile / "automodpack/client/update-transaction.json").exists():
    raise RuntimeError("Native transaction remains pending")

for relative in installed_paths:
    installed = profile / "automodpack/client/active" / relative
    if hashlib.sha256(installed.read_bytes()).hexdigest() != hashlib.sha256((canonical / relative).read_bytes()).hexdigest():
        raise RuntimeError("Installed file differs from canonical source: " + relative)
if (profile / "automodpack/client/active" / old_jar).exists():
    raise RuntimeError("Old JEMCompat jar remains active")

print("Exact native JEMCompat installation complete with matching token, files and no pending transaction")
