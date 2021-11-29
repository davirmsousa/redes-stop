package negocio.jogo;

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

	private final int MAX_JOGADORES = 5;
	private final int MAX_RODADAS = 5;

	private ArrayList<String> categorias;
	private ArrayList<Socket> jogadores;
	private ArrayList<Rodada> rodadas;

	public Stop() {
		jogadores = new ArrayList<Socket>();
		rodadas = new ArrayList<Rodada>();
	}

	/**
	 * Adiciona um novo jogador na partida e decide se o jogo deve começar.
	 * O jogador receberá duas respostas (boolean, string) informando
	 * se foi adicionado ou não na partida
	 */
	public void adicionarJogador(Socket jogador) throws IOException {
		if (jogador == null) {
			throw new NullPointerException("jogador não pode ser nulo");
		}

		if (this.jogadores.size() > this.MAX_JOGADORES) {
			String mensagem = "O jogo já começou, espere a próxima partida.";
			this.mandarMensagemParaJogador(jogador, mensagem, false);
			return;
		}

		this.jogadores.add(jogador);

		int jogadoresRestantes = this.MAX_JOGADORES - this.jogadores.size();
		String mensagem = "Seja Bem-Vindo!" + (jogadoresRestantes > 0 ?
				" Aguardando mais " + jogadoresRestantes + " jogador(es)." :
				" O Jogo começará em instantes");
		this.mandarMensagemParaJogador(jogador, mensagem, true);

		this.iniciarPartida();
	}

	/**
	 * Inicia a partida após validar se ja chegou ao numero necessario de jogadores.
	 * Responsável por iniciar e gerenciar as rodadas
	 */
	private void iniciarPartida() {
		if (this.jogadores.size() != this.MAX_JOGADORES) {
			return;
		}

		for (int rodada = 0; rodada < this.MAX_RODADAS; rodada++) {
			this.iniciarRodada();
		}
	}

	/**
	 * Inicia e gerencia uma rodada ate o seu fim.
	 */
	private void iniciarRodada() {

	}

	/**
	 * Manda duas mensagens para o jogador, a mensagem em si e um booleano para indicar o teor da mensagem
	 * (sucesso ou neutro = true; erro ou negacao = false).
	 */
	private void mandarMensagemParaJogador(Socket jogador, String mensagem, boolean sucesso) throws IOException {
		DataOutputStream jogadorOutput = new DataOutputStream(jogador.getOutputStream());
		jogadorOutput.writeBoolean(sucesso);
		jogadorOutput.writeUTF(mensagem);
	}
}