package negocio.util.mensagem;

import java.util.NoSuchElementException;

public enum ObjetivoMensagem {
    SUCESSO_REGISTRO(1, "Seja Bem-Vindo!"),
    FALHA_REGISTRO(2, "O jogo já começou, espere a próxima partida."),

    RESPONDER_RODADA(3, "A rodada foi iniciada!"),
    PARAR_RESPOSTA_RODADA(4, "O tempo acabou, os jogadores foram interrompidos."),

    RESPOSTA_RODADA(5, "Cliente terminou de responder os itens da rodada."),

    RELATORIO_RODADA(5, "A rodada terminou, este é o resultado geral."),
    CONFIRMACAO_RELATORIO_RODADA(5, "O jogador está pronto para a próxima rodada.");

    private final int valor;
    private final String mensagem;

    ObjetivoMensagem (int valor, String mensagem) {
        this.mensagem = mensagem;
        this.valor = valor;
    }

    public int obterValor() {
        return this.valor;
    }

    public String obterMensagem() {
        return this.mensagem;
    }

    public static ObjetivoMensagem intParaEnum(int valor) {
        for (ObjetivoMensagem item : ObjetivoMensagem.values()) {
            if (item.obterValor() == valor)
                return item;
        }

        throw new NoSuchElementException("valor " + valor + " não corresponde a um item válido");
    }
}
