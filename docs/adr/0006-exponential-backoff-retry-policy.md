# ADR-006: Política de Retransmissão com Exponential Backoff e Jitter

## Estado
Aceite (2026-10-26)

## Contexto
Nas redes móveis celulares, o canal rádio está sujeito a desvanecimento temporário (*Rayleigh fading*), ruído eletromagnético, bloqueio físico e congestionamento na torre celular (*Cell Congestion*). Durante estes eventos, a transmissão de um SMS (`AT+CMGS`) pode falhar temporariamente, devolvendo `ERROR` ou `+CMS ERROR: 500`.

Um cliente ingénuo que tente retransmitir imediatamente em loop cerrado provoca tempestades de pedidos (*request storm*), sobrecarregando o barramento do modem e a interface de rádio.

## Decisão
Implementar a extensão de fiabilidade rádio através de uma classe genérica [RetryPolicy.java](file:///Users/antonionascimento/Library/CloudStorage/GoogleDrive-af13741@gmail.com/My%20Drive/Pessoal/Academia/Mestrado/1S/RedesMoveis/cellular-modem-platform/src/main/java/com/celer/client/RetryPolicy.java) com:
1. **Número máximo de tentativas configurável** (padrão: 3).
2. **Backoff Exponencial**: Intervalo de espera cresce exponencialmente a cada falha ($T = \text{initialDelay} \times 2^k$, com valores 500 ms, 1000 ms, 2000 ms).
3. **Jitter Aleatório**: Adição de uma variação pseudo-aleatória ($\pm 100$ ms) para evitar que múltiplos dispositivos sincronizados colidam na mesma janela temporal.

## Consequências
### Positivas
* Garante a entrega resiliente de mensagens SMS críticas (como as confirmações de voto ou alertas de saldo).
* Protege a rede celular contra sobrecarga e sincronização espúria de pacotes.
* Cumpre o requisito de extensão do protocolo de avaliação prática.

### Negativas
* Em caso de falha persistente real da rede, o tempo total de resposta antes de reportar erro definitivo é prolongado.
