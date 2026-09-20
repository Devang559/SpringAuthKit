package io.github.devang559.authkit.schema;

public interface AuthKitSchemaManager {

    SchemaState inspect();

    SchemaDiff compare();

    void validate();
}
