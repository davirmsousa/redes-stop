package negocio.cliente;

import negocio.servidor.Servidor;

import java.io.DataInputStream;
import java.net.Socket;

/*
classe do cliente/jogador.
ela vai ser a responsavel por iniciar a conexao com o servidor e realizar as acoes dos jogadores
quando a classe Stop pedir.

deve estar preparado para quando a classe Stop pedir para inserir as respostas da rodada,
vai receber a letra da rodada e as categorias, deve responder com uma lista de strings

no final do jogo o cliente recebe um relatorio do jogo
*/

public class Cliente {

	public static void main(String[] args) {
		try {
			Socket cliente = new Socket(Servidor.HOST, Servidor.PORTA);

			DataInputStream clienteInput = new DataInputStream(cliente.getInputStream());
			boolean sucesso = clienteInput.readBoolean();

			if (!sucesso) {
				System.out.print(clienteInput.readUTF());
				return;
			}

			// continuacao da logica do cliente
			while (true);
		} catch (Exception e) {
			e.printStackTrace();
		}

	}
}
