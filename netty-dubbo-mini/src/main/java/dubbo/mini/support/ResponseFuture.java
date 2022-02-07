package dubbo.mini.support;

import dubbo.mini.remote.RemotingException;

public interface ResponseFuture {

  <T> T get() throws RemotingException;


  <T> T get(int timeoutInMillis) throws RemotingException;

  boolean isDone();

}