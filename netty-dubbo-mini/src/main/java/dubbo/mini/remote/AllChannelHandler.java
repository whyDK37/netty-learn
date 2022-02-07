package dubbo.mini.remote;

import dubbo.mini.common.NetURL;
import dubbo.mini.dispatcher.ChannelEventRunnable;
import dubbo.mini.exchange.Request;
import dubbo.mini.exchange.Response;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;

public class AllChannelHandler extends WrappedChannelHandler {

  public AllChannelHandler(ChannelEventHandler handler, NetURL url) {
    super(handler, url);
  }

  @Override
  public void connected(NetChannel channel) throws RemotingException {
    ExecutorService executor = getExecutorService();
    try {
      executor.execute(
          new ChannelEventRunnable(channel, handler, ChannelEventRunnable.ChannelState.CONNECTED));
    } catch (Throwable t) {
      throw new ExecutionException("connect event", channel,
          getClass() + " error when process connected event .", t);
    }
  }

  @Override
  public void disconnected(NetChannel channel) throws RemotingException {
    ExecutorService executor = getExecutorService();
    try {
      executor.execute(new ChannelEventRunnable(channel, handler,
          ChannelEventRunnable.ChannelState.DISCONNECTED));
    } catch (Throwable t) {
      throw new ExecutionException("disconnect event", channel,
          getClass() + " error when process disconnected event .", t);
    }
  }

  @Override
  public void received(NetChannel channel, Object message) throws RemotingException {
    ExecutorService executor = getExecutorService();
    try {
      executor.execute(
          new ChannelEventRunnable(channel, handler, ChannelEventRunnable.ChannelState.RECEIVED,
              message));
    } catch (Throwable t) {
      //TODO A temporary solution to the problem that the exception information can not be sent to the opposite end after the thread pool is full. Need a refactoring
      //fix The thread pool is full, refuses to call, does not return, and causes the consumer to wait for time out
      if (message instanceof Request && t instanceof RejectedExecutionException) {
        Request request = (Request) message;
        if (request.isTwoWay()) {
          String msg = "Server side(" + url.getIp() + "," + url.getPort()
              + ") threadpool is exhausted ,detail msg:" + t.getMessage();
          Response response = new Response(request.getId());
          response.setStatus(Response.SERVER_THREADPOOL_EXHAUSTED_ERROR);
          response.setErrorMessage(msg);
          channel.send(response);
          return;
        }
      }
      throw new ExecutionException(message, channel,
          getClass() + " error when process received event .", t);
    }
  }

  @Override
  public void caught(NetChannel channel, Throwable exception) throws RemotingException {
    ExecutorService executor = getExecutorService();
    try {
      executor.execute(
          new ChannelEventRunnable(channel, handler, ChannelEventRunnable.ChannelState.CAUGHT,
              exception));
    } catch (Throwable t) {
      throw new ExecutionException("caught event", channel,
          getClass() + " error when process caught event .", t);
    }
  }
}
