package negocio.jogo;

import java.net.Socket;

public class Jogador {
    private final String nome;
    private final Socket socket;

    public Jogador(String nome, Socket socket) {
        this.socket = socket;
        this.nome = nome;
    }

    public Socket getSocket() {
        return socket;
    }

    public String getNome() {
        return nome;
    }
}
