# Indent-Rainbow

[![Plugin version](https://img.shields.io/jetbrains/plugin/v/13308-indent-rainbow.svg)](https://plugins.jetbrains.com/plugin/13308-indent-rainbow)
[![Plugin downloads](https://img.shields.io/jetbrains/plugin/d/13308-indent-rainbow.svg)](https://plugins.jetbrains.com/plugin/13308-indent-rainbow)
[![GitHub issues](https://img.shields.io/github/issues/dima74/intellij-indent-rainbow)](https://github.com/dima74/intellij-indent-rainbow/issues)
[![check](https://github.com/dima74/intellij-indent-rainbow/workflows/check/badge.svg)](https://github.com/dima74/intellij-indent-rainbow/actions)

## A simple extension to make indentation more readable

This extension colorizes the indentation in front of your text alternating four different colors on each step.

Get it here: [JetBrains Plugins Repository](https://plugins.jetbrains.com/plugin/13308-indent-rainbow)

![Example](https://raw.githubusercontent.com/dima74/intellij-indent-rainbow/master/assets/example.png)

## Change colors
There are two options to change indent colors:

1. You can change indent colors opacity using corresponding slider (Settings / Indent Rainbow):

  ![Settings page](https://raw.githubusercontent.com/dima74/intellij-indent-rainbow/master/assets/opacity_slider.png)

2. You can configure each color (the error color and four indent colors) independently for each scheme (Settings / Editor / Color Scheme / Indent Rainbow). Firstly, uncheck checkbox "Inherit values from", then change background color. See [screenshot](https://raw.githubusercontent.com/dima74/intellij-indent-rainbow/master/assets/color-scheme.png) for details.

## FAQ

**Q:** Plugin is slow!  
**A:** Plugin performance has significantly improved in version 1.4. Please ensure that you have the latest version. If you still encounter performance issues, please switch to "simple highlighter" in settings ([screenshot](https://raw.githubusercontent.com/dima74/intellij-indent-rainbow/master/assets/choose-simple-annotator.png))

**Q:** What does "formatter based highlighting" mean?  
**A:** Currently there are two highlighting algorithms: formatter-based and simple. 

*Formatter-based* highlighter is default. It builds formatter model (which is used in "Reformat Code" action), and then (using this formatter model) computes for each line its indentation and alignment (more about indentation vs alignment [here](https://dmitryfrank.com/articles/indent_with_tabs_align_with_spaces)). So formatter-based highlighter is more correct than simple highlighter (e.g. it correctly handles [such code](https://user-images.githubusercontent.com/6505554/71819409-e5673880-309c-11ea-96c3-d1f3ecf88931.png)). It should be fast enough, because it is incremental.

*Simple* highlighter just highlights all spaces at the beginning of each line (both indentation and alignment). It should be very fast, because it is simple and incremental.

**Q:** Why indent is highlighted in red?  
**A:** Plugin highlights indent in red based on default IntelliJ formatter. If some line is highlighted in red, it means that after "Reformat Code" action this line indent will be changed. So you have four options: 

1) [Reformat your code](https://www.jetbrains.com/help/idea/reformat-and-rearrange-code.html)
2) [Configure your code style](https://www.jetbrains.com/help/idea/configuring-code-style.html)
3) Enable checkbox "Never highlight indent as error" in settings
4) Use "simple highlighter" (not recommended)


## Feedback
Please welcome to submit issues and feature requests!

## Acknowledgment
Our plugin was inspired by [Indent-Rainbow plugin for Visual Studio Code](https://github.com/oderwat/vscode-indent-rainbow).
