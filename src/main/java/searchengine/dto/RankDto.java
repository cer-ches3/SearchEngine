package searchengine.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import searchengine.model.PageModel;

@Data
@NoArgsConstructor
public class RankDto {
    private Integer pageId;
    private PageModel pageModel;
    private double absRelevance = 0.0;
    private double relativeRelevance = 0.0;
    private int maxLemmaRank = 0;
}
