package negocio.jogo;

import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Random;

/*
classe que representa uma rodada.
ela vai ter os dados relacionados a rodada, ou seja
- letra sorteada
- respostas e pontuacao dos participantes
*/

public class Rodada {

    private Character inicialSorteada;
    private HashMap<Socket, ArrayList<String>> respostas;

    public Rodada() {
        this.respostas = new HashMap<Socket, ArrayList<String>>();

        this.inicialSorteada = this.sortearInicial();
    }

    /**
     * Sorteia um novo caractere inicial para as categorias da rodada, exceto as letras
     * 'k', 'w' e 'y', por nao terem (muitas) palavras na lingua portuguesa com essas
     * iniciais.
     */
    private Character sortearInicial() {
        ArrayList<Character> excecoes = new ArrayList<Character>(Arrays.asList('k', 'w', 'y'));
        Random random = new Random();

        char inicial;
        while (!excecoes.contains(inicial = (char)(random.nextInt(26) + 'a')));

        return inicial;
    }
}
