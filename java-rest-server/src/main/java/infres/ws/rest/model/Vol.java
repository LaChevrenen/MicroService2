package infres.ws.rest.model;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "vol")
@XmlAccessorType(XmlAccessType.FIELD)
public class Vol {

    private int id;
    private String compagnie;
    private String numero;
    private String place;
    private double prix;
    private String date;

    public Vol() {}

    public Vol(int id, String compagnie, String numero, String place, double prix, String date) {
        this.id = id;
        this.compagnie = compagnie;
        this.numero = numero;
        this.place = place;
        this.prix = prix;
        this.date = date;
    }

    public int getId()           { return id; }
    public String getCompagnie() { return compagnie; }
    public String getNumero()    { return numero; }
    public String getPlace()     { return place; }
    public double getPrix()      { return prix; }
    public String getDate()      { return date; }
}
