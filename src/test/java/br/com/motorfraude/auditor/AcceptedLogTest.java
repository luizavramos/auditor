package br.com.motorfraude.auditor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class AcceptedLogTest {

    @Test
    @DisplayName("lê um transactionId por linha, ignorando linhas em branco e repetidas")
    void le(@TempDir Path tmp) throws Exception {
        var file = tmp.resolve("aceitas.log");
        Files.writeString(file, "tx-1\n\ntx-2\r\ntx-1\n  tx-3  \n");

        assertThat(AcceptedLog.read(file)).containsExactlyInAnyOrder("tx-1", "tx-2", "tx-3");
    }

    @Test
    @DisplayName("arquivo inexistente é um erro claro")
    void inexistente(@TempDir Path tmp) {
        assertThatThrownBy(() -> AcceptedLog.read(tmp.resolve("nao-existe.log"))).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("nao-existe.log");
    }
}
