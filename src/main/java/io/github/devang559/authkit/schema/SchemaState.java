package io.github.devang559.authkit.schema;

import java.util.Map;
import java.util.Set;

public record SchemaState(DbProduct dbProduct,
                          Map<String, ColumnMeta> authUserColumns,
                          Set<String> existingTables) {

    public record ColumnMeta(String name, String type, boolean nullable) {
    }
}
