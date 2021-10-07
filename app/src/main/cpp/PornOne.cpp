#include "PornOne.h"
#include <string>
#include "httplib/httplib.h"
#include "Shared.h"
#include "Logger.h"
#include "rapidjson/document.h"

using namespace std;
using namespace rapidjson;

using namespace std;
namespace PornOne {
    string FetchVideo(const char *uri, int timeout) {
        auto hostName = "pornone.com";
        httplib::SSLClient client(hostName, 443);
        client.set_connection_timeout(timeout);
        httplib::Headers headers = {
                {"Host",       hostName},
                {"User-Agent", "Mozilla/5.0 (Windows NT 6.1; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/92.0.4515.159 Safari/537.36"}
        };
        client.enable_server_certificate_verification(false);
        auto res = client.Get(uri, headers);
        if (!res) {
            return string();
        }
        return Substring(res->body, "<source src=\"", "\"");
    }

}
