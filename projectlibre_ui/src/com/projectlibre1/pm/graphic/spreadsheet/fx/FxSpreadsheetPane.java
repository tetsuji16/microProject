package com.projectlibre1.pm.graphic.spreadsheet.fx;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.Insets;
import java.awt.Point;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

import javafx.application.Platform;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.embed.swing.JFXPanel;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TablePosition;
import javafx.scene.control.TableView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;
import javafx.util.Callback;

import com.projectlibre1.configuration.Configuration;
import com.projectlibre1.graphic.configuration.CellFormat;
import com.projectlibre1.graphic.configuration.GraphicConfiguration;
import com.projectlibre1.grouping.core.Node;
import com.projectlibre1.graphic.configuration.SpreadSheetFieldArray;
import com.projectlibre1.field.Field;
import com.projectlibre1.field.FieldContext;
import com.projectlibre1.graphic.configuration.shape.Colors;
import com.projectlibre1.pm.graphic.model.cache.GraphicNode;
import com.projectlibre1.pm.graphic.fx.FxLog;
import com.projectlibre1.pm.graphic.spreadsheet.renderer.NameCellComponent;
import com.projectlibre1.pm.graphic.spreadsheet.SpreadSheet;
import com.projectlibre1.pm.graphic.spreadsheet.SpreadSheetColumnModel;
import com.projectlibre1.pm.graphic.spreadsheet.SpreadSheetModel;
import com.projectlibre1.pm.graphic.spreadsheet.common.CommonSpreadSheetModel;
import com.projectlibre1.util.BrowserControl;
import com.projectlibre1.strings.Messages;
import com.projectlibre1.menu.MenuActionConstants;

/**
 * JavaFX table surface for the spreadsheet portion of a split view.
 *
 * The Swing {@link SpreadSheet} is kept as the model/action bridge, while this
 * class renders the visible table header and body using JavaFX.
 */
public class FxSpreadsheetPane {
	private static final Logger LOGGER = FxLog.logger(FxSpreadsheetPane.class);
	private final SpreadSheet source;
	private final JScrollPane scrollPane = new JScrollPane();
	private final JFXPanel headerPanel = new JFXPanel();
	private final JFXPanel bodyPanel = new JFXPanel();
	private final AtomicBoolean refreshQueued = new AtomicBoolean(false);
	private final AtomicBoolean sceneQueued = new AtomicBoolean(false);
	private final TableModelListener modelListener = new TableModelListener() {
		public void tableChanged(TableModelEvent e) {
			requestRefresh();
		}
	};

	private volatile ArrayList fieldArray;
	private volatile int rowHeight = GraphicConfiguration.getInstance().getRowHeight();
	private volatile boolean readOnly;
	private volatile int contentWidth = 1;
	private volatile int contentHeight = 1;
	private volatile int headerHeight = GraphicConfiguration.getInstance().getColumnHeaderHeight();

	private TableView<Integer> tableView;
	private HBox headerBox;

	public FxSpreadsheetPane(SpreadSheet source) {
		this.source = source;
		Platform.setImplicitExit(false);
		scrollPane.setBorder(null);
		scrollPane.setViewportView(bodyPanel);
		scrollPane.setColumnHeaderView(headerPanel);
		attachSourceModel();
		Platform.runLater(new Runnable() {
			public void run() {
				ensureScenes();
				requestRefresh();
			}
		});
	}

	public JScrollPane getScrollPane() {
		return scrollPane;
	}

	public void attachSourceModel() {
		if (!(source.getModel() instanceof CommonSpreadSheetModel)) {
			return;
		}
		CommonSpreadSheetModel model = (CommonSpreadSheetModel) source.getModel();
		model.removeTableModelListener(modelListener);
		model.addTableModelListener(modelListener);
		requestRefresh();
	}

	public void setFieldArray(ArrayList fieldArray) {
		this.fieldArray = fieldArray;
		requestRefresh();
	}

	public void setRowHeight(int rowHeight) {
		this.rowHeight = rowHeight;
		requestRefresh();
	}

	public void setReadOnly(boolean readOnly) {
		this.readOnly = readOnly;
		requestRefresh();
	}

	public Dimension getContentSize() {
		return new Dimension(contentWidth, contentHeight);
	}

	public void cleanUp() {
		if (source.getModel() instanceof CommonSpreadSheetModel) {
			((CommonSpreadSheetModel) source.getModel()).removeTableModelListener(modelListener);
		}
		Platform.runLater(new Runnable() {
			public void run() {
				bodyPanel.setScene(null);
				headerPanel.setScene(null);
			}
		});
	}

	private void ensureScenes() {
		if (sceneQueued.getAndSet(true)) {
			return;
		}
		bodyPanel.setScene(createBodyScene());
		headerPanel.setScene(createHeaderScene());
		sceneQueued.set(false);
	}

	private Scene createBodyScene() {
		tableView = new TableView<Integer>();
		tableView.setEditable(!readOnly);
		tableView.getSelectionModel().setCellSelectionEnabled(true);
		tableView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
		tableView.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
		tableView.setFixedCellSize(rowHeight);
		tableView.setPlaceholder(new Label(""));
		tableView.setPrefHeight(1);
		tableView.setStyle("-fx-background-color: white; -fx-focus-color: transparent; -fx-faint-focus-color: transparent;");
		tableView.addEventFilter(KeyEvent.KEY_PRESSED, this::handleKeyPressed);
		tableView.addEventFilter(MouseEvent.MOUSE_CLICKED, this::handleMouseClicked);
		tableView.setRowFactory(new Callback<TableView<Integer>, javafx.scene.control.TableRow<Integer>>() {
			public javafx.scene.control.TableRow<Integer> call(TableView<Integer> tv) {
				return new javafx.scene.control.TableRow<Integer>() {
					{
						setOnMousePressed(event -> handleRowMousePressed(this, event));
						setOnMouseClicked(event -> handleRowMouseClicked(this, event));
						selectedProperty().addListener((obs, oldValue, newValue) -> {
							Integer rowIndex = getItem();
							if (rowIndex != null && rowIndex.intValue() >= 0) {
								setStyle(resolveRowStyle(rowIndex.intValue(), newValue.booleanValue()));
							}
						});
					}

					@Override
					protected void updateItem(Integer rowIndex, boolean empty) {
						super.updateItem(rowIndex, empty);
						if (empty || rowIndex == null || rowIndex.intValue() < 0) {
							setStyle("");
							return;
						}
						setStyle(resolveRowStyle(rowIndex.intValue(), isSelected()));
					}
				};
			}
		});
		tableView.getSelectionModel().getSelectedCells().addListener((javafx.collections.ListChangeListener.Change<? extends TablePosition> change) -> syncSelectionToSource());
		StackPane root = new StackPane(tableView);
		root.setStyle("-fx-background-color: white;");
		Scene scene = new Scene(root);
		hideTableHeaderLater();
		return scene;
	}

	private Scene createHeaderScene() {
		headerBox = new HBox();
		headerBox.setAlignment(Pos.CENTER_LEFT);
		headerBox.setStyle("-fx-background-color: linear-gradient(to bottom, #f9f9f9, #e2e2e2);");
		StackPane root = new StackPane(headerBox);
		root.setStyle("-fx-background-color: linear-gradient(to bottom, #f9f9f9, #e2e2e2);");
		Scene scene = new Scene(root);
		return scene;
	}

	private void hideTableHeaderLater() {
		Platform.runLater(new Runnable() {
			public void run() {
				if (tableView == null) {
					return;
				}
				javafx.scene.Node header = tableView.lookup(".column-header-background");
				if (header != null) {
					header.setStyle("-fx-max-height: 0; -fx-pref-height: 0; -fx-min-height: 0; -fx-padding: 0;");
				}
				javafx.scene.Node filler = tableView.lookup(".filler");
				if (filler != null) {
					filler.setStyle("-fx-max-height: 0; -fx-pref-height: 0; -fx-min-height: 0; -fx-padding: 0;");
				}
			}
		});
	}

	public void refresh() {
		requestRefresh();
	}

	private void requestRefresh() {
		if (refreshQueued.getAndSet(true)) {
			return;
		}
		Platform.runLater(new Runnable() {
			public void run() {
				refreshQueued.set(false);
				refreshFx();
			}
		});
	}

	private void refreshFx() {
		ensureScenes();
		if (!(source.getModel() instanceof SpreadSheetModel)) {
			return;
		}
		SpreadSheetModel model = (SpreadSheetModel) source.getModel();
		SpreadSheetColumnModel columnModel = (SpreadSheetColumnModel) source.getColumnModel();
		if (columnModel == null) {
			return;
		}
		int selectedRow = -1;
		int selectedColumn = -1;
		if (tableView != null && tableView.getSelectionModel() != null) {
			List<TablePosition> selected = tableView.getSelectionModel().getSelectedCells();
			if (selected != null && !selected.isEmpty()) {
				selectedRow = selected.get(0).getRow();
				selectedColumn = selected.get(0).getColumn();
			}
		}
		ObservableList<Integer> rows = FXCollections.observableArrayList();
		int rowCount = model.getRowCount();
		LOGGER.fine("refreshFx rowCount=" + rowCount + " columns=" + columnModel.getColumnCount());
		for (int i = 0; i < rowCount; i++) {
			rows.add(Integer.valueOf(i));
		}
		tableView.getColumns().setAll(buildColumns(model, columnModel));
		tableView.setItems(rows);
		tableView.setEditable(!readOnly);
		tableView.setFixedCellSize(rowHeight);

		contentWidth = computeContentWidth(columnModel);
		contentHeight = Math.max(headerHeight + 1, headerHeight + rowCount * rowHeight);
		Dimension bodySize = new Dimension(contentWidth, Math.max(1, rowCount * rowHeight));
		Dimension headerSize = new Dimension(contentWidth, headerHeight);
		bodyPanel.setPreferredSize(bodySize);
		headerPanel.setPreferredSize(headerSize);
		updateHeaderLabels(model, columnModel);
		tableView.setPrefWidth(contentWidth);
		tableView.setMinWidth(contentWidth);
		tableView.setMaxWidth(contentWidth);
		tableView.setPrefHeight(bodySize.height);
		tableView.setMinHeight(bodySize.height);
		tableView.setMaxHeight(bodySize.height);
		tableView.refresh();
		bodyPanel.setSize(bodySize);
		headerPanel.setSize(headerSize);
		scrollPane.revalidate();
		scrollPane.repaint();
		if (selectedRow >= 0 && selectedRow < rowCount && selectedColumn >= 0 && selectedColumn < tableView.getColumns().size()) {
			tableView.getSelectionModel().clearAndSelect(selectedRow, tableView.getColumns().get(selectedColumn));
		}
		hideTableHeaderLater();
	}

	private List<TableColumn<Integer, String>> buildColumns(final SpreadSheetModel model, final SpreadSheetColumnModel columnModel) {
		List<TableColumn<Integer, String>> columns = new ArrayList<TableColumn<Integer, String>>();
		TableColumn<Integer, String> rowColumn = new TableColumn<Integer, String>("#");
		rowColumn.setUserData(Integer.valueOf(-1));
		rowColumn.setPrefWidth(42);
		rowColumn.setMinWidth(42);
		rowColumn.setMaxWidth(42);
		rowColumn.setResizable(false);
		rowColumn.setCellValueFactory(cellData -> new ReadOnlyStringWrapper(Integer.toString(cellData.getValue().intValue() + 1)));
		columns.add(rowColumn);

		for (int i = 0; i < columnModel.getColumnCount(); i++) {
			final int fieldColumn = i + 1;
			final Field field = model.getFieldInColumn(fieldColumn);
			final int prefWidth = Math.max(60, columnModel.getColumn(i).getPreferredWidth());
			TableColumn<Integer, String> column = new TableColumn<Integer, String>(field.getName());
			column.setUserData(Integer.valueOf(fieldColumn));
			column.setPrefWidth(prefWidth);
			column.setMinWidth(prefWidth);
			column.setMaxWidth(prefWidth);
			column.setCellValueFactory(cellData -> new ReadOnlyStringWrapper(formatCellText(model, fieldColumn, field, cellData.getValue().intValue())));
			column.setCellFactory(col -> new EditingCell(fieldColumn, field));
			column.setOnEditCommit(evt -> commitCellEdit(model, evt.getRowValue().intValue(), fieldColumn, evt.getNewValue()));
			column.setSortable(false);
			columns.add(column);
		}
		return columns;
	}

	private void updateHeaderLabels(SpreadSheetModel model, SpreadSheetColumnModel columnModel) {
		headerBox.getChildren().clear();
		addHeaderLabel("#", 42);
		for (int i = 0; i < columnModel.getColumnCount(); i++) {
			Field field = model.getFieldInColumn(i + 1);
			int width = Math.max(60, columnModel.getColumn(i).getPreferredWidth());
			addHeaderLabel(field.getName(), width);
		}
	}

	private void addHeaderLabel(String text, int width) {
		Label label = new Label(text);
		label.setAlignment(Pos.CENTER_LEFT);
		label.setMinWidth(width);
		label.setPrefWidth(width);
		label.setMaxWidth(width);
		label.setPrefHeight(headerHeight);
		label.setStyle("-fx-padding: 0 6 0 6; -fx-font-weight: bold; -fx-border-color: transparent #c7c7c7 transparent transparent; -fx-border-width: 0 1 0 0;");
		HBox.setHgrow(label, Priority.NEVER);
		headerBox.getChildren().add(label);
	}

	private int computeContentWidth(SpreadSheetColumnModel columnModel) {
		int width = 42;
		for (int i = 0; i < columnModel.getColumnCount(); i++) {
			width += Math.max(60, columnModel.getColumn(i).getPreferredWidth());
		}
		return width;
	}

	private String formatCellText(SpreadSheetModel model, int fieldColumn, Field field, int row) {
		GraphicNode gnode = model.getNode(row);
		if (gnode == null) {
			return "";
		}
		Node node = gnode.getNode();
		FieldContext context = model.getFieldContext();
		String text = field.getText(node, model.getCache().getWalkersModel(), context);
		return text == null ? "" : text;
	}

	private void commitCellEdit(SpreadSheetModel model, int row, int fieldColumn, String newValue) {
		if (readOnly) {
			return;
		}
		if (fieldColumn <= 0) {
			return;
		}
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				try {
					model.setValueAt(newValue, row, fieldColumn);
				} finally {
					requestRefresh();
				}
			}
		});
	}

	private String resolveRowStyle(int row, boolean selected) {
		if (!(source.getModel() instanceof CommonSpreadSheetModel)) {
			return "";
		}
		CommonSpreadSheetModel model = (CommonSpreadSheetModel) source.getModel();
		GraphicNode node = model.getNode(row);
		if (node == null) {
			return "";
		}
		CellFormat format = model.getCellProperties(node);
		StringBuilder style = new StringBuilder("-fx-padding: 0 6 0 6;");
		if (selected) {
			style.append(" -fx-background-color: -fx-selection-bar;");
			style.append(" -fx-text-fill: -fx-selection-bar-text;");
		}
		if (format != null) {
			if (format.isBold()) {
				style.append(" -fx-font-weight: bold;");
			}
			if (format.isItalic()) {
				style.append(" -fx-font-style: italic;");
			}
			if (format.getBackgroundObject() != null && !selected) {
				style.append(" -fx-background-color: ").append(toFxColor(format.getBackgroundObject())).append(";");
			}
			if (format.getForegroundObject() != null && !selected) {
				style.append(" -fx-text-fill: ").append(toFxColor(format.getForegroundObject())).append(";");
			}
		}
		return style.toString();
	}

	private String toFxColor(java.awt.Color color) {
		return String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
	}

	private void syncSelectionToSource() {
		if (tableView == null) {
			return;
		}
		List<TablePosition> selected = tableView.getSelectionModel().getSelectedCells();
		if (selected == null || selected.isEmpty()) {
			return;
		}
		final int row = selected.get(0).getRow();
		final int col = selected.get(0).getColumn();
		FxSpreadsheetActionBridge.select(source, row, Math.max(0, col), false, false);
	}

	private void handleKeyPressed(KeyEvent event) {
		if (event.isControlDown() && event.getCode() == KeyCode.C) {
			FxSpreadsheetActionBridge.copy(source);
			event.consume();
		} else if (event.isControlDown() && event.getCode() == KeyCode.X) {
			FxSpreadsheetActionBridge.cut(source);
			event.consume();
		} else if (event.isControlDown() && event.getCode() == KeyCode.V) {
			if (event.isShiftDown()) {
				FxSpreadsheetActionBridge.pasteInsert(source);
			} else {
				FxSpreadsheetActionBridge.pasteValues(source);
			}
			event.consume();
		} else if (event.getCode() == KeyCode.INSERT) {
			FxSpreadsheetActionBridge.newTask(source);
			event.consume();
		} else if (event.getCode() == KeyCode.DELETE) {
			FxSpreadsheetActionBridge.delete(source);
			event.consume();
		} else if (event.getCode() == KeyCode.F3 || (event.isControlDown() && event.getCode() == KeyCode.F)) {
			FxSpreadsheetActionBridge.find(source);
			event.consume();
		} else if (event.isControlDown() && event.getCode() == KeyCode.LEFT) {
			if (isNameColumnSelected()) {
				FxSpreadsheetActionBridge.toggleHierarchy(source, false);
				event.consume();
			}
		} else if (event.isControlDown() && event.getCode() == KeyCode.RIGHT) {
			if (isNameColumnSelected()) {
				FxSpreadsheetActionBridge.toggleHierarchy(source, true);
				event.consume();
			}
		} else if (event.getCode() == KeyCode.TAB) {
			if (isNameColumnSelected()) {
				FxSpreadsheetActionBridge.indentOrOutdent(source, event.isShiftDown());
				event.consume();
			}
		} else if (event.getCode() == KeyCode.ENTER) {
			openSelectedRow();
			event.consume();
		}
	}

	private void openSelectedRow() {
		if (tableView == null) {
			return;
		}
		List<TablePosition> selected = tableView.getSelectionModel().getSelectedCells();
		if (selected == null || selected.isEmpty()) {
			return;
		}
		final int row = selected.get(0).getRow();
		final int col = selected.get(0).getColumn();
		FxSpreadsheetActionBridge.openInformation(source, row, Math.max(0, col));
	}

	private void handleMouseClicked(MouseEvent event) {
		// row handlers perform the real work; this keeps the table focused.
		if (event.getButton() == MouseButton.PRIMARY) {
			tableView.requestFocus();
		}
	}

	private void handleRowMousePressed(javafx.scene.control.TableRow<Integer> row, MouseEvent event) {
		if (event.getButton() != MouseButton.PRIMARY || row == null || row.isEmpty() || tableView == null) {
			return;
		}
		int rowIndex = row.getIndex();
		int viewColumn = findViewColumnIndex(event.getX());
		if (viewColumn < 0) {
			return;
		}
		selectFxAndSource(rowIndex, viewColumn, event.isShiftDown(), event.isShortcutDown());
		if (event.getClickCount() == 1 && isNameIconClick(rowIndex, event.getX(), event.getY(), viewColumn)) {
			toggleHierarchyForRow(rowIndex);
			event.consume();
			return;
		}
		if (isHyperlinkCell(rowIndex, viewColumn) && event.getClickCount() == 1) {
			openHyperlink(rowIndex, viewColumn);
		}
	}

	private void handleRowMouseClicked(javafx.scene.control.TableRow<Integer> row, MouseEvent event) {
		if (event.getButton() != MouseButton.PRIMARY || row == null || row.isEmpty() || tableView == null) {
			return;
		}
		int rowIndex = row.getIndex();
		int viewColumn = findViewColumnIndex(event.getX());
		if (viewColumn < 0) {
			return;
		}
		if (event.getClickCount() == 2) {
			if (isNameIconClick(rowIndex, event.getX(), event.getY(), viewColumn)) {
				toggleHierarchyForRow(rowIndex);
				event.consume();
				return;
			}
			if (isEditableCell(rowIndex, viewColumn)) {
				tableView.edit(rowIndex, tableView.getColumns().get(viewColumn));
				event.consume();
				return;
			}
			FxSpreadsheetActionBridge.openInformation(source, rowIndex, viewColumn);
			event.consume();
		}
	}

	private void selectFxAndSource(int row, int viewColumn, boolean extend, boolean toggle) {
		if (row < 0 || viewColumn < 0) {
			return;
		}
		if (tableView != null && viewColumn < tableView.getColumns().size()) {
			tableView.getSelectionModel().clearAndSelect(row, tableView.getColumns().get(viewColumn));
		}
		FxSpreadsheetActionBridge.select(source, row, viewColumn, toggle, extend);
	}

	private void toggleHierarchyForRow(int rowIndex) {
		SpreadSheetModel model = getSpreadSheetModel();
		if (model == null) {
			return;
		}
		GraphicNode node = model.getNode(rowIndex);
		if (node == null) {
			return;
		}
		FxSpreadsheetActionBridge.toggleHierarchy(source, node.isCollapsed());
	}

	private boolean isEditableCell(int rowIndex, int viewColumn) {
		SpreadSheetModel model = getSpreadSheetModel();
		if (model == null) {
			return false;
		}
		int fieldColumn = getFieldColumn(viewColumn);
		return fieldColumn > 0 && model.isCellEditable(rowIndex, fieldColumn);
	}

	private boolean isHyperlinkCell(int rowIndex, int viewColumn) {
		SpreadSheetModel model = getSpreadSheetModel();
		if (model == null) {
			return false;
		}
		int fieldColumn = getFieldColumn(viewColumn);
		if (fieldColumn <= 0) {
			return false;
		}
		Field field = model.getFieldInColumn(fieldColumn);
		return field != null && field.isHyperlink() && rowIndex >= 0 && rowIndex < model.getRowCount();
	}

	private void openHyperlink(int rowIndex, int viewColumn) {
		SpreadSheetModel model = getSpreadSheetModel();
		if (model == null) {
			return;
		}
		int fieldColumn = getFieldColumn(viewColumn);
		if (fieldColumn <= 0) {
			return;
		}
		Object value = model.getValueAt(rowIndex, fieldColumn);
		if (value instanceof com.projectlibre1.datatype.Hyperlink) {
			com.projectlibre1.datatype.Hyperlink link = (com.projectlibre1.datatype.Hyperlink) value;
			if (link.getAddress() != null) {
				BrowserControl.displayURL(link.getAddress());
			}
		}
	}

	private boolean isNameIconClick(int rowIndex, double x, double y, int viewColumn) {
		SpreadSheetModel model = getSpreadSheetModel();
		if (model == null || !isNameFieldColumn(viewColumn)) {
			return false;
		}
		GraphicNode node = model.getNode(rowIndex);
		CellFormat format = node == null ? null : model.getCellProperties(node);
		if (node == null || format == null || !format.isCompositeIcon()) {
			return false;
		}
		int fieldColumn = getFieldColumn(viewColumn);
		if (fieldColumn <= 0) {
			return false;
		}
		double width = tableView.getColumns().get(viewColumn).getWidth();
		Point point = new Point((int) Math.round(x), (int) Math.round(y));
		java.awt.Dimension bounds = new java.awt.Dimension((int) Math.round(width), rowHeight);
		int level = model.getCache().getLevel(node);
		return NameCellComponent.isOnIcon(point, bounds, level);
	}

	private boolean isNameColumnSelected() {
		if (tableView == null) {
			return false;
		}
		List<TablePosition> selected = tableView.getSelectionModel().getSelectedCells();
		if (selected == null || selected.isEmpty()) {
			return false;
		}
		return isNameFieldColumn(selected.get(0).getColumn());
	}

	private boolean isNameFieldColumn(int viewColumn) {
		SpreadSheetModel model = getSpreadSheetModel();
		if (model == null || viewColumn < 0) {
			return false;
		}
		int fieldColumn = getFieldColumn(viewColumn);
		Field field = model.getFieldInColumn(fieldColumn);
		return field != null && field.isNameField();
	}

	private int getFieldColumn(int viewColumn) {
		if (tableView == null || viewColumn < 0 || viewColumn >= tableView.getColumns().size()) {
			return -1;
		}
		Object userData = tableView.getColumns().get(viewColumn).getUserData();
		if (userData instanceof Integer) {
			return ((Integer) userData).intValue();
		}
		return viewColumn;
	}

	private int findViewColumnIndex(double x) {
		if (tableView == null) {
			return -1;
		}
		double width = 0.0d;
		for (int i = 0; i < tableView.getColumns().size(); i++) {
			width += tableView.getColumns().get(i).getWidth();
			if (x < width) {
				return i;
			}
		}
		return tableView.getColumns().isEmpty() ? -1 : tableView.getColumns().size() - 1;
	}

	private SpreadSheetModel getSpreadSheetModel() {
		if (!(source.getModel() instanceof SpreadSheetModel)) {
			return null;
		}
		return (SpreadSheetModel) source.getModel();
	}

	private final class EditingCell extends TableCell<Integer, String> {
		private final int fieldColumn;
		private final Field field;
		private javafx.scene.control.TextField textField;

		EditingCell(int fieldColumn, Field field) {
			this.fieldColumn = fieldColumn;
			this.field = field;
		}

		@Override
		public void startEdit() {
			if (readOnly || tableView == null) {
				return;
			}
			int row = getIndex();
			if (!(source.getModel() instanceof SpreadSheetModel)) {
				return;
			}
			SpreadSheetModel model = (SpreadSheetModel) source.getModel();
			if (!model.isCellEditable(row, fieldColumn)) {
				return;
			}
			super.startEdit();
			if (textField == null) {
				createTextField();
			}
			setText(null);
			setGraphic(textField);
			textField.selectAll();
			textField.requestFocus();
		}

		@Override
		public void cancelEdit() {
			super.cancelEdit();
			setText(getItem());
			setGraphic(null);
		}

		@Override
		public void updateItem(String item, boolean empty) {
			super.updateItem(item, empty);
			if (empty) {
				setText(null);
				setGraphic(null);
			} else if (isEditing()) {
				if (textField != null) {
					textField.setText(item);
				}
				setText(null);
				setGraphic(textField);
			} else if (field.isNameField()) {
				setText(null);
				setGraphic(buildNameGraphic(getIndex(), item));
			} else {
				setText(item);
				setGraphic(null);
			}
		}

		private void createTextField() {
			textField = new javafx.scene.control.TextField(getItem());
			textField.setOnAction(e -> commitEdit(textField.getText()));
			textField.focusedProperty().addListener((obs, oldValue, newValue) -> {
				if (!newValue) {
					commitEdit(textField.getText());
				}
			});
		}

		@Override
		public void commitEdit(String newValue) {
			super.commitEdit(newValue);
			if (getTableRow() != null && getTableRow().getItem() != null) {
				commitCellEdit((SpreadSheetModel) source.getModel(), getTableRow().getItem().intValue(), fieldColumn, newValue);
			}
			setGraphic(null);
		}

		private javafx.scene.Node buildNameGraphic(int rowIndex, String item) {
			SpreadSheetModel model = getSpreadSheetModel();
			if (model == null || rowIndex < 0 || rowIndex >= model.getRowCount()) {
				return new Label(item);
			}
			GraphicNode node = model.getNode(rowIndex);
			if (node == null) {
				return new Label(item);
			}
			int level = Math.max(0, model.getCache().getLevel(node) - 1);
			Region indent = new Region();
			indent.setMinWidth(level * 12.0d);
			indent.setPrefWidth(level * 12.0d);
			indent.setMaxWidth(level * 12.0d);
			Label arrow = new Label(node.isSummary() ? (node.isCollapsed() ? "\u25B6" : "\u25BC") : "");
			arrow.setMinWidth(12);
			arrow.setPrefWidth(12);
			arrow.setMaxWidth(12);
			arrow.setStyle("-fx-padding: 0 1 0 0;");
			Label label = new Label(item);
			label.setStyle("-fx-padding: 0 0 0 2;");
			HBox box = new HBox(indent, arrow, label);
			box.setAlignment(Pos.CENTER_LEFT);
			return box;
		}
	}
}
