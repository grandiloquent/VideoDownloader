package euphoria.psycho.explorer;

public interface ClientInterface {
    void onVideoUrl(String uri);

    boolean shouldOverrideUrlLoading(String uri);
}
