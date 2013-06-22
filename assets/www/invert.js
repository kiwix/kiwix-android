function load_script(src, callback) {
    var s = document.createElement('script');
    s.src = src;
    s.onload = callback;
    document.getElementsByTagName('head')[0].appendChild(s);
}

function invertColors() {
    var colorProperties = ['color', 'background-color'];
    $('*').each(function () {
        var color = null;
        for (var prop in colorProperties) {
            prop = colorProperties[prop];
            if (!$(this).css(prop)) continue;
            color = new RGBColor($(this).css(prop));
            if (color.ok) {
                $(this).css(prop, 'rgb(' + (255 - color.r) + ',' + (255 - color.g) + ',' + (255 - color.b) + ')');
            }
            color = null;
        }
    });
}

load_script('file:///android_asset/www/rgb.js', function () {
    if (!window.jQuery) load_script('file:///android_asset/www/jquery.js', invertColors);
    else invertColors();
    alert(2);
})
