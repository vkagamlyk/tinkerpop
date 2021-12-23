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
package org.apache.tinkerpop.gremlin.jsr223;

import org.apache.tinkerpop.gremlin.process.traversal.Traversal;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__;
import org.apache.tinkerpop.gremlin.structure.T;
import org.apache.tinkerpop.gremlin.structure.util.empty.EmptyGraph;
import org.junit.Test;

import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptException;
import javax.script.SimpleBindings;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.junit.Assert.assertEquals;

public class GremlinLangScriptEngineTest {

    private final static GremlinLangScriptEngine scriptEngine = new GremlinLangScriptEngine();
    private final static GraphTraversalSource g = EmptyGraph.instance().traversal();

    static {
        final Bindings globalBindings = new SimpleBindings();
        globalBindings.put("g", g);
        scriptEngine.setBindings(globalBindings, ScriptContext.GLOBAL_SCOPE);
    }

    @Test
    public void shouldEvalGremlinScript() throws ScriptException {
        final Object result = scriptEngine.eval("g.V()");
        assertThat(result, instanceOf(Traversal.Admin.class));
        assertEquals(g.V().asAdmin().getBytecode(), ((Traversal.Admin) result).getBytecode());
    }

    @Test
    public void shouldEvalGremlinScriptWithVariables() throws ScriptException {
        final Bindings b = new SimpleBindings();
        b.put("int0", 0);
        b.put("dec0", 0.0d);
        b.put("string0", "knows");
        b.put("string1", "created");
        b.put("bool0", true);
        b.put("t0", T.id);

        final Date d = new Date();
        b.put("date0", d);

        final List<String> l = new ArrayList<>();
        l.add("yes");
        l.add("no");
        b.put("list0", l);

        final Map<String,Object> m = new HashMap<>();
        m.put("x", 1);
        m.put("y", 2);
        b.put("map0", m);

        final Object result = scriptEngine.eval("g.inject(bool0, date0, map0, int0, dec0, list0).V().out(string0).in(string1).project('tid')", b);
        assertThat(result, instanceOf(Traversal.Admin.class));
        assertEquals(g.inject(true, d, m, 0, 0.0d, l).
                       V().
                       out("knows").in("created").
                       project("tid").asAdmin().getBytecode(),
                     ((Traversal.Admin) result).getBytecode());
    }

    @Test
    public void shouldEvalGremlinBytecode() throws ScriptException {
        final Object result = scriptEngine.eval(g.V().asAdmin().getBytecode(), "g");
        assertThat(result, instanceOf(Traversal.Admin.class));
        assertEquals(g.V().asAdmin().getBytecode(), ((Traversal.Admin) result).getBytecode());
    }
}
