package org.forgerock.duo.duoNode;

final class DuoUniversalPromptConstants {
    /**
     * Key for state param kept in OpenAM's shared state.
     */
    static final String SESSION_STATE = "state";

    /**
     * Response param from Duo
     */
    static final String RESP_DUO_CODE = "duo_code";

    /**
     * Response param from Duo
     */
    static final String RESP_STATE = "state";

    static final String DUO_TOKEN_SUCCESSFUL_RESULT = "ALLOW";
}
