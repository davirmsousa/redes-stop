package negocio.cliente;

import negocio.servidor.Servidor;
import negocio.util.Utilitario;
import negocio.util.mensagem.Mensagem;
import negocio.util.mensagem.ObjetivoMensagem;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Scanner;

/*
classe do cliente/jogador.
ela vai ser a responsavel por iniciar a conexao com o servidor e realizar as acoes dos jogadores
quando a classe Stop pedir.

deve estar preparado para quando a classe Stop pedir para inserir as respostas da rodada,
vai receber a letra da rodada e as categorias, deve responder com uma lista de strings

no final do jogo o cliente recebe um relatorio do jogo
*/

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

			Mensagem mensagem = this.lerMensagem();
			System.out.println(mensagem.mensagem);

			if (mensagem.objetivoMensagem == ObjetivoMensagem.FALHA_REGISTRO.obterValor()) {
				return;
			}

			this.tratarMensagens();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * realiza o tratamento das mensagens recebidas via socket, identifica a
	 * acao que deve tomar usando como base o enum ObjetivoMensagem
	 */
	private void tratarMensagens() throws IOException, ClassNotFoundException {
		while (true) {
			Mensagem mensagem = this.lerMensagem();

			switch (ObjetivoMensagem.intParaEnum(mensagem.objetivoMensagem)) {
				case RESPONDER_RODADA:
					System.out.println("RESPONDER_RODADA " + mensagem.mensagem);
					this.responderRodada(mensagem.lista, mensagem.character);
					break;
				case PARAR_RESPOSTA_RODADA:
					System.out.println("PARAR_RESPOSTA_RODADA " + mensagem.mensagem);
					this.interromperRodada();
					break;
				case RELATORIO_RODADA:
					System.out.println("RELATORIO_RODADA " + mensagem.mensagem);
					this.exibirRelatorio(mensagem.conteudo);
					break;
				case RELATORIO_PARTIDA:
					System.out.println("RELATORIO_PARTIDA " + mensagem.mensagem);
					this.exibirRelatorio(mensagem.conteudo);
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

				// TODO: fluxo de resposta
				// a cada insert do jogador adicionar em this.respostas pq quando a thread for abortada alas vao estar salvas

				for (String categoria : categorias) {
					String resposta = this.lerLinha("sua resposta [" + categoria + "]: ");

					// precisa verificar se a thread foi interrompida pq o scanner
					// simplesmente ignora isso
					if (this.threadDeResposta.isInterrupted()) {
						throw new InterruptedException();
					}

					this.respostas.add(resposta);

					System.out.println("->> " + resposta);
				}

				this.enviarResposta();
			} catch (IOException | InterruptedException ignored) { }
		});

		this.threadDeResposta.start();
	}

	/**
	 * interrompe a thread de resposta dos itens da rodada e envia as respostas
	 */
	private void interromperRodada() throws IOException {
		// caso a thread esteja rodando, encerra ela
		if (this.threadDeResposta != null && this.threadDeResposta.isAlive()) {
			this.threadDeResposta.interrupt();
		}

		this.enviarResposta();
	}

	/**
	 * envia as respostas do jogador
	 */
	private synchronized void enviarResposta() throws IOException {
		// caso ja tenha enviado a resposta, nao o faz novamente
		if (this.respostas == null)
			return;

		Mensagem mensagem = new Mensagem(ObjetivoMensagem.RESPOSTA_RODADA)
				.setLista(this.respostas);

		Utilitario.mandarMensagemParaJogador(this.cliente, mensagem);
		this.respostas = null;

		System.out.println("respostas enviadas");
	}

	/**
	 * Exibir o relatorio de final da rodada e coisas relacionadas.
	 * no final o usuario precisa confirmar o fim da rodada para que
	 * a proxima seja iniciada
	 * @param relatorio relatorio retornado pelo jogo
	 */
	private void exibirRelatorio(String relatorio) throws IOException {
		// TODO: printar relatorio
		System.out.println(relatorio);
		this.esperarConfirmacaoDeFimDaRodada();
	}

	/**
	 * esperar a confirmacao do usuario para iniciar a proxima rodada
	 */
	private void esperarConfirmacaoDeFimDaRodada() throws IOException {
		this.lerLinha("pressione qualquer tecla para continuar.");
		Mensagem msg = new Mensagem(ObjetivoMensagem.CONFIRMACAO_RELATORIO_RODADA);
		Utilitario.mandarMensagemParaJogador(this.cliente, msg);

		System.out.println("A próxima rodada vai começar em instantes.");
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
