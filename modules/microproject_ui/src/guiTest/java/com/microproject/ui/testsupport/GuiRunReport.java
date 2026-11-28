/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.ui.testsupport;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

/**
 * Folds every {@code runs/&lt;class&gt;/&lt;test&gt;/summary.json} of one artifacts directory into
 * {@code runs/summary.json} and a single-file {@code runs/report.html}.
 * Usage: {@code GuiRunReport <artifacts-dir>}.
 */
public final class GuiRunReport {
	private static final List<String> COUNT_KEYS = List.of("log-warning", "log-severe", "uncaught", "edt-stall",
		"edt-violation", "step", "snapshot");
	private static final List<String> PROBLEM_KEYS = List.of("uncaught", "log-severe", "edt-stall", "edt-violation");

	private GuiRunReport() {
	}

	public static void main(String[] args) throws IOException {
		Path artifacts = Path.of(args.length > 0 ? args[0] : System.getProperty("microproject.gui.artifacts.dir",
			"build/reports/guiTest-artifacts"));
		write(artifacts);
	}

	static void write(Path artifacts) throws IOException {
		Path runs = artifacts.resolve("runs");
		ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
		List<Map<String, Object>> rows = new ArrayList<>();
		if (Files.isDirectory(runs)) {
			List<Path> files;
			try (Stream<Path> paths = Files.walk(runs, 3)) {
				files = paths.filter(path -> path.getFileName().toString().equals("summary.json")
					&& runs.relativize(path).getNameCount() == 3).sorted().toList();
			}
			for (Path file : files) {
				JsonNode node = mapper.readTree(file.toFile());
				Map<String, Object> row = new LinkedHashMap<>();
				row.put("class", node.path("class").asText());
				row.put("method", node.path("method").asText());
				row.put("status", node.path("status").asText());
				row.put("durationMillis", node.path("durationMillis").asLong());
				Map<String, Object> counts = new LinkedHashMap<>();
				for (String key : COUNT_KEYS) counts.put(key, node.path("counts").path(key).asInt());
				row.put("counts", counts);
				row.put("failure", node.path("failure").isMissingNode() ? null : node.path("failure").path("type").asText()
					+ ": " + node.path("failure").path("message").asText());
				row.put("directory", runs.relativize(file.getParent()).toString().replace('\\', '/'));
				rows.add(row);
			}
		}
		rows.sort(Comparator.comparingInt((Map<String, Object> row) -> hasProblem(row) ? 0 : 1)
			.thenComparing(row -> (String) row.get("class")).thenComparing(row -> (String) row.get("method")));
		Map<String, Object> totals = new LinkedHashMap<>();
		totals.put("tests", rows.size());
		for (String status : List.of("passed", "failed", "aborted", "disabled")) {
			totals.put(status, rows.stream().filter(row -> status.equals(row.get("status"))).count());
		}
		for (String key : COUNT_KEYS) {
			totals.put(key, rows.stream().mapToInt(row -> ((Map<?, ?>) row.get("counts")).get(key) instanceof Integer i ? i : 0).sum());
		}
		Map<String, Object> summary = new LinkedHashMap<>();
		summary.put("totals", totals);
		summary.put("tests", rows);
		Files.createDirectories(runs);
		mapper.writeValue(runs.resolve("summary.json").toFile(), summary);
		Files.writeString(runs.resolve("report.html"), html(rows, totals), StandardCharsets.UTF_8);
	}

	private static boolean hasProblem(Map<String, Object> row) {
		String status = (String) row.get("status");
		if ("failed".equals(status) || "aborted".equals(status)) return true;
		Map<?, ?> counts = (Map<?, ?>) row.get("counts");
		for (String key : PROBLEM_KEYS) {
			if (counts.get(key) instanceof Integer count && count > 0) return true;
		}
		return false;
	}

	private static String html(List<Map<String, Object>> rows, Map<String, Object> totals) {
		StringBuilder html = new StringBuilder();
		html.append("<!DOCTYPE html><html><head><meta charset=\"utf-8\"><title>GUI run report</title><style>")
			.append("body{font-family:sans-serif}table{border-collapse:collapse}td,th{border:1px solid #999;padding:2px 6px;text-align:left}")
			.append("tr.problem{background:#fdd}td.n{text-align:right}</style></head><body><h1>GUI run report</h1><p>")
			.append(escape(totals.toString())).append("</p><table><tr><th>class</th><th>test</th><th>status</th><th>ms</th>");
		for (String key : COUNT_KEYS) html.append("<th>").append(key).append("</th>");
		html.append("<th>failure</th></tr>\n");
		for (Map<String, Object> row : rows) {
			Map<?, ?> counts = (Map<?, ?>) row.get("counts");
			html.append(hasProblem(row) ? "<tr class=\"problem\">" : "<tr>")
				.append("<td>").append(escape(String.valueOf(row.get("class")))).append("</td><td>")
				.append(escape(String.valueOf(row.get("method")))).append("</td><td>")
				.append(escape(String.valueOf(row.get("status")))).append("</td><td class=\"n\">")
				.append(row.get("durationMillis")).append("</td>");
			for (String key : COUNT_KEYS) html.append("<td class=\"n\">").append(counts.get(key)).append("</td>");
			Object failure = row.get("failure");
			html.append("<td>").append(failure == null ? "" : escape(String.valueOf(failure))).append("</td></tr>\n");
		}
		return html.append("</table></body></html>\n").toString();
	}

	private static String escape(String text) {
		return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
	}
}
