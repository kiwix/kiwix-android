(function() {
    window.DocumentParser.start();
    var headers = document.querySelectorAll('h1, h2, h3, h4, h5, h6');
    var usedIds = {};

    for (var i = 0; i < headers.length; i++) {
        var h = headers[i];

        if (h.id === "") {
            h.id = "documentparserid-" + i;
        }

        if (usedIds[h.id]) {
            var original = h.id;
            h.id = original + "-" + usedIds[original];
            usedIds[original]++;
        } else {
            usedIds[h.id] = 1;
        }

        window.DocumentParser.parse(h.textContent.trim(), h.tagName, h.id);
    }
    window.DocumentParser.stop();
})();
