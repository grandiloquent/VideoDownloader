package euphoria.psycho.tasks;

public interface HLSDownloadRequestListener {
    void onSubmit(HLSDownloadRequest request);

    void onFinish(HLSDownloadRequest request);
}
