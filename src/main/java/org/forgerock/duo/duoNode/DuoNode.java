/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2018 ForgeRock AS.
 */

package org.forgerock.duo.duoNode;

import static org.forgerock.duo.duoNode.DuoConstants.*;
import static org.forgerock.openam.auth.node.api.Action.send;

import com.duosecurity.duoweb.DuoWeb;
import com.duosecurity.duoweb.DuoWebException;
import com.google.inject.assistedinject.Assisted;
import com.sun.identity.authentication.callbacks.HiddenValueCallback;
import com.sun.identity.authentication.callbacks.ScriptTextOutputCallback;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import javax.inject.Inject;
import javax.security.auth.callback.Callback;
import org.forgerock.openam.annotations.sm.Attribute;
import org.forgerock.openam.auth.node.api.*;
import org.forgerock.openam.core.CoreWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Node.Metadata(outcomeProvider = AbstractDecisionNode.OutcomeProvider.class,
        configClass = DuoNode.Config.class, tags = {"mfa", "multi-factor authentication", "partner"})
public class DuoNode extends AbstractDecisionNode {

    private final Logger logger = LoggerFactory.getLogger("amAuth");
    private String iKey;
    private String sKey;
    private String apiHostName;
    private String aKey;
    private String duoJavascriptUrl;

    /**
     * Configuration for the Duo node.
     */
    public interface Config {
        @Attribute(order = 100)
        default String iKey() {
            return "";
        }

        @Attribute(order = 200)
        default String sKey() {
            return "";
        }

        @Attribute(order = 300)
        default String apiHostName() {
            return "";
        }

        @Attribute(order = 400)
        default String aKey() {
            return "";
        }

        @Attribute(order = 500)
        default String duoJavascriptUrl() {
            return "https://api.duosecurity.com/frame/hosted/Duo-Web-v2.min.js";
        }

    }

    @Inject
    public DuoNode(@Assisted Config config, CoreWrapper coreWrapper) throws NodeProcessException {
        this.iKey = config.iKey();
        this.sKey = config.sKey();
        this.apiHostName = config.apiHostName();
        this.aKey = config.aKey();
        this.duoJavascriptUrl = config.duoJavascriptUrl();
    }

    @Override
    public Action process(TreeContext context) throws NodeProcessException {
        String username = context.sharedState.get(SharedStateConstants.USERNAME).asString().toLowerCase();

        if (context.hasCallbacks()) {
            logger.debug("Duo Callbacks Received");
            String signatureResponse = context.getCallback(HiddenValueCallback.class).get().getValue();
            try {
                return goTo(username.equals(DuoWeb.verifyResponse(iKey, sKey, aKey, signatureResponse))).build();
            } catch (DuoWebException | NoSuchAlgorithmException | InvalidKeyException | IOException e) {
                e.printStackTrace();
                return goTo(false).build();
            }
        }
        return buildCallbacks(username);
    }

    private Action buildCallbacks(String username) {
        logger.debug("Sending Duo Callbacks to client");
        return send(new ArrayList<Callback>() {{
            add(new ScriptTextOutputCallback(String.format(SETUP_DOM_SCRIPT, duoJavascriptUrl) + STYLE_SCRIPT +
                    String.format(INIT_SCRIPT, apiHostName, DuoWeb.signRequest(iKey, sKey, aKey, username))));
            add(new HiddenValueCallback("duo_response"));
        }}).build();
    }

}