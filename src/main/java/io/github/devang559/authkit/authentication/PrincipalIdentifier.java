package io.github.devang559.authkit.authentication;

import io.github.devang559.authkit.config.IdentifierType;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
@EqualsAndHashCode
public final class PrincipalIdentifier {

    private final IdentifierType type;
    private final String value;

    public PrincipalIdentifier(IdentifierType type, String value) {
        this.type = type;
        this.value = value;
    }
}
