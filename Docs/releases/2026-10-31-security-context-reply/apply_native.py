from pathlib import Path
import hashlib
import json
import os
import subprocess


root = Path(__file__).resolve().parents[3]
server = root / "Server"
profile = Path("C:/Users/user/AppData/Roaming/.minecraft/versions/JustEnoughMods")
publication = server / "local/minerent-production"
published_source = publication / "host-modpack/main"
expected_jar_sha256 = "8614637b96854c7d2831cf28521a2513d5ff512e47237426b8576d7487c6e5d2"
expected_manifest_sha256 = "6e22ec975e79b7997ade38ce48551e3d3b557a20311645f9281e5068a5e7f987"
expected_projection_sha256 = "7b13726e054137077f5f64ac61c92e35e7ff025d1760071860bbb0498995c130"
expected_journal_sha256 = "4fadef365e70e2d17b00bf13f7f40a8def0f0b445af4ddd28099badb9a2e6fdc"
expected_content_token = "b3b16bde689df823f19c00ad9b18a4ad8f733aa7"


def sha256(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


for path, expected in (
    (publication / "current-projection.json", expected_projection_sha256),
    (publication / "journal.jsonl", expected_journal_sha256),
    (published_source / "jem-pack/manifest.json", expected_manifest_sha256),
    (published_source / "mods/jem_security-1.0.7.jar", expected_jar_sha256),
):
    if not path.is_file() or sha256(path) != expected:
        raise SystemExit(f"MineRENT publication evidence differs from the reviewed release: {path}")
processes = subprocess.run(
    [
        "pwsh", "-NoProfile", "-Command",
        "Get-CimInstance Win32_Process -Filter \"Name='java.exe' OR Name='javaw.exe'\" | "
        "Where-Object { $_.CommandLine -and $_.CommandLine.Contains('versions\\JustEnoughMods') } | "
        "Select-Object -ExpandProperty ProcessId",
    ],
    capture_output=True, text=True, check=True,
)
if processes.stdout.strip():
    raise SystemExit("Close JustEnoughMods before native installation: " + processes.stdout.strip())

libraries = json.loads((profile / "JustEnoughMods.json").read_text())["libraries"]
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
class ApprovedSecurityContextReplyInstall {
 static Object invoke(Object object,String name,Class<?>[] types,Object... values)throws Exception {Method method=object.getClass().getDeclaredMethod(name,types);method.setAccessible(true);return method.invoke(object,values);}
 static String sha(Path path)throws Exception{return java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-1").digest(Files.readAllBytes(path)));}
 static void run()throws Exception{
  Constants.LOADER="forge";Constants.MC_VERSION="1.20.1";Constants.LOADER_VERSION="47.4.10";Constants.preload=false;
  Constants.MODPACK_LOADER=new pl.skidam.automodpack_loader_core_forge.mods.ModpackLoader();
  var storage=ClientStorage.open(Path.of(System.getProperty("user.dir")));
  Constants.clientConfig=ReconfConfigs.read(storage.clientConfigFile(),ClientConfigJsons.ClientConfigFieldsV3.class).orElseThrow();
  var active=storage.readActiveState();
  if(!active.modpackId.equals("fn9aq74"))throw new IllegalStateException("Unexpected active modpack: "+active.modpackId);
  Path serverRoot=Path.of("C:/MyProjects/Minecraft/JustEnoughMods/Server/local/minerent-production");
  Path fetchedHead=Files.createTempFile("security-context-head-", ".json"), fetchedJournal=Files.createTempFile("security-context-journal-", ".jsonl");
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
  Path staged=serverRoot.resolve("host-modpack/main");
  try(var updater=new ModpackUpdater(target,null,null,storage)){
   Object session=invoke(updater,"beginUpdateAttempt",new Class<?>[0]);
   invoke(session,"prepare",new Class<?>[]{boolean.class,boolean.class},false,true);
   Object prepared=invoke(session,"prepared",new Class<?>[0]);
   UpdatePlan plan=(UpdatePlan)invoke(prepared,"plan",new Class<?>[0]);
   var deleted=new HashSet<String>();var changed=new HashSet<String>();
   for(var operation:plan.operations()){
    String path=operation.relativePath();
    System.out.println("Native operation: "+operation.operation()+" "+operation.root()+" "+path);
    Path reviewed=staged.resolve(path).normalize();
    if(!reviewed.startsWith(staged))throw new IllegalStateException("Native path escapes reviewed publication: "+path);
    if(operation.operation()==UpdatePlan.OperationType.DELETE){
     if(!path.equals("mods/jem_security-1.0.5.jar"))throw new IllegalStateException("Unexpected MineRENT deletion: "+path);
     deleted.add(path);if(Files.exists(reviewed))throw new IllegalStateException("Delete contradicts reviewed publication: "+path);
    } else {
     if(!path.equals("jem-pack/manifest.json")&&!path.equals("mods/jem_security-1.0.7.jar"))throw new IllegalStateException("Unexpected MineRENT replacement: "+path);
     changed.add(path);if(!Files.isRegularFile(reviewed)||!sha(reviewed).equals(operation.expectedObjectHash()))throw new IllegalStateException("Target differs from reviewed publication source: "+path);
    }
   }
   if(!plan.conflicts().isEmpty())throw new IllegalStateException("Native plan contains conflicts");
   if(!plan.packTarget().contentToken().equals("b3b16bde689df823f19c00ad9b18a4ad8f733aa7"))throw new IllegalStateException("Native plan targets an unexpected MineRENT token");
   Path activeMods=Path.of(System.getProperty("user.dir")).resolve("automodpack/client/active/mods");
   try(var stream=Files.list(activeMods)){for(Path file:(Iterable<Path>)stream::iterator){String name=file.getFileName().toString();if(name.startsWith("jem_security-")&&!name.equals("jem_security-1.0.7.jar")&&!deleted.contains("mods/"+name))throw new IllegalStateException("Native plan leaves retired JEM Security active: "+name);}}
   System.out.println("Approved publication: "+plan.packTarget().contentToken());
   ((UpdateAttempt)session).approve();((UpdateAttempt)session).commit();
   if(!new ClientGenerationStore(storage).headMatchesActive(active.modpackId,plan.packTarget().contentToken()))throw new IllegalStateException("Native installed head does not match");
   ClientStorageMutation.run(storage, () -> { storage.setDetached(active.modpackId,false); return null; });
   if(storage.isDetached(active.modpackId))throw new IllegalStateException("Native installation remains detached");
  }
 }
}
try { ApprovedSecurityContextReplyInstall.run(); } catch(Throwable failure){failure.printStackTrace();}
/exit
'''

jshell_script = server / "local/security-context-reply-native.jsh"
jshell_log = server / "local/security-context-reply-native.log"
jshell_script.write_text(source, encoding="utf-8")
with jshell_log.open("w", encoding="utf-8") as log:
    result = subprocess.run(
        [
            "C:/Users/user/.jdks/ms-17.0.15/bin/jshell.exe",
            "--class-path", os.pathsep.join(str(path) for path in [bootstrap, *dependencies]),
            "--feedback", "concise", str(jshell_script),
        ],
        cwd=profile, stdout=log, stderr=subprocess.STDOUT,
    )
if result.returncode != 0:
    raise RuntimeError(f"Native installation command failed with exit code {result.returncode}")

log_text = jshell_log.read_text(encoding="utf-8", errors="replace")
print(log_text[-5000:])
if "Exception" in log_text or "Error" in log_text:
    raise RuntimeError("Native installation log contains a failure")

active_after = json.loads((profile / "automodpack/client/active-state.json").read_text())
published = json.loads((publication / "current-projection.json").read_text())
if active_after["modpackId"] != "fn9aq74" or active_after["contentToken"] != published["contentToken"]:
    raise RuntimeError("Native active state differs from the reviewed published head")
if active_after["contentToken"] != expected_content_token:
    raise RuntimeError("Native active state differs from the MineRENT production token")
if (profile / "automodpack/client/update-transaction.json").exists():
    raise RuntimeError("Native transaction remains pending")
security_jars = list((profile / "automodpack/client/active/mods").glob("jem_security-*.jar"))
if [path.name for path in security_jars] != ["jem_security-1.0.7.jar"]:
    raise RuntimeError("Unexpected active JEM Security files: " + ", ".join(path.name for path in security_jars))
if sha256(security_jars[0]) != expected_jar_sha256:
    raise RuntimeError("Installed JEM Security 1.0.7 differs from the approved artifact")
installed_manifest = profile / "automodpack/client/active/jem-pack/manifest.json"
if sha256(installed_manifest) != expected_manifest_sha256:
    raise RuntimeError("Installed JEM manifest differs from the reviewed publication")
print("Exact native security-context installation complete with matching token, files and no pending transaction")
