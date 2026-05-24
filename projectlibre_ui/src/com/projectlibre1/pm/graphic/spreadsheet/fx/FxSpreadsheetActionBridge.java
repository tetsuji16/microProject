package com.projectlibre1.pm.graphic.spreadsheet.fx;

import javax.swing.SwingUtilities;

import com.projectlibre1.menu.MenuActionConstants;
import com.projectlibre1.pm.graphic.frames.GraphicManager;
import com.projectlibre1.pm.graphic.spreadsheet.SpreadSheet;

/**
 * Swing action bridge used by the JavaFX spreadsheet surface.
 *
 * The FX table stays thin and forwards all real editing and hierarchy
 * operations back to the Swing implementation so the existing action model,
 * clipboard handling, and undo stack keep working.
 */
final class FxSpreadsheetActionBridge {
	private FxSpreadsheetActionBridge() {
	}

	static void select(final SpreadSheet source, final int row, final int col, final boolean toggle, final boolean extend) {
		runOnSwing(source, new Runnable() {
			public void run() {
				source.changeSelection(row, col, toggle, extend);
				if (source.getSelection() != null && source.getColumnCount() > 0) {
					source.getSelection().getColumnSelection().addSelectionInterval(0, source.getColumnCount() - 1);
				}
			}
		});
	}

	static void executeAction(final SpreadSheet source, final String actionId) {
		runOnSwing(source, new Runnable() {
			public void run() {
				source.finishCurrentOperations();
				if (actionId == null) {
					return;
				}
				source.executeAction(actionId);
			}
		});
	}

	static void copy(final SpreadSheet source) {
		executeAction(source, MenuActionConstants.ACTION_COPY);
	}

	static void cut(final SpreadSheet source) {
		executeAction(source, MenuActionConstants.ACTION_CUT);
	}

	static void pasteValues(final SpreadSheet source) {
		runOnSwing(source, new Runnable() {
			public void run() {
				source.finishCurrentOperations();
				source.pasteClipboardAsValues();
			}
		});
	}

	static void pasteInsert(final SpreadSheet source) {
		runOnSwing(source, new Runnable() {
			public void run() {
				source.finishCurrentOperations();
				source.insertClipboardContents();
			}
		});
	}

	static void newTask(final SpreadSheet source) {
		executeAction(source, MenuActionConstants.ACTION_NEW);
	}

	static void delete(final SpreadSheet source) {
		executeAction(source, MenuActionConstants.ACTION_DELETE);
	}

	static void expand(final SpreadSheet source) {
		executeAction(source, MenuActionConstants.ACTION_EXPAND);
	}

	static void collapse(final SpreadSheet source) {
		executeAction(source, MenuActionConstants.ACTION_COLLAPSE);
	}

	static void indentOrOutdent(final SpreadSheet source, final boolean outdent) {
		runOnSwing(source, new Runnable() {
			public void run() {
				source.finishCurrentOperations();
				source.executeNameCellTabAction(outdent);
			}
		});
	}

	static void toggleHierarchy(final SpreadSheet source, final boolean expand) {
		runOnSwing(source, new Runnable() {
			public void run() {
				source.finishCurrentOperations();
				source.executeNameCellCollapseExpand(expand);
			}
		});
	}

	static void find(final SpreadSheet source) {
		runOnSwing(source, new Runnable() {
			public void run() {
				GraphicManager.getInstance(source).doFind(source, null);
			}
		});
	}

	static void openInformation(final SpreadSheet source, final int row, final int col) {
		runOnSwing(source, new Runnable() {
			public void run() {
				source.finishCurrentOperations();
				source.doDoubleClick(row, col);
			}
		});
	}

	private static void runOnSwing(final SpreadSheet source, final Runnable runnable) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				try {
					runnable.run();
				} catch (Throwable ignore) {
					// The JavaFX view remains usable even if a Swing action fails.
				}
			}
		});
	}
}
