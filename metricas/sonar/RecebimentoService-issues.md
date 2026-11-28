# SonarQube: issues da RecebimentoService

Anotações para o relatório de inspeção (#75, #101). SonarQube Community Build 26.9, modo MQR, perfil "Sonar way". Análise "antes" em 28/11/2026 às 01:14 UTC, sobre o código original, e "depois" às 01:32 UTC.

Prints: [antes](RecebimentoService-antes.png), [depois](RecebimentoService-depois.png).

| | Antes | Depois |
|---|---|---|
| Issues | 33 | 0 |
| Esforço | 7h06min | 0 |
| Segurança / Confiabilidade / Manutenibilidade | 0 / 15 / 27 | 0 / 0 / 0 |
| Severidade alta / média / baixa / info | 2 / 31 / 0 / 0 | 0 / 0 / 0 / 0 |

As linhas são as do arquivo antes da correção.

| # | Linha | Mensagem | Regra | Qualidade (severidade) | Categoria | Tags | Esforço | Situação | O que foi feito |
|---|---|---|---|---|---|---|---|---|---|
| 1 | 33 | Remova esta injeção em campo e use injeção por construtor. | java:S6813 | Confiabilidade (média), Manutenibilidade (média) | Consistência | — | 5min | Corrigida | Injeção por construtor, campos `final` |
| 2 | 36 | (igual à 1) | java:S6813 | Confiabilidade (média), Manutenibilidade (média) | Consistência | — | 5min | Corrigida | (igual à 1) |
| 3 | 39 | (igual à 1) | java:S6813 | Confiabilidade (média), Manutenibilidade (média) | Consistência | — | 5min | Corrigida | (igual à 1) |
| 4 | 42 | (igual à 1) | java:S6813 | Confiabilidade (média), Manutenibilidade (média) | Consistência | — | 5min | Corrigida | (igual à 1) |
| 5 | 45 | (igual à 1) | java:S6813 | Confiabilidade (média), Manutenibilidade (média) | Consistência | — | 5min | Corrigida | (igual à 1) |
| 6 | 48 | (igual à 1) | java:S6813 | Confiabilidade (média), Manutenibilidade (média) | Consistência | — | 5min | Corrigida | (igual à 1) |
| 7 | 51 | (igual à 1) | java:S6813 | Confiabilidade (média), Manutenibilidade (média) | Consistência | — | 5min | Corrigida | (igual à 1) |
| 8 | 54 | (igual à 1) | java:S6813 | Confiabilidade (média), Manutenibilidade (média) | Consistência | — | 5min | Corrigida | (igual à 1) |
| 9 | 57 | (igual à 1) | java:S6813 | Confiabilidade (média), Manutenibilidade (média) | Consistência | — | 5min | Corrigida | (igual à 1) |
| 10 | 70 | Substitua exceções genéricas por exceções específicas da biblioteca ou por uma exceção própria. | java:S112 | Manutenibilidade (média) | Intencionalidade | cwe, error-handling, cert | 20min | Corrigida | `IllegalStateException` |
| 11 | 72 | Strings e tipos boxed devem ser comparados com "equals()". | java:S4973 | Confiabilidade (média) | Intencionalidade | cwe, cert | 5min | Corrigida | `equals` no lugar de `!=` (é o bug #14) |
| 12 | 73 | (igual à 10) | java:S112 | Manutenibilidade (média) | Intencionalidade | cwe, error-handling, cert | 20min | Corrigida | `IllegalArgumentException` |
| 13 | 82 | (igual à 10) | java:S112 | Manutenibilidade (média) | Intencionalidade | cwe, error-handling, cert | 20min | Corrigida | Removido o `try` em volta de `lista.add`, que não lançava nada, e o `e.getMessage()` solto |
| 14 | 89 | (igual à 10) | java:S112 | Manutenibilidade (média) | Intencionalidade | cwe, error-handling, cert | 20min | Corrigida | `buscaPessoa(...).orElseThrow(...)` com `IllegalArgumentException` |
| 15 | 97 | (igual à 10) | java:S112 | Manutenibilidade (média) | Intencionalidade | cwe, error-handling, cert | 20min | Corrigida | `IllegalStateException` com a causa original |
| 16 | 104 | Refatore este método para reduzir sua Complexidade Cognitiva de 24 para os 15 permitidos. | java:S3776 | Manutenibilidade (alta) | Adaptabilidade | brain-overload | 14min | Corrigida | Extraídos `emCentavos`, `saldoDasParcelas`, `isCartao`, `quitaParcelas` e `lancaNoCaixa` |
| 17 | 109 | (igual à 10) | java:S112 | Manutenibilidade (média) | Intencionalidade | cwe, error-handling, cert | 20min | Corrigida | `IllegalArgumentException`; a condição virou `codtitulo == null \|\| codtitulo == 0` (é o bug #13) |
| 18 | 112 | (igual à 10) | java:S112 | Manutenibilidade (média) | Intencionalidade | cwe, error-handling, cert | 20min | Corrigida | `IllegalStateException` |
| 19 | 115 | Chame "titulo.isPresent()" antes de acessar o valor. | java:S3655 | Confiabilidade (média) | Intencionalidade | cwe, symbolic-execution | 10min | Corrigida | `orElseThrow` com "Título não encontrado" (é o bug #102) |
| 20 | 115 | Chame "recebimento.isPresent()" antes de acessar o valor. | java:S3655 | Confiabilidade (média) | Intencionalidade | cwe, symbolic-execution | 10min | Corrigida | `orElseThrow` com "Recebimento não encontrado" (é o bug #102) |
| 21 | 119 | Troque esta chamada a "replaceAll()" por "replace()". | java:S5361 | Manutenibilidade (alta) | Intencionalidade | regex, performance | 2min | Corrigida | `replace(",", ".")` |
| 22 | 119 | Chame "Optional#isPresent()" antes de acessar o valor. | java:S3655 | Confiabilidade (média) | Intencionalidade | cwe, symbolic-execution | 10min | Corrigida | Usa o `Recebimento` já desembrulhado |
| 23 | 122 | (igual à 10) | java:S112 | Manutenibilidade (média) | Intencionalidade | cwe, error-handling, cert | 20min | Corrigida | `IllegalArgumentException` |
| 24 | 127 | (igual à 10) | java:S112 | Manutenibilidade (média) | Intencionalidade | cwe, error-handling, cert | 20min | Corrigida | `IllegalStateException` |
| 25 | 130 | (igual à 10) | java:S112 | Manutenibilidade (média) | Intencionalidade | cwe, error-handling, cert | 20min | Corrigida | `IllegalArgumentException` |
| 26 | 153 | (igual à 10) | java:S112 | Manutenibilidade (média) | Intencionalidade | cwe, error-handling, cert | 20min | Corrigida | `IllegalStateException` com a causa original |
| 27 | 163 | (igual à 22) | java:S3655 | Confiabilidade (média) | Intencionalidade | cwe, symbolic-execution | 10min | Corrigida | Usa o `Titulo` já desembrulhado |
| 28 | 172 | Chame "caixa.isPresent()" antes de acessar o valor. | java:S3655 | Confiabilidade (média) | Intencionalidade | cwe, symbolic-execution | 10min | Corrigida | `caixaAberto().orElseThrow(...)` com "Nenhum caixa aberto", **antes** de quitar as parcelas (é o bug #103) |
| 29 | 182 | (igual à 10) | java:S112 | Manutenibilidade (média) | Intencionalidade | cwe, error-handling, cert | 20min | Corrigida | `IllegalStateException` com a causa original |
| 30 | 197 | (igual à 10) | java:S112 | Manutenibilidade (média) | Intencionalidade | cwe, error-handling, cert | 20min | Corrigida | `IllegalStateException` com a causa original |
| 31 | 207 | (igual à 10) | java:S112 | Manutenibilidade (média) | Intencionalidade | cwe, error-handling, cert | 20min | Corrigida | `IllegalStateException` |
| 32 | 212 | Substitua este uso de System.out por um logger. | java:S106 | Manutenibilidade (média) | Adaptabilidade | bad-practice, cert | 10min | Corrigida | Removido; a causa vai junto na exceção |
| 33 | 213 | (igual à 10) | java:S112 | Manutenibilidade (média) | Intencionalidade | cwe, error-handling, cert | 20min | Corrigida | `IllegalStateException` com a causa original |

Além das issues do Sonar, a correção trata a #104: o valor recebido também é comparado com o saldo atual das parcelas, e não só com o total gravado na abertura.
