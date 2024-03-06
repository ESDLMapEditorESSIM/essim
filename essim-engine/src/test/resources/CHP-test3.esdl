<?xml version='1.0' encoding='UTF-8'?>
<esdl:EnergySystem xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:esdl="http://www.tno.nl/esdl" version="4" id="2f1f63c7-892b-440e-b48a-d806f5fe85eb" description="" esdlVersion="v2311" name="Untitled EnergySystem">
  <energySystemInformation xsi:type="esdl:EnergySystemInformation" id="88753e8c-a5a3-4376-b142-404c2880c1db">
    <quantityAndUnits xsi:type="esdl:QuantityAndUnits" id="e54777bd-afc6-46ea-b366-398e71c40b1f">
      <quantityAndUnit xsi:type="esdl:QuantityAndUnitType" multiplier="GIGA" physicalQuantity="ENERGY" description="Energy in GJ" id="eb07bccb-203f-407e-af98-e687656a221d" unit="JOULE"/>
      <quantityAndUnit xsi:type="esdl:QuantityAndUnitType" multiplier="KILO" physicalQuantity="ENERGY" description="Energy in kWh" id="12c481c0-f81e-49b6-9767-90457684d24a" unit="WATTHOUR"/>
    </quantityAndUnits>
    <carriers xsi:type="esdl:Carriers" id="bf9d946c-7028-4610-9153-9d7dceab9164">
      <carrier xsi:type="esdl:GasCommodity" id="9d13b486-dba4-4ab6-bce1-80a388b32119" name="gas"/>
      <carrier xsi:type="esdl:ElectricityCommodity" id="d9b6ad9b-0fba-40c2-91fe-7f063d9e7b6d" name="elektriciteit"/>
      <carrier xsi:type="esdl:HeatCommodity" id="130ee23f-d41a-4f86-a7ae-0bc6dbc003ce" name="warmte"/>
    </carriers>
  </energySystemInformation>
  <instance xsi:type="esdl:Instance" id="810b4bf5-4434-4825-96fc-5eb4be231fff" name="Untitled Instance">
    <area xsi:type="esdl:Area" name="Untitled Area" id="bb635dbc-e569-4428-80d8-0038e2124d0b">
      <asset xsi:type="esdl:CHP" power="20000.0" heatEfficiency="0.4" controlStrategy="b48bb602-bcaa-43e5-970c-42e8cce7a4f8" name="CHP_620e" electricalEfficiency="0.5" efficiency="1.0" id="620efa79-b989-4fc5-8cd0-26d4e4fc6eb8">
        <geometry xsi:type="esdl:Point" lat="51.797908602848196" CRS="WGS84" lon="4.415173670433701"/>
        <port xsi:type="esdl:InPort" carrier="9d13b486-dba4-4ab6-bce1-80a388b32119" name="In" connectedTo="3508210c-15b7-4b72-bd81-48948a6a83f9" id="9406985d-ebba-4792-a0d6-73f9f1e60439"/>
        <port xsi:type="esdl:OutPort" carrier="130ee23f-d41a-4f86-a7ae-0bc6dbc003ce" name="E Out" connectedTo="a8253154-da98-4ed1-a1a2-7aefb722f25c" id="ac4c9d42-f557-4bb9-8b19-1c11a552469a"/>
        <port xsi:type="esdl:OutPort" carrier="d9b6ad9b-0fba-40c2-91fe-7f063d9e7b6d" name="H Out" connectedTo="3dbbda19-0ce7-48f3-8ab3-2aec9bc07182" id="c7c61412-5f0c-49fd-967f-133b0884cd80"/>
      </asset>
      <asset xsi:type="esdl:Import" power="100000.0" name="Import_7e31" id="7e319aa6-5e8b-49f9-9ccd-973e74cebeb3">
        <geometry xsi:type="esdl:Point" lat="51.797920225864985" lon="4.414775222539903"/>
        <costInformation xsi:type="esdl:CostInformation">
          <marginalCosts xsi:type="esdl:SingleValue" id="92a675f7-1d1b-4811-9fc0-2a35ae0f9b26" name="Import_7e31-MarginalCosts" value="0.9"/>
        </costInformation>
        <port xsi:type="esdl:OutPort" carrier="9d13b486-dba4-4ab6-bce1-80a388b32119" name="Out" connectedTo="9406985d-ebba-4792-a0d6-73f9f1e60439" id="3508210c-15b7-4b72-bd81-48948a6a83f9"/>
      </asset>
      <asset xsi:type="esdl:HeatingDemand" name="HeatingDemand_9f58" id="9f5894ea-93c5-4382-a325-e1d16e1095db">
        <geometry xsi:type="esdl:Point" lat="51.79801642105753" CRS="WGS84" lon="4.415546519158876"/>
        <port xsi:type="esdl:InPort" carrier="130ee23f-d41a-4f86-a7ae-0bc6dbc003ce" name="In" connectedTo="ac4c9d42-f557-4bb9-8b19-1c11a552469a" id="a8253154-da98-4ed1-a1a2-7aefb722f25c">
          <profile xsi:type="esdl:SingleValue" id="7f7c1a95-51f9-42b0-a9ea-158b0c45e528" value="8.0">
            <profileQuantityAndUnit xsi:type="esdl:QuantityAndUnitReference" reference="12c481c0-f81e-49b6-9767-90457684d24a"/>
          </profile>
        </port>
      </asset>
      <asset xsi:type="esdl:ElectricityDemand" name="ElectricityDemand_0601" id="0601ddb2-54d8-4064-9bca-35d40e3bda11">
        <geometry xsi:type="esdl:Point" lat="51.79781073686574" CRS="WGS84" lon="4.415535789699155"/>
        <port xsi:type="esdl:InPort" carrier="d9b6ad9b-0fba-40c2-91fe-7f063d9e7b6d" name="In" connectedTo="081f620e-ce19-41b7-9d21-3ea615ae5c58" id="e6069079-28d2-4b8f-8d70-5c400874e948">
          <profile xsi:type="esdl:TimeSeriesProfile" name="E Demand profile" id="0578899b-e36a-4ff9-b4b0-6a93ebd5e57b" startDateTime="2019-01-01T00:00:00.000000" values="12.0 0.0 1.0 2.0 3.0 5.0 7.0 12.0 3.0 0.0 1.0 0.0">
            <profileQuantityAndUnit xsi:type="esdl:QuantityAndUnitReference" reference="12c481c0-f81e-49b6-9767-90457684d24a"/>
          </profile>
        </port>
      </asset>
      <asset xsi:type="esdl:Import" power="20000.0" name="Import_05f5" id="05f58064-78b0-4951-b371-0b4265d21d8d">
        <geometry xsi:type="esdl:Point" lat="51.79775144498082" CRS="WGS84" lon="4.415307641029359"/>
        <costInformation xsi:type="esdl:CostInformation">
          <marginalCosts xsi:type="esdl:SingleValue" id="1c5bec1d-69e4-40e4-878d-c0b479c46f66" name="Import_05f5-MarginalCosts" value="0.9"/>
        </costInformation>
        <port xsi:type="esdl:OutPort" carrier="d9b6ad9b-0fba-40c2-91fe-7f063d9e7b6d" name="Out" connectedTo="3dbbda19-0ce7-48f3-8ab3-2aec9bc07182" id="acc7d613-3769-43c2-b374-510476e850e9"/>
      </asset>
      <asset xsi:type="esdl:ElectricityNetwork" name="ElectricityNetwork_8e92" id="8e926c51-338e-477f-897a-532ab814f098">
        <geometry xsi:type="esdl:Point" lat="51.797849727783785" CRS="WGS84" lon="4.415323734283448"/>
        <port xsi:type="esdl:InPort" carrier="d9b6ad9b-0fba-40c2-91fe-7f063d9e7b6d" name="In" connectedTo="acc7d613-3769-43c2-b374-510476e850e9 c7c61412-5f0c-49fd-967f-133b0884cd80" id="3dbbda19-0ce7-48f3-8ab3-2aec9bc07182"/>
        <port xsi:type="esdl:OutPort" carrier="d9b6ad9b-0fba-40c2-91fe-7f063d9e7b6d" name="Out" connectedTo="e6069079-28d2-4b8f-8d70-5c400874e948" id="081f620e-ce19-41b7-9d21-3ea615ae5c58"/>
      </asset>
      <asset xsi:type="esdl:Export" power="100000.0"  id="e6de95e7-8ee3-4a31-ac09-b8c9e76f524f" name="Export_e6de">
        <costInformation xsi:type="esdl:CostInformation">
          <marginalCosts xsi:type="esdl:SingleValue" name="Export_e6de-MarginalCosts" value="0.1" id="b26c4534-3ecb-40cb-af5d-ac3da64b8214"/>
        </costInformation>
        <geometry xsi:type="esdl:Point" CRS="WGS84" lat="51.797892441340274" lon="4.415553733706475"/>
        <port xsi:type="esdl:InPort" carrier="d9b6ad9b-0fba-40c2-91fe-7f063d9e7b6d" connectedTo="081f620e-ce19-41b7-9d21-3ea615ae5c58" name="In" id="2613df35-d30b-4cf1-bfff-f225b8791268"/>
      </asset>
      <asset xsi:type="esdl:Export" power="100000.0" id="7741a966-39c8-4179-89b7-352844f2e1ca" name="Export_7741">
        <costInformation xsi:type="esdl:CostInformation">
          <marginalCosts xsi:type="esdl:SingleValue" name="Export_7741-MarginalCosts" value="0.1" id="e860e941-51af-48f5-a33a-109fb2882e92"/>
        </costInformation>
        <geometry xsi:type="esdl:Point" CRS="WGS84" lat="51.79807075963405" lon="4.415480643510819"/>
        <port xsi:type="esdl:InPort" carrier="130ee23f-d41a-4f86-a7ae-0bc6dbc003ce" connectedTo="ac4c9d42-f557-4bb9-8b19-1c11a552469a" name="In" id="850caf96-c26d-4e47-a176-ef1bd635aef6"/>
      </asset>
      <asset xsi:type="esdl:Import" id="92acdfcc-d095-4d69-8e8b-09f3f2af18f7" power="100000.0" name="Import_92ac">
        <costInformation xsi:type="esdl:CostInformation">
          <marginalCosts xsi:type="esdl:SingleValue" name="Import_92ac-MarginalCosts" value="0.9" id="5eca483b-1506-4557-b259-ec4d6c9b4e96"/>
        </costInformation>
        <geometry xsi:type="esdl:Point" CRS="WGS84" lat="51.79809025019552" lon="4.415365979075433"/>
        <port xsi:type="esdl:OutPort" carrier="130ee23f-d41a-4f86-a7ae-0bc6dbc003ce" connectedTo="a8253154-da98-4ed1-a1a2-7aefb722f25c" name="Out" id="d0847820-21b1-47e5-9591-d0eac0a931b7"/>
      </asset>
    </area>
  </instance>
  <services xsi:type="esdl:Services" id="39ea0b0e-013f-4c72-ac6e-90f48e9e9afc">
    <service xsi:type="esdl:DrivenByDemand" energyAsset="620efa79-b989-4fc5-8cd0-26d4e4fc6eb8" name="DrivenByDemand for CHP_620e" id="b48bb602-bcaa-43e5-970c-42e8cce7a4f8" outPort="c7c61412-5f0c-49fd-967f-133b0884cd80"/>
  </services>
</esdl:EnergySystem>
