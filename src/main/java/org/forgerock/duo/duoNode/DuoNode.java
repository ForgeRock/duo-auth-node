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

import static org.forgerock.duo.duoNode.DuoConstants.INIT_SCRIPT;
import static org.forgerock.duo.duoNode.DuoConstants.SETUP_DOM_SCRIPT;
import static org.forgerock.duo.duoNode.DuoConstants.STYLE_SCRIPT;
import static org.forgerock.openam.auth.node.api.Action.send;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import javax.inject.Inject;
import javax.security.auth.callback.Callback;

import org.forgerock.json.JsonValue;
import org.forgerock.openam.annotations.sm.Attribute;
import org.forgerock.openam.auth.node.api.AbstractDecisionNode;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.Node;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.SharedStateConstants;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.core.CoreWrapper;
import org.forgerock.util.i18n.PreferredLocales;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.duosecurity.duoweb.DuoWeb;
import com.duosecurity.duoweb.DuoWebException;
import com.google.inject.assistedinject.Assisted;
import com.sun.identity.authentication.callbacks.HiddenValueCallback;
import com.sun.identity.authentication.callbacks.ScriptTextOutputCallback;

@Node.Metadata(outcomeProvider = DuoNode.OutcomeProvider.class,
        configClass = DuoNode.Config.class, tags = {"multi-factor authentication", "marketplace", "trustnetwork"})
public class DuoNode extends AbstractDecisionNode {

    private final Logger logger = LoggerFactory.getLogger(DuoNode.class);
    private String loggerPrefix = "[Duo]" + DuoNodePlugin.logAppender;
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
        try {
            String username = context.getStateFor(this).get(SharedStateConstants.USERNAME).asString().toLowerCase();

            if (context.hasCallbacks()) {
                logger.debug(loggerPrefix + "Duo Callbacks Received");
                String signatureResponse = context.getCallback(HiddenValueCallback.class).get().getValue();
                try {
                    return Action.goTo(String.valueOf(username.equals(DuoWeb.verifyResponse(iKey, sKey, aKey, signatureResponse)))).build();
                } catch (DuoWebException e) {
                    logger.error(loggerPrefix+ ": " + e.getMessage());
                    logger.error(loggerPrefix + "Exception occurred: " + e.getStackTrace());
                    return Action.goTo("false").build();
                }
            }
            return buildCallbacks(username);
        } catch(Exception ex) {
			logger.error(loggerPrefix + "Exception occurred: " + ex.getStackTrace());
			context.getStateFor(this).putShared(loggerPrefix + "Exception", new Date() + ": " + ex.getMessage());
			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);
			ex.printStackTrace(pw);
			context.getStateFor(this).putShared(loggerPrefix + "StackTracke", new Date() + ": " + sw.toString());
            return Action.goTo("error").build();
        }
    }

    private Action buildCallbacks(String username) {
        logger.debug(loggerPrefix + "Sending Duo Callbacks to client");
        return send(new ArrayList<Callback>() {{
            add(new ScriptTextOutputCallback(String.format(SETUP_DOM_SCRIPT, duoJavascriptUrl) + STYLE_SCRIPT +
                    String.format(INIT_SCRIPT, apiHostName, DuoWeb.signRequest(iKey, sKey, aKey, username))));
            add(new HiddenValueCallback("duo_response"));
        }}).build();
    }

    public static final class OutcomeProvider implements org.forgerock.openam.auth.node.api.OutcomeProvider {
        /**
         * Outcomes Ids for this node.
         */
        static final String SUCCESS_OUTCOME = "true";
        static final String ERROR_OUTCOME = "error";
        static final String FALSE_OUTCOME = "false";

        @Override
        public List<Outcome> getOutcomes(PreferredLocales locales, JsonValue nodeAttributes) {

    

            List<Outcome> results = new ArrayList<>(
                    Arrays.asList(
                            new Outcome(SUCCESS_OUTCOME, "True")
                    )
            );
            results.add(new Outcome(FALSE_OUTCOME, "False"));
            results.add(new Outcome(ERROR_OUTCOME, "Error"));

            return Collections.unmodifiableList(results);
        }
    }

}