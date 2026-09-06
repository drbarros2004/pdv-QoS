# Trabalho COS482 — Qualidade e Teste de Software (2026.2)

Fork de [repo-software-testing-courses/pdv](https://github.com/repo-software-testing-courses/pdv).

## Artefatos

| Artefato | Link |
|---|---|
| Setup do ambiente / guia do grupo | [docs/SETUP.md](docs/SETUP.md) |
| Código-fonte original (fork) | [src/](src/) |
| Testes unitários | [src/test/java](src/test/java) |
| Plano de Teste (IEEE 829) | _a definir — Google Docs_ |
| Casos de teste manuais | _a definir — TestLink / Google Docs_ |
| Bugs reportados | [Issues](https://github.com/drbarros2004/pdv-QoS/issues) |

## Integrantes

| Nome | GitHub | Responsabilidades |
|---|---|---|
| | | |

## Como rodar

Precisa apenas do **Docker Desktop** — não instale Java, Maven nem MySQL.
Com o Docker Desktop aberto, na raiz do projeto:

```sh
docker compose up -d                          # sobe app + banco
docker compose logs -f pdv-app                # acompanha até "Started PdvApplication"
```

A aplicação sobe em <http://localhost:8080> — usuário `gerente`, senha `123`.
A primeira execução leva alguns minutos (baixa a imagem e as dependências Maven).

Testes: `docker compose run --rm pdv-app mvn test`

Todos os comandos e detalhes em [docs/SETUP.md](docs/SETUP.md).

---

## Sobre o sistema

Sistema de ERP web (PDV) desenvolvido em Java com Spring Framework.

### Recursos

- Cadastro de produtos / clientes / fornecedores
- Controle de estoque
- Gerenciar comandas
- Realizar venda
- Controle de fluxo de caixa
- Controle de pagar e receber
- Venda com cartões
- Gerenciar permissões de usuários por grupos
- Cadastrar novas formas de pagamento
- Relatórios

### Tecnologias

Java 8 · Spring Boot 2.0 · Spring Security · Thymeleaf 3 · Hibernate/JPA · Flyway · MySQL 8 · JasperReports
