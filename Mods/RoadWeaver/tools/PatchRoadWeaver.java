import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

public final class PatchRoadWeaver {
    private static final String ROAD = "net/shiroha233/roadweaver/features/path/pathlogic/core/Road.class";
    private static final String PROFILE = "net/shiroha233/roadweaver/features/path/pathlogic/surface/HeightProfileService.class";
    private static final String HELPER_OWNER = "net/shiroha233/roadweaver/features/path/pathlogic/surface/GroundAnchoredHeightProfile";

    private PatchRoadWeaver() {
    }

    public static void main(String[] arguments) throws Exception {
        if (arguments.length != 3) {
            throw new IllegalArgumentException("Expected input JAR, output JAR, and compiled helper directory");
        }
        Path input = Path.of(arguments[0]);
        Path output = Path.of(arguments[1]);
        Path helperRoot = Path.of(arguments[2]);
        Path helperClass = helperRoot.resolve(HELPER_OWNER + ".class");
        Set<String> patched = new HashSet<>();

        Files.createDirectories(output.getParent());
        try (JarFile source = new JarFile(input.toFile());
             JarOutputStream destination = new JarOutputStream(Files.newOutputStream(output))) {
            Enumeration<JarEntry> entries = source.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                if (entry.getName().equals(HELPER_OWNER + ".class")) {
                    continue;
                }
                destination.putNextEntry(new JarEntry(entry.getName()));
                if (!entry.isDirectory()) {
                    try (InputStream content = source.getInputStream(entry)) {
                        byte[] bytes = content.readAllBytes();
                        if (entry.getName().equals(ROAD)) {
                            bytes = patchRoad(bytes);
                            patched.add(ROAD);
                        } else if (entry.getName().equals(PROFILE)) {
                            bytes = patchProfile(bytes);
                            patched.add(PROFILE);
                        }
                        destination.write(bytes);
                    }
                }
                destination.closeEntry();
            }

            destination.putNextEntry(new JarEntry(HELPER_OWNER + ".class"));
            destination.write(Files.readAllBytes(helperClass));
            destination.closeEntry();

            destination.putNextEntry(new JarEntry("META-INF/jem-roadweaver-patch.txt"));
            destination.write(("RoadWeaver 2.1.1-1.20.1 JEM ground-anchor patch\n"
                    + "Prevents backward slope smoothing from raising low terrain endpoints\n"
                    + "Caps ordinary road lift to configured causeway depth\n").getBytes(java.nio.charset.StandardCharsets.UTF_8));
            destination.closeEntry();
        }

        if (!patched.equals(Set.of(ROAD, PROFILE))) {
            Files.deleteIfExists(output);
            throw new IllegalStateException("Expected RoadWeaver classes were not patched: " + patched);
        }
    }

    private static byte[] patchRoad(byte[] input) {
        ClassNode type = read(input);
        int changes = 0;
        for (MethodNode method : type.methods) {
            if (!method.name.equals("computeTargetY") || !method.desc.endsWith(")Ljava/util/List;")) {
                continue;
            }
            for (AbstractInsnNode instruction = method.instructions.getFirst(); instruction != null; instruction = instruction.getNext()) {
                if (instruction.getOpcode() == Opcodes.ARETURN) {
                    InsnList call = new InsnList();
                    call.add(new VarInsnNode(Opcodes.ALOAD, 1));
                    call.add(new VarInsnNode(Opcodes.ALOAD, 2));
                    call.add(new VarInsnNode(Opcodes.ALOAD, 4));
                    call.add(new MethodInsnNode(
                            Opcodes.INVOKESTATIC,
                            HELPER_OWNER,
                            "adjustRoadTargets",
                            "(Ljava/util/List;Ljava/util/List;Ljava/util/List;Lnet/shiroha233/roadweaver/config/RoadGenerationConfig;)Ljava/util/List;",
                            false));
                    method.instructions.insertBefore(instruction, call);
                    changes++;
                }
            }
        }
        if (changes == 0) {
            throw new IllegalStateException("Road.computeTargetY return was not found");
        }
        return write(type);
    }

    private static byte[] patchProfile(byte[] input) {
        ClassNode type = read(input);
        int changes = 0;
        String descriptor = "(Lnet/minecraft/world/level/WorldGenLevel;Ljava/util/List;Lnet/minecraft/world/level/ChunkPos;IZILjava/util/List;)Lnet/shiroha233/roadweaver/features/path/pathlogic/surface/HeightProfileService$HeightProfile;";
        for (MethodNode method : type.methods) {
            if (!method.name.equals("build") || !method.desc.equals(descriptor)) {
                continue;
            }
            for (AbstractInsnNode instruction = method.instructions.getFirst(); instruction != null; instruction = instruction.getNext()) {
                if (instruction.getOpcode() == Opcodes.ARETURN) {
                    InsnList call = new InsnList();
                    call.add(new VarInsnNode(Opcodes.ALOAD, 1));
                    call.add(new VarInsnNode(Opcodes.ILOAD, 4));
                    call.add(new VarInsnNode(Opcodes.ILOAD, 5));
                    call.add(new MethodInsnNode(
                            Opcodes.INVOKESTATIC,
                            HELPER_OWNER,
                            "adjustPlacementProfile",
                            "(Lnet/shiroha233/roadweaver/features/path/pathlogic/surface/HeightProfileService$HeightProfile;Ljava/util/List;ZI)Lnet/shiroha233/roadweaver/features/path/pathlogic/surface/HeightProfileService$HeightProfile;",
                            false));
                    method.instructions.insertBefore(instruction, call);
                    changes++;
                }
            }
        }
        if (changes == 0) {
            throw new IllegalStateException("HeightProfileService.build return was not found");
        }
        return write(type);
    }

    private static ClassNode read(byte[] input) {
        ClassNode result = new ClassNode();
        new ClassReader(input).accept(result, 0);
        return result;
    }

    private static byte[] write(ClassNode type) {
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        type.accept(writer);
        return writer.toByteArray();
    }
}
