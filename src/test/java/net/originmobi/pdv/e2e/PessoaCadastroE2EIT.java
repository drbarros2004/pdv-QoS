package net.originmobi.pdv.e2e;

import static org.junit.Assert.assertEquals;

import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openqa.selenium.Alert;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;

/**
 * Teste de sistema (E2E) do cadastro de pessoas pela interface, com Selenium.
 * Mesma funcionalidade dos testes manuais do TestLink (Bernardo Pozzato).
 */
public class PessoaCadastroE2EIT {

	private static final String DB_URL = env("DB_URL",
			"jdbc:mysql://pdv-db/pdv?allowPublicKeyRetrieval=true&useSSL=false");
	private static final String DB_USER = env("DB_USER", "root");
	private static final String DB_PASSWORD = env("DB_PASSWORD", "123456");
	private static final String APP_URL = env("APP_BASE_URL", "http://pdv-app:8080");
	private static final String SELENIUM_URL = env("SELENIUM_REMOTE_URL", "http://selenium:4444/wd/hub");

	private WebDriver driver;
	private WebDriverWait wait;
	private final List<String> cpfsCriados = new ArrayList<>();

	@Before
	public void abrirNavegador() throws Exception {
		driver = new RemoteWebDriver(new URL(SELENIUM_URL), new ChromeOptions());
		wait = new WebDriverWait(driver, 20);

		driver.get(APP_URL + "/login");
		driver.findElement(By.id("user")).sendKeys("gerente");
		driver.findElement(By.id("password")).sendKeys("123");
		driver.findElement(By.id("btn-login")).click();
		wait.until(ExpectedConditions.not(ExpectedConditions.urlContains("/login")));
	}

	@After
	public void fecharNavegador() throws Exception {
		if (driver != null)
			driver.quit();
		for (String cpf : cpfsCriados)
			limpa(cpf);
	}

	// teste que exercita o comportamento esperado, mas falha hoje (issue associada: #27)
	@Test
	public void cadastraPessoaPelaInterface() throws Exception {
		String cpf = cpfUnico();

		String mensagem = preencheESalva("Maria E2E Recebimento", cpf, "15/01/2000");

		assertEquals("Pessoa salva com sucesso", mensagem);
		try (Connection conexao = conecta();
				PreparedStatement consulta = conexao.prepareStatement(
						"select p.nome, p.apelido, date_format(p.data_nascimento, '%d/%m/%Y') nascimento, "
								+ "e.rua, e.bairro, e.numero, e.cidade_codigo, t.fone, t.tipo "
								+ "from pessoa p join endereco e on e.codigo = p.endereco_codigo "
								+ "join pessoa_telefone pt on pt.pessoa_codigo = p.codigo "
								+ "join telefone t on t.codigo = pt.telefone_codigo where p.cpfcnpj = ?")) {
			consulta.setString(1, cpf);
			try (ResultSet pessoa = consulta.executeQuery()) {
				assertEquals("pessoa deveria ter sido gravada", true, pessoa.next());
				assertEquals("Maria E2E Recebimento", pessoa.getString("nome"));
				assertEquals("Mari", pessoa.getString("apelido"));
				assertEquals("15/01/2000", pessoa.getString("nascimento"));
				assertEquals("Rua das Flores", pessoa.getString("rua"));
				assertEquals("Centro", pessoa.getString("bairro"));
				assertEquals("123", pessoa.getString("numero"));
				assertEquals(1, pessoa.getInt("cidade_codigo"));
				assertEquals("69999990000", pessoa.getString("fone"));
				assertEquals("CELULAR", pessoa.getString("tipo"));
				assertEquals("deveria existir uma única pessoa com o CPF", false, pessoa.next());
			}
		}
	}

	@Test
	public void rejeitaCpfJaCadastradoPelaInterface() throws Exception {
		String cpf = cpfUnico();
		assertEquals("Pessoa salva com sucesso", preencheESalva("Primeiro Cadastro E2E", cpf, "15/01/2000"));

		driver.get(APP_URL + "/pessoa/form");
		String mensagem = preencheESalva("Segundo Cadastro E2E", cpf, "20/02/1990");

		assertEquals("Já existe uma pessoa cadastrada com este CPF/CNPJ, verifique", mensagem);
		try (Connection conexao = conecta();
				PreparedStatement consulta = conexao.prepareStatement("select count(*) from pessoa where cpfcnpj = ?")) {
			consulta.setString(1, cpf);
			try (ResultSet total = consulta.executeQuery()) {
				total.next();
				assertEquals(1, total.getInt(1));
			}
		}
	}

	/** Preenche o formulário como o usuário faria, salva e devolve o texto do alerta exibido. */
	private String preencheESalva(String nome, String cpf, String nascimento) {
		cpfsCriados.add(cpf);
		if (!driver.getCurrentUrl().endsWith("/pessoa/form"))
			driver.get(APP_URL + "/pessoa/form");

		wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("nome"))).sendKeys(nome);
		driver.findElement(By.id("apelido")).sendKeys("Mari");
		driver.findElement(By.id("cpfcnpj")).sendKeys(cpf);
		WebElement data = driver.findElement(By.id("nascimento"));
		data.sendKeys(nascimento);
		data.sendKeys(Keys.ESCAPE);

		driver.findElement(By.cssSelector("a[href='#menu1']")).click();
		new Select(wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("cidade")))).selectByValue("1");
		driver.findElement(By.id("rua")).sendKeys("Rua das Flores");
		driver.findElement(By.id("bairro")).sendKeys("Centro");
		driver.findElement(By.id("numero")).sendKeys("123");
		driver.findElement(By.id("cep")).sendKeys("76960000");

		driver.findElement(By.cssSelector("a[href='#menu2']")).click();
		wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("fone"))).sendKeys("69999990000");
		new Select(driver.findElement(By.name("tipo"))).selectByVisibleText("CELULAR");

		WebElement salvar = driver.findElement(By.cssSelector("#form_pessoa input[type='submit']"));
		((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", salvar);
		salvar.click();

		Alert alerta = wait.until(ExpectedConditions.alertIsPresent());
		String texto = alerta.getText();
		alerta.accept();
		return texto;
	}

	private String cpfUnico() {
		String digitos = Long.toString(System.nanoTime());
		return ("9" + digitos).substring(0, 11);
	}

	private void limpa(String cpf) throws Exception {
		try (Connection conexao = conecta()) {
			List<long[]> pessoas = new ArrayList<>();
			try (PreparedStatement consulta = conexao
					.prepareStatement("select codigo, endereco_codigo from pessoa where cpfcnpj = ?")) {
				consulta.setString(1, cpf);
				try (ResultSet rs = consulta.executeQuery()) {
					while (rs.next())
						pessoas.add(new long[] { rs.getLong(1), rs.getLong(2) });
				}
			}
			for (long[] pessoa : pessoas) {
				List<Long> telefones = new ArrayList<>();
				try (PreparedStatement consulta = conexao
						.prepareStatement("select telefone_codigo from pessoa_telefone where pessoa_codigo = ?")) {
					consulta.setLong(1, pessoa[0]);
					try (ResultSet rs = consulta.executeQuery()) {
						while (rs.next())
							telefones.add(rs.getLong(1));
					}
				}
				executa(conexao, "delete from pessoa_telefone where pessoa_codigo = ?", pessoa[0]);
				executa(conexao, "delete from pessoa where codigo = ?", pessoa[0]);
				executa(conexao, "delete from endereco where codigo = ?", pessoa[1]);
				for (Long telefone : telefones)
					executa(conexao, "delete from telefone where codigo = ?", telefone);
			}
		}
	}

	private Connection conecta() throws Exception {
		return DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
	}

	private void executa(Connection conexao, String sql, long codigo) throws Exception {
		try (PreparedStatement comando = conexao.prepareStatement(sql)) {
			comando.setLong(1, codigo);
			comando.executeUpdate();
		}
	}

	private static String env(String nome, String padrao) {
		String valor = System.getenv(nome);
		return valor == null || valor.isEmpty() ? padrao : valor;
	}
}
