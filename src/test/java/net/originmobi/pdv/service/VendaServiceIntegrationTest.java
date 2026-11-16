package net.originmobi.pdv.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.sql.Timestamp;

import javax.persistence.EntityManager;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

import net.originmobi.pdv.enumerado.VendaSituacao;
import net.originmobi.pdv.model.PagamentoTipo;
import net.originmobi.pdv.model.Parcela;
import net.originmobi.pdv.model.Pessoa;
import net.originmobi.pdv.model.Titulo;
import net.originmobi.pdv.model.TituloTipo;
import net.originmobi.pdv.model.Usuario;
import net.originmobi.pdv.model.Venda;
import net.originmobi.pdv.repository.PagamentoTipoRespository;
import net.originmobi.pdv.repository.ParcelaRepository;
import net.originmobi.pdv.repository.PessoaRepository;
import net.originmobi.pdv.repository.TituloRepository;
import net.originmobi.pdv.repository.TituloTipoRepository;
import net.originmobi.pdv.repository.UsuarioRepository;
import net.originmobi.pdv.repository.VendaRepository;

@RunWith(SpringRunner.class)
@SpringBootTest
@Transactional
public class VendaServiceIntegrationTest {

	@Autowired
	private VendaService vendaService;

	@Autowired
	private VendaRepository vendas;

	@Autowired
	private PessoaRepository pessoas;

	@Autowired
	private UsuarioRepository usuarios;

	@Autowired
	private PagamentoTipoRespository pagamentos;

	@Autowired
	private TituloTipoRepository tiposTitulo;

	@Autowired
	private TituloRepository titulos;

	@Autowired
	private ParcelaRepository parcelas;

	@Autowired
	private EntityManager entityManager;

	@Test
	public void fechaVendaAPrazoPersisteVendaEParcela() {
		Pessoa pessoa = pessoas.findById(1L).orElseThrow(AssertionError::new);
		Usuario usuario = usuarios.findById(1L).orElseThrow(AssertionError::new);
		PagamentoTipo pagamento = pagamentos.findById(2L).orElseThrow(AssertionError::new);
		TituloTipo dinheiro = tiposTitulo.findById(1L).orElseThrow(AssertionError::new);

		Titulo titulo = new Titulo();
		titulo.setDescricao("Dinheiro integração");
		titulo.setTipo(dinheiro);
		titulo = titulos.save(titulo);

		Venda venda = new Venda();
		venda.setSituacao(VendaSituacao.ABERTA);
		venda.setValor_produtos(100.0);
		venda.setData_cadastro(new Timestamp(System.currentTimeMillis()));
		venda.setPessoa(pessoa);
		venda.setUsuario(usuario);
		venda = vendas.save(venda);
		Long codigoVenda = venda.getCodigo();

		String mensagem = vendaService.fechaVenda(codigoVenda, pagamento.getCodigo(), 100.0, 10.0, 20.0,
				new String[] { "100" }, new String[] { titulo.getCodigo().toString() });

		entityManager.flush();
		entityManager.clear();

		Venda vendaFechada = vendas.findByCodigoEquals(codigoVenda);
		Parcela parcela = parcelas.findAll().stream()
				.filter(item -> item.getReceber().getVenda().getCodigo().equals(codigoVenda))
				.findFirst().orElse(null);

		assertEquals("Venda finalizada com sucesso", mensagem);
		assertEquals(VendaSituacao.FECHADA, vendaFechada.getSituacao());
		assertEquals(110.0, vendaFechada.getValor_total(), 0.0);
		assertEquals(10.0, vendaFechada.getValor_desconto(), 0.0);
		assertEquals(20.0, vendaFechada.getValor_acrescimo(), 0.0);
		assertNotNull(parcela);
		assertEquals(110.0, parcela.getValor_total(), 0.0);
		assertEquals(110.0, parcela.getValor_restante(), 0.0);
	}
}
