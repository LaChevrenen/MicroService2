package infres.ws.rest.resource;

import infres.ws.rest.model.Vol;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.stream.Collectors;

@Path("/compagnies")
@Produces(MediaType.APPLICATION_JSON)
public class CompagnieResource {

    // Route 3 : GET /api/compagnies  → liste des compagnies distinctes
    @GET
    public List<String> getCompagnies() {
        return VolResource.VOLS.stream()
            .map(Vol::getCompagnie)
            .distinct()
            .sorted()
            .collect(Collectors.toList());
    }

    // Route 4 : GET /api/compagnies/{nom}/vols  → vols d'une compagnie
    @GET
    @Path("/{nom}/vols")
    public Response getVolsByCompagnie(@PathParam("nom") String nom) {
        List<Vol> resultat = VolResource.VOLS.stream()
            .filter(v -> v.getCompagnie().equalsIgnoreCase(nom))
            .collect(Collectors.toList());

        if (resultat.isEmpty()) {
            return Response.status(Response.Status.NOT_FOUND)
                .entity("{\"message\":\"Compagnie inconnue : " + nom + "\"}")
                .build();
        }
        return Response.ok(resultat).build();
    }
}
