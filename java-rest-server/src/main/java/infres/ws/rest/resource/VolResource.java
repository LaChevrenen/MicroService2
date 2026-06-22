package infres.ws.rest.resource;

import infres.ws.rest.model.Vol;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;

@Path("/vols")
@Produces(MediaType.APPLICATION_JSON)
public class VolResource {

    static final List<Vol> VOLS = List.of(
        new Vol(1, "Air France", "AF1234", "12A", 249.99, "2026-07-15"),
        new Vol(2, "Lufthansa",  "LH5678", "7B",  189.50, "2026-07-20"),
        new Vol(3, "EasyJet",    "EZY321", "3C",   89.99, "2026-07-22"),
        new Vol(4, "Air France", "AF5678", "22D", 312.00, "2026-07-18"),
        new Vol(5, "KLM",        "KL1010", "14E", 215.75, "2026-08-01")
    );

    // Route 1 : GET /api/vols  → liste tous les vols
    @GET
    public List<Vol> getAll() {
        return VOLS;
    }

    // Route 2 : GET /api/vols/{id}  → un vol par son id
    @GET
    @Path("/{id}")
    public Response getById(@PathParam("id") int id) {
        return VOLS.stream()
            .filter(v -> v.getId() == id)
            .findFirst()
            .map(vol -> Response.ok(vol).build())
            .orElse(Response.status(Response.Status.NOT_FOUND).build());
    }
}
