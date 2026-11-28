# Entrega 2 — Bernardo / RecebimentoService

Registro do trabalho da Entrega 2 para `RecebimentoService` (testes unitários, integração, técnicas funcional, estrutural e baseada em defeitos, SonarQube) e para o teste de sistema do **cadastro de pessoas**, a mesma funcionalidade dos testes manuais da Entrega 1.

Segue a seção 3 do [ENTREGA2.md](ENTREGA2.md#3-convenções): os testes conferem o que a classe **deveria** fazer. Cada teste que falhou virou issue, ficou vermelho até a correção, e a correção entrou junto com as do SonarQube, depois do print "antes".

## Status

| Item | Issue | Resultado | Status |
|---|---|---|---|
| Testes unitários | #97 (de #67) | `RecebimentoServiceTest`: 27 → 38 testes, todos verdes, as 9 dependências isoladas com Mockito | FEITO |
| Integração | #98 (de #69) | `RecebimentoServiceIntegrationTest`: Spring + MySQL do Docker, 4/4 verdes | FEITO |
| Sistema / E2E | #99 (de #73) | `PessoaCadastroE2EIT`: Selenium, 2/2 verdes | FEITO |
| Técnicas funcional, estrutural e baseada em defeitos | #100 (de #74) | funcional abaixo; JaCoCo **36/36 arestas = 100%**; PIT **46/47 = 98%** | FEITO |
| SonarQube | #101 (de #75) | 33 issues → 0 (7h06min → 0), prints e tabela em [metricas/sonar/RecebimentoService-issues.md](../metricas/sonar/RecebimentoService-issues.md) | FEITO |
| Defeitos | #13, #14, #27, #102, #103, #104 | reproduzidos em RED e corrigidos | FEITO |

## Defeitos

Antes da correção, 7 testes falhavam (6 unitários e 1 de integração), e o E2E gravava `20/06/0022` no lugar de `15/01/2000`. Depois da correção, todos ficam verdes.

| Issue | Defeito | Teste | Correção |
|---|---|---|---|
| #13 | título `null` gera `NullPointerException` (`codtitulo == 0` desempacota antes do `== null`) | `testReceberTituloNulo` | ordem invertida: `codtitulo == null \|\| codtitulo == 0` |
| #14 | mesmo cliente rejeitado quando os `Long` são objetos diferentes (`!=` compara referência) | `testAbrirRecebimentoMesmoClienteIdsDistintos` | `equals` |
| #102 | recebimento ou título inexistente gera `NoSuchElementException` | `testReceberRecebimentoInexistente`, `testReceberTituloInexistente` | `orElseThrow` com "Recebimento não encontrado" / "Título não encontrado" |
| #103 | sem caixa aberto, as parcelas são quitadas e só depois estoura `NoSuchElementException` | `testReceberDinheiroSemCaixaAberto`, `recebimentoEmDinheiroSemCaixaAbertoNaoQuitaParcelas` | caixa conferido antes de quitar, mensagem "Nenhum caixa aberto" |
| #104 | valor acima do saldo atual das parcelas entra inteiro no caixa sem quitar nada | `testReceberValorMaiorQueSaldoDasParcelas` | valor também comparado com o saldo atual das parcelas |
| #27 | data de nascimento salva errada: o datepicker usa `yyyy/mm/dd` e o servidor faz parse leniente com `dd/MM/yyyy` | `cadastraPessoaPelaInterface` (E2E) | `pessoa/form.html` com `dd/mm/yyyy`; `PessoaService` com `setLenient(false)` e a mensagem "Data de nascimento inválida, informe no formato dd/mm/aaaa" |

O `testReceberFalhaNaParcela`, da Entrega 1, foi ajustado: ele supunha que o caixa só era consultado depois das parcelas, ordem que a correção da #103 inverte.

Achado lateral, que não está na classe: `Aplicacao.getInstancia()` guarda para sempre o usuário da primeira chamada. Na primeira versão da correção ele trocou o usuário do lançamento, e o `testReceberDinheiro` pegou o erro. A `RecebimentoService` continua com `new Aplicacao()`, como no original. É uma causa provável da #19.

## Testes unitários

`src/test/java/net/originmobi/pdv/service/RecebimentoServiceTest.java` usa `MockitoJUnitRunner` com as 9 dependências de `RecebimentoService` mockadas. Testes novos da Entrega 2:

| Teste | O que confere |
|---|---|
| `testReceberTituloNulo` | título `null` tem a mesma validação do título zero (#13) |
| `testAbrirRecebimentoMesmoClienteIdsDistintos` | parcela do mesmo cliente é aceita com objetos `Long` diferentes (#14) |
| `testReceberRecebimentoInexistente` | "Recebimento não encontrado" (#102) |
| `testReceberTituloInexistente` | "Título não encontrado" (#102) |
| `testReceberDinheiroSemCaixaAberto` | "Nenhum caixa aberto", sem quitar parcelas (#103) |
| `testReceberValorMaiorQueSaldoDasParcelas` | valor acima do saldo atual é rejeitado (#104) |
| `testReceberCartaoNaoConsultaCaixa` | cartão não consulta o caixa e grava título, acréscimo, desconto e data |
| `testReceberValorMinimo` | limite inferior válido (R$ 0,01) |
| `testReceberTotalComCentavosArredondados` | o total é comparado já arredondado em centavos |
| `testReceberLancamentoReferenciaORecebimento` | a observação do lançamento de caixa cita o recebimento |
| `testReceberParcelaSemSaldoNaoConsomeValor` | parcela com saldo zero não consome o valor recebido |
| `testAbrirRecebimentoClienteNaoEncontradoComParcelaValida` | cliente inexistente com parcela válida |
| `testAbrirRecebimentoSegundaParcelaQuitada` | a validação de parcela quitada vale para qualquer posição da lista |

```sh
docker compose run --rm --no-deps pdv-app mvn -Dtest=RecebimentoServiceTest test
```

Resultado: **38 testes, 0 falhas, 0 erros**.

## Técnica funcional

### Classes de equivalência — `receber`

| Entrada | Classe válida | Classes inválidas | Testes |
|---|---|---|---|
| `codtitulo` | título existente | `0`, `null`, inexistente | `testReceberTituloZero`, `testReceberTituloNulo`, `testReceberTituloInexistente` |
| `codreceber` | recebimento aberto | inexistente, já processado | `testReceberRecebimentoInexistente`, `testReceberJaProcessado` |
| `vlrecebido` | `0 < v <= total` e `<= saldo das parcelas` | `<= 0`, `> total`, `> saldo` | `testReceberValorZero`, `testReceberValorNegativo`, `testReceberAcimaDoTotal`, `testReceberValorMaiorQueSaldoDasParcelas` |
| parcelas do recebimento | lista com parcelas | lista vazia | `testReceberSemParcelas` |
| tipo do título | `DIN` (caixa), `CARTDEB`/`CARTCRED` (cartão) | — | `testReceberDinheiro`, `testReceberCartaoDebito`, `testReceberCartaoCredito` |
| caixa (título `DIN`) | aberto | nenhum aberto | `testReceberDinheiroSemCaixaAberto` |

### Classes de equivalência — `abrirRecebimento` e `remover`

| Entrada | Classe válida | Classes inválidas | Testes |
|---|---|---|---|
| parcela | não quitada, do cliente | quitada, de outro cliente | `testAbrirRecebimentoParcelaQuitada`, `testAbrirRecebimentoSegundaParcelaQuitada`, `testAbrirRecebimentoOutroCliente` |
| `codpes` | cliente existente (mesmo valor, qualquer objeto `Long`) | inexistente | `testAbrirRecebimento`, `testAbrirRecebimentoMesmoClienteIdsDistintos`, `testAbrirRecebimentoClienteInexistente`, `testAbrirRecebimentoClienteNaoEncontradoComParcelaValida` |
| recebimento a remover | aberto | processado | `testRemoverRecebimentoAberto`, `testRemoverRecebimentoProcessado` |

### Valores-limite de `vlrecebido` (parcelas de R$ 50 + R$ 50, total R$ 100)

| Valor | Esperado | Teste |
|---|---|---|
| -1,00 | rejeita | `testReceberValorNegativo` |
| 0,00 | rejeita | `testReceberValorZero` |
| 0,01 | aceita, só a 1ª parcela | `testReceberValorMinimo` |
| 30,00 | parcial da 1ª parcela | `testReceberParcial` |
| 50,00 | quita exatamente a 1ª | `testReceberValorIgualPrimeiraParcela` |
| 80,00 | quita a 1ª e abate na 2ª | `testReceberDistribuido` |
| 100,00 | quita as duas | `testReceberTotal` |
| 100,01 | rejeita | `testReceberAcimaDoTotal`, `testReceberTotalComCentavosArredondados` |

### Tabela de decisão de `receber`

| Regra | Título válido | Recebimento aberto | 0 < valor ≤ total e saldo | Tem parcelas | Tipo | Caixa aberto | Resultado | Teste |
|---|---|---|---|---|---|---|---|---|
| R1 | não | — | — | — | — | — | "Selecione um título..." / "Título não encontrado" | `testReceberTitulo*` |
| R2 | sim | não | — | — | — | — | "Recebimento não encontrado" / "já esta fechado" | `testReceberRecebimentoInexistente`, `testReceberJaProcessado` |
| R3 | sim | sim | > total ou > saldo | — | — | — | "superior aos títulos" | `testReceberAcimaDoTotal`, `testReceberValorMaiorQueSaldoDasParcelas` |
| R4 | sim | sim | sim | não | — | — | "não possue parcelas" | `testReceberSemParcelas` |
| R5 | sim | sim | ≤ 0 | sim | — | — | "Valor de recebimento inválido" | `testReceberValorZero`, `testReceberValorNegativo` |
| R6 | sim | sim | sim | sim | DIN | não | "Nenhum caixa aberto", nada quitado | `testReceberDinheiroSemCaixaAberto` |
| R7 | sim | sim | sim | sim | DIN | sim | quita parcelas + lançamento de caixa | `testReceberDinheiro` |
| R8 | sim | sim | sim | sim | cartão | — | quita parcelas + lançamento de cartão | `testReceberCartaoDebito`, `testReceberCartaoCredito`, `testReceberCartaoNaoConsultaCaixa` |

## Técnica estrutural — JaCoCo

```sh
docker compose run --rm --no-deps pdv-app mvn -Dtest=RecebimentoServiceTest test
docker compose run --rm --no-deps pdv-app grep RecebimentoService, target/site/jacoco/jacoco.csv
```

| Estado | Linha do `jacoco.csv` | Arestas |
|---|---|---|
| Testes novos, código original | `RecebimentoService,9,452,1,33,3,88,1,20,0,4` | 33/34 = 97% |
| Depois da correção | `RecebimentoService,0,506,0,36,0,102,0,31,0,13` | **36/36 = 100%** |

No código original, a aresta que faltava era o lado verdadeiro de `codtitulo == null`, inalcançável por causa da #13. A meta era 80%.

## Técnica baseada em defeitos — PIT

`RecebimentoService` foi incluída em `<targetClasses>` e `RecebimentoServiceTest` em `<targetTests>`, no `pom.xml` (seção 6 do [ENTREGA2.md](ENTREGA2.md#6-pit)).

```sh
docker compose run --rm --no-deps pdv-app mvn test-compile pitest:mutationCoverage "-DtimestampedReports=false"
```

Resultado de `RecebimentoService`: 47 mutantes, **46 mortos = 98%**, nenhum sem cobertura, linhas 102/102. Print em [metricas/pit/escore-de-mutacao.png](../metricas/pit/escore-de-mutacao.png).

O único sobrevivente é o `<` → `<=` em `i < listParcelas.size()`, no `quitaParcelas`. Ele é equivalente: como o valor recebido nunca passa do saldo das parcelas (#104), o valor acaba antes de o índice sair da lista.

No código original, a primeira medição foi 36/39 (92%). O mutante desse mesmo laço só morria quando o saldo das parcelas era menor que o total do recebimento, e foi assim que a #104 apareceu.

## Teste de integração

`src/test/java/net/originmobi/pdv/service/RecebimentoServiceIntegrationTest.java` — `@SpringBootTest` + `@Transactional` com o MySQL do Docker. Cada teste cria venda, receber, duas parcelas (R$ 60 + R$ 40), título e, quando precisa, caixa, dentro da transação, que é desfeita no fim.

| Teste | Confere |
|---|---|
| `recebeTotalEmDinheiroQuitaParcelasELancaNoCaixa` | recebimento processado, parcelas quitadas no banco e `caixa_lancamento` de entrada ligado ao recebimento |
| `recebeParcialQuitaPrimeiraParcelaEAbateNaSegunda` | R$ 70 quita a 1ª e deixa R$ 30 na 2ª |
| `recebimentoProcessadoNaoPodeSerRemovido` | remoção bloqueada depois de processar |
| `recebimentoEmDinheiroSemCaixaAbertoNaoQuitaParcelas` | sem caixa, "Nenhum caixa aberto" e nada quitado (#103; pulado se o banco já tiver caixa aberto) |

```sh
docker compose up -d pdv-db
docker compose run --rm --no-deps pdv-app mvn -Dtest=RecebimentoServiceIntegrationTest test
```

Resultado: **4 testes, 0 falhas, 0 erros**.

## Teste de sistema (E2E) — cadastro de pessoas

`src/test/java/net/originmobi/pdv/e2e/PessoaCadastroE2EIT.java`, com a infraestrutura Selenium que o Pedro configurou (`docker-compose.e2e.yml`).

| Teste | Fluxo |
|---|---|
| `cadastraPessoaPelaInterface` | login como `gerente`, preenche nome, apelido, CPF, nascimento, endereço e telefone pelas abas do formulário, salva, confere o alerta "Pessoa salva com sucesso" e confere no banco todos os campos gravados (#27) |
| `rejeitaCpfJaCadastradoPelaInterface` | cadastra uma pessoa e tenta outra com o mesmo CPF; confere o alerta de duplicidade e que só existe um registro |

O `@After` apaga pessoa, endereço e telefones criados.

```sh
docker compose -f docker-compose.yml -f docker-compose.e2e.yml up -d --build pdv-db selenium pdv-app
# esperar "Started PdvApplication" em: docker compose logs -f pdv-app
docker compose -f docker-compose.yml -f docker-compose.e2e.yml run --rm --no-deps pdv-app mvn -Dtest=PessoaCadastroE2EIT test
```

Resultado: **2 testes, 0 falhas, 0 erros**.

## SonarQube

Seção 5 do [ENTREGA2.md](ENTREGA2.md#5-sonarqube). A análise "antes" rodou sobre o código original da classe, antes de qualquer correção. Tabela de cada issue e do que foi feito em [metricas/sonar/RecebimentoService-issues.md](../metricas/sonar/RecebimentoService-issues.md).

| | Antes | Depois |
|---|---|---|
| Issues | 33 | 0 |
| Esforço | 7h06min | 0 |
| Print | [antes](../metricas/sonar/RecebimentoService-antes.png) | [depois](../metricas/sonar/RecebimentoService-depois.png) |

Com 4 GB de memória no Docker, o SonarQube foi morto por falta de memória (OOM) enquanto `pdv-app` e `selenium` também rodavam. Parar os dois antes da análise resolve.

## Regressão

- `RecebimentoServiceTest`: 38/38
- `RecebimentoServiceIntegrationTest`: 4/4
- `PessoaCadastroE2EIT`: 2/2
- PIT das três classes configuradas: `BUILD SUCCESS`
- `mvn test` completo: 163 testes, com 5 failures + 3 errors, todos em `CaixaServiceTest`. As mesmas falhas aparecem no `master` antes deste trabalho (148 testes, 5 + 3), e `CaixaServiceTest` passa 42/42 quando roda sozinho. É interferência entre testes da suíte, fora deste escopo.
