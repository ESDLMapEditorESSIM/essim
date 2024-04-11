package nl.tno.essim.kpi.data;

import lombok.Data;

@Data
public class KPIModuleMessage {
    private KPIResult result;
    private KPIStatus status;
}
