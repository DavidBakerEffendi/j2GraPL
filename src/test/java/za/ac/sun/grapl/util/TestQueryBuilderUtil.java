package za.ac.sun.grapl.util;

import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import za.ac.sun.grapl.domain.enums.EdgeLabels;
import za.ac.sun.grapl.domain.enums.VertexLabels;

public class TestQueryBuilderUtil {

    public static GraphTraversal<Vertex, Vertex> buildASTRepeat(GraphTraversalSource g, EdgeLabels edge, Vertex rootVertex) {
        return g.V(rootVertex).repeat(__.out(edge.toString())).emit();
    }

    public static GraphTraversal<Vertex, Vertex> getVertexAlongEdge(GraphTraversalSource g, EdgeLabels edge, Vertex rootVertex, VertexLabels label, String key, String value) {
        return buildASTRepeat(g, edge, rootVertex).has(label.toString(), key, value);
    }

}
