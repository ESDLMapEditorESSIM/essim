<?xml version='1.0' encoding='UTF-8'?>
<esdl:EnergySystem xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:esdl="http://www.tno.nl/esdl" name="New Energy System" version="3" id="112a9061-8513-42de-a45a-9fc0cc79ed8e" description="" esdlVersion="v2401">
  <instance xsi:type="esdl:Instance" id="cd100375-0ef4-4dbd-b902-cd6e9f1e720a" name="Untitled instance">
    <area xsi:type="esdl:Area" id="fc1f4ded-5125-4254-8be9-9f2e7d55f2c6" name="Untitled area">
      <asset xsi:type="esdl:ElectricityDemand" id="1aa96ef7-0500-4316-850e-50f1bb43e9ca" name="ElectricityDemand_1aa9">
        <geometry xsi:type="esdl:Point" lat="52.17778719197867" lon="5.154926776885987" CRS="WGS84"/>
        <port xsi:type="esdl:InPort" carrier="69ce1f2e-26c9-4acc-b43c-1a21f5d855ac" name="In" connectedTo="9e12bac9-4de3-4e4f-8386-64c3b99231d4" id="af8acc7b-ff26-4910-a4ad-c9f431abb931">
          <profile xsi:type="esdl:TimeSeriesProfile" startDateTime="2019-01-01T00:00:00.000000" values="120.0 0.0 10.0 20.0 30.0 50.0 70.0 120.0 30.0 0.0 10.0 0.0" id="e9e2a3df-c23c-4bb7-ac86-7342157af729" name="E Demand profile">
            <profileQuantityAndUnit xsi:type="esdl:QuantityAndUnitReference" reference="e9405fc8-5e57-4df5-8584-4babee7cdf1a"/>
          </profile>
        </port>
      </asset>
      <asset xsi:type="esdl:Import" id="a5d62c0a-55e5-4d3b-98bd-fd88d65b5978" power="1000000.0" name="Import_a5d6">
        <geometry xsi:type="esdl:Point" lat="52.1778398244707" lon="5.160505771636964" CRS="WGS84"/>
        <costInformation xsi:type="esdl:CostInformation" id="6c41a6e4-22d1-4663-b40e-67570755592b">
          <marginalCosts xsi:type="esdl:SingleValue" id="cf33baa5-39c9-4bab-a39f-f0ba924acc43" value="0.9"/>
        </costInformation>
        <port xsi:type="esdl:OutPort" carrier="69ce1f2e-26c9-4acc-b43c-1a21f5d855ac" name="Out" id="e2969677-5e80-4719-ac01-401a74c501d2" connectedTo="4124914a-ec79-4a4a-8e3f-326b2b1928aa"/>
      </asset>
      <asset xsi:type="esdl:ElectricityCable" id="417d74b3-e6d5-4ce3-b27a-61bbfd2f6ff3" length="460.3" name="ElectricityCable_417d">
        <geometry xsi:type="esdl:Line" CRS="WGS84">
          <point xsi:type="esdl:Point" lat="52.1778398244707" lon="5.160505771636964"/>
          <point xsi:type="esdl:Point" lat="52.17826088216478" lon="5.160623788833619"/>
          <point xsi:type="esdl:Point" lat="52.17849114640528" lon="5.159196853637696"/>
          <point xsi:type="esdl:Point" lat="52.178412198799975" lon="5.156471729278565"/>
          <point xsi:type="esdl:Point" lat="52.17778719197867" lon="5.154926776885987"/>
        </geometry>
        <port xsi:type="esdl:InPort" carrier="69ce1f2e-26c9-4acc-b43c-1a21f5d855ac" name="In" connectedTo="e2969677-5e80-4719-ac01-401a74c501d2 66835188-9fc8-415d-993f-ee0cb270e8a7" id="4124914a-ec79-4a4a-8e3f-326b2b1928aa"/>
        <port xsi:type="esdl:OutPort" carrier="69ce1f2e-26c9-4acc-b43c-1a21f5d855ac" name="Out" id="9e12bac9-4de3-4e4f-8386-64c3b99231d4" connectedTo="af8acc7b-ff26-4910-a4ad-c9f431abb931"/>
      </asset>
      <asset xsi:type="esdl:GenericConversion" id="a3c898ff-7cc7-472c-95c4-b9c531606baf" controlStrategy="84d306a8-7228-4c07-bd67-a43477a31c88" name="GenericConversion_a3c8" power="100000.0">
        <geometry xsi:type="esdl:Point" lat="52.17784311399938" lon="5.159894227981568"/>
        <port xsi:type="esdl:InPort" carrier="685166f4-b025-4ca6-adc4-2d9c26ccd404" name="In" connectedTo="cc189a37-8a7e-480c-8950-79987c0176af" id="2ec2d0db-256b-45d8-a03b-fb1be3350e39"/>
        <port xsi:type="esdl:OutPort" carrier="69ce1f2e-26c9-4acc-b43c-1a21f5d855ac" name="Out" id="66835188-9fc8-415d-993f-ee0cb270e8a7" connectedTo="4124914a-ec79-4a4a-8e3f-326b2b1928aa"/>
      </asset>
      <asset xsi:type="esdl:Import" id="8ee820d0-6247-4415-8b6e-cb662b2ed1df" power="2000000.0" name="Import_8ee8">
        <geometry xsi:type="esdl:Point" lat="52.17754705544348" lon="5.1592880487442025" CRS="WGS84"/>
        <port xsi:type="esdl:OutPort" carrier="685166f4-b025-4ca6-adc4-2d9c26ccd404" name="Out" id="cc189a37-8a7e-480c-8950-79987c0176af" connectedTo="2ec2d0db-256b-45d8-a03b-fb1be3350e39"/>
      </asset>
    </area>
  </instance>
  <services xsi:type="esdl:Services">
    <service xsi:type="esdl:DrivenByDemand" outPort="66835188-9fc8-415d-993f-ee0cb270e8a7" id="84d306a8-7228-4c07-bd67-a43477a31c88" energyAsset="a3c898ff-7cc7-472c-95c4-b9c531606baf" name="DrivenByDemand for GenericConversion_a3c8"/>
  </services>
  <energySystemInformation xsi:type="esdl:EnergySystemInformation" id="792b6e40-1205-4d37-9140-08d8aca3337d">
    <carriers xsi:type="esdl:Carriers" id="37e1a66d-0190-4a31-974f-08ccbfcb2407">
      <carrier xsi:type="esdl:ElectricityCommodity" id="69ce1f2e-26c9-4acc-b43c-1a21f5d855ac" voltage="24000.0" name="24KV"/>
      <carrier xsi:type="esdl:GasCommodity" id="685166f4-b025-4ca6-adc4-2d9c26ccd404" name="Gas"/>
    </carriers>
    <quantityAndUnits xsi:type="esdl:QuantityAndUnits" id="1e415fbe-44ba-42f5-b537-aadba2fde17c">
      <quantityAndUnit xsi:type="esdl:QuantityAndUnitType" unit="WATT" id="e9405fc8-5e57-4df5-8584-4babee7cdf1a" description="Power in kW" physicalQuantity="POWER" multiplier="KILO"/>
    </quantityAndUnits>
  </energySystemInformation>
</esdl:EnergySystem>
