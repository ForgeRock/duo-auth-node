/*
 * Copyright 2019-2020 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

package org.forgerock.duo.duoNode;
import org.forgerock.openam.auth.node.api.ExternalRequestContext.Builder;
import org.forgerock.util.i18n.PreferredLocales;
import org.forgerock.duo.duoNode.DuoNode;
import static org.forgerock.json.JsonValue.*;
import org.forgerock.json.JsonValue;
import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.json.resource.ResourceException.BAD_REQUEST;
import static org.forgerock.json.resource.ResourceException.NOT_FOUND;
import static org.forgerock.json.resource.ResourceException.newResourceException;
import static org.forgerock.json.resource.ResourceResponse.FIELD_CONTENT_ID;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.OBJECT_ATTRIBUTES;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.USERNAME;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import org.forgerock.json.JsonValue;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.ExternalRequestContext.Builder;
import org.forgerock.openam.auth.node.api.TreeContext;
import java.util.Optional;
import org.mockito.InjectMocks;
import org.forgerock.json.JsonValue;
import org.forgerock.openam.auth.node.api.ExternalRequestContext;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.core.realms.Realm;
import static org.mockito.MockitoAnnotations.openMocks;
import org.mockito.Mock;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.forgerock.http.Handler;
import org.mockito.ArgumentCaptor;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import com.sun.identity.authentication.callbacks.HiddenValueCallback;
import com.sun.identity.authentication.callbacks.ScriptTextOutputCallback;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import org.forgerock.util.i18n.PreferredLocales;
import java.util.ArrayList;

public class DuoNodeTest {

    @Mock
    private DuoNode.Config config;

    @Mock
    private Realm realm;

    private TreeContext context;

    private DuoNode node;


   @BeforeMethod
   public void setUp() throws Exception {
       node = null;
       initMocks(this);

       when(config.iKey()).thenReturn("iKey");
       when(config.sKey()).thenReturn("sKey");
       when(config.apiHostName()).thenReturn("apiHostName");
       when(config.aKey()).thenReturn("aKey");

       node = new DuoNode(config, null);
   }

    @Test
    public void testProcessWithNoCallbacks() throws Exception {
      JsonValue sharedState = json(object(field("username", "bob")));
      Action result = node.process(getContext(sharedState));

      assertThat(result.outcome).isEqualTo(null);
      assertThat(result.callbacks).hasSize(2);
      assertThat(result.callbacks.get(0)).isInstanceOf(ScriptTextOutputCallback.class);
      assertThat(result.callbacks.get(1)).isInstanceOf(HiddenValueCallback.class);
    }

    //@Test
    //public void testProcessWithCallbacks() throws Exception {
    //  JsonValue sharedState = json(object(field("username", "bob")));
    //  ArrayList<Callback> callbacks = new ArrayList<Callback>() {{
    //      add(new ScriptTextOutputCallback("test"));
    //      add(new HiddenValueCallback("test"));
    //  }};
    //  Action result = node.process(getContext(sharedState, new PreferredLocales(), callbacks));
    //
    //  assertThat(result.outcome).isEqualTo(true);
    //}


    private TreeContext getContext() {
        return getContext(json(object()), json(object()));
    }


    private TreeContext getContext(JsonValue sharedState) {
        return new TreeContext(sharedState, json(object()), new Builder().build(), emptyList(), Optional.empty());
    }
    private TreeContext getContext(JsonValue sharedState, JsonValue transientState) {
        return new TreeContext(sharedState, transientState, new Builder().build(), emptyList(), Optional.empty());
    }
    private TreeContext getContext(JsonValue sharedState, PreferredLocales preferredLocales,
                List<? extends Callback> callbacks) {
            return new TreeContext(sharedState,
                    new Builder().locales(preferredLocales).build(), callbacks, Optional.empty());
        }

    private TreeContext getContext(JsonValue sharedState, JsonValue transientState, ExternalRequestContext request) {
        return new TreeContext(sharedState, transientState, request, emptyList(), Optional.empty());
    }


}