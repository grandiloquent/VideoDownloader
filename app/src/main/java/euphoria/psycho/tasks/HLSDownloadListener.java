package euphoria.psycho.tasks;
public interface HLSDownloadListener{
    void onSubmit(HLSDownloadRequest request);

    void onFinish(HLSDownloadRequest request);
}
