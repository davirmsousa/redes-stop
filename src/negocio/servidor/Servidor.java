package negocio.servidor;

import negocio.jogo.Jogador;
import negocio.jogo.Stop;
import negocio.util.Utilitario;
import negocio.util.mensagem.Mensagem;
import negocio.util.mensagem.ObjetivoMensagem;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class Servidor {

	public static final int PORTA = 1234;
	public static final String HOST = "";

	public void run() {
		Stop stop = new Stop();

		try {
			ServerSocket servidor = new ServerSocket(Servidor.PORTA);
			System.out.println("Servidor ativo");

			while (true) {
				Socket sctJogador = servidor.accept();
				Jogador jogador = realizarCadastro(sctJogador);
				stop.adicionarJogador(jogador);
			}

		} catch (Exception e) {
			System.out.println("Servidor quebrou: " + e.getMessage());
		}
	}

	private Jogador realizarCadastro(Socket socket) throws IOException, ClassNotFoundException {
		Mensagem mensagem = new Mensagem(ObjetivoMensagem.CADASTRO_INICIAL)
				.setMensagem("Registre seu nome de jogador: ");

		Utilitario.mandarMensagemParaJogador(socket, mensagem);

		mensagem = this.lerMensagem(socket, ObjetivoMensagem.CADASTRO_INICIAL);

		return new Jogador(mensagem.conteudo, socket);
	}

	/**
	 * espera a resposta do jogador
	 * @param jogador socket do jogador
	 * @return Mensagem do jogador
	 */
	@SuppressWarnings (value="unchecked")
	public Mensagem lerMensagem(Socket jogador, ObjetivoMensagem objetivo) throws IOException, ClassNotFoundException {
		ObjectInputStream jogadorInput = new ObjectInputStream(jogador.getInputStream());
		Object objeto = jogadorInput.readObject();

		if (!(objeto instanceof Mensagem) || (objetivo != null && ((Mensagem) objeto).objetivoMensagem != objetivo.obterValor())) {
			throw new IllegalArgumentException();
		}

		return ((Mensagem) objeto);
	}

	public static void main(String[] args) {
		new Servidor().run();
	}
}
