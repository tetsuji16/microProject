package com.projectlibre1.pm.graphic.timescale;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

import javax.swing.SwingUtilities;

import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;

import com.projectlibre1.graphic.configuration.GraphicConfiguration;
import com.projectlibre1.pm.graphic.fx.FxLog;
import com.projectlibre1.timescale.TimeInterval;
import com.projectlibre1.timescale.TimeIterator;

/**
 * JavaFX replacement for the old Swing time scale header.
 */
public class FxTimeScalePane extends JFXPanel {
	private static final long serialVersionUID = 1L;
	private static final Logger LOGGER = FxLog.logger(FxTimeScalePane.class);
	private final CoordinatesConverter coord;
	private final Canvas canvas = new Canvas();
	private final AtomicBoolean redrawQueued = new AtomicBoolean(false);
	private final BufferedImage metricsImage = new BufferedImage(8, 8, BufferedImage.TYPE_INT_ARGB);
	private final Font headerFont = new Font("Dialog", Font.PLAIN, 11);
	private final FontMetrics fontMetrics;

	public FxTimeScalePane(CoordinatesConverter coord) {
		this.coord = coord;
		Graphics2D g2 = metricsImage.createGraphics();
		fontMetrics = g2.getFontMetrics(headerFont);
		g2.dispose();
		Platform.setImplicitExit(false);
		Platform.runLater(() -> {
			StackPane root = new StackPane(canvas);
			root.setStyle("-fx-background-color: white;");
			setScene(new Scene(root));
			syncCanvasSize();
			requestRedraw();
		});
	}

	public void setCoord(CoordinatesConverter coord) {
		requestRedraw();
	}

	public void requestRedraw() {
		if (!SwingUtilities.isEventDispatchThread()) {
			SwingUtilities.invokeLater(this::requestRedraw);
			return;
		}
		if (redrawQueued.getAndSet(true)) {
			return;
		}
		Platform.runLater(() -> {
			redrawQueued.set(false);
			syncCanvasSize();
			draw();
		});
	}

	private void syncCanvasSize() {
		int headerHeight = GraphicConfiguration.getInstance().getColumnHeaderHeight();
		double width = Math.max(1.0d, getWidth());
		double height = Math.max(headerHeight, getHeight());
		canvas.setWidth(width);
		canvas.setHeight(height);
		setPreferredSize(new Dimension((int) Math.ceil(width), headerHeight));
		setSize((int) Math.ceil(width), headerHeight);
	}

	private void draw() {
		GraphicConfiguration config = GraphicConfiguration.getInstance();
		int headerHeight = config.getColumnHeaderHeight();
		LOGGER.fine("draw width=" + canvas.getWidth() + " height=" + canvas.getHeight());
		GraphicsContext gc = canvas.getGraphicsContext2D();
		gc.setFill(Color.WHITE);
		gc.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
		if (coord == null) {
			return;
		}

		double width = canvas.getWidth();
		double height = Math.max(1.0d, headerHeight);
		gc.setStroke(Color.BLACK);
		gc.setLineWidth(1.0d);
		gc.strokeLine(0, height - 1, width, height - 1);
		gc.strokeLine(0, height / 2.0d, width, height / 2.0d);

		TimeIterator iterator = coord.getTimeIterator(0, width);
		javafx.scene.text.Font fxFont = javafx.scene.text.Font.font("System", 11.0d);
		gc.setFont(fxFont);
		while (iterator.hasNext()) {
			TimeInterval interval = iterator.next();
			double x1 = coord.toX(interval.getStart1());
			double x2 = coord.toX(interval.getEnd1());
			gc.strokeLine(x1, height / 2.0d, x1, height);
			gc.strokeLine(x2, height / 2.0d, x2, height);
			String text1 = interval.getText1();
			if (text1 != null) {
				gc.setFill(Color.BLACK);
				gc.fillText(text1, x1 + 2.0d, height - fontMetrics.getDescent() - fontMetrics.getLeading());
			}
			if (interval.getText2() != null) {
				double X1 = coord.toX(interval.getStart2());
				double X2 = coord.toX(interval.getEnd2());
				gc.strokeLine(X1, 0, X1, height / 2.0d);
				gc.strokeLine(X2, 0, X2, height / 2.0d);
				if (X1 + 2.0d >= 0) {
					gc.setFill(Color.BLACK);
					gc.fillText(interval.getText2(), X1 + 2.0d, height / 2.0d - fontMetrics.getDescent() - fontMetrics.getLeading());
				}
			}
		}
	}
}
