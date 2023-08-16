package org.forgerock.duo.duoNode;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

import javax.inject.Inject;

import org.forgerock.json.JsonValue;
import org.forgerock.openam.annotations.sm.Attribute;
import org.forgerock.openam.auth.node.api.AbstractDecisionNode;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.Node;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.NodeState;
import org.forgerock.openam.auth.node.api.SharedStateConstants;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.core.CoreWrapper;
import org.forgerock.util.i18n.PreferredLocales;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.duosecurity.Client;
import com.duosecurity.model.Token;
import com.google.inject.assistedinject.Assisted;
import com.iplanet.am.util.SystemProperties;
import com.sun.identity.authentication.spi.RedirectCallback;
import com.sun.identity.shared.Constants;
import com.sun.identity.sm.RequiredValueValidator;

@Node.Metadata(outcomeProvider = DuoUniversalPromptNode.OutcomeProvider.class, configClass = DuoUniversalPromptNode.Config.class, tags = {"multi-factor authentication", "marketplace", "trustnetwork"})
public class DuoUniversalPromptNode extends AbstractDecisionNode {
    public enum FailureModes {
        CLOSED,
        OPEN,
    }

    private final Logger logger = LoggerFactory.getLogger(DuoUniversalPromptNode.class);

    private String loggerPrefix = "[Duo Universal Prompt]" + DuoNodePlugin.logAppender;

    private Client duoClient;
    private String clientId;
    private String clientSecret;
    private String apiHostName;
    private String callbackUri;
    private FailureModes failureMode;

    @Inject
    public DuoUniversalPromptNode(@Assisted Config config, CoreWrapper coreWrapper) throws NodeProcessException {
        clientId = config.clientId();
        clientSecret = config.clientSecret();
        apiHostName = config.apiHostName();
        callbackUri = config.callbackUri();
        failureMode = config.failureMode();
        duoClient = initializeDuoClient();
    }

    @Override
    public Action process(TreeContext context) throws NodeProcessException {
        try {
            logger.debug(loggerPrefix + "Started");
            Map<String, List<String>> parameters = context.request.parameters;
            
            NodeState ns = context.getStateFor(this);
            String userReference = ns.get(SharedStateConstants.USERNAME).asString().toLowerCase();

            try {
                duoClient.healthCheck();
            } catch (Exception e) {
                if (failureMode.equals(FailureModes.CLOSED)) {
                    throw new NodeProcessException(loggerPrefix + "Duo health check failed. Cannot proceed when failure mode closed is configured.", e);
                }

                // Failing OPEN means we skip this node when it's down.
                logger.error(loggerPrefix + "Duo health check failed, but failure mode is set to open. Bypassing Duo.");
                return goTo(true).build();
            }

            if (parameters.containsKey(DuoUniversalPromptConstants.RESP_DUO_CODE) && parameters.containsKey(
                    DuoUniversalPromptConstants.RESP_STATE)) {

                try {
                    Boolean authenticated = validateCallback(parameters, ns, userReference);

                    return goTo(authenticated).build();
                } catch (InvalidStateError e) {
                    // This exception is resolvable by starting the Duo flow over.
                    logger.warn(loggerPrefix + e.getMessage());

                    // Clear the session out & let the rest of the method re-do the Duo redirect.
                    ns.remove(DuoUniversalPromptConstants.SESSION_STATE);
                }
            }

            String state = duoClient.generateState();
            ns.putShared(DuoUniversalPromptConstants.SESSION_STATE, state);

            String duoUrl = createDuoAuthUrl(userReference, state);

            RedirectCallback redirectCallback = new RedirectCallback(duoUrl, null, "GET");
            redirectCallback.setTrackingCookie(true);

            return Action.send(redirectCallback).build();
        } catch(Exception ex) {
            logger.error(loggerPrefix + "Exception occurred: " + ex.getStackTrace());
            context.getStateFor(this).putShared(loggerPrefix + "Exception", new Date() + ": " + ex.getMessage());
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            ex.printStackTrace(pw);
            context.getStateFor(this).putShared(loggerPrefix + "StackTrace", new Date() + ": " + sw.toString());
            return Action.goTo("error").build();
        }
    }

    private Client initializeDuoClient() throws NodeProcessException {
        Client.Builder duoClient = new Client.Builder(clientId, clientSecret, apiHostName, callbackUri);

        try {
            return duoClient.build();
        } catch (Exception e) {
            throw new NodeProcessException(loggerPrefix + "Could not initialize Duo client. This probably indicates invalid configuration for the auth node.", e);
        }
    }

    private String createDuoAuthUrl(String userReference, String state) throws NodeProcessException {
        try {
            return duoClient.createAuthUrl(userReference, state);
        } catch (Exception e) {
            throw new NodeProcessException("Unable to create Duo authentication URL", e);
        }
    }

    /**
     * Validates the tokens in the callback URL params against the OpenAM session and Duo's auth API.
     *
     * Throws an InvalidStateError if the session/URL params don't match. This is going to be caused by an expired
     * session, or by somebody trying to spoof a Duo response by manipulating the URL.
     */
    private Boolean validateCallback(Map<String, List<String>> parameters, NodeState sharedState, String userReference) throws InvalidStateError, NodeProcessException {
        if (! sharedState.isDefined(DuoUniversalPromptConstants.SESSION_STATE)) {
            throw new InvalidStateError(loggerPrefix + "Detected Duo callback without initialized session. This may be a spoofing attempt (or a timed out session).");
        }

        String state = sharedState.get(DuoUniversalPromptConstants.SESSION_STATE).asString();
        String stateFromDuoCallback = parameters.get(DuoUniversalPromptConstants.RESP_STATE).get(0);
        String duoCode = parameters.get(DuoUniversalPromptConstants.RESP_DUO_CODE).get(0);

        // Validate that the Duo callback we've received is for this user.
        if (! state.equals(stateFromDuoCallback)) {
            throw new InvalidStateError(loggerPrefix + "Detected Duo callback with invalid session. This may be a spoofing attempt (or a timed out session).");
        }

        return validateDuoAuthenticated(duoCode, userReference);
    }

    private Boolean validateDuoAuthenticated(String duoCode, String userReference) throws NodeProcessException {
        try {
            Token token = duoClient.exchangeAuthorizationCodeFor2FAResult(duoCode, userReference);

            if (token == null || token.getAuth_result() == null) {
                return false;
            }

            return DuoUniversalPromptConstants.DUO_TOKEN_SUCCESSFUL_RESULT.equalsIgnoreCase(token.getAuth_result().getStatus());
        } catch (Exception e) {
            throw new NodeProcessException("Unable to exchange authorization code for result", e);
        }
    }

    /**
     * Configuration for the Duo node.
     */
    public interface Config {
        @Attribute(order = 100, validators = RequiredValueValidator.class)
        String clientId();

        @Attribute(order = 200, validators = RequiredValueValidator.class)
        String clientSecret();

        @Attribute(order = 300, validators = RequiredValueValidator.class)
        String apiHostName();

        @Attribute(order = 400, validators = RequiredValueValidator.class)
        default FailureModes failureMode() {
            return FailureModes.CLOSED;
        }

        @Attribute(order = 500, validators = RequiredValueValidator.class)
        default String callbackUri() {
            final String protocol = SystemProperties.get(Constants.AM_SERVER_PROTOCOL);
            final String host = SystemProperties.get(Constants.AM_SERVER_HOST);
            final String port = SystemProperties.get(Constants.AM_SERVER_PORT);
            final String descriptor = SystemProperties.get(Constants.AM_SERVICES_DEPLOYMENT_DESCRIPTOR);

            if (protocol != null && host != null && port != null && descriptor != null) {
                return protocol + "://" + host + ":" + port + descriptor;
            }

            return "";
        }
    }
    public static final class OutcomeProvider implements org.forgerock.openam.auth.node.api.OutcomeProvider {
        /**
         * Outcomes Ids for this node.
         */
        static final String SUCCESS_OUTCOME = "true";
        static final String ERROR_OUTCOME = "error";
        static final String FALSE_OUTCOME = "false";
        private static final String BUNDLE = DuoUniversalPromptNode.class.getName();

        @Override
        public List<Outcome> getOutcomes(PreferredLocales locales, JsonValue nodeAttributes) {

            ResourceBundle bundle = locales.getBundleInPreferredLocale(BUNDLE, OutcomeProvider.class.getClassLoader());

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
