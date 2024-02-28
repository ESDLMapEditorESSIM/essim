<?xml version='1.0' encoding='UTF-8'?>
<esdl:EnergySystem xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:esdl="http://www.tno.nl/esdl" version="7" id="4ebd69bb-f696-411c-9317-847d7576978d" description="" esdlVersion="v2401" name="Test conversion">
  <instance xsi:type="esdl:Instance" id="a865b251-083e-43b2-a67f-6384f7749b89" name="Untitled instance">
    <area xsi:type="esdl:Area" name="Untitled area" id="bc85a495-a064-48bc-bfb0-ce15be37eabd">
      <asset xsi:type="esdl:Import" id="5587c1e1-eebd-4779-9439-a2216b29fe44" power="100000.0" name="Import_5587">
        <costInformation xsi:type="esdl:CostInformation">
          <marginalCosts xsi:type="esdl:SingleValue" name="Import_5587-MarginalCosts" id="7c4bcb72-1797-4639-87a0-ae1210d51171" value="0.9"/>
        </costInformation>
        <port xsi:type="esdl:OutPort" id="b723e5ea-f29c-4f1b-9773-08d0786a6dee" connectedTo="3887dc69-dc86-422c-a51b-1a47e7f5d164" carrier="d1e3e04a-29a4-4297-8034-9c955471e42a" name="Out"/>
        <geometry xsi:type="esdl:Point" lon="4.478429406881333" CRS="WGS84" lat="52.179004302424076"/>
      </asset>
      <asset xsi:type="esdl:GenericConversion" id="7e877c62-0003-41dd-b0a5-c0b3347f9dc5" power="10000.0" controlStrategy="63f46c5f-6ad1-4684-b44d-3211816291c8" name="GenericConversion_7e87">
        <costInformation xsi:type="esdl:CostInformation">
          <marginalCosts xsi:type="esdl:SingleValue" name="GenericConversion_7e87-MarginalCosts" id="9f11fcdf-afd4-4d5a-8e93-da480adc95c2" value="0.4"/>
        </costInformation>
        <port xsi:type="esdl:InPort" id="3887dc69-dc86-422c-a51b-1a47e7f5d164" carrier="d1e3e04a-29a4-4297-8034-9c955471e42a" name="In" connectedTo="b723e5ea-f29c-4f1b-9773-08d0786a6dee"/>
        <port xsi:type="esdl:OutPort" id="8edd7b44-0eb8-46af-9648-99729f5f4a4e" connectedTo="e2db9700-0ae4-460f-89de-5c76be3e3cfa 4fa6e92a-d419-4b79-a99e-fd1aa8dfc8a2" carrier="5c1d7e2d-54e8-42b6-983d-5b804c6c6094" name="Out"/>
        <geometry xsi:type="esdl:Point" lon="4.478546753525735" CRS="WGS84" lat="52.179107097389114"/>
      </asset>
      <asset xsi:type="esdl:ElectricityDemand" id="7321d5b5-6730-46fe-a11a-459208c7975e" name="ElectricityDemand_7321">
        <costInformation xsi:type="esdl:CostInformation">
          <marginalCosts xsi:type="esdl:SingleValue" name="ElectricityDemand_7321-MarginalCosts" id="3089aff0-0978-4159-a72b-1aa537968fe4" value="0.5"/>
        </costInformation>
        <port xsi:type="esdl:InPort" connectedTo="8edd7b44-0eb8-46af-9648-99729f5f4a4e" id="e2db9700-0ae4-460f-89de-5c76be3e3cfa" carrier="5c1d7e2d-54e8-42b6-983d-5b804c6c6094" name="In">
          <profile xsi:type="esdl:TimeSeriesProfile" name="E Demand profile" id="e9e2a3df-c23c-4bb7-ac86-7342157af729" startDateTime="2019-01-01T00:00:00.000000" values="12.0 0.0 1.0 2.0 3.0 5.0 7.0 12.0 3.0 0.0 1.0 0.0">
            <profileQuantityAndUnit xsi:type="esdl:QuantityAndUnitReference" reference="12c481c0-f81e-49b6-9767-90457684d24a"/>
          </profile>
        </port>
        <geometry xsi:type="esdl:Point" lon="4.478656053543092" CRS="WGS84" lat="52.179200435011595"/>
      </asset>
      <asset xsi:type="esdl:Export" id="b874c329-8837-4632-b26c-6ed730876705" power="100000.0" name="Export_b874">
        <costInformation xsi:type="esdl:CostInformation">
          <marginalCosts xsi:type="esdl:SingleValue" name="Export_b874-MarginalCosts" id="f0ec471f-2dfb-4e1e-970b-aa58076c1b0b" value="0.1"/>
        </costInformation>
        <port xsi:type="esdl:InPort" id="4fa6e92a-d419-4b79-a99e-fd1aa8dfc8a2" carrier="5c1d7e2d-54e8-42b6-983d-5b804c6c6094" name="In" connectedTo="8edd7b44-0eb8-46af-9648-99729f5f4a4e"/>
        <geometry xsi:type="esdl:Point" lon="4.478757977485658" CRS="WGS84" lat="52.179167129538804"/>
      </asset>
    </area>
  </instance>
  <services xsi:type="esdl:Services">
    <service xsi:type="esdl:DrivenByDemand" energyAsset="7e877c62-0003-41dd-b0a5-c0b3347f9dc5" id="63f46c5f-6ad1-4684-b44d-3211816291c8" outPort="8edd7b44-0eb8-46af-9648-99729f5f4a4e" name="DrivenByDemand for GenericConversion_7e87"/>
  </services>
  <energySystemInformation xsi:type="esdl:EnergySystemInformation" id="e0b645e9-636b-4f6b-aed7-caccd1da5429">
    <carriers xsi:type="esdl:Carriers" id="ac7c8cc2-a143-4edd-9d51-64ddb759661b">
      <carrier xsi:type="esdl:ElectricityCommodity" voltage="380.0" name="Electricity" id="5c1d7e2d-54e8-42b6-983d-5b804c6c6094"/>
      <carrier xsi:type="esdl:GasCommodity" name="Gas" id="d1e3e04a-29a4-4297-8034-9c955471e42a"/>
    </carriers>
    <quantityAndUnits xsi:type="esdl:QuantityAndUnits" id="cebca2c4-c336-4a0f-96f3-43ebeeea1083">
      <quantityAndUnit xsi:type="esdl:QuantityAndUnitType" description="Energy in kWh" physicalQuantity="ENERGY" id="12c481c0-f81e-49b6-9767-90457684d24a" multiplier="KILO" unit="WATTHOUR"/>
    </quantityAndUnits>
  </energySystemInformation>
</esdl:EnergySystem>
