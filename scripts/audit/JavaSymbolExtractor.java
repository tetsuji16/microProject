import com.sun.source.tree.BlockTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.JavacTask;
import com.sun.source.util.SourcePositions;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreePathScanner;
import com.sun.source.util.Trees;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.HashMap;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

/** Emits parser-backed Java symbol ranges for the ProjectLibre delta inventory. */
public final class JavaSymbolExtractor {
	private JavaSymbolExtractor() {
	}

	public static void main(String[] args) throws Exception {
		if (args.length != 1) {
			System.err.println("usage: JavaSymbolExtractor <source-root>");
			System.exit(2);
		}
		Path root = Path.of(args[0]).toAbsolutePath().normalize();
		List<Path> sources = new ArrayList<>();
		try (var paths = Files.walk(root)) {
			paths.filter(path -> Files.isRegularFile(path) && path.toString().endsWith(".java"))
				.sorted()
				.forEach(sources::add);
		}

		JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
		if (compiler == null) {
			throw new IllegalStateException("A full JDK is required");
		}
		DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
		try (StandardJavaFileManager files = compiler.getStandardFileManager(diagnostics, Locale.ROOT, StandardCharsets.UTF_8)) {
			Iterable<? extends JavaFileObject> units = files.getJavaFileObjectsFromPaths(sources);
			JavacTask task = (JavacTask) compiler.getTask(null, files, diagnostics, List.of("-proc:none"), null, units);
			Iterable<? extends CompilationUnitTree> parsed = task.parse();
			Trees trees = Trees.instance(task);
			SourcePositions positions = trees.getSourcePositions();
			System.out.println("path\tkind\tsymbol\tcanonical_symbol\tstart_line\tend_line\tcontent_sha256");
			for (CompilationUnitTree unit : parsed) {
				Path source = Path.of(unit.getSourceFile().toUri()).toAbsolutePath().normalize();
				String text = Files.readString(source, StandardCharsets.UTF_8);
				String relative = root.relativize(source).toString().replace('\\', '/');
				new SymbolScanner(unit, positions, relative, text).scan(unit, null);
			}
		}

		long errors = diagnostics.getDiagnostics().stream()
			.filter(diagnostic -> diagnostic.getKind() == Diagnostic.Kind.ERROR)
			.count();
		if (errors > 0) {
			diagnostics.getDiagnostics().stream()
				.filter(diagnostic -> diagnostic.getKind() == Diagnostic.Kind.ERROR)
				.limit(20)
				.forEach(diagnostic -> System.err.println(diagnostic.toString()));
			System.err.println("Java parser errors: " + errors);
			System.exit(3);
		}
	}

	private static final class SymbolScanner extends TreePathScanner<Void, Void> {
		private final CompilationUnitTree unit;
		private final SourcePositions positions;
		private final String relativePath;
		private final String source;
		private final String packageName;
		private final Deque<String> typeNames = new ArrayDeque<>();
		private final Map<String, Integer> initializerCounts = new HashMap<>();

		private SymbolScanner(CompilationUnitTree unit, SourcePositions positions, String relativePath, String source) {
			this.unit = unit;
			this.positions = positions;
			this.relativePath = relativePath;
			this.source = source;
			this.packageName = unit.getPackageName() == null ? "" : unit.getPackageName().toString();
		}

		@Override
		public Void visitClass(ClassTree node, Void unused) {
			String simpleName = node.getSimpleName().toString();
			if (simpleName.isEmpty()) {
				simpleName = "<anonymous>@" + startLine(node);
			}
			typeNames.addLast(simpleName);
			emit("TYPE", qualifiedType(), node);
			super.visitClass(node, unused);
			typeNames.removeLast();
			return null;
		}

		@Override
		public Void visitMethod(MethodTree node, Void unused) {
			if (!typeNames.isEmpty()) {
				String name = node.getReturnType() == null ? "<init>" : node.getName().toString();
				String parameters = node.getParameters().stream()
					.map(parameter -> normalizeType(parameter.getType().toString()))
					.reduce((left, right) -> left + "," + right)
					.orElse("");
				emit(node.getReturnType() == null ? "CONSTRUCTOR" : "METHOD",
					qualifiedType() + "#" + name + "(" + parameters + ")", node);
			}
			return super.visitMethod(node, unused);
		}

		@Override
		public Void visitVariable(VariableTree node, Void unused) {
			TreePath parentPath = getCurrentPath().getParentPath();
			if (!typeNames.isEmpty() && parentPath != null && parentPath.getLeaf() instanceof ClassTree) {
				emit("FIELD", qualifiedType() + "#" + node.getName(), node);
			}
			return super.visitVariable(node, unused);
		}

		@Override
		public Void visitBlock(BlockTree node, Void unused) {
			TreePath parentPath = getCurrentPath().getParentPath();
			if (!typeNames.isEmpty() && parentPath != null && parentPath.getLeaf() instanceof ClassTree) {
				String name = node.isStatic() ? "<clinit>" : "<init-block>";
				String counterKey = qualifiedType() + "#" + name;
				int ordinal = initializerCounts.merge(counterKey, 1, Integer::sum);
				emit("HUNK", counterKey + "[" + ordinal + "]", node);
			}
			return super.visitBlock(node, unused);
		}

		private String qualifiedType() {
			String nested = String.join("$", typeNames);
			return packageName.isEmpty() ? nested : packageName + "." + nested;
		}

		private long startLine(Tree node) {
			long start = positions.getStartPosition(unit, node);
			return start < 0 ? -1 : unit.getLineMap().getLineNumber(start);
		}

		private void emit(String kind, String symbol, Tree node) {
			long start = positions.getStartPosition(unit, node);
			long end = positions.getEndPosition(unit, node);
			if (start < 0 || end < start || end > source.length()) {
				return;
			}
			long startLine = unit.getLineMap().getLineNumber(start);
			long endLine = unit.getLineMap().getLineNumber(Math.max(start, end - 1));
			String rawContent = source.substring((int) start, (int) end);
			if ("TYPE".equals(kind)) {
				int bodyStart = rawContent.indexOf('{');
				if (bodyStart >= 0) rawContent = rawContent.substring(0, bodyStart);
			}
			String content = canonicalize(rawContent);
			System.out.printf("%s\t%s\t%s\t%s\t%d\t%d\t%s%n",
				escape(relativePath), kind, escape(symbol), escape(normalizeName(symbol)), startLine, endLine, sha256(content));
		}
	}

	private static String normalizeType(String value) {
		return normalizeName(value).replaceAll("\\s+", "");
	}

	private static String normalizeName(String value) {
		return value.replace("com.microproject", "com.projity")
			.replace("org.projectlibre1", "org.projity");
	}

	private static String canonicalize(String value) {
		StringBuilder result = new StringBuilder(value.length());
		boolean lineComment = false;
		boolean blockComment = false;
		boolean string = false;
		boolean character = false;
		boolean escaped = false;
		for (int index = 0; index < value.length(); index++) {
			char current = value.charAt(index);
			char next = index + 1 < value.length() ? value.charAt(index + 1) : '\0';
			if (lineComment) {
				if (current == '\n' || current == '\r') lineComment = false;
				continue;
			}
			if (blockComment) {
				if (current == '*' && next == '/') {
					blockComment = false;
					index++;
				}
				continue;
			}
			if (!string && !character && current == '/' && next == '/') {
				lineComment = true;
				index++;
				continue;
			}
			if (!string && !character && current == '/' && next == '*') {
				blockComment = true;
				index++;
				continue;
			}
			if (!string && !character && Character.isWhitespace(current)) {
				continue;
			}
			result.append(current);
			if (escaped) {
				escaped = false;
			} else if ((string || character) && current == '\\') {
				escaped = true;
			} else if (!character && current == '"') {
				string = !string;
			} else if (!string && current == '\'') {
				character = !character;
			}
		}
		return normalizeName(result.toString());
	}

	private static String sha256(String value) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
		} catch (NoSuchAlgorithmException exception) {
			throw new IllegalStateException(exception);
		}
	}

	private static String escape(String value) {
		return value.replace("\t", "\\t").replace("\r", "\\r").replace("\n", "\\n");
	}
}
