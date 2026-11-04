# ADR-007: Telemetria Estruturada e Exportação Contínua em JSON

## Estado
Aceite (2026-10-28)

## Contexto
O marco de avaliação da Semana 7 (*TP1 Checkpoint*) exige a demonstração de observabilidade em sistemas de telecomunicações, incluindo:
* Registo temporal detalhado de cada comando executado e resposta recebida.
* Métricas consolidadas de desempenho (latência média, latência máxima, taxa de erro de comandos).
* Estado final dos serviços de negócio (votos contabilizados, saldo do modem).
* Exportação estruturada para ficheiro acessível para auditoria e avaliação automática.

## Decisão
Criar o módulo [TelemetryCollector.java](file:///Users/antonionascimento/Library/CloudStorage/GoogleDrive-af13741@gmail.com/My%20Drive/Pessoal/Academia/Mestrado/1S/RedesMoveis/cellular-modem-platform/src/main/java/com/celer/client/TelemetryCollector.java) que:
1. Regista de forma imutável cada execução através do record [CommandResult.java](file:///Users/antonionascimento/Library/CloudStorage/GoogleDrive-af13741@gmail.com/My%20Drive/Pessoal/Academia/Mestrado/1S/RedesMoveis/cellular-modem-platform/src/main/java/com/celer/client/CommandResult.java).
2. Calcula em tempo de execução métricas de engenharia (latência média e máxima em ms, taxa de erro em percentagem).
3. Produz um resumo formatado para a consola e serializa os dados em JSON nativo (`telemetry.json`).

## Consequências
### Positivas
* Fornece evidências determinísticas e auditáveis para a avaliação dos docentes.
* Não depende de bibliotecas externas pesadas para serialização JSON (utiliza formatação limpa e determinística).
* Permite integração futura com ferramentas de visualização e dashboards de monitorização (ex.: Grafana, Kibana).

### Negativas
* O ficheiro `telemetry.json` é gerado localmente na raiz do projeto, devendo ser gerido no controlo de versões ou ignorado em execuções sucessivas.
