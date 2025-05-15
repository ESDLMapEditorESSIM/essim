package nl.tno.essim.observation;

import lombok.Getter;
import nl.tno.essim.commons.BidFunction;

@Getter
public enum ObservationField {
    IMBALANCE_ENERGY("imbalanceEnergy", Double.class),
    IMBALANCE_POWER("imbalancePower", Double.class),
    MATCHING_PRICE("matchingPrice", Double.class),
    ALLOCATION_ENERGY("allocationEnergy", Double.class),
    ALLOCATION_POWER("allocationPower", Double.class),
    MARGINAL_COST("marginalCost", Double.class),
    FUEL_CONSUMPTION("fuelConsumption", Double.class),
    SIM_DESCRIPTION("SimDescription", String.class),
    BIDCURVE("bidcurve", BidFunction.class),
    EMISSION("emission", Double.class),
    COST("cost", Double.class),
    CAPACITY("capacity", Double.class),
    LOAD("load", Double.class),
    SOC("soc", Double.class),
    LOADFLOW_POWER("loadflowPower", Double.class),
    LOADFLOW_ENERGY("loadflowEnergy", Double.class),
    LOADFLOW_REACTIVE_POWER("loadflowReactivePower", Double.class),
    LOADFLOW_CURRENT("loadflowCurrent", Double.class),
    ;

    private final String key;
    private final Class<?> observationType;

    ObservationField(String key, Class<?> observationType) {
        this.key = key;
        this.observationType = observationType;
    }

    public static ObservationField valueOfKey(String key) {
        for (ObservationField field : values()) {
            if (field.getKey().equals(key)) {
                return field;
            }
        }
        throw new IllegalArgumentException("No ObservationField with key: " + key);
    }
}