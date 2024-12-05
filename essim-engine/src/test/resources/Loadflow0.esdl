<?xml version='1.0' encoding='UTF-8'?>
<esdl:EnergySystem xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:esdl="http://www.tno.nl/esdl" esdlVersion="v2401" name="Untitled EnergySystem" version="14" id="e6502739-88a9-407d-854f-ff143df0e492" description="">
  <energySystemInformation xsi:type="esdl:EnergySystemInformation" id="312a60e4-b66e-4c39-adec-61c3980277f6">
    <carriers xsi:type="esdl:Carriers" id="5eacce9a-7a91-49a3-b8b0-130419f8fa0a">
      <carrier xsi:type="esdl:ElectricityCommodity" voltage="20000.0" name="MS" id="ca9149c7-2a72-43ec-a481-5c81d619b8b1"/>
    </carriers>
    <quantityAndUnits xsi:type="esdl:QuantityAndUnits" id="960ddf7e-29ae-4c94-a0ab-f43649794ad8">
      <quantityAndUnit xsi:type="esdl:QuantityAndUnitType" unit="JOULE" id="eb07bccb-203f-407e-af98-e687656a221d" description="Energy in GJ" physicalQuantity="ENERGY" multiplier="GIGA"/>
      <quantityAndUnit xsi:type="esdl:QuantityAndUnitType" unit="WATTHOUR" id="12c481c0-f81e-49b6-9767-90457684d24a" description="Energy in kWh" physicalQuantity="ENERGY" multiplier="KILO"/>
    </quantityAndUnits>
  </energySystemInformation>
  <instance xsi:type="esdl:Instance" id="e2d85023-2612-4689-87fa-45e361dd41c0" name="Untitled Instance">
    <area xsi:type="esdl:Area" name="Untitled Area" id="ba255053-43cd-498b-8ad9-d13ae03cd581">
      <asset xsi:type="esdl:EVChargingStation" id="a1a602fe-ec9b-4807-9fb6-3a4e87941ae0" name="Laadplein busstation">
        <port xsi:type="esdl:InPort" connectedTo="213606b5-6bb7-408e-8d86-f1c9243850ba" id="0c619f3d-03c0-493a-bf2b-4ee10aa0e29f" carrier="ca9149c7-2a72-43ec-a481-5c81d619b8b1" name="In">
          <profile xsi:type="esdl:InfluxDBProfile" port="8086" host="http://geis" filters="" startDate="2015-01-01T00:00:00.000000+0100" id="6316db08-60e5-45d6-a8e2-09f6df3ded12" database="energy_profiles" field="E1B" measurement="nedu_elektriciteit_2015-2018" endDate="2016-01-01T00:00:00.000000+0100" multiplier="8760.0">
            <profileQuantityAndUnit xsi:type="esdl:QuantityAndUnitReference" reference="eb07bccb-203f-407e-af98-e687656a221d"/>
          </profile>
        </port>
        <geometry xsi:type="esdl:Point" lon="5.497713088989258" lat="51.446010237625224"/>
      </asset>
      <asset xsi:type="esdl:EConnection" id="04f74257-cf70-4b6c-9cd6-93cd5939965e" name="EConnection_04f7">
        <port xsi:type="esdl:InPort" connectedTo="a80634c4-36a5-4a0e-893b-b9cc0b993ad7" id="2931a9ba-7815-43de-a408-e2762dc392b1" carrier="ca9149c7-2a72-43ec-a481-5c81d619b8b1" name="In"/>
        <port xsi:type="esdl:OutPort" id="213606b5-6bb7-408e-8d86-f1c9243850ba" connectedTo="0c619f3d-03c0-493a-bf2b-4ee10aa0e29f 85d417c8-e69b-4562-a63e-92de3b1953f5" carrier="ca9149c7-2a72-43ec-a481-5c81d619b8b1" name="Out"/>
        <geometry xsi:type="esdl:Point" lon="5.4980671405792245" lat="51.4462175269164" CRS="WGS84"/>
      </asset>
      <asset xsi:type="esdl:ElectricityCable" length="155.5" id="9c9f7410-06fc-4df2-95fc-e3de9a227f5d" name="ElectricityCable_9c9f">
        <port xsi:type="esdl:InPort" connectedTo="99ba81a5-3d32-4666-b2b8-56a3aa47819b" id="e88f4f46-ddf2-4d9f-8885-af1f534b113c" carrier="ca9149c7-2a72-43ec-a481-5c81d619b8b1" name="In"/>
        <port xsi:type="esdl:OutPort" id="a80634c4-36a5-4a0e-893b-b9cc0b993ad7" connectedTo="2931a9ba-7815-43de-a408-e2762dc392b1" carrier="ca9149c7-2a72-43ec-a481-5c81d619b8b1" name="Out"/>
        <geometry xsi:type="esdl:Line">
          <point xsi:type="esdl:Point" lon="5.49599513411522" lat="51.44578915587244"/>
          <point xsi:type="esdl:Point" lon="5.496511459350586" lat="51.446020267773896"/>
          <point xsi:type="esdl:Point" lon="5.49768626689911" lat="51.44623758712043"/>
          <point xsi:type="esdl:Point" lon="5.4980671405792245" lat="51.4462175269164"/>
        </geometry>
      </asset>
      <asset xsi:type="esdl:Battery" capacity="24000000000.0" id="e297277d-e002-45de-bc2f-9b13dcd94c12" controlStrategy="af062f20-b3a9-4480-9bfe-6a0737830663" maxChargeRate="500000.0" maxDischargeRate="500000.0" description="6.8 MWh buurtbatterij is ongeveer 24 GJ" name="Batterij busstation">
        <port xsi:type="esdl:InPort" connectedTo="213606b5-6bb7-408e-8d86-f1c9243850ba" id="85d417c8-e69b-4562-a63e-92de3b1953f5" carrier="ca9149c7-2a72-43ec-a481-5c81d619b8b1" name="In"/>
        <geometry xsi:type="esdl:Point" lon="5.4981958866119385" lat="51.44594002652293"/>
      </asset>
      <asset xsi:type="esdl:ElectricityDemand" id="561715ea-e0f0-46c1-b3ff-244c2e54ce33" name="Campus Demand">
        <port xsi:type="esdl:InPort" connectedTo="0576c163-d1cd-4037-976a-d47643c0da0f" id="4f0c6404-b231-4eb3-8057-7f92a9fdf1ab" carrier="ca9149c7-2a72-43ec-a481-5c81d619b8b1" name="In">
          <profile xsi:type="esdl:InfluxDBProfile" port="8086" host="http://geis" filters="" startDate="2015-01-01T00:00:00.000000+0100" id="f26f3aab-870b-48e0-a4da-3f40accead47" database="energy_profiles" field="E3A" measurement="nedu_elektriciteit_2015-2018" endDate="2016-01-01T00:00:00.000000+0100" multiplier="3000.0">
            <profileQuantityAndUnit xsi:type="esdl:QuantityAndUnitReference" reference="eb07bccb-203f-407e-af98-e687656a221d"/>
          </profile>
        </port>
        <geometry xsi:type="esdl:Point" lon="5.49091100692749" lat="51.4479493587373" CRS="WGS84"/>
      </asset>
      <asset xsi:type="esdl:ElectricityDemand" id="dc538be8-b532-4a14-848d-a170fd39d879" name="Woningen">
        <port xsi:type="esdl:InPort" connectedTo="99c86bed-bc03-4f1c-8e40-553260487c80" id="06b82ec8-ab5c-4eab-98be-020dd3cebfca" carrier="ca9149c7-2a72-43ec-a481-5c81d619b8b1" name="In">
          <profile xsi:type="esdl:InfluxDBProfile" port="8086" host="http://geis" filters="" startDate="2015-01-01T00:00:00.000000+0100" id="0699ab7e-eb26-40d7-8ae8-5eca553ddc73" database="energy_profiles" field="E1A" measurement="nedu_elektriciteit_2015-2018" endDate="2016-01-01T00:00:00.000000+0100" multiplier="5000.0">
            <profileQuantityAndUnit xsi:type="esdl:QuantityAndUnitReference" reference="eb07bccb-203f-407e-af98-e687656a221d"/>
          </profile>
        </port>
        <geometry xsi:type="esdl:Point" lon="5.476641654968262" lat="51.447962731700436"/>
      </asset>
      <asset xsi:type="esdl:ElectricityCable" length="702.6" id="e88f3f70-ac3c-47ad-8514-0dcbf47460ea" name="ElectricityCable_e88f">
        <port xsi:type="esdl:InPort" connectedTo="99ba81a5-3d32-4666-b2b8-56a3aa47819b" id="43e04703-f56b-4ef7-9ed3-abfe5f11e4f6" carrier="ca9149c7-2a72-43ec-a481-5c81d619b8b1" name="In"/>
        <port xsi:type="esdl:OutPort" id="0576c163-d1cd-4037-976a-d47643c0da0f" connectedTo="4f0c6404-b231-4eb3-8057-7f92a9fdf1ab" carrier="ca9149c7-2a72-43ec-a481-5c81d619b8b1" name="Out"/>
        <geometry xsi:type="esdl:Line">
          <point xsi:type="esdl:Point" lon="5.495966970920563" lat="51.44579709644544"/>
          <point xsi:type="esdl:Point" lon="5.495781898498536" lat="51.446117225763984"/>
          <point xsi:type="esdl:Point" lon="5.490213632583619" lat="51.445759483192774"/>
          <point xsi:type="esdl:Point" lon="5.489988327026367" lat="51.44753479493761"/>
          <point xsi:type="esdl:Point" lon="5.49091100692749" lat="51.4479493587373"/>
        </geometry>
      </asset>
      <asset xsi:type="esdl:ElectricityCable" length="1788.8" id="551ab14f-b17b-480a-b048-87f38541f68e" name="ElectricityCable_551a">
        <port xsi:type="esdl:InPort" connectedTo="99ba81a5-3d32-4666-b2b8-56a3aa47819b" id="81a5b223-8031-432b-b8bd-1d04d2a33214" carrier="ca9149c7-2a72-43ec-a481-5c81d619b8b1" name="In"/>
        <port xsi:type="esdl:OutPort" id="99c86bed-bc03-4f1c-8e40-553260487c80" connectedTo="06b82ec8-ab5c-4eab-98be-020dd3cebfca" carrier="ca9149c7-2a72-43ec-a481-5c81d619b8b1" name="Out"/>
        <geometry xsi:type="esdl:Line">
          <point xsi:type="esdl:Point" lon="5.495958253741265" lat="51.44579333512155"/>
          <point xsi:type="esdl:Point" lon="5.49567461013794" lat="51.44599686409025"/>
          <point xsi:type="esdl:Point" lon="5.481126308441163" lat="51.44506070690928"/>
          <point xsi:type="esdl:Point" lon="5.476491451263428" lat="51.444285019277444"/>
          <point xsi:type="esdl:Point" lon="5.476233959197999" lat="51.44483335155461"/>
          <point xsi:type="esdl:Point" lon="5.476641654968262" lat="51.447962731700436"/>
        </geometry>
      </asset>
      <asset xsi:type="esdl:PVPark" surfaceArea="7522" originalIdInSource="energy_power_solar_pv_solar_radiation" panelEfficiency="0.17" id="cca4f609-f9b3-4a18-9114-41dcd55f8657" power="2256600.0" technicalLifetime="25.0" name="PVPark_cca4">
        <costInformation xsi:type="esdl:CostInformation">
          <investmentCosts xsi:type="esdl:SingleValue" value="17420000.0">
            <profileQuantityAndUnit xsi:type="esdl:QuantityAndUnitType" unit="EURO" physicalQuantity="COST" description="EUR"/>
          </investmentCosts>
          <fixedOperationalAndMaintenanceCosts xsi:type="esdl:SingleValue" value="318000.0">
            <profileQuantityAndUnit xsi:type="esdl:QuantityAndUnitType" unit="EURO" perTimeUnit="YEAR" physicalQuantity="COST" description="EUR / year"/>
          </fixedOperationalAndMaintenanceCosts>
        </costInformation>
        <port xsi:type="esdl:OutPort" id="220ede13-6dba-4d7b-98e8-3cbffce77245" connectedTo="dd11af71-d8d3-4af7-bb02-b125fca4a415" carrier="ca9149c7-2a72-43ec-a481-5c81d619b8b1" name="Out">
          <profile xsi:type="esdl:InfluxDBProfile" port="8086" host="http://geis" filters="" startDate="2015-01-01T00:00:00.000000+0100" id="089dd6ef-c0be-40d1-83ac-7de85829d04f" database="energy_profiles" field="value" measurement="solar_relative_2011-2016" endDate="2016-01-01T00:00:00.000000+0100" multiplier="1912500.0">
            <profileQuantityAndUnit xsi:type="esdl:QuantityAndUnitReference" reference="12c481c0-f81e-49b6-9767-90457684d24a"/>
          </profile>
        </port>
        <geometry xsi:type="esdl:Polygon" CRS="WGS84">
          <exterior xsi:type="esdl:SubPolygon">
            <point xsi:type="esdl:Point" lon="5.496554374694824" lat="51.4469497186546"/>
            <point xsi:type="esdl:Point" lon="5.49668312072754" lat="51.44657192255712"/>
            <point xsi:type="esdl:Point" lon="5.499102473258973" lat="51.447274019006095"/>
            <point xsi:type="esdl:Point" lon="5.498898625373841" lat="51.44761837665177"/>
          </exterior>
        </geometry>
        <dataSource xsi:type="esdl:DataSource" reference="https://github.com/quintel/etdataset-public/blob/master/nodes_source_analyses/energy/energy/energy_chp_local_engine_biogas.xlsx" name="ETM"/>
      </asset>
      <asset xsi:type="esdl:PVPark" surfaceArea="200000" originalIdInSource="energy_power_solar_pv_solar_radiation" panelEfficiency="0.17" id="3a9576ab-21a7-422b-9b59-1c321b1092a1" power="20000000.0" technicalLifetime="25.0" name="ElectricityCable_3a95">
        <costInformation xsi:type="esdl:CostInformation">
          <investmentCosts xsi:type="esdl:SingleValue" value="17420000.0">
            <profileQuantityAndUnit xsi:type="esdl:QuantityAndUnitType" unit="EURO" physicalQuantity="COST" description="EUR"/>
          </investmentCosts>
          <fixedOperationalAndMaintenanceCosts xsi:type="esdl:SingleValue" value="318000.0">
            <profileQuantityAndUnit xsi:type="esdl:QuantityAndUnitType" unit="EURO" perTimeUnit="YEAR" physicalQuantity="COST" description="EUR / year"/>
          </fixedOperationalAndMaintenanceCosts>
        </costInformation>
        <port xsi:type="esdl:OutPort" name="Out" id="1522a888-88c6-4c3f-b729-f0ee94cc841b"/>
        <geometry xsi:type="esdl:Line">
          <point xsi:type="esdl:Point" lon="5.497804372736596" lat="51.4471008477508"/>
          <point xsi:type="esdl:Point" lon="5.496093034744264" lat="51.44666887937588"/>
          <point xsi:type="esdl:Point" lon="5.496019944548608" lat="51.44583387381805"/>
        </geometry>
        <dataSource xsi:type="esdl:DataSource" reference="https://github.com/quintel/etdataset-public/blob/master/nodes_source_analyses/energy/energy/energy_chp_local_engine_biogas.xlsx" name="ETM"/>
      </asset>
      <asset xsi:type="esdl:ElectricityCable" length="228.6" id="74749415-f070-40ae-bf0d-53339f971e89" name="ElectricityCable_7474">
        <port xsi:type="esdl:InPort" connectedTo="220ede13-6dba-4d7b-98e8-3cbffce77245" id="dd11af71-d8d3-4af7-bb02-b125fca4a415" carrier="ca9149c7-2a72-43ec-a481-5c81d619b8b1" name="In"/>
        <port xsi:type="esdl:OutPort" id="ef76b1e5-1eca-4aa2-8e3f-6aed79134bab" connectedTo="35ee69ac-50c5-4042-88bc-3b2a56987948" carrier="ca9149c7-2a72-43ec-a481-5c81d619b8b1" name="Out"/>
        <geometry xsi:type="esdl:Line">
          <point xsi:type="esdl:Point" lon="5.497804372736596" lat="51.4471008477508"/>
          <point xsi:type="esdl:Point" lon="5.495985746383668" lat="51.44661872932291"/>
          <point xsi:type="esdl:Point" lon="5.495985075831414" lat="51.44579458889623"/>
        </geometry>
      </asset>
      <asset xsi:type="esdl:Bus" id="fdbfaaa1-e82b-4adb-90de-289bbb5d9e56" name="Bus_fdbf">
        <port xsi:type="esdl:InPort" connectedTo="ef76b1e5-1eca-4aa2-8e3f-6aed79134bab 27234ec3-d308-455e-80a8-ff17f552bc27" id="35ee69ac-50c5-4042-88bc-3b2a56987948" carrier="ca9149c7-2a72-43ec-a481-5c81d619b8b1" name="In"/>
        <port xsi:type="esdl:OutPort" id="99ba81a5-3d32-4666-b2b8-56a3aa47819b" connectedTo="e88f4f46-ddf2-4d9f-8885-af1f534b113c 43e04703-f56b-4ef7-9ed3-abfe5f11e4f6 81a5b223-8031-432b-b8bd-1d04d2a33214" carrier="ca9149c7-2a72-43ec-a481-5c81d619b8b1" name="Out"/>
        <geometry xsi:type="esdl:Point" lon="5.4959575831890115" lat="51.44577202094711"/>
      </asset>
      <asset xsi:type="esdl:Import" id="d685bf68-b2a4-42f4-b199-32389134b669" power="1000000.0" controlStrategy="772ce384-7b87-4bd9-a59e-78643ac63743" name="Import_d685">
        <port xsi:type="esdl:OutPort" id="27234ec3-d308-455e-80a8-ff17f552bc27" connectedTo="35ee69ac-50c5-4042-88bc-3b2a56987948" carrier="ca9149c7-2a72-43ec-a481-5c81d619b8b1" name="Out"/>
        <geometry xsi:type="esdl:Point" lon="5.496892333030702" lat="51.44556222207159"/>
      </asset>
    </area>
  </instance>
  <services xsi:type="esdl:Services" id="3ad92844-1093-4573-b31d-a7ff886455c7">
    <service xsi:type="esdl:StorageStrategy" energyAsset="e297277d-e002-45de-bc2f-9b13dcd94c12" id="af062f20-b3a9-4480-9bfe-6a0737830663" name="StorageStrategy for Batterij busstation">
      <marginalChargeCosts xsi:type="esdl:SingleValue" value="0.2" name="marginalChargeCosts for Batterij busstation" id="99be13cb-4a99-4834-bf72-2ebe426a9bd8"/>
      <marginalDischargeCosts xsi:type="esdl:SingleValue" value="0.8" name="marginalChargeCosts for Batterij busstation" id="e739c523-7d59-44ab-a1d1-55e53d1f4738"/>
    </service>
    <service xsi:type="esdl:DrivenByDemand" outPort="27234ec3-d308-455e-80a8-ff17f552bc27" name="DrivenByDemand for Import_d685" energyAsset="d685bf68-b2a4-42f4-b199-32389134b669" id="772ce384-7b87-4bd9-a59e-78643ac63743"/>
  </services>
</esdl:EnergySystem>
