package searchengine.dto.responses;

import lombok.*;

@AllArgsConstructor
@Getter
@Setter
@ToString
public class ErrorResponse {
    private final Boolean result = false;
    private String error;
}
