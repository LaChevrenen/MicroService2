package infres.ws.rest.model;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "reservation")
@XmlAccessorType(XmlAccessType.FIELD)
public class Reservation {

    private String reference;
    private int volId;
    private String compagnie;
    private String numero;
    private String statut;

    public Reservation() {}

    public Reservation(String reference, int volId, String compagnie, String numero) {
        this.reference = reference;
        this.volId = volId;
        this.compagnie = compagnie;
        this.numero = numero;
        this.statut = "CONFIRMÉE";
    }

    public String getReference() { return reference; }
    public int getVolId()        { return volId; }
    public String getCompagnie() { return compagnie; }
    public String getNumero()    { return numero; }
    public String getStatut()    { return statut; }
}
