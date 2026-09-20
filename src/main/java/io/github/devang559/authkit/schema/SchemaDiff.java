package io.github.devang559.authkit.schema;

import java.util.Set;

public record SchemaDiff(Set<String> missingColumns,
                         Set<String> disabledPresentColumns,
                         Set<String> unexpectedColumns,
                         Set<String> typeMismatches,
                         Set<String> missingTables) {

    public boolean hasBlockingIssues() {
        return !missingColumns.isEmpty() || !missingTables.isEmpty();
    }

    public boolean hasIssues() {
        return hasBlockingIssues() || !disabledPresentColumns.isEmpty()
                || !unexpectedColumns.isEmpty() || !typeMismatches.isEmpty();
    }
}
