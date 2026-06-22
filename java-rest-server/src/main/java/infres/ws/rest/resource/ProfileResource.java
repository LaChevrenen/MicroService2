package infres.ws.rest.resource;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Map;

@Path("/profile")
@Produces(MediaType.APPLICATION_JSON)
public class ProfileResource {

    // Retourne le profil Google stocké en session après le callback OAuth
    @GET
    @SuppressWarnings("unchecked")
    public Response getProfile(@Context HttpServletRequest req) {
        HttpSession session = req.getSession(false);
        if (session == null) {
            return Response.status(Response.Status.UNAUTHORIZED)
                .entity("{\"message\":\"Non connecté\"}")
                .build();
        }

        Map<String, Object> profile = (Map<String, Object>) session.getAttribute("profile");
        if (profile == null) {
            return Response.status(Response.Status.UNAUTHORIZED)
                .entity("{\"message\":\"Non connecté\"}")
                .build();
        }

        return Response.ok(profile).build();
    }
}
