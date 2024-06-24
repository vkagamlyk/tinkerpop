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

package org.apache.tinkerpop.gremlin.process.traversal.step.map;

import org.apache.tinkerpop.gremlin.LoadGraphWith;
import org.apache.tinkerpop.gremlin.process.AbstractGremlinProcessTest;
import org.apache.tinkerpop.gremlin.process.traversal.Order;
import org.apache.tinkerpop.gremlin.process.traversal.Traversal;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.apache.tinkerpop.gremlin.LoadGraphWith.GraphData.MODERN;
import static org.junit.Assert.assertEquals;

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
public abstract class ProjectTest extends AbstractGremlinProcessTest {

    public abstract Traversal<Vertex, Map<String, Number>> get_g_V_hasLabelXpersonX_projectXa_bX_byXoutE_countX_byXageX();

    public abstract Traversal<Vertex, Map<String, Number>> get_g_V_projectXa_bX_byXinE_countX_byXageX();

    public abstract Traversal<Vertex, String> get_g_V_outXcreatedX_projectXa_bX_byXnameX_byXinXcreatedX_countX_order_byXselectXbX__descX_selectXaX();

    public abstract Traversal<Vertex, Map<String, Object>> get_g_V_valueMap_projectXxX_byXselectXnameXX();

    @Test
    @LoadGraphWith(MODERN)
    public void g_V_hasLabelXpersonX_projectXa_bX_byXoutE_countX_byXageX() {
        final Traversal<Vertex, Map<String, Number>> traversal = get_g_V_hasLabelXpersonX_projectXa_bX_byXoutE_countX_byXageX();
        printTraversalForm(traversal);
        checkResults(makeMapList(2,
                "a", 3L, "b", 29,
                "a", 0L, "b", 27,
                "a", 2L, "b", 32,
                "a", 1L, "b", 35), traversal);
    }

    @Test
    @LoadGraphWith(MODERN)
    public void g_V_projectXa_bX_byXinE_countX_byXageX() {
        final Traversal<Vertex, Map<String, Number>> traversal = get_g_V_projectXa_bX_byXinE_countX_byXageX();
        printTraversalForm(traversal);

        final List<Map<String, Number>> a = new ArrayList<>();
        a.add(new HashMap<String,Number>() {{
            put("a", 3L);
        }});
        a.add(new HashMap<String,Number>() {{
            put("a", 1L);
        }});
        a.add(new HashMap<String,Number>() {{
            put("a", 0L);
            put("b", 29);
        }});
        a.add(new HashMap<String,Number>() {{
            put("a", 1L);
            put("b", 27);
        }});
        a.add(new HashMap<String,Number>() {{
            put("a", 1L);
            put("b", 32);
        }});
        a.add(new HashMap<String,Number>() {{
            put("a", 0L);
            put("b", 35);
        }});

        checkResults(a, traversal);
    }

    @Test
    @LoadGraphWith(MODERN)
    public void g_V_outXcreatedX_projectXa_bX_byXnameX_byXinXcreatedX_countX_order_byXselectXbX__descX_selectXaX() {
        final Traversal<Vertex, String> traversal = get_g_V_outXcreatedX_projectXa_bX_byXnameX_byXinXcreatedX_countX_order_byXselectXbX__descX_selectXaX();
        printTraversalForm(traversal);
        final List<String> names = traversal.toList();
        assertEquals(4, names.size());
        assertEquals("lop", names.get(0));
        assertEquals("lop", names.get(1));
        assertEquals("lop", names.get(2));
        assertEquals("ripple", names.get(3));
    }

    @Test
    @LoadGraphWith(MODERN)
    public void g_V_valueMap_projectXxX_byXselectXnameXX() {
        final Traversal<Vertex, Map<String, Object>> traversal = get_g_V_valueMap_projectXxX_byXselectXnameXX();
        printTraversalForm(traversal);
        checkResults(makeMapList(1,
                "x", Collections.singletonList("marko"),
                "x", Collections.singletonList("vadas"),
                "x", Collections.singletonList("lop"),
                "x", Collections.singletonList("josh"),
                "x", Collections.singletonList("ripple"),
                "x", Collections.singletonList("peter")), traversal);
    }

    public static class Traversals extends ProjectTest {

        @Override
        public Traversal<Vertex, Map<String, Number>> get_g_V_hasLabelXpersonX_projectXa_bX_byXoutE_countX_byXageX() {
            return g.V().hasLabel("person").<Number>project("a", "b").by(__.outE().count()).by("age");
        }

        @Override
        public Traversal<Vertex, Map<String, Number>> get_g_V_projectXa_bX_byXinE_countX_byXageX() {
            return g.V().<Number>project("a", "b").by(__.inE().count()).by("age");
        }

        @Override
        public Traversal<Vertex, String> get_g_V_outXcreatedX_projectXa_bX_byXnameX_byXinXcreatedX_countX_order_byXselectXbX__descX_selectXaX() {
            return g.V().out("created").project("a", "b").by("name").by(__.in("created").count()).order().by(__.select("b"), Order.desc).select("a");
        }

        @Override
        public Traversal<Vertex, Map<String, Object>> get_g_V_valueMap_projectXxX_byXselectXnameXX() {
            return g.V().valueMap().project("x").by(__.select("name"));
        }
    }
}
