package com.tinkerpop.gremlin.structure.io;

import com.tinkerpop.gremlin.structure.Direction;
import com.tinkerpop.gremlin.structure.Edge;
import com.tinkerpop.gremlin.structure.Vertex;
import com.tinkerpop.gremlin.util.function.QuintFunction;

import java.io.IOException;
import java.io.InputStream;
import java.util.function.BiFunction;

/**
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
public interface GraphReader {

    public void readGraph(final InputStream inputStream) throws IOException;

    public Vertex readVertex(final InputStream inputStream, final Direction direction, final BiFunction<Object, Object[], Vertex> vertexMaker) throws IOException;

    public Vertex readVertex(final InputStream inputStream, final BiFunction<Object, Object[], Vertex> vertexMaker) throws IOException;  // only reads the vertex/properties, no edges

    public Edge readEdge(final InputStream inputStream, final QuintFunction<Object, Object, Object, String, Object[], Edge> edgeMaker) throws IOException;

}
