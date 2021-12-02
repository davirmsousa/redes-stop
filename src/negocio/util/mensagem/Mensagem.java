package negocio.util.mensagem;

import java.io.Serializable;
import java.util.ArrayList;

/*a ideia eh concentrar as mensagens via socke nesse objeto
pq assim da pra mandar mais coisas de uma vez

nao me importei muito com a nomenclatura das propriedades
e nem com a questao do return this dos metodos]
(queria fazer um builder mas nao queria perder tempo),
me importei mais em ter como enviar as coisas
*/

public class Mensagem implements Serializable {
    public int objetivoMensagem;
    public String mensagem;
    public String conteudo;

    public ArrayList<String> lista;
    public Character character;

    public Mensagem () {
    }

    public Mensagem (ObjetivoMensagem itemEnum) {
        this.setObjetivoMensagem(itemEnum.obterValor());
        this.setMensagem(itemEnum.obterMensagem());
    }

    public Mensagem setMensagem(String mensagem) {
        this.mensagem = mensagem;
        return this;
    }

    public Mensagem setCharacter(Character character) {
        this.character = character;
        return this;
    }

    public Mensagem setLista(ArrayList<String> lista) {
        this.lista = lista;
        return this;
    }

    public Mensagem setObjetivoMensagem(int objetivoMensagem) {
        this.objetivoMensagem = objetivoMensagem;
        return this;
    }

    public Mensagem setConteudo(String conteudo) {
        this.conteudo = conteudo;
        return this;
    }
}
