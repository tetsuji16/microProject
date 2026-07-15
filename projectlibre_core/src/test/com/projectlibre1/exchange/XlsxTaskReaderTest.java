package com.projectlibre1.exchange;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public final class XlsxTaskReaderTest {
    private XlsxTaskReaderTest() {
    }

    public static void main(String[] args) throws Exception {
        Path workbook = Files.createTempFile("projectlibre-xlsx-reader-", ".xlsx");
        try {
            createWorkbook(workbook);
            List<XlsxTaskReader.TaskRow> rows = XlsxTaskReader.read(workbook.toFile());
            assertEquals(2, rows.size(), "task row count");

            XlsxTaskReader.TaskRow first = rows.get(0);
            assertEquals("Foundation", first.getName(), "shared-string task name");
            assertEquals("2024-01-01", format(first.getStart()), "numeric start date");
            assertEquals("2024-01-05", format(first.getEnd()), "inline-string end date");
            assertEquals(50.0d, first.getProgressPercent(), "percentage-formatted progress");

            XlsxTaskReader.TaskRow second = rows.get(1);
            assertEquals("Inspection", second.getName(), "inline-string task name");
            assertEquals("2024-02-01", format(second.getStart()), "text start date");
            assertEquals(100.0d, second.getProgressPercent(), "progress clamping");
        } finally {
            Files.deleteIfExists(workbook);
        }
    }

    private static void createWorkbook(Path path) throws IOException {
        try (ZipOutputStream output = new ZipOutputStream(Files.newOutputStream(path))) {
            add(output, "xl/workbook.xml", """
                    <?xml version="1.0" encoding="UTF-8"?>
                    <workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"
                              xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
                      <sheets><sheet name="Tasks" sheetId="1" r:id="rId7"/></sheets>
                    </workbook>
                    """);
            add(output, "xl/_rels/workbook.xml.rels", """
                    <?xml version="1.0" encoding="UTF-8"?>
                    <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
                      <Relationship Id="rId7" Type="worksheet" Target="worksheets/sheet2.xml"/>
                    </Relationships>
                    """);
            add(output, "xl/sharedStrings.xml", """
                    <?xml version="1.0" encoding="UTF-8"?>
                    <sst xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
                      <si><t>Task Name</t></si>
                      <si><r><t>Found</t></r><r><t>ation</t></r></si>
                    </sst>
                    """);
            add(output, "xl/styles.xml", """
                    <?xml version="1.0" encoding="UTF-8"?>
                    <styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
                      <cellXfs count="2"><xf numFmtId="0"/><xf numFmtId="10"/></cellXfs>
                    </styleSheet>
                    """);
            add(output, "xl/worksheets/sheet2.xml", """
                    <?xml version="1.0" encoding="UTF-8"?>
                    <worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
                      <sheetData>
                        <row r="1"><c r="A1" t="s"><v>0</v></c></row>
                        <row r="2">
                          <c r="A2" t="s"><v>1</v></c><c r="B2"><v>45292</v></c>
                          <c r="C2" t="inlineStr"><is><t>2024-01-05</t></is></c>
                          <c r="D2" s="1"><v>0.5</v></c>
                        </row>
                        <row r="3">
                          <c r="A3" t="inlineStr"><is><r><t>Inspec</t></r><r><t>tion</t></r></is></c>
                          <c r="B3" t="inlineStr"><is><t>02/01/2024</t></is></c>
                          <c r="D3"><v>125</v></c>
                        </row>
                      </sheetData>
                    </worksheet>
                    """);
        }
    }

    private static void add(ZipOutputStream output, String name, String contents) throws IOException {
        output.putNextEntry(new ZipEntry(name));
        output.write(contents.getBytes(StandardCharsets.UTF_8));
        output.closeEntry();
    }

    private static String format(java.util.Date value) {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).format(value);
    }

    private static void assertEquals(Object expected, Object actual, String label) {
        if (!expected.equals(actual)) {
            throw new AssertionError(label + ": expected " + expected + ", got " + actual);
        }
    }

    private static void assertEquals(double expected, double actual, String label) {
        if (Double.compare(expected, actual) != 0) {
            throw new AssertionError(label + ": expected " + expected + ", got " + actual);
        }
    }
}
