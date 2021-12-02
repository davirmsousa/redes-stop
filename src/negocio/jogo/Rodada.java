package negocio.jogo;

import negocio.util.Utilitario;
import negocio.util.mensagem.Mensagem;
import negocio.util.mensagem.ObjetivoMensagem;
import org.javatuples.Pair;

import java.io.IOException;
import java.io.ObjectInputStream;
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
    private final int numero;
    private final Character inicialSorteada;
    private final ArrayList<Socket> jogadores;
    private final ArrayList<String> categorias;

    private boolean jogadoresRespondendo;
    private Thread threadDeContagemDeTempo;
    private HashMap<Socket, Pair<Integer, ArrayList<String>>> respostas;

    private final int PONTOS_POR_ACERTO = 5;

    public Rodada(int numero, ArrayList<Socket> jogadores, ArrayList<String> categorias) {
        this.respostas = new HashMap<Socket, Pair<Integer, ArrayList<String>>>();
        this.jogadoresRespondendo = false;
        this.categorias = categorias;
        this.jogadores = jogadores;
        this.numero = numero;

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
        while (excecoes.contains(inicial = (char)(random.nextInt(26) + 'a')));

        return inicial;
    }

    /**
     * get da propriedade inicialSorteada
     * @return valor de inicialSorteada
     */
    public Character obterInicialSorteada() {
        return this.inicialSorteada;
    }

    /**
     * manda mensagens para os jogadores para que comecem a responder
     */
    public void solicitarRespostas() {
        System.out.println("solicitando respostas");

        this.jogadores.forEach(jogador -> {
            try {

                Mensagem mensagem = new Mensagem(ObjetivoMensagem.RESPONDER_RODADA)
                        .setCharacter(this.inicialSorteada)
                        .setLista(this.categorias);

                Utilitario.mandarMensagemParaJogador(jogador, mensagem);
            } catch (IOException ignored) { }
        });
    }

    /**
     * inicia a contagem do tempo que os jogadores tem para responder
     * @param tempoDeResposta tempo maximo em milisegundos para responder
     */
    public void iniciarTimer(long tempoDeResposta) {
        System.out.println("contando o tempo");

        this.threadDeContagemDeTempo = new Thread(() -> {
            try {
                // dormir pelo tempo permitido de resposta, quando acordar significa que o tempo acabou
                // e que ninguem terminou, pq se nao a thread teria sido encerrada
                Thread.sleep(tempoDeResposta);

                System.out.println("tempo acabou");

                // caso o tempo acabe, interromper todos os jogadores
                this.interromperRespostaDosJogadores();
            } catch (InterruptedException ignored) { }
        });

        threadDeContagemDeTempo.start();
    }

    /**
     * Inicia um conjunto de threads para esperar a resposta dos jogadores.
     * Depois que o primeiro jogador terminar, todos os outros devem parar de responder.
     */
    public void esperarPorRespostas() {
        System.out.println("esperando respostas");

        this.jogadoresRespondendo = true;

        this.jogadores.forEach(jogador -> {
            new Thread(() -> {
                // ler a lista de respostas que o jogador vai mandar e processar
                try {
                    ArrayList<String> respostas = this.lerMensagem(jogador, ObjetivoMensagem.RESPOSTA_RODADA).lista;
                    int pontos = processarRespostas(respostas);
                    System.out.println("1+ resposta recebida");

                    this.respostas.put(jogador, new Pair<Integer, ArrayList<String>>(pontos, respostas));
                } catch (IOException | ClassNotFoundException ignored) { }

                // o primeiro que mandar as respostas deve ser o gatilho para parar os outros
                this.interromperRespostaDosJogadores(jogador);

                // enviar o relatorio com a pontuacao para os jogadores
                this.enviarRelatorioDaRodada();
            }).start();
        });
    }

    /**
     * espera a resposta do jogador
     * @param jogador socket do jogador
     * @return Mensagem do jogador
     */
    @SuppressWarnings (value="unchecked")
    private Mensagem lerMensagem(Socket jogador, ObjetivoMensagem objetivo) throws IOException, ClassNotFoundException {
        // pegar o objeto que o cliente mandou
        ObjectInputStream jogadorInput = new ObjectInputStream(jogador.getInputStream());
        Object objeto = jogadorInput.readObject();

        // validar o tipo e objetivo
        if (!(objeto instanceof Mensagem) || (objetivo != null && ((Mensagem) objeto).objetivoMensagem != objetivo.obterValor())) {
            throw new IllegalArgumentException();
        }

        return ((Mensagem) objeto);
    }

    /**
     * envia uma mensagem para todos os jogadores para que eles parem de responder os itens da rodada.
     */
    public void interromperRespostaDosJogadores() {
        this.interromperRespostaDosJogadores(null);
    }

    /**
     * envia uma mensagem para os jogadores (exceto o que foi passado via parametro)
     * para que eles parem de responder os itens da rodada.
     * @param jogador socket do jogador que nao vai receber a mensagem de interrupcao
     */
    public void interromperRespostaDosJogadores(Socket jogador) {
        if (!this.jogadoresRespondendo)
            return;

        System.out.println("interrompendo jogadores");
        this.jogadoresRespondendo = false;

        this.threadDeContagemDeTempo.interrupt();

        // mandar mensagem para todos os jogadores para que parem de responder
        this.jogadores.forEach(jogadorRespondendo -> {
            if (jogador == null || jogadorRespondendo != jogador) {
                try {
                    Mensagem mensagem = new Mensagem(ObjetivoMensagem.PARAR_RESPOSTA_RODADA);
                    Utilitario.mandarMensagemParaJogador(jogadorRespondendo, mensagem);
                } catch (IOException ignored) { }
            }
        });
    }

    /**
     * valida as respostas do jogador.
     * (ate o momento so vamos validar a inicial)
     * @param respostas respostas que o jogador mandou
     * @return pontuacao do jogador
     */
    public int processarRespostas(ArrayList<String> respostas) {
        return respostas.stream().mapToInt(resposta ->
                resposta.startsWith(this.inicialSorteada.toString()) ? PONTOS_POR_ACERTO : 0)
                .sum();
    }

    /**
     * gera e envia o relatorio com a pontuacao da rodada para os participantes.
     */
    private void enviarRelatorioDaRodada() {
        if (this.respostas.size() < this.jogadores.size())
            return;

        // TODO: gerar relatorio
        String relatorio = "";

        // mandar o relatorio para todos os jogadores
        this.jogadores.forEach(jogador -> {
            try {
                Mensagem mensagem = new Mensagem(ObjetivoMensagem.RELATORIO_RODADA)
                        .setConteudo(relatorio);
                Utilitario.mandarMensagemParaJogador(jogador, mensagem);
            } catch (IOException ignored) { }
        });

        // esperar a confirmacao de todos os jogadores para poder iniciar a prox rodada
        this.jogadores.forEach(jogador -> {
            new Thread(() -> {
                try {
                    Mensagem mensagem = this.lerMensagem(jogador, ObjetivoMensagem.CONFIRMACAO_RELATORIO_RODADA);

                    // a ideia aqui era fazer a contagem de confirmacoes para poder iniciar a prox rodada,
                    // mas quem inicia a prox rodada eh a Stop kkkkk
                } catch (IOException | ClassNotFoundException ignored) { }
            }).start();
        });
    }
}
