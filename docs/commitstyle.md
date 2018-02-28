# Commit Style Guidelines for Kiwix-Android

### Message Structure
Commit messages consist of three distinct parts, separated by a blank line: the title, an optional body/content, and an optional footer/metadata. The layout looks like this:

type subject

body

***

### Title
The title consists of the subject and type of the commit message.

### Type
The type is contained within the title and can be one of the following types:

* **Implement** feature x
* **Fix** a bug in x feature
* **Improve** documentation for x feature
* **Remove** removed redundant code
* **Restyle** formatting, missing semi-colons, etc; no code change
* **Refactor** refactoring production code

### Subject
The subject is a single short line summarising the change. It should be no greater than 50 characters, should begin with a capital letter and do not end with a period.

Use an imperative tone to describe what a commit does, rather than what it did. For example, use fix; not fixed or fixes or fixing.

For example:
- Fix typo in Commit Style guidelines
- Add crash reporting via e-mail
- Implement SearchActivity using MVP pattern

Instead of writing the following:
- Fixed bug with Y
- Changing behaviour of X

### Body
The body includes the kind of information commit message (if any) should contain.

Not every commit requires both a subject and a body. Sometimes a single line is fine, especially when the change is self-explanatory and no further context is necessary, therefore it is optional. The body is used to explain the what and why of a commit, not the how.

When writing a body, the blank line between the title and the body is required and we should try to limit the length of each line to no more than 72 characters.

***
