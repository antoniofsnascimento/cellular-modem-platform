# ADR-003: Modelação da Máquina de Estados (FSM) e Atrasos Rádio Reais

## Estado
Aceite (2026-10-20)

## Contexto
Um emulador simplista que responda instantaneamente `OK` a qualquer comando AT ignora os fenómenos físicos de hardware e rádio:
1. No arranque a frio (*Power-on*), os condensadores de alimentação carregam, as tensões estabilizam e o processador de banda base carrega o firmware da memória flash (demora típica: 1.5 a 3 segundos).
2. O cartão SIM requer autenticação de segurança via PIN antes de libertar a identidade IMSI.
3. A procura de rede rádio (*Network Search*) exige que o transceptor RF varra canais de rádio frequência (canais BCCH em 2G ou blocos SSB em 5G) e complete o handshake de fixação de célula (*Cell Selection/Registration*), o que demora vários segundos.

## Decisão
Implementar no emulador [AT.java](file:///Users/antonionascimento/Library/CloudStorage/GoogleDrive-af13741@gmail.com/My%20Drive/Pessoal/Academia/Mestrado/1S/RedesMoveis/cellular-modem-platform/src/main/java/com/celer/emul/AT.java) uma **Máquina de Estados Finitos (FSM) explícita e determinística**:
* `BOOT`: Duração de 2000 ms. Rejeita comandos com `ERROR`.
* `PIN_REQUIRED`: Modem funcional mas rádio desligado. `AT+CPIN?` responde `+CPIN: SIM PIN`. Desbloqueado apenas com `AT+CPIN=0000`.
* `REGISTRATION`: Duração de 5000 ms após desbloqueio. `AT+CREG?` responde `+CREG: 0,2` (Searching). Transita automaticamente para `IDLE` após 5 segundos.
* `IDLE`: Registado na Home Network (`+CREG: 0,1`). Pilha completa de SMS e USSD habilitada.

Associou-se também uma tabela de latências rádio parametrizada (AT básico: 20-50 ms; CSQ: 10 ms; CMGS: 200-800 ms; CUSD: 500-2000 ms).

## Consequências
### Positivas
* Comportamento rádio de alta fidelidade que obriga a aplicação cliente a implementar verificações reais de estado e esperas ativas.
* Previne que uma aplicação tente transmitir mensagens sem antes confirmar o registo na célula.
* Facilidade de demonstração académica das várias fases do ciclo de vida celular.

### Negativas
* Obriga os testes automatizados a aguardar a janela de transição de 5 segundos para o registo na rede.
