# Code Style Guidelines for Kiwix-Android

Our code style guidelines is based on the [Square's Android Style Guide](https://github.com/square/java-code-styles/blob/master/configs/codestyles/SquareAndroid.xml).

Our code style is managed in version control and will be installed automatically when you clone the repo.

### Only a few extra rules

- Line length is 100 characters
- FIXME must not be committed in the repository use TODO instead. FIXME can be used in your own local repository only.

### Some **DONT's**

- Don't use Hungarian Notation like `mContext` `mCount` etc
- Don't use underscores in variable names
- All constants should be CAPS. e.g `MINIMUM_TIMEOUT_ERROR_EXTERNAL`
- Always use `Locale.ENGLISH` when using `String.format()` unless the format itself is locale dependent e.g. `String query = String.format(Locale.ENGLISH,...`
- Never concat `null` with `""` (Empty String). It will become `"null"` e.g. `String.equals("" + null, "null") == TRUE`
