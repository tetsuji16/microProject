/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.pm.ccpm;

import java.time.ZoneOffset;
import java.time.temporal.IsoFields;
import java.util.List;

/** Deterministic CSV/HTML reports built from persisted CCPM observations. */
public final class CriticalChainReportService {
	public String toCsv(CriticalChainBufferHistory history) {
		StringBuilder out = new StringBuilder("observedAt,actorId,actorName,progressPercent,consumptionPercent,zone,baselineId\n");
		if (history != null) for (CriticalChainBufferHistory.Point p : history.points()) {
			out.append(p.observedAt()).append(',').append(csv(p.actorId())).append(',').append(csv(p.actorName())).append(',')
				.append(p.progressPercent()).append(',').append(p.consumptionPercent()).append(',').append(p.zone()).append(',').append(csv(p.baselineId())).append('\n');
		}
		return out.toString();
	}

	public String toHtml(String projectName, CriticalChainBufferHistory history) {
		StringBuilder out = new StringBuilder("<!doctype html><meta charset=\"utf-8\"><title>CCPM report</title><h1>CCPM report: ");
		out.append(escape(projectName == null ? "" : projectName)).append("</h1><table><thead><tr><th>Observed</th><th>Actor</th><th>Progress %</th><th>Buffer %</th><th>Zone</th></tr></thead><tbody>");
		if (history != null) for (CriticalChainBufferHistory.Point p : history.points()) out.append("<tr><td>").append(p.observedAt()).append("</td><td>").append(escape(p.actorName())).append("</td><td>").append(p.progressPercent()).append("</td><td>").append(p.consumptionPercent()).append("</td><td>").append(escape(p.zone())).append("</td></tr>");
		return out.append("</tbody></table>").toString();
	}

	public List<CriticalChainBufferHistory.Point> isoWeek(CriticalChainBufferHistory history, int week) {
		if (history == null) return List.of();
		return history.points().stream().filter(p -> p.observedAt().atZone(ZoneOffset.UTC).get(IsoFields.WEEK_OF_WEEK_BASED_YEAR) == week).toList();
	}

	private static String csv(String value) { String s = value == null ? "" : value; return s.contains(",") || s.contains("\"") ? "\"" + s.replace("\"", "\"\"") + "\"" : s; }
	private static String escape(String value) { return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;"); }
}
