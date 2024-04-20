package nl.tno.essim.kpi.data;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.*;

import java.util.Date;
import java.util.Map;

@Data
@Builder
@RequiredArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ModuleInfo {
    //    module = {'calculator_id': self.kpi_calculator_id,
//            'title': self.kpi_calculator.get_title(),
//            'description': self.kpi_calculator.get_description(),
//            'config_config': self.kpi_calculator.get_sample_config(),
//            'sample_output': self.kpi_calculator.get_sample_output(),
//            'kafka_consumer_group': "",
//            'kafka_client': "",
//            'status': 'registered',
//            'up_since': datetime.now()
//    }
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
