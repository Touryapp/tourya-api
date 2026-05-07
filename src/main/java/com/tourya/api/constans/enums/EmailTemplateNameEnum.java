package com.tourya.api.constans.enums;

import lombok.Getter;

@Getter
public enum EmailTemplateNameEnum {
    ACTIVATE_ACCOUNT("activate_account"),
    TEMPORARY_PASSWORD("temporary_password");
    private final String name;
    EmailTemplateNameEnum(String name) {
        this.name = name;
    }
}
