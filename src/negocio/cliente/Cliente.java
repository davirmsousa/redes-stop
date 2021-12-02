package negocio.cliente;

import negocio.servidor.Servidor;
import negocio.util.Utilitario;
import negocio.util.mensagem.Mensagem;
import negocio.util.mensagem.ObjetivoMensagem;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
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
					System.out.println(mensagem.mensagem);
					this.responderRodada(mensagem.lista, mensagem.character);
					break;
				case PARAR_RESPOSTA_RODADA:
					System.out.println(mensagem.mensagem);
					this.interromperRodada();
					break;
			}
		}
	}

	/**
	 * inicia a thread de resposta das rodadas.
	 * @param categorias
	 * @param inicialSorteada
	 */
	private void responderRodada(ArrayList<String> categorias, Character inicialSorteada) {
		this.respostas = new ArrayList<String>();

		this.threadDeResposta = new Thread(() -> {
			// TODO: fluxo de resposta
			// a cada insert do jogador adicionar em this.respostas pq quando a thread for abortada alas vao estar salvas
			System.out.println("sua resposta: ");
			Scanner sc = new Scanner(System.in);
			sc.nextLine();

			try {
				this.enviarResposta();
			} catch (IOException ignored) { }
		});

		threadDeResposta.start();
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
	private void enviarResposta() throws IOException {
		Mensagem mensagem = new Mensagem(ObjetivoMensagem.RESPOSTA_RODADA)
				.setLista(this.respostas);

		Utilitario.mandarMensagemParaJogador(this.cliente, mensagem);
		System.out.println("respostas enviadas");
	}

	/**
	 * ler o objeto no buffer do socket do cliente
	 * @return objeto de Mensagem
	 */
	private Mensagem lerMensagem() throws IOException, ClassNotFoundException {
		ObjectInputStream clienteInput = new ObjectInputStream(this.cliente.getInputStream());
		return (Mensagem) clienteInput.readObject();
	}

	public static void main(String[] args) {
		new Cliente().conectar(Servidor.HOST, Servidor.PORTA);
	}
}
