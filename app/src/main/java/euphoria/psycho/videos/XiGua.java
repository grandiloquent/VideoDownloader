package euphoria.psycho.videos;

import euphoria.psycho.explorer.MainActivity;

public class XiGua extends BaseExtractor<String> {
    protected XiGua(String inputUri, MainActivity mainActivity) {
        super(inputUri, mainActivity);
    }

    @Override
    protected String fetchVideoUri(String uri) {
        return null;
    }

    @Override
    protected void processVideo(String videoUri) {
    }
}

