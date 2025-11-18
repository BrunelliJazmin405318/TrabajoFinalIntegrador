package ar.edu.utn.tfi.service;

public interface WhatsAppGateway {
    /**
     * Envía un mensaje de WhatsApp al número destino.
     * El número debería venir ya normalizado (por ej. +549351XXXXXXX).
     */
    void send(String telefonoDestino, String mensaje);
}
