package br.com.motorfraude.auditor;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Set;

public final class AcceptedLog {

    private AcceptedLog() {
    }

    public static Set<String> read(Path file) {
        if (!Files.isRegularFile(file)) {
            throw new IllegalArgumentException("arquivo de transações aceitas não encontrado: " + file);
        }
        try (var lines = Files.lines(file)) {
            var ids = new LinkedHashSet<String>();
            lines.map(String::strip).filter(line -> !line.isEmpty()).forEach(ids::add);
            return ids;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
