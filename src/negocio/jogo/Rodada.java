package negocio.jogo;

import negocio.util.Utilitario;
import negocio.util.DicionarioBr;
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
    private final long tempoDaRodada;
    private final int pontosPorAcerto;
    private final Character inicialSorteada;
    private final ArrayList<Socket> jogadores;
    private final ArrayList<String> categorias;
    private final Runnable callbackDeFimDaRodada;

    private int contagemDeConfirmacoes;
    private boolean jogadoresRespondendo;
    private Thread threadDeContagemDeTempo;
    private final HashMap<Socket, Pair<Integer, ArrayList<String>>> respostas;

    public Rodada(int numero, long tempoDaRodada, int pontosPorAcerto, ArrayList<Socket> jogadores,
                  ArrayList<String> categorias, Runnable callbackDeFimDaRodada) {

        this.respostas = new HashMap<Socket, Pair<Integer, ArrayList<String>>>();
        this.callbackDeFimDaRodada = callbackDeFimDaRodada;
        this.pontosPorAcerto = pontosPorAcerto;
        this.tempoDaRodada = tempoDaRodada;
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
     * concentra o fluxo de acoes necessarios para executar uma rodada
     */
    public void iniciar() {
        // solicitar a resposta aos jogadores
        this.solicitarRespostas();

        // começar a contar o tempo
        this.iniciarTimer();

        // começar a escutar todos os jogadores para coletar as respostas
        this.esperarPorRespostas();
    }

    /**
     * manda mensagens para os jogadores para que comecem a responder
     */
    private void solicitarRespostas() {
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
     */
    private void iniciarTimer() {
        this.threadDeContagemDeTempo = new Thread(() -> {
            try {
                Thread.sleep(this.tempoDaRodada);
                this.interromperRespostaDosJogadores();
            } catch (InterruptedException ignored) { }
        });

        threadDeContagemDeTempo.start();
    }

    /**
     * Inicia um conjunto de threads para esperar a resposta dos jogadores.
     * Depois que o primeiro jogador terminar, todos os outros devem parar de responder.
     */
    private void esperarPorRespostas() {
        this.jogadoresRespondendo = true;

        this.jogadores.forEach(jogador -> {
            new Thread(() -> {
                try {
                    ArrayList<String> respostas = this.lerMensagem(jogador, ObjetivoMensagem.RESPOSTA_RODADA).lista;
                    int pontos = processarRespostas(respostas);

                    this.respostas.put(jogador, new Pair<Integer, ArrayList<String>>(pontos, respostas));
                } catch (IOException | ClassNotFoundException ignored) { }

                this.interromperRespostaDosJogadores(jogador);

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
        ObjectInputStream jogadorInput = new ObjectInputStream(jogador.getInputStream());
        Object objeto = jogadorInput.readObject();

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
    public synchronized void interromperRespostaDosJogadores(Socket jogador) {
        if (!this.jogadoresRespondendo)
            return;

        this.jogadoresRespondendo = false;

        this.threadDeContagemDeTempo.interrupt();

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
     * @param respostas respostas que o jogador mandou
     * @return pontuacao do jogador
     */
    public int processarRespostas(ArrayList<String> respostas) {
        return respostas.stream().mapToInt(resposta ->
                    resposta.startsWith(this.inicialSorteada.toString()) &&
                    DicionarioBr.encontraPalavraNoDicionario(resposta) ? this.pontosPorAcerto : 0)
                .sum();
    }

    /**
     * gera e envia o relatorio com a pontuacao da rodada para os participantes.
     */
    private void enviarRelatorioDaRodada() {
        if (this.respostas.size() < this.jogadores.size())
            return;

        this.contagemDeConfirmacoes = 0;

        // TODO: gerar relatorio
        String relatorio = "RELATORIO RODADA";

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
                    this.lerMensagem(jogador, ObjetivoMensagem.CONFIRMACAO_RELATORIO_RODADA);
                    this.contagemDeConfirmacoes++;

                    if (this.contagemDeConfirmacoes == this.jogadores.size()) {
                        this.callbackDeFimDaRodada.run();
                    }
                } catch (IOException | ClassNotFoundException ignored) { }
            }).start();
        });
    }
}
