### Cable and Transformer parameters used by the load‑flow integration

This note summarizes which parameters from `CableSpec` and `TransformerSpec` are actually consumed by the software when building the Powsybl network in `EssimLoadFlowController`.

Sources (paths relative to repo root):
- `essim-engine/src/main/java/nl/tno/essim/loadflow/EssimLoadFlowController.java`
- `essim-engine/src/main/java/nl/tno/essim/loadflow/CableSpec.java`
- `essim-engine/src/main/java/nl/tno/essim/loadflow/TransformerSpec.java`

#### Cables (`CableSpec`)
Fields in `CableSpec`:
- `name`, `rOhmPerKm`, `xOhmPerKm`, `cNfPerKm`, `maxIkA`, `type`, `qMm2`, `alpha`

Used parameters (where/how):
- `rOhmPerKm`: Series resistance of the line
  - Used to set Powsybl line `R`: `R = length_m * rOhmPerKm / (1000 * parallelLines)`
  - Code: `EssimLoadFlowController#createLines` lines ~266–280
- `xOhmPerKm`: Series reactance of the line
  - Used to set Powsybl line `X`: `X = length_m * xOhmPerKm / (1000 * parallelLines)`
  - Code: same block as above
- `cNfPerKm`: Line capacitance per km (nano‑farad)
  - Used to compute shunt susceptance `B1` and `B2` (assuming 50 Hz):
    - `B = (length_m * cNfPerKm * parallelLines * 1e-9 * 2π * 50) / 2`
    - Set as both ends: `line.setB1(B)` and `line.setB2(B)`
  - Code: `EssimLoadFlowController#createLines` line ~269–281
- `maxIkA`: Permanent current limit per circuit (kA)
  - Used to set operational current limit: `permanentLimit = maxIkA * 1000 * parallelLines`
  - Code: `EssimLoadFlowController#createLines` lines ~284–288

Parameters currently NOT used (and potential Powsybl mapping):
- `name`: Metadata only. Could be set via `line.setName(name)`.
- `type` (e.g., `cs`/`ol`): Not consulted. Could be used to:
  - Select different default shunt conductance `G` (dielectric losses).
  - Apply different thermal rating models (e.g., overhead line vs. underground cable).
- `qMm2` (cross‑section, mm²): Not used. Could be used to:
  - Derive/validate series resistance `R` or permanent current limits `Imax`.
- `alpha` (temp. coefficient, 1/°C): Not used. Could be used for temperature‑corrected resistance: `R(T) = R20 * (1 + alpha * (T - 20))`.

Additional inputs affecting cables (from ESDL assets):
- Cable length (meters): `((ElectricityCable) cable).getLength()`
- Parallel circuits: `parallelLines = max(cable.getAggregationCount(), 1)`
- Frequency is assumed 50 Hz (hardcoded) for susceptance calculation.
- Shunt conductance `G` is not set (left at default; see TODO in code).

Defaults / error handling:
- If the cable `assetType` is unknown or missing, a default type is used with a warning: `"243-AL1/39-ST1A 20.0"`.
  - Code: `EssimLoadFlowController#createLines` lines ~259–265

#### Transformers (`TransformerSpec`)
Fields in `TransformerSpec`:
- `name`, `snMva`, `vnHvKv`, `vnLvKv`, `vkPercent`, `vkrPercent`, `pfeKw`, `i0Percent`, `shiftDegree`, `tapSide`, `tapNeutral`, `tapMin`, `tapMax`, `tapStepPercent`, `tapStepDegree`, `tapPhaseShifter`

Used parameters (where/how):
- `snMva` (rated power, MVA), `vnHvKv` (kV), `vnLvKv` (kV):
  - Base impedance on LV side: `Zb_tr = (vnLvKv^2) / snMva`
  - Rated voltages set on the created transformer: `.setRatedU2(vnHvKv)`, `.setRatedU1(vnLvKv)`
- `vkPercent` (% short‑circuit voltage) and `vkrPercent` (% resistive part):
  - Per‑unit values: `zk = vkPercent/100`, `rk = vkrPercent/100`, `xk = sqrt(zk^2 − rk^2)`
  - Converted to ohmic: `R = rk * Zb_tr`, `X = xk * Zb_tr`
- `pfeKw` (core loss at rated voltage, kW) and `i0Percent` (% no‑load current):
  - Per‑unit: `ym = i0Percent/100`, `gm = pfeKw / (snMva * 1000)`
  - Susceptive part: `bm = −sign(ym) * sqrt(ym^2 − gm^2)`
  - Converted to siemens on LV base: `G = gm / Zb_tr`, `B = bm / Zb_tr`
- Code: `EssimLoadFlowController#createTransformers` lines ~207–233

Parameters currently NOT used (and potential Powsybl mapping):
- `shiftDegree`: Fixed phase shift (vector group). Could be mapped to the transformer's phase angle clock/shift.
- `tapSide`, `tapNeutral`, `tapMin`, `tapMax`, `tapStepPercent`, `tapStepDegree`, `tapPhaseShifter`:
  - Currently ignored. These could be used to create Powsybl `RatioTapChanger` (for voltage regulation) or `PhaseTapChanger` (for active power flow control) on the specified `tapSide`.
- `name`: Metadata only. Could be set via `transformer.setName(name)`.

Notes:
- Two‑winding transformer is created with `R`, `X`, `G`, `B` computed on the LV base; rated U1/U2 set as above.
- Ports are assigned to LV/HV sides by comparing carrier voltages of the connected ports; U1 is the LV side, U2 the HV side in code.
- The transformer asset is associated with the low‑voltage carrier for observations.

Defaults / error handling:
- If transformer `assetType` is missing/empty, a default is assumed with a warning: `"160 MVA 380/110 kV"`.
- If the provided transformer name has no corresponding spec entry, an `IllegalStateException` is thrown (no silent defaulting).
  - Code: `EssimLoadFlowController#createTransformers` lines ~180–189

#### Summary
- Cable parameters used: `rOhmPerKm`, `xOhmPerKm`, `cNfPerKm`, `maxIkA`.
  - Unused fields (`type`, `qMm2`, `alpha`) could enable more advanced physical modeling like temperature correction or automated `G` calculation.
- Transformer parameters used: `snMva`, `vnHvKv`, `vnLvKv`, `vkPercent`, `vkrPercent`, `pfeKw`, `i0Percent`.
  - Tap/phase‑shifting related fields are currently ignored but map directly to Powsybl `RatioTapChanger` and `PhaseTapChanger` capabilities for automated network regulation.
- Assumptions: 50 Hz network frequency for cable shunt susceptance; transformer magnetization and short‑circuit parameters are interpreted on the LV base.
