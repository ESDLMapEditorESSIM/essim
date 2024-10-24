package nl.tno.essim.loadflow;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
class TransformerSpec {
    private final static Map<String, TransformerSpec> transformerSpecs = new HashMap<>();

    static {
        transformerSpecs.put("160 MVA 380/110 kV", new TransformerSpec("160 MVA 380/110 kV", 160.0, 380.0, 110.0, 12.2, 0.25, 60.0, 0.06, 0, "hv", 0, -9, 9, 1.5, 0, false));
        transformerSpecs.put("100 MVA 220/110 kV", new TransformerSpec("100 MVA 220/110 kV", 100.0, 220.0, 110.0, 12.0, 0.26, 55.0, 0.06, 0, "hv", 0, -9, 9, 1.5, 0, false));
        transformerSpecs.put("63 MVA 110/20 kV", new TransformerSpec("63 MVA 110/20 kV", 63.0, 110.0, 20.0, 18.0, 0.32, 22.0, 0.04, 150, "hv", 0, -9, 9, 1.5, 0, false));
        transformerSpecs.put("40 MVA 110/20 kV", new TransformerSpec("40 MVA 110/20 kV", 40.0, 110.0, 20.0, 16.2, 0.34, 18.0, 0.05, 150, "hv", 0, -9, 9, 1.5, 0, false));
        transformerSpecs.put("25 MVA 110/20 kV", new TransformerSpec("25 MVA 110/20 kV", 25.0, 110.0, 20.0, 12.0, 0.41, 14.0, 0.07, 150, "hv", 0, -9, 9, 1.5, 0, false));
        transformerSpecs.put("63 MVA 110/10 kV", new TransformerSpec("63 MVA 110/10 kV", 63.0, 110.0, 10.0, 18.0, 0.32, 22.0, 0.04, 150, "hv", 0, -9, 9, 1.5, 0, false));
        transformerSpecs.put("40 MVA 110/10 kV", new TransformerSpec("40 MVA 110/10 kV", 40.0, 110.0, 10.0, 16.2, 0.34, 18.0, 0.05, 150, "hv", 0, -9, 9, 1.5, 0, false));
        transformerSpecs.put("25 MVA 110/10 kV", new TransformerSpec("25 MVA 110/10 kV", 25.0, 110.0, 10.0, 12.0, 0.41, 14.0, 0.07, 150, "hv", 0, -9, 9, 1.5, 0, false));
        transformerSpecs.put("0.25 MVA 20/0.4 kV", new TransformerSpec("0.25 MVA 20/0.4 kV", 0.25, 20.0, 0.4, 6.0, 1.44, 0.8, 0.32, 150, "hv", 0, -2, 2, 2.5, 0, false));
        transformerSpecs.put("0.4 MVA 20/0.4 kV", new TransformerSpec("0.4 MVA 20/0.4 kV", 0.4, 20.0, 0.4, 6.0, 1.425, 1.35, 0.3375, 150, "hv", 0, -2, 2, 2.5, 0, false));
        transformerSpecs.put("0.63 MVA 20/0.4 kV", new TransformerSpec("0.63 MVA 20/0.4 kV", 0.63, 20.0, 0.4, 6.0, 1.206, 1.65, 0.2619, 150, "hv", 0, -2, 2, 2.5, 0, false));
        transformerSpecs.put("0.25 MVA 10/0.4 kV", new TransformerSpec("0.25 MVA 10/0.4 kV", 0.25, 10.0, 0.4, 4.0, 1.2, 0.6, 0.24, 150, "hv", 0, -2, 2, 2.5, 0, false));
        transformerSpecs.put("0.4 MVA 10/0.4 kV", new TransformerSpec("0.4 MVA 10/0.4 kV", 0.4, 10.0, 0.4, 4.0, 1.325, 0.95, 0.2375, 150, "hv", 0, -2, 2, 2.5, 0, false));
        transformerSpecs.put("0.63 MVA 10/0.4 kV", new TransformerSpec("0.63 MVA 10/0.4 kV", 0.63, 10.0, 0.4, 4.0, 1.0794, 1.18, 0.1873, 150, "hv", 0, -2, 2, 2.5, 0, false));
    }

    private String name;
    private double snMva;
    private double vnHvKv;
    private double vnLvKv;
    private double vkPercent;
    private double vkrPercent;
    private double pfeKw;
    private double i0Percent;
    private int shiftDegree;
    private String tapSide;
    private int tapNeutral;
    private int tapMin;
    private int tapMax;
    private double tapStepPercent;
    private int tapStepDegree;
    private boolean tapPhaseShifter;

    public static TransformerSpec getTransformerSpec(String name) {
        return transformerSpecs.get(name);
    }
}

