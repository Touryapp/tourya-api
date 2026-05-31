package com.tourya.api.models.request;

import lombok.Data;

import java.util.List;

@Data
public class UpdateProviderOperatorRequest {

    private String firstname;
    private String lastname;

    /** Si se envía, reemplaza la asignación de tours del operador (no puede estar vacío). */
    private List<Integer> tourIds;

    /**
     * Tour principal del operador.
     * Si se envían tourIds, debe estar incluido.
     * Si solo se envía principalTourId, el operador debe estar ya asignado a ese tour.
     */
    private Integer principalTourId;
}
