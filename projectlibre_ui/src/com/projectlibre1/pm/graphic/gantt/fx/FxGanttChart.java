/*******************************************************************************
 * The contents of this file are subject to the Common Public Attribution License
 * Version 1.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * http://www.projectlibre.com/license . The License is based on the Mozilla Public
 * License Version 1.1 but Sections 14 and 15 have been added to cover use of
 * software over a computer network and provide for limited attribution for the
 * Original Developer. In addition, Exhibit A has been modified to be consistent
 * with Exhibit B.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License. The
 * Original Code is ProjectLibre. The Original Developer is the Initial Developer
 * and is ProjectLibre Inc. All portions of the code written by ProjectLibre are
 * Copyright (c) 2012-2019. All Rights Reserved. All portions of the code written by
 * ProjectLibre are Copyright (c) 2012-2019. All Rights Reserved. Contributor
 * ProjectLibre, Inc.
 *
 * Alternatively, the contents of this file may be used under the terms of the
 * ProjectLibre End-User License Agreement (the ProjectLibre License) in which case
 * the provisions of the ProjectLibre License are applicable instead of those above.
 * If you wish to allow use of your version of this file only under the terms of the
 * ProjectLibre License and not to allow others to use your version of this file
 * under the CPAL, indicate your decision by deleting the provisions above and
 * replace them with the notice and other provisions required by the ProjectLibre
 * License. If you do not delete the provisions above, a recipient may use your
 * version of this file under either the CPAL or the ProjectLibre Licenses.
 *
 *
 * [NOTE: The text of this Exhibit A may differ slightly from the text of the notices
 * in the Source Code files of the Original Code. You should use the text of this
 * Exhibit A rather than the text found in the Original Code Source Code for Your
 * Modifications.]
 *
 * EXHIBIT B. Attribution Information for ProjectLibre required
 *
 * Attribution Copyright Notice: Copyright (c) 2012-2019, ProjectLibre, Inc.
 * Attribution Phrase (not exceeding 10 words):
 * ProjectLibre, open source project management software.
 * Attribution URL: http://www.projectlibre.com
 * Graphic Image as provided in the Covered Code as file: projectlibre-logo.png with
 * alternatives listed on http://www.projectlibre.com/logo
 *
 * Display of Attribution Information is required in Larger Works which are defined
 * in the CPAL as a work which combines Covered Code or portions thereof with code
 * not governed by the terms of the CPAL. However, in addition to the other notice
 * obligations, all copies of the Covered Code in Executable and Source Code form
 * distributed must, as a form of attribution of the original author, include on
 * each user interface screen the "ProjectLibre" logo visible to all users.
 * The ProjectLibre logo should be located horizontally aligned with the menu bar
 * and left justified on the top left of the screen adjacent to the File menu. The
 * logo must be at least 144 x 31 pixels. When users click on the "ProjectLibre"
 * logo it must direct them back to http://www.projectlibre.com.
 *******************************************************************************/
package com.projectlibre1.pm.graphic.gantt.fx;

import java.awt.Dimension;

import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.layout.StackPane;

import com.projectlibre1.graphic.configuration.BarStyles;
import com.projectlibre1.graphic.configuration.GraphicConfiguration;
import com.projectlibre1.pm.graphic.gantt.GanttModel;
import com.projectlibre1.pm.graphic.model.cache.NodeModelCache;
import com.projectlibre1.pm.graphic.timescale.CoordinatesConverter;
import com.projectlibre1.pm.graphic.timescale.ScaledComponent;
import com.projectlibre1.pm.task.Project;
import com.projectlibre1.pm.time.HasStartAndEnd;
import com.projectlibre1.timescale.TimeScaleEvent;
import com.projectlibre1.timescale.TimeScaleListener;

/**
 * JavaFX-based Gantt chart component.
 * Replaces Swing Gantt rendering with JavaFX Canvas for the visualization layer.
 * Embedded via JFXPanel into the existing Swing UI.
 */
public class FxGanttChart extends JFXPanel implements ScaledComponent, TimeScaleListener {

    private static final long serialVersionUID = 1L;

    protected GanttModel model;
    protected Project project;
    protected NodeModelCache cache;
    protected CoordinatesConverter coord;
    protected BarStyles barStyles;
    protected boolean progressLineEnabled = false;
    protected int rowHeight;

    // JavaFX components
    protected Canvas canvas;
    protected FxGanttRenderer renderer;
    protected volatile boolean needsRedraw = true;

    // listener to propagate repaint to scrollpane
    protected Runnable repaintCallback;

    public FxGanttChart(Project project, String viewName) {
        this.project = project;
        this.model = new GanttModel(project, viewName);
        this.rowHeight = GraphicConfiguration.getInstance().getRowHeight();
        initFX();
    }

    public FxGanttChart(GanttModel model, Project project) {
        this.project = project;
        this.model = model;
        this.rowHeight = GraphicConfiguration.getInstance().getRowHeight();
        initFX();
    }

    private void initFX() {
        Platform.setImplicitExit(false);
        Platform.runLater(() -> {
            canvas = new Canvas();
            renderer = new FxGanttRenderer();
            StackPane root = new StackPane(canvas);
            Scene scene = new Scene(root);
            setScene(scene);

            // Listen to canvas size changes to auto-redraw
            canvas.widthProperty().addListener((obs, old, nv) -> requestRedraw());
            canvas.heightProperty().addListener((obs, old, nv) -> requestRedraw());
            requestRedraw();
        });
    }

    @Override
    public void setSize(int width, int height) {
        super.setSize(width, height);
        syncCanvasSize(width, height);
    }

    @Override
    public void setBounds(int x, int y, int width, int height) {
        super.setBounds(x, y, width, height);
        syncCanvasSize(width, height);
    }

    private void syncCanvasSize(int width, int height) {
        Platform.runLater(() -> {
            if (canvas != null) {
                canvas.setWidth(Math.max(0, width));
                canvas.setHeight(Math.max(0, height));
                requestRedraw();
            }
        });
    }

    /**
     * Schedule a redraw on the JavaFX thread.
     */
    public void requestRedraw() {
        needsRedraw = true;
        Platform.runLater(this::draw);
    }

    protected void draw() {
        if (canvas == null || renderer == null) return;
        if (!needsRedraw) return;
        needsRedraw = false;

        double w = canvas.getWidth();
        double h = canvas.getHeight();
        if (w <= 0 || h <= 0) return;

        renderer.render(canvas.getGraphicsContext2D(), this, coord, cache, w, h);
    }

    @Override
    public void setCoord(CoordinatesConverter coord) {
        if (this.coord != null) {
            this.coord.removeTimeScaleListener(this);
        }
        this.coord = coord;
        if (coord != null) {
            coord.addTimeScaleListener(this);
            model.setCoord(coord);
        }
        requestRedraw();
    }

    @Override
    public CoordinatesConverter getCoord() {
        return coord;
    }

    @Override
    public void timeScaleChanged(TimeScaleEvent e) {
        updateSize();
    }

    public void updateSize() {
        if (model == null || coord == null) return;
        double totalWidth = coord.getEnd() - coord.getOrigin();
        int taskCount = (cache != null) ? cache.getSize() : 0;
        double totalHeight = (taskCount + 1) * rowHeight;
        int preferredWidth = Math.max((int) totalWidth, 100);
        int preferredHeight = Math.max((int) totalHeight, 100);
        setPreferredSize(new Dimension(preferredWidth, preferredHeight));
        revalidate();
        requestRedraw();
    }

    public void setCache(NodeModelCache cache) {
        this.cache = cache;
        requestRedraw();
    }

    public void setBarStyles(BarStyles barStyles) {
        this.barStyles = barStyles;
        requestRedraw();
    }

    public void setRowHeight(int rowHeight) {
        this.rowHeight = rowHeight;
        if (model != null) model.setRowHeight(rowHeight);
        updateSize();
        requestRedraw();
    }

    public int getRowHeight() {
        return rowHeight;
    }

    public void setProgressLineEnabled(boolean enabled) {
        this.progressLineEnabled = enabled;
        requestRedraw();
    }

    public boolean isProgressLineEnabled() {
        return progressLineEnabled;
    }

    public void scrollToTask(HasStartAndEnd interval, boolean center) {
        if (coord == null || interval == null) return;
        long start = interval.getStart();
        int x = (int) coord.toX(start);
        scrollRectToVisible(new java.awt.Rectangle(x - (center ? 200 : 0), 0, 400, getHeight()));
    }

    public void cleanUp() {
        if (coord != null) {
            coord.removeTimeScaleListener(this);
        }
        model = null;
        cache = null;
        project = null;
        coord = null;
        renderer = null;
        canvas = null;
    }

    // Called when the component is made visible; sizes canvas to fill
    @Override
    public void addNotify() {
        super.addNotify();
        syncCanvasSize(getWidth(), getHeight());
    }
}
