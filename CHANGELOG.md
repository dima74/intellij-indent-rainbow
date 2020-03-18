# Changelog
----

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
