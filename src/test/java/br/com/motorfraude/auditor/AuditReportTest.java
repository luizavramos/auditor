package br.com.motorfraude.auditor;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AuditReportTest {

    private static AuditReport.Decision decision(String id, String severity) {
        return new AuditReport.Decision(id, severity);
    }

    private static AuditReport.Alert alert(String id, String alertId) {
        return new AuditReport.Alert(id, alertId);
    }

    @Test
    @DisplayName("uma decisão para cada transação aceita, sem sobra: tudo limpo")
    void limpo() {
        var report = AuditReport.compute(Set.of("tx-1", "tx-2", "tx-3"),
                List.of(decision("tx-1", "NONE"), decision("tx-2", "HIGH"), decision("tx-3", "NONE")), List.of(alert("tx-2", "a-2")));

        assertThat(report.accepted()).isEqualTo(3);
        assertThat(report.decided()).isEqualTo(3);
        assertThat(report.missing()).isEmpty();
        assertThat(report.duplicatedDecisions()).isEmpty();
        assertThat(report.duplicatedAlerts()).isEmpty();
        assertThat(report.alertsMissing()).isEmpty();
        assertThat(report.ok()).isTrue();
    }

    @Test
    @DisplayName("transação aceita sem decisão é listada como faltante e reprova")
    void faltante() {
        var report = AuditReport.compute(Set.of("tx-1", "tx-2"), List.of(decision("tx-1", "NONE")), List.of());

        assertThat(report.missing()).containsExactly("tx-2");
        assertThat(report.ok()).isFalse();
    }

    @Test
    @DisplayName("mais de uma decisão para a mesma transação é duplicidade e reprova")
    void decisaoDuplicada() {
        var report = AuditReport.compute(Set.of("tx-1"), List.of(decision("tx-1", "NONE"), decision("tx-1", "NONE")), List.of());

        assertThat(report.duplicatedDecisions()).containsExactly("tx-1");
        assertThat(report.ok()).isFalse();
    }

    @Test
    @DisplayName("alerta repetido para a mesma transação reprova, mesmo com o mesmo alertId")
    void alertaDuplicado() {
        var report = AuditReport.compute(Set.of("tx-1"), List.of(decision("tx-1", "HIGH")),
                List.of(alert("tx-1", "a-1"), alert("tx-1", "a-1")));

        assertThat(report.duplicatedAlerts()).containsExactly("tx-1");
        assertThat(report.ok()).isFalse();
    }

    @Test
    @DisplayName("decisão MEDIUM ou HIGH sem alerta reprova; decisão NONE não precisa de alerta")
    void alertaFaltante() {
        var report = AuditReport.compute(Set.of("tx-1", "tx-2", "tx-3"),
                List.of(decision("tx-1", "MEDIUM"), decision("tx-2", "HIGH"), decision("tx-3", "NONE")), List.of(alert("tx-2", "a-2")));

        assertThat(report.alertsMissing()).containsExactly("tx-1");
        assertThat(report.ok()).isFalse();
    }

    @Test
    @DisplayName("decisões de transações fora do log de aceitas (503 que o Kafka gravou, ou de outra rodada) são só informativas")
    void extras() {
        var report = AuditReport.compute(Set.of("tx-1"), List.of(decision("tx-1", "NONE"), decision("tx-zzz", "NONE")), List.of());

        assertThat(report.extras()).isEqualTo(1);
        assertThat(report.ok()).isTrue();
    }

    @Test
    @DisplayName("duplicidade em transação fora do log também é denunciada")
    void duplicidadeEmExtra() {
        var report = AuditReport.compute(Set.of("tx-1"), List.of(decision("tx-1", "NONE"), decision("tx-zzz", "NONE"), decision("tx-zzz", "NONE")),
                List.of());

        assertThat(report.duplicatedDecisions()).containsExactly("tx-zzz");
        assertThat(report.ok()).isFalse();
    }

    @Test
    @DisplayName("o resumo diz os números e o veredito")
    void resumo() {
        var ok = AuditReport.compute(Set.of("tx-1"), List.of(decision("tx-1", "NONE")), List.of()).summary();
        var bad = AuditReport.compute(Set.of("tx-1", "tx-2"), List.of(decision("tx-1", "NONE")), List.of()).summary();

        assertThat(ok).contains("aceitas: 1", "faltantes: 0", "APROVADO");
        assertThat(bad).contains("faltantes: 1", "tx-2", "REPROVADO");
    }
}
