package searchengine.dto;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class IndexResponse
{
    private boolean result;

    private String error;
}
