#include <xapian.h>

#include <iomanip>
#include <iostream>

#include <cmath> // For log10().
#include <cstdlib> // For exit().
#include <cstring> // For strcmp() and strrchr().
#include <string>
#include <fcntl.h>
#include <stdio.h>
#include <ftw.h>
#include <unistd.h>

using namespace std;

#define PROG_NAME "glassify"
#define PROG_DESC "Perform a document-by-document copy of one or more Xapian databases and make it a single file"

static void
show_usage(int rc)
{
    cout << "Usage: " PROG_NAME " SOURCE_DATABASE... DESTINATION_DATABASE\n\n"
"Options:\n"
"  --no-renumber    Preserve the numbering of document ids (useful if you have\n"
"                   external references to them, or have set them to match\n"
"                   unique ids from an external source).  If multiple source\n"
"                   databases are specified and the same docid occurs in more\n"
"                   one, the last occurrence will be the one which ends up in\n"
"                   the destination database.\n"
"  --help           display this help and exit\n"
"  --version        output version information and exit" << endl;
    exit(rc);
}

void compact(const char* in, const char* out) try {
    Xapian::Database indb(in);
    int fd = open(out, O_CREAT|O_RDWR, 0666);
    if (fd != -1) {
        indb.compact(fd);
        cout << "Done!" << endl;
        return;
    }
    cout << "Some error happened..." << endl;
} catch (const Xapian::Error &e) {
    cout << e.get_description().c_str() << endl;
}

int unlinker(const char *fpth, const struct stat *sb, int t, struct FTW *fb) {
    int rv = remove(fpth);
    if (rv)
        perror(fpth);
    return rv;
}

int cleaner(const char *path) {
    return nftw(path, unlinker, 64, FTW_DEPTH | FTW_PHYS);
}

int
main(int argc, char **argv)
try {
    bool renumber = true;
    if (argc > 1 && argv[1][0] == '-') {
	if (strcmp(argv[1], "--help") == 0) {
	    cout << PROG_NAME " - " PROG_DESC "\n\n";
	    show_usage(0);
	}
	if (strcmp(argv[1], "--version") == 0) {
	    cout << PROG_NAME << endl;
	    exit(0);
	}
	if (strcmp(argv[1], "--no-renumber") == 0) {
	    renumber = false;
	    argv[1] = argv[0];
	    ++argv;
	    --argc;
	}
    }

    // We expect two or more arguments: at least one source database path
    // followed by the destination database path.
    if (argc < 3) show_usage(1);

    // Create the destination database, using DB_CREATE so that we don't
    // try to overwrite or update an existing database in case the user
    // got the command line argument order wrong.
    string dest_str = string(argv[argc - 1]);
    dest_str += ".tmp";
    const char *dest = dest_str.c_str();
    Xapian::WritableDatabase db_out(dest, Xapian::DB_CREATE|Xapian::DB_BACKEND_GLASS);

    for (int i = 1; i < argc - 1; ++i) {
	char * src = argv[i];
	if (*src) {
	    // Remove any trailing directory separator.
	    char & ch = src[strlen(src) - 1];
	    if (ch == '/' || ch == '\\') ch = '\0';
	}

	// Open the source database.
	Xapian::Database db_in(src);

	// Find the leaf-name of the database path for reporting progress.
	const char * leaf = strrchr(src, '/');
#if defined __WIN32__ || defined __OS2__
	if (!leaf) leaf = strrchr(src, '\\');
#endif
	if (leaf) ++leaf; else leaf = src;

	// Iterate over all the documents in db_in, copying each to db_out.
	Xapian::doccount dbsize = db_in.get_doccount();
	if (dbsize == 0) {
	    cout << leaf << ": empty!" << endl;
	} else {
	    // Calculate how many decimal digits there are in dbsize.
	    int width = static_cast<int>(log10(double(dbsize))) + 1;

	    Xapian::doccount c = 0;
	    Xapian::PostingIterator it = db_in.postlist_begin(string());
	    while (it != db_in.postlist_end(string())) {
		Xapian::docid did = *it;
		if (renumber) {
		    db_out.add_document(db_in.get_document(did));
		} else {
		    db_out.replace_document(did, db_in.get_document(did));
		}

		// Update for the first 10, and then every 13th document
		// counting back from the end (this means that all the
		// digits "rotate" and the counter ends up on the exact
		// total.
		++c;
		if (c <= 10 || (dbsize - c) % 13 == 0) {
		    cout << '\r' << leaf << ": ";
		    cout << setw(width) << c << '/' << dbsize << flush;
		}

		++it;
	    }

	    cout << endl;
	}

	cout << "Copying spelling data..." << flush;
	Xapian::TermIterator spellword = db_in.spellings_begin();
	while (spellword != db_in.spellings_end()) {
	    db_out.add_spelling(*spellword, spellword.get_termfreq());
	    ++spellword;
	}
	cout << " done." << endl;

	cout << "Copying synonym data..." << flush;
	Xapian::TermIterator synkey = db_in.synonym_keys_begin();
	while (synkey != db_in.synonym_keys_end()) {
	    string key = *synkey;
	    Xapian::TermIterator syn = db_in.synonyms_begin(key);
	    while (syn != db_in.synonyms_end(key)) {
		db_out.add_synonym(key, *syn);
		++syn;
	    }
	    ++synkey;
	}
	cout << " done." << endl;

	cout << "Copying user metadata..." << flush;
	Xapian::TermIterator metakey = db_in.metadata_keys_begin();
	while (metakey != db_in.metadata_keys_end()) {
	    string key = *metakey;
	    db_out.set_metadata(key, db_in.get_metadata(key));
	    ++metakey;
	}
	cout << " done." << endl;
    }

    cout << "Committing..." << flush;
    // Commit explicitly so that any error is reported.
    db_out.commit();
    cout << " done." << endl;
    cout << "Turning into single file..." << endl;
    compact(dest, argv[argc - 1]);
    cout << "All finished. Cleaning up..." << endl;
    cleaner(dest);
    cout << "Done!" << endl;
} catch (const Xapian::Error & e) {
    cerr << '\n' << argv[0] << ": " << e.get_description() << endl;
    exit(1);
}
