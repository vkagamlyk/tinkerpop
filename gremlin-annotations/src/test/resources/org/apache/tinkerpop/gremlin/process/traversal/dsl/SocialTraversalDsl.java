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
package org.apache.tinkerpop.gremlin.process.traversal.dsl;

import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.apache.tinkerpop.gremlin.process.traversal.P;

/**
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
@GremlinDsl
public interface SocialTraversalDsl<S, E> extends GraphTraversal.Admin<S, E> {
    public default GraphTraversal<S, Vertex> knows(final String personName) {
        return ((SocialTraversalDsl) out("knows")).person().has("name", personName);
    }

    public default <E2 extends Number> GraphTraversal<S, E2> meanAgeOfFriends() {
        return ((SocialTraversalDsl) out("knows")).person().values("age").mean();
    }

    @GremlinDsl.AnonymousMethod(returnTypeParameters = {"A", "A"}, methodTypeParameters = {"A"})
    public default GraphTraversal<S, E> person() {
        return hasLabel("person");
    }

    @GremlinDsl.AnonymousMethod(returnTypeParameters = {"A", "org.apache.tinkerpop.gremlin.structure.Vertex"}, methodTypeParameters = {"A"})
    public default GraphTraversal<S, Vertex> knowsOverride(final String personName) {
        return out("knows").hasLabel("person").has("name", personName);
    }

    @GremlinDsl.AnonymousMethod(returnTypeParameters = {"A", "E2"}, methodTypeParameters = {"A", "E2 extends java.lang.Number"})
    public default <E2 extends Number> GraphTraversal<S, E2> meanAgeOfFriendsOverride() {
        return out("knows").hasLabel("person").values("age").mean();
    }
}
