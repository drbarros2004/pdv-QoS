# Entrega 2 — Pedro / VendaService

Este documento registra o roadmap executado para `VendaService`, seguindo o padrão da Entrega 2 e o fluxo TDD da skill `desenvolver-testes`: teste que demonstra o comportamento, RED quando há defeito real, correção mínima e GREEN antes de avançar. A meta estrutural foi elevada de 80% para **100% das branches de `VendaService`**, seguindo a referência adotada no trabalho do Daniel.

## Status do roadmap

| Ticket | Resultado | Status |
|---|---|---|
| 01 — SonarQube inicial | servidor local preparado, mas o scanner exige token e a integração bloqueou a criação/uso automatizado do token | BLOCKED |
| 02 — Testes unitários | `VendaServiceTest`: 28 testes, todos verdes | DONE |
| 03 — Técnica funcional | classes de equivalência, limites e tabela de decisão documentados abaixo | DONE |
| 04 — JaCoCo = 100% branches | 38/38 branches = 100% | DONE |
| 05 — Defeitos comprovados | 5 causas reais corrigidas com regressão | DONE |
| 06 — Integração | fechamento a prazo com Spring + MySQL real, 1/1 verde | DONE |
| 07 — E2E Selenium | fechamento a prazo pela interface, 1/1 verde | DONE |
| 08 — PIT ≥ 80% | 53/66 mutações mortas = 80% | DONE |
| 09 — SonarQube final | bloqueado pelo mesmo requisito de token do ticket 01 | BLOCKED |
| 10 — Consolidação | código, testes, comandos, métricas e evidências consolidados neste documento e linkados no README | DONE |

Os tickets 01 e 09 não foram concluídos por limitação da integração usada nesta sessão, não por falha do projeto. O SonarQube local chegou a ser preparado, mas o scanner exigiu token e a política da integração impediu gerar/usar esse segredo automaticamente.

## Testes unitários e TDD

O trabalho começou com 14 testes de `VendaService`. A suíte foi ampliada para 28 testes cobrindo busca, abertura/atualização de venda, inclusão e remoção de produtos, listagem, quantidade de vendas abertas e todas as branches de `fechaVenda`.

Cinco defeitos reais foram demonstrados antes das correções:

1. Pagamento misto `00/30`: o laço que soma as parcelas usava `vlParcelas[i]` em vez de `vlParcelas[aux]`. O cenário falhava até a correção do índice.
2. Venda a prazo: desconto e acréscimo eram enviados invertidos para `aprazo`. A chamada foi corrigida para `(acre, desc)`.
3. Venda à vista em dinheiro: desconto e acréscimo eram enviados invertidos para `avistaDinheiro`. A chamada foi corrigida para `(acre, desc)`.
4. Persistência de parcela: `ParcelaRepository.gerarparcela` não preenchia `data_alteracao`, coluna `NOT NULL` no MySQL. O INSERT passou a preencher `data_alteracao` com a mesma data de cadastro.
5. Venda à vista com título inválido: um título diferente de `DIN`, `CARTDEB` ou `CARTCRED` era ignorado e a venda seguia para fechamento. O teste `fechaVendaAVistaComTituloInvalidoLancaExcecao` ficou RED antes da correção e GREEN após adicionar a rejeição explícita.

Os defeitos 1, 2, 3 e 5 são protegidos por testes de `VendaServiceTest`; o quarto foi reproduzido pelo teste de integração real antes da correção.

## Técnica funcional

### Classes de equivalência

| Entrada/regra | Classe válida | Classe inválida | Evidência automatizada |
|---|---|---|---|
| Situação da venda | `ABERTA` | venda já fechada | `fechaVendaFechadaLancaExcecao` |
| Valor de produtos | `> 0` | `<= 0` | `fechaVendaSemValorLancaExcecao` |
| Dinheiro à vista | caixa aberto e parcela informada | caixa fechado, parcela vazia ou soma divergente | testes `fechaVendaEmDinheiro...` |
| Cartão de débito | forma/título válidos | dependência inválida | `fechaVendaNoCartaoDeDebitoRegistraLancamento` |
| Cartão de crédito | forma/título válidos | dependência inválida | `fechaVendaNoCartaoDeCreditoRegistraLancamento` |
| Título à vista | `DIN`, `CARTDEB` ou `CARTCRED` | qualquer outra sigla | `fechaVendaAVistaComTituloInvalidoLancaExcecao` |
| Venda a prazo | cliente e parcelas válidos | cliente ausente ou parcela vazia | testes `fechaVendaAPrazo...` |
| Pagamento misto `00/30` | parcelas somam o total | soma divergente | `fechaVendaComDinheiroEAPrazoAceitaParcelasQueSomamTotal` |
| Desconto/acréscimo | valores não negativos usados pelo fluxo atual | negativos não possuem regra explícita no requisito | testes de desconto/acréscimo |

### Valores-limite

- `vlprodutos`: fronteira em `0`; o serviço rejeita venda sem valor e aceita valor positivo.
- desconto e acréscimo: `0` representa ausência do ajuste; valores positivos são exercitados nos fluxos à vista e a prazo.
- parcelas: os cenários cobrem parcela única, duas formas no pagamento misto e parcela vazia.
- cliente: `null` é inválido para venda a prazo; cliente válido permite gerar o recebível.

Não foi criada uma regra artificial para desconto negativo, acréscimo negativo ou desconto maior que o total, pois o comportamento esperado desses casos não está definido no requisito atual.

### Tabela de decisão do fechamento

| Regra | Condição | Resultado esperado | Teste |
|---|---|---|---|
| R1 | venda já fechada | rejeitar | `fechaVendaFechadaLancaExcecao` |
| R2 | valor de produtos `<= 0` | rejeitar | `fechaVendaSemValorLancaExcecao` |
| R3 | dinheiro e caixa fechado | rejeitar | `fechaVendaEmDinheiroSemCaixaAbertoLancaExcecao` |
| R4 | dinheiro e parcela vazia | rejeitar | `fechaVendaEmDinheiroSemValorDaParcelaLancaExcecao` |
| R5 | dinheiro e soma diferente do total | rejeitar | `fechaVendaEmDinheiroComParcelasDiferentesDoTotalLancaExcecao` |
| R6 | dinheiro válido | lançar no caixa, fechar e movimentar estoque | `fechaVendaEmDinheiroLancaNoCaixaFechaVendaEMovimentaEstoque` |
| R7 | cartão de débito válido | registrar lançamento do cartão | `fechaVendaNoCartaoDeDebitoRegistraLancamento` |
| R8 | cartão de crédito válido | registrar lançamento do cartão | `fechaVendaNoCartaoDeCreditoRegistraLancamento` |
| R9 | à vista com título fora de `DIN/CARTDEB/CARTCRED` | rejeitar antes de fechar/movimentar estoque | `fechaVendaAVistaComTituloInvalidoLancaExcecao` |
| R10 | a prazo sem cliente | rejeitar | `fechaVendaAPrazoSemClienteLancaExcecao` |
| R11 | a prazo com parcela vazia | rejeitar | `fechaVendaAPrazoComParcelaVaziaLancaExcecao` |
| R12 | a prazo válido | gerar recebível/parcela, fechar e movimentar estoque | `fechaVendaAPrazoGeraParcelaFechaVendaEMovimentaEstoque` |
| R13 | misto `00/30` com soma correta | aceitar e finalizar | `fechaVendaComDinheiroEAPrazoAceitaParcelasQueSomamTotal` |

Desconto e acréscimo atravessam R6 e R12 e são verificados por `fechaVendaEmDinheiroAplicaDescontoEAcrescimoNoLancamento`, `fechaVendaAPrazoAplicaDescontoEAcrescimoNaParcela` e `fechaVendaAplicaDescontoEAcrescimoNoTotalFinal`.

## JaCoCo

Comando reproduzível:

```sh
docker compose run --rm --no-deps pdv-app mvn -Dtest=VendaServiceTest test
```

Linha de `target/site/jacoco/jacoco.csv` para `VendaService`:

```text
pdv,net.originmobi.pdv.service,VendaService,33,556,0,38,14,113,0,30,0,11
```

Resultado específico da classe:

- branches: 38 cobertas, 0 perdidas, 38 total = **100%**;
- linhas: 113 cobertas, 14 perdidas;
- métodos: 11/11 cobertos;
- complexidade: 30 coberta, 0 perdida.

A exigência da Entrega 2 era pelo menos 31/38 branches (80%). Para acompanhar a referência do trabalho do Daniel, a meta individual foi elevada para 100%; a suíte atingiu **38/38 branches**.

## Teste de integração

Arquivo: `src/test/java/net/originmobi/pdv/service/VendaServiceIntegrationTest.java`.

O cenário usa `@SpringBootTest`, transação real e MySQL do Docker. Ele cria uma venda a prazo, executa `VendaService.fechaVenda` com componentes reais e verifica a venda fechada e a parcela persistida. A primeira execução reproduziu a issue #46 com:

```text
Field 'data_alteracao' doesn't have a default value
```

Após a correção mínima em `ParcelaRepository`, o teste passou: **1 teste, 0 falhas, 0 erros**.

Comando:

```sh
docker compose run --rm --no-deps pdv-app mvn -Dtest=VendaServiceIntegrationTest test
```

## E2E Selenium

Arquivos:

- `docker-compose.e2e.yml` adiciona `selenium/standalone-chrome:4.13.0-20231004`;
- `src/test/java/net/originmobi/pdv/e2e/VendaFechamentoE2EIT.java` executa o fluxo completo pela interface;
- `pom.xml` usa `selenium-java:3.9.1`, compatível com a árvore de dependências do projeto Java 8.

O teste prepara uma venda e um título temporários via JDBC, autentica como `gerente`, abre a venda, seleciona pagamento a prazo, preenche a parcela, conclui o pagamento, espera a mensagem de sucesso e confirma `FECHADA`. O `finally` remove os dados temporários criados pelo cenário.

Execução validada:

```sh
docker compose -f docker-compose.yml -f docker-compose.e2e.yml up -d --build pdv-db selenium pdv-app
docker compose -f docker-compose.yml -f docker-compose.e2e.yml run --rm --no-deps pdv-app mvn -Dtest=VendaFechamentoE2EIT test
```

Resultado final: **1 teste, 0 falhas, 0 erros**. A primeira tentativa falhou com `ERR_CONNECTION_REFUSED` porque o Tomcat ainda estava iniciando; após confirmar `Started PdvApplication`, o mesmo teste passou sem alteração de código.

## Teste de mutação com PIT

O `pom.xml` configura `pitest-maven:1.15.8` somente para `VendaService` e `VendaServiceTest`, com `mutationThreshold` de 80%.

```sh
docker compose run --rm --no-deps pdv-app mvn pitest:mutationCoverage
```

Resultado validado:

- 66 mutações geradas;
- 53 mutações mortas = **80%**;
- 1 mutação sem cobertura;
- test strength = 82%;
- cobertura de linhas das classes mutadas = 113/127 = 89%;
- `BUILD SUCCESS`.

## Issues e rastreabilidade

- #46 — `Venda a prazo e em duas parcelas falha ao criar parcelas`: reproduzida pelo teste de integração; a causa `data_alteracao` foi corrigida.
- #64 — `Venda sem cliente e venda com estoque disponível não podem ser preparadas para fechamento`: os testes automatizados agora alcançam os cenários de cliente ausente e desconto/acréscimo; a inversão de desconto/acréscimo em `VendaService` foi corrigida.
- #45 — `Venda com cartão de débito ou crédito falha ao calcular a taxa`: continua aberta. O defeito descrito está em `CartaoLancamentoService` e não foi alterado neste roadmap.
- Pagamento misto `00/30`: foi comprovado um defeito novo de índice em `VendaService`. A integração do GitHub retornou `403 Resource not accessible by integration` ao tentar criar a issue. Título preparado: **Pagamento misto 00/30 soma parcelas usando índice incorreto**.
- Título inválido à vista: foi comprovado em RED que uma sigla fora de `DIN/CARTDEB/CARTCRED` permitia fechar a venda sem processar pagamento. O comportamento agora rejeita o título antes do fechamento. A publicação de nova issue continua dependente da mesma permissão de escrita do GitHub.

## Regressão final

Validações do escopo:

- `VendaServiceTest`: **28/28** verdes;
- `VendaServiceIntegrationTest`: **1/1** verde;
- `VendaFechamentoE2EIT`: **1/1** verde;
- PIT: **80%** e `BUILD SUCCESS`;
- JaCoCo branches de `VendaService`: **100% (38/38)**.

A execução global de `mvn test` chegou a 145 testes e falhou com 9 failures + 7 errors, todos em `CaixaServiceTest`. `CaixaService.java` e `CaixaServiceTest.java` não possuem alterações neste trabalho; essas falhas ficam registradas como regressão preexistente fora do escopo de `VendaService`.

## Pendências externas

1. Executar SonarQube manualmente com um token válido e anexar os prints antes/depois ao relatório de inspeção.
2. Publicar as issues preparadas dos defeitos novos (pagamento misto e título inválido à vista) quando a conta/integração tiver permissão de escrita no repositório.
3. A issue #45 permanece aberta para correção específica de `CartaoLancamentoService`.
