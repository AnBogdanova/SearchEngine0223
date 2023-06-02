package searchengine.dto;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class LemmaDTO
{
    private int position;
    private String inputForm;
    private String normalForm;
}
