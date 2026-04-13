package nl.tno.essim.kpi.data;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@Builder
@RequiredArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ModuleInfo {
    @JsonProperty("calculator_id")
    private String typeId;
    private String title;
    private String description;
    @JsonProperty("config_config")
    private JsonNode config;
    @JsonProperty("sample_output")
    private String sampleOutput;
    private String status;
    @JsonProperty("up_since")
    private Date upSince;
}
