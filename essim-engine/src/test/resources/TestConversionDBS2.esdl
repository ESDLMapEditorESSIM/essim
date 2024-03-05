<?xml version='1.0' encoding='UTF-8'?>
<esdl:EnergySystem xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:esdl="http://www.tno.nl/esdl" version="13" id="4ebd69bb-f696-411c-9317-847d7576978d" description="" esdlVersion="v2401" name="Test conversion">
  <instance xsi:type="esdl:Instance" id="a865b251-083e-43b2-a67f-6384f7749b89" name="Untitled instance">
    <area xsi:type="esdl:Area" id="bc85a495-a064-48bc-bfb0-ce15be37eabd" name="Untitled area">
      <asset xsi:type="esdl:GenericConversion" power="10000.0" id="7e877c62-0003-41dd-b0a5-c0b3347f9dc5" controlStrategy="d61bef91-4eab-4390-8090-65224619f068" efficiency="0.8" name="GenericConversion_7e87">
        <port xsi:type="esdl:InPort" carrier="d1e3e04a-29a4-4297-8034-9c955471e42a" name="In" connectedTo="bc25c2ae-8ed0-490e-9ef3-6e13e1738579" id="3887dc69-dc86-422c-a51b-1a47e7f5d164"/>
        <port xsi:type="esdl:OutPort" connectedTo="e850556c-91cb-422d-9318-3990130bdc54" carrier="5c1d7e2d-54e8-42b6-983d-5b804c6c6094" name="Out" id="8edd7b44-0eb8-46af-9648-99729f5f4a4e"/>
        <geometry xsi:type="esdl:Point" lat="52.179107097389114" lon="4.478546753525735" CRS="WGS84"/>
        <costInformation xsi:type="esdl:CostInformation">
          <marginalCosts xsi:type="esdl:SingleValue" id="9f11fcdf-afd4-4d5a-8e93-da480adc95c2" value="0.6" name="GenericConversion_7e87-MarginalCosts"/>
        </costInformation>
      </asset>
      <asset xsi:type="esdl:ElectricityDemand" id="7321d5b5-6730-46fe-a11a-459208c7975e" power="100000.0" name="ElectricityDemand_7321">
        <port xsi:type="esdl:InPort" carrier="5c1d7e2d-54e8-42b6-983d-5b804c6c6094" name="In" connectedTo="48fcfd9a-af64-4d32-b448-44df354b0d0d" id="e2db9700-0ae4-460f-89de-5c76be3e3cfa">
          <profile xsi:type="esdl:SingleValue" id="34585fab-73e9-4b69-b9b2-a4744a69e8eb" value="7.0">
            <profileQuantityAndUnit xsi:type="esdl:QuantityAndUnitReference" reference="12c481c0-f81e-49b6-9767-90457684d24a"/>
          </profile>
        </port>
        <geometry xsi:type="esdl:Point" lat="52.179200435011595" lon="4.478656053543092" CRS="WGS84"/>
        <costInformation xsi:type="esdl:CostInformation">
          <marginalCosts xsi:type="esdl:SingleValue" id="3089aff0-0978-4159-a72b-1aa537968fe4" value="0.1" name="ElectricityDemand_7321-MarginalCosts"/>
        </costInformation>
      </asset>
      <asset xsi:type="esdl:Import" id="e2493006-6a46-4684-ab6b-49f4f52707cd" power="20000.0" name="Import_e249">
        <port xsi:type="esdl:OutPort" connectedTo="e850556c-91cb-422d-9318-3990130bdc54" carrier="5c1d7e2d-54e8-42b6-983d-5b804c6c6094" name="Out" id="4f738318-aa4a-4747-b947-a0fa201204f1"/>
        <geometry xsi:type="esdl:Point" lat="52.179121488665274" lon="4.4787680357694635" CRS="WGS84"/>
        <costInformation xsi:type="esdl:CostInformation">
          <marginalCosts xsi:type="esdl:SingleValue" id="9add6093-c5bc-4266-9058-a117145f9b79" name="Import_e249-MarginalCosts" value="0.9"/>
        </costInformation>
      </asset>
      <asset xsi:type="esdl:GasProducer" id="87be62c5-208f-4ddd-974e-f99a7ca1044a" name="GasProducer_87be">
        <port xsi:type="esdl:OutPort" connectedTo="3887dc69-dc86-422c-a51b-1a47e7f5d164 57dbfebc-95ad-4a74-908d-a8d86380ca06" carrier="d1e3e04a-29a4-4297-8034-9c955471e42a" name="Out" id="bc25c2ae-8ed0-490e-9ef3-6e13e1738579">
          <profile xsi:type="esdl:TimeSeriesProfile" name="E Demand profile" id="e9e2a3df-c23c-4bb7-ac86-7342157af729" startDateTime="2019-01-01T00:00:00.000000" values="12.5 0.0 1.0 2.0 3.0 5.0 7.0 15 3.0 0.0 1.0 0.0">
            <profileQuantityAndUnit xsi:type="esdl:QuantityAndUnitReference" reference="12c481c0-f81e-49b6-9767-90457684d24a"/>
          </profile>
        </port>
        <geometry xsi:type="esdl:Point" lat="52.17901910491368" lon="4.478505849838258" CRS="WGS84"/>
      </asset>
      <asset xsi:type="esdl:Export" power="100000.0" id="ca558d4c-38d8-4c2b-9103-d10420f9685f" name="Export_ca55">
        <port xsi:type="esdl:InPort" carrier="d1e3e04a-29a4-4297-8034-9c955471e42a" name="In" connectedTo="bc25c2ae-8ed0-490e-9ef3-6e13e1738579" id="57dbfebc-95ad-4a74-908d-a8d86380ca06"/>
        <geometry xsi:type="esdl:Point" lat="52.179013348390534" lon="4.478591009974481" CRS="WGS84"/>
        <costInformation xsi:type="esdl:CostInformation">
          <marginalCosts xsi:type="esdl:SingleValue" id="6a7afd81-6145-4a54-a9d8-c02141ae2f2f" value="0.1" name="Export_ca55-MarginalCosts"/>
        </costInformation>
      </asset>
      <asset xsi:type="esdl:ElectricityNetwork" id="950e4179-dab3-4263-80c2-79316ceed9be" name="ElectricityNetwork_950e">
        <port xsi:type="esdl:InPort" carrier="5c1d7e2d-54e8-42b6-983d-5b804c6c6094" name="In" connectedTo="8edd7b44-0eb8-46af-9648-99729f5f4a4e 4f738318-aa4a-4747-b947-a0fa201204f1" id="e850556c-91cb-422d-9318-3990130bdc54"/>
        <port xsi:type="esdl:OutPort" connectedTo="e2db9700-0ae4-460f-89de-5c76be3e3cfa" carrier="5c1d7e2d-54e8-42b6-983d-5b804c6c6094" name="Out" id="48fcfd9a-af64-4d32-b448-44df354b0d0d"/>
        <geometry xsi:type="esdl:Point" lat="52.17913752465319" lon="4.47864666581154" CRS="WGS84"/>
      </asset>
    </area>
  </instance>
  <services xsi:type="esdl:Services">
    <service xsi:type="esdl:DrivenBySupply" inPort="3887dc69-dc86-422c-a51b-1a47e7f5d164" id="d61bef91-4eab-4390-8090-65224619f068" name="DrivenBySupply for GenericConversion_7e87" energyAsset="7e877c62-0003-41dd-b0a5-c0b3347f9dc5"/>
  </services>
  <energySystemInformation xsi:type="esdl:EnergySystemInformation" id="e0b645e9-636b-4f6b-aed7-caccd1da5429">
    <carriers xsi:type="esdl:Carriers" id="ac7c8cc2-a143-4edd-9d51-64ddb759661b">
      <carrier xsi:type="esdl:ElectricityCommodity" name="Electricity" id="5c1d7e2d-54e8-42b6-983d-5b804c6c6094" voltage="380.0"/>
      <carrier xsi:type="esdl:GasCommodity" name="Gas" id="d1e3e04a-29a4-4297-8034-9c955471e42a"/>
    </carriers>
    <quantityAndUnits xsi:type="esdl:QuantityAndUnits" id="cebca2c4-c336-4a0f-96f3-43ebeeea1083">
      <quantityAndUnit xsi:type="esdl:QuantityAndUnitType" multiplier="KILO" id="12c481c0-f81e-49b6-9767-90457684d24a" unit="WATTHOUR" description="Energy in kWh" physicalQuantity="ENERGY"/>
    </quantityAndUnits>
  </energySystemInformation>
</esdl:EnergySystem>
