package br.com.motorfraude.auditor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CliTest {

    @Test
    @DisplayName("só o arquivo de aceitas é obrigatório; o resto tem padrão")
    void padroes() {
        var config = Cli.parse(new String[] {"--accepted-log", "aceitas.log"});

        assertThat(config.acceptedLog().toString()).isEqualTo("aceitas.log");
        assertThat(config.bootstrapServers()).isEqualTo("localhost:9092");
        assertThat(config.decisionsTopic()).isEqualTo("fraud-decisions.v1");
        assertThat(config.alertsTopic()).isEqualTo("fraud-alerts.v1");
        assertThat(config.idle()).isEqualTo(Duration.ofSeconds(20));
        assertThat(config.timeout()).isEqualTo(Duration.ofMinutes(5));
    }

    @Test
    @DisplayName("aceita as opções e as durações em s, m e ms")
    void opcoes() {
        var config = Cli.parse(new String[] {"--accepted-log", "a.log", "--bootstrap", "kafka:29092", "--idle", "5s", "--timeout", "2m",
                "--decisions-topic", "d", "--alerts-topic", "al"});

        assertThat(config.bootstrapServers()).isEqualTo("kafka:29092");
        assertThat(config.idle()).isEqualTo(Duration.ofSeconds(5));
        assertThat(config.timeout()).isEqualTo(Duration.ofMinutes(2));
        assertThat(config.decisionsTopic()).isEqualTo("d");
        assertThat(config.alertsTopic()).isEqualTo("al");
    }

    @Test
    @DisplayName("sem o arquivo de aceitas, ou com opção desconhecida, é erro com a lista de opções")
    void erros() {
        assertThatThrownBy(() -> Cli.parse(new String[] {})).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("--accepted-log");
        assertThatThrownBy(() -> Cli.parse(new String[] {"--accepted-log", "a.log", "--xpto", "1"})).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("--xpto");
    }
}
