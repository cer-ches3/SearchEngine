package searchengine.dto.responses;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Objects;

@Data
@AllArgsConstructor
public class SearchDataResponse {
    private String site;
    private String siteName;
    private String uri;
    private String title;
    private String snippet;
    private Double relevance;

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (object == null || getClass() != object.getClass()) return false;
        SearchDataResponse that = (SearchDataResponse) object;
        return Objects.equals(uri, that.uri);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(uri);
    }
}
