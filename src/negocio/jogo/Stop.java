package negocio.jogo;

import negocio.util.Utilitario;
import negocio.util.mensagem.Mensagem;
import negocio.util.mensagem.ObjetivoMensagem;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;

/*
classe para gerenciar o jogo.
vai ter as regras e fluxos gerais do jogo como
- adicionar jogadores e decidir quando o jogo inicia
- iniciar/validar/finalizar uma rodada
	- ao iniciar, criar uma nova rodada e permitir que os jogadores respondam
	- apos terminarem de responder, validar as iniciais das palavras
	  (acho que nao vai dar pra deixar que os jogadores validem entre eles)
	- ao finalizar, mandar um relatorio para os jogadores informando a pontuação geral
*/

public class Stop {

	private final long MAX_TEMPO_RODADA = 5000;
	private final int MAX_JOGADORES = 1;
	private final int MAX_RODADAS = 2;

	private ArrayList<String> categorias;
	private ArrayList<Socket> jogadores;
	private ArrayList<Rodada> rodadas;
	private int rodadaAtual;

	public Stop() {
		this.jogadores = new ArrayList<Socket>();
		this.rodadas = new ArrayList<Rodada>();
		this.rodadaAtual = 1;
	}

	/**
	 * Adiciona um novo jogador na partida e decide se o jogo deve começar.
	 * O jogador receberá a respostas informando se foi adicionado ou não na partida
	 */
	public void adicionarJogador(Socket jogador) throws IOException {
		if (jogador == null) {
			throw new NullPointerException("jogador não pode ser nulo");
		}

		if (this.jogadores.size() == this.MAX_JOGADORES) {
			Mensagem mensagem = new Mensagem(ObjetivoMensagem.FALHA_REGISTRO);
			Utilitario.mandarMensagemParaJogador(jogador, mensagem);
			return;
		}

		this.jogadores.add(jogador);

		int jogadoresRestantes = this.MAX_JOGADORES - this.jogadores.size();
		String strMensagem = "Seja Bem-Vindo!" + (jogadoresRestantes > 0 ?
				" Aguardando mais " + jogadoresRestantes + " jogador(es)." :
				" O Jogo começará em instantes");
		Mensagem mensagem = new Mensagem(ObjetivoMensagem.SUCESSO_REGISTRO)
				.setMensagem(strMensagem);

		Utilitario.mandarMensagemParaJogador(jogador, mensagem);

		new Thread(this::iniciarPartida).start();
	}

	/**
	 * Inicia a partida após validar se ja chegou ao numero necessario de jogadores.
	 */
	private synchronized void iniciarPartida() {
		if (this.jogadores.size() != this.MAX_JOGADORES) {
			return;
		}

		this.iniciarRodada(this.rodadaAtual);
	}

	/**
	 * Inicia a rodada. como o processo eh assincrono, o termino do metodo
	 * nao significa o termino da rodada
	 */
	private void iniciarRodada(int numero) {
		System.out.println("rodada " + numero + " iniciada");

		Rodada rodada = new Rodada(numero, this.jogadores, this.categorias);
		this.rodadas.add(rodada);

		// solicitar a resposta aos jogadores
		rodada.solicitarRespostas();

		// começar a contar o tempo
		rodada.iniciarTimer(this.MAX_TEMPO_RODADA);

		// começar a escutar todos os jogadores para coletar as respostas
		rodada.esperarPorRespostas();
	}
}