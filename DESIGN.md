# Design

This file intends to provide information on how to work with design in the Kiwix android applicaiton. 

## Colors

<img src="https://user-images.githubusercontent.com/22193232/83739760-4ef07080-a656-11ea-8f87-344f48e76324.png" alt="drawing" width="300"/>

The Kiwix colors are black and white with different colored accents. For Kiwix Android, black is accented by different shades of blue:

- ![#000000](https://via.placeholder.com/15/000000/000000?text=+) `#000000`/`black`
- ![#1565c0](https://via.placeholder.com/15/1565c0/000000?text=+) `#1565c0`/`denim_blue800`
- ![#42a5f5](https://via.placeholder.com/15/42a5f5/000000?text=+) `#42a5f5`/`denim_blue400`
- ![#2196F3](https://via.placeholder.com/15/2196F3/000000?text=+) `#2196F3`/`dodger_blue`

> Color indicates which elements are interactive, how they relate to other elements, and their level of prominence. Important elements should stand out the most [1].

For a guide on how to apply colors to UI, see ["Applying color to UI - Material Design"](https://material.io/design/color/applying-color-to-ui.html#backdrop).

## Themes
Kiwix uses themes to apply styles to views. This means that instead of adding a specific color or style to each view in the application, a theme should be set.
Themes are defined in `res/values/themes.xml` or `res/values-night/themes.xml` depending on night/day.
The following steps should be followed to set a color or style of a view.
1. Define the color or style:
    * Define the color in `core/colors.xml`. Use good naming (`denim_blue`, `mine_shaft_gray` etc..).
    * Define the style in `core/styles.xml`. Most styles override theme attributes and should be named to indicate that. This is done by naming the style to match its parent. That is, `Widget.MaterialComponent.*` -> `Widget.KiwixTheme.*`. If multiple styles for the same parent are needed, descriptive names should be used, e.x. `list_item_title`. 
2. Add the color or style to a specific theme attribute in `themes.xml`.
3. Make sure that the color or style works in both day and night mode. If it does not, add the dark mode compatible attribute to `values-night/themes.xml`.

For a video on how to work with themes, styles and colors, see [Developing Themes with Style (Android Dev Summit '19)](https://www.youtube.com/watch?v=Owkf8DhAOSo).

## Night Mode / Dark Theme
Night mode is a different theme that has many benefits such as saving battery and improving visibility in low light conditions [2]. When doing any design, make sure the new design works in dark mode. Night mode can be activated from the systems settings or from the kiwix app. 

Whenever a resource exists in both `res/*-night` and `res/*` it will be used as such. For example: `kiwix_icon_with_title.png` exists in both `res/drawable` and `res/drawable-night` which means that the image in `res/drawable-night` will be used automagically in night mode. For further reading on development with dark mode, see [Material Design - Dark Theme](https://developer.android.com/guide/topics/ui/look-and-feel/darktheme). 

## Typography
Text should contrast well, use the correct weight and size to present content as clearly and efficiently as possible. Google provides a "type scale" system that provides thirteen styles of typography to use for different contexts. Just as we do not give specific style attributes to views, we should not use specific text style attributes for text. Instead a type style should be used. For example, the button type style should be used for text on a button. To generate new type styles use the [Material Design type scale generator](https://material.io/design/typography/the-type-system.html#type-scale). To use type styles in practice, find a type style in `core/../res/type.xml` (e.x. `TextAppearance.KiwixTheme.Button`) and add it to a theme. Alternatively add a new type scale style to `core/../res/type.xml`. Additional reading can be found at [Material Design - Type System](https://material.io/design/typography/the-type-system.html#type-scale). 

## Resources
Following is a list of resources that can and should be used to support a design decision.

1. [Material Design](https://material.io/) - Googles design guide that should be followed to keep Android applications consistent.
2. [Design for Android](https://developer.android.com/design) - Another design guide by Google, this one is specifically for Android while Material Design is more general.
3. [Figma](https://www.figma.com) - A tool that can be used to mock design ideas.

## Accessibility
Accessibility is an important part of Android applications. Especially applications that are as widely used as Kiwix. As such, accessibility should be part of design decisions. This can include choices such as:

- Having text with a large font and good contrast ratio.
- Large simple controls.
- Consistent controls and navigation. (consistent to Kiwix, and other Android applications.)

Kiwix does not yet contain content descriptions for its UI elements. This can prohibit users with for example poor eyesight to use Kiwix successfully. Therefore, newly introduced interactive elements should contain content descriptions (boy scout rule applies here too!). 

See ["Make apps more accessible"](https://developer.android.com/guide/topics/ui/accessibility/apps) for a more detailed guide on accessibility. [Firefox Fenix](https://github.com/mozilla-mobile/shared-docs/blob/master/android/accessibility_guide.md) also has a great concise description of accessibility. A third accessibility resource is googles [list of key steps](https://android-developers.googleblog.com/2012/04/accessibility-are-you-serving-all-your.html) for making sure an Android application is accessible.
<br/>
<br/>
<br/>
<br/>

[1] https://material.io/design/color/the-color-system.html 2020-06-04

[2] https://developer.android.com/guide/topics/ui/look-and-feel/darktheme 2020-07-02


