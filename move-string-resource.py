import sys
import os
import mmap


def print_correct_usage(arg0):
    print("Usage: {} [source module] [destination module] [string key]".format(arg0))
    sys.exit(exit)


def print_argv_is_not_valid_dir(arg1, path_to_values):
    print(arg1 + path_to_values + " is not a valid directory.")
    sys.exit(exit)


def print_invalid_key():
    print('Could not find string with key \'' + key + "\' in file " + source + path_to_values + "/strings.xml")
    sys.exit(exit)


if __name__ == '__main__':
    if len(sys.argv) < 3:
        print_correct_usage(sys.argv[0])
    path_to_values = "/src/main/res/"
    english_values_dirname = "values"
    source = sys.argv[1]
    destination = sys.argv[2]
    key = "name=\"" + sys.argv[3] + "\""
    if not os.path.isdir(source + path_to_values + english_values_dirname):
        print_argv_is_not_valid_dir(soruce, path_to_values + english_values_dirname)
    if not os.path.isdir(destination + path_to_values + english_values_dirname):
        print_argv_is_not_valid_dir(destination, path_to_values + english_values_dirname)

    found_values = {}
    for dirname in os.listdir(source + path_to_values):
        print(source + path_to_values + dirname + "/strings.xml")
        if dirname.startswith("values"):
            with open(source + path_to_values + dirname + "/strings.xml") as file, \
                    mmap.mmap(file.fileno(), 0, access=mmap.ACCESS_READ) as input_string:
                if input_string.find(str.encode(key)) != -1:
                    line = file.readline()
                    while line:
                        if line.find(key) != -1:
                            print(line)
                            found_values[dirname] = line
                            break
                        line = file.readline()
                else:
                    print_invalid_key()
