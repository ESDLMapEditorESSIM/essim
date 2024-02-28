<?xml version='1.0' encoding='UTF-8'?>
<esdl:EnergySystem xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:esdl="http://www.tno.nl/esdl" description="" esdlVersion="v2401" name="Test conversion" version="8" id="4ebd69bb-f696-411c-9317-847d7576978d">
  <energySystemInformation xsi:type="esdl:EnergySystemInformation" id="e0b645e9-636b-4f6b-aed7-caccd1da5429">
    <carriers xsi:type="esdl:Carriers" id="ac7c8cc2-a143-4edd-9d51-64ddb759661b">
      <carrier xsi:type="esdl:ElectricityCommodity" name="Electricity" voltage="380.0" id="5c1d7e2d-54e8-42b6-983d-5b804c6c6094"/>
      <carrier xsi:type="esdl:GasCommodity" name="Gas" id="d1e3e04a-29a4-4297-8034-9c955471e42a"/>
    </carriers>
    <quantityAndUnits xsi:type="esdl:QuantityAndUnits" id="cebca2c4-c336-4a0f-96f3-43ebeeea1083">
      <quantityAndUnit xsi:type="esdl:QuantityAndUnitType" multiplier="KILO" physicalQuantity="ENERGY" description="Energy in kWh" unit="WATTHOUR" id="12c481c0-f81e-49b6-9767-90457684d24a"/>
    </quantityAndUnits>
  </energySystemInformation>
  <instance xsi:type="esdl:Instance" name="Untitled instance" id="a865b251-083e-43b2-a67f-6384f7749b89">
    <area xsi:type="esdl:Area" id="bc85a495-a064-48bc-bfb0-ce15be37eabd" name="Untitled area">
      <asset xsi:type="esdl:Import" name="Import_5587" power="100000.0" id="5587c1e1-eebd-4779-9439-a2216b29fe44">
        <costInformation xsi:type="esdl:CostInformation">
          <marginalCosts xsi:type="esdl:SingleValue" id="7c4bcb72-1797-4639-87a0-ae1210d51171" value="0.9" name="Import_5587-MarginalCosts"/>
        </costInformation>
        <port xsi:type="esdl:OutPort" name="Out" connectedTo="3887dc69-dc86-422c-a51b-1a47e7f5d164" id="b723e5ea-f29c-4f1b-9773-08d0786a6dee" carrier="d1e3e04a-29a4-4297-8034-9c955471e42a"/>
        <geometry xsi:type="esdl:Point" lon="4.478429406881333" lat="52.179004302424076" CRS="WGS84"/>
      </asset>
      <asset xsi:type="esdl:GenericConversion" name="GenericConversion_7e87" id="7e877c62-0003-41dd-b0a5-c0b3347f9dc5" controlStrategy="63f46c5f-6ad1-4684-b44d-3211816291c8" power="10000.0">
        <costInformation xsi:type="esdl:CostInformation">
          <marginalCosts xsi:type="esdl:SingleValue" id="9f11fcdf-afd4-4d5a-8e93-da480adc95c2" value="0.4" name="GenericConversion_7e87-MarginalCosts"/>
        </costInformation>
        <port xsi:type="esdl:InPort" name="In" connectedTo="b723e5ea-f29c-4f1b-9773-08d0786a6dee" id="3887dc69-dc86-422c-a51b-1a47e7f5d164" carrier="d1e3e04a-29a4-4297-8034-9c955471e42a"/>
        <port xsi:type="esdl:OutPort" name="Out" connectedTo="e2db9700-0ae4-460f-89de-5c76be3e3cfa" id="8edd7b44-0eb8-46af-9648-99729f5f4a4e" carrier="5c1d7e2d-54e8-42b6-983d-5b804c6c6094"/>
        <geometry xsi:type="esdl:Point" lon="4.478546753525735" lat="52.179107097389114" CRS="WGS84"/>
      </asset>
      <asset xsi:type="esdl:ElectricityDemand" name="ElectricityDemand_7321" id="7321d5b5-6730-46fe-a11a-459208c7975e">
        <costInformation xsi:type="esdl:CostInformation">
          <marginalCosts xsi:type="esdl:SingleValue" id="3089aff0-0978-4159-a72b-1aa537968fe4" value="0.5" name="ElectricityDemand_7321-MarginalCosts"/>
        </costInformation>
        <port xsi:type="esdl:InPort" name="In" connectedTo="8edd7b44-0eb8-46af-9648-99729f5f4a4e 4f738318-aa4a-4747-b947-a0fa201204f1" id="e2db9700-0ae4-460f-89de-5c76be3e3cfa" carrier="5c1d7e2d-54e8-42b6-983d-5b804c6c6094">
          <profile xsi:type="esdl:TimeSeriesProfile" name="E Demand profile" id="e9e2a3df-c23c-4bb7-ac86-7342157af729" startDateTime="2019-01-01T00:00:00.000000" values="12.0 0.0 1.0 2.0 3.0 5.0 7.0 12.0 3.0 0.0 1.0 0.0">
            <profileQuantityAndUnit xsi:type="esdl:QuantityAndUnitReference" reference="12c481c0-f81e-49b6-9767-90457684d24a"/>
          </profile>
        </port>
        <geometry xsi:type="esdl:Point" lon="4.478656053543092" lat="52.179200435011595" CRS="WGS84"/>
      </asset>
      <asset xsi:type="esdl:Import" name="Import_e249" id="e2493006-6a46-4684-ab6b-49f4f52707cd" power="20000.0">
        <costInformation xsi:type="esdl:CostInformation">
          <marginalCosts xsi:type="esdl:SingleValue" name="Import_e249-MarginalCosts" value="0.9" id="9add6093-c5bc-4266-9058-a117145f9b79"/>
        </costInformation>
        <port xsi:type="esdl:OutPort" name="Out" connectedTo="e2db9700-0ae4-460f-89de-5c76be3e3cfa" id="4f738318-aa4a-4747-b947-a0fa201204f1" carrier="5c1d7e2d-54e8-42b6-983d-5b804c6c6094"/>
        <geometry xsi:type="esdl:Point" lon="4.4787680357694635" lat="52.179121488665274" CRS="WGS84"/>
      </asset>
    </area>
  </instance>
  <services xsi:type="esdl:Services">
    <service xsi:type="esdl:DrivenByDemand" outPort="8edd7b44-0eb8-46af-9648-99729f5f4a4e" energyAsset="7e877c62-0003-41dd-b0a5-c0b3347f9dc5" id="63f46c5f-6ad1-4684-b44d-3211816291c8" name="DrivenByDemand for GenericConversion_7e87"/>
  </services>
</esdl:EnergySystem>
