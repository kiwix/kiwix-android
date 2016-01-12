#include <iostream>
#include <string>
#include <xapian.h>
#include <fcntl.h>
#include <unistd.h>

using namespace std;

void compact(const char* in, const char* out) {
    Xapian::Database indb(in);
    int fd = open(out, O_CREAT|O_RDWR, 0666);
    if (fd != -1) {
        indb.compact(fd);
        close(fd);
        cout << "Done!" << endl;
        return;
    }
    cout << "Some error happened..." << endl;
}

int main(int argc, char** argv) {
    if (argc != 3) {
        cout << "Wrong number of arguments!" << endl << "\t" << argv[0] << " [input folder] [output glassdb]" << endl;
    }
    compact(argv[1], argv[2]);
    return 0;
}
