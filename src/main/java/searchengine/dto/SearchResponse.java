package searchengine.dto;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

@Data
@Accessors(chain = true)
public class SearchResponse
{
    private boolean result;
    private int count;
    private List<SearchDTO> data;
    private String error;
}
