package infres.ws.rest.oauth;

import com.google.api.client.auth.oauth2.AuthorizationCodeFlow;
import com.google.api.client.auth.oauth2.AuthorizationCodeResponseUrl;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.servlet.auth.oauth2.AbstractAuthorizationCodeCallbackServlet;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.GenericJson;
import com.google.api.client.json.jackson2.JacksonFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

// Étape 3b — URL de callback : reçoit le code, l'échange contre un access token,
//             puis appelle l'API Google pour récupérer le profil utilisateur (étape 3c)
@SuppressWarnings("deprecation")
public class OAuthCallbackServlet extends AbstractAuthorizationCodeCallbackServlet {

    @Override
    protected AuthorizationCodeFlow initializeFlow() throws IOException {
        return OAuthConfig.buildFlow();
    }

    @Override
    protected String getRedirectUri(HttpServletRequest req) {
        return OAuthConfig.callbackUrl();
    }

    @Override
    protected String getUserId(HttpServletRequest req) {
        return req.getSession(true).getId();
    }

    // Étape 3c : accès token reçu → appel à l'API userinfo de Google
    @Override
    protected void onSuccess(HttpServletRequest req, HttpServletResponse resp,
                             Credential credential) throws IOException {
        HttpRequestFactory factory = new NetHttpTransport().createRequestFactory(credential);
        HttpRequest request = factory.buildGetRequest(
            new GenericUrl("https://www.googleapis.com/oauth2/v3/userinfo")
        );
        request.setParser(JacksonFactory.getDefaultInstance().createJsonObjectParser());

        GenericJson profileJson = request.execute().parseAs(GenericJson.class);

        // Stockage en session sous forme de Map pour la sérialisation JSON (Jersey)
        Map<String, Object> profile = new HashMap<>(profileJson);
        req.getSession().setAttribute("profile", profile);

        resp.sendRedirect("/");
    }

    @Override
    protected void onError(HttpServletRequest req, HttpServletResponse resp,
                           AuthorizationCodeResponseUrl errorResponse) throws IOException {
        resp.sendRedirect("/?oauth_error=true");
    }
}
