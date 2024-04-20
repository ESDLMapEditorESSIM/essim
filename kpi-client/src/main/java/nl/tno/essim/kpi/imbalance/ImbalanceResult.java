package nl.tno.essim.kpi.imbalance;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;


public class ImbalanceResult {
    public static class Kpi {
        @JsonProperty("Values")
        public Object values;
        @JsonProperty("Unit")
        public String unit;
        @JsonProperty("Name")
        public String name;
    }

    public static class TotalEnergyImbalanceID {
        @JsonProperty("Description")
        public String description;
        public ArrayList<Kpi> kpi;
        public String scope;
        public String id;
        public String status;
    }

    @JsonProperty("TotalEnergyImbalanceID")
    public TotalEnergyImbalanceID totalEnergyImbalanceID;
}

