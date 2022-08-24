package org.forgerock.duo.duoNode;

final class DuoConstants {
    static final String SETUP_DOM_SCRIPT =
            "var script = document.createElement('script');\n" +
            "document.getElementsByTagName('button')[0].style.display = 'none';\n" +
            "var duo_iframe = document.createElement('iframe');\n" +
            "duo_iframe.setAttribute('id', 'duo_iframe');\n" +
            "function duoCallback(response) {" +
            "var duo_iframe = document.getElementById('duo_iframe'); " +
            "duo_iframe.parentNode.removeChild(duo_iframe); " +
            "loginHelpers.setHiddenCallback('duo_response', response.elements.sig_response.value); " +
            "document.getElementsByTagName('button')[0].click();};\n" +
            "duo_iframe.setAttribute('data-submit-callback', duoCallback);\n" +
            "script.src = '%s';\n";
    static final String STYLE_SCRIPT = "duo_iframe.style.cssText = 'width: 100%;  min-width: 304px; " +
            "max-width: 620px; height: 330px; border: none; display:block; margin: auto;';\n" +
            "document.getElementById('callbacksPanel').appendChild(duo_iframe);\n";
    static final String INIT_SCRIPT = "script.onload = function() {" +
            "Duo.init({'host': '%s','sig_request': '%s','submit_callback': duoCallback});};\n" +
            "document.body.appendChild(script);\n";
}