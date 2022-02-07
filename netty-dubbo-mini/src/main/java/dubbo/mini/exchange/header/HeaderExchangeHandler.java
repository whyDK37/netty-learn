/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package dubbo.mini.exchange.header;


import dubbo.mini.common.Constants;
import dubbo.mini.common.NetURL;
import dubbo.mini.common.utils.NetUtils;
import dubbo.mini.common.utils.StringUtils;
import dubbo.mini.exchange.ExchangeChannel;
import dubbo.mini.exchange.ExchangeHandler;
import dubbo.mini.exchange.Request;
import dubbo.mini.exchange.Response;
import dubbo.mini.remote.ChannelEventHandler;
import dubbo.mini.remote.ChannelHandlerDelegate;
import dubbo.mini.remote.ExecutionException;
import dubbo.mini.remote.NetChannel;
import dubbo.mini.remote.RemotingException;
import dubbo.mini.support.DefaultFuture;
import java.net.InetSocketAddress;
import java.util.concurrent.CompletableFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * ExchangeReceiver 它是一个 ChannelHandlerDelegate ！！！
 */
public class HeaderExchangeHandler implements ChannelHandlerDelegate {

  protected static final Logger logger = LoggerFactory.getLogger(HeaderExchangeHandler.class);

  public static final String KEY_READ_TIMESTAMP = HeartbeatHandler.KEY_READ_TIMESTAMP;

  public static final String KEY_WRITE_TIMESTAMP = HeartbeatHandler.KEY_WRITE_TIMESTAMP;

  private final ExchangeHandler handler;

  public HeaderExchangeHandler(ExchangeHandler handler) {
    if (handler == null) {
      throw new IllegalArgumentException("handler == null");
    }
    this.handler = handler;
  }

  /**
   * 处理响应
   *
   * @param channel
   * @param response
   * @throws RemotingException
   */
  static void handleResponse(NetChannel channel, Response response) throws RemotingException {
    if (response != null && !response.isHeartbeat()) {
      DefaultFuture.received(channel, response);
    }
  }

  private static boolean isClientSide(NetChannel channel) {
    InetSocketAddress address = channel.getRemoteAddress();
    NetURL url = channel.getUrl();
    return url.getPort() == address.getPort() &&
        NetUtils.filterLocalHost(url.getIp())
            .equals(NetUtils.filterLocalHost(address.getAddress().getHostAddress()));
  }

  /**
   * 客户端接收到 READONLY_EVENT 事件请求，进行记录到通道。后续，不再向该服务器，发送新的请求。
   *
   * @param channel
   * @param req
   */
  void handlerEvent(NetChannel channel, Request req) {
    if (req.getData() != null && req.getData().equals(Request.READONLY_EVENT)) {
      channel.setAttribute(Constants.CHANNEL_ATTRIBUTE_READONLY_KEY, Boolean.TRUE);
    }
  }

  /**
   * 处理请求
   *
   * @param channel
   * @param req
   * @throws RemotingException
   */
  void handleRequest(final ExchangeChannel channel, Request req) throws RemotingException {
    Response res = new Response(req.getId());
    // 请求无法解析，返回 BAD_REQUEST 响应
    if (req.isBroken()) {
      Object data = req.getData();

      String msg;
      if (data == null) {
        msg = null;
      } else if (data instanceof Throwable) {
        msg = StringUtils.toString((Throwable) data);
      } else {
        msg = data.toString();
      }
      res.setErrorMessage("Fail to decode request due to: " + msg);
      res.setStatus(Response.BAD_REQUEST);

      channel.send(res);
      return;
    }
    // find handler by message class.
    // 使用 ExchangeHandler 处理，并返回响应
    // 调用 ExchangeHandler#reply(channel, message) 方法，返回结果，并设置到响应( Response) 最终返回。
    Object msg = req.getData();
    try {
      // handle data.
      CompletableFuture<Object> future = handler.reply(channel, msg);
      if (future.isDone()) {
        res.setStatus(Response.OK);
        res.setResult(future.get());
        channel.send(res);
        return;
      }
      future.whenComplete((result, t) -> {
        try {
          if (t == null) {
            res.setStatus(Response.OK);
            res.setResult(result);
          } else {
            res.setStatus(Response.SERVICE_ERROR);
            res.setErrorMessage(StringUtils.toString(t));
          }
          channel.send(res);
        } catch (RemotingException e) {
          logger.warn("Send result to consumer failed, channel is " + channel + ", msg is " + e);
        } finally {
          // HeaderExchangeChannel.removeChannelIfDisconnected(channel);
        }
      });
    } catch (Throwable e) {
      res.setStatus(Response.SERVICE_ERROR);
      res.setErrorMessage(StringUtils.toString(e));
      channel.send(res);
    }
  }

  @Override
  public void connected(NetChannel channel) throws RemotingException {
    channel.setAttribute(KEY_READ_TIMESTAMP, System.currentTimeMillis());
    channel.setAttribute(KEY_WRITE_TIMESTAMP, System.currentTimeMillis());
    ExchangeChannel exchangeChannel = HeaderExchangeChannel.getOrAddChannel(channel);
    try {
      handler.connected(exchangeChannel);
    } finally {
      HeaderExchangeChannel.removeChannelIfDisconnected(channel);
    }
  }

  @Override
  public void disconnected(NetChannel channel) throws RemotingException {
    channel.setAttribute(KEY_READ_TIMESTAMP, System.currentTimeMillis());
    channel.setAttribute(KEY_WRITE_TIMESTAMP, System.currentTimeMillis());
    ExchangeChannel exchangeChannel = HeaderExchangeChannel.getOrAddChannel(channel);
    try {
      handler.disconnected(exchangeChannel);
    } finally {
      DefaultFuture.closeChannel(channel);
      HeaderExchangeChannel.removeChannelIfDisconnected(channel);
    }
  }

  @Override
  public void sent(NetChannel channel, Object message) throws RemotingException {
    Throwable exception = null;
    try {
      channel.setAttribute(KEY_WRITE_TIMESTAMP, System.currentTimeMillis());
      ExchangeChannel exchangeChannel = HeaderExchangeChannel.getOrAddChannel(channel);
      try {
        handler.sent(exchangeChannel, message);
      } finally {
        HeaderExchangeChannel.removeChannelIfDisconnected(channel);
      }
    } catch (Throwable t) {
      exception = t;
    }
    if (message instanceof Request) {
      Request request = (Request) message;
      DefaultFuture.sent(channel, request);
    }
    if (exception != null) {
      if (exception instanceof RuntimeException) {
        throw (RuntimeException) exception;
      } else if (exception instanceof RemotingException) {
        throw (RemotingException) exception;
      } else {
        throw new RemotingException(channel.getLocalAddress(), channel.getRemoteAddress(),
            exception.getMessage(), exception);
      }
    }
  }

  @Override
  public void received(NetChannel channel, Object message) throws RemotingException {
    // 设置最后的读时间
    channel.setAttribute(KEY_READ_TIMESTAMP, System.currentTimeMillis());
    // 创建 ExchangeChannel 对象
    final ExchangeChannel exchangeChannel = HeaderExchangeChannel.getOrAddChannel(channel);
    try {
      if (message instanceof Request) {
        // handle request.
        Request request = (Request) message;
        // 调用 #handlerEvent(channel, request) 方法，处理事件请求。
        if (request.isEvent()) {
          handlerEvent(channel, request);
        } else {
          // 调用 #handleRequest(channel, request) 方法，处理普通请求（需要响应），并将响应写回请求方。
          if (request.isTwoWay()) {
            handleRequest(exchangeChannel, request);
          } else {
            // 提交给装饰的 `handler`，继续处理
            // 调用 ChannelHandler#received(channel, message) 方法，处理普通请求（无需响应）。
            handler.received(exchangeChannel, request.getData());
          }
        }
      } else if (message instanceof Response) {
        handleResponse(channel, (Response) message);
      } else {
        handler.received(exchangeChannel, message);
      }
    } finally {
      HeaderExchangeChannel.removeChannelIfDisconnected(channel);
    }
  }

  @Override
  public void caught(NetChannel channel, Throwable exception) throws RemotingException {
    // 当发生 ExecutionException 异常，返回异常响应( Response )
    if (exception instanceof ExecutionException) {
      ExecutionException e = (ExecutionException) exception;
      Object msg = e.getRequest();
      if (msg instanceof Request) {
        Request req = (Request) msg;
        if (req.isTwoWay() && !req.isHeartbeat()) {
          Response res = new Response(req.getId());
          res.setStatus(Response.SERVER_ERROR);
          res.setErrorMessage(StringUtils.toString(e));
          channel.send(res);
          return;
        }
      }
    }
    // 创建 ExchangeChannel 对象
    ExchangeChannel exchangeChannel = HeaderExchangeChannel.getOrAddChannel(channel);
    try {
      // 提交给装饰的 `handler`，继续处理
      handler.caught(exchangeChannel, exception);
    } finally {
      // 移除 ExchangeChannel 对象，若已断开
      HeaderExchangeChannel.removeChannelIfDisconnected(channel);
    }
  }

  @Override
  public ChannelEventHandler getHandler() {
    if (handler instanceof ChannelHandlerDelegate) {
      return ((ChannelHandlerDelegate) handler).getHandler();
    } else {
      return handler;
    }
  }
}
