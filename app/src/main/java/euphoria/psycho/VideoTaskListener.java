package euphoria.psycho;

public interface VideoTaskListener {
    void synchronizeTask(VideoTask videoTask);

    void taskStarted(VideoTask videoTask);
}
