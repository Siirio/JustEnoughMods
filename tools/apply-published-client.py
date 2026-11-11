from __future__ import annotations

import argparse
import hashlib
import json
import os
from pathlib import Path
import shutil
import subprocess


ROOT = Path(__file__).resolve().parents[1]
SERVER_STATE = ROOT / "Server/automodpack/server"
CANONICAL = ROOT / "Client/ManagedPack"
BOOTSTRAP_SOURCE = ROOT / "Mods/JEMBootstrap/build/libs/jem-bootstrap.jar"


def sha1(path: Path) -> str:
    digest = hashlib.sha1()
    with path.open("rb") as stream:
        for chunk in iter(lambda: stream.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def require(condition: bool, message: str) -> None:
    if not condition:
        raise SystemExit(message)


def java_string(value: Path | str) -> str:
    return json.dumps(str(value).replace("\\", "/"))


def profile_dependencies(profile: Path) -> list[Path]:
    descriptor = profile / f"{profile.name}.json"
    require(descriptor.is_file(), f"Missing TLauncher profile descriptor: {descriptor}")
    libraries = json.loads(descriptor.read_text(encoding="utf-8"))["libraries"]
    result: list[Path] = []
    library_root = profile.parent.parent / "libraries"
    for library in libraries:
        group, artifact, version = library["name"].split(":")[:3]
        candidate = library_root / group.replace(".", "/") / artifact / version / f"{artifact}-{version}.jar"
        if candidate.is_file():
            result.append(candidate)
    return result


def require_profile_closed(profile: Path) -> None:
    powershell = shutil.which("pwsh") or shutil.which("powershell")
    require(powershell is not None, "PowerShell is required")
    command = (
        "Get-CimInstance Win32_Process | Where-Object { "
        "$_.Name -match 'java|javaw' -and $_.CommandLine -and "
        "$_.CommandLine.IndexOf($env:JEM_PROFILE, [StringComparison]::OrdinalIgnoreCase) -ge 0 } | "
        "Select-Object -ExpandProperty ProcessId"
    )
    result = subprocess.run(
        [powershell, "-NoProfile", "-Command", command],
        env={**os.environ, "JEM_PROFILE": str(profile)},
        capture_output=True,
        text=True,
        check=True,
    )
    require(not result.stdout.strip(), "Close Minecraft before applying the published pack")


def main() -> None:
    parser = argparse.ArgumentParser(description="Apply the current published JEM pack through native AutoModpack")
    parser.add_argument("--profile", type=Path, required=True)
    args = parser.parse_args()

    profile = args.profile.resolve()
    require(profile.is_dir(), f"Profile does not exist: {profile}")
    require(profile.parent.name == "versions", "Profile must be a direct child of the TLauncher versions directory")
    require(CANONICAL.is_dir(), "Canonical Client/ManagedPack is missing")
    require((SERVER_STATE / "current-projection.json").is_file(), "Published server head is missing")
    require((SERVER_STATE / "journal.jsonl").is_file(), "Published server journal is missing")
    require(BOOTSTRAP_SOURCE.is_file(), "Current bootstrap artifact is missing")
    require_profile_closed(profile)

    bootstrap = profile / "mods/jem-bootstrap.jar"
    require(bootstrap.is_file(), "Profile bootstrap is missing")
    require(sha1(bootstrap) == sha1(BOOTSTRAP_SOURCE), "Profile bootstrap differs from the current approved artifact")
    dependencies = profile_dependencies(profile)
    require(dependencies, "No profile libraries were found")

    head = json.loads((SERVER_STATE / "current-projection.json").read_text(encoding="utf-8"))
    expected_token = head["contentToken"]
    log_path = ROOT / "Server/local/apply-published-client.log"
    script_path = ROOT / "Server/local/apply-published-client.jsh"
    log_path.parent.mkdir(parents=True, exist_ok=True)

    source = f'''
import java.nio.file.*;
import java.util.*;
import java.lang.reflect.*;
import pl.skidam.automodpack_core.Constants;
import pl.skidam.automodpack_core.client.*;
import pl.skidam.automodpack_core.config.*;
import pl.skidam.automodpack_core.update.*;
import pl.skidam.automodpack_core.modpack.group.*;
class ApplyPublishedJemPack {{
 static Object call(Object object,String name,Class<?>[] types,Object... values)throws Exception {{ Method method=object.getClass().getDeclaredMethod(name,types);method.setAccessible(true);return method.invoke(object,values); }}
 static String sha(Path path)throws Exception {{ try(var input=Files.newInputStream(path)){{var digest=java.security.MessageDigest.getInstance("SHA-1");byte[] buffer=new byte[1048576];for(int count;(count=input.read(buffer))!=-1;)digest.update(buffer,0,count);return java.util.HexFormat.of().formatHex(digest.digest());}} }}
 static void require(boolean condition,String message) {{ if(!condition)throw new IllegalStateException(message); }}
 static void run()throws Exception {{
  Constants.LOADER="forge";Constants.MC_VERSION="1.20.1";Constants.LOADER_VERSION="47.4.10";Constants.preload=false;
  Constants.MODPACK_LOADER=new pl.skidam.automodpack_loader_core_forge.mods.ModpackLoader();
  var storage=ClientStorage.open(Path.of(System.getProperty("user.dir")));
  System.out.println("STATE_ROOT="+storage.clientDirectory());
  Constants.clientConfig=ReconfConfigs.read(storage.clientConfigFile(),ClientConfigJsons.ClientConfigFieldsV3.class).orElseThrow();
  var active=storage.readActiveState();require(active!=null,"No installed AutoModpack generation to update");
  if(active.contentToken.equals({json.dumps(expected_token)})){{
   require(new ClientGenerationStore(storage).headMatchesActive(active.modpackId,active.contentToken),"Installed head differs");
   require(!Files.exists(storage.transactionFile()),"Native transaction remains pending");
   ClientStorageMutation.run(storage,()->{{storage.setDetached(active.modpackId,false);return null;}});
   System.out.println("INSTALLED_TOKEN="+active.contentToken);
   return;
  }}
  Path serverRoot=Path.of({java_string(SERVER_STATE)});
  Path fetchedHead=Files.createTempFile("jem-head-", ".json");Path fetchedJournal=Files.createTempFile("jem-journal-", ".jsonl");
  Files.copy(serverRoot.resolve("current-projection.json"),fetchedHead,StandardCopyOption.REPLACE_EXISTING);
  Files.copy(serverRoot.resolve("journal.jsonl"),fetchedJournal,StandardCopyOption.REPLACE_EXISTING);
  new HeadMirror(storage).replaceFrom(fetchedHead);
  var mirror=new JournalMirror(storage);mirror.replaceFrom(active.modpackId,fetchedJournal);
  var entries=mirror.entries(active.modpackId);require(!entries.isEmpty(),"Published journal is empty");var latest=entries.get(entries.size()-1);
  var hashes=new HashSet<String>();
  for(var entry:entries){{hashes.add(entry.policySha1());for(var change:entry.changes())if(change.toSha1()!=null&&!change.toSha1().isBlank())hashes.add(change.toSha1());}}
  for(String hash:hashes){{Path object=serverRoot.resolve("data/objects").resolve(hash.substring(0,2)).resolve(hash.substring(2));if(Files.isRegularFile(object))ClientObjectStore.storeObject(storage,hash,Files.readAllBytes(object));}}
  var document=new ClientGenerationStore(storage).document(active.modpackId,latest);
  require(document.contentToken().equals({json.dumps(expected_token)}),"Published document token differs");
  var target=SelectedModpackTarget.prepare(document,new ClientSelectionStore(storage.selectionFile()),ClientPlatform.WINDOWS);
  Path canonical=Path.of({java_string(CANONICAL)});
  try(var updater=new ModpackUpdater(target,null,null,storage)){{
   Object session=call(updater,"beginUpdateAttempt",new Class<?>[0]);
   call(session,"prepare",new Class<?>[]{{boolean.class,boolean.class}},false,true);
   Object prepared=call(session,"prepared",new Class<?>[0]);
   UpdatePlan plan=(UpdatePlan)call(prepared,"plan",new Class<?>[0]);
   require(plan.packTarget().contentToken().equals({json.dumps(expected_token)}),"Native plan token differs");
   Path gameDir=Path.of(System.getProperty("user.dir")).toAbsolutePath().normalize();
   for(var conflict:plan.conflicts()){{
    System.out.println("CONFLICT "+conflict);
    require(conflict.action()==UpdatePlan.ConflictAction.REMOVE_OWNED,"Native plan contains an unapproved local conflict");
    require(conflict.sourcePath().equals(conflict.targetPath()),"Owned replacement changes its logical path");
    Path existing=gameDir.resolve(conflict.sourcePath()).normalize();Path replacement=canonical.resolve(conflict.targetPath()).normalize();
    require(existing.startsWith(gameDir)&&Files.isRegularFile(existing),"Owned conflict source is absent or escaped the profile");
    require(replacement.startsWith(canonical)&&Files.isRegularFile(replacement),"Owned conflict target is absent or escaped the canonical pack");
    require(Files.size(existing)==conflict.sourceSize()&&sha(existing).equals(conflict.sourceHash()),"Owned conflict source differs from the reviewed native plan");
    require(Files.size(replacement)==conflict.targetSize()&&sha(replacement).equals(conflict.targetHash()),"Owned conflict target differs from the canonical pack");
   }}
   for(var operation:plan.operations()){{
    String relative=operation.relativePath();Path source=canonical.resolve(relative).normalize();
    System.out.println("OP "+operation.operation()+" "+operation.root()+" "+relative);
    require(source.startsWith(canonical),"Operation escaped canonical root: "+relative);
    if(operation.operation()==UpdatePlan.OperationType.INSTALL_OBJECT){{
     require(Files.isRegularFile(source),"Install source is absent from canonical pack: "+relative);
     require(sha(source).equals(operation.expectedObjectHash()),"Install bytes differ from canonical pack: "+relative);
    }} else if(operation.operation()==UpdatePlan.OperationType.DELETE){{
     require(!Files.exists(source),"Native plan deletes a current canonical path: "+relative);
    }} else throw new IllegalStateException("Unreviewed native operation: "+operation.operation()+" "+relative);
   }}
   require(!plan.operations().isEmpty(),"Native plan contains no update");
   ((UpdateAttempt)session).approve();((UpdateAttempt)session).commit();
   var installed=storage.readActiveState();
   require(installed!=null&&installed.contentToken.equals({json.dumps(expected_token)}),"Native generation did not commit");
   require(new ClientGenerationStore(storage).headMatchesActive(installed.modpackId,installed.contentToken),"Installed head differs");
   require(!Files.exists(storage.transactionFile()),"Native transaction remains pending");
   ClientStorageMutation.run(storage,()->{{storage.setDetached(installed.modpackId,false);return null;}});
   System.out.println("INSTALLED_TOKEN="+installed.contentToken);
  }}
 }}
}}
try {{ ApplyPublishedJemPack.run(); }} catch(Throwable failure) {{ failure.printStackTrace(); }}
/exit
'''
    script_path.write_text(source, encoding="utf-8")
    jshell = shutil.which("jshell") or "C:/Users/user/.jdks/ms-17.0.15/bin/jshell.exe"
    with log_path.open("w", encoding="utf-8") as log:
        result = subprocess.run(
            [jshell, "--class-path", os.pathsep.join(str(path) for path in [bootstrap, *dependencies]), "--feedback", "concise", str(script_path)],
            cwd=profile,
            stdout=log,
            stderr=subprocess.STDOUT,
        )
    output = log_path.read_text(encoding="utf-8", errors="replace")
    print(output[-16000:])
    require(result.returncode == 0, f"Native client apply exited with {result.returncode}")
    require(f"INSTALLED_TOKEN={expected_token}" in output, "Native client apply did not report the published token")
    require(not (profile / "automodpack").exists(), "Legacy gameDir/automodpack remains after migration")

    state_root_line = next(line for line in output.splitlines() if line.startswith("STATE_ROOT="))
    state_root = Path(state_root_line.removeprefix("STATE_ROOT=").strip())
    active_state = json.loads((state_root / "active-state.json").read_text(encoding="utf-8"))
    require(active_state["contentToken"] == expected_token, "External active state differs from the published head")
    require(not (state_root / "update-transaction.json").exists(), "External native transaction remains pending")

    checked_jars = 0
    for source in sorted((CANONICAL / "mods").glob("*.jar")):
        installed = profile / "mods" / source.name
        require(installed.is_file(), f"Managed JAR is missing from the standard mods directory: {source.name}")
        require(sha1(installed) == sha1(source), f"Managed JAR differs from canonical source: {source.name}")
        checked_jars += 1
    print(json.dumps({
        "profile": str(profile),
        "stateRoot": str(state_root),
        "contentToken": expected_token,
        "verifiedManagedJars": checked_jars,
        "legacyAutomodpackFolderRemoved": True,
    }, ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()
