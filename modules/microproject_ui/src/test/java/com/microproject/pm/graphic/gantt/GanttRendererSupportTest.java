/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2012-2019 ProjectLibre, Inc.  (Previous Copyright Holder)
 * Copyright (c) 2026 microProject
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *******************************************************************************/
package com.microproject.pm.graphic.gantt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;

import org.junit.jupiter.api.Test;

import com.microproject.graphic.configuration.BarFormat;
import com.microproject.pm.dependency.Dependency;
import com.microproject.pm.dependency.DependencyService;
import com.microproject.pm.dependency.DependencyType;
import com.microproject.pm.resource.ResourcePool;
import com.microproject.pm.task.NormalTask;
import com.microproject.pm.task.Project;
import com.microproject.preference.GlobalPreferences;
import com.microproject.undo.DataFactoryUndoController;
import com.microproject.util.MicrosoftProjectGanttPalette;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GanttRendererSupportTest {
	@Test
	void annotationLayoutPrefersRightSideWhenSpaceExists() {
		GanttRendererSupport.AnnotationLayout layout = GanttRendererSupport.resolveAnnotationLayout(
				new Rectangle(0, 0, 200, 40),
				20.0d,
				90.0d,
				8,
				50);

		assertEquals(98, layout.x);
		assertEquals(64, layout.availableWidth);
	}

	@Test
	void annotationLayoutReturnsNullWhenNothingIsVisible() {
		assertNull(GanttRendererSupport.resolveAnnotationLayout(
				new Rectangle(0, 0, 80, 40),
				200.0d,
				260.0d,
				8,
				40));
	}

	@Test
	void annotationLayoutHonorsRequestedSideWhenItFits() {
		Rectangle clip = new Rectangle(0, 0, 400, 80);
		GanttRendererSupport.AnnotationLayout left = GanttRendererSupport.resolveAnnotationLayout(
				clip, 150, 210, 4, 80, GlobalPreferences.GANTT_BAR_TEXT_POSITION_LEFT);
		GanttRendererSupport.AnnotationLayout right = GanttRendererSupport.resolveAnnotationLayout(
				clip, 150, 210, 4, 80, GlobalPreferences.GANTT_BAR_TEXT_POSITION_RIGHT);
		assertTrue(left.x < 150);
		assertTrue(right.x > 210);
	}

	@Test
	void individualMilestoneShapeProducesOnlyKnownShapePaths() {
		assertNull(GanttRendererSupport.individualMilestoneShape("ARROW_UP", 12.0d, 30.0d, 20.0d));
		assertTrue(GanttRendererSupport.individualMilestoneShape("DIAMOND", 12.0d, 30.0d, 20.0d).getBounds2D().getWidth() > 0.0d);
		assertTrue(GanttRendererSupport.individualMilestoneShape("SQUARE", 12.0d, 30.0d, 20.0d).getBounds2D().getHeight() > 0.0d);
		assertTrue(GanttRendererSupport.individualMilestoneShape("TRIANGLE_UP", 12.0d, 30.0d, 20.0d).getBounds2D().getWidth() > 0.0d);
		assertTrue(GanttRendererSupport.individualMilestoneShape("TRIANGLE_DOWN", 12.0d, 30.0d, 20.0d).getBounds2D().getHeight() > 0.0d);
	}

	@Test
	void configuredDefaultBarColorKeepsIndividualCriticalAndBaselinePriorities() {
		assertEquals(new Color(0x112233), GanttRendererSupport.resolveTaskBarColor(false, false, null, 0x112233,
				Color.BLUE, Color.GRAY, Color.RED));
		assertEquals(new Color(0x445566), GanttRendererSupport.resolveTaskBarColor(false, false, 0x445566, 0x112233,
				Color.BLUE, Color.GRAY, Color.RED));
		assertEquals(Color.RED, GanttRendererSupport.resolveTaskBarColor(false, true, null, 0x112233,
				Color.BLUE, Color.GRAY, Color.RED));
		assertEquals(Color.GRAY, GanttRendererSupport.resolveTaskBarColor(true, false, null, 0x112233,
				Color.BLUE, Color.GRAY, Color.RED));
	}

	@Test
	void clipAnnotationTextShortensWithEllipsis() {
		FontMetrics metrics = createMetrics();
		String clipped = GanttRendererSupport.clipAnnotationText(metrics, "project milestone", 40);

		assertTrue(clipped.endsWith("..."));
		assertTrue(clipped.length() < "project milestone".length());
	}

	@Test
	void endpointColorFollowsUniformEndpointRule() {
		BarFormat format = new BarFormat();
		format.setId("Bar.task");
		Color status = Color.RED;
		Color accent = Color.BLUE;

		assertEquals(status, GanttRendererSupport.resolveEndpointColor(format, status, accent));

		format.setId("Bar.custom");
		assertEquals(accent, GanttRendererSupport.resolveEndpointColor(format, status, accent));
	}

	@Test
	void annotationKeyUsesBothFieldAndFormatIds() {
		BarFormat format = new BarFormat();
		format.setId("Bar.task");
		com.microproject.field.Field field = new com.microproject.field.Field();
		field.setName("name");

		assertEquals("name|Bar.task", GanttRendererSupport.annotationKey(field, format));
	}

	@Test
	void ganttRendererDefaultsToMicrosoftProjectPalette() {
		GanttRenderer renderer = new GanttRenderer();
		assertTrue(renderer.getPalette() instanceof MicrosoftProjectGanttPalette);
	}

	@Test
	void crossProjectLinkLabelUsesThePredecessorSourceProject() throws Exception {
		Project source = project("Source project");
		Project target = project("Target project");
		NormalTask predecessor = (NormalTask) source.createLocalTaskNode(null).getImpl();
		predecessor.setName("Design");
		NormalTask successor = (NormalTask) target.createLocalTaskNode(null).getImpl();
		Dependency dependency = DependencyService.getInstance().newDependency(predecessor, successor,
				DependencyType.Kind.FS.code(), 0L, this);

		assertEquals("Source project: Design", GanttRenderer.crossProjectLinkLabel(dependency));
	}

	private static Project project(String name) {
		DataFactoryUndoController undo = new DataFactoryUndoController();
		Project project = Project.createProject(ResourcePool.createRourcePool(name, undo), undo);
		project.initialize(false, false);
		project.setName(name);
		return project;
	}

	private static FontMetrics createMetrics() {
		BufferedImage image = new BufferedImage(10, 10, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g2 = image.createGraphics();
		try {
			return g2.getFontMetrics(new Font("Dialog", Font.PLAIN, 12));
		} finally {
			g2.dispose();
		}
	}
}
