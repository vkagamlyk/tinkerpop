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
package org.apache.tinkerpop.gremlin.server.channel;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.apache.tinkerpop.gremlin.server.Channelizer;

public interface TestChannelizer extends Channelizer {
    public ChannelHandlerContext getMostRecentChannelHandlerContext();

    @ChannelHandler.Sharable
    class ContextHandler extends ChannelInboundHandlerAdapter {

        private ChannelHandlerContext ctx;

        @Override
        public void channelRead(final ChannelHandlerContext ctx, final Object msg) throws Exception {
            super.channelRead(ctx, msg);
            this.ctx = ctx;
        }

        @Override
        public void userEventTriggered(final ChannelHandlerContext ctx, final Object evt) throws Exception {
            super.userEventTriggered(ctx, evt);
            this.ctx = ctx;
        }

        public ChannelHandlerContext getMostRecentChannelHandlerContext() {
            return this.ctx;
        }
    }
}


