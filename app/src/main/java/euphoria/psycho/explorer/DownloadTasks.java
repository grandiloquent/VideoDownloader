
package euphoria.psycho.explorer;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.concurrent.CopyOnWriteArrayList;

public class DownloadTasks {

    private CopyOnWriteArrayList<DownloadTaskObject> mDownloadTaskObjects = new CopyOnWriteArrayList<>();



    public void update(String uri, int size) {
        for (DownloadTaskObject object : mDownloadTaskObjects) {
            if (object.Uri.equals(uri)) {
                object.Size = size;
                break;
            }
        }
    }

    public void write() throws JSONException {
        JSONArray jsonArray = new JSONArray();
        for (DownloadTaskObject object : mDownloadTaskObjects) {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("fileName", object.FileName);
            jsonObject.put("size", object.Size);
            jsonObject.put("uri", object.Uri);
            jsonArray.put(jsonObject);
        }
        jsonArray.toString();
    }

    public class DownloadTaskObject {
        public String Uri;
        public int Size;
        public String FileName;
        public String FullPath;
    }
}
