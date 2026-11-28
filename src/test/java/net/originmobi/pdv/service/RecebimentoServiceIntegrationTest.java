package net.originmobi.pdv.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeTrue;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.Map;

import javax.persistence.EntityManager;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integração de RecebimentoService com Spring, JPA e o MySQL do Docker.
 *
 * Os dados (venda, receber, parcelas, título e caixa) são criados dentro da
 * transação do teste, que sofre rollback no fim, então o banco não fica sujo.
 */
@RunWith(SpringRunner.class)
@SpringBootTest
@Transactional
public class RecebimentoServiceIntegrationTest {

	private static final long PESSOA = 1L;
	private static final long USUARIO = 1L;
	private static final long TIPO_DINHEIRO = 1L;

	@Autowired
	private RecebimentoService recebimentoService;

	@Autowired
	private JdbcTemplate jdbc;

	@Autowired
	private EntityManager entityManager;

	private long parcela1;
	private long parcela2;
	private long titulo;

	@Before
	public void preparar() {
		SecurityContextHolder.getContext()
				.setAuthentication(new UsernamePasswordAuthenticationToken("gerente", "naoUtilizada"));

		long venda = insere("insert into venda (observacao, valor_produtos, situacao, pessoa_codigo, usuario_codigo, data_cadastro) "
				+ "values ('Venda integração recebimento', 100.0, 'FECHADA', ?, ?, now())", PESSOA, USUARIO);
		long receber = insere("insert into receber (observacao, valor_total, pessoa_codigo, data_cadastro, venda_codigo) "
				+ "values ('Receber integração', 100.0, ?, now(), ?)", PESSOA, venda);
		parcela1 = insereParcela(receber, 60.0, 1);
		parcela2 = insereParcela(receber, 40.0, 2);
		titulo = insere("insert into titulo (descricao, tipo_codigo) values ('Dinheiro integração', ?)", TIPO_DINHEIRO);
	}

	@After
	public void finalizar() {
		SecurityContextHolder.clearContext();
	}

	@Test
	public void recebeTotalEmDinheiroQuitaParcelasELancaNoCaixa() {
		long caixa = garanteCaixaAberto();

		String codigo = recebimentoService.abrirRecebimento(PESSOA,
				new String[] { Long.toString(parcela1), Long.toString(parcela2) });
		String mensagem = recebimentoService.receber(Long.valueOf(codigo), 100.0, 0.0, 0.0, titulo);
		sincroniza();

		assertEquals("Recebimento realizado com sucesso", mensagem);

		Map<String, Object> recebimento = jdbc.queryForMap("select * from recebimento where codigo = ?", Long.valueOf(codigo));
		assertEquals(100.0, ((Number) recebimento.get("valor_total")).doubleValue(), 0.001);
		assertEquals(100.0, ((Number) recebimento.get("valor_recebido")).doubleValue(), 0.001);
		assertEquals(titulo, ((Number) recebimento.get("titulo_codigo")).longValue());
		assertNotNull(recebimento.get("data_processamento"));

		assertParcela(parcela1, 0.0, 1);
		assertParcela(parcela2, 0.0, 1);

		Map<String, Object> lancamento = jdbc.queryForMap(
				"select * from caixa_lancamento where recebimento_codigo = ?", Long.valueOf(codigo));
		assertEquals(100.0, ((Number) lancamento.get("valor")).doubleValue(), 0.001);
		assertEquals("RECEBIMENTO", lancamento.get("tipo"));
		assertEquals("ENTRADA", lancamento.get("estilo"));
		assertEquals(caixa, ((Number) lancamento.get("caixa_codigo")).longValue());
	}

	@Test
	public void recebeParcialQuitaPrimeiraParcelaEAbateNaSegunda() {
		garanteCaixaAberto();

		String codigo = recebimentoService.abrirRecebimento(PESSOA,
				new String[] { Long.toString(parcela1), Long.toString(parcela2) });
		recebimentoService.receber(Long.valueOf(codigo), 70.0, 0.0, 0.0, titulo);
		sincroniza();

		assertParcela(parcela1, 0.0, 1);
		assertParcela(parcela2, 30.0, 0);
	}

	@Test
	public void recebimentoProcessadoNaoPodeSerRemovido() {
		garanteCaixaAberto();

		String codigo = recebimentoService.abrirRecebimento(PESSOA, new String[] { Long.toString(parcela1) });
		recebimentoService.receber(Long.valueOf(codigo), 60.0, 0.0, 0.0, titulo);
		sincroniza();

		try {
			recebimentoService.remover(Long.valueOf(codigo));
			fail("Era esperado impedir a remoção de recebimento processado");
		} catch (RuntimeException e) {
			assertEquals("Esse recebimento não pode ser removido, pois ele já esta processado", e.getMessage());
		}
	}

	// teste que exercita o comportamento esperado, mas falha hoje (issue associada: #103)
	@Test
	public void recebimentoEmDinheiroSemCaixaAbertoNaoQuitaParcelas() {
		Integer abertos = jdbc.queryForObject(
				"select count(*) from caixa where tipo = 'CAIXA' and data_fechamento is null", Integer.class);
		assumeTrue("Cenário exige banco sem caixa aberto", abertos == 0);

		String codigo = recebimentoService.abrirRecebimento(PESSOA, new String[] { Long.toString(parcela1) });

		try {
			recebimentoService.receber(Long.valueOf(codigo), 60.0, 0.0, 0.0, titulo);
			fail("Era esperado exigir caixa aberto");
		} catch (RuntimeException e) {
			assertEquals("Nenhum caixa aberto", e.getMessage());
		}
		sincroniza();

		assertParcela(parcela1, 60.0, 0);
	}

	private void assertParcela(long codigo, double restante, int quitado) {
		Map<String, Object> parcela = jdbc.queryForMap("select valor_restante, quitado from parcela where codigo = ?", codigo);
		assertEquals(restante, ((Number) parcela.get("valor_restante")).doubleValue(), 0.001);
		assertEquals(quitado, ((Number) parcela.get("quitado")).intValue());
	}

	private long garanteCaixaAberto() {
		return jdbc.query("select codigo from caixa where tipo = 'CAIXA' and data_fechamento is null",
				rs -> rs.next() ? rs.getLong(1)
						: insere("insert into caixa (descricao, valor_abertura, valor_total, tipo, usuario_codigo, data_cadastro) "
								+ "values ('Caixa integração recebimento', 0, 0, 'CAIXA', ?, curdate())", USUARIO));
	}

	private long insereParcela(long receber, double valor, int sequencia) {
		return insere("insert into parcela (valor_total, valor_desconto, valor_acrescimo, valor_recebido, valor_restante, "
				+ "data_cadastro, data_alteracao, data_vencimento, receber_codigo, quitado, sequencia) "
				+ "values (?, 0, 0, 0, ?, now(), now(), now(), ?, 0, ?)", valor, valor, receber, sequencia);
	}

	/** Envia o que o JPA tem pendente e descarta o cache, para ler o estado real do banco. */
	private void sincroniza() {
		entityManager.flush();
		entityManager.clear();
	}

	private long insere(String sql, Object... parametros) {
		KeyHolder chave = new GeneratedKeyHolder();
		jdbc.update(conexao -> {
			PreparedStatement comando = conexao.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
			for (int i = 0; i < parametros.length; i++)
				comando.setObject(i + 1, parametros[i]);
			return comando;
		}, chave);
		return chave.getKey().longValue();
	}
}
