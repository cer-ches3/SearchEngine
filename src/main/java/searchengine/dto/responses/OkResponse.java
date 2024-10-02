package searchengine.dto.responses;

import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public class OkResponse {
    private final Boolean result = true;
}
