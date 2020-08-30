# Changelog
----

# 1.6
- Now you can [choose color palette in settings](https://user-images.githubusercontent.com/6505554/91661297-94f18f00-eaf4-11ea-9d8e-0a97ba7ae982.png):
    - Added pastel color palette. Please try it out, especially if you use light theme!
    - Added custom palette, where you can change number of colors
- Added an option to choose specific file types plugin should work on
- Addressed some exceptions, thanks to everyone who reported them!
- Reduced plugin size from 2M to 0.5M

# 1.5
- Now indent rainbow cuts through [multiline strings](https://github.com/dima74/intellij-indent-rainbow/issues/9) and [comments](https://github.com/dima74/intellij-indent-rainbow/issues/17)
- Fix [incorrect highlighting](https://github.com/dima74/intellij-indent-rainbow/issues/10) for language injections
- Make plugin dynamic
- Improve error reporting

# 1.4.2
- Fix for F# in Rider

# 1.4.1
- Use simple highlighter instead of incremental formatter-based if there is no formatting model for language. This is workaround for Rider and Haskell

# 1.4.0
- Implement new incremental formatter-based highlighter, which is now default. It should significantly improve performance on large files
- Disable indent rainbow for read-only files by default
- Add option "Never highlight indent as error"

# 1.3.0
- Fix that indent rainbow not working when file contains errors
- Improve performance up to 2-3 times on some languages (there was a bug that annotator may be registered several times for some languages)

# 1.2.0
- Allow using custom indent colors (configure in Settings / Editor / Color Scheme / Indent Rainbow)
- Add settings to change colors opacity

# 1.1.1
- Fix exception related to TextAttributes initialization

# 1.1.0
- Fix indent and alignment handling
- Add settings page

# 1.0.4
- Add plugin icon

# 1.0.3
- Fix highlighting for tabs
- Add action for disabling indent rainbow

# 1.0.2
- Fix for themes which extends Empty scheme

# 1.0.1
- Fix rendering on macOS and Windows
- Improve colors

# 1.0.0
- Initial version
