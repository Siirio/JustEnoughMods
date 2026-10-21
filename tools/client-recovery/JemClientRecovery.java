import java.lang.reflect.Method;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import javax.swing.JOptionPane;
import pl.skidam.automodpack_core.Constants;
import pl.skidam.automodpack_core.client.*;
import pl.skidam.automodpack_core.config.*;
import pl.skidam.automodpack_core.modpack.group.*;
import pl.skidam.automodpack_core.update.*;

/** Player-approved adapter to the installed native updater for the October 21 hotfix. */
public final class JemClientRecovery {
    private static final String TOKEN = "14522bb63bb0ef90ecb9a71e1650cb2bb19402c6";
    private static final String SETTLEMENTS = "mods/jem-settlements-2.1.0.jar";
    private static final String FIX = "e61c824bc391707922bd3bc0b7e244fe676955f7a77cd1ab60e60fec7c9466fe";

    private static String hash(Path path, String algorithm) throws Exception {
        try (var input = Files.newInputStream(path)) {
            var digest = MessageDigest.getInstance(algorithm);
            byte[] buffer = new byte[65536];
            for (int count; (count = input.read(buffer)) != -1;) digest.update(buffer, 0, count);
            return HexFormat.of().formatHex(digest.digest());
        }
    }

    private static Object invoke(Object object, String name, Class<?>[] types, Object... args) throws Exception {
        Method method = object.getClass().getDeclaredMethod(name, types);
        method.setAccessible(true);
        return method.invoke(object, args);
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }

    private static void requireGameClosed() {
        require(ProcessHandle.allProcesses().noneMatch(process -> process.pid() != ProcessHandle.current().pid()
            && Arrays.stream(process.info().arguments().orElse(new String[0])).anyMatch(argument ->
                argument.contains("net.minecraft.client.main.Main") || argument.contains("cpw.mods.bootstraplauncher.BootstrapLauncher"))),
            "Close Minecraft before recovery");
    }

    public static void main(String[] args) {
        int result = 1;
        try {
            apply(Path.of(args[0]).toRealPath(), Path.of(args[1]).toRealPath());
            result = 0;
        } catch (Throwable failure) {
            failure.printStackTrace();
            JOptionPane.showMessageDialog(null,
                "Восстановление не завершено. Не удаляйте кэш AutoModpack.\n"
                + "Сохраните текст из открытого окна и отправьте администратору.\n\n" + failure,
                "JEM: восстановление остановлено", JOptionPane.ERROR_MESSAGE);
        }
        System.exit(result);
    }

    private static void apply(Path profile, Path bundle) throws Exception {
        require(Path.of(System.getProperty("user.dir")).toRealPath().equals(profile), "Unexpected working directory");
        requireGameClosed();
        Constants.LOADER = "forge";
        Constants.MC_VERSION = "1.20.1";
        Constants.LOADER_VERSION = "47.4.10";
        Constants.preload = false;
        Constants.MODPACK_LOADER = new pl.skidam.automodpack_loader_core_forge.mods.ModpackLoader();
        var storage = ClientStorage.open(profile);
        require(!Files.exists(profile.resolve("automodpack/client/update-transaction.json")),
            "An earlier native transaction is pending; contact the administrator before continuing");
        var active = storage.readActiveState();
        require(active != null, "No installed AutoModpack generation. Install the normal JEM client first");
        if (TOKEN.equals(active.contentToken)) {
            require(hash(storage.activeDirectory().resolve(SETTLEMENTS), "SHA-256").equals(FIX), "Installed artifact differs");
            JOptionPane.showMessageDialog(null, "Исправление уже установлено. Можно запускать JustEnoughMods.");
            return;
        }
        Constants.clientConfig = ReconfConfigs.read(storage.clientConfigFile(),
            ClientConfigJsons.ClientConfigFieldsV3.class).orElseThrow();
        var entries = new JournalMirror(storage).entries(active.modpackId);
        require(!entries.isEmpty(), "No fetched server journal. Launch the game once, wait for the update check, close it and retry");
        var latest = entries.get(entries.size() - 1);
        var document = new ClientGenerationStore(storage).document(active.modpackId, latest);
        require(TOKEN.equals(document.contentToken()),
            "The cached server release differs. Launch once to fetch the current MineRENT release, close the game and retry");
        var target = SelectedModpackTarget.prepare(document, new ClientSelectionStore(storage.selectionFile()), ClientPlatform.WINDOWS);
        var payload = bundle.resolve("Файлы");
        var reviewed = Map.of(SETTLEMENTS, FIX, "jem-pack/manifest.json",
            "87f1ce9c0e863d6e285bf04424b8b582bc08750c3103be03fa8e3a86bdaa1a83");
        for (var entry : reviewed.entrySet()) {
            Path file = payload.resolve(entry.getKey());
            require(hash(file, "SHA-256").equals(entry.getValue()), "Recovery payload differs: " + entry.getKey());
            var item = target.flatTarget().list.stream().filter(value -> value.file.equals(entry.getKey())).findFirst().orElseThrow();
            require(hash(file, "SHA-1").equals(item.sha1), "Payload differs from native published target");
            ClientObjectStore.storeObject(storage, item.sha1, Files.readAllBytes(file));
        }
        System.out.println("Подготовка штатного плана AutoModpack. Это может занять несколько минут.");
        try (var updater = new ModpackUpdater(target, null, null, storage)) {
            Object session = invoke(updater, "beginUpdateAttempt", new Class<?>[0]);
            invoke(session, "prepare", new Class<?>[]{boolean.class, boolean.class}, false, true);
            Object prepared = invoke(session, "prepared", new Class<?>[0]);
            UpdatePlan plan = (UpdatePlan) invoke(prepared, "plan", new Class<?>[0]);
            require(plan.conflicts().isEmpty(), "The native plan has conflicts");
            require(TOKEN.equals(plan.packTarget().contentToken()), "Prepared target differs");
            int duplicates = 0;
            for (var operation : plan.operations()) {
                String path = operation.relativePath();
                System.out.println(operation.operation() + " " + operation.root() + " " + path);
                if (operation.operation() == UpdatePlan.OperationType.DELETE && operation.root() == UpdatePlan.Root.GAME_DIR) {
                    require(path.startsWith("mods/") && path.endsWith(".jar"), "Unreviewed deletion: " + path);
                    var retained = target.flatTarget().list.stream().filter(value -> value.file.equals(path)).findFirst().orElseThrow();
                    require(hash(profile.resolve(path), "SHA-1").equals(retained.sha1), "Nonmatching duplicate: " + path);
                    duplicates++;
                } else {
                    require(operation.operation() == UpdatePlan.OperationType.INSTALL_OBJECT && reviewed.containsKey(path)
                        && (operation.root() == UpdatePlan.Root.PROJECTION || operation.root() == UpdatePlan.Root.GAME_DIR),
                        "Unreviewed mutation: " + path);
                    require(hash(payload.resolve(path), "SHA-1").equals(operation.expectedObjectHash()), "Unreviewed bytes: " + path);
                }
            }
            int answer = JOptionPane.showConfirmDialog(null,
                "Установить исправление краша Xaero для JustEnoughMods?\n\n"
                + "Профиль: " + profile + "\n"
                + "Будут применены опубликованные Settlements и манифест.\n"
                + "Точных лишних копий модов в обычной mods: " + duplicates + ".\n"
                + "Эти моды остаются в активной установке AutoModpack.\n"
                + "Миры, настройки и личные данные не удаляются.\n\n"
                + "Закройте Minecraft перед подтверждением.",
                "JEM: подтвердите штатное обновление", JOptionPane.YES_NO_OPTION);
            if (answer != JOptionPane.YES_OPTION) {
                ((UpdateAttempt)session).cancel();
                System.out.println("Отменено игроком. Обновление не применено.");
                return;
            }
            requireGameClosed();
            ((UpdateAttempt)session).approve();
            ((UpdateAttempt)session).commit();
            require(TOKEN.equals(storage.readActiveState().contentToken), "Native generation did not commit");
            require(new ClientGenerationStore(storage).headMatchesActive(active.modpackId, TOKEN), "Native head differs");
            require(hash(storage.activeDirectory().resolve(SETTLEMENTS), "SHA-256").equals(FIX), "Installed artifact differs");
            require(!Files.exists(profile.resolve("automodpack/client/update-transaction.json")), "Pending transaction remains");
        }
        System.out.println("ГОТОВО: " + TOKEN + "; SHA256 " + FIX);
        JOptionPane.showMessageDialog(null, "Исправление установлено. Можно запускать JustEnoughMods.");
    }
}
