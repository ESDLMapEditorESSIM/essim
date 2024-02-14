<?xml version='1.0' encoding='UTF-8'?>
<esdl:EnergySystem xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:esdl="http://www.tno.nl/esdl" description="" esdlVersion="v2311" name="Untitled EnergySystem" version="1" id="2f1f63c7-892b-440e-b48a-d806f5fe85eb">
  <energySystemInformation xsi:type="esdl:EnergySystemInformation" id="88753e8c-a5a3-4376-b142-404c2880c1db">
    <quantityAndUnits xsi:type="esdl:QuantityAndUnits" id="e54777bd-afc6-46ea-b366-398e71c40b1f">
      <quantityAndUnit xsi:type="esdl:QuantityAndUnitType" description="Energy in GJ" physicalQuantity="ENERGY" multiplier="GIGA" id="eb07bccb-203f-407e-af98-e687656a221d" unit="JOULE"/>
    </quantityAndUnits>
    <carriers xsi:type="esdl:Carriers" id="bf9d946c-7028-4610-9153-9d7dceab9164">
      <carrier xsi:type="esdl:GasCommodity" id="9d13b486-dba4-4ab6-bce1-80a388b32119" name="gas"/>
      <carrier xsi:type="esdl:ElectricityCommodity" id="d9b6ad9b-0fba-40c2-91fe-7f063d9e7b6d" name="elektriciteit"/>
      <carrier xsi:type="esdl:HeatCommodity" id="130ee23f-d41a-4f86-a7ae-0bc6dbc003ce" name="warmte"/>
    </carriers>
  </energySystemInformation>
  <instance xsi:type="esdl:Instance" id="810b4bf5-4434-4825-96fc-5eb4be231fff" name="Untitled Instance">
    <area xsi:type="esdl:Area" name="Untitled Area" id="bb635dbc-e569-4428-80d8-0038e2124d0b">
      <asset xsi:type="esdl:CHP" controlStrategy="7f5943cf-38f9-44c6-9ed4-28e36e989cbf" efficiency="1.0" name="CHP_620e" power="10000.0" heatEfficiency="0.4" id="620efa79-b989-4fc5-8cd0-26d4e4fc6eb8" electricalEfficiency="0.5">
        <geometry xsi:type="esdl:Point" lon="4.415173670433701" CRS="WGS84" lat="51.797908602848196"/>
        <port xsi:type="esdl:InPort" connectedTo="3508210c-15b7-4b72-bd81-48948a6a83f9" id="9406985d-ebba-4792-a0d6-73f9f1e60439" carrier="9d13b486-dba4-4ab6-bce1-80a388b32119" name="In"/>
        <port xsi:type="esdl:OutPort" id="ac4c9d42-f557-4bb9-8b19-1c11a552469a" connectedTo="a8253154-da98-4ed1-a1a2-7aefb722f25c" carrier="130ee23f-d41a-4f86-a7ae-0bc6dbc003ce" name="E Out"/>
        <port xsi:type="esdl:OutPort" id="c7c61412-5f0c-49fd-967f-133b0884cd80" connectedTo="e6069079-28d2-4b8f-8d70-5c400874e948" carrier="d9b6ad9b-0fba-40c2-91fe-7f063d9e7b6d" name="H Out"/>
      </asset>
      <asset xsi:type="esdl:Import" power="10000.0" name="Import_7e31" id="7e319aa6-5e8b-49f9-9ccd-973e74cebeb3">
        <geometry xsi:type="esdl:Point" lon="4.414776680424196" CRS="WGS84" lat="51.797920214052375"/>
        <port xsi:type="esdl:OutPort" id="3508210c-15b7-4b72-bd81-48948a6a83f9" connectedTo="9406985d-ebba-4792-a0d6-73f9f1e60439" carrier="9d13b486-dba4-4ab6-bce1-80a388b32119" name="Out"/>
      </asset>
      <asset xsi:type="esdl:HeatingDemand" name="HeatingDemand_9f58" id="9f5894ea-93c5-4382-a325-e1d16e1095db">
        <geometry xsi:type="esdl:Point" lon="4.415546519158876" CRS="WGS84" lat="51.79801642105753"/>
        <port xsi:type="esdl:InPort" connectedTo="ac4c9d42-f557-4bb9-8b19-1c11a552469a" id="a8253154-da98-4ed1-a1a2-7aefb722f25c" carrier="130ee23f-d41a-4f86-a7ae-0bc6dbc003ce" name="In">
          <profile xsi:type="esdl:InfluxDBProfile" endDate="2019-12-31T22:00:00.000000+0000" field="G1A" measurement="standard_profiles" database="energy_profiles" id="0e193a04-f5a1-4a54-b458-2fb779046426" multiplier="50.0" startDate="2018-12-31T23:00:00.000000+0000" filters="" host="http://10.30.2.1" port="8086">
            <profileQuantityAndUnit xsi:type="esdl:QuantityAndUnitReference" reference="eb07bccb-203f-407e-af98-e687656a221d"/>
          </profile>
        </port>
      </asset>
      <asset xsi:type="esdl:ElectricityDemand" power="10000.0" name="ElectricityDemand_0601" id="0601ddb2-54d8-4064-9bca-35d40e3bda11">
        <geometry xsi:type="esdl:Point" lon="4.415535789699155" CRS="WGS84" lat="51.79781073686574"/>
        <port xsi:type="esdl:InPort" connectedTo="c7c61412-5f0c-49fd-967f-133b0884cd80" id="e6069079-28d2-4b8f-8d70-5c400874e948" carrier="d9b6ad9b-0fba-40c2-91fe-7f063d9e7b6d" name="In"/>
      </asset>
    </area>
  </instance>
  <services xsi:type="esdl:Services" id="39ea0b0e-013f-4c72-ac6e-90f48e9e9afc">
    <service xsi:type="esdl:DrivenByDemand" outPort="c7c61412-5f0c-49fd-967f-133b0884cd80" id="7f5943cf-38f9-44c6-9ed4-28e36e989cbf" energyAsset="620efa79-b989-4fc5-8cd0-26d4e4fc6eb8" name="DrivenByDemand for CHP_620e"/>
  </services>
</esdl:EnergySystem>
