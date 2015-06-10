(function() {
  var e = 'img {-webkit-filter: invert(100%);' + '-moz-filter: invert(100%);' + '-o-filter: invert(100%);' + '-ms-filter: invert(100%); }',
    t = document.getElementsByTagName('head')[0],
    n = document.createElement('style');
  if (!window.counter) {
    window.counter = 1
  } else {
    window.counter++;
    if (window.counter % 2 == 0) {
      var e = 'html {-webkit-filter: invert(0%); -moz-filter: invert(0%); -o-filter: invert(0%); -ms-filter: invert(0%); }'
    }
  }
  n.type = 'text/css';
  if (n.styleSheet) {
    n.styleSheet.cssText = e
  } else {
    n.appendChild(document.createTextNode(e))
  }
  t.appendChild(n)
})();