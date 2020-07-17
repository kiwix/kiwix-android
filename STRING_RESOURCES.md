# String resources
All user visible strings in an Android application should be listed in strings.xml files to allow for easy translation, replacement and modification. Newly added string resources are imported by translatewiki and are then manually translated. After translations are complete the translations are merged with Kiwix Android. 

## How do I add a new string resource?
Start by adding your new string resource `new_string` to `values/strings.xml` in English. That is:
```
...
<string name="new_string">New String</string>
...
```
You will now have to describe the string in `values-qq/strings.xml` with where and how the new string is used. E.x. for the string `<string name="on">On</string>`:

`values-qq/strings.xml:`
```
...
<string name="on">This is used in the settings screen to turn on the night mode.</string>
...
```
- The values in `values/strings.xml` are the strings that are going to be displayed in the Kiwix application to the user. 
- The values in `values-qq/strings.xml` are only visible to the translator and are only there to help them make a correct translation. 
