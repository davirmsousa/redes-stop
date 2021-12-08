package negocio.util;

import java.io.FileReader;
import org.json.simple.JSONArray;
import org.json.simple.parser.JSONParser;

public class DicionarioBr {

    private static final String CAMINHO_ARQUIVO = "negocio/util/palavras.json";
    private static JSONArray palavras;

    public static boolean encontraPalavraNoDicionario(String palavra) {

        if (DicionarioBr.palavras == null) {
            try (FileReader reader = new FileReader(DicionarioBr.CAMINHO_ARQUIVO)) {
                Object obj = (new JSONParser()).parse(reader);
                DicionarioBr.palavras = (JSONArray) obj;
            } catch (Exception ignored) {
                return false;
            }
        }

        return DicionarioBr.palavras.contains(palavra);
    }
}
