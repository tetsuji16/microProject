/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 ******************************************************************************/

import java.io.IOException;
import java.io.ObjectStreamClass;
import java.io.Serializable;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Pins {@code serialVersionUID} on serializable production classes so that the
 * classes can later move between packages without breaking MPO/POD files
 * (the JDK-computed default UID depends on the class name).
 *
 * <p>Run from the repository root, after
 * {@code .\gradlew.bat compileJava processResources writeSerialPinClasspath}:
 * <pre>
 *   java -Duser.language=en -Duser.country=US scripts/PinSerialVersionUid.java                              (insert missing UIDs into sources)
 *   java -Duser.language=en -Duser.country=US scripts/PinSerialVersionUid.java --check                      (exit 1 listing classes still missing a UID)
 *   java -Duser.language=en -Duser.country=US scripts/PinSerialVersionUid.java --dump build/serial-uids.txt (also write name=uid for all selected classes)
 * </pre>
 * All values are computed from the compiled (unmodified) classes before any source is edited, so
 * the inserted value equals the current computed one. Recompile before running again.
 *
 * <p>Selected: classes under {@code com.microproject.} in every
 * {@code modules/microproject_*}{@code /build/classes/java/main} that are Serializable and are not
 * interfaces, enums, records, anonymous/local/synthetic classes, or {@code javax.swing.JComponent}
 * descendants. Reasons: Swing components are not written to MPO/POD/workspace streams; anonymous and
 * local class names are compiler generated; enum and record serialVersionUID values are ignored by
 * the JDK.
 */
public final class PinSerialVersionUid {
	private static final String FIELD = "private static final long serialVersionUID = ";

	record Target(String module, Class<?> type, long uid) { }

	public static void main(String[] args) throws Exception {
		boolean check = false;
		Path dump = null;
		for (int i = 0; i < args.length; i++) {
			if (args[i].equals("--check")) check = true;
			else if (args[i].equals("--dump")) dump = Path.of(args[++i]);
			else throw new IllegalArgumentException("Unknown argument: " + args[i]);
		}
		Path root = Path.of("").toAbsolutePath();
		Path cpFile = root.resolve("build/serial-pin-classpath.txt");
		if (!Files.isRegularFile(cpFile))
			throw new IllegalStateException("Missing " + cpFile + "; run .\\gradlew.bat compileJava writeSerialPinClasspath");

		List<Path> modules = new ArrayList<>();
		try (Stream<Path> s = Files.list(root.resolve("modules"))) {
			s.filter(p -> p.getFileName().toString().startsWith("microproject_")
					&& Files.isDirectory(p.resolve("build/classes/java/main"))).sorted().forEach(modules::add);
		}
		List<URL> urls = new ArrayList<>();
		for (Path m : modules) urls.add(m.resolve("build/classes/java/main").toUri().toURL());
		// Resource bundles are needed because some serializable classes initialise messages statically.
		for (Path m : modules)
			if (Files.isDirectory(m.resolve("build/resources/main"))) urls.add(m.resolve("build/resources/main").toUri().toURL());
		for (String line : Files.readAllLines(cpFile)) if (!line.isBlank()) urls.add(Path.of(line.trim()).toUri().toURL());
		ClassLoader loader = new URLClassLoader(urls.toArray(URL[]::new), ClassLoader.getPlatformClassLoader());
		Thread.currentThread().setContextClassLoader(loader); // static initialisers may use the context loader

		// Phase 1: compute every value from the compiled classes.
		List<Target> missing = new ArrayList<>();
		Map<String, Long> all = new TreeMap<>();
		List<String> loadFailures = new ArrayList<>();
		Class<?> jcomponent = Class.forName("javax.swing.JComponent", false, loader);
		for (Path m : modules) {
			Path classes = m.resolve("build/classes/java/main");
			List<String> names = new ArrayList<>();
			try (Stream<Path> s = Files.walk(classes.resolve("com/microproject"))) {
				s.filter(p -> p.toString().endsWith(".class")).forEach(p -> {
					String rel = classes.relativize(p).toString().replace('\\', '/');
					names.add(rel.substring(0, rel.length() - 6).replace('/', '.'));
				});
			}
			Collections.sort(names);
			for (String name : names) {
				Class<?> c;
				try {
					c = Class.forName(name, false, loader);
				} catch (Throwable t) {
					loadFailures.add(name + " (" + t + ")");
					continue;
				}
				if (!selected(c, jcomponent)) continue;
				long uid = ObjectStreamClass.lookup(c).getSerialVersionUID();
				all.put(name, uid);
				if (!declaresUid(c)) missing.add(new Target(m.getFileName().toString(), c, uid));
			}
		}
		if (dump != null) {
			List<String> lines = new ArrayList<>();
			all.forEach((k, v) -> lines.add(k + "=" + v));
			Files.createDirectories(dump.toAbsolutePath().getParent());
			Files.write(dump, lines);
		}
		for (String f : loadFailures) System.err.println("WARNING could not load: " + f);

		if (check) {
			for (Target t : missing) System.out.println("MISSING " + t.module() + " " + t.type().getName());
			System.out.println("selected=" + all.size() + " missing=" + missing.size() + " loadFailures=" + loadFailures.size());
			System.exit(missing.isEmpty() && loadFailures.isEmpty() ? 0 : 1);
		}

		// Phase 2: edit sources.
		Map<String, Integer> perModule = new TreeMap<>();
		List<String> notLocated = new ArrayList<>();
		Map<Path, List<Target>> byFile = new TreeMap<>();
		for (Target t : missing) {
			Path file = locate(root, t);
			if (file == null) notLocated.add(t.module() + " " + t.type().getName());
			else byFile.computeIfAbsent(file, k -> new ArrayList<>()).add(t);
		}
		for (Map.Entry<Path, List<Target>> e : byFile.entrySet()) {
			// ISO-8859-1 round trip keeps every byte of the file unchanged; only ASCII is inserted.
			String text = new String(Files.readAllBytes(e.getKey()), StandardCharsets.ISO_8859_1);
			String masked = mask(text);
			String eol = text.contains("\r\n") ? "\r\n" : "\n";
			List<int[]> positions = new ArrayList<>();
			List<String> texts = new ArrayList<>();
			List<Target> done = new ArrayList<>();
			for (Target t : e.getValue()) {
				int[] open = findBody(masked, t.type());
				if (open == null) {
					notLocated.add(t.module() + " " + t.type().getName() + " (declaration not found in " + e.getKey() + ")");
					continue;
				}
				int lineStart = text.lastIndexOf('\n', open[1]) + 1;
				int p = lineStart;
				while (p < text.length() && (text.charAt(p) == ' ' || text.charAt(p) == '\t')) p++;
				String declIndent = text.substring(lineStart, p);
				String indent = declIndent + "\t";
				int eolAt = open[0] + 1;
				while (eolAt < text.length() && text.charAt(eolAt) != '\n' && text.charAt(eolAt) != '\r') eolAt++;
				boolean restBlank = text.substring(open[0] + 1, eolAt).isBlank();
				String field = indent + FIELD + t.uid() + "L;";
				texts.add(restBlank ? eol + field + eol : eol + field + eol + declIndent);
				positions.add(new int[] { open[0] + 1 });
				done.add(t);
			}
			Integer[] order = new Integer[positions.size()];
			for (int i = 0; i < order.length; i++) order[i] = i;
			Arrays.sort(order, (a, b) -> Integer.compare(positions.get(b)[0], positions.get(a)[0]));
			StringBuilder sb = new StringBuilder(text);
			for (int i : order) sb.insert(positions.get(i)[0], texts.get(i));
			Files.write(e.getKey(), sb.toString().getBytes(StandardCharsets.ISO_8859_1));
			for (Target t : done) perModule.merge(t.module(), 1, Integer::sum);
		}
		System.out.println("pinned per module: " + perModule);
		System.out.println("total selected=" + all.size() + " pinned=" + perModule.values().stream().mapToInt(i -> i).sum());
		for (String n : notLocated) System.out.println("NOT LOCATED " + n);
		System.exit(notLocated.isEmpty() ? 0 : 1);
	}

	private static boolean selected(Class<?> c, Class<?> jcomponent) {
		if (!Serializable.class.isAssignableFrom(c)) return false;
		if (c.isInterface() || c.isEnum() || c.isRecord() || c.isAnnotation()) return false;
		if (c.isAnonymousClass() || c.isLocalClass() || c.isSynthetic()) return false;
		return !jcomponent.isAssignableFrom(c);
	}

	private static boolean declaresUid(Class<?> c) {
		try {
			c.getDeclaredField("serialVersionUID");
			return true;
		} catch (NoSuchFieldException e) {
			return false;
		}
	}

	private static Path locate(Path root, Target t) throws IOException {
		Class<?> top = t.type();
		while (top.getDeclaringClass() != null) top = top.getDeclaringClass();
		String pkg = top.getPackageName().replace('.', '/');
		Path dir = root.resolve("modules").resolve(t.module()).resolve("src/main/java").resolve(pkg);
		Path direct = dir.resolve(top.getSimpleName() + ".java");
		if (Files.isRegularFile(direct)) return direct;
		if (!Files.isDirectory(dir)) return null;
		// Secondary top-level class declared in a differently named file of the same package.
		Pattern p = Pattern.compile("\\b(?:class|interface|enum|record)\\s+" + Pattern.quote(top.getSimpleName()) + "\\b");
		try (Stream<Path> s = Files.list(dir)) {
			for (Path f : (Iterable<Path>) s.filter(x -> x.toString().endsWith(".java"))::iterator) {
				String m = mask(new String(Files.readAllBytes(f), StandardCharsets.ISO_8859_1));
				if (p.matcher(m).find()) return f;
			}
		}
		return null;
	}

	/** Returns {indexOfOpeningBrace, indexOfDeclarationKeyword} for the class body, or null. */
	private static int[] findBody(String masked, Class<?> c) {
		List<String> chain = new ArrayList<>();
		for (Class<?> k = c; k != null; k = k.getDeclaringClass()) chain.add(0, k.getSimpleName());
		// depth[i]: brace depth of character i ('{' before opening, '}' after closing).
		int[] depth = new int[masked.length() + 1];
		int d = 0;
		for (int i = 0; i < masked.length(); i++) {
			char ch = masked.charAt(i);
			if (ch == '}') d--;
			depth[i] = d;
			if (ch == '{') d++;
		}
		int from = 0;
		int to = masked.length();
		int baseDepth = 0;
		int[] result = null;
		for (String name : chain) {
			Pattern p = Pattern.compile("\\b(?:class|interface|enum|record)\\s+" + Pattern.quote(name) + "\\b");
			Matcher m = p.matcher(masked);
			m.region(from, to);
			result = null;
			while (m.find()) {
				if (depth[m.start()] != baseDepth) continue;
				int open = masked.indexOf('{', m.end());
				if (open < 0 || open >= to) continue;
				result = new int[] { open, m.start() };
				break;
			}
			if (result == null) return null;
			int end = result[0] + 1;
			while (end < to && !(masked.charAt(end) == '}' && depth[end] == baseDepth)) end++;
			from = result[0] + 1;
			to = end;
			baseDepth++;
		}
		return result;
	}

	/** Replaces comments, string/char literals and text blocks by spaces (line breaks kept). */
	private static String mask(String s) {
		char[] a = s.toCharArray();
		int n = a.length;
		int i = 0;
		while (i < n) {
			char c = a[i];
			if (c == '/' && i + 1 < n && a[i + 1] == '/') {
				while (i < n && a[i] != '\n' && a[i] != '\r') a[i++] = ' ';
			} else if (c == '/' && i + 1 < n && a[i + 1] == '*') {
				a[i++] = ' ';
				a[i++] = ' ';
				while (i < n && !(a[i] == '*' && i + 1 < n && a[i + 1] == '/')) {
					if (a[i] != '\n' && a[i] != '\r') a[i] = ' ';
					i++;
				}
				if (i < n) a[i++] = ' ';
				if (i < n) a[i++] = ' ';
			} else if (c == '"' && i + 2 < n && a[i + 1] == '"' && a[i + 2] == '"') {
				a[i++] = ' ';
				a[i++] = ' ';
				a[i++] = ' ';
				while (i < n && !(a[i] == '"' && i + 2 < n && a[i + 1] == '"' && a[i + 2] == '"')) {
					if (a[i] == '\\' && i + 1 < n) a[i++] = ' ';
					if (a[i] != '\n' && a[i] != '\r') a[i] = ' ';
					i++;
				}
				for (int k = 0; k < 3 && i < n; k++) a[i++] = ' ';
			} else if (c == '"' || c == '\'') {
				char q = c;
				a[i++] = ' ';
				while (i < n && a[i] != q && a[i] != '\n') {
					if (a[i] == '\\' && i + 1 < n) a[i++] = ' ';
					if (a[i] != '\n' && a[i] != '\r') a[i] = ' ';
					i++;
				}
				if (i < n && a[i] == q) a[i++] = ' ';
			} else {
				i++;
			}
		}
		return new String(a);
	}
}
