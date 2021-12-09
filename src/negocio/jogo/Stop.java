package negocio.jogo;

import negocio.util.Utilitario;
import negocio.util.mensagem.Mensagem;
import negocio.util.mensagem.ObjetivoMensagem;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;

public class Stop {

	private final long MAX_TEMPO_RODADA = 15000;
	private final int PONTOS_POR_ACERTO = 5;
	private final int MAX_JOGADORES = 2;
	private final int MAX_RODADAS = 3;

	private final ArrayList<String> categorias;
	private final ArrayList<Jogador> jogadores;
	private final ArrayList<Rodada> rodadas;
	private int rodadaAtual;

	public Stop() {
		this.jogadores = new ArrayList<Jogador>();
		this.rodadas = new ArrayList<Rodada>();
		this.rodadaAtual = 1;


		this.categorias = new ArrayList<String>(Arrays.asList(
				"Nomes",
				"Marcas",
				"Frutas"
		));
	}

	/**
	 * Adiciona um novo jogador na partida e decide se o jogo deve começar.
	 * O jogador receberá a respostas informando se foi adicionado ou não na partida
	 */
	public void adicionarJogador(Jogador jogador) throws IOException {
		if (jogador == null) {
			throw new NullPointerException("jogador não pode ser nulo");
		}

		if (this.jogadores.size() == this.MAX_JOGADORES) {
			Mensagem mensagem = new Mensagem(ObjetivoMensagem.FALHA_REGISTRO);
			Utilitario.mandarMensagemParaJogador(jogador.getSocket(), mensagem);
			return;
		}

		this.jogadores.add(jogador);

		int jogadoresRestantes = this.MAX_JOGADORES - this.jogadores.size();
		String strMensagem = "Seja Bem-Vindo!" + (jogadoresRestantes > 0 ?
				" Aguardando mais " + jogadoresRestantes + " jogador(es)." :
				" O Jogo começará em instantes");
		Mensagem mensagem = new Mensagem(ObjetivoMensagem.SUCESSO_REGISTRO)
				.setMensagem(strMensagem);

		Utilitario.mandarMensagemParaJogador(jogador.getSocket(), mensagem);

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

		// callback que a Rodada chama quando a rodada termina
		Runnable callbackDeFimDaRodada = () -> {
			if (rodadaAtual >= this.MAX_RODADAS) {
				this.enviarRelatorioDaPartida();
				return;
			}

			try {
				Thread.sleep(5000);
			} catch (InterruptedException ignored) {
				ignored.printStackTrace();
			}

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
		StringBuilder relatorio = new StringBuilder("RELATORIO FINAL\n");
		for (Jogador jogador : this.jogadores) {
			int pontosTotais = this.rodadas.stream()
					.mapToInt(rodada -> rodada.obterPontosDoJogador(jogador))
					.sum();

			relatorio.append(jogador.getNome())
					.append(": ")
					.append(pontosTotais)
					.append(" pts;\n");
		}

		this.jogadores.forEach(jogador -> {
			try {
				Mensagem mensagem = new Mensagem(ObjetivoMensagem.RELATORIO_PARTIDA)
						.setConteudo(relatorio.toString());
				Utilitario.mandarMensagemParaJogador(jogador.getSocket(), mensagem);
			} catch (IOException ignored) {
				ignored.printStackTrace();
			}
		});
	}
}