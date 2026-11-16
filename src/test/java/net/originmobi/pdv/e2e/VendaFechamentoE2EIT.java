package net.originmobi.pdv.e2e;

import static org.junit.Assert.assertEquals;

import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

import org.junit.Test;
import org.openqa.selenium.Alert;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;

public class VendaFechamentoE2EIT {

	private static final String DB_URL = env("DB_URL",
			"jdbc:mysql://pdv-db/pdv?allowPublicKeyRetrieval=true&useSSL=false");
	private static final String DB_USER = env("DB_USER", "root");
	private static final String DB_PASSWORD = env("DB_PASSWORD", "123456");
	private static final String APP_URL = env("APP_BASE_URL", "http://pdv-app:8080");
	private static final String SELENIUM_URL = env("SELENIUM_REMOTE_URL", "http://selenium:4444/wd/hub");

	@Test
	public void fechaVendaAPrazoPelaInterface() throws Exception {
		long titulo = 0;
		long venda = 0;
		WebDriver driver = null;

		try {
			titulo = insere("insert into titulo (descricao, tipo_codigo) values ('Dinheiro E2E', 1)");
			venda = insere("insert into venda (observacao, valor_produtos, situacao, pessoa_codigo, usuario_codigo, data_cadastro) "
					+ "values ('Venda E2E', 100.0, 'ABERTA', 1, 1, now())");

			driver = new RemoteWebDriver(new URL(SELENIUM_URL), new ChromeOptions());
			WebDriverWait wait = new WebDriverWait(driver, 20);

			driver.get(APP_URL + "/login");
			driver.findElement(By.id("user")).sendKeys("gerente");
			driver.findElement(By.id("password")).sendKeys("123");
			driver.findElement(By.id("btn-login")).click();
			wait.until(ExpectedConditions.not(ExpectedConditions.urlContains("/login")));

			driver.get(APP_URL + "/venda/" + venda);
			wait.until(ExpectedConditions.elementToBeClickable(By.id("btn-venda"))).click();
			new Select(wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("pagamento"))))
					.selectByVisibleText("A Prazo");

			wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector(".vlparcela"))).clear();
			driver.findElement(By.cssSelector(".vlparcela")).sendKeys("100,00");
			wait.until(ExpectedConditions.presenceOfElementLocated(
					By.cssSelector(".selectnovo option[value='" + titulo + "']")));
			new Select(driver.findElement(By.cssSelector(".selectnovo"))).selectByValue(Long.toString(titulo));

			driver.findElement(By.cssSelector(".btn-pagamento")).click();
			Alert alerta = wait.until(ExpectedConditions.alertIsPresent());
			assertEquals("Venda finalizada com sucesso", alerta.getText());
			alerta.accept();

			wait.until(ExpectedConditions.attributeToBe(By.id("statusPedido"), "value", "FECHADA"));
		} finally {
			if (driver != null)
				driver.quit();
			limpa(venda, titulo);
		}
	}

	private long insere(String sql) throws Exception {
		try (Connection conexao = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
				Statement comando = conexao.createStatement()) {
			comando.executeUpdate(sql, Statement.RETURN_GENERATED_KEYS);
			try (ResultSet chaves = comando.getGeneratedKeys()) {
				if (!chaves.next())
					throw new IllegalStateException("Insert E2E não retornou chave");
				return chaves.getLong(1);
			}
		}
	}

	private void limpa(long venda, long titulo) throws Exception {
		try (Connection conexao = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
			if (venda > 0) {
				executa(conexao, "delete from parcela where receber_codigo in (select codigo from receber where venda_codigo = ?)", venda);
				executa(conexao, "delete from receber where venda_codigo = ?", venda);
				executa(conexao, "delete from pagamento_tipo_venda where ven_codigo = ?", venda);
				executa(conexao, "delete from venda where codigo = ?", venda);
			}
			if (titulo > 0)
				executa(conexao, "delete from titulo where codigo = ?", titulo);
		}
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
