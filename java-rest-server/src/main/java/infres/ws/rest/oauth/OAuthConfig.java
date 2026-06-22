package infres.ws.rest.oauth;

import com.google.api.client.auth.oauth2.AuthorizationCodeFlow;
import com.google.api.client.auth.oauth2.BearerToken;
import com.google.api.client.auth.oauth2.ClientParametersAuthentication;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.MemoryDataStoreFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Properties;

public class OAuthConfig {

    private static final Properties PROPS = loadProps();

    private static Properties loadProps() {
        Properties p = new Properties();
        try (InputStream in = OAuthConfig.class.getClassLoader()
                .getResourceAsStream("oauth.properties")) {
            if (in == null) {
                throw new RuntimeException(
                    "Fichier oauth.properties introuvable dans src/main/resources/. " +
                    "Créez-le avec google.client.id, google.client.secret et google.callback.url."
                );
            }
            p.load(in);
        } catch (IOException e) {
            throw new RuntimeException("Erreur lecture oauth.properties", e);
        }
        return p;
    }

    static String clientId()     { return PROPS.getProperty("google.client.id"); }
    static String clientSecret() { return PROPS.getProperty("google.client.secret"); }
    static String callbackUrl()  { return PROPS.getProperty("google.callback.url"); }

    @SuppressWarnings("deprecation")
    static AuthorizationCodeFlow buildFlow() throws IOException {
        return new AuthorizationCodeFlow.Builder(
            BearerToken.authorizationHeaderAccessMethod(),
            new NetHttpTransport(),
            JacksonFactory.getDefaultInstance(),
            new GenericUrl("https://oauth2.googleapis.com/token"),
            new ClientParametersAuthentication(clientId(), clientSecret()),
            clientId(),
            "https://accounts.google.com/o/oauth2/v2/auth"
        )
        .setScopes(Arrays.asList("openid", "profile", "email"))
        .setDataStoreFactory(MemoryDataStoreFactory.getDefaultInstance())
        .build();
    }
}
