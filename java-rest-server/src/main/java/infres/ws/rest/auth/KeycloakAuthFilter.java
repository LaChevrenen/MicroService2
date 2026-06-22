package infres.ws.rest.auth;

import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.SignedJWT;

import javax.annotation.Priority;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.net.URI;
import java.util.Date;

@Provider
@Priority(Priorities.AUTHENTICATION)
public class KeycloakAuthFilter implements ContainerRequestFilter {

    static final String JWKS_URL  = "http://localhost:8180/realms/flightbook/protocol/openid-connect/certs";
    static final String ISSUER    = "http://localhost:8180/realms/flightbook";
    private static final long     TTL_MS = 10 * 60_000L;

    private static JWKSet cachedJwks;
    private static long   cacheTs = 0;

    @Override
    public void filter(ContainerRequestContext ctx) throws IOException {
        String auth = ctx.getHeaderString(HttpHeaders.AUTHORIZATION);
        if (auth == null || !auth.startsWith("Bearer ")) {
            reject(ctx, 401);
            return;
        }
        try {
            SignedJWT jwt = SignedJWT.parse(auth.substring(7));
            RSAKey key = (RSAKey) getJwks().getKeyByKeyId(jwt.getHeader().getKeyID());
            if (key == null || !jwt.verify(new RSASSAVerifier(key))) {
                reject(ctx, 403); return;
            }
            var claims = jwt.getJWTClaimsSet();
            if (claims.getExpirationTime().before(new Date())) {
                reject(ctx, 403); return;
            }
            if (!ISSUER.equals(claims.getIssuer())) {
                reject(ctx, 403); return;
            }
        } catch (Exception e) {
            reject(ctx, 403);
        }
    }

    private static synchronized JWKSet getJwks() throws Exception {
        if (cachedJwks == null || System.currentTimeMillis() - cacheTs > TTL_MS) {
            cachedJwks = JWKSet.load(URI.create(JWKS_URL).toURL());
            cacheTs = System.currentTimeMillis();
        }
        return cachedJwks;
    }

    private static void reject(ContainerRequestContext ctx, int status) {
        ctx.abortWith(Response.status(status)
            .entity("{\"error\":\"unauthorized\"}")
            .type("application/json").build());
    }
}
