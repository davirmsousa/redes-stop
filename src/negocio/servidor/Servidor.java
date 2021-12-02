package negocio.servidor;

import negocio.jogo.Stop;

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
				Socket jogador = servidor.accept();
				stop.adicionarJogador(jogador);
			}

		} catch (Exception e) {
			System.out.println("Servidor quebrou");
		}
	}

	public static void main(String[] args) {
		new Servidor().run();
	}
}
