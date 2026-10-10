import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.JavacTask;
import com.sun.source.util.TreePathScanner;
import com.sun.source.util.Trees;
import java.io.DataInputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.ToolProvider;

public final class JavaSourceMetrics {
    public static void main(String[] args) throws Exception {
        var input = new DataInputStream(System.in);
        List<JavaFileObject> files = new ArrayList<>();
        int count = input.readInt();
        for (int index = 0; index < count; index++) files.add(new MetricSource(read(input), read(input)));
        var compiler = ToolProvider.getSystemJavaCompiler();
        var task = (JavacTask) compiler.getTask(null, null, null, List.of("-proc:none"), null, files);
        var units = task.parse();
        var trees = Trees.instance(task);
        for (var unit : units) new MetricVisitor(unit, trees).scan(unit, null);
    }

    private static String read(DataInputStream input) throws Exception {
        return new String(input.readNBytes(input.readInt()), StandardCharsets.UTF_8);
    }
}

final class MetricSource extends SimpleJavaFileObject {
    private final String source;

    MetricSource(String name, String source) {
        super(URI.create("string:///" + name), Kind.SOURCE);
        this.source = source;
    }

    @Override
    public CharSequence getCharContent(boolean ignoreEncodingErrors) { return source; }
}

final class MetricVisitor extends TreePathScanner<Void, Void> {
    private final CompilationUnitTree unit;
    private final Trees trees;

    MetricVisitor(CompilationUnitTree unit, Trees trees) {
        this.unit = unit;
        this.trees = trees;
    }

    @Override
    public Void visitClass(ClassTree node, Void unused) {
        var parent = getCurrentPath().getParentPath().getLeaf();
        String category = node.getSimpleName().isEmpty() ? "anonymous"
                : parent.getKind() == Tree.Kind.COMPILATION_UNIT ? "top"
                : parent instanceof ClassTree ? "nested" : "local";
        emit(category, node.getSimpleName().toString(), node);
        return super.visitClass(node, unused);
    }

    @Override
    public Void visitMethod(MethodTree node, Void unused) {
        emit("method", node.getName().toString(), node);
        return super.visitMethod(node, unused);
    }

    private void emit(String category, String name, Tree node) {
        var positions = trees.getSourcePositions();
        long start = positions.getStartPosition(unit, node), end = positions.getEndPosition(unit, node);
        System.out.println(category + "\t" + unit.getSourceFile().toUri().getPath().substring(1)
                + "\t" + name + "\t" + start + "\t" + end);
    }
}
