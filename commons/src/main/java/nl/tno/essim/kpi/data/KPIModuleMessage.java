package nl.tno.essim.kpi.data;

import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class KPIModuleMessage {
    private KPIResult result;
    private KPIStatus status;
}
