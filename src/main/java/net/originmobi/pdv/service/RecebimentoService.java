package net.originmobi.pdv.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.ibm.icu.text.DecimalFormat;

import net.originmobi.pdv.controller.TituloService;
import net.originmobi.pdv.enumerado.TituloTipo;
import net.originmobi.pdv.enumerado.caixa.EstiloLancamento;
import net.originmobi.pdv.enumerado.caixa.TipoLancamento;
import net.originmobi.pdv.model.Caixa;
import net.originmobi.pdv.model.CaixaLancamento;
import net.originmobi.pdv.model.Parcela;
import net.originmobi.pdv.model.Pessoa;
import net.originmobi.pdv.model.Recebimento;
import net.originmobi.pdv.model.Titulo;
import net.originmobi.pdv.model.Usuario;
import net.originmobi.pdv.repository.RecebimentoRepository;
import net.originmobi.pdv.service.cartao.CartaoLancamentoService;
import net.originmobi.pdv.singleton.Aplicacao;
import net.originmobi.pdv.utilitarios.DataAtual;

@Service
public class RecebimentoService {

	private static final String ERRO_RECEBER = "Erro ao receber, chame o suporte";
	private static final String ERRO_RECEBIMENTO = "Ocorreu um erro ao realizar o recebimento, chame o suporte";

	private final RecebimentoRepository recebimentos;
	private final PessoaService pessoas;
	private final RecebimentoParcelaService receParcelas;
	private final ParcelaService parcelas;
	private final CaixaService caixas;
	private final UsuarioService usuarios;
	private final CaixaLancamentoService lancamentos;
	private final TituloService titulos;
	private final CartaoLancamentoService cartaoLancamentos;

	public RecebimentoService(RecebimentoRepository recebimentos, PessoaService pessoas,
			RecebimentoParcelaService receParcelas, ParcelaService parcelas, CaixaService caixas,
			UsuarioService usuarios, CaixaLancamentoService lancamentos, TituloService titulos,
			CartaoLancamentoService cartaoLancamentos) {
		this.recebimentos = recebimentos;
		this.pessoas = pessoas;
		this.receParcelas = receParcelas;
		this.parcelas = parcelas;
		this.caixas = caixas;
		this.usuarios = usuarios;
		this.lancamentos = lancamentos;
		this.titulos = titulos;
		this.cartaoLancamentos = cartaoLancamentos;
	}

	public String abrirRecebimento(Long codpes, String[] arrayParcelas) {
		List<Parcela> lista = new ArrayList<>();

		DataAtual dataAtual = new DataAtual();
		Double vlTotal = 0.0;

		for (int i = 0; i < arrayParcelas.length; i++) {
			Parcela parcela = parcelas.busca(Long.decode(arrayParcelas[i]));

			if (parcela.getQuitado() == 1)
				throw new IllegalStateException("Parcela " + parcela.getCodigo() + " já esta quitada, verifique.");

			if (!parcela.getReceber().getPessoa().getCodigo().equals(codpes))
				throw new IllegalArgumentException(
						"A parcela " + parcela.getCodigo() + " não pertence ao cliente selecionado");

			lista.add(parcela);
			vlTotal = vlTotal + parcela.getValor_restante();
		}

		Pessoa pessoa = pessoas.buscaPessoa(codpes)
				.orElseThrow(() -> new IllegalArgumentException("Cliente não encontrado"));

		Recebimento recebimento = new Recebimento(vlTotal, dataAtual.dataAtualTimeStamp(), pessoa, lista);

		try {
			recebimentos.save(recebimento);
		} catch (Exception e) {
			throw new IllegalStateException(ERRO_RECEBER, e);
		}

		return recebimento.getCodigo().toString();
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public String receber(Long codreceber, Double vlrecebido, Double vlacrescimo, Double vldesconto, Long codtitulo) {
		Optional<Recebimento> recebimentoOptional = recebimentos.findById(codreceber);
		Optional<Titulo> tituloOptional = titulos.busca(codtitulo);

		if (codtitulo == null || codtitulo == 0)
			throw new IllegalArgumentException("Selecione um título para realizar o recebimento");

		Recebimento recebimento = recebimentoOptional
				.orElseThrow(() -> new IllegalArgumentException("Recebimento não encontrado"));
		Titulo titulo = tituloOptional.orElseThrow(() -> new IllegalArgumentException("Título não encontrado"));

		if (recebimento.getData_processamento() != null)
			throw new IllegalStateException("Recebimento já esta fechado");

		// vincula o titulo ao recebimento
		recebimento.setTitulo(titulo);

		if (vlrecebido > emCentavos(recebimento.getValor_total()))
			throw new IllegalArgumentException("Valor de recebimento é superior aos títulos");

		List<Parcela> listParcelas = receParcelas.parcelasDoReceber(codreceber);

		if (listParcelas.isEmpty())
			throw new IllegalStateException("Recebimento não possue parcelas");

		if (vlrecebido <= 0.0)
			throw new IllegalArgumentException("Valor de recebimento inválido");

		// o saldo das parcelas pode ter caído depois da abertura do recebimento
		if (vlrecebido > emCentavos(saldoDasParcelas(listParcelas)))
			throw new IllegalArgumentException("Valor de recebimento é superior aos títulos");

		boolean cartao = isCartao(titulo);

		// fora do cartão o valor entra no caixa, que precisa estar aberto antes de quitar as parcelas
		Caixa caixa = cartao ? null
				: caixas.caixaAberto().orElseThrow(() -> new IllegalStateException("Nenhum caixa aberto"));

		quitaParcelas(listParcelas, vlrecebido);

		Usuario usuario = usuarios.buscaUsuario(new Aplicacao().getUsuarioAtual());

		if (cartao)
			cartaoLancamentos.lancamento(vlrecebido, tituloOptional);
		else
			lancaNoCaixa(recebimento, vlrecebido, caixa, usuario);

		try {
			DataAtual dataAtual = new DataAtual();

			recebimento.setValor_recebido(vlrecebido);
			recebimento.setValor_acrescimo(vlacrescimo);
			recebimento.setValor_desconto(vldesconto);
			recebimento.setData_processamento(dataAtual.dataAtualTimeStamp());

			recebimentos.save(recebimento);
		} catch (Exception e) {
			throw new IllegalStateException(ERRO_RECEBIMENTO, e);
		}

		return "Recebimento realizado com sucesso";
	}

	private static Double emCentavos(Double valor) {
		DecimalFormat formata = new DecimalFormat("0.00");
		return Double.valueOf(formata.format(valor).replace(",", "."));
	}

	private static Double saldoDasParcelas(List<Parcela> listParcelas) {
		Double saldo = 0.0;
		for (Parcela parcela : listParcelas)
			saldo = saldo + parcela.getValor_restante();
		return saldo;
	}

	private static boolean isCartao(Titulo titulo) {
		String sigla = titulo.getTipo().getSigla();
		return sigla.equals(TituloTipo.CARTDEB.toString()) || sigla.equals(TituloTipo.CARTCRED.toString());
	}

	// distribui o valor recebido pelas parcelas, na ordem, até acabar
	private void quitaParcelas(List<Parcela> listParcelas, Double vlrecebido) {
		Double restante = vlrecebido;

		for (int i = 0; i < listParcelas.size() && restante > 0; i++) {
			Parcela parcela = listParcelas.get(i);

			Double vlsobra = Math.max(restante - parcela.getValor_restante(), 0);
			Double vlquitado = restante - vlsobra;
			restante = vlsobra;

			try {
				parcelas.receber(parcela.getCodigo(), vlquitado, 0.00, 0.00);
			} catch (Exception e) {
				throw new IllegalStateException(ERRO_RECEBIMENTO, e);
			}
		}
	}

	private void lancaNoCaixa(Recebimento recebimento, Double valor, Caixa caixa, Usuario usuario) {
		CaixaLancamento lancamento = new CaixaLancamento("Referente ao recebimento " + recebimento.getCodigo(),
				valor, TipoLancamento.RECEBIMENTO, EstiloLancamento.ENTRADA, caixa, usuario);

		// vincula o recebimento ao caixa_lancamento
		lancamento.setRecebimento(recebimento);

		try {
			lancamentos.lancamento(lancamento);
		} catch (Exception e) {
			throw new IllegalStateException(ERRO_RECEBIMENTO, e);
		}
	}

	public String remover(Long codigo) {
		Optional<Recebimento> recebimento = recebimentos.findById(codigo);

		if (recebimento.map(Recebimento::getData_processamento).isPresent())
			throw new IllegalStateException("Esse recebimento não pode ser removido, pois ele já esta processado");

		try {
			recebimentos.deleteById(codigo);
		} catch (Exception e) {
			throw new IllegalStateException("Erro ao remover orçamento, chame o suporte", e);
		}

		return "removido com sucesso";
	}

}
