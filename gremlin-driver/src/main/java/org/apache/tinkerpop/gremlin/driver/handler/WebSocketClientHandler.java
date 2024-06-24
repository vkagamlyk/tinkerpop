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
package org.apache.tinkerpop.gremlin.driver.handler;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.websocketx.PingWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshaker13;
import io.netty.handler.codec.http.websocketx.WebSocketClientProtocolHandler;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshaker;
import io.netty.handler.codec.http.websocketx.WebSocketVersion;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.ssl.SslHandshakeCompletionEvent;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.concurrent.Promise;

import java.net.URI;
import java.util.concurrent.TimeoutException;

import javax.net.ssl.SSLHandshakeException;

import org.apache.tinkerpop.gremlin.driver.Channelizer;
import org.apache.tinkerpop.gremlin.driver.Cluster;
import org.apache.tinkerpop.gremlin.driver.HandshakeInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Wrapper over {@link WebSocketClientProtocolHandler}. This wrapper provides a future which represents the termination
 * of a WS handshake (both success and failure).
 */
public final class WebSocketClientHandler extends WebSocketClientProtocolHandler {
    private static final Logger logger = LoggerFactory.getLogger(WebSocketClientHandler.class);

    private final long connectionSetupTimeoutMillis;
    private ChannelPromise handshakeFuture;
    private boolean sslHandshakeCompleted;
    private boolean useSsl;

    public WebSocketClientHandler(final WebSocketClientHandshaker handshaker, final long timeoutMillis, final boolean useSsl) {
        super(handshaker, /*handleCloseFrames*/true, /*dropPongFrames*/true, timeoutMillis);
        this.connectionSetupTimeoutMillis = timeoutMillis;
        this.useSsl = useSsl;
    }

    public ChannelFuture handshakeFuture() {
        return handshakeFuture;
    }

    @Override
    public void handlerAdded(final ChannelHandlerContext ctx) {
        super.handlerAdded(ctx);
        handshakeFuture = ctx.newPromise();
    }

    @Override
    public void exceptionCaught(final ChannelHandlerContext ctx, final Throwable cause) throws Exception {
        if (!handshakeFuture.isDone()) {
            handshakeFuture.setFailure(cause);
        }

        // let the GremlinResponseHandler take care of exception logging, channel closing, and cleanup
        ctx.fireExceptionCaught(cause);
    }

    @Override
    public void channelInactive(final ChannelHandlerContext ctx) throws Exception {
        if (!handshakeFuture.isDone()) {
            // channel was closed before the handshake could be completed.
            handshakeFuture.setFailure(
                    new RuntimeException(String.format("WebSocket channel=[%s] closed before the handshake could complete." +
                                    " Server logs could contain the reason for abrupt connection disconnect or the " +
                                    "server might not be reachable from the client anymore.",
                            ctx.channel().id().asShortText())));
        }

        super.channelInactive(ctx);
    }

    @Override
    public void userEventTriggered(final ChannelHandlerContext ctx, final Object event) throws Exception {
        if (event instanceof IdleStateEvent) {
            IdleStateEvent e = (IdleStateEvent) event;
            if (e.state() == IdleState.READER_IDLE) {
                logger.warn("WebSocket connection {} has been idle for too long.", ctx.channel());
            } else if (e.state() == IdleState.WRITER_IDLE) {
                logger.debug("Sending ping frame to the server");
                ctx.writeAndFlush(new PingWebSocketFrame());
            }
        } else if (ClientHandshakeStateEvent.HANDSHAKE_COMPLETE.equals(event)) {
            if (!handshakeFuture.isDone()) {
                handshakeFuture.setSuccess();
            }
        } else if (ClientHandshakeStateEvent.HANDSHAKE_TIMEOUT.equals(event)) {
            if (!handshakeFuture.isDone()) {
                TimeoutException te = new TimeoutException(
                        String.format((useSsl && !sslHandshakeCompleted) ?
                                        "SSL handshake not completed in stipulated time=[%s]ms" :
                                        "WebSocket handshake not completed in stipulated time=[%s]ms",
                                connectionSetupTimeoutMillis));
                handshakeFuture.setFailure(te);
                logger.error(te.getMessage());
            }

            if (useSsl && !sslHandshakeCompleted) {
                SslHandler handler = ((SslHandler) ctx.pipeline().get(Channelizer.AbstractChannelizer.PIPELINE_SSL_HANDLER));
                ((Promise<Channel>) handler.handshakeFuture()).tryFailure(new SSLHandshakeException("SSL handshake timed out."));
            }
        } else if (event instanceof SslHandshakeCompletionEvent) {
            sslHandshakeCompleted = true;
        } else {
            super.userEventTriggered(ctx, event);
        }
    }

    /**
     * Extension to the Netty implementation that allows for the {@link #newHandshakeRequest()} to be modified by way
     * of a {@link HandshakeInterceptor} that is supplied to the {@link Cluster} when it is created.
     */
    public static class InterceptedWebSocketClientHandshaker13 extends WebSocketClientHandshaker13 {

        private final HandshakeInterceptor interceptor;

        public InterceptedWebSocketClientHandshaker13(final URI webSocketURL, final WebSocketVersion version,
                                                      final String subprotocol, final boolean allowExtensions,
                                                      final HttpHeaders customHeaders, final int maxFramePayloadLength,
                                                      final boolean performMasking, final boolean allowMaskMismatch,
                                                      final long forceCloseTimeoutMillis, final HandshakeInterceptor interceptor) {
            super(webSocketURL, version, subprotocol, allowExtensions, customHeaders, maxFramePayloadLength,
                    performMasking, allowMaskMismatch, forceCloseTimeoutMillis);
            this.interceptor = interceptor;
        }

        @Override
        protected FullHttpRequest newHandshakeRequest() {
            return this.interceptor.apply(super.newHandshakeRequest());
        }
    }
}
