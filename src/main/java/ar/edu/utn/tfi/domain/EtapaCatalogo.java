package ar.edu.utn.tfi.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name="etapa_catalogo")
public class EtapaCatalogo {
    @Id
    private String codigo;   // PK: coincide con la columna "codigo"

    private Integer orden;   // orden secuencial (1,2,3,...)

    // --- getters/setters ---
    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }

    public Integer getOrden() { return orden; }
    public void setOrden(Integer orden) { this.orden = orden; }
}
