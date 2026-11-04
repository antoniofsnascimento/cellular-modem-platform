# ADR-004: Gestão do Ciclo de Vida da Memória SIM e Prevenção de Exaustão

## Estado
Aceite (2026-10-22)

## Contexto
Na norma 3GPP TS 27.005, a memória predefinida de armazenamento de mensagens curtas em modems é a memória do cartão SIM (`"SM"`). Fisicamente, a EEPROM de um SIM Card possui capacidade extremamente reduzida, alocando habitualmente espaço para apenas 20 a 50 mensagens SMS.

Num serviço de valor acrescentado de elevado tráfego (como uma Central de Votações por SMS), se as mensagens lidas forem mantidas no cartão, o índice de armazenamento esgota-se rapidamente, fazendo com que o modem rejeite mensagens subsequentes da rede móvel com o erro padrão `+CMS ERROR: 322` (*Memory full*).

## Decisão
Implementar no serviço cliente [ClientApp.java](file:///Users/antonionascimento/Library/CloudStorage/GoogleDrive-af13741@gmail.com/My%20Drive/Pessoal/Academia/Mestrado/1S/RedesMoveis/cellular-modem-platform/src/main/java/com/celer/client/ClientApp.java) uma **política de limpeza atómica imediata**:
1. Receção do evento assíncrono `+CMTI: "SM", <index>`.
2. Leitura da mensagem via `AT+CMGR=<index>`, com extração de remetente e conteúdo.
3. Processamento do voto pelo serviço de domínio.
4. **Eliminação imediata do registo físico através de `AT+CMGD=<index>`**.

## Consequências
### Positivas
* Garante que os slots da memória SIM permanecem livres para novos votos continuamente.
* Evita interrupção do serviço ou saturação do modem em cenários de alta cadência.
* Modela fielmente a prática recomendada de engenharia em gateways GSM industriais.

### Negativas
* O histórico de mensagens não persiste na memória do modem; a responsabilidade de arquivo histórico passa exclusivamente para a base de dados ou logs da aplicação cliente.
