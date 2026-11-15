package br.com.motorfraude.auditor;

import java.nio.file.Path;
import java.time.Duration;

public final class Cli {

    public record Config(Path acceptedLog, String bootstrapServers, String decisionsTopic, String alertsTopic, Duration idle,
            Duration timeout) {
    }

    public static final String HELP = """
            Uso: auditor --accepted-log <arquivo> [opções]

              --accepted-log <arquivo>   transactionId aceitos, um por linha (gerado pelo load-generator --accepted-log)
              --bootstrap <host:porta>   Kafka (padrão localhost:9092)
              --decisions-topic <t>      padrão fraud-decisions.v1
              --alerts-topic <t>         padrão fraud-alerts.v1
              --idle <tempo>             encerra a leitura depois deste tempo sem dado novo (padrão 20s)
              --timeout <tempo>          prazo máximo de leitura por tópico (padrão 5m)
            Códigos de saída: 0 aprovado, 1 reprovado, 2 erro de uso.
            """;

    private Cli() {
    }

    public static Config parse(String[] args) {
        Path accepted = null;
        var bootstrap = "localhost:9092";
        var decisions = "fraud-decisions.v1";
        var alerts = "fraud-alerts.v1";
        var idle = Duration.ofSeconds(20);
        var timeout = Duration.ofMinutes(5);
        for (int i = 0; i < args.length; i++) {
            var option = args[i];
            if (i + 1 >= args.length) {
                throw new IllegalArgumentException("falta o valor de " + option);
            }
            var value = args[++i];
            switch (option) {
                case "--accepted-log" -> accepted = Path.of(value);
                case "--bootstrap" -> bootstrap = value;
                case "--decisions-topic" -> decisions = value;
                case "--alerts-topic" -> alerts = value;
                case "--idle" -> idle = duration(value);
                case "--timeout" -> timeout = duration(value);
                default -> throw new IllegalArgumentException("opção desconhecida: " + option);
            }
        }
        if (accepted == null) {
            throw new IllegalArgumentException("falta --accepted-log");
        }
        return new Config(accepted, bootstrap, decisions, alerts, idle, timeout);
    }

    static Duration duration(String text) {
        try {
            if (text.endsWith("ms")) {
                return Duration.ofMillis(Long.parseLong(text.substring(0, text.length() - 2)));
            }
            if (text.endsWith("s")) {
                return Duration.ofSeconds(Long.parseLong(text.substring(0, text.length() - 1)));
            }
            if (text.endsWith("m")) {
                return Duration.ofMinutes(Long.parseLong(text.substring(0, text.length() - 1)));
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("duração inválida: " + text, e);
        }
        throw new IllegalArgumentException("duração inválida (use ms, s ou m): " + text);
    }
}
