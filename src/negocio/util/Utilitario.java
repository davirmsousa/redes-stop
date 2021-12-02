package negocio.util;

import negocio.util.mensagem.Mensagem;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class Utilitario {
    /**
     * Manda uma mensagem para o jogador
     * @param jogador socket do jogador que vai receber a mensagem
     * @param mensagem objeto de mensagem para ser enviado
     */
    public static void mandarMensagemParaJogador(Socket jogador, Mensagem mensagem) throws IOException {
        ObjectOutputStream jogadorOutput = new ObjectOutputStream(jogador.getOutputStream());
        jogadorOutput.writeObject(mensagem);
    }
}
