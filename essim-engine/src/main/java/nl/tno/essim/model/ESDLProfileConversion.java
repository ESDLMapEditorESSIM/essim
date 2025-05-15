package nl.tno.essim.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Getter
@Setter
@JsonPropertyOrder({"esdlContents", "startDate", "endDate", "timeStep"})
public class ESDLProfileConversion {
    @JsonProperty("esdlContents")
    private String esdlContents;
    @JsonProperty("startDate")
    @JsonFormat(
            shape = JsonFormat.Shape.STRING,
            pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX"
    )
    @JsonDeserialize(
            using = TimestampDeserializer.class
    )
    private Date startDate;

    @JsonProperty("endDate")
    @JsonFormat(
            shape = JsonFormat.Shape.STRING,
            pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX"
    )
    @JsonDeserialize(
            using = TimestampDeserializer.class
    )
    private Date endDate;

    @JsonProperty("timeStep")
    private String timeStep;

}
