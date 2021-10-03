#include "Logger.h"
#include <fstream>

using namespace std;

void WriteFile(const string &fileName, const string &content) {
    ofstream file(fileName);
    if (file.is_open()) {
        file << content;
        file.close();
    }
}