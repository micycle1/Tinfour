/*
 * Copyright 2025 Gary W. Lucas.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.tinfour.geom;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import org.tinfour.common.LinearConstraint;
import org.tinfour.common.PolygonConstraint;
import org.tinfour.common.Vertex;

/**
 * Verifies that the path-producing core types describe their geometry to a
 * {@link GeometryWriter} correctly (correct primitive, coordinates, and
 * closed/hole structure), in model coordinates.
 */
public class GeometryWriterTest {

  /** A recording GeometryWriter that captures every call for inspection. */
  private static class Recorder implements GeometryWriter {

    final List<double[]> polylines = new ArrayList<>();
    final List<Boolean> polylineClosed = new ArrayList<>();
    final List<double[]> polygonShells = new ArrayList<>();
    final List<double[][]> polygonHoles = new ArrayList<>();

    @Override
    public void addPolyline(double[] xy, boolean closed) {
      polylines.add(xy);
      polylineClosed.add(closed);
    }

    @Override
    public void addPolygon(double[] shell, double[][] holes) {
      polygonShells.add(shell);
      polygonHoles.add(holes);
    }
  }

  private static final double EPS = 1.0e-12;

  @Test
  public void polygonConstraintWritesClosedPolygonNoHoles() {
    PolygonConstraint p = new PolygonConstraint();
    p.add(new Vertex(0, 0, 0));
    p.add(new Vertex(4, 0, 0));
    p.add(new Vertex(4, 3, 0));
    p.complete();

    Recorder r = new Recorder();
    p.writeTo(r);

    assertTrue("no polylines expected", r.polylines.isEmpty());
    assertEquals("one polygon expected", 1, r.polygonShells.size());
    assertArrayEquals(new double[]{0, 0, 4, 0, 4, 3}, r.polygonShells.get(0), EPS);
    assertTrue("no holes expected", r.polygonHoles.get(0) == null
            || r.polygonHoles.get(0).length == 0);
  }

  @Test
  public void linearConstraintWritesOpenPolyline() {
    LinearConstraint c = new LinearConstraint();
    c.add(new Vertex(1, 2, 0));
    c.add(new Vertex(3, 5, 0));
    c.add(new Vertex(7, 1, 0));
    c.complete();

    Recorder r = new Recorder();
    c.writeTo(r);

    assertTrue("no polygons expected", r.polygonShells.isEmpty());
    assertEquals("one polyline expected", 1, r.polylines.size());
    assertFalse("polyline must be open", r.polylineClosed.get(0));
    assertArrayEquals(new double[]{1, 2, 3, 5, 7, 1}, r.polylines.get(0), EPS);
  }
}
