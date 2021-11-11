package euphoria.psycho.explorer;

public class Native {

    static {
        System.loadLibrary("hello-libs");
    }

    public native static boolean deleteDirectory(String dir);

    public native static String[] fetch91Porn(String url,boolean isInChina);

    public native static String fetchAcFun(String url);

    public native static String[] fetch57Ck(String url);

    public native static String fetchKuaiShou(String url);

    public native static String[] fetchXVideos(String url);

    public native static String fetchDouYin(String url);

    public native static String fetchMangoTV(String url);

    public native static String fetchXiGua(String url);

    public native static String[] fetchTencent(String url, String cookie);

    public native static String fetchTencentKey(String url, String vid, int format, String cookie);

    public native static String[] fetchIqiyi(String url);

    public native static String fetchPornOne(String url);

    public native static String[] fetchBilibili(String url);

    public native static String fetchCCTV(String url);

    public native static String fetchWeather(String province, String city, String county);

    public native static String fetchApplicationVersion();


}
