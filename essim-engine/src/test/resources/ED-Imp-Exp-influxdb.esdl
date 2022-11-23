<?xml version='1.0' encoding='UTF-8'?>
<esdl:EnergySystem xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:esdl="http://www.tno.nl/esdl" id="a4ebc0f7-da5a-4e6f-a7fd-85107d7287fa" description="" esdlVersion="v2207" version="6" name="ED-Imp-Exp">
  <energySystemInformation xsi:type="esdl:EnergySystemInformation" id="cd040fd2-e0c0-4f20-8ca4-d82b5c4cd3ad">
    <quantityAndUnits xsi:type="esdl:QuantityAndUnits" id="d5e36bd2-e478-4952-b734-6dcea859365a">
      <quantityAndUnit xsi:type="esdl:QuantityAndUnitType" physicalQuantity="ENERGY" multiplier="GIGA" id="eb07bccb-203f-407e-af98-e687656a221d" description="Energy in GJ" unit="JOULE"/>
    </quantityAndUnits>
    <carriers xsi:type="esdl:Carriers" id="cff3dc52-611c-439a-ad02-eeda7fd6a55f">
      <carrier xsi:type="esdl:ElectricityCommodity" name="Electricity" id="2f0c1159-d7dc-4190-95d0-29b05608961b"/>
    </carriers>
  </energySystemInformation>
  <instance xsi:type="esdl:Instance" id="c309d262-f484-455a-b0a5-39c658eecf47" name="Untitled Instance">
    <area xsi:type="esdl:Area" id="db57a88a-7e98-4698-8a88-a44b7bf100b4" name="Untitled Area">
      <asset xsi:type="esdl:ElectricityDemand" name="ElectricityDemand_c106" id="c10691dd-3cc0-4804-a526-6069c16144c0">
        <geometry xsi:type="esdl:Point" CRS="WGS84" lat="52.305119921105245" lon="6.064453125000001"/>
        <port xsi:type="esdl:InPort" id="6fd63a50-94ba-4f1c-9a5e-1076c6f7ff3d" carrier="2f0c1159-d7dc-4190-95d0-29b05608961b" connectedTo="3c247acf-485f-4305-a70c-b263b7966e4f" name="In">
          <profile xsi:type="esdl:InfluxDBProfile" id="3fc38656-8850-4eff-ac63-a9a24eae1ded" host="http://10.30.2.1" multiplier="10.0" startDate="2015-01-01T00:00:00.000000+0100" measurement="nedu_elektriciteit_2015-2018" endDate="2016-01-01T00:00:00.000000+0100" field="E1A" database="energy_profiles" filters="" port="8086">
            <profileQuantityAndUnit xsi:type="esdl:QuantityAndUnitReference" reference="eb07bccb-203f-407e-af98-e687656a221d"/>
          </profile>
        </port>
      </asset>
      <asset xsi:type="esdl:Import" name="Import_7e47" id="7e47b79a-4cde-4db9-81ae-67fd40a92de0" power="15000.0">
        <costInformation xsi:type="esdl:CostInformation">
          <marginalCosts xsi:type="esdl:SingleValue" value="0.9" id="f5fcb90e-a128-4eb7-9c78-d0b6e849d48a" name="Import_7e47-MarginalCosts"/>
        </costInformation>
        <geometry xsi:type="esdl:Point" CRS="WGS84" lat="52.305226579952574" lon="6.063165787442807"/>
        <port xsi:type="esdl:OutPort" id="7264204b-d71d-4e13-8ae1-b7916f1e6bd2" carrier="2f0c1159-d7dc-4190-95d0-29b05608961b" connectedTo="8f59e2de-e0c1-48a9-bbf7-1e65875ff05f" name="Out"/>
      </asset>
      <asset xsi:type="esdl:Export" name="Export_66b0" id="66b0e3a3-6379-4525-b980-90eb95be944b" power="15000.0">
        <costInformation xsi:type="esdl:CostInformation">
          <marginalCosts xsi:type="esdl:SingleValue" value="0.1" id="39daa06f-122f-474b-8572-acc467cbf433" name="Export_66b0-MarginalCosts"/>
        </costInformation>
        <geometry xsi:type="esdl:Point" CRS="WGS84" lat="52.30505359493556" lon="6.063163104772241"/>
        <port xsi:type="esdl:InPort" id="56735967-0353-472c-aa48-c8744290deb6" carrier="2f0c1159-d7dc-4190-95d0-29b05608961b" connectedTo="3c247acf-485f-4305-a70c-b263b7966e4f" name="In"/>
      </asset>
      <asset xsi:type="esdl:ElectricityNetwork" name="ElectricityNetwork_973e" id="973e635c-e083-448a-97c0-8147be1c1757">
        <geometry xsi:type="esdl:Point" CRS="WGS84" lat="52.30512983959983" lon="6.063768046982646"/>
        <port xsi:type="esdl:InPort" id="8f59e2de-e0c1-48a9-bbf7-1e65875ff05f" carrier="2f0c1159-d7dc-4190-95d0-29b05608961b" connectedTo="7264204b-d71d-4e13-8ae1-b7916f1e6bd2" name="In"/>
        <port xsi:type="esdl:OutPort" id="3c247acf-485f-4305-a70c-b263b7966e4f" carrier="2f0c1159-d7dc-4190-95d0-29b05608961b" connectedTo="6fd63a50-94ba-4f1c-9a5e-1076c6f7ff3d 56735967-0353-472c-aa48-c8744290deb6" name="Out"/>
      </asset>
    </area>
  </instance>
</esdl:EnergySystem>
