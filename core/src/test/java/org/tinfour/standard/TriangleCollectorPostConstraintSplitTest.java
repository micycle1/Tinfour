/* --------------------------------------------------------------------
 * Copyright 2024 The Tinfour Contributors.
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
 * -------------------------------------------------------------------
 */
package org.tinfour.standard;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.tinfour.common.IIncrementalTin;
import org.tinfour.common.IIntegrityCheck;
import org.tinfour.common.PolygonConstraint;
import org.tinfour.common.Vertex;
import org.tinfour.utils.TriangleCollector;

public class TriangleCollectorPostConstraintSplitTest {

    @Test
    public void testTriangleCollectionAfterConstraintSplit() {
        // 1. Initialize an IncrementalTin instance
        IIncrementalTin tin = new IncrementalTin(1.0);

        // 2. Define a simple rectangular PolygonConstraint
        List<Vertex> constraintVertices = new ArrayList<>();
        Vertex v1 = new Vertex(1, 1, 0);
        Vertex v2 = new Vertex(5, 1, 0);
        Vertex v3 = new Vertex(5, 5, 0);
        Vertex v4 = new Vertex(1, 5, 0);
        constraintVertices.add(v1);
        constraintVertices.add(v2);
        constraintVertices.add(v3);
        constraintVertices.add(v4);

        PolygonConstraint polygonConstraint = new PolygonConstraint(constraintVertices);
        polygonConstraint.complete();

        // 3. Add this polygon constraint to the TIN
        tin.addConstraints(Collections.singletonList(polygonConstraint), true);

        // 4. Add a new Vertex that lies exactly on one of the horizontal or vertical edges
        Vertex vSplit = new Vertex(3, 1, 0); // Lies on the edge (1,1,0) to (5,1,0)
        tin.add(vSplit);

        // 5. Collect all constrained triangles
        List<Vertex[]> collectedTriangles = new ArrayList<>();
        Consumer<Vertex[]> consumer = triangle -> {
            collectedTriangles.add(new Vertex[]{triangle[0], triangle[1], triangle[2]});
        };
        TriangleCollector.visitTrianglesConstrained(tin, consumer);

        // 6. Perform assertions
        // a. Assert that tin.isBootstrapped() is true
        assertTrue(tin.isBootstrapped(), "TIN should be bootstrapped");

        // b. Assert that tin.getIntegrityCheck().inspect() returns true
        IIntegrityCheck integrityCheck = tin.getIntegrityCheck();
        assertTrue(integrityCheck.inspect(), "TIN integrity check failed");

        // c. Assert that at least two triangles are collected
        assertTrue(collectedTriangles.size() >= 2, "At least two triangles should be collected. Found: " + collectedTriangles.size());

        // d/e. Verify triangles involving vSplit and forming part of the original constraint boundary
        // We expect to find two triangles that share the original edge that was split,
        // now forming two new edges with vSplit as a common vertex.
        // Triangle 1: (v1, vSplit, v4) or (v1, vSplit, v3) depending on triangulation
        // Triangle 2: (vSplit, v2, v3) or (vSplit, v2, v4) depending on triangulation
        // More robustly: check for triangles [v1, vSplit, someOtherPointOnUpperBoundary] and [vSplit, v2, someOtherPointOnUpperBoundary]

        boolean foundTriangle1 = false; // e.g. (v1, vSplit, v4)
        boolean foundTriangle2 = false; // e.g. (vSplit, v2, v4) or (vSplit, v2, v3)

        // Let's check for triangles that have vSplit and one of the original segment endpoints (v1 or v2)
        // and whose third vertex is one of the "top" vertices of the rectangle (v3 or v4).
        // This ensures the edge (v1,vSplit) or (vSplit,v2) is part of the constraint boundary.

        for (Vertex[] triangle : collectedTriangles) {
            List<Vertex> triVerts = List.of(triangle[0], triangle[1], triangle[2]);
            if (triVerts.contains(vSplit)) {
                // Check for triangle involving v1 and vSplit on the boundary
                if (triVerts.contains(v1)) {
                    // The third vertex should make the edge (v1, vSplit) a boundary edge.
                    // In a simple rectangle, this third vertex would be v4 for the triangle (v1, vSplit, v4)
                    // if the diagonal chosen by the TIN is (vSplit,v4).
                    // Or, if the diagonal is (v1,v3), then this test is more complex.
                    // Let's check if the edge (v1,vSplit) is on the constraint boundary.
                    // This means v1 and vSplit are consecutive in the triangle, and the third vertex
                    // is "inside" the constraint relative to this edge.
                    if ( (triVerts.contains(v4) && isEdgeOnBoundary(v1, vSplit, v4, polygonConstraint)) ||
                         (triVerts.contains(v3) && isEdgeOnBoundary(v1, vSplit, v3, polygonConstraint)) // Less likely for this simple case
                    ) {
                        // Check if v1 and vSplit form an edge of the triangle that is also on the constraint boundary.
                        // The third vertex (v4 or v3) determines the orientation.
                        // A simple check for this setup: triangle (v1, vSplit, v4)
                        if(triVerts.contains(v4)){
                             if(containsEdge(triangle, v1, vSplit) && containsEdge(triangle, vSplit, v4) && containsEdge(triangle, v4, v1)){
                                 // This is triangle (v1, vSplit, v4). Is edge (v1,vSplit) on boundary? Yes.
                                 foundTriangle1 = true;
                             }
                        }
                    }
                }
                // Check for triangle involving v2 and vSplit on the boundary
                if (triVerts.contains(v2)) {
                     // Similar logic for the segment (vSplit, v2)
                     // Expected triangle (vSplit, v2, v3) or (vSplit, v2, v4)
                     if(triVerts.contains(v3)){
                         if(containsEdge(triangle, vSplit, v2) && containsEdge(triangle, v2, v3) && containsEdge(triangle, v3, vSplit)){
                            // This is triangle (vSplit, v2, v3). Is edge (vSplit,v2) on boundary? Yes.
                            foundTriangle2 = true;
                         }
                     } else if (triVerts.contains(v4)){ // Alternative triangulation
                          if(containsEdge(triangle, vSplit, v2) && containsEdge(triangle, v2, v4) && containsEdge(triangle, v4, vSplit)){
                            // This is triangle (vSplit, v2, v4). Is edge (vSplit,v2) on boundary? Yes.
                            // This case is more likely if the diagonal is (v2,v4)
                            // Let's re-evaluate which triangle we expect.
                            // Given constraint (1,1)-(5,1)-(5,5)-(1,5)
                            // Split (1,1)-(5,1) at (3,1)
                            // We expect two triangles along the bottom edge:
                            // T1: (1,1), (3,1), (1,5)  -- if diagonal is (3,1)-(1,5)
                            // T2: (3,1), (5,1), (5,5)  -- if diagonal is (3,1)-(5,5)
                            // OR
                            // T1: (1,1), (3,1), (5,5) -- if diagonal is (1,1)-(5,5) and (3,1)-(5,5)
                            // T2: (3,1), (5,1), (1,5) -- if diagonal is (5,1)-(1,5) and (3,1)-(1,5)

                            // Let's focus on the presence of edges (v1, vSplit), (vSplit, v2)
                            // and a third vertex from the "top" of the rectangle (v3 or v4).

                            // Triangle (vSplit, v2, v4)
                            foundTriangle2 = true; // Potentially, based on actual triangulation
                         }
                     }
                }
            }
        }

        // Refined check for the specific expected triangles
        // After splitting (1,1)-(5,1) with (3,1)=vSplit,
        // the original constraint polygon is now effectively composed of two sub-polygons (conceptually)
        // P1: (1,1)-(3,1)-(1,5) and P2: (3,1)-(5,1)-(5,5)-(1,5) (this isn't quite right)
        // The constrained triangles must lie *within* the original rectangle.
        // The common edge for the two expected triangles will be from vSplit to a vertex on the opposite side.
        // Expected triangles:
        // T1: (v1, vSplit, v4)  i.e. ( (1,1,0), (3,1,0), (1,5,0) )
        // T2: (vSplit, v2, v3)  i.e. ( (3,1,0), (5,1,0), (5,5,0) )
        // This assumes the TIN creates diagonals (vSplit, v4) and (vSplit, v3) OR (v1,v3) and (v2,v4) etc.
        // Let's verify based on the edges that MUST form part of the boundary.
        // The edge (v1, vSplit) is a constraint boundary.
        // The edge (vSplit, v2) is a constraint boundary.

        boolean foundV1VSplitBasedTriangle = false;
        boolean foundVSplitV2BasedTriangle = false;

        for (Vertex[] tri : collectedTriangles) {
            if (containsAll(tri, v1, vSplit, v4) ) { // Expected: (1,1), (3,1), (1,5)
                foundV1VSplitBasedTriangle = true;
            }
            if (containsAll(tri, vSplit, v2, v3) ) { // Expected: (3,1), (5,1), (5,5)
                 foundVSplitV2BasedTriangle = true;
            }
             // Alternative triangulation for the second part
            if (!foundVSplitV2BasedTriangle && containsAll(tri, vSplit, v2, v4)) { // (3,1), (5,1), (1,5)
                // This triangle would mean the diagonal is (vSplit, v4) and (v2, v4)
                // This would share edge (vSplit,v4) with (v1,vSplit,v4)
                // And edge (v2,v4) with nothing? This seems wrong.
                // The two triangles along the split edge v1-vSplit-v2 should share an edge starting at vSplit
                // and going to the *opposite* side of the rectangle.
                // So, if T1 is (v1,vSplit,v4), then T2 could be (vSplit,v2,v4) or (vSplit,v2,v3)
                // If T2 is (vSplit,v2,v4), they share edge (vSplit,v4)
                // If T2 is (vSplit,v2,v3), they don't share an edge starting at vSplit to opposite side unless v4=v3 (not true)
            }
        }
        
        // Let's simplify the check:
        // We need one triangle with edge (v1, vSplit) on boundary and another with (vSplit, v2) on boundary.
        // The third vertex of such triangles must be "above" the line v1-v2.
        // So the third vertex must be v3 or v4.

        int countV1VSplit = 0;
        int countVSplitV2 = 0;

        System.out.println("Collected Triangles (" + collectedTriangles.size() + "):");
        for(Vertex[] t : collectedTriangles){
            System.out.println("  - ("+t[0]+"), ("+t[1]+"), ("+t[2]+")");
        }


        for (Vertex[] triangle : collectedTriangles) {
            List<Vertex> triVerts = List.of(triangle[0], triangle[1], triangle[2]);
            if (triVerts.contains(vSplit) && triVerts.contains(v1)) {
                if (triVerts.contains(v4)) { // Triangle (v1, vSplit, v4)
                    countV1VSplit++;
                }
                // Potentially other triangles if the constraint was more complex
            }
            if (triVerts.contains(vSplit) && triVerts.contains(v2)) {
                 if (triVerts.contains(v3)) { // Triangle (vSplit, v2, v3)
                    countVSplitV2++;
                } else if (triVerts.contains(v4)){ // Triangle (vSplit, v2, v4)
                    // This triangle would share vSplit and v4 with (v1, vSplit, v4)
                    // This means the edge (v2,v4) and (v1,v4) are present.
                    // And the split edge gives (v1,vSplit) and (vSplit,v2).
                    // Triangle 1: (v1, vSplit, v4)
                    // Triangle 2: (vSplit, v2, v4)
                    // These two triangles would cover the quadrilateral (v1, v2, v4)
                    // But the constraint is (v1,v2,v3,v4).
                    // The expected triangles are those that form the rectangle.
                    // With vSplit on edge v1-v2, the rectangle is split into two smaller regions.
                    // Region 1: v1-vSplit-v4 (triangle) and vSplit-v3-v4 (triangle), forming quad v1-vSplit-v3-v4
                    // Region 2: vSplit-v2-v3 (triangle)
                    // This does not seem right.

                    // The constraint is a single polygon.
                    // Adding vSplit on edge (v1,v2) means this edge is now two edges: (v1,vSplit) and (vSplit,v2).
                    // The TIN algorithm will then form triangles *inside* this constraint.
                    // The most natural triangulation for the rectangle (1,1)-(5,1)-(5,5)-(1,5)
                    // would be two triangles, e.g. (1,1)-(5,1)-(1,5) and (5,1)-(5,5)-(1,5) by diagonal (5,1)-(1,5)
                    // OR (1,1)-(5,1)-(5,5) and (1,1)-(5,5)-(1,5) by diagonal (1,1)-(5,5)

                    // When vSplit(3,1) is added on (1,1)-(5,1):
                    // The edge (1,1)-(5,1) is now (1,1)-(3,1) and (3,1)-(5,1).
                    // We expect two triangles whose base is on the original bottom edge of the rectangle.
                    // T1: uses edge (v1, vSplit), e.g., (v1, vSplit, v4) = ((1,1), (3,1), (1,5))
                    // T2: uses edge (vSplit, v2), e.g., (vSplit, v2, v3) = ((3,1), (5,1), (5,5))
                    // These two triangles T1 and T2 do NOT cover the whole rectangle.
                    // T1 covers the region defined by (1,1)-(3,1)-(1,5).
                    // T2 covers the region defined by (3,1)-(5,1)-(5,5).
                    // What about the region (3,1)-(1,5)-(5,5)? This is also part of the original rectangle.
                    // This region would be triangle (vSplit, v4, v3) = ( (3,1), (1,5), (5,5) )

                    // So, we expect 3 triangles if vSplit is on the boundary and then triangulated with opposite vertices.
                    // 1. (v1, vSplit, v4)
                    // 2. (vSplit, v2, v3)
                    // 3. (vSplit, v4, v3)

                    // Let's re-check the TriangleCollector documentation or behavior.
                    // It collects all triangles *within* the constraint.
                    // If the TIN triangulates the rectangle (v1,v2,v3,v4) using diagonal (v1,v3), we get:
                    // TriA: (v1,v2,v3)
                    // TriB: (v1,v3,v4)
                    // If vSplit(3,1) is on v1-v2.
                    // TriA is split. Edge (v1,v2) becomes (v1,vSplit) and (vSplit,v2).
                    // So TriA could become:
                    //   (v1, vSplit, v3)
                    //   (vSplit, v2, v3)
                    // TriB (v1,v3,v4) remains.
                    // Total triangles: 3.
                    // ( (1,1), (3,1), (5,5) ), ( (3,1), (5,1), (5,5) ), ( (1,1), (5,5), (1,5) )

                    // If the TIN triangulates using diagonal (v2,v4), we get:
                    // TriA: (v1,v2,v4)
                    // TriB: (v2,v3,v4)
                    // If vSplit(3,1) is on v1-v2.
                    // TriA is split. Edge (v1,v2) becomes (v1,vSplit) and (vSplit,v2).
                    // So TriA could become:
                    //   (v1, vSplit, v4)
                    //   (vSplit, v2, v4)
                    // TriB (v2,v3,v4) remains.
                    // Total triangles: 3.
                    // ( (1,1), (3,1), (1,5) ), ( (3,1), (5,1), (1,5) ), ( (5,1), (5,5), (1,5) )

                    // The key is that the original edge is split.
                    // The triangles that used to share the original edge v1-v2,
                    // will now be replaced by triangles that share v1-vSplit and vSplit-v2.

                    // Let's look for:
                    // 1. A triangle containing v1, vSplit, and some other vertex (v3 or v4).
                    // 2. A triangle containing vSplit, v2, and some other vertex (v3 or v4).
                    // These triangles must have (v1,vSplit) and (vSplit,v2) as constrained edges.

                    if (triVerts.contains(v4)) { // Triangle (vSplit, v2, v4)
                         countVSplitV2++;
                    }
                }
            }
        }

        String assertionMessage = "Expected two triangles along the split constraint edge involving vSplit. "
            + "T1 (v1,vSplit, {v3 or v4}), T2 (vSplit,v2, {v3 or v4}).\n"
            + "Collected " + collectedTriangles.size() + " triangles:\n";
        for(Vertex[] t : collectedTriangles){
            assertionMessage += "  - ("+t[0]+"), ("+t[1]+"), ("+t[2]+")\n";
        }
        assertionMessage += "v1=" + v1 + ", v2=" + v2 + ", v3=" + v3 + ", v4=" + v4 + ", vSplit=" + vSplit + "\n";


        // Assert that we found one triangle for segment v1-vSplit and one for vSplit-v2 on the boundary
        // These triangles will share vSplit and an edge to an opposite vertex (v3 or v4)
        boolean foundTri1 = false; // For (v1, vSplit, X) where X is v3 or v4
        boolean foundTri2 = false; // For (vSplit, v2, Y) where Y is v3 or v4

        // And that these two triangles are distinct and cover the base.
        // Let's list the vertices: v1=(1,1), v2=(5,1), v3=(5,5), v4=(1,5), vSplit=(3,1)

        List<Vertex[]> candidatesV1VSplit = new ArrayList<>();
        List<Vertex[]> candidatesVSplitV2 = new ArrayList<>();

        for(Vertex[] t : collectedTriangles){
            if(containsEdge(t, v1, vSplit) && (containsVertex(t,v3) || containsVertex(t,v4))){
                candidatesV1VSplit.add(t);
                foundTri1 = true;
            }
            if(containsEdge(t, vSplit, v2) && (containsVertex(t,v3) || containsVertex(t,v4))){
                candidatesVSplitV2.add(t);
                foundTri2 = true;
            }
        }
        
        assertTrue(foundTri1, "Did not find a constrained triangle for edge (v1, vSplit). " + assertionMessage);
        assertTrue(foundTri2, "Did not find a constrained triangle for edge (vSplit, v2). " + assertionMessage);

        // Further check: the two triangles found should together cover the part of the
        // rectangle along the original v1-v2 edge.
        // e.g. if T1 = (v1, vSplit, v4) and T2 = (vSplit, v2, v4), they share edge (vSplit, v4)
        // e.g. if T1 = (v1, vSplit, v3) and T2 = (vSplit, v2, v3), they share edge (vSplit, v3)
        // It's also possible T1 = (v1, vSplit, v4) and T2 = (vSplit, v2, v3) and a third T3=(vSplit,v3,v4)

        // For the specific setup (1,1)-(5,1)-(5,5)-(1,5) and vSplit=(3,1)
        // The two triangles should be:
        // T_A = ( (1,1), (3,1), (1,5) )  -- uses v1, vSplit, v4
        // T_B = ( (3,1), (5,1), (1,5) )  -- uses vSplit, v2, v4
        //   OR
        // T_A = ( (1,1), (3,1), (5,5) ) -- uses v1, vSplit, v3
        // T_B = ( (3,1), (5,1), (5,5) ) -- uses vSplit, v2, v3
        //
        // And there will be a third triangle:
        // T_C = ( (1,1), (1,5), (5,5) ) if T_A and T_B are the second pair
        // T_C = ( (5,1), (5,5), (1,5) ) if T_A and T_B are the first pair (this is wrong)

        // The total number of triangles for a convex polygon with N vertices is N-2.
        // Our constraint polygon has 4 vertices. So, initially 2 triangles.
        // Adding vSplit on an edge effectively creates a polygon with 5 vertices if we consider vSplit part of boundary.
        // (v1, vSplit, v2, v3, v4). This would give 5-2 = 3 triangles.
        assertEquals(3, collectedTriangles.size(), "Expected 3 triangles for the split rectangle. " + assertionMessage);

        // Check for specific triangles based on the typical "fan" triangulation from vSplit
        // or from one of the original vertices.
        // If the TIN chose (v1,v3) as original diagonal: (v1,v2,v3) and (v1,v3,v4)
        // vSplit on (v1,v2) modifies (v1,v2,v3) into (v1,vSplit,v3) and (vSplit,v2,v3).
        // So collected should be: (v1,vSplit,v3), (vSplit,v2,v3), (v1,v3,v4)
        boolean combo1Found = false;
        if (containsAll(collectedTriangles, v1, vSplit, v3) &&
            containsAll(collectedTriangles, vSplit, v2, v3) &&
            containsAll(collectedTriangles, v1, v3, v4)) {
            combo1Found = true;
        }

        // If the TIN chose (v2,v4) as original diagonal: (v1,v2,v4) and (v2,v3,v4)
        // vSplit on (v1,v2) modifies (v1,v2,v4) into (v1,vSplit,v4) and (vSplit,v2,v4).
        // So collected should be: (v1,vSplit,v4), (vSplit,v2,v4), (v2,v3,v4)
        boolean combo2Found = false;
        if (containsAll(collectedTriangles, v1, vSplit, v4) &&
            containsAll(collectedTriangles, vSplit, v2, v4) &&
            containsAll(collectedTriangles, v2, v3, v4)) {
            combo2Found = true;
        }
        
        // The actual observed valid triangulation for v1=(1,1), v2=(5,1), v3=(5,5), v4=(1,5), vSplit=(3,1) is:
        // T1: (v1, vSplit, v4)
        // T2: (vSplit, v2, v3)
        // T3: (vSplit, v4, v3)  -- or (vSplit, v3, v4)
        boolean comboObserved = false;
        if (containsAll(collectedTriangles, v1, vSplit, v4) &&
            containsAll(collectedTriangles, vSplit, v2, v3) &&
            containsAll(collectedTriangles, vSplit, v4, v3)) { // Order of v4,v3 for containsAll helper doesn't matter
            comboObserved = true;
        }

        assertTrue(comboObserved, "The collected triangles do not match the expected observed pattern for a split rectangle. "
            + "Expected: [(v1,vSplit,v4), (vSplit,v2,v3), (vSplit,v4,v3)].\n" + assertionMessage);

    }

    // Helper to check if a triangle (array of 3 vertices) contains all specified vertices
    private boolean containsAll(Vertex[] triangle, Vertex... verticesToCheck) {
        if (triangle.length != 3) return false;
        List<Vertex> triVerts = List.of(triangle[0], triangle[1], triangle[2]);
        for (Vertex vCheck : verticesToCheck) {
            boolean found = false;
            for (Vertex tv : triVerts) {
                if (tv.equals(vCheck)) {
                    found = true;
                    break;
                }
            }
            if (!found) return false;
        }
        return true;
    }

    // Helper to check if a list of triangles contains a specific triangle
    private boolean containsAll(List<Vertex[]> triangles, Vertex vA, Vertex vB, Vertex vC) {
        for (Vertex[] tri : triangles) {
            if (containsAll(tri, vA, vB, vC)) {
                return true;
            }
        }
        return false;
    }


    // Helper to check if a triangle contains a specific edge
    private boolean containsEdge(Vertex[] triangle, Vertex vA, Vertex vB) {
        List<Vertex> triVerts = List.of(triangle[0], triangle[1], triangle[2]);
        return triVerts.contains(vA) && triVerts.contains(vB);
    }

    // Helper to check if a triangle contains a specific vertex
    private boolean containsVertex(Vertex[] triangle, Vertex v) {
        return triangle[0].equals(v) || triangle[1].equals(v) || triangle[2].equals(v);
    }


    // This helper might be too complex or not needed if assertions on specific triangles are sufficient
    private boolean isEdgeOnBoundary(Vertex edgeV1, Vertex edgeV2, Vertex thirdVertex, PolygonConstraint constraint) {
        // This is a simplified check. A robust check would involve constraint.isEdgeOnConstraint(edgeV1, edgeV2)
        // and then checking orientation with the third vertex.
        // For this specific rectangular case, if {edgeV1, edgeV2} is one of the 4 initial segments, it's on boundary.
        List<Vertex> cVerts = constraint.getVertices();
        for (int i = 0; i < cVerts.size(); i++) {
            Vertex c1 = cVerts.get(i);
            Vertex c2 = cVerts.get((i + 1) % cVerts.size());
            if ((c1.equals(edgeV1) && c2.equals(edgeV2)) || (c1.equals(edgeV2) && c2.equals(edgeV1))) {
                // Now check if thirdVertex makes it point "inward" or lies on the correct side.
                // This part is tricky without knowing the winding order and full geometry.
                // For now, this simplified check might be enough if we know the expected triangles.
                return true;
            }
        }
        return false;
    }
}
