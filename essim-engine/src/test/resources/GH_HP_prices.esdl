<?xml version='1.0' encoding='UTF-8'?>
<esdl:EnergySystem xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:esdl="http://www.tno.nl/esdl" esdlVersion="v2102" version="15" id="3abd9d1f-00bf-4781-ac32-f66f20f221cd">
  <instance xsi:type="esdl:Instance" id="3d42e2e5-fc47-4462-bbd2-dfeee98a9109">
    <area xsi:type="esdl:Area" id="Area1">
      <asset xsi:type="esdl:Building" floorArea="90.0" owner="ECONOMY_SENSITIVE" id="bld1" name="Building34">
        <buildinginformation xsi:type="esdl:BuildingStructureInformation" flatRoofArea="17.3" uWindow="1.8" slantedRoofArea="90.3" wallArea="131.1" rcFloor="3.76" ventilationType="MECHANIC_INOUT" rcRoof="6.17" rcWall="4.67" height="2.5" windowArea="28.4"/>
        <asset xsi:type="esdl:HeatingDemand" power="50000.0" id="HD1" name="HeatingDemand">
          <geometry xsi:type="esdl:Point" CRS="Simple" lat="250.0" lon="333.3333333333333"/>
          <port xsi:type="esdl:InPort" name="In" id="14338308-94c5-4a3f-80ea-4b33134ef3ca" connectedTo="HPHOutPort GHHOutPort" carrier="3e97e6f5-d59f-455d-b430-613a4804c66a">
            <profile xsi:type="esdl:InfluxDBProfile" database="energy_profiles" endDate="2019-12-31T22:00:00.000000+0000" multiplier="50.0" startDate="2018-12-31T23:00:00.000000+0000" filters="" id="26332cd8-87bc-42cd-b1e0-032709783ba2" port="8086" measurement="standard_profiles" host="http://influxdb" field="G1A">
              <profileQuantityAndUnit xsi:type="esdl:QuantityAndUnitReference" reference="eb07bccb-203f-407e-af98-e687656a221d"/>
            </profile>
          </port>
        </asset>
        <asset xsi:type="esdl:HeatPump" COP="0.01" power="1000000.0" efficiency="0.1" id="HP1" name="Heatpump_9f15">
          <geometry xsi:type="esdl:Point" lat="204.0" lon="179.0"/>
          <port xsi:type="esdl:InPort" name="EInPort" id="EInPort" connectedTo="95653b27-e5bc-4a0d-b018-70387a3a3d94" carrier="8a54ae3b-4eca-4026-8886-885c57f6ba0c"/>
          <port xsi:type="esdl:OutPort" name="HOutPort" id="HPHOutPort" connectedTo="14338308-94c5-4a3f-80ea-4b33134ef3ca" carrier="3e97e6f5-d59f-455d-b430-613a4804c66a"/>
        </asset>
        <asset xsi:type="esdl:GasHeater" power="1000000.0" efficiency="1.0" id="GH1" name="GasHeater_9f15">
          <geometry xsi:type="esdl:Point" lat="304.0" lon="179.0"/>
          <port xsi:type="esdl:InPort" name="GInPort" id="GInPort" connectedTo="36600722-3c05-4e9a-810b-9c461e323d14" carrier="b41530e3-0757-476f-a99f-b2fffee39bef"/>
          <port xsi:type="esdl:OutPort" name="HOutPort" id="GHHOutPort" connectedTo="14338308-94c5-4a3f-80ea-4b33134ef3ca" carrier="3e97e6f5-d59f-455d-b430-613a4804c66a"/>
        </asset>
        <buildingUsage xsi:type="esdl:BuildingUsage" name="BuildingUsage" id="0bcf71a3-12b0-4d55-a775-65783ddfa137">
          <heatingSetpoints xsi:type="esdl:WeekSchedule">
            <weekdays xsi:type="esdl:DaySchedule">
              <event xsi:type="esdl:Event" value="19.0" id="014fa884-48c7-439b-a5a1-91002bf19ba0" time="00:00"/>
            </weekdays>
            <weekenddays xsi:type="esdl:DaySchedule">
              <event xsi:type="esdl:Event" value="19.0" id="014fa884-48c7-439b-a5a1-91002bf19ba1" time="00:00"/>
            </weekenddays>
          </heatingSetpoints>
        </buildingUsage>
        <geometry xsi:type="esdl:Point" lat="52.04041977595355" lon="4.896634950793306"/>
      </asset>
      <asset xsi:type="esdl:Import" power="1000000.0" id="IMP1" name="ElectricityImport">
        <geometry xsi:type="esdl:Point" lat="52.04082442678148" lon="4.894763231277467"/>
        <port xsi:type="esdl:OutPort" name="Out" id="95653b27-e5bc-4a0d-b018-70387a3a3d94" connectedTo="EInPort" carrier="8a54ae3b-4eca-4026-8886-885c57f6ba0c"/>
      </asset>
      <asset xsi:type="esdl:Import" power="1000000.0" id="c50c115a-82bd-4ea7-9399-56b1b68a83bc" name="GasImport">
        <geometry xsi:type="esdl:Point" CRS="WGS84" lat="52.04013149434364" lon="4.89467740058899"/>
        <port xsi:type="esdl:OutPort" name="Out" id="36600722-3c05-4e9a-810b-9c461e323d14" connectedTo="GInPort" carrier="b41530e3-0757-476f-a99f-b2fffee39bef"/>
      </asset>
    </area>
  </instance>
  <energySystemInformation xsi:type="esdl:EnergySystemInformation" id="536a10b7-bc51-490a-882f-7f8d3d81f28b">
    <carriers xsi:type="esdl:Carriers" id="3ebc0399-bcf9-447d-8392-5151854a7dec">
      <carrier xsi:type="esdl:EnergyCarrier" name="Gas" id="b41530e3-0757-476f-a99f-b2fffee39bef">
        <cost xsi:type="esdl:InfluxDBProfile" database="energy_profiles" endDate="2019-12-31T22:00:00.000000+0000" startDate="2018-12-31T23:00:00.000000+0000" filters="" id="36332cd8-87bc-42cd-b1e0-032709783ba2" port="8086" measurement="price_profiles" host="http://influxdb" field="consumer_g_price_EUR_per_m3">
          <profileQuantityAndUnit xsi:type="esdl:QuantityAndUnitType" unit="EURO" id="eb07bccb-203f-407e-af98-e687656a221d" physicalQuantity="COST" description="COST in EUR"/>
        </cost>
      </carrier>
      <carrier xsi:type="esdl:HeatCommodity" name="Heat" id="3e97e6f5-d59f-455d-b430-613a4804c66a"/>
      <carrier xsi:type="esdl:EnergyCarrier" name="Electricity" id="8a54ae3b-4eca-4026-8886-885c57f6ba0c">
        <cost xsi:type="esdl:InfluxDBProfile" database="energy_profiles" endDate="2019-12-31T22:00:00.000000+0000" startDate="2018-12-31T23:00:00.000000+0000" filters="" id="36332cd8-87bc-42cd-b1e0-032709783ba2" port="8086" measurement="price_profiles" host="http://influxdb" field="consumer_e_price_EUR_per_kWh">
          <profileQuantityAndUnit xsi:type="esdl:QuantityAndUnitType" unit="EURO" id="fb07bccb-203f-407e-af98-e687656a221d" physicalQuantity="COST" description="COST in EUR"/>
        </cost>
      </carrier>
    </carriers>
    <quantityAndUnits xsi:type="esdl:QuantityAndUnits" id="34b07d4c-7869-4abd-858b-60d95bb23b67">
      <quantityAndUnit xsi:type="esdl:QuantityAndUnitType" multiplier="GIGA" physicalQuantity="ENERGY" id="eb07bccb-203f-407e-af98-e687656a221d" description="ENERGY in GJ" unit="JOULE"/>
    </quantityAndUnits>
    <environmentalProfiles xsi:type="esdl:EnvironmentalProfiles" id="591a7827-55c3-40b1-9250-e3e2f05463b5">
      <solarIrradianceProfile xsi:type="esdl:InfluxDBProfile" database="energy_profiles" endDate="2020-01-01T00:00:00.000000" startDate="2019-01-01T00:00:00.000000" id="3e9e773e-2a84-4b8f-ab7f-1068bc5663fa" port="8086" name="ConstantSolarIrradiance" measurement="weer_2011-2020" host="http://localhost" field="west_cloud"/>
      <outsideTemperatureProfile xsi:type="esdl:InfluxDBProfile" database="energy_profiles" endDate="2020-01-01T00:00:00.000000" startDate="2019-01-01T00:00:00.000000" id="a1887a65-903a-4f1a-bfbc-db6052335c07" port="8086" name="Ambient Temperature" measurement="weer_2011-2020" host="http://localhost" field="temperature">
        <profileQuantityAndUnit xsi:type="esdl:QuantityAndUnitType" id="d0feb754-9deb-4def-b772-8877e8f50d0b" physicalQuantity="TEMPERATURE" unit="DEGREES_CELSIUS"/>
      </outsideTemperatureProfile>
    </environmentalProfiles>
  </energySystemInformation>
</esdl:EnergySystem>
