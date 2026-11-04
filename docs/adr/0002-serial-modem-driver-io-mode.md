# ADR-002: Modo de Leitura Semi-Bloqueante com Timeout no Driver Série (`jSerialComm`)

## Estado
Aceite (2026-10-18)

## Contexto
Na comunicação com modems celulares, os tempos de resposta aos comandos AT variam drasticamente: um comando local como `AT+CSQ` demora cerca de 10 ms, enquanto o envio de um SMS (`AT+CMGS`) demora entre 200 ms e 800 ms, e uma sessão USSD (`AT+CUSD`) pode demorar até 2000 ms. 

A escolha do modo de I/O na biblioteca `jSerialComm` determinava a estabilidade da leitura:
* **Não Bloqueante (`TIMEOUT_NONBLOCKING`)**: Retorna de imediato se o buffer estiver vazio, originando leituras parciais e falsos fins de mensagem (EOF prematuro).
* **Bloqueante Puro (`TIMEOUT_READ_BLOCKING`)**: Bloqueia a thread até ler o número exato de bytes requisitados. Como o tamanho da resposta AT é desconhecido *a priori*, causava esperas indefinidas (hang).

## Decisão
Configurar o driver [SerialModemDriver.java](file:///Users/antonionascimento/Library/CloudStorage/GoogleDrive-af13741@gmail.com/My%20Drive/Pessoal/Academia/Mestrado/1S/RedesMoveis/cellular-modem-platform/src/main/java/com/celer/client/SerialModemDriver.java) em modo **Semi-Bloqueante (`TIMEOUT_READ_SEMI_BLOCKING`)** com um timeout de leitura de 100 ms entre bytes, associado a um acumulador que pesquisa ativamente os terminadores de protocolo padrão 3GPP (`\r\nOK\r\n`, `\r\nERROR\r\n`, `\r\n+CME ERROR:`, ou o prompt `> ` no caso do SMS).

## Consequências
### Positivas
* Respostas curtas são recolhidas assim que o terminador `OK` chega, sem esperar pelo tempo limite máximo global (5000 ms).
* Suporte nativo ao prompt interativo `> ` necessário para a terminação de mensagens SMS com `<Ctrl+Z>`.
* Imunidade a bloqueios permanentes caso o modem deixe de responder.

### Negativas
* O driver precisa de analisar o fluxo de caracteres linha a linha para detetar os terminadores do protocolo AT.
