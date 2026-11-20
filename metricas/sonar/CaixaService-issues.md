# SonarQube: issues da CaixaService

Anotações para o relatório de inspeção (#75). SonarQube Community Build 26.9, modo MQR, perfil "Sonar way". Análise "antes" em 20/11/2026 às 19:17 UTC e "depois" às 20:26 UTC.

Prints: [antes](CaixaService-antes.png), [depois](CaixaService-depois.png).

| | Antes | Depois |
|---|---|---|
| Issues | 22 | 1 |
| Esforço | 3h56min | 15min |
| Segurança / Confiabilidade / Manutenibilidade | 0 / 6 / 20 | 0 / 0 / 1 |
| Severidade alta / média / baixa / info | 1 / 17 / 2 / 2 | 0 / 0 / 0 / 1 |

As linhas são as do arquivo antes da correção.

| # | Linha | Mensagem | Regra | Qualidade (severidade) | Categoria | Tags | Esforço | Situação | O que foi feito |
|---|---|---|---|---|---|---|---|---|---|
| 1 | 30 | Remova o campo "usuario" e declare-o como variável local. | java:S1450 | Manutenibilidade (baixa) | Intencionalidade | pitfall | 5min | Corrigida | `usuario` e `descricao` viraram variáveis locais (o serviço é singleton, então os campos eram compartilhados entre requisições) |
| 2 | 32 | Remova esta injeção em campo e use injeção por construtor. | java:S6813 | Confiabilidade (média), Manutenibilidade (média) | Consistência | — | 5min | Corrigida | Injeção por construtor, campos `final` |
| 3 | 35 | (igual à 2) | java:S6813 | Confiabilidade (média), Manutenibilidade (média) | Consistência | — | 5min | Corrigida | (igual à 2) |
| 4 | 38 | (igual à 2) | java:S6813 | Confiabilidade (média), Manutenibilidade (média) | Consistência | — | 5min | Corrigida | (igual à 2) |
| 5 | 42 | Refatore este método para reduzir sua Complexidade Cognitiva de 24 para os 15 permitidos. | java:S3776 | Manutenibilidade (alta) | Adaptabilidade | brain-overload | 14min | Corrigida | Extraídos `descricaoOuPadrao`, `validaELimpaAgenciaEConta` e `observacaoAbertura` |
| 6 | 45 | Substitua exceções genéricas por exceções específicas da biblioteca ou por uma exceção própria. | java:S112 | Manutenibilidade (média) | Intencionalidade | cwe, error-handling, cert | 20min | Corrigida | `IllegalStateException` |
| 7 | 53 | (igual à 6) | java:S112 | Manutenibilidade (média) | Intencionalidade | cwe, error-handling, cert | 20min | Corrigida | `IllegalArgumentException` |
| 8 | 65 | Informe o fuso horário explicitamente, passando um ZoneId ou um Clock para o método .now(). | java:S8688 | Confiabilidade (info), Manutenibilidade (info) | Intencionalidade | datetime, java8, timezone, pitfall | 5min | Corrigida | `LocalDate.now(ZoneId.systemDefault())` |
| 9 | 73 | Substitua este uso de System.out por um logger. | java:S106 | Manutenibilidade (média) | Adaptabilidade | bad-practice, cert | 10min | Corrigida | Removido (print de depuração) |
| 10 | 74 | (igual à 9) | java:S106 | Manutenibilidade (média) | Adaptabilidade | bad-practice, cert | 10min | Corrigida | (igual à 9) |
| 11 | 83 | (igual à 6) | java:S112 | Manutenibilidade (média) | Intencionalidade | cwe, error-handling, cert | 20min | Corrigida | `IllegalStateException` com a causa original; removido o `e.getStackTrace()`, que não fazia nada |
| 12 | 90 | Extraia esta operação ternária aninhada para uma instrução independente. | java:S3358 | Manutenibilidade (média) | Intencionalidade | confusing | 5min | Corrigida | Virou o método `observacaoAbertura` |
| 13 | 99 | (igual à 6) | java:S112 | Manutenibilidade (média) | Intencionalidade | cwe, error-handling, cert | 20min | Corrigida | `IllegalStateException` com a causa original |
| 14 | 113 | Renomeie "usuario", que esconde o campo declarado na linha 30. | java:S1117 | Manutenibilidade (média) | Intencionalidade | cert, suspicious, pitfall | 5min | Corrigida | Resolvida junto com a 1 |
| 15 | 126 | (igual à 6) | java:S112 | Manutenibilidade (média) | Intencionalidade | cwe, error-handling, cert | 20min | Corrigida | `IllegalStateException` |
| 16 | 129 | Chame "Optional#isPresent()" antes de acessar o valor. | java:S3655 | Confiabilidade (média) | Intencionalidade | cwe, symbolic-execution | 10min | Corrigida | `findById(...).orElseThrow(...)` com "Caixa não encontrado" (é o bug #21) |
| 17 | 132 | Chame "caixaAtual.isPresent()" antes de acessar o valor. | java:S3655 | Confiabilidade (média) | Intencionalidade | cwe, symbolic-execution | 10min | Corrigida | (igual à 16) |
| 18 | 138 | (igual à 6) | java:S112 | Manutenibilidade (média) | Intencionalidade | cwe, error-handling, cert | 20min | Corrigida | `IllegalStateException` com a causa original |
| 19 | 158 | Junte este if com o if que o envolve. | java:S1066 | Manutenibilidade (média) | Intencionalidade | clumsy | 5min | Corrigida | Condições unidas com `&&` |
| 20 | 182 | Retorne esta expressão direto em vez de atribuí-la à variável temporária "caixaOptional". | java:S1488 | Manutenibilidade (baixa) | Intencionalidade | clumsy | 2min | Corrigida | `return` direto |
| 21 | 196 | (igual à 19) | java:S1066 | Manutenibilidade (média) | Intencionalidade | clumsy | 5min | Corrigida | (igual à 19) |
| 22 | — | Use a API "java.time" para data e hora. | java:S2143 | Manutenibilidade (info) | Consistência | java8, pitfall | 15min | Aberta | Não corrigida: `Caixa.setData_cadastro` e o `CaixaRepository` exigem `java.sql.Date`, e trocar mexe na entidade, no repositório e nas telas |
