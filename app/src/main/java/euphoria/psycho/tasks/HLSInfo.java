package euphoria.psycho.tasks;

public class HLSInfo {


    private String mFileName;
    private String mContent;
    public HLSInfo(String fileName, String content) {
        mFileName = fileName;
        mContent = content;
    }

    public String getContent() {
        return mContent;
    }

    public void setContent(String content) {
        mContent = content;
    }

    public String getFileName() {
        return mFileName;
    }

    public void setFileName(String fileName) {
        mFileName = fileName;
    }

}
