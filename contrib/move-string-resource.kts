/*
 * Kiwix Android
 * Copyright (c) 2020 Kiwix <android.kiwix.org>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 */

/**
 * USAGE:
 * Cuts string resources with the name arg3 from the module from arg1
 * and pastes them to the string resources from the module from arg2.
 *
 * Example usage:
 * kotlinc -script move-string-resource.kts ../app ../core history_from_current_book
 *
 * This will cut all strings with the name history_from_current_book from
 * all strings.xml files within directories with a prefix of values*
 * in ../app/src/main/res/ and paste them into
 * their corresponding files within ../core/src/main/res/values*.
 *
 */
import java.io.File
import kotlin.system.exitProcess


if (args.size < 3) {
  print_correct_usage_and_exit()
}

var path_to_values = "/src/main/res/"
var english_values_dirname = "values"
var source = args[0]
var destination = args[1]
var key = "name=\"" + args[2] + "\""

var source_dir = File(source + path_to_values)
var destination_dir = File(destination + path_to_values)
validate_input()

var found_values: HashMap<String, String> = HashMap<String, String>()
cut_values_from_source_files()
println("")
paste_values_to_destination_files()


fun cut_values_from_source_files() {
  // iterate all files in source directory
  source_dir.walk().maxDepth(1).forEach { current_dir ->
    if (current_dir.name.startsWith("values")) {
      val strings_file = File(source + path_to_values + current_dir.name + "/strings.xml")
      if (strings_file.exists()) {
        cut_lines_with_key_from_file(strings_file, current_dir)
      }
    }
  }
}

fun paste_values_to_destination_files() {
  // iterate all files in destination directory
  destination_dir.walk().maxDepth(1).forEach { destination_directory ->
    if (we_cut_value_from_source_same_language(destination_directory)) {
      val strings_file = File(destination + path_to_values + destination_directory.name + "/strings.xml")
      if (strings_file.exists()) {
        paste_strings(strings_file, destination_directory)
        print_pasted_value(destination_directory, strings_file)
      }
    }
  }
}

fun validate_input() {
  if (!source_dir.exists()) {
    print_module_is_not_valid_dir(source, path_to_values)
  }

  if (!destination_dir.exists()) {
    print_module_is_not_valid_dir(destination, path_to_values)
  }
}

fun we_cut_value_from_source_same_language(destination_directory: File) =
  found_values.keys.contains(destination_directory.name)

fun cut_lines_with_key_from_file(strings_file: File, current_dir: File) {
  var xml_data_without_cut_string = ""
  var firstLineRead = false
  strings_file.forEachLine { line ->
    if (line.contains(key)) {
      found_values.put(current_dir.name, line)
      print_cut_values(current_dir, strings_file)
    } else {
      xml_data_without_cut_string = addNewLine(firstLineRead, xml_data_without_cut_string, line)
      firstLineRead = true
    }
  }
  strings_file.writeText(xml_data_without_cut_string)
}

fun paste_strings(strings_file: File, destination_directory: File) {
  var xml_data_with_cut_string = ""
  var firstLineRead = false
  strings_file.forEachLine { line ->
    xml_data_with_cut_string = addNewLine(firstLineRead, xml_data_with_cut_string, line)
    firstLineRead = true

    if (line.contains("<resources")) {
      xml_data_with_cut_string = xml_data_with_cut_string + "\n" + found_values[destination_directory.name]
    }
  }
  strings_file.writeText(xml_data_with_cut_string)
}


fun print_correct_usage_and_exit() {
  System.err.println("Usage:\nmove-string-resource.kts [source module] [destination module] [string key]")
  exitProcess(-1)
}

fun print_module_is_not_valid_dir(source: String, path_to_values: String) {
  System.err.println(source + path_to_values + " is not a valid directory.")
  exitProcess(-1)
}

fun print_pasted_value(destination_directory: File, strings_file: File) {
  println("Wrote string " + key + " to " + destination + path_to_values + destination_directory.name + "/" + strings_file.name)
}

fun print_cut_values(current_dir: File, strings_file: File) {
  println("Cut string " + key + " from " + source + path_to_values + current_dir.name + "/" + strings_file.name)
}

fun addNewLine(firstLineRead: Boolean, stringToAddLineTo: String, line: String): String {
  var temp_string = stringToAddLineTo
  if (firstLineRead) {
    temp_string = temp_string + "\n"
  }
  temp_string = temp_string + line
  return temp_string
}
