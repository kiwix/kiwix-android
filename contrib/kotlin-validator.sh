#!/usr/bin/env bash
 # Kiwix Android
 # Copyright (c) 2022 Kiwix <android.kiwix.org>
 # This program is free software: you can redistribute it and/or modify
 # it under the terms of the GNU General Public License as published by
 # the Free Software Foundation, either version 3 of the License, or
 # (at your option) any later version.
 #
 # This program is distributed in the hope that it will be useful,
 # but WITHOUT ANY WARRANTY; without even the implied warranty of
 # MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 # GNU General Public License for more details.
 #
 # You should have received a copy of the GNU General Public License
 # along with this program. If not, see <http://www.gnu.org/licenses/>.
 #
 #

 if ./gradlew buildKotlinToolingMetadata; then
   version=$(cat app/build/kotlinToolingMetadata/kotlin-tooling-metadata.json | tr { '\n' | tr , '\n' | tr } '\n' | grep "buildPluginVersion" | awk  -F'"' '{print $4}')
   if [ $version == "1.7.0" ];
   then
    echo "Build System is using kotlin Version 1.7.0"
   else
     echo "Build Failure : Due to kotlin Version 1.7.0 is missing"
     exit 1
   fi
 else
   exit 1
 fi
