# ADR-005: Desacoplamento da Lógica de Negócio (VAS) do Driver de Modem

## Estado
Aceite (2026-10-24)

## Contexto
O trabalho prático exige a implementação de serviços de alto nível (VAS — *Value-Added Services*), como uma Central de Votações por SMS e a monitorização de saldo por USSD.

Havia o risco arquitetural de acoplar a lógica de negócio (contagem de votos, regras anti-fraude de voto único, extração regex de saldos) diretamente dentro da classe [SerialModemDriver.java](file:///Users/antonionascimento/Library/CloudStorage/GoogleDrive-af13741@gmail.com/My%20Drive/Pessoal/Academia/Mestrado/1S/RedesMoveis/cellular-modem-platform/src/main/java/com/celer/client/SerialModemDriver.java) ou no próprio emulador [AT.java](file:///Users/antonionascimento/Library/CloudStorage/GoogleDrive-af13741@gmail.com/My%20Drive/Pessoal/Academia/Mestrado/1S/RedesMoveis/cellular-modem-platform/src/main/java/com/celer/emul/AT.java).

Se esse acoplamento ocorresse:
1. O modem deixaria de ser genérico (passaria a "saber" o que é uma votação).
2. Não seria possível enviar ou receber mensagens vulgares entre telemóveis sem passar por regras de votos.
3. Violaria o Princípio da Responsabilidade Única (*Single Responsibility Principle* — SRP).

## Decisão
Seguir os princípios da **Arquitetura Limpa (Clean Architecture)**:
1. O emulador [AT.java](file:///Users/antonionascimento/Library/CloudStorage/GoogleDrive-af13741@gmail.com/My%20Drive/Pessoal/Academia/Mestrado/1S/RedesMoveis/cellular-modem-platform/src/main/java/com/celer/emul/AT.java) e o driver [SerialModemDriver.java](file:///Users/antonionascimento/Library/CloudStorage/GoogleDrive-af13741@gmail.com/My%20Drive/Pessoal/Academia/Mestrado/1S/RedesMoveis/cellular-modem-platform/src/main/java/com/celer/client/SerialModemDriver.java) são componentes genéricos de camada física e de transporte, sem qualquer conhecimento de votos ou tarifários.
2. A lógica de negócio reside em serviços dedicados e testáveis unitariamente:
   * [VotingService.java](file:///Users/antonionascimento/Library/CloudStorage/GoogleDrive-af13741@gmail.com/My%20Drive/Pessoal/Academia/Mestrado/1S/RedesMoveis/cellular-modem-platform/src/main/java/com/celer/client/VotingService.java): Gere as opções válidas, mantém a contagem e aplica a política anti-fraude (1 voto por número de telemóvel).
   * [UssdService.java](file:///Users/antonionascimento/Library/CloudStorage/GoogleDrive-af13741@gmail.com/My%20Drive/Pessoal/Academia/Mestrado/1S/RedesMoveis/cellular-modem-platform/src/main/java/com/celer/client/UssdService.java): Extrai através de expressões regulares o saldo numérico da resposta USSD da operadora e gera alertas de saldo crítico.

## Consequências
### Positivas
* O modem mantém a capacidade de enviar e receber qualquer tipo de mensagem arbitrária sem restrições.
* O `VotingService` pode ser testado isoladamente sem necessidade de portas série ou emuladores a correr.
* Permite ligar facilmente novas interfaces (GUI ou web) aos mesmos serviços.

### Negativas
* Requer um orquestrador ([ClientApp.java](file:///Users/antonionascimento/Library/CloudStorage/GoogleDrive-af13741@gmail.com/My%20Drive/Pessoal/Academia/Mestrado/1S/RedesMoveis/cellular-modem-platform/src/main/java/com/celer/client/ClientApp.java)) que faça a ponte entre a leitura do driver e a invocação do serviço.
