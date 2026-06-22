/*
 * Copyright 2014 Gary W. Lucas.
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
 * Date    Name       Description
 * ------  ---------  -------------------------------------------------
 * 05/2014 G. Lucas   Created
 *
 * Notes: This class collects fragments of code from the various methods/classes
 * in the TIN family into a single unified (and hopefully consistent)
 * class.
 *
 * -----------------------------------------------------------------------
 */
package org.tinfour.common;

/**
 * Provides a standard calculation of threshold values appropriate for use in an
 * incremental TIN implementation based on nominal point spacing.
 * With the exception of vertex tolerance, all thresholds are computed using
 * a small multiplier times the Unit of Least Precision (ULP) computed
 * from the nominal point spacing.  The vertex tolerance is a fixed fraction
 * of the nominal point spacing.
 */
public class Thresholds {

  /** Factor for computing precision threshold. */
  public static final double PRECISION_THRESHOLD_FACTOR = 256;
  /** Factor for computing the half-plane threshold. */
  public static final double HALF_PLANE_THRESHOLD_FACTOR = 256.0;
  /** Factor for computing the Delaunay threshold. */
  public static final double DELAUNAY_THRESHOLD_FACTOR = 256.0;
  /** Factor for computing the in-circle threshold. */
  public static final double IN_CIRCLE_THRESHOLD_FACTOR = 1024 * 1024;
  /** Factor for computing the vertex tolerance. */
  public static final double VERTEX_TOLERANCE_FACTOR_DEFAULT = 1.0e+5;

  /**
   * The unit round-off for IEEE-754 double-precision arithmetic, 2<sup>-53</sup>.
   * This is half of {@code Math.ulp(1.0)} and represents the maximum relative
   * error introduced by a single rounded floating-point operation.
   */
  public static final double DOUBLE_PRECISION_EPSILON = 0x1.0p-53; // 2^-53 ~ 1.11e-16

  /**
   * Relative error bound for the ordinary-precision in-circle determinant.
   * <p>
   * Unlike the absolute thresholds above (which are keyed to the nominal point
   * spacing), this bound is applied to a "permanent" derived from the magnitude
   * of the terms in the determinant actually being evaluated. The result is an
   * adaptive criterion that remains valid regardless of the magnitude of the
   * input coordinates: whenever the magnitude of an ordinary-precision in-circle
   * result is no larger than this bound times the permanent, the sign of the
   * result cannot be trusted and an extended-precision evaluation is required.
   * <p>
   * The value follows the {@code iccerrboundA} constant from Jonathan Shewchuk's
   * "Adaptive Precision Floating-Point Arithmetic and Fast Robust Geometric
   * Predicates" (Discrete &amp; Computational Geometry, 1997).
   */
  public static final double IN_CIRCLE_ERROR_BOUND
    = (10.0 + 96.0 * DOUBLE_PRECISION_EPSILON) * DOUBLE_PRECISION_EPSILON;

  /**
   * The nominal point spacing value specified in the constructor.
   * In general, this value is a rough estimate of the
   * mean distance between neighboring points (or vertices).
   */
  private final double nominalPointSpacing;
  /**
   * A threshold value giving guidelines for the smallest absolute value
   * that can be used in geometric calculations without excessive loss
   * of precision. This value is based on general assumptions about the
   * what constitutes a significant distance given the nominal point
   * spacing of the vertices in the TIN.
   */
  private final double precisionThreshold;
  /**
   * A threshold value giving guidelines for the smallest absolute value
   * result that can be trusted in geometric calculations for determining
   * on which side of a point a plane lies (the "half-plane calculation").
   * If the absolute value of the result is smaller than this threshold,
   * extended-precision arithmetic is advised.
   */
  private final double halfPlaneThreshold;

  /**
   * The computed value for the threshold that indicates
   * when an ordinary precision calculation for the in-circle criterion
   * may be inaccurate and an extended precision calculation should be used.
   */
  private final double inCircleThreshold;

  /**
   * The computed value for evaluating whether a triangle pair is
   * within sufficient tolerance when testing to see if they approximately
   * meet the Delaunay criterion using the in-circle calculation.
   * A positive (non-zero) in-circle value indicates that the pair
   * violates the criterion, but for case where floating-point limitations
   * may result in conflicts, a very small positive value may be acceptable
   * for approximation purposes.
   */
  private final double delaunayThreshold;

  /**
   * A threshold value indicating the distance at which a pair
   * of (x,y) coordinates will be treated as effectively a match for
   * a vertex.
   */
  private final double vertexTolerance;
  /**
   * The square of the vertex tolerance value.
   */
  private final double vertexTolerance2; // square of vertexTolerance;

  private final double circumcircleDeterminantThreshold;
  /**
   * Constructs thresholds for a nominal point spacing of 1.
   */
  public Thresholds() {
    nominalPointSpacing = 1.0;
    double ulp = Math.ulp(nominalPointSpacing);
    precisionThreshold = PRECISION_THRESHOLD_FACTOR * ulp;
    halfPlaneThreshold = HALF_PLANE_THRESHOLD_FACTOR * precisionThreshold;
    inCircleThreshold = IN_CIRCLE_THRESHOLD_FACTOR * precisionThreshold;
    delaunayThreshold = DELAUNAY_THRESHOLD_FACTOR * precisionThreshold;
    vertexTolerance = nominalPointSpacing / VERTEX_TOLERANCE_FACTOR_DEFAULT;
    vertexTolerance2 = vertexTolerance * vertexTolerance;
    circumcircleDeterminantThreshold = 256 * inCircleThreshold;
  }

  /**
   * Constructs threshold values for the specified nominalPointSpacing.
   * In general, the nominal point spacing is a rough estimate of the
   * mean distance between neighboring points (or vertices). It is used
   * for estimating threshold values for logic used by the IncrementalTin
   * and related classes. A perfect value is not necessary. An estimate
   * within a couple orders of magnitude of the actual value is sufficient.
   *
   * @param nominalPointSpacing a positive, non-zero value
   */
  public Thresholds(final double nominalPointSpacing) {
    if (nominalPointSpacing <= 0) {
      throw new IllegalArgumentException(
        "Nominal point spacing specification "
        + nominalPointSpacing
        + " is not greater than zero");
    }
    this.nominalPointSpacing = nominalPointSpacing;
    double ulp = Math.ulp(nominalPointSpacing);
    precisionThreshold = PRECISION_THRESHOLD_FACTOR * ulp;
    halfPlaneThreshold = HALF_PLANE_THRESHOLD_FACTOR * precisionThreshold;
    inCircleThreshold = IN_CIRCLE_THRESHOLD_FACTOR * precisionThreshold;
    delaunayThreshold = DELAUNAY_THRESHOLD_FACTOR * precisionThreshold;

    vertexTolerance = nominalPointSpacing / VERTEX_TOLERANCE_FACTOR_DEFAULT;
    vertexTolerance2 = vertexTolerance * vertexTolerance;

    circumcircleDeterminantThreshold = 32 * inCircleThreshold;
  }

  /**
   * Gets the threshold value indicating when an extended-precision
   * calculation must be used for the in-circle determination.
   *
   * @return a positive value scaled according to the nominal
   * point spacing of the TIN.
   */
  public double getInCircleThreshold() {
    return inCircleThreshold;
  }

  /**
   * Gets a threshold value indicating the distance at which a pair
   * of (x,y) coordinates will be treated as effectively a match for
   * a vertex.
   *
   * @return a distance in the system of units consistent with the TIN.
   */
  public double getVertexTolerance() {
    return vertexTolerance;
  }

  /**
   * Gets a threshold value indicating the square of the distance at which a
   * pair of (x,y) coordinates will be treated as effectively a match for
   * a vertex.
   *
   * @return a distance squared in the system of units consistent with the
   * TIN.
   */
  public double getVertexTolerance2() {
    return vertexTolerance2;
  }

  /**
   * Get a threshold value giving guidelines for the smallest absolute value
   * result from a geometric calculations that can be accepted without
   * concern for an excessive loss of precision. This value is based on
   * general assumptions about the what constitutes a significant distance
   * given the nominal point spacing of the vertices in the TIN.
   *
   * @return a small, positive value.
   */
  public double getPrecisionThreshold() {
    return precisionThreshold;
  }

  /**
   * Gets the nominal point spacing value specified in the constructor.
   * In general, this value is a rough estimate of the
   * mean distance between neighboring points (or vertices).
   *
   * @return a positive, non-zero value.
   */
  public double getNominalPointSpacing() {
    return nominalPointSpacing;
  }

  /**
   * Gets the computed value for evaluating whether a triangle pair is
   * within sufficient tolerance when testing to see if they approximately
   * meet the Delaunay criterion using the in-circle calculation.
   * A positive (non-zero) in-circle value indicates that the pair
   * violates the criterion, but for case where floating-point limitations
   * may result in conflicts, a very small positive value may be acceptable
   * for approximation purposes.
   * <p>
   * This value is primarily used in test procedures that evaluate
   * the correctness of a TIN constructed by the IncrementalTin class.
   *
   * @return A positive value much smaller than the nominal point spacing.
   */
  public double getDelaunayThreshold() {
    return delaunayThreshold;
  }


  /**
   * Gets a threshold value giving guidelines for the smallest absolute value
   * result that can be trusted in geometric calculations for determining
   * on which side of a point a plane lies (the "half-plane calculation").
   * If the absolute value of the result is smaller than this threshold,
   * extended-precision arithmetic is advised.
   * @return a positive, non-zero value much smaller than the nominal
   * point spacing.
   */
  public double getHalfPlaneThreshold() {
    return halfPlaneThreshold;
  }

  /**
   * Gets a threshold value giving guidelines for the smallest absolute value
   * result that can be trusted in geometric calculations for for computing
   * a determinant to be used in determining a set of circumcircle
   * center coordinates and radius.  If the absolute value
   * of the determinant result is smaller than this threshold, extended-precision
   * arithmetic is advised.
   * @return a positive, non-zero value significantly smaller than the
   * nominal point spacing.
   */
  public double getCircumcircleDeterminantThreshold(){
    return   circumcircleDeterminantThreshold;
  }
}
