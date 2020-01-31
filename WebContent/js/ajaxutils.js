//jslint browser: true, sloppy: true, vars: true, white: true, nomen: true, plusplus: true, indent: 4 */

/* browser globals */
/*global window:false, ActiveXObject:false, DOMParser:false */

/*--------------------------------------------------|
 | AJAX Utilities   |   www.javascript.about.com    |
 |--------------------------------------------------|
 | Copyright (c) 2007 javascript.about.com			|
 |                                                  |
 | This script can be used freely as long as all    |
 | copyright messages are intact.                   |
 |                                                  |
 | Updated: 03.11.2008	                            |
 |--------------------------------------------------*/

function createHttpRequest() {
    if( typeof XMLHttpRequest !== 'undefined') {
        return new XMLHttpRequest();
    }

    if(window.ActiveXObject) {
        var avers = ["Microsoft.XmlHttp", "MSXML2.XmlHttp", "MSXML2.XmlHttp.3.0", "MSXML2.XmlHttp.4.0", "MSXML2.XmlHttp.5.0"];
        var i;

        for(i = avers.length - 1; i >= 0; i--) {
            try {
                var httpObj = new ActiveXObject(avers[i]);
                return httpObj;
            } catch(e) {
            }
        }
    }

    throw new Error('XMLHttp (AJAX) not supported');
}

function sendHttpRequest(url, onReadyFunc, onErrorFunc) {
    var r = createHttpRequest();

    // Warning - Calling r.open() after setting r.onreadystatechange
    // will cause memory leaks in most browsers

    r.open("GET", url, true);
    r.onreadystatechange = function() {
        if(r.readyState === 4) {
            if(r.status === 200) {
                onReadyFunc(r.responseXML);
            } else {
                onErrorFunc(r);
            }
            // stop memory leaks under IE6 and IE8
            r = null;
        }
    };

    r.send(null);
}

function postHttpRequest(url, doc, onReadyFunc, onErrorFunc) {
    var r = createHttpRequest();

    // Warning - Calling r.open() after setting r.onreadystatechange
    // will cause memory leaks in most browsers

    r.open("POST", url, true);
    r.onreadystatechange = function() {
        if(r.readyState === 4) {
            if(r.status === 200) {
                onReadyFunc(r.responseXML);
            } else {
                onErrorFunc(r);
            }
            // stop memory leaks under IE6 and IE8
            r = null;
        }
    };

    r.send(doc);
}

/*------------------------------------------|
| Non-www.javascript.about.com extensions   |
|-------------------------------------------|
| Copyright (c) 2009, 2012 SPX UK Limited   |
|------------------------------------------*/

/*
 * Warning - Microsoft.XMLDOM handles XML declarations differently
 * to DOMParser. The former exposes their existence in the DOM,
 * therefore xml.firstChild == declaration and not root node
 */
function createDOM(xmlString) {
    if(window.DOMParser) {
        // Firefox, Chrome, IE9 etc
        var xmlParser = new DOMParser();
        return xmlParser.parseFromString(xmlString, "text/xml");
    }

    // IE only
    var xmlDoc = new ActiveXObject("Microsoft.XMLDOM");
    xmlDoc.async = "false";
    xmlDoc.loadXML(xmlString);
    return xmlDoc;
}

/*
 * Microsoft.XMLDOM handles CDATA nodes differently to DOMParser
 */
function getCDATAValue(e) {
    if(window.DOMParser) {
        if (e.text) {
            // IE9
            return e.text;
        }
        else {
            // Firefox, Chrome, etc
            return e.textContent;
        }
    }
    // IE < 9 only
    return e.firstChild.text;
}

/* Implements cross domain AJAX using JSON enabled servers.
 *
 * Uses the HTML SCRIPT tag to execute javascript fragments returned
 * from a server. Unlike XmlHttpRequest (see createHttpRequest),
 * SCRIPT.src does NOT restrict access to servers in different domains.
 *
 * IE treats JSON declared variables as globals. However, Firefox scopes
 * them to the script being executed. To workaround this, the returned
 * javascript should invoke a user method, passing any required data as
 * parameters. The name of the user method can be passed to the server
 * (generating the JSON) via a URL parameter. For example,
 *
 * function myFunc(data) { ... }
 *
 * sendCrossDomainJsonRequest( "http://localhost/myServlet?callback=myFunc" );
 */
function sendCrossDomainJsonRequest(jsonUrl) {
    var head = document.getElementsByTagName("head").item(0);

    // destroy current script tag

    var oscript = document.getElementById("crossDomainAjaxScript");
    if(oscript) {
        head.removeChild(oscript);
    }

    // create new script tag

    oscript = document.createElement("script");
    oscript.id = "crossDomainAjaxScript";
    oscript.type = "text/javascript";
    oscript.src = jsonUrl;

    // adding new tag will trigger fetch

    head.appendChild(oscript);
}