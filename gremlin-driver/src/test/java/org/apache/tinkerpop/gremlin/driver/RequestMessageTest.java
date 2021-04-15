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
package org.apache.tinkerpop.gremlin.driver;

import org.apache.tinkerpop.gremlin.driver.message.RequestMessage;
import org.apache.tinkerpop.gremlin.driver.remote.DriverRemoteConnection;
import org.apache.tinkerpop.gremlin.driver.ser.GraphSONMessageSerializerV3d0;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.structure.io.IoRegistry;
import org.apache.tinkerpop.gremlin.structure.io.graphson.GraphSONMapper;
import org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerIoRegistryV3d0;
import org.junit.Assert;
import org.junit.Test;

import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.apache.tinkerpop.gremlin.process.traversal.AnonymousTraversalSource.traversal;
import static org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__.both;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

/**
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
public class RequestMessageTest {

    @Test
    public void shouldOverrideRequest() {
        final UUID request = UUID.randomUUID();
        final RequestMessage msg = RequestMessage.build("op").overrideRequestId(request).create();
        assertEquals(request, msg.getRequestId());
    }

    @Test
    public void shouldSetProcessor() {
        final RequestMessage msg = RequestMessage.build("op").processor("ppp").create();
        assertEquals("ppp", msg.getProcessor());
    }

    @Test
    public void shouldSetOpWithDefaults() {
        final RequestMessage msg = RequestMessage.build("op").create();
        Assert.assertEquals("", msg.getProcessor());    // standard op processor
        assertNotNull(msg.getRequestId());
        assertEquals("op", msg.getOp());
        assertNotNull(msg.getArgs());
    }

    @Test
    public void shouldReturnEmptyOptionalArg() {
        final RequestMessage msg = RequestMessage.build("op").create();
        assertFalse(msg.optionalArgs("test").isPresent());
    }

    @Test
    public void shouldReturnArgAsOptional() {
        final RequestMessage msg = RequestMessage.build("op").add("test", "testing").create();
        assertEquals("testing", msg.optionalArgs("test").get());
    }

    @Test
    public void test() {
        int port = Integer.parseInt(System.getProperty("port", "8182"));

        MessageSerializer serializer = new GraphSONMessageSerializerV3d0();

        /**
         * There typically needs to be only one Cluster instance in an application.
         */
        Cluster cluster = Cluster.build().port(port).serializer(serializer).create();

        /**
         * Construct a remote GraphTraversalSource using the above created Cluster instance that will connect to Gremlin
         * Server.
         */
        GraphTraversalSource g = traversal().withRemote(DriverRemoteConnection.using(cluster));


        List<Integer> list = new LinkedList<>();
        for (int i = 0; i < 1; i++)
        {
            list.add(i);
        }
        list.parallelStream().map(i -> longRunning(g)).collect(Collectors.toList());


        //long count = g.V().count().next();
        //assertEquals(100, count);
    }

    private int longRunning(GraphTraversalSource g) {
        try{
            g.V().repeat(both()).times(3).path().limit(100000000).count().next();
        } catch(Exception e){

        }
        return 1;
    }
}
