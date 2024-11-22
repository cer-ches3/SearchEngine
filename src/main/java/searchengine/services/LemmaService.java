package searchengine.services;

import java.io.IOException;
import java.util.Map;

public interface LemmaService {
    Map<String, Integer> getLemmasFromText(String text) throws IOException;
    void getLemmaByWord(String word, Map<String, Integer> lemmasInText);
}
