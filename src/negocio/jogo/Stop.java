package negocio.jogo;

import negocio.util.Utilitario;
import negocio.util.mensagem.Mensagem;
import negocio.util.mensagem.ObjetivoMensagem;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;

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
	private final int PONTOS_POR_ACERTO = 5;
	private final int MAX_JOGADORES = 1;
	private final int MAX_RODADAS = 2;

	private ArrayList<String> categorias;
	private ArrayList<Socket> jogadores;
	private ArrayList<Rodada> rodadas;
	private int rodadaAtual;

	public Stop() {
		this.categorias = new ArrayList<String>(Arrays.asList("cat1", "cart2", "cart3", "cart4"));
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

		// callback que a Rodada chama quando a rodada termina
		Runnable callbackDeFimDaRodada = () -> {
			// verificar se chegou na ultima rodada
			if (rodadaAtual > this.MAX_RODADAS) {
				this.enviarRelatorioDaPartida();
				return;
			}

			System.out.println("iniciando proxima rodada");

			// dormir e iniciar a proxima rodada
			try {
				Thread.sleep(5000);
			} catch (InterruptedException ignored) { }

			this.iniciarRodada(++rodadaAtual);
		};

		Rodada rodada = new Rodada(numero, this.MAX_TEMPO_RODADA, this.PONTOS_POR_ACERTO, this.jogadores,
				this.categorias, callbackDeFimDaRodada);

		this.rodadas.add(rodada);

		rodada.iniciar();
	}

	/**
	 * gerar o relatorio de fim da partida
	 */
	private void enviarRelatorioDaPartida() {
		String relatorio = "RELATORIO FINAL";

		// mandar o relatorio para todos os jogadores
		this.jogadores.forEach(jogador -> {
			try {
				Mensagem mensagem = new Mensagem(ObjetivoMensagem.RELATORIO_PARTIDA)
						.setConteudo(relatorio);
				Utilitario.mandarMensagemParaJogador(jogador, mensagem);
				System.out.println("1+ relatorio final enviado");
			} catch (IOException ignored) { }
		});
	}
}