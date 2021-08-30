package euphoria.psycho.videos;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import euphoria.psycho.explorer.MainActivity;
import euphoria.psycho.share.Logger;
import euphoria.psycho.share.StringShare;

import static euphoria.psycho.videos.VideosHelper.getString;

public class QQ extends BaseVideoExtractor<String> {
    private static Pattern MATCH_QQ = Pattern.compile("qq\\.com");
    public static final String PLAYER_VERSION = "3.2.19.333";

    protected QQ(String inputUri, MainActivity mainActivity) {
        super(inputUri, mainActivity);
    }

    public static boolean handle(String uri, MainActivity mainActivity) {
        if (MATCH_QQ.matcher(uri).find()) {
            new QQ(uri, mainActivity).parsingVideo();
            return true;
        }
        return false;
    }

    private JSONObject getInfo(String resolution, String vid) {
        return getObject(String.format("http://vv.video.qq.com/getinfo?otype=json&platform=11&defnpayver=1&appver=%s&defn=%s&vid=%s",
                "3.2.19.333", resolution, vid));
    }

    private JSONObject getObject(String uri) {
        String response = getString(uri, null);
        if (response == null) {
            Logger.d(String.format("'%s' is null.", "response"));
            return null;
        }
        String jsonBody = StringShare.substringMax(response, "QZOutputJson=", ";");
        if (jsonBody == null) {
            Logger.d(String.format("'%s' is null.", "jsonBody"));
            return null;
        }
        JSONObject obj;
        try {
            obj = new JSONObject(jsonBody);
        } catch (JSONException e) {
            Logger.d(String.format("getInfo: %s", e.getMessage()));
            return null;
        }
        return obj;
    }

    private String getVid(String uri) {
        String vid = StringShare.substringLeast(uri, "/", ".html");
        if (vid != null && vid.length() == 11) {
            return vid;
        }
        String response = getString(uri, null);
        if (response == null) {
            Logger.d(String.format("'%s' is null.", "response"));
            return null;
        }
        String[] patterns = new String[]{
                "vid=(\\w+)",
                "vid:\\s*[\"'](\\w+)",
                "vid\\s*=\\s*[\"']\\s*(\\w+)"
        };
        for (int i = 0; i < 3; i++) {
            Pattern pattern = Pattern.compile(patterns[i]);
            Matcher matcher = pattern.matcher(response);
            if (matcher.find()) {
                vid = matcher.group(1);
                break;
            }
        }
        if (vid == null) {
            Logger.d(String.format("'%s' is null.", "vid"));
            return null;
        }
        return vid;
    }

    private String parseCDN(JSONObject obj) {
        try {
            JSONObject vl;
            if (obj.has("vl")) {
                vl = obj.getJSONObject("vl");
            } else {
                return null;
            }
            JSONArray vi;
            if (vl.has("vi")) {
                vi = vl.getJSONArray("vi");
            } else {
                return null;
            }
            JSONObject ul;
            if (vi.getJSONObject(0).has("ul")) {
                ul = vi.getJSONObject(0).getJSONObject("ul");
            } else {
                return null;
            }
            JSONArray ui;
            if (ul.has("ui")) {
                ui = ul.getJSONArray("ui");
            } else {
                return null;
            }
            String url;
            if (ui.getJSONObject(0).has("url")) {
                url = ui.getJSONObject(0).getString("url");
                return url;
            } else {
                return null;
            }

        } catch (Exception ignored) {
            Logger.d(String.format("parseJSON: %s", ignored));
        }
        return null;
    }

    private int parseFC(JSONObject obj) {
        try {
            JSONObject vl;
            if (obj.has("vl")) {
                vl = obj.getJSONObject("vl");
            } else {
                return 0;
            }
            JSONArray vi;
            if (vl.has("vi")) {
                vi = vl.getJSONArray("vi");
            } else {
                return 0;
            }
            JSONObject cl;
            if (vi.getJSONObject(0).has("cl")) {
                cl = vi.getJSONObject(0).getJSONObject("cl");
            } else {
                return 0;
            }
            int fc;
            if (cl.has("fc")) {
                fc = cl.getInt("fc");
                return fc;
            } else {
                return 0;
            }

        } catch (Exception ignored) {
            Logger.d(String.format("parseJSON: %s", ignored));
        }
        return 0;
    }

    private String parseFN(JSONObject obj) {
        try {
            JSONObject vl;
            if (obj.has("vl")) {
                vl = obj.getJSONObject("vl");
            } else {
                return null;
            }
            JSONArray vi;
            if (vl.has("vi")) {
                vi = vl.getJSONArray("vi");
            } else {
                return null;
            }
            String fn;
            if (vi.getJSONObject(0).has("fn")) {
                fn = vi.getJSONObject(0).getString("fn");
                return fn;
            } else {
                return null;
            }

        } catch (Exception ignored) {
            Logger.d(String.format("parseJSON: %s", ignored));
        }
        return null;
    }

    private String[][] parseResolutions(JSONObject obj) {
        try {
            JSONObject fl;
            if (obj.has("fl")) {
                fl = obj.getJSONObject("fl");
            } else {
                return null;
            }
            JSONArray fi;
            if (fl.has("fi")) {
                fi = fl.getJSONArray("fi");
            } else {
                return null;
            }
            String[][] names = new String[fi.length()][2];
            for (int i = 0; i < fi.length(); i++) {
                names[i][0] = fi.getJSONObject(i).getString("name");
                names[i][1] = Integer.toString(fi.getJSONObject(i).getInt("id"));

            }
            return names;
        } catch (Exception ignored) {
            Logger.d(String.format("parseJSON: %s", ignored));
        }
        return null;
    }

    @Override
    protected String fetchVideoUri(String uri) {
        // 1.
        String vid = getVid(uri);
        if (vid == null) return null;
        JSONObject obj = getInfo("shd", vid);
        String cdn = parseCDN(obj);
        if (cdn == null) {
            Logger.d(String.format("'%s' is null.", "cdn"));
            return null;
        }
        String[][] names = parseResolutions(obj);
        String fvKey = parseFvKey(obj);
        if (names == null) {
            Logger.d(String.format("'%s' is null.", "names"));
            return null;
        }
        for (String[] name : names) {
            JSONObject objTmp = getInfo(name[0], vid);
            String fn = parseFN(objTmp);
            if (fn == null) {
                Logger.d(String.format("'%s' is null.", "fn"));
                return null;
            }
            int fc = parseFC(objTmp);
            if (fc == 0) {
                fc = 1;
            }
            boolean fileNameInValid = false;
            if (StringShare.count(fn, '.') >= 3) {
                String[] segments = fn.split("\\.");
                Pattern pattern = Pattern.compile("^p(\\d{3})$");
                Matcher matcher = pattern.matcher(segments[1]);
                if (matcher.find()) {
                    fileNameInValid = true;
                }
            }
            for (int i = 0; i < fc; i++) {
                JSONObject keyObject = getObject(String.format("http://vv.video.qq.com/getkey?otype=json&platform=11&appver=%s&filename=%s&format=%s&vid=%s",
                        PLAYER_VERSION, fn, name[1], vid));
                String key = parseKey(keyObject);
                if (key == null) {
                    key = fvKey;
                }
                uri = String.format("%s%s?vkey=%s", cdn, fn, key);

            }
            Logger.d(String.format("fetchVideoUri: %d %s", fc, fn));
            //
        }
        return null;
    }

    private String parseFvKey(JSONObject obj) {
        try {
            JSONObject vl;
            if (obj.has("vl")) {
                vl = obj.getJSONObject("vl");
            } else {
                return null;
            }
            JSONArray vi;
            if (vl.has("vi")) {
                vi = vl.getJSONArray("vi");
            } else {
                return null;
            }
            String fvkey;
            if (vi.getJSONObject(0).has("fvkey")) {
                return vi.getJSONObject(0).getString("fvkey");
            } else {
                return null;
            }

        } catch (Exception ignored) {
            Logger.d(String.format("parseJSON: %s", ignored));
        }
        return null;
    }

    private String parseKey(JSONObject obj) {
        try {
            String key;
            if (obj.has("key")) {
                return obj.getString("key");
            } else {
                return null;
            }

        } catch (Exception ignored) {
            Logger.d(String.format("parseJSON: %s", ignored));
        }
        return null;
    }

    @Override
    protected String processUri(String inputUri) {
        return inputUri;
    }

    @Override
    protected void processVideo(String videoUri) {
    }
}
// https://v.qq.com/x/cover/k16928rkrk217zb/z00401l30ys.html
