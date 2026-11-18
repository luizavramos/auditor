# auditor

Consumidor-auditor: confere que **cada transação aceita tem exatamente uma decisão** em `fraud-decisions.v1` e que **nenhum alerta saiu em duplicidade** em `fraud-alerts.v1`. Lê os dois tópicos com `read_committed` (as saídas do motor são transacionais).

```bash
./mvnw package
java -jar target/auditor.jar --accepted-log aceitas.log --bootstrap localhost:9092
```

O arquivo de aceitas vem do gerador de carga: `load-generator --accepted-log aceitas.log ...` (só as respostas `ACCEPTED`).
Reprova (código de saída 1) se houver transação aceita sem decisão, decisão ou alerta duplicado, ou decisão MEDIUM/HIGH sem alerta.
Decisões de transações fora do log (uma resposta 503 que o Kafka gravou mesmo assim, ou outra rodada) são só informativas.
Use `--help` para as opções.
