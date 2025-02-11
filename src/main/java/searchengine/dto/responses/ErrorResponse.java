package searchengine.dto.responses;

import lombok.*;

@AllArgsConstructor
@Getter
@Setter
public class ErrorResponse {
    private final Boolean result = false;
    private String error;
}
