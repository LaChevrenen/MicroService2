package infres.ws.rest.resource;

import infres.ws.rest.model.Reservation;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.UUID;

@Path("/reservations")
@Produces(MediaType.APPLICATION_JSON)
public class ReservationResource {

    // Route 5 : POST /api/reservations?volId=1  → confirme une réservation
    @POST
    public Response reserver(@QueryParam("volId") int volId) {
        if (volId <= 0) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity("{\"message\":\"Paramètre volId requis\"}")
                .build();
        }

        return VolResource.VOLS.stream()
            .filter(v -> v.getId() == volId)
            .findFirst()
            .map(vol -> {
                String ref = "RES-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
                Reservation resa = new Reservation(ref, vol.getId(), vol.getCompagnie(), vol.getNumero());
                return Response.status(Response.Status.CREATED).entity(resa).build();
            })
            .orElse(Response.status(Response.Status.NOT_FOUND)
                .entity("{\"message\":\"Vol introuvable\"}")
                .build());
    }
}
