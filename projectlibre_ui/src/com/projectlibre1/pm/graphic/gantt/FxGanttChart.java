package com.projectlibre1.pm.graphic.gantt;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.MouseWheelEvent;
import java.awt.geom.GeneralPath;
import java.awt.geom.PathIterator;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.SwingUtilities;

import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Paint;
import javafx.scene.text.FontWeight;

import com.projectlibre1.configuration.Configuration;
import com.projectlibre1.field.Field;
import com.projectlibre1.functor.IntervalConsumer;
import com.projectlibre1.functor.ScheduleIntervalGenerator;
import org.apache.commons.collections.Closure;
import com.projectlibre1.graphic.configuration.BarFormat;
import com.projectlibre1.graphic.configuration.BarStyles;
import com.projectlibre1.graphic.configuration.GraphicConfiguration;
import com.projectlibre1.pm.calendar.CalendarService;
import com.projectlibre1.pm.calendar.WorkingCalendar;
import com.projectlibre1.pm.dependency.Dependency;
import com.projectlibre1.pm.dependency.DependencyType;
import com.projectlibre1.pm.graphic.gantt.link_routing.GanttLinkRouting;
import com.projectlibre1.pm.graphic.graph.GraphInteractor;
import com.projectlibre1.pm.graphic.graph.GraphUI;
import com.projectlibre1.pm.graphic.model.cache.GraphicDependency;
import com.projectlibre1.pm.graphic.model.cache.GraphicNode;
import com.projectlibre1.pm.graphic.model.cache.NodeModelCache;
import com.projectlibre1.pm.graphic.timescale.CoordinatesConverter;
import com.projectlibre1.pm.scheduling.ScheduleInterval;
import com.projectlibre1.pm.task.Project;
import com.projectlibre1.pm.task.Task;
import com.projectlibre1.timescale.TimeInterval;
import com.projectlibre1.timescale.TimeIterator;
import com.projectlibre1.pm.graphic.fx.FxLog;
import com.projectlibre1.util.DateTime;

/**
 * JavaFX-backed drawing surface for the Gantt body.
 */
public class FxGanttChart extends JFXPanel {
	private static final long serialVersionUID = 1L;
	private static final Logger LOGGER = FxLog.logger(FxGanttChart.class);
	private static final Color PROGRESS_LINE_COLOR = new Color(0xCC0000);
	private static final Color PROGRESS_LINE_HALO_COLOR = Color.WHITE;
	private static final double PROGRESS_POINT_SIZE = 6.0d;
	private static final double PROJECT_MARKER_STROKE = 1.0d;

	private final Gantt gantt;
	private final Canvas canvas = new Canvas();
	private final AtomicBoolean redrawQueued = new AtomicBoolean(false);
	private final AtomicBoolean redrawPending = new AtomicBoolean(false);
	private final AtomicBoolean sizeQueued = new AtomicBoolean(false);
	private final BufferedImage metricsImage = new BufferedImage(8, 8, BufferedImage.TYPE_INT_ARGB);
	private final Font annotationFont = new Font("Dialog", Font.PLAIN, 11);
	private final FontMetrics annotationMetrics;
	private volatile Dimension contentSize = new Dimension(1, 1);

	private volatile RenderSnapshot latestSnapshot = RenderSnapshot.empty();

	public FxGanttChart(Gantt gantt) {
		this.gantt = gantt;
		Graphics2D metricsGraphics = metricsImage.createGraphics();
		this.annotationMetrics = metricsGraphics.getFontMetrics(annotationFont);
		metricsGraphics.dispose();
		setOpaque(true);
		installSizeBridge();
		installInputBridge();
		Platform.setImplicitExit(false);
		Platform.runLater(() -> {
			StackPane root = new StackPane(canvas);
			root.setStyle("-fx-background-color: white;");
			Scene scene = new Scene(root);
			setScene(scene);
			syncCanvasSize();
			requestRedraw();
		});
	}

	public void installInputBridge() {
		final GraphUI ui = gantt.getUI();
		if (ui == null || ui.getInteractor() == null) {
			return;
		}
		final GraphInteractor interactor = ui.getInteractor();

		addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				interactor.mousePressed(e);
			}

			@Override
			public void mouseReleased(MouseEvent e) {
				interactor.mouseReleased(e);
			}

			@Override
			public void mouseClicked(MouseEvent e) {
				interactor.mouseClicked(e);
			}

			@Override
			public void mouseEntered(MouseEvent e) {
				interactor.mouseEntered(e);
			}

			@Override
			public void mouseExited(MouseEvent e) {
				interactor.mouseExited(e);
			}
		});
		addMouseMotionListener(new MouseMotionAdapter() {
			@Override
			public void mouseDragged(MouseEvent e) {
				interactor.mouseDragged(e);
			}

			@Override
			public void mouseMoved(MouseEvent e) {
				interactor.mouseMoved(e);
			}
		});
		addMouseWheelListener(e -> interactor.mouseWheelMoved((MouseWheelEvent) e));
	}

	private void installSizeBridge() {
		addComponentListener(new ComponentAdapter() {
			@Override
			public void componentResized(ComponentEvent e) {
				queueCanvasResize();
			}
		});
	}

	public void setCoord(CoordinatesConverter coord) {
		requestRedraw();
	}

	public void setCache(NodeModelCache cache) {
		requestRedraw();
	}

	public void setBarStyles(BarStyles barStyles) {
		requestRedraw();
	}

	public void setRowHeight(int rowHeight) {
		requestRedraw();
	}

	public void setProgressLineEnabled(boolean enabled) {
		requestRedraw();
	}

	public void setContentSize(Dimension size) {
		if (size == null) {
			return;
		}
		contentSize = new Dimension(Math.max(1, size.width), Math.max(1, size.height));
		LOGGER.fine("setContentSize " + contentSize.width + "x" + contentSize.height);
		queueCanvasResize();
	}

	public void setRouting(Object routing) {
		requestRedraw();
	}

	public void requestRedraw() {
		if (!SwingUtilities.isEventDispatchThread()) {
			SwingUtilities.invokeLater(this::requestRedraw);
			return;
		}
		if (redrawQueued.getAndSet(true)) {
			redrawPending.set(true);
			return;
		}
		LOGGER.fine("requestRedraw");
		RenderSnapshot snapshot = buildSnapshot();
		latestSnapshot = snapshot;
		Platform.runLater(() -> {
			redrawQueued.set(false);
			renderSnapshot(latestSnapshot);
			if (redrawPending.getAndSet(false)) {
				requestRedraw();
			}
		});
	}

	public void cleanUp() {
		Platform.runLater(() -> setScene(null));
	}

	private void queueCanvasResize() {
		if (sizeQueued.getAndSet(true)) {
			return;
		}
		Platform.runLater(() -> {
			sizeQueued.set(false);
			syncCanvasSize();
			renderSnapshot(latestSnapshot);
		});
	}

	private void syncCanvasSize() {
		double width = Math.max(1, Math.max(getWidth(), contentSize.width));
		double height = Math.max(1, Math.max(getHeight(), contentSize.height));
		LOGGER.fine("syncCanvasSize width=" + width + " height=" + height + " view=" + getWidth() + "x" + getHeight());
		canvas.setWidth(width);
		canvas.setHeight(height);
	}

	private RenderSnapshot buildSnapshot() {
		CoordinatesConverter coord = gantt.getCoord();
		NodeModelCache cache = gantt.getCache();
		BarStyles barStyles = gantt.getBarStyles();
		GraphicConfiguration config = gantt.getConfiguration();
		LOGGER.fine("buildSnapshot coord=" + (coord != null) + " cache=" + (cache != null ? cache.getSize() : -1) + " barStyles=" + (barStyles != null) + " config=" + (config != null));
		if (coord == null || cache == null || barStyles == null || config == null) {
			return RenderSnapshot.empty();
		}

		double rowHeight = gantt.getRowHeight();
		double width = Math.max(1.0d, coord.getWidth());
		double height = Math.max(1.0d, rowHeight * Math.max(1, cache.getSize()));
		Rectangle2D ganttBounds = new Rectangle2D.Double(0.0d, 0.0d, width, height);
		int visibleStartRow = 0;
		int visibleEndRow = cache.getSize();

		List<FillRectOp> fills = new ArrayList<>();
		List<PathOp> paths = new ArrayList<>();
		List<LineOp> lines = new ArrayList<>();
		List<TextOp> texts = new ArrayList<>();
		List<CircleOp> circles = new ArrayList<>();
		List<ProgressPoint> progressPoints = new ArrayList<>();

		collectNonWorkingDays(coord, config, fills, lines, ganttBounds);

		List<GraphicNode> nodeList = new ArrayList<>();
		for (java.util.ListIterator i = cache.getIterator(visibleStartRow); i.hasNext() && i.nextIndex() < visibleEndRow;) {
			int row = i.nextIndex();
			GraphicNode node = (GraphicNode) i.next();
			node.setRow(row);
			if (!node.isSchedule()) {
				continue;
			}
			nodeList.add(node);
			collectNodeRender(node, barStyles, coord, config, paths, texts, lines);
		}

		for (Iterator i = cache.getVisibleDependencies().getIterator(); i.hasNext();) {
			GraphicDependency dependency = (GraphicDependency) i.next();
			collectDependency(dependency, coord, config, barStyles, paths);
		}

		collectProgressLine(nodeList, coord, config, progressPoints, circles);
		collectProjectMarkers(coord, config, fills, lines, ganttBounds);
		LOGGER.fine("buildSnapshot counts fills=" + fills.size() + " paths=" + paths.size() + " lines=" + lines.size()
				+ " texts=" + texts.size() + " circles=" + circles.size() + " progress=" + progressPoints.size());

		return new RenderSnapshot(width, height, fills, paths, lines, texts, circles, progressPoints);
	}

	private void collectNodeRender(GraphicNode node, BarStyles barStyles, CoordinatesConverter coord, GraphicConfiguration config,
			List<PathOp> paths, List<TextOp> texts, List<LineOp> lines) {
		BarCollector collector = new BarCollector(node, coord, config, paths, texts, lines);
		barStyles.apply(node.getNode().getImpl(), collector);
	}

	private void collectDependency(GraphicDependency dependency, CoordinatesConverter coord, GraphicConfiguration config, BarStyles barStyles, List<PathOp> paths) {
		Dependency dep = dependency.getDependency();
		GraphicNode from = dependency.getPredecessor();
		GraphicNode to = dependency.getSuccessor();
		int type = dependency.getType();
		int fromSign = (type == DependencyType.SF || type == DependencyType.SS) ? -1 : 1;
		int toSign = (type == DependencyType.FS || type == DependencyType.SS) ? -1 : 1;
		double fx0 = coord.toX(from.getStart());
		double fx1 = coord.toX(from.getEnd());
		fx1 = CoordinatesConverter.adaptSmallBarEndX(fx0, fx1, from, config);
		double tx0 = coord.toX(to.getStart());
		double tx1 = coord.toX(to.getEnd());
		tx1 = CoordinatesConverter.adaptSmallBarEndX(tx0, tx1, to, config);
		double x0 = fromSign < 0 ? fx0 : fx1;
		double x1 = toSign < 0 ? tx0 : tx1;
		int rowHeight = gantt.getRowHeight();
		int yOffset = config.getGanttBarYOffset() + config.getGanttBarHeight() / 2;
		int y0 = rowHeight * from.getRow();
		int y1 = rowHeight * to.getRow();
		double y2 = Math.max(y0, y1);
		y0 += yOffset;
		y1 += yOffset;

		GeneralPath path = new GeneralPath();
		((GanttLinkRouting) gantt.getRouting()).routePath(path, x0, y0, x1, y1, y2, y1 + to.getGanttShapeHeight() / 2, y1 - to.getGanttShapeHeight() / 2, type);
		Color stroke = dep.isCrossProject() ? new Color(0x8A2BE2) : from.getNode().getImpl() instanceof Task ? GanttRenderSupport.resolveBarColor(barStyles, from.getNode().getImpl()) : Color.DARK_GRAY;
		if (dep.isDisabled()) {
			paths.add(new PathOp(path, null, stroke, 1.0d, new double[] { 6.0d, 4.0d }));
		} else {
			paths.add(new PathOp(path, null, stroke, 1.0d, null));
		}
	}

	private void collectProgressLine(List<GraphicNode> nodes, CoordinatesConverter coord, GraphicConfiguration config, List<ProgressPoint> progressPoints, List<CircleOp> circles) {
		if (!gantt.isProgressLineEnabled()) {
			return;
		}
		for (GraphicNode node : nodes) {
			if (!GanttRenderSupport.shouldIncludeInProgressLine(node)) {
				continue;
			}
			Task task = (Task) node.getNode().getImpl();
			double progressX = GanttRenderSupport.getProgressLineX(coord, task);
			double y = GanttRenderSupport.getProgressLineY(node, config, gantt.getRowHeight());
			progressPoints.add(new ProgressPoint(progressX, y));
			circles.add(new CircleOp(progressX, y, PROGRESS_POINT_SIZE, PROGRESS_LINE_HALO_COLOR));
			circles.add(new CircleOp(progressX, y, PROGRESS_POINT_SIZE - 2, PROGRESS_LINE_COLOR));
		}
	}

	private void collectProjectMarkers(CoordinatesConverter coord, GraphicConfiguration config, List<FillRectOp> fills, List<LineOp> lines, Rectangle2D bounds) {
		Project project = coord.getProject();
		if (project == null) {
			return;
		}
		long projectStart = project.getStart();
		if (projectStart > 0L) {
			double x = coord.toX(projectStart);
			lines.add(LineOp.marker(x, bounds.getY(), bounds.getY() + bounds.getHeight(), new Color(0x808080), PROJECT_MARKER_STROKE, new double[] { 8.0d, 4.0d }));
		}
		long statusDate = project.getStatusDate();
		if (statusDate > 0L) {
			double x = coord.toX(statusDate);
			lines.add(LineOp.marker(x, bounds.getY(), bounds.getY() + bounds.getHeight(), new Color(0x2E8B57), PROJECT_MARKER_STROKE, new double[] { 2.0d, 6.0d }));
		}
	}

	private void collectNonWorkingDays(CoordinatesConverter coord, GraphicConfiguration config, List<FillRectOp> fills, List<LineOp> lines, Rectangle2D bounds) {
		BarFormat calFormat = getCalendarFormat();
		if (calFormat == null) {
			return;
		}
		Project project = coord.getProject();
		WorkingCalendar wc = (WorkingCalendar) project.getWorkCalendar();
		if (coord.getTimescaleManager().isShowWholeDays()) {
			boolean useScale2 = coord.getTimescaleManager().getCurrentScaleIndex() == 0;
			TimeIterator iterator = coord.getTimeIterator(bounds.getX(), bounds.getMaxX(), useScale2);
			long startNonworking = -1L;
			long endNonworking = -1L;
			Calendar cal = DateTime.calendarInstance();
			while (iterator.hasNext()) {
				TimeInterval interval = iterator.next();
				long s = interval.getStart();
				if (CalendarService.getInstance().getDay(wc, s).isWorking()) {
					if (startNonworking != -1L) {
						fills.add(createNonWorkingFill(coord, bounds, startNonworking, endNonworking, useScale2, cal));
						startNonworking = endNonworking = -1L;
					}
				} else {
					if (startNonworking == -1L) {
						startNonworking = s;
					}
					endNonworking = s;
				}
			}
			if (startNonworking != -1L) {
				fills.add(createNonWorkingFill(coord, bounds, startNonworking, endNonworking, useScale2, cal));
			}
		}
		if (project != null) {
			double startX = coord.toX(project.getStart());
			if (startX >= bounds.getX() && startX <= bounds.getMaxX()) {
				lines.add(LineOp.marker(startX, bounds.getY(), bounds.getY() + bounds.getHeight(), new Color(0x808080), PROJECT_MARKER_STROKE, new double[] { 8.0d, 4.0d }));
			}
			long statusDate = project.getStatusDate();
			if (statusDate != 0L) {
				double statusX = coord.toX(statusDate);
				if (statusX >= bounds.getX() && statusX <= bounds.getMaxX()) {
					lines.add(LineOp.marker(statusX, bounds.getY(), bounds.getY() + bounds.getHeight(), new Color(0x2E8B57), PROJECT_MARKER_STROKE, new double[] { 2.0d, 6.0d }));
				}
			}
		}
	}

	private FillRectOp createNonWorkingFill(CoordinatesConverter coord, Rectangle2D bounds, long startNonworking, long endNonworking, boolean useScale2, Calendar cal) {
		cal.setTimeInMillis(endNonworking);
		if (useScale2) {
			coord.getTimescaleManager().getScale().increment2(cal);
		} else {
			coord.getTimescaleManager().getScale().increment1(cal);
		}
		long adjustedEnd = cal.getTimeInMillis();
		double x = coord.toX(startNonworking);
		double w = coord.toW(adjustedEnd - startNonworking);
		return new FillRectOp(x, bounds.getY(), w, bounds.getHeight(), new Color(0xEFEFEF));
	}

	private BarFormat getCalendarFormat() {
		final BarFormat[] result = new BarFormat[1];
		BarStyles barStyles = gantt.getBarStyles();
		if (barStyles == null) {
			return null;
		}
		barStyles.apply(null, arg0 -> result[0] = (BarFormat) arg0, false, false, true, false);
		return result[0];
	}

	private void renderSnapshot(RenderSnapshot snapshot) {
		if (snapshot == null) {
			return;
		}
		boolean empty = snapshot.fills.isEmpty() && snapshot.paths.isEmpty() && snapshot.lines.isEmpty() && snapshot.texts.isEmpty() && snapshot.circles.isEmpty() && snapshot.progressPoints.isEmpty();
		LOGGER.fine("renderSnapshot empty=" + empty + " canvas=" + canvas.getWidth() + "x" + canvas.getHeight());
		syncCanvasSize();
		GraphicsContext gc = canvas.getGraphicsContext2D();
		gc.setFill(Paint.valueOf("white"));
		gc.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());

		for (FillRectOp fill : snapshot.fills) {
			gc.setFill(toFx(fill.fill));
			gc.fillRect(fill.x, fill.y, fill.w, fill.h);
		}

		for (LineOp line : snapshot.lines) {
			drawLine(gc, line);
		}

		for (PathOp path : snapshot.paths) {
			drawPath(gc, path);
		}

		for (TextOp text : snapshot.texts) {
			gc.setFont(javafx.scene.text.Font.font("System", FontWeight.NORMAL, 10.0d));
			gc.setFill(toFx(text.color));
			gc.fillText(text.text, text.x, text.y);
		}

		for (CircleOp circle : snapshot.circles) {
			gc.setFill(toFx(circle.fill));
			gc.fillOval(circle.x - circle.r, circle.y - circle.r, circle.r * 2.0d, circle.r * 2.0d);
		}

		if (!snapshot.progressPoints.isEmpty()) {
			gc.save();
			gc.setStroke(toFx(PROGRESS_LINE_HALO_COLOR));
			gc.setLineWidth(4.0d);
			drawProgressPolyline(gc, snapshot.progressPoints);
			gc.setStroke(toFx(PROGRESS_LINE_COLOR));
			gc.setLineWidth(2.0d);
			drawProgressPolyline(gc, snapshot.progressPoints);
			gc.restore();
		}
	}

	private void drawProgressPolyline(GraphicsContext gc, List<ProgressPoint> points) {
		if (points.isEmpty()) {
			return;
		}
		gc.beginPath();
		ProgressPoint first = points.get(0);
		gc.moveTo(first.x, first.y);
		for (int i = 1; i < points.size(); i++) {
			ProgressPoint point = points.get(i);
			gc.lineTo(point.x, point.y);
		}
		gc.stroke();
	}

	private void drawLine(GraphicsContext gc, LineOp line) {
		gc.save();
		gc.setStroke(toFx(line.color));
		gc.setLineWidth(line.width);
		if (line.dashes != null) {
			gc.setLineDashes(line.dashes);
		} else {
			gc.setLineDashes(null);
		}
		gc.strokeLine(line.x1, line.y1, line.x2, line.y2);
		gc.restore();
	}

	private void drawPath(GraphicsContext gc, PathOp path) {
		gc.save();
		if (path.dashes != null) {
			gc.setLineDashes(path.dashes);
		} else {
			gc.setLineDashes(null);
		}
		gc.setLineWidth(path.strokeWidth);
		if (path.stroke != null) {
			gc.setStroke(toFx(path.stroke));
		}
		if (path.fill != null) {
			gc.setFill(toFx(path.fill));
		}
		gc.beginPath();
		PathIterator it = path.shape.getPathIterator(null);
		double[] seg = new double[6];
		while (!it.isDone()) {
			switch (it.currentSegment(seg)) {
			case PathIterator.SEG_MOVETO:
				gc.moveTo(seg[0], seg[1]);
				break;
			case PathIterator.SEG_LINETO:
				gc.lineTo(seg[0], seg[1]);
				break;
			case PathIterator.SEG_QUADTO:
				gc.quadraticCurveTo(seg[0], seg[1], seg[2], seg[3]);
				break;
			case PathIterator.SEG_CUBICTO:
				gc.bezierCurveTo(seg[0], seg[1], seg[2], seg[3], seg[4], seg[5]);
				break;
			case PathIterator.SEG_CLOSE:
				gc.closePath();
				break;
			}
			it.next();
		}
		if (path.fill != null) {
			gc.fill();
		}
		if (path.stroke != null) {
			gc.stroke();
		}
		gc.restore();
	}

	private javafx.scene.paint.Color toFx(Color color) {
		if (color == null) {
			return javafx.scene.paint.Color.TRANSPARENT;
		}
		return javafx.scene.paint.Color.rgb(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha() / 255.0d);
	}

	private final class BarCollector implements Closure, IntervalConsumer {
		private final GraphicNode node;
		private final CoordinatesConverter coord;
		private final GraphicConfiguration config;
		private final List<PathOp> paths;
		private final List<TextOp> texts;
		private final List<LineOp> lines;
		private final GanttBarSingleIntervalGenerator singleIntervalGenerator = new GanttBarSingleIntervalGenerator();
		private final java.awt.FontMetrics metrics;
		private BarFormat format;
		private int yrow;
		private int minLayer = Integer.MIN_VALUE;
		private int maxLayer = Integer.MAX_VALUE;

		BarCollector(GraphicNode node, CoordinatesConverter coord, GraphicConfiguration config, List<PathOp> paths, List<TextOp> texts, List<LineOp> lines) {
			this.node = node;
			this.coord = coord;
			this.config = config;
			this.paths = paths;
			this.texts = texts;
			this.lines = lines;
			this.yrow = node.getRow() * ganttRowHeight();
			BufferedImage img = new BufferedImage(8, 8, BufferedImage.TYPE_INT_ARGB);
			Graphics2D g2 = img.createGraphics();
			this.metrics = g2.getFontMetrics(new Font("Dialog", Font.PLAIN, 11));
			g2.dispose();
		}

		private int ganttRowHeight() {
			return gantt.getRowHeight();
		}

		@Override
		public void execute(Object arg0) {
			format = (BarFormat) arg0;
			if (format.getLayer() > maxLayer || format.getLayer() < minLayer) {
				return;
			}
			ScheduleIntervalGenerator generator;
			if (format.getScheduleIntervalGenerator() == null) {
				singleIntervalGenerator.initialize(gantt.getCache().getModel(), format.getFromField(), format.getToField());
				generator = singleIntervalGenerator;
			} else {
				generator = format.getScheduleIntervalGenerator();
			}
			generator.consumeIntervals(node, this);
			double y = (node.getRow() + 1) * ganttRowHeight() - 1.0d;
			lines.add(new LineOp(0.0d, y, coord.getWidth(), y, new Color(0xE0E0E0), 1.0d, null, false));
		}

		@Override
		public void consumeInterval(ScheduleInterval interval) {
			if (interval.getEnd() > 100000000000000L) {
				return;
			}
			GanttRenderSupport.BarGeometry geometry = GanttRenderSupport.computeBarGeometry(node, interval, format, coord, config, gantt.getRowHeight());
			double x = geometry.x;
			double y = geometry.y;
			double width = geometry.width;
			double height = geometry.height;

			if (format.getMiddle() != null) {
				Shape shape = format.getMiddle().toGeneralPath(width, height, x, y, null);
				paths.add(new PathOp(shape, format.getMiddle().getPaint() == null ? format.getMiddle().getColor() : format.getMiddle().getColor(), format.getMiddle().getColor(), 1.0d, null));
				Rectangle2D bounds = shape.getBounds2D();
				node.setGanttShapeOffset(bounds.getY() - y + height / 2.0d);
				node.setGanttShapeHeight(bounds.getHeight());
			}

			if (format.getStart() != null) {
				Shape shape = format.getStart().toGeneralPath(height, height, x, y, null);
				paths.add(new PathOp(shape, format.getStart().getColor(), format.getStart().getColor(), 1.0d, null));
			}
			if (format.getEnd() != null) {
				Shape shape = format.getEnd().toGeneralPath(height, height, x + width, y, null);
				paths.add(new PathOp(shape, format.getEnd().getColor(), format.getEnd().getColor(), 1.0d, null));
			}

			if (format.isMain() && !node.isSummary() && node.isStarted()) {
				long completedT = node.getCompleted();
				if (completedT >= interval.getStart()) {
					double completedW = GanttRenderSupport.computeCompletedWidth(node, interval, width, coord, config, com.projectlibre1.options.GanttOption.getInstance().isCompletionIsContiguous());
					lines.add(new LineOp(x, y, x + completedW, y, Color.BLACK, config.getGanttProgressBarHeight(), null, false));
				}
			}

			if (format.getField() != null) {
				Object value = format.getField().getValue(node.getNode(), gantt.getCache().getModel(), null);
				if (value != null) {
					String text = GanttRenderSupport.formatAnnotationValue(value);
					double tx = GanttRenderSupport.computeAnnotationX(node, coord, config);
					int w = metrics.stringWidth(text);
					int ascent = metrics.getAscent();
					texts.add(new TextOp(text, tx, y + ascent - 1, format.getMiddle() == null ? Color.BLACK : format.getMiddle().getColor()));
				}
			}
		}

	}

	private static final class RenderSnapshot {
		final double width;
		final double height;
		final List<FillRectOp> fills;
		final List<PathOp> paths;
		final List<LineOp> lines;
		final List<TextOp> texts;
		final List<CircleOp> circles;
		final List<ProgressPoint> progressPoints;

		RenderSnapshot(double width, double height, List<FillRectOp> fills, List<PathOp> paths, List<LineOp> lines, List<TextOp> texts, List<CircleOp> circles, List<ProgressPoint> progressPoints) {
			this.width = width;
			this.height = height;
			this.fills = fills;
			this.paths = paths;
			this.lines = lines;
			this.texts = texts;
			this.circles = circles;
			this.progressPoints = progressPoints;
		}

		static RenderSnapshot empty() {
			return new RenderSnapshot(1.0d, 1.0d, new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
		}
	}

	private static final class FillRectOp {
		final double x;
		final double y;
		final double w;
		final double h;
		final Color fill;

		FillRectOp(double x, double y, double w, double h, Color fill) {
			this.x = x;
			this.y = y;
			this.w = w;
			this.h = h;
			this.fill = fill;
		}
	}

	private static final class PathOp {
		final Shape shape;
		final Color fill;
		final Color stroke;
		final double strokeWidth;
		final double[] dashes;

		PathOp(Shape shape, Color fill, Color stroke, double strokeWidth, double[] dashes) {
			this.shape = shape;
			this.fill = fill;
			this.stroke = stroke;
			this.strokeWidth = strokeWidth;
			this.dashes = dashes;
		}
	}

	private static final class LineOp {
		final double x1;
		final double y1;
		final double x2;
		final double y2;
		final Color color;
		final double width;
		final double[] dashes;
		LineOp(double x1, double y1, double x2, double y2, Color color, double width, double[] dashes, boolean progressLine) {
			this.x1 = x1;
			this.y1 = y1;
			this.x2 = x2;
			this.y2 = y2;
			this.color = color;
			this.width = width;
			this.dashes = dashes;
		}

		static LineOp marker(double x, double y1, double y2, Color color, double width, double[] dashes) {
			return new LineOp(x, y1, x, y2, color, width, dashes, false);
		}
	}

	private static final class ProgressPoint {
		final double x;
		final double y;

		ProgressPoint(double x, double y) {
			this.x = x;
			this.y = y;
		}
	}

	private static final class TextOp {
		final String text;
		final double x;
		final double y;
		final Color color;

		TextOp(String text, double x, double y, Color color) {
			this.text = text;
			this.x = x;
			this.y = y;
			this.color = color;
		}
	}

	private static final class CircleOp {
		final double x;
		final double y;
		final double r;
		final Color fill;

		CircleOp(double x, double y, double r, Color fill) {
			this.x = x;
			this.y = y;
			this.r = r;
			this.fill = fill;
		}
	}
}
