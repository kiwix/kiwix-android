# Design

This file intends to provide links to design resources, display the Kiwix colors, as well as presenting accessibility.

## Colors

<img src="https://user-images.githubusercontent.com/22193232/83739760-4ef07080-a656-11ea-8f87-344f48e76324.png" alt="drawing" width="300"/>

The Kiwix colors are black and white with different colored accents. For Kiwix Android, black is accented by different shades of blue:

- ![#000000](https://via.placeholder.com/15/000000/000000?text=+) `#000000`/`black`
- ![#1565c0](https://via.placeholder.com/15/1565c0/000000?text=+) `#1565c0`/`color_primary`
- ![#2196F3](https://via.placeholder.com/15/2196F3/000000?text=+) `#2196F3`/`accent`
- ![#42a5f5](https://via.placeholder.com/15/42a5f5/000000?text=+) `#42a5f5`/`blue400`

> Color indicates which elements are interactive, how they relate to other elements, and their level of prominence. Important elements should stand out the most [1].

For a guide on how to apply colors to UI, see ["Applying color to UI - Material Design"](https://material.io/design/color/applying-color-to-ui.html#backdrop).

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

[1] https://material.io/design/color/the-color-system.html 2020-06-04


