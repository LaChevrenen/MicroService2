package infres.ws.rest.oauth;

import com.google.api.client.auth.oauth2.AuthorizationCodeFlow;
import com.google.api.client.extensions.servlet.auth.oauth2.AbstractAuthorizationCodeServlet;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

// Étape 3b — URL de login : redirige l'utilisateur vers Google pour qu'il s'authentifie
public class OAuthLoginServlet extends AbstractAuthorizationCodeServlet {

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
}
