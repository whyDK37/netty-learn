package dubbo.mini.common;

public interface Node {

    /**
     * get url.
     *
     * @return url.
     */
    NetURL getUrl();

    /**
     * is available.
     *
     * @return available.
     */
    boolean isAvailable();

    /**
     * destroy.
     */
    void destroy();

}