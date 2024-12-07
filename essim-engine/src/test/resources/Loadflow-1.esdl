<?xml version='1.0' encoding='UTF-8'?>
<esdl:EnergySystem xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:esdl="http://www.tno.nl/esdl" esdlVersion="v2401" name="New Energy System" version="1" id="112a9061-8513-42de-a45a-9fc0cc79ed8e" description="">
  <energySystemInformation xsi:type="esdl:EnergySystemInformation" id="792b6e40-1205-4d37-9140-08d8aca3337d">
    <carriers xsi:type="esdl:Carriers" id="37e1a66d-0190-4a31-974f-08ccbfcb2407">
      <carrier xsi:type="esdl:ElectricityCommodity" id="69ce1f2e-26c9-4acc-b43c-1a21f5d855ac" name="24KV" voltage="24000.0"/>
    </carriers>
    <quantityAndUnits xsi:type="esdl:QuantityAndUnits" id="1e415fbe-44ba-42f5-b537-aadba2fde17c">
      <quantityAndUnit xsi:type="esdl:QuantityAndUnitType" id="e9405fc8-5e57-4df5-8584-4babee7cdf1a" multiplier="KILO" physicalQuantity="POWER" description="Power in kW" unit="WATT"/>
    </quantityAndUnits>
  </energySystemInformation>
  <instance xsi:type="esdl:Instance" name="Untitled instance" id="cd100375-0ef4-4dbd-b902-cd6e9f1e720a">
    <area xsi:type="esdl:Area" id="fc1f4ded-5125-4254-8be9-9f2e7d55f2c6" name="Untitled area">
      <asset xsi:type="esdl:ElectricityDemand" name="ElectricityDemand_1aa9" id="1aa96ef7-0500-4316-850e-50f1bb43e9ca">
        <geometry xsi:type="esdl:Point" CRS="WGS84" lon="5.154926776885987" lat="52.17778719197867"/>
        <port xsi:type="esdl:InPort" id="af8acc7b-ff26-4910-a4ad-c9f431abb931" carrier="69ce1f2e-26c9-4acc-b43c-1a21f5d855ac" connectedTo="9e12bac9-4de3-4e4f-8386-64c3b99231d4" name="In">
          <profile xsi:type="esdl:InfluxDBProfile" port="8086" startDate="2018-12-31T23:00:00.000000+0000" id="921c8133-1ebc-4d34-9c60-e0d05367f212" field="E1A" measurement="standard_profiles" database="energy_profiles" endDate="2019-12-31T22:00:00.000000+0000" multiplier="10000.0" filters="" host="http://influxdb">
            <profileQuantityAndUnit xsi:type="esdl:QuantityAndUnitReference" reference="e9405fc8-5e57-4df5-8584-4babee7cdf1a"/>
          </profile>
        </port>
      </asset>
      <asset xsi:type="esdl:Import" name="Import_a5d6" power="1000000.0" id="a5d62c0a-55e5-4d3b-98bd-fd88d65b5978">
        <geometry xsi:type="esdl:Point" CRS="WGS84" lon="5.160505771636964" lat="52.1778398244707"/>
        <port xsi:type="esdl:OutPort" id="e2969677-5e80-4719-ac01-401a74c501d2" connectedTo="4124914a-ec79-4a4a-8e3f-326b2b1928aa" carrier="69ce1f2e-26c9-4acc-b43c-1a21f5d855ac" name="Out"/>
      </asset>
      <asset xsi:type="esdl:ElectricityCable" name="ElectricityCable_417d" length="460.3" id="417d74b3-e6d5-4ce3-b27a-61bbfd2f6ff3">
        <geometry xsi:type="esdl:Line" CRS="WGS84">
          <point xsi:type="esdl:Point" lon="5.160505771636964" lat="52.1778398244707"/>
          <point xsi:type="esdl:Point" lon="5.160623788833619" lat="52.17826088216478"/>
          <point xsi:type="esdl:Point" lon="5.159196853637696" lat="52.17849114640528"/>
          <point xsi:type="esdl:Point" lon="5.156471729278565" lat="52.178412198799975"/>
          <point xsi:type="esdl:Point" lon="5.154926776885987" lat="52.17778719197867"/>
        </geometry>
        <port xsi:type="esdl:InPort" id="4124914a-ec79-4a4a-8e3f-326b2b1928aa" carrier="69ce1f2e-26c9-4acc-b43c-1a21f5d855ac" connectedTo="e2969677-5e80-4719-ac01-401a74c501d2" name="In"/>
        <port xsi:type="esdl:OutPort" id="9e12bac9-4de3-4e4f-8386-64c3b99231d4" connectedTo="af8acc7b-ff26-4910-a4ad-c9f431abb931" carrier="69ce1f2e-26c9-4acc-b43c-1a21f5d855ac" name="Out"/>
      </asset>
    </area>
  </instance>
</esdl:EnergySystem>
