# ADR-001: Emulação da Camada Física sem Hardware via PTY (`socat`)

## Estado
Aceite (2026-10-15)

## Contexto
O trabalho prático exige a interação completa com um modem celular (GSM/3G/4G) através do conjunto de comandos Hayes AT, sem recorrer a módulos de hardware físicos (dongles USB ou shields UART). Era necessário definir como seria estabelecida a camada de transporte entre a aplicação cliente e o emulador do modem.

Duas abordagens foram consideradas:
1. **Ligação TCP Direta**: A aplicação cliente conecta-se diretamente a uma socket TCP aberta pelo emulador (`127.0.0.1:2001`).
2. **Ponte de Porta Série Virtual (PTY Bridge via `socat`)**: O emulador corre por detrás de um par de pseudo-terminais (`/tmp/ttyB`), emulando um nó físico de porta série POSIX.

## Decisão
Adotou-se a **Ponte de Porta Série Virtual via `socat`** (`socat pty,raw,echo=0,link=/tmp/ttyB tcp:127.0.0.1:2001`).

## Consequências
### Positivas
* **Realismo Rigoroso**: A aplicação cliente usa drivers e bibliotecas de comunicação série reais (`jSerialComm`), tornando o código do cliente 100% reutilizável caso seja ligado no futuro a um modem USB físico real (ex.: Quectel BG96, SIMCom SIM7600 ou Telit) apenas mudando a string da porta para `/dev/ttyUSB0`.
* **Conformidade com o Protocolo**: Cumpre estritamente a arquitetura de laboratório descrita na Secção 3.2 do protocolo da UC (`Application -> Virtual Port A -> bridge -> Virtual Port B -> AT Emulator`).
* **Isolamento de Camadas**: O emulador de rádio corre como processo independente, sem que a aplicação cliente aceda diretamente à sua memória ou instância Java.

### Negativas
* Requer um processo de apoio (`socat`) ativo em segundo plano durante os testes automáticos.
