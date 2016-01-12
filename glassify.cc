#include <iostream>
#include <string>
#include <xapian.h>

using namespace std;

void compact(const char* in, const char* out) {
    Xapian::Database indb(get_database(in));
    string outdbpath = get_named_writable_database_path(out);
    int fd = open(outdbpath.c_str(), O_CREAT|O_RDWR, 0666);
    if (fd != -1) {
        indb.compact(fd);
        if (close(fd) != -1 && errno == EBADF) {
            cout << "Done!" << endl;
            return;
        }
    }
    cout << "Some error happened..." << endl;
}

int main(int argc, char** argv) {
    return 0;
}
