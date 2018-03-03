# Code Style Guidelines for Kiwix-Android

Our code style guidelines is based on the [Google Java Style Guide](https://google.github.io/styleguide/javaguide.html).

Do take some time to read it.

### For XML files

Kiwix follows `2dp` for the `Indent` and `Continuous Indent` values. You can update these values using following steps:

- Go to **Settings** -> **Editor** -> **Code Style** -> **XML**
- Update the respective values under the **Tabs and Indents** tab

After changing the values, one can simply select the files to indent, `Right click` -> `Reformat Code` -> Check `Rearrange entries` -> `OK`.

### Only a few extra rules

- Line length is 120 characters
- FIXME must not be committed in the repository use TODO instead. FIXME can be used in your own local repository only.

You can run a checkstyle with most rules via a gradle command:

```
$ ./gradlew checkstyle
```

It generates a HTML report in `build/reports/checkstyle/checkstyle.html`.

Try to remove as much warnings as possible, It's not completely possible to remove all the warnings, but over a period of time, we should try to make it as complete as possible.

### Some **DONT's**

- Don't use Hungarian Notation like `mContext` `mCount` etc
- Don't use underscores in variable names
- All constants should be CAPS. e.g `MINIMUM_TIMEOUT_ERROR_EXTERNAL`
- Always use `Locale.ENGLISH` when using `String.format()` unless the format itself is locale dependent e.g. `String query = String.format(Locale.ENGLISH,...`
- Never concat `null` with `""` (Empty String). It will become `"null"` e.g. `String.equals("" + null, "null") == TRUE`
