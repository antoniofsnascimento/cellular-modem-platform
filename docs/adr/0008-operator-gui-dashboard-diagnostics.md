# ADR-008: Interface Gráfica de Operador (GUI) para Diagnóstico e Controlo Interativo

## Estado
Aceite (2026-11-04)

## Contexto
O protocolo prático da disciplina (**Secção 3.1**) especifica que a aplicação desenvolvida pode ser implementada em modo linha de comando (CLI) ou interface gráfica (GUI):
> *"Develop an application (CLI/GUI or service) capable of sending/receiving SMS, managing message stores, and triggering supplementary operations (...)"*

Nas semanas iniciais, foi construído um serviço cliente headless autónomo ([ClientApp.java](file:///Users/antonionascimento/Library/CloudStorage/GoogleDrive-af13741@gmail.com/My%20Drive/Pessoal/Academia/Mestrado/1S/RedesMoveis/cellular-modem-platform/src/main/java/com/celer/client/ClientApp.java)), ideal para integração contínua (CI), automação de testes determinísticos e emulação de sistemas embebidos de produção (IoT).

No entanto, para efeitos de demonstração laboratorial, defesa perante o docente e testes exploratórios de comandos manuais, torna-se vantajoso ter um painel visual com indicadores em tempo real (LEDs de registo, potência de sinal em dBm, saldo da operadora e consola de tráfego AT).

## Decisão
Implementar um **Dashboard de Operador em Java Swing ([ModemGuiApp.java](file:///Users/antonionascimento/Library/CloudStorage/GoogleDrive-af13741@gmail.com/My%20Drive/Pessoal/Academia/Mestrado/1S/RedesMoveis/cellular-modem-platform/src/main/java/com/celer/client/ModemGuiApp.java))**:
1. **Nativo do JDK (`java.desktop`)**: Sem dependências externas pesadas (sem JavaFX ou Electron), mantendo o `pom.xml` leve e portável.
2. **Execução Assíncrona Não-Bloqueante**: Todas as interações com o driver de porta série correm numa thread de trabalho dedicada (`ExecutorService.newSingleThreadExecutor()`). As atualizações de interface são despachadas via `SwingUtilities.invokeLater()`, garantindo que a interface gráfica nunca congela durante as latências do emulador de rádio (500–2000 ms).
3. **Consola AT Bidirecional**: Mostra os comandos transmitidos (`[TX]`) e as respostas recebidas (`[RX]`) com carimbo temporal e medição de latência em milissegundos, permitindo também o envio de comandos AT arbitrários.
4. **Extração Dinâmica de Slots SMS**: Em vez de índices estáticos, analisa as notificações espontâneas `+CMTI: "SM",<index>` para manipular dinamicamente o slot real alocado pelo modem.
5. **Independência Operacional**: A GUI corre através de um comando próprio, mantendo a suite de testes de terminal (`ClientApp`) 100% operacional através do seu comando original.

## Consequências
### Positivas
* Permite alternar perfeitamente entre demonstrações automatizadas em terminal (para validação formal de métricas e `telemetry.json`) e demonstrações visuais interativas (para acompanhamento dos estados da FSM e potência de sinal).
* Proporciona feedback visual instantâneo (cores dos LEDs, barra de sinal proporcional ao valor CSQ, saldo formatado).
* Facilita a injeção e teste de votos e mensagens SMS sem necessidade de editar ficheiros de script.

### Negativas
* Requer um ambiente gráfico ativo (display X11/Wayland/macOS Quartz), não sendo executável em servidores Linux puramente headless sem servidor gráfico virtual (ex.: Xvfb).
