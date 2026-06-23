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

 /*
 * -----------------------------------------------------------------------
 *
 * Revision History:
 * Date Name Description
 * ------   ---------   -------------------------------------------------
 * 00/2025  G. Lucas    Created as part of removing java.awt dependencies
 *
 * Notes:
 *
 *   This class provides a lightweight, double-precision replacement for
 *   java.awt.geom.Path2D so that the Tinfour core module does not depend on
 *   the AWT graphics package (see GitHub issue #136).
 *
 *   The path is limited to the straight-line segment operations that Tinfour
 *   actually uses (moveTo, lineTo, closePath). The recorded segments can be
 *   read back through getSegmentTypes() and getCoordinates() so that a
 *   downstream rendering module can reconstruct an equivalent
 *   java.awt.geom.Path2D for drawing.
 *
 * -----------------------------------------------------------------------
 */
package org.tinfour.geom;

import java.util.Arrays;

/**
 * A double-precision path made up of straight-line segments. This class is a
 * lightweight, dependency-free replacement for the portion of
 * {@code java.awt.geom.Path2D} used by Tinfour. It supports the
 * {@code moveTo}, {@code lineTo}, and {@code closePath} operations and records
 * the resulting segments so that they may be read back for rendering.
 */
public class GeoPath {

  /**
   * The winding rule constant for an even-odd interior determination. The
   * value matches {@code java.awt.geom.Path2D.WIND_EVEN_ODD}.
   */
  public static final int WIND_EVEN_ODD = 0;

  /**
   * The winding rule constant for a non-zero interior determination. The
   * value matches {@code java.awt.geom.Path2D.WIND_NON_ZERO}.
   */
  public static final int WIND_NON_ZERO = 1;

  /**
   * A segment type indicating a move to a new point (start of a subpath). The
   * value matches {@code java.awt.geom.PathIterator.SEG_MOVETO}.
   */
  public static final int SEG_MOVETO = 0;

  /**
   * A segment type indicating a line to a point. The value matches
   * {@code java.awt.geom.PathIterator.SEG_LINETO}.
   */
  public static final int SEG_LINETO = 1;

  /**
   * A segment type indicating that the current subpath should be closed. The
   * value matches {@code java.awt.geom.PathIterator.SEG_CLOSE}.
   */
  public static final int SEG_CLOSE = 4;

  private int windingRule;

  private int[] segmentTypes = new int[16];
  private int numSegments;

  private double[] coords = new double[32];
  private int numCoords;

  /**
   * Constructs an empty path with the {@link #WIND_NON_ZERO} winding rule.
   */
  public GeoPath() {
    this.windingRule = WIND_NON_ZERO;
  }

  /**
   * Constructs an empty path with the specified winding rule.
   *
   * @param windingRule either {@link #WIND_EVEN_ODD} or {@link #WIND_NON_ZERO}
   */
  public GeoPath(int windingRule) {
    setWindingRule(windingRule);
  }

  /**
   * Sets the winding rule for determining the interior of the path.
   *
   * @param windingRule either {@link #WIND_EVEN_ODD} or {@link #WIND_NON_ZERO}
   */
  public final void setWindingRule(int windingRule) {
    if (windingRule != WIND_EVEN_ODD && windingRule != WIND_NON_ZERO) {
      throw new IllegalArgumentException(
              "winding rule must be WIND_EVEN_ODD or WIND_NON_ZERO");
    }
    this.windingRule = windingRule;
  }

  /**
   * Gets the winding rule for determining the interior of the path.
   *
   * @return either {@link #WIND_EVEN_ODD} or {@link #WIND_NON_ZERO}
   */
  public int getWindingRule() {
    return windingRule;
  }

  /**
   * Adds a point to the path by moving to the specified coordinates, starting
   * a new subpath.
   *
   * @param x the x coordinate
   * @param y the y coordinate
   */
  public void moveTo(double x, double y) {
    addSegment(SEG_MOVETO);
    addCoordinate(x);
    addCoordinate(y);
  }

  /**
   * Adds a point to the path by drawing a straight line from the current
   * coordinates to the specified coordinates.
   *
   * @param x the x coordinate
   * @param y the y coordinate
   */
  public void lineTo(double x, double y) {
    addSegment(SEG_LINETO);
    addCoordinate(x);
    addCoordinate(y);
  }

  /**
   * Closes the current subpath by drawing a straight line back to the
   * coordinates of the last {@code moveTo}.
   */
  public void closePath() {
    addSegment(SEG_CLOSE);
  }

  /**
   * Gets the number of recorded segments.
   *
   * @return a positive integer (zero if the path is empty)
   */
  public int getSegmentCount() {
    return numSegments;
  }

  /**
   * Gets a copy of the recorded segment types, in order. Each value is one of
   * {@link #SEG_MOVETO}, {@link #SEG_LINETO}, or {@link #SEG_CLOSE}.
   *
   * @return a valid, potentially zero-length array
   */
  public int[] getSegmentTypes() {
    return Arrays.copyOf(segmentTypes, numSegments);
  }

  /**
   * Gets a copy of the recorded coordinates, stored as consecutive (x,y)
   * pairs. There is one pair for each {@link #SEG_MOVETO} and
   * {@link #SEG_LINETO} segment, and no coordinates for {@link #SEG_CLOSE}.
   *
   * @return a valid, potentially zero-length array
   */
  public double[] getCoordinates() {
    return Arrays.copyOf(coords, numCoords);
  }

  private void addSegment(int type) {
    if (numSegments == segmentTypes.length) {
      segmentTypes = Arrays.copyOf(segmentTypes, segmentTypes.length * 2);
    }
    segmentTypes[numSegments++] = type;
  }

  private void addCoordinate(double value) {
    if (numCoords == coords.length) {
      coords = Arrays.copyOf(coords, coords.length * 2);
    }
    coords[numCoords++] = value;
  }
}
