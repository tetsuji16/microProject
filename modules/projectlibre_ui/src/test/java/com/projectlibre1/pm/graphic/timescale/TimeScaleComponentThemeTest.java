package com.projectlibre1.pm.graphic.timescale;

import static org.junit.jupiter.api.Assertions.assertEquals;

import javax.swing.border.Border;

import org.junit.jupiter.api.Test;

import com.projectlibre.ui.theme.ProjectLibreTheme;
import com.projectlibre1.pm.graphic.gantt.GanttParamsImpl;
import com.projectlibre1.util.FlatUiSupport;

class TimeScaleComponentThemeTest {
	@Test
	void timeScaleHeaderUsesSharedHeaderPaletteAndFont() {
		ProjectLibreTheme.installLight();
		TimeScaleComponent component = new TimeScaleComponent(null);

		assertEquals(FlatUiSupport.spreadsheetHeaderBackground(), component.getBackground());
		assertEquals(FlatUiSupport.headerForeground(), component.getForeground());
		assertEquals(FlatUiSupport.ganttHeaderFont().getSize2D(), component.getFont().getSize2D());
		Border border = component.getBorder();
		assertEquals(FlatUiSupport.tableHeaderBorder().getClass(), border.getClass());
	}

	@Test
	void ganttParamsExposeReadableHeaderFont() {
		GanttParamsImpl params = new GanttParamsImpl();

		assertEquals(FlatUiSupport.ganttHeaderFont().getSize2D(), params.getColumnHeaderFont().getSize2D());
	}
}
