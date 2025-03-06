<?xml version='1.0' encoding='UTF-8'?>
<esdl:EnergySystem xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:esdl="http://www.tno.nl/esdl" name="Untitled EnergySystem" version="13" id="e6502739-88a9-407d-854f-ff143df0e492" description="" esdlVersion="v2401">
  <instance xsi:type="esdl:Instance" id="e2d85023-2612-4689-87fa-45e361dd41c0" name="Untitled Instance">
    <area xsi:type="esdl:Area" id="ba255053-43cd-498b-8ad9-d13ae03cd581" name="Untitled Area">
      <asset xsi:type="esdl:EVChargingStation" id="a1a602fe-ec9b-4807-9fb6-3a4e87941ae0" name="Laadplein busstation">
        <geometry xsi:type="esdl:Point" lon="5.497713088989258" lat="51.446010237625224"/>
        <port xsi:type="esdl:InPort" name="In" connectedTo="213606b5-6bb7-408e-8d86-f1c9243850ba" id="0c619f3d-03c0-493a-bf2b-4ee10aa0e29f" carrier="ca9149c7-2a72-43ec-a481-5c81d619b8b1">
          <profile xsi:type="esdl:InfluxDBProfile" port="8086" multiplier="8760.0" endDate="2016-01-01T00:00:00.000000+0100" field="E1B" database="energy_profiles" measurement="nedu_elektriciteit_2015-2018" id="6316db08-60e5-45d6-a8e2-09f6df3ded12" startDate="2015-01-01T00:00:00.000000+0100" filters="" host="http://geis">
            <profileQuantityAndUnit xsi:type="esdl:QuantityAndUnitReference" reference="eb07bccb-203f-407e-af98-e687656a221d"/>
          </profile>
        </port>
      </asset>
      <asset xsi:type="esdl:EConnection" id="04f74257-cf70-4b6c-9cd6-93cd5939965e" name="EConnection_04f7">
        <geometry xsi:type="esdl:Point" lon="5.4980671405792245" CRS="WGS84" lat="51.4462175269164"/>
        <port xsi:type="esdl:InPort" name="In" connectedTo="a80634c4-36a5-4a0e-893b-b9cc0b993ad7" id="2931a9ba-7815-43de-a408-e2762dc392b1" carrier="ca9149c7-2a72-43ec-a481-5c81d619b8b1"/>
        <port xsi:type="esdl:OutPort" name="Out" id="213606b5-6bb7-408e-8d86-f1c9243850ba" connectedTo="0c619f3d-03c0-493a-bf2b-4ee10aa0e29f 85d417c8-e69b-4562-a63e-92de3b1953f5" carrier="ca9149c7-2a72-43ec-a481-5c81d619b8b1"/>
      </asset>
      <asset xsi:type="esdl:Transformer" id="f3005325-7577-473d-a50f-587743cafdb1" name="Transformer_f300" assetType="63 MVA 110/20 kV">
        <geometry xsi:type="esdl:Point" lon="5.496005192399026" lat="51.44577202094711"/>
        <port xsi:type="esdl:InPort" name="PrimIn" connectedTo="777c60a0-9a70-4b80-8333-d751a5d82356" id="ab887267-1ab1-4e7e-9492-d0278d9e2244" carrier="6a9d1250-f539-4ec4-8ee0-b8c0a11a193d"/>
        <port xsi:type="esdl:OutPort" name="SecOut" id="05001546-124f-4a8d-a419-d637342ec24c" connectedTo="35ee69ac-50c5-4042-88bc-3b2a56987948" carrier="ca9149c7-2a72-43ec-a481-5c81d619b8b1"/>
      </asset>
      <asset xsi:type="esdl:ElectricityCable" id="9c9f7410-06fc-4df2-95fc-e3de9a227f5d" length="155.5" name="ElectricityCable_9c9f">
        <geometry xsi:type="esdl:Line">
          <point xsi:type="esdl:Point" lon="5.49599513411522" lat="51.44578915587244"/>
          <point xsi:type="esdl:Point" lon="5.496511459350586" lat="51.446020267773896"/>
          <point xsi:type="esdl:Point" lon="5.49768626689911" lat="51.44623758712043"/>
          <point xsi:type="esdl:Point" lon="5.4980671405792245" lat="51.4462175269164"/>
        </geometry>
        <port xsi:type="esdl:InPort" name="In" connectedTo="99ba81a5-3d32-4666-b2b8-56a3aa47819b" id="e88f4f46-ddf2-4d9f-8885-af1f534b113c" carrier="ca9149c7-2a72-43ec-a481-5c81d619b8b1"/>
        <port xsi:type="esdl:OutPort" name="Out" id="a80634c4-36a5-4a0e-893b-b9cc0b993ad7" connectedTo="2931a9ba-7815-43de-a408-e2762dc392b1" carrier="ca9149c7-2a72-43ec-a481-5c81d619b8b1"/>
      </asset>
      <asset xsi:type="esdl:Battery" id="e297277d-e002-45de-bc2f-9b13dcd94c12" maxDischargeRate="500000.0" controlStrategy="af062f20-b3a9-4480-9bfe-6a0737830663" maxChargeRate="500000.0" name="Batterij busstation" description="6.8 MWh buurtbatterij is ongeveer 24 GJ" capacity="24000000000.0">
        <geometry xsi:type="esdl:Point" lon="5.4981958866119385" lat="51.44594002652293"/>
        <port xsi:type="esdl:InPort" name="In" connectedTo="213606b5-6bb7-408e-8d86-f1c9243850ba" id="85d417c8-e69b-4562-a63e-92de3b1953f5" carrier="ca9149c7-2a72-43ec-a481-5c81d619b8b1"/>
      </asset>
      <asset xsi:type="esdl:ElectricityDemand" id="561715ea-e0f0-46c1-b3ff-244c2e54ce33" name="Campus Demand">
        <geometry xsi:type="esdl:Point" lon="5.49091100692749" CRS="WGS84" lat="51.4479493587373"/>
        <port xsi:type="esdl:InPort" name="In" connectedTo="0576c163-d1cd-4037-976a-d47643c0da0f" id="4f0c6404-b231-4eb3-8057-7f92a9fdf1ab" carrier="ca9149c7-2a72-43ec-a481-5c81d619b8b1">
          <profile xsi:type="esdl:InfluxDBProfile" port="8086" multiplier="3000.0" endDate="2016-01-01T00:00:00.000000+0100" field="E3A" database="energy_profiles" measurement="nedu_elektriciteit_2015-2018" id="f26f3aab-870b-48e0-a4da-3f40accead47" startDate="2015-01-01T00:00:00.000000+0100" filters="" host="http://geis">
            <profileQuantityAndUnit xsi:type="esdl:QuantityAndUnitReference" reference="eb07bccb-203f-407e-af98-e687656a221d"/>
          </profile>
        </port>
      </asset>
      <asset xsi:type="esdl:ElectricityDemand" id="dc538be8-b532-4a14-848d-a170fd39d879" name="Woningen">
        <geometry xsi:type="esdl:Point" lon="5.476641654968262" lat="51.447962731700436"/>
        <port xsi:type="esdl:InPort" name="In" connectedTo="99c86bed-bc03-4f1c-8e40-553260487c80" id="06b82ec8-ab5c-4eab-98be-020dd3cebfca" carrier="ca9149c7-2a72-43ec-a481-5c81d619b8b1">
          <profile xsi:type="esdl:InfluxDBProfile" port="8086" multiplier="5000.0" endDate="2016-01-01T00:00:00.000000+0100" field="E1A" database="energy_profiles" measurement="nedu_elektriciteit_2015-2018" id="0699ab7e-eb26-40d7-8ae8-5eca553ddc73" startDate="2015-01-01T00:00:00.000000+0100" filters="" host="http://geis">
            <profileQuantityAndUnit xsi:type="esdl:QuantityAndUnitReference" reference="eb07bccb-203f-407e-af98-e687656a221d"/>
          </profile>
        </port>
      </asset>
      <asset xsi:type="esdl:ElectricityCable" id="e88f3f70-ac3c-47ad-8514-0dcbf47460ea" length="702.6" name="ElectricityCable_e88f">
        <geometry xsi:type="esdl:Line">
          <point xsi:type="esdl:Point" lon="5.495966970920563" lat="51.44579709644544"/>
          <point xsi:type="esdl:Point" lon="5.495781898498536" lat="51.446117225763984"/>
          <point xsi:type="esdl:Point" lon="5.490213632583619" lat="51.445759483192774"/>
          <point xsi:type="esdl:Point" lon="5.489988327026367" lat="51.44753479493761"/>
          <point xsi:type="esdl:Point" lon="5.49091100692749" lat="51.4479493587373"/>
        </geometry>
        <port xsi:type="esdl:InPort" name="In" connectedTo="99ba81a5-3d32-4666-b2b8-56a3aa47819b" id="43e04703-f56b-4ef7-9ed3-abfe5f11e4f6" carrier="ca9149c7-2a72-43ec-a481-5c81d619b8b1"/>
        <port xsi:type="esdl:OutPort" name="Out" id="0576c163-d1cd-4037-976a-d47643c0da0f" connectedTo="4f0c6404-b231-4eb3-8057-7f92a9fdf1ab" carrier="ca9149c7-2a72-43ec-a481-5c81d619b8b1"/>
      </asset>
      <asset xsi:type="esdl:ElectricityCable" id="551ab14f-b17b-480a-b048-87f38541f68e" length="1788.8" name="ElectricityCable_551a">
        <geometry xsi:type="esdl:Line">
          <point xsi:type="esdl:Point" lon="5.495958253741265" lat="51.44579333512155"/>
          <point xsi:type="esdl:Point" lon="5.49567461013794" lat="51.44599686409025"/>
          <point xsi:type="esdl:Point" lon="5.481126308441163" lat="51.44506070690928"/>
          <point xsi:type="esdl:Point" lon="5.476491451263428" lat="51.444285019277444"/>
          <point xsi:type="esdl:Point" lon="5.476233959197999" lat="51.44483335155461"/>
          <point xsi:type="esdl:Point" lon="5.476641654968262" lat="51.447962731700436"/>
        </geometry>
        <port xsi:type="esdl:InPort" name="In" connectedTo="99ba81a5-3d32-4666-b2b8-56a3aa47819b" id="81a5b223-8031-432b-b8bd-1d04d2a33214" carrier="ca9149c7-2a72-43ec-a481-5c81d619b8b1"/>
        <port xsi:type="esdl:OutPort" name="Out" id="99c86bed-bc03-4f1c-8e40-553260487c80" connectedTo="06b82ec8-ab5c-4eab-98be-020dd3cebfca" carrier="ca9149c7-2a72-43ec-a481-5c81d619b8b1"/>
      </asset>
      <asset xsi:type="esdl:Import" id="b4ec2317-169c-4e86-87a1-e1fd3398ef68" power="100000000.0" name="Import_b4ec">
        <geometry xsi:type="esdl:Point" lon="5.532713234424592" lat="51.44665216269769"/>
        <costInformation xsi:type="esdl:CostInformation">
          <marginalCosts xsi:type="esdl:SingleValue" name="Import_b4ec-MarginalCosts" value="0.9" id="6a9de46f-def2-469d-8d08-79cbe468755c"/>
        </costInformation>
        <port xsi:type="esdl:OutPort" name="Out" id="361d2ffc-3332-4e01-9319-286faee0e803" connectedTo="51d7727c-2f6d-4658-8e62-4740d856fffb" carrier="6a9d1250-f539-4ec4-8ee0-b8c0a11a193d"/>
      </asset>
      <asset xsi:type="esdl:ElectricityCable" id="78f34828-fd15-4e5b-8584-71dd976f79a0" length="2767.6" name="ElectricityCable_78f3" assetType="N2XS(FL)2Y 1x240 RM/35 64/110 kV">
        <geometry xsi:type="esdl:Line">
          <point xsi:type="esdl:Point" lon="5.5320990085601816" lat="51.44701324158524"/>
          <point xsi:type="esdl:Point" lon="5.530114173889161" lat="51.44825693589835"/>
          <point xsi:type="esdl:Point" lon="5.505695343017578" lat="51.44512757591583"/>
          <point xsi:type="esdl:Point" lon="5.501747131347656" lat="51.4454752931714"/>
          <point xsi:type="esdl:Point" lon="5.500159263610841" lat="51.447187093364626"/>
          <point xsi:type="esdl:Point" lon="5.496326386928559" lat="51.44638803836988"/>
          <point xsi:type="esdl:Point" lon="5.496001839637757" lat="51.44580503701703"/>
        </geometry>
        <port xsi:type="esdl:InPort" name="In" connectedTo="04fbf1cf-1933-477a-b3ff-c610eedbf970" id="a591eec2-b795-480c-b687-e5c02c491ccb" carrier="6a9d1250-f539-4ec4-8ee0-b8c0a11a193d"/>
        <port xsi:type="esdl:OutPort" name="Out" id="777c60a0-9a70-4b80-8333-d751a5d82356" connectedTo="ab887267-1ab1-4e7e-9492-d0278d9e2244" carrier="6a9d1250-f539-4ec4-8ee0-b8c0a11a193d"/>
      </asset>
      <asset xsi:type="esdl:PVPark" technicalLifetime="25.0" id="cca4f609-f9b3-4a18-9114-41dcd55f8657" power="2256600.0" panelEfficiency="0.17" name="PVPark_cca4" surfaceArea="7522" originalIdInSource="energy_power_solar_pv_solar_radiation">
        <geometry xsi:type="esdl:Polygon" CRS="WGS84">
          <exterior xsi:type="esdl:SubPolygon">
            <point xsi:type="esdl:Point" lon="5.496554374694824" lat="51.4469497186546"/>
            <point xsi:type="esdl:Point" lon="5.49668312072754" lat="51.44657192255712"/>
            <point xsi:type="esdl:Point" lon="5.499102473258973" lat="51.447274019006095"/>
            <point xsi:type="esdl:Point" lon="5.498898625373841" lat="51.44761837665177"/>
          </exterior>
        </geometry>
        <dataSource xsi:type="esdl:DataSource" reference="https://github.com/quintel/etdataset-public/blob/master/nodes_source_analyses/energy/energy/energy_chp_local_engine_biogas.xlsx" name="ETM"/>
        <costInformation xsi:type="esdl:CostInformation">
          <fixedOperationalAndMaintenanceCosts xsi:type="esdl:SingleValue" value="318000.0">
            <profileQuantityAndUnit xsi:type="esdl:QuantityAndUnitType" physicalQuantity="COST" description="EUR / year" unit="EURO" perTimeUnit="YEAR"/>
          </fixedOperationalAndMaintenanceCosts>
          <investmentCosts xsi:type="esdl:SingleValue" value="17420000.0">
            <profileQuantityAndUnit xsi:type="esdl:QuantityAndUnitType" physicalQuantity="COST" description="EUR" unit="EURO"/>
          </investmentCosts>
        </costInformation>
        <port xsi:type="esdl:OutPort" name="Out" id="220ede13-6dba-4d7b-98e8-3cbffce77245" connectedTo="dd11af71-d8d3-4af7-bb02-b125fca4a415" carrier="ca9149c7-2a72-43ec-a481-5c81d619b8b1">
          <profile xsi:type="esdl:InfluxDBProfile" port="8086" multiplier="1912500.0" endDate="2016-01-01T00:00:00.000000+0100" field="value" database="energy_profiles" measurement="solar_relative_2011-2016" id="089dd6ef-c0be-40d1-83ac-7de85829d04f" startDate="2015-01-01T00:00:00.000000+0100" filters="" host="http://geis">
            <profileQuantityAndUnit xsi:type="esdl:QuantityAndUnitReference" reference="12c481c0-f81e-49b6-9767-90457684d24a"/>
          </profile>
        </port>
      </asset>
      <asset xsi:type="esdl:PVPark" technicalLifetime="25.0" id="3a9576ab-21a7-422b-9b59-1c321b1092a1" power="20000000.0" panelEfficiency="0.17" name="ElectricityCable_3a95" surfaceArea="200000" originalIdInSource="energy_power_solar_pv_solar_radiation">
        <geometry xsi:type="esdl:Line">
          <point xsi:type="esdl:Point" lon="5.497804372736596" lat="51.4471008477508"/>
          <point xsi:type="esdl:Point" lon="5.496093034744264" lat="51.44666887937588"/>
          <point xsi:type="esdl:Point" lon="5.496019944548608" lat="51.44583387381805"/>
        </geometry>
        <dataSource xsi:type="esdl:DataSource" reference="https://github.com/quintel/etdataset-public/blob/master/nodes_source_analyses/energy/energy/energy_chp_local_engine_biogas.xlsx" name="ETM"/>
        <costInformation xsi:type="esdl:CostInformation">
          <fixedOperationalAndMaintenanceCosts xsi:type="esdl:SingleValue" value="318000.0">
            <profileQuantityAndUnit xsi:type="esdl:QuantityAndUnitType" physicalQuantity="COST" description="EUR / year" unit="EURO" perTimeUnit="YEAR"/>
          </fixedOperationalAndMaintenanceCosts>
          <investmentCosts xsi:type="esdl:SingleValue" value="17420000.0">
            <profileQuantityAndUnit xsi:type="esdl:QuantityAndUnitType" physicalQuantity="COST" description="EUR" unit="EURO"/>
          </investmentCosts>
        </costInformation>
        <port xsi:type="esdl:OutPort" name="Out" id="1522a888-88c6-4c3f-b729-f0ee94cc841b"/>
      </asset>
      <asset xsi:type="esdl:ElectricityCable" id="74749415-f070-40ae-bf0d-53339f971e89" length="228.6" name="ElectricityCable_7474">
        <geometry xsi:type="esdl:Line">
          <point xsi:type="esdl:Point" lon="5.497804372736596" lat="51.4471008477508"/>
          <point xsi:type="esdl:Point" lon="5.495985746383668" lat="51.44661872932291"/>
          <point xsi:type="esdl:Point" lon="5.495985075831414" lat="51.44579458889623"/>
        </geometry>
        <port xsi:type="esdl:InPort" name="In" connectedTo="220ede13-6dba-4d7b-98e8-3cbffce77245" id="dd11af71-d8d3-4af7-bb02-b125fca4a415" carrier="ca9149c7-2a72-43ec-a481-5c81d619b8b1"/>
        <port xsi:type="esdl:OutPort" name="Out" id="ef76b1e5-1eca-4aa2-8e3f-6aed79134bab" connectedTo="35ee69ac-50c5-4042-88bc-3b2a56987948" carrier="ca9149c7-2a72-43ec-a481-5c81d619b8b1"/>
      </asset>
      <asset xsi:type="esdl:Export" id="53a5f5fd-45a9-4495-b551-50043a85d263" power="100000000.0" name="Export_53a5">
        <geometry xsi:type="esdl:Point" lon="5.532758831977844" lat="51.447203809847"/>
        <costInformation xsi:type="esdl:CostInformation" id="fba170cc-64a6-493b-9498-3c681b3ef29c">
          <marginalCosts xsi:type="esdl:SingleValue" id="463fa954-a6bc-45a7-909e-1905982d5534" value="0.1"/>
        </costInformation>
        <port xsi:type="esdl:InPort" name="In" connectedTo="04fbf1cf-1933-477a-b3ff-c610eedbf970" id="4e9659fb-0ddd-4b3e-83f5-393f11963ddb" carrier="6a9d1250-f539-4ec4-8ee0-b8c0a11a193d"/>
      </asset>
      <asset xsi:type="esdl:Bus" id="fdbfaaa1-e82b-4adb-90de-289bbb5d9e56" name="Bus_fdbf">
        <geometry xsi:type="esdl:Point" lon="5.4959575831890115" lat="51.44577202094711"/>
        <port xsi:type="esdl:InPort" name="In" connectedTo="05001546-124f-4a8d-a419-d637342ec24c ef76b1e5-1eca-4aa2-8e3f-6aed79134bab" id="35ee69ac-50c5-4042-88bc-3b2a56987948" carrier="ca9149c7-2a72-43ec-a481-5c81d619b8b1"/>
        <port xsi:type="esdl:OutPort" name="Out" id="99ba81a5-3d32-4666-b2b8-56a3aa47819b" connectedTo="e88f4f46-ddf2-4d9f-8885-af1f534b113c 43e04703-f56b-4ef7-9ed3-abfe5f11e4f6 81a5b223-8031-432b-b8bd-1d04d2a33214" carrier="ca9149c7-2a72-43ec-a481-5c81d619b8b1"/>
      </asset>
      <asset xsi:type="esdl:Bus" id="cbd113ba-36b9-4e3f-99e3-dd2cd1ddd49b" name="Bus_cbd1">
        <geometry xsi:type="esdl:Point" lon="5.532132200896741" lat="51.44701240575778"/>
        <port xsi:type="esdl:InPort" name="In" connectedTo="361d2ffc-3332-4e01-9319-286faee0e803" id="51d7727c-2f6d-4658-8e62-4740d856fffb" carrier="6a9d1250-f539-4ec4-8ee0-b8c0a11a193d"/>
        <port xsi:type="esdl:OutPort" name="Out" id="04fbf1cf-1933-477a-b3ff-c610eedbf970" connectedTo="4e9659fb-0ddd-4b3e-83f5-393f11963ddb a591eec2-b795-480c-b687-e5c02c491ccb" carrier="6a9d1250-f539-4ec4-8ee0-b8c0a11a193d"/>
      </asset>
    </area>
  </instance>
  <services xsi:type="esdl:Services" id="3ad92844-1093-4573-b31d-a7ff886455c7">
    <service xsi:type="esdl:StorageStrategy" id="af062f20-b3a9-4480-9bfe-6a0737830663" name="StorageStrategy for Batterij busstation" energyAsset="e297277d-e002-45de-bc2f-9b13dcd94c12">
      <marginalDischargeCosts xsi:type="esdl:SingleValue" name="marginalChargeCosts for Batterij busstation" value="0.8" id="e739c523-7d59-44ab-a1d1-55e53d1f4738"/>
      <marginalChargeCosts xsi:type="esdl:SingleValue" name="marginalChargeCosts for Batterij busstation" value="0.2" id="99be13cb-4a99-4834-bf72-2ebe426a9bd8"/>
    </service>
  </services>
  <energySystemInformation xsi:type="esdl:EnergySystemInformation" id="312a60e4-b66e-4c39-adec-61c3980277f6">
    <quantityAndUnits xsi:type="esdl:QuantityAndUnits" id="960ddf7e-29ae-4c94-a0ab-f43649794ad8">
      <quantityAndUnit xsi:type="esdl:QuantityAndUnitType" unit="JOULE" id="eb07bccb-203f-407e-af98-e687656a221d" description="Energy in GJ" physicalQuantity="ENERGY" multiplier="GIGA"/>
      <quantityAndUnit xsi:type="esdl:QuantityAndUnitType" unit="WATTHOUR" id="12c481c0-f81e-49b6-9767-90457684d24a" description="Energy in kWh" physicalQuantity="ENERGY" multiplier="KILO"/>
    </quantityAndUnits>
    <carriers xsi:type="esdl:Carriers" id="5eacce9a-7a91-49a3-b8b0-130419f8fa0a">
      <carrier xsi:type="esdl:ElectricityCommodity" id="ca9149c7-2a72-43ec-a481-5c81d619b8b1" voltage="20000.0" name="MS"/>
      <carrier xsi:type="esdl:ElectricityCommodity" id="6a9d1250-f539-4ec4-8ee0-b8c0a11a193d" voltage="110000.0" name="HS110"/>
    </carriers>
  </energySystemInformation>
</esdl:EnergySystem>
