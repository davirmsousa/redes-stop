package negocio.cliente;

import negocio.servidor.Servidor;
import negocio.util.Utilitario;
import negocio.util.mensagem.Mensagem;
import negocio.util.mensagem.ObjetivoMensagem;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Scanner;

public class Cliente {

	private Socket cliente;

	private ArrayList<String> respostas;
	private Thread threadDeResposta;

	/**
	 * realizar a conexao via socket em um host e uma porta
	 * @param host host a se conectar
	 * @param porta porta a se conectar
	 */
	public void conectar(String host, int porta) {
		try {
			this.cliente = new Socket(host, porta);
			this.tratarMensagens();
		} catch (Exception e) {
			System.out.println("Ocorreu um erro inesperado.");
		}
	}

	/**
	 * realiza o tratamento das mensagens recebidas via socket, identifica a
	 * acao que deve tomar usando como base o enum ObjetivoMensagem
	 */
	private void tratarMensagens() throws IOException, ClassNotFoundException {
		boolean parar = false;

		while (!parar) {
			Mensagem mensagem = this.lerMensagem();

			switch (ObjetivoMensagem.intParaEnum(mensagem.objetivoMensagem)) {
				case SUCESSO_REGISTRO:
					System.out.println(mensagem.mensagem);
					break;
				case FALHA_REGISTRO:
					System.out.println(mensagem.mensagem);
					parar = true;
					break;
				case CADASTRO_INICIAL:
					Mensagem msg = new Mensagem(ObjetivoMensagem.CADASTRO_INICIAL)
							.setConteudo(this.lerLinha(mensagem.mensagem))
							.setMensagem("Nome de usuario");
					Utilitario.mandarMensagemParaJogador(this.cliente, msg);
					break;
				case RESPONDER_RODADA:
					this.responderRodada(mensagem.lista, mensagem.character);
					break;
				case PARAR_RESPOSTA_RODADA:
					this.interromperRodada();
					break;
				case RELATORIO_RODADA:
					this.exibirRelatorio(mensagem.conteudo, false);
					break;
				case RELATORIO_PARTIDA:
					this.exibirRelatorio(mensagem.conteudo, true);
					System.out.println("O jogo terminou!");
					parar = true;
					break;
			}
		}
	}

	/**
	 * inicia a thread de resposta das rodadas.
	 * @param categorias categorias que o jogador deve responder
	 * @param inicialSorteada letra com a qual as respostas devem comecar
	 */
	private void responderRodada(ArrayList<String> categorias, Character inicialSorteada) {
		this.respostas = new ArrayList<String>();

		this.threadDeResposta = new Thread(() -> {
			try {

				System.out.println("Letra da rodada: " + inicialSorteada);

				for (String categoria : categorias) {
					String resposta = this.lerLinha("sua resposta para a categoria " + categoria + ": ");

					if (this.threadDeResposta.isInterrupted()) {
						throw new InterruptedException();
					}

					this.respostas.add(resposta);
				}

				this.enviarResposta();
			} catch (IOException | InterruptedException ignored) {
				System.out.println("Ocorreu um erro inesperado.");
			}
		});

		this.threadDeResposta.start();
	}

	/**
	 * interrompe a thread de resposta dos itens da rodada e envia as respostas
	 */
	private void interromperRodada() throws IOException {
		if (this.threadDeResposta != null && this.threadDeResposta.isAlive()) {
			this.threadDeResposta.interrupt();
		}

		this.enviarResposta();
	}

	/**
	 * envia as respostas do jogador
	 */
	private synchronized void enviarResposta() throws IOException {
		if (this.respostas == null)
			return;

		Mensagem mensagem = new Mensagem(ObjetivoMensagem.RESPOSTA_RODADA)
				.setLista(this.respostas);

		Utilitario.mandarMensagemParaJogador(this.cliente, mensagem);
		this.respostas = null;
	}

	/**
	 * Exibir o relatorio de final da rodada e coisas relacionadas.
	 * no final o usuario precisa confirmar o fim da rodada para que
	 * a proxima seja iniciada
	 * @param relatorio relatorio retornado pelo jogo
	 * @param relatorioFinal se esse eh o relatorio de final do jogo
	 */
	private void exibirRelatorio(String relatorio, boolean relatorioFinal) throws IOException {
		System.out.println("\n" + relatorio + "\n");

		if (!relatorioFinal)
			this.esperarConfirmacaoDeFimDaRodada();
	}

	/**
	 * esperar a confirmacao do usuario para iniciar a proxima rodada
	 */
	private void esperarConfirmacaoDeFimDaRodada() throws IOException {
		this.lerLinha("pressione qualquer tecla para continuar.");
		Mensagem msg = new Mensagem(ObjetivoMensagem.CONFIRMACAO_RELATORIO_RODADA);
		Utilitario.mandarMensagemParaJogador(this.cliente, msg);
		System.out.println("Finalizando rodada...");
	}

	/**
	 * ler o objeto no buffer do socket do cliente
	 * @return objeto de Mensagem
	 */
	private Mensagem lerMensagem() throws IOException, ClassNotFoundException {
		ObjectInputStream clienteInput = new ObjectInputStream(this.cliente.getInputStream());
		return (Mensagem) clienteInput.readObject();
	}

	/**
	 * exibe uma mensagem para o usuario e espera o input no console
	 * @param mensagem mensagem para exibir solicitando o input
	 * @return input do usuario
	 */
	private String lerLinha(String mensagem) {
		System.out.print(mensagem);
		return new Scanner(System.in).nextLine();
	}

	public static void main(String[] args) {
		new Cliente().conectar(Servidor.HOST, Servidor.PORTA);
	}
}
