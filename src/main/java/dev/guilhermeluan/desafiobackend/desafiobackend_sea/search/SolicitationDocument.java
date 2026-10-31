package dev.guilhermeluan.desafiobackend.desafiobackend_sea.search;

import dev.guilhermeluan.desafiobackend.desafiobackend_sea.solicitation.Priority;
import dev.guilhermeluan.desafiobackend.desafiobackend_sea.solicitation.ServiceType;
import dev.guilhermeluan.desafiobackend.desafiobackend_sea.solicitation.Solicitation;
import dev.guilhermeluan.desafiobackend.desafiobackend_sea.solicitation.Status;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
@Document(indexName = "solicitations")
public class SolicitationDocument {

    @Id
    private Long id;

    @Field(type = FieldType.Text)
    private String title;

    @Field(type = FieldType.Text)
    private String description;

    @Field(type = FieldType.Keyword)
    private Status status;

    @Field(type = FieldType.Keyword)
    private ServiceType serviceType;

    @Field(type = FieldType.Keyword)
    private Priority priority;

    @Field(type = FieldType.Date, format = DateFormat.date_time)
    private Instant createdAt;

    @Field(type = FieldType.Date, format = DateFormat.date_time)
    private Instant submittedAt;

    @Field(type = FieldType.Scaled_Float, scalingFactor = 100)
    private BigDecimal estimatedValue;

    @Field(type = FieldType.Keyword)
    private String city;

    @Field(type = FieldType.Long)
    private Long ownerId;

    @Field(type = FieldType.Long)
    private Long decidedBy;

    @Field(type = FieldType.Keyword)
    private String state;

    public static SolicitationDocument from(Solicitation solicitation) {
        SolicitationDocument document = new SolicitationDocument();
        document.setId(solicitation.getId());
        document.setTitle(solicitation.getTitle());
        document.setDescription(solicitation.getDescription());
        document.setStatus(solicitation.getStatus());
        document.setServiceType(solicitation.getServiceType());
        document.setPriority(solicitation.getPriority());
        document.setCreatedAt(solicitation.getCreatedAt());
        document.setSubmittedAt(solicitation.getSubmittedAt());
        document.setEstimatedValue(solicitation.getEstimatedValue());
        document.setCity(solicitation.getCity());
        document.setOwnerId(solicitation.getOwnerId());
        document.setDecidedBy(solicitation.getDecidedBy());
        document.setState(solicitation.getState());
        return document;
    }
}
