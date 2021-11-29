package negocio.servidor;

import negocio.jogo.Stop;

import java.net.ServerSocket;
import java.net.Socket;

public class Servidor {

	public static final int PORTA = 1234;
	public static final String HOST = "";

	private static Stop stop;

	public static void main(String[] args) {
		Servidor.stop = new Stop();

		try {
			ServerSocket servidor = new ServerSocket(Servidor.PORTA);
			System.out.println("Servidor ativo");

			while (true) {
				Socket jogador = servidor.accept();
				Servidor.stop.adicionarJogador(jogador);
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
