package com.projectlibre1.exchange;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

/** Reads the task columns from the first worksheet of an XLSX workbook. */
final class XlsxTaskReader {
    private static final String WORKBOOK_PATH = "xl/workbook.xml";
    private static final String WORKBOOK_RELS_PATH = "xl/_rels/workbook.xml.rels";
    private static final String SHARED_STRINGS_PATH = "xl/sharedStrings.xml";
    private static final String STYLES_PATH = "xl/styles.xml";
    private static final long MILLIS_PER_DAY = 86_400_000L;

    private XlsxTaskReader() {
    }

    static List<TaskRow> read(File file) throws IOException {
        try (ZipFile workbook = new ZipFile(file)) {
            WorkbookInfo workbookInfo = readWorkbookInfo(workbook);
            String worksheetPath = findWorksheetPath(workbook, workbookInfo.relationshipId);
            List<String> sharedStrings = readSharedStrings(workbook);
            List<Boolean> percentageStyles = readPercentageStyles(workbook);
            return readWorksheet(workbook, worksheetPath, sharedStrings, percentageStyles,
                    workbookInfo.date1904);
        } catch (XMLStreamException | NumberFormatException e) {
            throw new IOException("Invalid XLSX workbook", e);
        }
    }

    private static WorkbookInfo readWorkbookInfo(ZipFile workbook)
            throws IOException, XMLStreamException {
        ZipEntry entry = requiredEntry(workbook, WORKBOOK_PATH);
        String relationshipId = null;
        boolean date1904 = false;
        try (InputStream input = workbook.getInputStream(entry)) {
            XMLStreamReader reader = newXmlReader(input);
            try {
                while (reader.hasNext()) {
                    if (reader.next() != XMLStreamConstants.START_ELEMENT) {
                        continue;
                    }
                    if ("workbookPr".equals(reader.getLocalName())) {
                        date1904 = parseBoolean(attribute(reader, "date1904"));
                    } else if ("sheet".equals(reader.getLocalName()) && relationshipId == null) {
                        relationshipId = attribute(reader, "id");
                    }
                }
            } finally {
                reader.close();
            }
        }
        return new WorkbookInfo(relationshipId, date1904);
    }

    private static String findWorksheetPath(ZipFile workbook, String relationshipId)
            throws IOException, XMLStreamException {
        if (relationshipId == null) {
            throw new IOException("No worksheet found in XLSX file");
        }

        ZipEntry entry = requiredEntry(workbook, WORKBOOK_RELS_PATH);
        try (InputStream input = workbook.getInputStream(entry)) {
            XMLStreamReader reader = newXmlReader(input);
            try {
                while (reader.hasNext()) {
                    if (reader.next() != XMLStreamConstants.START_ELEMENT
                            || !"Relationship".equals(reader.getLocalName())
                            || !relationshipId.equals(attribute(reader, "Id"))) {
                        continue;
                    }
                    String target = attribute(reader, "Target");
                    if (target == null) {
                        break;
                    }
                    String normalized = normalizeWorksheetPath(target);
                    requiredEntry(workbook, normalized);
                    return normalized;
                }
            } finally {
                reader.close();
            }
        }
        throw new IOException("First worksheet relationship is missing from XLSX file");
    }

    private static String normalizeWorksheetPath(String target) throws IOException {
        String unixTarget = target.replace('\\', '/');
        if (unixTarget.startsWith("/")) {
            unixTarget = unixTarget.substring(1);
        }
        Path normalized = unixTarget.startsWith("xl/")
                ? Paths.get(unixTarget).normalize()
                : Paths.get("xl").resolve(unixTarget).normalize();
        String result = normalized.toString().replace('\\', '/');
        if (!result.startsWith("xl/") || result.contains("../")) {
            throw new IOException("Invalid worksheet path in XLSX file");
        }
        return result;
    }

    private static List<String> readSharedStrings(ZipFile workbook)
            throws IOException, XMLStreamException {
        List<String> strings = new ArrayList<>();
        ZipEntry entry = workbook.getEntry(SHARED_STRINGS_PATH);
        if (entry == null) {
            return strings;
        }

        try (InputStream input = workbook.getInputStream(entry)) {
            XMLStreamReader reader = newXmlReader(input);
            StringBuilder current = null;
            try {
                while (reader.hasNext()) {
                    int event = reader.next();
                    if (event == XMLStreamConstants.START_ELEMENT) {
                        if ("si".equals(reader.getLocalName())) {
                            current = new StringBuilder();
                        } else if ("t".equals(reader.getLocalName()) && current != null) {
                            current.append(reader.getElementText());
                        }
                    } else if (event == XMLStreamConstants.END_ELEMENT
                            && "si".equals(reader.getLocalName()) && current != null) {
                        strings.add(current.toString());
                        current = null;
                    }
                }
            } finally {
                reader.close();
            }
        }
        return strings;
    }

    private static List<Boolean> readPercentageStyles(ZipFile workbook)
            throws IOException, XMLStreamException {
        List<Boolean> styles = new ArrayList<>();
        ZipEntry entry = workbook.getEntry(STYLES_PATH);
        if (entry == null) {
            return styles;
        }

        Map<Integer, Boolean> customFormats = new HashMap<>();
        boolean insideCellFormats = false;
        try (InputStream input = workbook.getInputStream(entry)) {
            XMLStreamReader reader = newXmlReader(input);
            try {
                while (reader.hasNext()) {
                    int event = reader.next();
                    if (event == XMLStreamConstants.START_ELEMENT) {
                        String name = reader.getLocalName();
                        if ("numFmt".equals(name)) {
                            int id = parseInt(attribute(reader, "numFmtId"), -1);
                            String format = attribute(reader, "formatCode");
                            customFormats.put(id, format != null && format.contains("%"));
                        } else if ("cellXfs".equals(name)) {
                            insideCellFormats = true;
                        } else if (insideCellFormats && "xf".equals(name)) {
                            int id = parseInt(attribute(reader, "numFmtId"), 0);
                            styles.add(id == 9 || id == 10 || Boolean.TRUE.equals(customFormats.get(id)));
                        }
                    } else if (event == XMLStreamConstants.END_ELEMENT
                            && "cellXfs".equals(reader.getLocalName())) {
                        insideCellFormats = false;
                    }
                }
            } finally {
                reader.close();
            }
        }
        return styles;
    }

    private static List<TaskRow> readWorksheet(ZipFile workbook, String worksheetPath,
            List<String> sharedStrings, List<Boolean> percentageStyles, boolean date1904)
            throws IOException, XMLStreamException {
        List<TaskRow> rows = new ArrayList<>();
        ZipEntry entry = requiredEntry(workbook, worksheetPath);
        try (InputStream input = workbook.getInputStream(entry)) {
            XMLStreamReader reader = newXmlReader(input);
            WorksheetRow currentRow = null;
            CellData currentCell = null;
            int nextColumn = 0;
            try {
                while (reader.hasNext()) {
                    int event = reader.next();
                    if (event == XMLStreamConstants.START_ELEMENT) {
                        String name = reader.getLocalName();
                        if ("row".equals(name)) {
                            currentRow = new WorksheetRow(parseInt(attribute(reader, "r"), rows.size() + 1));
                            nextColumn = 0;
                        } else if ("c".equals(name) && currentRow != null) {
                            String reference = attribute(reader, "r");
                            int column = reference == null ? nextColumn : columnIndex(reference);
                            currentCell = new CellData(column, attribute(reader, "t"),
                                    parseInt(attribute(reader, "s"), -1));
                            nextColumn = column + 1;
                        } else if (currentCell != null && ("v".equals(name) || "t".equals(name))) {
                            String value = reader.getElementText();
                            currentCell.value = "t".equals(name) && currentCell.value != null
                                    ? currentCell.value + value : value;
                        }
                    } else if (event == XMLStreamConstants.END_ELEMENT) {
                        String name = reader.getLocalName();
                        if ("c".equals(name) && currentRow != null && currentCell != null) {
                            currentRow.set(currentCell.column,
                                    resolveCellValue(currentCell, sharedStrings));
                            currentRow.setStyle(currentCell.column, currentCell.styleIndex);
                            currentCell = null;
                        } else if ("row".equals(name) && currentRow != null) {
                            String taskName = trimToEmpty(currentRow.values[0]);
                            if (!taskName.isEmpty() && !(rows.isEmpty() && isHeader(taskName))) {
                                rows.add(toTaskRow(currentRow, percentageStyles, date1904));
                            }
                            currentRow = null;
                        }
                    }
                }
            } finally {
                reader.close();
            }
        }
        return rows;
    }

    private static TaskRow toTaskRow(WorksheetRow row, List<Boolean> percentageStyles,
            boolean date1904) throws IOException {
        String name = trimToEmpty(row.values[0]);
        if (name.isEmpty()) {
            return null;
        }

        Date start = parseDate(row.values[1], date1904);
        Date end = parseDate(row.values[2], date1904);
        double progress = parseProgress(row.values[3], isPercentageStyle(row.styles[3], percentageStyles));
        return new TaskRow(row.rowNumber, name, start, end, progress);
    }

    private static Date parseDate(String value, boolean date1904) throws IOException {
        String text = trimToEmpty(value);
        if (text.isEmpty()) {
            return null;
        }
        try {
            double serial = Double.parseDouble(text);
            if (!Double.isFinite(serial) || serial < 0) {
                throw new IOException("Invalid Excel date value: " + text);
            }
            long wholeDays = (long)Math.floor(serial);
            long millis = Math.round((serial - wholeDays) * MILLIS_PER_DAY);
            LocalDateTime base = date1904
                    ? LocalDateTime.of(1904, 1, 1, 0, 0)
                    : LocalDateTime.of(1899, 12, 30, 0, 0);
            return Date.from(base.plusDays(wholeDays).plusNanos(millis * 1_000_000L)
                    .atZone(ZoneId.systemDefault()).toInstant());
        } catch (NumberFormatException ignored) {
            String[] formats = {"yyyy-MM-dd", "MM/dd/yyyy", "yyyy/MM/dd"};
            for (String format : formats) {
                SimpleDateFormat parser = new SimpleDateFormat(format, Locale.ROOT);
                parser.setLenient(false);
                try {
                    return parser.parse(text);
                } catch (ParseException ignoredFormat) {
                    // Try the next supported representation.
                }
            }
            throw new IOException("Unsupported date value: " + text);
        }
    }

    private static double parseProgress(String value, boolean percentageStyle) throws IOException {
        String text = trimToEmpty(value);
        if (text.isEmpty()) {
            return 0.0d;
        }
        boolean percentageText = text.endsWith("%");
        if (percentageText) {
            text = text.substring(0, text.length() - 1).trim();
        }
        try {
            double progress = Double.parseDouble(text);
            if (!Double.isFinite(progress)) {
                throw new NumberFormatException();
            }
            if (percentageStyle && !percentageText) {
                progress *= 100.0d;
            }
            return Math.max(0.0d, Math.min(100.0d, progress));
        } catch (NumberFormatException e) {
            throw new IOException("Invalid progress value: " + value, e);
        }
    }

    private static String resolveCellValue(CellData cell, List<String> sharedStrings)
            throws IOException {
        String value = cell.value == null ? "" : cell.value;
        if (!"s".equals(cell.type)) {
            return value;
        }
        int index = parseInt(value, -1);
        if (index < 0 || index >= sharedStrings.size()) {
            throw new IOException("Invalid shared string index in XLSX file");
        }
        return sharedStrings.get(index);
    }

    private static boolean isHeader(String value) {
        String header = trimToEmpty(value).toLowerCase(Locale.ROOT)
                .replace("_", " ").replace("-", " ");
        return "task".equals(header) || "task name".equals(header) || "name".equals(header)
                || "タスク".equals(header) || "タスク名".equals(header);
    }

    private static boolean isPercentageStyle(int styleIndex, List<Boolean> styles) {
        return styleIndex >= 0 && styleIndex < styles.size() && styles.get(styleIndex);
    }

    private static int columnIndex(String reference) {
        int result = 0;
        int length = 0;
        while (length < reference.length() && Character.isLetter(reference.charAt(length))) {
            result = result * 26 + Character.toUpperCase(reference.charAt(length)) - 'A' + 1;
            length++;
        }
        return result - 1;
    }

    private static XMLStreamReader newXmlReader(InputStream input) throws XMLStreamException {
        XMLInputFactory factory = XMLInputFactory.newFactory();
        factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
        factory.setProperty("javax.xml.stream.isSupportingExternalEntities", false);
        return factory.createXMLStreamReader(input);
    }

    private static ZipEntry requiredEntry(ZipFile workbook, String path) throws IOException {
        ZipEntry entry = workbook.getEntry(path);
        if (entry == null) {
            throw new IOException("Missing XLSX part: " + path);
        }
        return entry;
    }

    private static String attribute(XMLStreamReader reader, String localName) {
        for (int i = 0; i < reader.getAttributeCount(); i++) {
            if (localName.equals(reader.getAttributeLocalName(i))) {
                return reader.getAttributeValue(i);
            }
        }
        return null;
    }

    private static int parseInt(String value, int defaultValue) {
        if (value == null || value.isEmpty()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private static boolean parseBoolean(String value) {
        return "1".equals(value) || "true".equalsIgnoreCase(value);
    }

    private static String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    private static final class WorkbookInfo {
        private final String relationshipId;
        private final boolean date1904;

        private WorkbookInfo(String relationshipId, boolean date1904) {
            this.relationshipId = relationshipId;
            this.date1904 = date1904;
        }
    }

    private static final class WorksheetRow {
        private final int rowNumber;
        private final String[] values = new String[4];
        private final int[] styles = {-1, -1, -1, -1};

        private WorksheetRow(int rowNumber) {
            this.rowNumber = rowNumber;
        }

        private void set(int column, String value) {
            if (column >= 0 && column < values.length) {
                values[column] = value;
            }
        }

        private void setStyle(int column, int style) {
            if (column >= 0 && column < styles.length) {
                styles[column] = style;
            }
        }
    }

    private static final class CellData {
        private final int column;
        private final String type;
        private final int styleIndex;
        private String value;

        private CellData(int column, String type, int styleIndex) {
            this.column = column;
            this.type = type;
            this.styleIndex = styleIndex;
        }
    }

    static final class TaskRow {
        private final int rowNumber;
        private final String name;
        private final Date start;
        private final Date end;
        private final double progressPercent;

        private TaskRow(int rowNumber, String name, Date start, Date end, double progressPercent) {
            this.rowNumber = rowNumber;
            this.name = name;
            this.start = start;
            this.end = end;
            this.progressPercent = progressPercent;
        }

        int getRowNumber() {
            return rowNumber;
        }

        String getName() {
            return name;
        }

        Date getStart() {
            return start;
        }

        Date getEnd() {
            return end;
        }

        double getProgressPercent() {
            return progressPercent;
        }
    }
}
