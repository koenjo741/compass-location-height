// ##################################################################
// ADOBE BRIDGE GPS TO GOOGLE MAPS FIX (v10)
// FIXED: Menu Positioning, URL, and Parsing Logic
// ##################################################################

#target bridge

if (BridgeTalk.appName == "bridge") {

    // 1. Definiere die Haupt-Menü-ID (muss einzigartig sein)
    var commandID = "com.mycompany.gpsmapsviewer.fixed";

    // 2. Entferne den Befehl, falls er existiert, um Konflikte zu vermeiden
    var existingMenu = MenuElement.find(commandID);
    if (existingMenu) {
        existingMenu.remove();
    }

    // 3. Erstelle das Menü am Ende des Thumbnail-Kontextmenüs.
    // Diese Methode ist zuverlässiger als die Positionierung nach einem bestimmten Separator.
    var menu = MenuElement.create("command", "In Google Maps zeigen", "at the end of Thumbnail", commandID);

    // Steuert, wann der Menüpunkt sichtbar und klickbar ist.
    menu.onDisplay = function () {
        // Nur sichtbar und aktiv, wenn genau ein Element ausgewählt ist.
        var isSingleSelection = app.document.selections.length === 1;
        this.visible = isSingleSelection;
        this.enabled = isSingleSelection;
    };

    menu.onSelect = function () {
        var thumb = app.document.selections[0];

        try {
            if (ExternalObject.AdobeXMPScript == undefined) {
                ExternalObject.AdobeXMPScript = new ExternalObject("lib:AdobeXMPScript");
            }

            var xmp = new XMPMeta(thumb.metadata.serialize());

            // GPS-Daten aus den Metadaten extrahieren
            var latD = xmp.getProperty(XMPConst.NS_EXIF, "GPSLatitude");
            var lonD = xmp.getProperty(XMPConst.NS_EXIF, "GPSLongitude");
            var latRef = xmp.getProperty(XMPConst.NS_EXIF, "GPSLatitudeRef");
            var lonRef = xmp.getProperty(XMPConst.NS_EXIF, "GPSLongitudeRef");

            // Überprüfen, ob die wichtigsten GPS-Tags vorhanden sind
            if (!latD || !lonD) {
                alert("Fehler: Die GPS-Metadaten (Latitude/Longitude) fehlen in dieser Datei.");
                return;
            }

            // Funktion zur Umwandlung von GPS-Koordinaten (Grad, Minuten, Sekunden) in Dezimalgrad
            function parseCoordinates(coordinate, ref) {
                if (!coordinate) return null;

                var coordString = coordinate.toString();
                var coordRef = ref ? ref.toString().toUpperCase() : "";

                // 1. Ersetze das Dezimalkomma durch einen Punkt, um die Analyse zu vereinheitlichen
                var normalizedString = coordString.replace(/,/g, '.');

                // 2. Extrahiere alle Zahlen (inklusive Dezimalzahlen) aus dem String.
                //    Diese Methode ist robust gegenüber verschiedenen Formatierungen wie 59° 19' 21.87"
                var numbers = normalizedString.match(/\d+\.?\d*/g);

                if (!numbers || numbers.length < 1) {
                    return null; // Keine Zahlen im String gefunden
                }

                // 3. Wandle die extrahierten Zahlen-Strings in echte Zahlen um
                var degrees = parseFloat(numbers[0]) || 0;
                var minutes = (numbers.length > 1) ? parseFloat(numbers[1]) : 0;
                var seconds = (numbers.length > 2) ? parseFloat(numbers[2]) : 0;

                // 4. Berechne den Dezimalgrad
                var decimal = degrees + (minutes / 60) + (seconds / 3600);

                // 5. Vorzeichen basierend auf der Himmelsrichtung anpassen (Süd/West sind negativ)
                if (coordRef === "S" || coordRef === "W") {
                    decimal *= -1;
                }

                return decimal;
            }

            var lat = parseCoordinates(latD, latRef);
            var lon = parseCoordinates(lonD, lonRef);

            if (lat !== null && lon !== null) {
                // Moderne und korrekte Google Maps URL
                var mapsUrl = "https://www.google.com/maps?q=" + lat + "," + lon;

                // URL für das Betriebssystem kodieren
                var encodedUrl = encodeURI(mapsUrl);

                // Betriebssystem-spezifischer Befehl zum Öffnen der URL im Standardbrowser
                if ($.os.indexOf("Windows") != -1) {
                    // Für Windows
                    app.system('explorer "' + encodedUrl + '"');
                } else {
                    // Für macOS
                    app.system('open "' + encodedUrl + '"');
                }
            } else {
                alert("Die GPS-Koordinaten konnten nicht korrekt umgewandelt werden. Bitte überprüfen Sie die Metadaten.");
            }

        } catch (err) {
            alert("Ein unerwarteter Fehler ist aufgetreten:\n\n" + err.message + "\nZeile: " + err.line);
        }
    };
}
