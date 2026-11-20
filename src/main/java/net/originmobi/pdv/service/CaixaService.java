package net.originmobi.pdv.service;

import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import net.originmobi.pdv.enumerado.caixa.CaixaTipo;
import net.originmobi.pdv.enumerado.caixa.EstiloLancamento;
import net.originmobi.pdv.enumerado.caixa.TipoLancamento;
import net.originmobi.pdv.filter.BancoFilter;
import net.originmobi.pdv.filter.CaixaFilter;
import net.originmobi.pdv.model.Caixa;
import net.originmobi.pdv.model.CaixaLancamento;
import net.originmobi.pdv.model.Usuario;
import net.originmobi.pdv.repository.CaixaRepository;
import net.originmobi.pdv.singleton.Aplicacao;

@Service
public class CaixaService {

	private final CaixaRepository caixas;
	private final UsuarioService usuarios;
	private final CaixaLancamentoService lancamentos;

	public CaixaService(CaixaRepository caixas, UsuarioService usuarios, CaixaLancamentoService lancamentos) {
		this.caixas = caixas;
		this.usuarios = usuarios;
		this.lancamentos = lancamentos;
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public Long cadastro(Caixa caixa) {

		if (caixa.getTipo().equals(CaixaTipo.CAIXA) && caixaIsAberto())
			throw new IllegalStateException("Existe caixa de dias anteriores em aberto, favor verifique");

		// caso o valor de abertura seja null, modifica o mesmo para 0.0, esse valor é
		// adicionado tambem no valor_total
		Double vlbertura = caixa.getValor_abertura() == null ? 0.0 : caixa.getValor_abertura();
		caixa.setValor_abertura(vlbertura);

		if (caixa.getValor_abertura() < 0)
			throw new IllegalArgumentException("Valor informado é inválido");

		Aplicacao aplicacao = Aplicacao.getInstancia();
		Usuario usuario = usuarios.buscaUsuario(aplicacao.getUsuarioAtual());

		LocalDate dataAtual = LocalDate.now(ZoneId.systemDefault());

		caixa.setDescricao(descricaoOuPadrao(caixa));
		caixa.setUsuario(usuario);
		caixa.setData_cadastro(Date.valueOf(dataAtual));

		if (caixa.getTipo().equals(CaixaTipo.BANCO))
			validaELimpaAgenciaEConta(caixa);

		try {
			caixas.save(caixa);
		} catch (Exception e) {
			throw new IllegalStateException("Erro no processo de abertura, chame o suporte técnico", e);
		}

		if (caixa.getValor_abertura() > 0) {
			try {
				CaixaLancamento lancamento = new CaixaLancamento(observacaoAbertura(caixa.getTipo()),
						caixa.getValor_abertura(), TipoLancamento.SALDOINICIAL, EstiloLancamento.ENTRADA, caixa,
						usuario);

				lancamentos.lancamento(lancamento);

			} catch (Exception e) {
				throw new IllegalStateException("Erro no processo, chame o suporte", e);
			}
		} else {
			// se não for realizado o lançamento de caixa então joga o valor total do caixa
			// para 0.0
			caixa.setValor_total(0.0);
		}

		return caixa.getCodigo();
	}

	private static String descricaoOuPadrao(Caixa caixa) {
		String descricao = caixa.getDescricao();
		if (descricao != null && !descricao.isEmpty())
			return descricao;

		if (caixa.getTipo().equals(CaixaTipo.CAIXA))
			return "Caixa diário";
		if (caixa.getTipo().equals(CaixaTipo.COFRE))
			return "Cofre";
		return "Banco";
	}

	// valida e remove a máscara de agência e conta
	private static void validaELimpaAgenciaEConta(Caixa banco) {
		if (banco.getAgencia() == null || banco.getAgencia().isEmpty()
				|| banco.getConta() == null || banco.getConta().isEmpty())
			throw new IllegalArgumentException("Agência e conta são obrigatórias");

		banco.setAgencia(banco.getAgencia().replaceAll("\\D", ""));
		banco.setConta(banco.getConta().replaceAll("\\D", ""));
	}

	private static String observacaoAbertura(CaixaTipo tipo) {
		if (tipo.equals(CaixaTipo.CAIXA))
			return "Abertura de caixa";
		if (tipo.equals(CaixaTipo.COFRE))
			return "Abertura de cofre";
		return "Abertura de banco";
	}

	public String fechaCaixa(Long caixa, String senha) {

		Aplicacao aplicacao = Aplicacao.getInstancia();
		Usuario usuario = usuarios.buscaUsuario(aplicacao.getUsuarioAtual());

		BCryptPasswordEncoder decode = new BCryptPasswordEncoder();

		if (senha == null || senha.isEmpty())
			return "Favor, informe a senha";

		if (decode.matches(senha, usuario.getSenha())) {

			// busca caixa atual
			Caixa caixaAtual = caixas.findById(caixa)
					.orElseThrow(() -> new NoSuchElementException("Caixa não encontrado"));

			if (caixaAtual.getData_fechamento() != null)
				throw new IllegalStateException("Caixa já esta fechado");

			Double valorTotal = caixaAtual.getValor_total() == null ? 0.0 : caixaAtual.getValor_total();

			Timestamp dataHoraAtual = new Timestamp(System.currentTimeMillis());
			caixaAtual.setData_fechamento(dataHoraAtual);
			caixaAtual.setValor_fechamento(valorTotal);

			try {
				caixas.save(caixaAtual);
			} catch (Exception e) {
				throw new IllegalStateException("Ocorreu um erro ao fechar o caixa, chame o suporte", e);
			}

			return "Caixa fechado com sucesso";

		} else {
			return "Senha incorreta, favor verifique";
		}
	}

	public boolean caixaIsAberto() {
		return caixas.caixaAberto().isPresent();
	}

	public List<Caixa> listaTodos() {
		return caixas.findByCodigoOrdenado();
	}

	public List<Caixa> listarCaixas(CaixaFilter filter) {
		if (filter.getData_cadastro() != null && !filter.getData_cadastro().isEmpty()) {
			filter.setData_cadastro(filter.getData_cadastro().replace("/", "-"));
			return caixas.buscaCaixasPorDataAbertura(Date.valueOf(filter.getData_cadastro()));
		}
		
		return caixas.listaCaixasAbertos();
	}

	public Optional<Caixa> caixaAberto() {
		return caixas.caixaAberto();
	}

	public List<Caixa> caixasAbertos() {
		return caixas.caixasAbertos();
	}

	public Optional<Caixa> busca(Long codigo) {
		return caixas.findById(codigo);
	}

	// pega o caixa aberto do usuário informado
	public Optional<Caixa> buscaCaixaUsuario(String usuario) {
		Usuario usu = usuarios.buscaUsuario(usuario);
		return Optional.ofNullable(caixas.findByCaixaAbertoUsuario(usu.getCodigo()));
	}

	public List<Caixa> listaBancos() {
		return caixas.buscaBancos(CaixaTipo.BANCO);
	}

	public List<Caixa> listaCaixasAbertosTipo(CaixaTipo tipo) {
		return caixas.buscaCaixaTipo(tipo);
	}

	public List<Caixa> listaBancosAbertosTipoFilterBanco(CaixaTipo tipo, BancoFilter filter) {
		if (filter.getData_cadastro() != null && !filter.getData_cadastro().isEmpty()) {
			filter.setData_cadastro(filter.getData_cadastro().replace("/", "-"));
			return caixas.buscaCaixaTipoData(tipo, Date.valueOf(filter.getData_cadastro()));
		}

		return caixas.buscaCaixaTipo(CaixaTipo.BANCO);
	}

}
