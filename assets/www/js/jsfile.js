function() {
  window.onload = onLoad();

  function onLoad() {
   window.HTMLUtils.start();
   for (i=0; i<document.querySelectorAll('h1, h2, h3, h4, h5, h6').length; i++) {
      headerObject = document.querySelectorAll('h1, h2, h3, h4, h5, h6')[i];
     window.HTMLUtils.parse(headerObject.textContent, headerObject.tagName, headerObject.id);
   }
    window.HTMLUtils.stop();
  }
}