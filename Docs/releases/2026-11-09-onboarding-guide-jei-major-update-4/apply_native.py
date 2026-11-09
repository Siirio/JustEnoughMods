from pathlib import Path
import hashlib
import json
import os
import subprocess
import tempfile


root = Path(__file__).resolve().parents[3]
server = root / "Server"
profile = Path("C:/Users/user/AppData/Roaming/.minecraft/versions/test jem")
canonical = root / "Client/ManagedPack"
expected_operations = {
    "jem-pack/manifest.json": "PRESENT",
    "mods/jem-rewards-1.1.0.jar": "PRESENT",
}


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
class ApprovedOnboardingGuideJeiInstall {
 static Object invoke(Object object,String name,Class<?>[] types,Object... values)throws Exception {Method method=object.getClass().getDeclaredMethod(name,types);method.setAccessible(true);return method.invoke(object,values);}
 static String sha(Path path)throws Exception{return java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-1").digest(Files.readAllBytes(path)));}
 static void run()throws Exception{
  Constants.LOADER="forge";Constants.MC_VERSION="1.20.1";Constants.LOADER_VERSION="47.4.10";Constants.preload=false;
  Constants.MODPACK_LOADER=new pl.skidam.automodpack_loader_core_forge.mods.ModpackLoader();
  var storage=ClientStorage.open(Path.of(System.getProperty("user.dir")));
  Constants.clientConfig=ReconfConfigs.read(storage.clientConfigFile(),ClientConfigJsons.ClientConfigFieldsV3.class).orElseThrow();
  Path pendingPath=Path.of(System.getProperty("user.dir")).resolve("automodpack/client/update-transaction.json");
  if(Files.exists(pendingPath)){
   var resumed=pl.skidam.automodpack_core.client.UpdateTransactionSupport.executor().recoverLatest();
   System.out.println("Native recovery: "+resumed.status());
   if(!resumed.success())throw new IllegalStateException("Native deferred transaction did not recover");
   var resumedState=storage.readActiveState();
   ClientStorageMutation.run(storage,()->{storage.setDetached(resumedState.modpackId,false);return null;});
   return;
  }
  var active=storage.readActiveState();
  Path serverRoot=Path.of("C:/MyProjects/Minecraft/JustEnoughMods/Server/automodpack/server");
  Path fetchedHead=Files.createTempFile("onboarding-guide-jei-head-", ".json");
  Path fetchedJournal=Files.createTempFile("onboarding-guide-jei-journal-", ".jsonl");
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
  Map<String,String> expected=Map.of(
   "jem-pack/manifest.json","PRESENT",
   "mods/jem-rewards-1.1.0.jar","PRESENT");
  try(var updater=new ModpackUpdater(target,null,null,storage)){
   Object session=invoke(updater,"beginUpdateAttempt",new Class<?>[0]);
   invoke(session,"prepare",new Class<?>[]{boolean.class,boolean.class},false,true);
   Object prepared=invoke(session,"prepared",new Class<?>[0]);
   UpdatePlan plan=(UpdatePlan)invoke(prepared,"plan",new Class<?>[0]);
   var reviewed=new HashSet<String>();
   for(var operation:plan.operations()){
    String relative=operation.relativePath();
    System.out.println("Native operation: "+operation.operation()+" "+operation.root()+" "+relative);
    if(!expected.containsKey(relative))throw new IllegalStateException("Unreviewed native mutation: "+relative);
    boolean deletion=operation.operation()==UpdatePlan.OperationType.DELETE;
    if(deletion != expected.get(relative).equals("DELETE"))throw new IllegalStateException("Unexpected operation for "+relative);
    if(!deletion && !sha(canonical.resolve(relative)).equals(operation.expectedObjectHash()))throw new IllegalStateException("Target differs from canonical source: "+relative);
    if(deletion && Files.exists(canonical.resolve(relative)))throw new IllegalStateException("Deleted path still exists in canonical source: "+relative);
    reviewed.add(relative);
   }
   if(!plan.conflicts().isEmpty())throw new IllegalStateException("Native plan contains conflicts");
   if(!reviewed.equals(expected.keySet()))throw new IllegalStateException("Native plan differs from reviewed onboarding transition: "+reviewed);
   System.out.println("Approved publication: "+plan.packTarget().contentToken());
   ((UpdateAttempt)session).approve();((UpdateAttempt)session).commit();
   if(!new ClientGenerationStore(storage).headMatchesActive(active.modpackId,plan.packTarget().contentToken()))throw new IllegalStateException("Native installed head does not match");
   ClientStorageMutation.run(storage, () -> { storage.setDetached(active.modpackId,false); return null; });
   if(storage.isDetached(active.modpackId))throw new IllegalStateException("Native installation remains detached");
  }
 }
}
try { ApprovedOnboardingGuideJeiInstall.run(); } catch(Throwable failure){failure.printStackTrace();}
/exit
'''

with tempfile.TemporaryDirectory(prefix="jem-onboarding-guide-jei-") as temporary:
    jshell_script = Path(temporary) / "apply-native.jsh"
    jshell_log = Path(temporary) / "apply-native.log"
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
    terminal_markers = (
        "\njava.lang.IllegalStateException:",
        "\njava.lang.RuntimeException:",
        "\njava.lang.Error:",
    )
    if any(marker in log_text for marker in terminal_markers):
        raise RuntimeError("Native installation log contains a failure")

active_after = json.loads((profile / "automodpack/client/active-state.json").read_text())
published = json.loads((server / "automodpack/server/current-projection.json").read_text())
if active_after["contentToken"] != published["contentToken"]:
    raise RuntimeError("Native active token differs from published head")
if (profile / "automodpack/client/update-transaction.json").exists():
    raise RuntimeError("Native transaction remains pending")

active_root = profile / "automodpack/client/active"
for relative, state in expected_operations.items():
    installed = active_root / relative
    if state == "DELETE":
        if installed.exists():
            raise RuntimeError("Retired managed path remains active: " + relative)
    elif sha256(installed) != sha256(canonical / relative):
        raise RuntimeError("Installed file differs from canonical source: " + relative)

receipt = {
    "journalSequence": published["journalHead"],
    "contentToken": active_after["contentToken"],
    "artifacts": {
        relative: sha256(active_root / relative)
        for relative, state in expected_operations.items()
        if state == "PRESENT" and relative.startswith("mods/")
    },
    "pendingTransaction": False,
    "runtimeServerVerified": True,
    "playerVisibleAcceptanceVerified": False,
}
(Path(__file__).with_name("native-install-receipt.json")).write_text(
    json.dumps(receipt, indent=2) + "\n", encoding="utf-8"
)

print("Exact native onboarding installation complete with matching token and no pending transaction")
