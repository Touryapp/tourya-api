package com.tourya.api._utils;

import com.tourya.api.models.Role;
import com.tourya.api.models.User;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Utils {
    private static final String EMAIL_PATTERN = "(?:[a-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\\.[a-z0-9!#$%&'*+/=?^_`{|}~-]+)*|\"(?:[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x21\\x23-\\x5b\\x5d-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])*\")@(?:(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\\.)+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?|\\[(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?|[a-z0-9-]*[a-z0-9]:(?:[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x21-\\x5a\\x53-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])+)\\])";

    public static boolean isValidEmail(String email) {
        Pattern pattern = Pattern.compile(EMAIL_PATTERN);
        Matcher mather = pattern.matcher(email.toLowerCase());

        return mather.matches();
    }

    public static boolean isAdmin(List<Role> roleList) {
        return roleList != null && roleList.stream()
                .anyMatch(role -> role.getName() != null && "ADMIN".equalsIgnoreCase(role.getName()));
    }

    public static boolean isBackofficeOperation(List<Role> roleList) {
        return roleList != null && roleList.stream()
                .anyMatch(role -> role.getName() != null
                        && "BACKOFFICE_OPERATION".equalsIgnoreCase(role.getName()));
    }

    /** Admin o backoffice operaciones: puede ver/editar porcentaje Tourya. */
    public static boolean isTouryaBackoffice(List<Role> roleList) {
        return isAdmin(roleList) || isBackofficeOperation(roleList);
    }

    /** Roles backoffice desde entidad User y/o authorities del SecurityContext (JWT). */
    public static boolean isTouryaBackoffice(Authentication authentication) {
        if (authentication == null) {
            return false;
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof User user && isTouryaBackoffice(user.getRoles())) {
            return true;
        }
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .map(Utils::normalizeRoleAuthority)
                .anyMatch(name -> "ADMIN".equalsIgnoreCase(name)
                        || "BACKOFFICE_OPERATION".equalsIgnoreCase(name));
    }

    private static String normalizeRoleAuthority(String authority) {
        if (authority == null) {
            return "";
        }
        return authority.startsWith("ROLE_") ? authority.substring(5) : authority;
    }

    public static boolean isProvider(List<Role> roleList) {
        return roleList != null && roleList.stream()
                .anyMatch(role -> role.getName() != null && role.getName().equals("PROVIDER"));
    }

    public static boolean isProviderOperator(List<Role> roleList) {
        return roleList != null && roleList.stream()
                .anyMatch(role -> role.getName() != null && role.getName().equals("PROVIDER_OPERATOR"));
    }

    /** Cuenta de proveedor titular u operador turístico. */
    public static boolean isProviderSide(List<Role> roleList) {
        return isProvider(roleList) || isProviderOperator(roleList);
    }
}
