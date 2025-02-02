package searchengine.dto.responses;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SearchDataResponse {
    private String site;
    private String siteName;
    private String url;
    private String title;
    private String snippet;
    private Double relevance;
    @JsonIgnore
    private int wordsFound;

}
