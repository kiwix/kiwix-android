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
 * Example usage #2:
 * kotlinc -script move-string-resource.kts -- ../app ../core file_with_line_separated_keys.txt
 *
 * This will cut all strings with the name that matches any line in file_with_line_separated_keys.txt from
 * all strings.xml files within directories with a prefix of values*
 * in ../app/src/main/res/ and paste them into
 * their corresponding files within ../core/src/main/res/values*.
 *
 */

/**
 * To recompile script to binary use kscript:
 * https://github.com/holgerbrandl/kscript
 * https://github.com/holgerbrandl/kscript#deploy-scripts-as-standalone-binaries
 */

import java.io.File
import java.lang.StringBuilder
import kotlin.system.exitProcess

if (args.size < 3) {
  printCorrectUsageAndExit()
}

val pathToValues = "/src/main/res/"
val source = args[0]
val destination = args[1]

val sourceDir = File(source + pathToValues)
if (!sourceDir.exists()) {
  printModuleIsNotValidDir(source, pathToValues)
}

val keyIds = if (args[2] == "--keys") {
  if (args.size != 4) {
    printCorrectUsageAndExit()
  }
  val keysFileName = args[3]
  val keysFile = File(keysFileName)
  if (!keysFile.exists()) {
    System.err.println("File $keysFileName doesn't exist")
    printCorrectUsageAndExit()
  }

  keysFile.readLines()
} else {
  listOf(args[2])
}

val keys = keyIds.map { keyId -> """name="${keyId}"""" }

println("\nRunning transfer of string resources...\n")
val numberOfFilesAffected = sourceDir.cutStringResourcesAndPasteToDestination(keys, source, destination)
println("\nTransfer of string resources complete. Moved strings from $numberOfFilesAffected files.")

fun File.cutStringResourcesAndPasteToDestination(keys: List<String>, source: String, destination: String): Int {
  var numberOfFilesAffected = 0
  this.walk().filter { it.name.equals("strings.xml") }.forEach { resourceFile ->
    cutLinesFromResourceFile(resourceFile, keys).takeIf { it.isNotEmpty() }?.let { cutLine ->
      pasteLinesToDestination(
        openOrCreateDestinationResourceFile(resourceFile, source, destination),
        cutLine
      )
      numberOfFilesAffected++
    }
  }
  return numberOfFilesAffected
}

fun openOrCreateDestinationResourceFile(resourceFile: File, source: String, destination: String): File {

  val destinationFilePath = resourceFile.path.replaceRange(0..source.length, destination + "/")
  val destinationFile = File(destinationFilePath)
  val destinationDirectory = destinationFile.parentFile

  if (!destinationDirectory.exists()) {
    createDestinationDirectoryAndFile(destinationDirectory, destinationFile)
  }
  return destinationFile
}

fun createDestinationDirectoryAndFile(destinationDirectory: File, destinationFile: File) {
  destinationDirectory.mkdirs()
  val isNewFileCreated: Boolean = destinationFile.createNewFile()
  if (!isNewFileCreated) {
    System.err.println("Could not create resource file ${destinationFile.path}")
    System.exit(-1)
  }

  destinationFile.writeText(
    """
    <?xml version="1.0" encoding="UTF-8"?>
    <resources xmlns:tools="http://schemas.android.com/tools">
    </resources>
  """.trimIndent()
  )
  println("Created directory ${destinationDirectory.path} and resource file ${destinationFile.path}")
}

fun pasteLinesToDestination(destinationFile: File, cutLines: String) {
  var resourceDataWithPastedLine = StringBuilder()
  destinationFile.forEachLine { line ->
    if (line.contains("</resources")) {
      printPastedValue(cutLines, destinationFile)
      resourceDataWithPastedLine.appendln(cutLines)
    }
    resourceDataWithPastedLine.appendln(line)
  }
  destinationFile.writeText(resourceDataWithPastedLine.toString())
}

fun cutLinesFromResourceFile(resourceFile: File, keys: List<String>): String {
  var resourceDataWithoutCutLine = StringBuilder()
  var cutLine = StringBuilder()

  var waitUntilPluralsEnd = false

  resourceFile.forEachLine { line ->
    if (waitUntilPluralsEnd || keys.any { line.contains(it) }) {
      cutLine.appendln(line)
      printCutValueAndPath(line, resourceFile)
      if (waitUntilPluralsEnd && line.contains("</plurals>")) {
        waitUntilPluralsEnd = false
      } else if (line.contains("<plurals")) {
        waitUntilPluralsEnd = true
      }
    } else {
      resourceDataWithoutCutLine.appendln(line)
    }
  }

  resourceFile.writeText(resourceDataWithoutCutLine.toString())
  return cutLine.trimEnd().toString()
}

fun printCorrectUsageAndExit() {
  System.err.println(
    """
    Usage:
    move-string-resource.kts [source module] [destination module] [string key]
    OR
    move-string-resource.kts [source module] [destination module] --keys [file containing line separated string keys]
    """.trimIndent()
  )
  printExample()
  exitProcess(-1)
}

fun printModuleIsNotValidDir(source: String, pathToValues: String) {
  System.err.println("$source$pathToValues is not a valid directory.")
  printExample()
  exitProcess(-1)
}

fun printExample() {
  System.err.println("Example when copying strings with key kiwi from app to core:")
  System.err.println("move-string-resource.kts ../app ../core kiwi")
}

fun printPastedValue(key: String, resourceFile: File) {
  println("Wrote string $key to ${resourceFile.path}")
}

fun printCutValueAndPath(key: String, resourceFile: File) {
  println("Cut string $key from ${resourceFile.path}")
}

fun addNewLine(firstLineRead: Boolean, stringToAddLineTo: String, line: String): String {
  var tempString = stringToAddLineTo
  if (firstLineRead) {
    tempString = tempString + "\n"
  }
  tempString = tempString + line
  return tempString
}
