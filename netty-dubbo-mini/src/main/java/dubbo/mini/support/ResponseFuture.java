package dubbo.mini.support;

import dubbo.mini.remote.RemotingException;

public interface ResponseFuture {

    /**
     * get result.
     *
     * @return result.
     */
    <T> T get() throws RemotingException;

    /**
     * get result with the specified timeout.
     *
     * @param timeoutInMillis timeout.
     * @return result.
     */
    <T> T get(int timeoutInMillis) throws RemotingException;

    /**
     * check is done.
     *
     * @return done or not.
     */
    boolean isDone();

}