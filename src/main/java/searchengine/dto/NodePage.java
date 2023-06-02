package searchengine.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

@Component
@Accessors(chain = true)
@Getter
@Setter
public class NodePage implements Serializable
{
    private String path;

    private String suffix;

    private String prefix;

    private int siteId;

    private int timeBetweenRequest;

    private Set<String> refOnChildSet = new HashSet<>();

}
