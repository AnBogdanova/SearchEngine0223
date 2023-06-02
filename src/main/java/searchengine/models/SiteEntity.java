package searchengine.models;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

import javax.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Getter
@Setter
@Accessors(chain = true)
@RequiredArgsConstructor
@Table(name = "\"site\"")
public class SiteEntity
{
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "\"id\"", nullable = false)
    private Integer id;

    @Column(columnDefinition = "ENUM")
    @Enumerated(EnumType.STRING)
    private StatusType status;

    @Column(name = "\"status_time\"",nullable = false)
    private LocalDateTime statusTime;

    @Column(columnDefinition = "TEXT")
    private String lastError;

    @Column(name = "\"url\"", nullable = false)
    private String url;

    private String name;

    public SiteEntity(Integer id, StatusType status, LocalDateTime statusTime, String lastError, String url, String name) {
        this.id = id;
        this.status = status;
        this.statusTime = statusTime;
        this.lastError = lastError;
        this.url = url;
        this.name = name;
    }



}
