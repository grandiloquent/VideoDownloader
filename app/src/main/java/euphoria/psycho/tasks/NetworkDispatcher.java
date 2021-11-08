package euphoria.psycho.tasks;

import android.os.Process;

import java.util.concurrent.BlockingQueue;

public class NetworkDispatcher extends Thread {
    private final BlockingQueue<HLSDownloadRequest> mQueue;
    private volatile boolean mQuit = false;

    public NetworkDispatcher(BlockingQueue<HLSDownloadRequest> queue) {
        mQueue = queue;
    }//

    public void quit() {
        mQuit = true;
        interrupt();
    }

    private void processRequest() throws InterruptedException {
        // Take a request from the queue.
        HLSDownloadRequest request = mQueue.take();
        processRequest(request);
    }

    private void processRequest(HLSDownloadRequest request) {
        request.sendEvent(RequestQueue.RequestEvent.REQUEST_NETWORK_DISPATCH_STARTED);
        try {
            request.start();
        } catch (Exception e) {
        } finally {
            request.sendEvent(RequestQueue.RequestEvent.REQUEST_NETWORK_DISPATCH_FINISHED);
        }
    }

    @Override
    public void run() {
        Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
        while (true) {
            try {
                processRequest();
            } catch (InterruptedException e) {
                // We may have been interrupted because it was time to quit.
                if (mQuit) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }
    }


}
