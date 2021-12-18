/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.tinkerpop.gremlin.language.grammar;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.DefaultGraphTraversal;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.structure.util.empty.EmptyGraph;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.apache.tinkerpop.gremlin.process.traversal.AnonymousTraversalSource.traversal;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class VariableVisitorTest {

    private final GraphTraversalSource g = traversal().withEmbedded(EmptyGraph.instance());

    private Object eval(final String query, final GremlinAntlrToJava antlrToLanguage) {
        final GremlinLexer lexer = new GremlinLexer(CharStreams.fromString(query));
        final GremlinParser parser = new GremlinParser(new CommonTokenStream(lexer));
        return antlrToLanguage.visit(parser.queryList());
    }

    private void compare(final Object expected, final Object actual) {
        assertEquals(((DefaultGraphTraversal) expected).asAdmin().getBytecode(),
                ((DefaultGraphTraversal) actual).asAdmin().getBytecode());
    }

    @Test
    public void shouldReplaceVariable() {
        final Map<String,Object> bindings = new HashMap<>();
        bindings.put("xxx", "knows");
        final GremlinAntlrToJava antlr = new GremlinAntlrToJava(bindings);
        compare(g.V().addE("knows"), eval("g.V().addE(xxx)", antlr));
    }

    @Test
    public void shouldFailToReplaceVariable() {
        final GremlinAntlrToJava antlr = new GremlinAntlrToJava();
        try {
            eval("g.V().addE(xxx)", antlr);
            fail("Should have failed to parse without bindings for 'xxx'");
        } catch (UnboundIdentifierException uie) {
            assertEquals("xxx", uie.getVariableName());
        }
    }

}
