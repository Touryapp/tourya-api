package com.tourya.api.config.auth;

/**
 * Datos ya validados de un usuario que se autentico contra un proveedor social
 * (Google, Facebook, etc). Es la salida de un TokenVerifier — el email/name/sub
 * vienen del proveedor tras verificar la firma/validez del token, no del cliente.
 *
 * <p>SEC-06 (RN-006): el flujo de login social nunca debe confiar en datos que
 * envie el cliente. Este record modela ese contrato: el {@link com.tourya.api.config.auth.AuthenticationService}
 * consume este objeto en lugar de un DTO controlado por el cliente.</p>
 *
 * @param providerSubject identificador estable del usuario en el proveedor
 *                        (sub en Google, id en Facebook)
 * @param email           email verificado por el proveedor
 * @param firstname       nombre (best-effort, puede venir vacio en Facebook)
 * @param lastname        apellido (best-effort)
 */
public record VerifiedSocialUser(
        String providerSubject,
        String email,
        String firstname,
        String lastname
) {
}
