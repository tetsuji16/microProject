package net.sf.mpxj.projectlibre;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;

final class XlsxPayloadUtils {
	private XlsxPayloadUtils() {
	}

	static String encodeObject(Object value) throws Exception {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		ObjectOutputStream objectOut = new ObjectOutputStream(out);
		try {
			objectOut.writeObject(value);
		} finally {
			objectOut.close();
		}
		return Base64.getEncoder().encodeToString(out.toByteArray());
	}

	static Object decodeObject(String value) throws Exception {
		byte[] bytes = Base64.getDecoder().decode(value.getBytes(StandardCharsets.UTF_8));
		ObjectInputStream objectIn = new ObjectInputStream(new ByteArrayInputStream(bytes));
		try {
			return objectIn.readObject();
		} finally {
			objectIn.close();
		}
	}

	static void writeChunkedTextSheet(Workbook workbook, String sheetName, String header, String text, int chunkSize) {
		Sheet sheet = workbook.createSheet(sheetName);
		sheet.createRow(0).createCell(0).setCellValue(header);
		if (text == null) {
			return;
		}
		int rowIndex = 1;
		for (int offset = 0; offset < text.length(); offset += chunkSize) {
			int end = Math.min(offset + chunkSize, text.length());
			Row row = sheet.createRow(rowIndex++);
			row.createCell(0).setCellValue(text.substring(offset, end));
		}
	}

	static String readChunkedTextSheet(Sheet sheet) {
		if (sheet == null) {
			return null;
		}
		StringBuilder result = new StringBuilder();
		for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
			Row row = sheet.getRow(rowIndex);
			if (row == null || row.getCell(0) == null) {
				continue;
			}
			result.append(row.getCell(0).getStringCellValue());
		}
		return result.length() == 0 ? null : result.toString();
	}
}
