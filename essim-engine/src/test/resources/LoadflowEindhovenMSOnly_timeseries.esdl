<?xml version="1.0" encoding="ASCII"?>
<esdl:EnergySystem xmi:version="2.0" xmlns:xmi="http://www.omg.org/XMI" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:esdl="http://www.tno.nl/esdl" name="Eindhoven MS only" description="" id="e6502739-88a9-407d-854f-ff143df0e492" version="15" esdlVersion="v2401">
  <instance id="e2d85023-2612-4689-87fa-45e361dd41c0" name="Untitled Instance">
    <area id="ba255053-43cd-498b-8ad9-d13ae03cd581" name="Untitled Area">
      <asset xsi:type="esdl:EVChargingStation" id="a1a602fe-ec9b-4807-9fb6-3a4e87941ae0" name="Laadplein busstation">
        <geometry xsi:type="esdl:Point" lat="51.446010237625224" lon="5.497713088989258"/>
        <port xsi:type="esdl:InPort" id="0c619f3d-03c0-493a-bf2b-4ee10aa0e29f" carrier="ca9149c7-2a72-43ec-a481-5c81d619b8b1" name="In" connectedTo="213606b5-6bb7-408e-8d86-f1c9243850ba">
          <profile xsi:type="esdl:TimeSeriesProfile" name="E Demand profile" id="e9e2a3df-c23c-4bb7-ac86-7342157af729" startDateTime="2015-01-01T00:00:00.000+0100">
            <profileQuantityAndUnit xsi:type="esdl:QuantityAndUnitReference" reference="12c481c0-f81e-49b6-9767-90457684d24a"/>
            <values>12.0</values>
            <values>0.0</values>
            <values>1.0</values>
            <values>2.0</values>
            <values>3.0</values>
            <values>5.0</values>
            <values>7.0</values>
            <values>12.0</values>
            <values>3.0</values>
            <values>0.0</values>
            <values>1.0</values>
            <values>0.0</values>
          </profile>
        </port>
      </asset>
      <asset xsi:type="esdl:EConnection" id="04f74257-cf70-4b6c-9cd6-93cd5939965e" name="EConnection_04f7">
        <geometry xsi:type="esdl:Point" CRS="WGS84" lat="51.4462175269164" lon="5.4980671405792245"/>
        <port xsi:type="esdl:InPort" id="2931a9ba-7815-43de-a408-e2762dc392b1" carrier="ca9149c7-2a72-43ec-a481-5c81d619b8b1" name="In" connectedTo="a80634c4-36a5-4a0e-893b-b9cc0b993ad7"/>
        <port xsi:type="esdl:OutPort" id="213606b5-6bb7-408e-8d86-f1c9243850ba" carrier="ca9149c7-2a72-43ec-a481-5c81d619b8b1" name="Out" connectedTo="0c619f3d-03c0-493a-bf2b-4ee10aa0e29f 85d417c8-e69b-4562-a63e-92de3b1953f5"/>
      </asset>
      <asset xsi:type="esdl:ElectricityCable" id="9c9f7410-06fc-4df2-95fc-e3de9a227f5d" name="ElectricityCable_9c9f" length="155.5">
        <geometry xsi:type="esdl:Line">
          <point lat="51.44578915587244" lon="5.49599513411522"/>
          <point lat="51.446020267773896" lon="5.496511459350586"/>
          <point lat="51.44623758712043" lon="5.49768626689911"/>
          <point lat="51.4462175269164" lon="5.4980671405792245"/>
        </geometry>
        <port xsi:type="esdl:InPort" id="e88f4f46-ddf2-4d9f-8885-af1f534b113c" carrier="ca9149c7-2a72-43ec-a481-5c81d619b8b1" name="In" connectedTo="99ba81a5-3d32-4666-b2b8-56a3aa47819b"/>
        <port xsi:type="esdl:OutPort" id="a80634c4-36a5-4a0e-893b-b9cc0b993ad7" carrier="ca9149c7-2a72-43ec-a481-5c81d619b8b1" name="Out" connectedTo="2931a9ba-7815-43de-a408-e2762dc392b1"/>
      </asset>
      <asset xsi:type="esdl:Battery" id="e297277d-e002-45de-bc2f-9b13dcd94c12" name="Batterij busstation" description="6.8 MWh buurtbatterij is ongeveer 24 GJ" controlStrategy="af062f20-b3a9-4480-9bfe-6a0737830663" capacity="2.4E10" maxChargeRate="500000.0" maxDischargeRate="500000.0">
        <geometry xsi:type="esdl:Point" lat="51.44594002652293" lon="5.4981958866119385"/>
        <port xsi:type="esdl:InPort" id="85d417c8-e69b-4562-a63e-92de3b1953f5" carrier="ca9149c7-2a72-43ec-a481-5c81d619b8b1" name="In" connectedTo="213606b5-6bb7-408e-8d86-f1c9243850ba"/>
      </asset>
      <asset xsi:type="esdl:ElectricityDemand" id="561715ea-e0f0-46c1-b3ff-244c2e54ce33" name="Campus Demand">
        <geometry xsi:type="esdl:Point" CRS="WGS84" lat="51.4479493587373" lon="5.49091100692749"/>
        <port xsi:type="esdl:InPort" id="4f0c6404-b231-4eb3-8057-7f92a9fdf1ab" carrier="ca9149c7-2a72-43ec-a481-5c81d619b8b1" name="In" connectedTo="0576c163-d1cd-4037-976a-d47643c0da0f">
          <profile xsi:type="esdl:TimeSeriesProfile" startDateTime="2015-01-01T01:00:00.000+0100" timestep="900">
            <values>5.783999999999999E7</values>
            <values>6.003E7</values>
            <values>6.134999999999999E7</values>
            <values>6.249E7</values>
            <values>6.303E7</values>
            <values>6.360000000000001E7</values>
            <values>6.387000000000001E7</values>
            <values>6.369E7</values>
            <values>6.396E7</values>
            <values>6.381000000000001E7</values>
            <values>6.351E7</values>
            <values>6.411E7</values>
            <values>6.444E7</values>
            <values>6.48E7</values>
            <values>6.378E7</values>
            <values>6.378E7</values>
            <values>6.465E7</values>
            <values>6.54E7</values>
            <values>6.564000000000001E7</values>
            <values>6.501E7</values>
            <values>6.561E7</values>
            <values>6.555E7</values>
            <values>6.623999999999999E7</values>
            <values>6.555E7</values>
            <values>6.585000000000001E7</values>
            <values>6.585000000000001E7</values>
            <values>6.612E7</values>
            <values>6.672E7</values>
            <values>6.69E7</values>
            <values>6.738E7</values>
            <values>6.72E7</values>
            <values>6.770999999999999E7</values>
            <values>6.774000000000001E7</values>
            <values>6.723E7</values>
            <values>6.621000000000001E7</values>
            <values>6.519E7</values>
            <values>6.495000000000001E7</values>
            <values>6.495000000000001E7</values>
            <values>6.470999999999999E7</values>
            <values>6.465E7</values>
            <values>6.525E7</values>
            <values>6.672E7</values>
            <values>6.735E7</values>
            <values>6.747E7</values>
            <values>6.774000000000001E7</values>
            <values>6.783E7</values>
            <values>6.774000000000001E7</values>
            <values>6.696000000000001E7</values>
            <values>6.693E7</values>
            <values>6.69E7</values>
            <values>6.651E7</values>
            <values>6.621000000000001E7</values>
            <values>6.618E7</values>
            <values>6.654E7</values>
            <values>6.69E7</values>
            <values>6.660000000000001E7</values>
            <values>6.623999999999999E7</values>
            <values>6.587999999999999E7</values>
            <values>6.621000000000001E7</values>
            <values>6.648E7</values>
            <values>6.63E7</values>
            <values>6.579E7</values>
            <values>6.527999999999999E7</values>
            <values>6.489E7</values>
            <values>6.546000000000001E7</values>
            <values>6.648E7</values>
            <values>6.78E7</values>
            <values>6.917999999999999E7</values>
            <values>7.02E7</values>
            <values>7.077E7</values>
            <values>7.047E7</values>
            <values>7.017000000000001E7</values>
            <values>7.038E7</values>
            <values>7.053E7</values>
            <values>7.053E7</values>
            <values>7.008E7</values>
            <values>6.992999999999999E7</values>
            <values>6.992999999999999E7</values>
            <values>7.011E7</values>
            <values>7.02E7</values>
            <values>6.996E7</values>
            <values>7.029E7</values>
            <values>7.005E7</values>
            <values>7.053E7</values>
            <values>7.011E7</values>
            <values>6.987E7</values>
            <values>6.951E7</values>
            <values>6.917999999999999E7</values>
            <values>6.855E7</values>
            <values>6.788999999999999E7</values>
            <values>6.753E7</values>
            <values>6.786E7</values>
            <values>6.87E7</values>
            <values>6.878999999999999E7</values>
            <values>6.804E7</values>
            <values>6.891E7</values>
          </profile>
        </port>
      </asset>
      <asset xsi:type="esdl:ElectricityDemand" id="dc538be8-b532-4a14-848d-a170fd39d879" name="Woningen">
        <geometry xsi:type="esdl:Point" lat="51.447962731700436" lon="5.476641654968262"/>
        <port xsi:type="esdl:InPort" id="06b82ec8-ab5c-4eab-98be-020dd3cebfca" carrier="ca9149c7-2a72-43ec-a481-5c81d619b8b1" name="In" connectedTo="99c86bed-bc03-4f1c-8e40-553260487c80">
          <profile xsi:type="esdl:TimeSeriesProfile" startDateTime="2015-01-01T01:00:00.000+0100" timestep="900">
            <values>1.996E8</values>
            <values>1.939E8</values>
            <values>1.8575E8</values>
            <values>1.7875000000000003E8</values>
            <values>1.691E8</values>
            <values>1.5805E8</values>
            <values>1.4855E8</values>
            <values>1.3705E8</values>
            <values>1.2985E8</values>
            <values>1.192E8</values>
            <values>1.1355000000000001E8</values>
            <values>1.062E8</values>
            <values>9.870000000000001E7</values>
            <values>9.004999999999999E7</values>
            <values>8.29E7</values>
            <values>8.09E7</values>
            <values>7.905E7</values>
            <values>7.955E7</values>
            <values>7.755E7</values>
            <values>7.84E7</values>
            <values>7.655E7</values>
            <values>7.64E7</values>
            <values>7.37E7</values>
            <values>7.755E7</values>
            <values>7.94E7</values>
            <values>8.37E7</values>
            <values>8.255E7</values>
            <values>8.539999999999999E7</values>
            <values>8.62E7</values>
            <values>9.004999999999999E7</values>
            <values>9.140000000000001E7</values>
            <values>9.505E7</values>
            <values>9.82E7</values>
            <values>1.0635E8</values>
            <values>1.1685000000000001E8</values>
            <values>1.2605E8</values>
            <values>1.2920000000000001E8</values>
            <values>1.267E8</values>
            <values>1.3005E8</values>
            <values>1.3755E8</values>
            <values>1.5005E8</values>
            <values>1.6354999999999997E8</values>
            <values>1.7525E8</values>
            <values>1.8475E8</values>
            <values>1.8960000000000003E8</values>
            <values>1.914E8</values>
            <values>1.9575E8</values>
            <values>2.0575E8</values>
            <values>2.0509999999999997E8</values>
            <values>2.024E8</values>
            <values>1.916E8</values>
            <values>1.8975E8</values>
            <values>1.9624999999999997E8</values>
            <values>2.0190000000000003E8</values>
            <values>2.039E8</values>
            <values>1.9575E8</values>
            <values>1.929E8</values>
            <values>1.939E8</values>
            <values>1.961E8</values>
            <values>1.944E8</values>
            <values>1.9225E8</values>
            <values>1.9110000000000003E8</values>
            <values>1.891E8</values>
            <values>1.9340000000000003E8</values>
            <values>1.9340000000000003E8</values>
            <values>1.956E8</values>
            <values>2.0190000000000003E8</values>
            <values>2.184E8</values>
            <values>2.381E8</values>
            <values>2.6024999999999997E8</values>
            <values>2.801E8</values>
            <values>2.926E8</values>
            <values>2.8525E8</values>
            <values>2.8045E8</values>
            <values>2.7405E8</values>
            <values>2.8225E8</values>
            <values>2.8459999999999994E8</values>
            <values>2.8045E8</values>
            <values>2.7125E8</values>
            <values>2.6489999999999997E8</values>
            <values>2.7475E8</values>
            <values>2.7855E8</values>
            <values>2.7405E8</values>
            <values>2.5874999999999997E8</values>
            <values>2.4725000000000003E8</values>
            <values>2.346E8</values>
            <values>2.241E8</values>
            <values>2.1475E8</values>
            <values>2.109E8</values>
            <values>2.0775E8</values>
            <values>2.064E8</values>
            <values>2.0040000000000003E8</values>
            <values>1.9025000000000003E8</values>
            <values>1.7540000000000003E8</values>
            <values>1.6205000000000003E8</values>
            <values>1.5255E8</values>
          </profile>
        </port>
      </asset>
      <asset xsi:type="esdl:ElectricityCable" id="e88f3f70-ac3c-47ad-8514-0dcbf47460ea" name="ElectricityCable_e88f" length="702.6">
        <geometry xsi:type="esdl:Line">
          <point lat="51.44579709644544" lon="5.495966970920563"/>
          <point lat="51.446117225763984" lon="5.495781898498536"/>
          <point lat="51.445759483192774" lon="5.490213632583619"/>
          <point lat="51.44753479493761" lon="5.489988327026367"/>
          <point lat="51.4479493587373" lon="5.49091100692749"/>
        </geometry>
        <port xsi:type="esdl:InPort" id="43e04703-f56b-4ef7-9ed3-abfe5f11e4f6" carrier="ca9149c7-2a72-43ec-a481-5c81d619b8b1" name="In" connectedTo="99ba81a5-3d32-4666-b2b8-56a3aa47819b"/>
        <port xsi:type="esdl:OutPort" id="0576c163-d1cd-4037-976a-d47643c0da0f" carrier="ca9149c7-2a72-43ec-a481-5c81d619b8b1" name="Out" connectedTo="4f0c6404-b231-4eb3-8057-7f92a9fdf1ab"/>
      </asset>
      <asset xsi:type="esdl:ElectricityCable" id="551ab14f-b17b-480a-b048-87f38541f68e" name="ElectricityCable_551a" length="1788.8">
        <geometry xsi:type="esdl:Line">
          <point lat="51.44579333512155" lon="5.495958253741265"/>
          <point lat="51.44599686409025" lon="5.49567461013794"/>
          <point lat="51.44506070690928" lon="5.481126308441163"/>
          <point lat="51.444285019277444" lon="5.476491451263428"/>
          <point lat="51.44483335155461" lon="5.476233959197999"/>
          <point lat="51.447962731700436" lon="5.476641654968262"/>
        </geometry>
        <port xsi:type="esdl:InPort" id="81a5b223-8031-432b-b8bd-1d04d2a33214" carrier="ca9149c7-2a72-43ec-a481-5c81d619b8b1" name="In" connectedTo="99ba81a5-3d32-4666-b2b8-56a3aa47819b"/>
        <port xsi:type="esdl:OutPort" id="99c86bed-bc03-4f1c-8e40-553260487c80" carrier="ca9149c7-2a72-43ec-a481-5c81d619b8b1" name="Out" connectedTo="06b82ec8-ab5c-4eab-98be-020dd3cebfca"/>
      </asset>
      <asset xsi:type="esdl:PVPark" id="cca4f609-f9b3-4a18-9114-41dcd55f8657" name="PVPark_cca4" originalIdInSource="energy_power_solar_pv_solar_radiation" surfaceArea="7522" technicalLifetime="25.0" power="2256600.0" panelEfficiency="0.17">
        <dataSource xsi:type="esdl:DataSource" name="ETM" reference="https://github.com/quintel/etdataset-public/blob/master/nodes_source_analyses/energy/energy/energy_chp_local_engine_biogas.xlsx"/>
        <geometry xsi:type="esdl:Polygon" CRS="WGS84">
          <exterior>
            <point lat="51.4469497186546" lon="5.496554374694824"/>
            <point lat="51.44657192255712" lon="5.49668312072754"/>
            <point lat="51.447274019006095" lon="5.499102473258973"/>
            <point lat="51.44761837665177" lon="5.498898625373841"/>
          </exterior>
        </geometry>
        <costInformation>
          <investmentCosts xsi:type="esdl:SingleValue" value="1.742E7">
            <profileQuantityAndUnit xsi:type="esdl:QuantityAndUnitType" physicalQuantity="COST" unit="EURO" description="EUR"/>
          </investmentCosts>
          <fixedOperationalAndMaintenanceCosts xsi:type="esdl:SingleValue" value="318000.0">
            <profileQuantityAndUnit xsi:type="esdl:QuantityAndUnitType" physicalQuantity="COST" unit="EURO" description="EUR / year" perTimeUnit="YEAR"/>
          </fixedOperationalAndMaintenanceCosts>
        </costInformation>
        <port xsi:type="esdl:OutPort" id="220ede13-6dba-4d7b-98e8-3cbffce77245" carrier="ca9149c7-2a72-43ec-a481-5c81d619b8b1" name="Out" connectedTo="dd11af71-d8d3-4af7-bb02-b125fca4a415">
          <profile xsi:type="esdl:TimeSeriesProfile" startDateTime="2015-01-01T01:00:00.000+0100" timestep="900">
            <values>0.0</values>
            <values>0.0</values>
            <values>0.0</values>
            <values>0.0</values>
            <values>0.0</values>
            <values>0.0</values>
            <values>0.0</values>
            <values>0.0</values>
            <values>0.0</values>
            <values>0.0</values>
            <values>0.0</values>
            <values>0.0</values>
            <values>0.0</values>
            <values>0.0</values>
            <values>0.0</values>
            <values>0.0</values>
            <values>0.0</values>
            <values>0.0</values>
            <values>0.0</values>
            <values>0.0</values>
            <values>0.0</values>
            <values>0.0</values>
            <values>0.0</values>
            <values>0.0</values>
            <values>0.0</values>
            <values>0.0</values>
            <values>0.0</values>
            <values>0.0</values>
            <values>4534617.208493599</values>
            <values>4534617.208493599</values>
            <values>4534617.208493599</values>
            <values>4534617.208493599</values>
            <values>9.976157858685918E7</values>
            <values>9.976157858685918E7</values>
            <values>9.976157858685918E7</values>
            <values>9.976157858685918E7</values>
            <values>1.6778083671426314E8</values>
            <values>1.6778083671426314E8</values>
            <values>1.6778083671426314E8</values>
            <values>1.6778083671426314E8</values>
            <values>3.174232045945518E8</values>
            <values>3.174232045945518E8</values>
            <values>3.174232045945518E8</values>
            <values>3.174232045945518E8</values>
            <values>2.811462669266031E8</values>
            <values>2.811462669266031E8</values>
            <values>2.811462669266031E8</values>
            <values>2.811462669266031E8</values>
            <values>2.675424153011223E8</values>
            <values>2.675424153011223E8</values>
            <values>2.675424153011223E8</values>
            <values>2.675424153011223E8</values>
            <values>1.2243466462932715E8</values>
            <values>1.2243466462932715E8</values>
            <values>1.2243466462932715E8</values>
            <values>1.2243466462932715E8</values>
            <values>6.348464091891039E7</values>
            <values>6.348464091891039E7</values>
            <values>6.348464091891039E7</values>
            <values>6.348464091891039E7</values>
            <values>4534617.208493599</values>
            <values>4534617.208493599</values>
            <values>4534617.208493599</values>
            <values>4534617.208493599</values>
            <values>0.0</values>
            <values>0.0</values>
            <values>0.0</values>
            <values>0.0</values>
            <values>0.0</values>
            <values>0.0</values>
            <values>0.0</values>
            <values>0.0</values>
            <values>0.0</values>
            <values>0.0</values>
            <values>0.0</values>
            <values>0.0</values>
            <values>0.0</values>
            <values>0.0</values>
            <values>0.0</values>
            <values>0.0</values>
            <values>0.0</values>
            <values>0.0</values>
            <values>0.0</values>
            <values>0.0</values>
            <values>0.0</values>
            <values>0.0</values>
            <values>0.0</values>
            <values>0.0</values>
            <values>0.0</values>
            <values>0.0</values>
            <values>0.0</values>
            <values>0.0</values>
            <values>0.0</values>
            <values>0.0</values>
            <values>0.0</values>
            <values>0.0</values>
          </profile>
        </port>
      </asset>
      <asset xsi:type="esdl:PVPark" id="3a9576ab-21a7-422b-9b59-1c321b1092a1" name="ElectricityCable_3a95" originalIdInSource="energy_power_solar_pv_solar_radiation" surfaceArea="200000" technicalLifetime="25.0" power="2.0E7" panelEfficiency="0.17">
        <dataSource xsi:type="esdl:DataSource" name="ETM" reference="https://github.com/quintel/etdataset-public/blob/master/nodes_source_analyses/energy/energy/energy_chp_local_engine_biogas.xlsx"/>
        <geometry xsi:type="esdl:Line">
          <point lat="51.4471008477508" lon="5.497804372736596"/>
          <point lat="51.44666887937588" lon="5.496093034744264"/>
          <point lat="51.44583387381805" lon="5.496019944548608"/>
        </geometry>
        <costInformation>
          <investmentCosts xsi:type="esdl:SingleValue" value="1.742E7">
            <profileQuantityAndUnit xsi:type="esdl:QuantityAndUnitType" physicalQuantity="COST" unit="EURO" description="EUR"/>
          </investmentCosts>
          <fixedOperationalAndMaintenanceCosts xsi:type="esdl:SingleValue" value="318000.0">
            <profileQuantityAndUnit xsi:type="esdl:QuantityAndUnitType" physicalQuantity="COST" unit="EURO" description="EUR / year" perTimeUnit="YEAR"/>
          </fixedOperationalAndMaintenanceCosts>
        </costInformation>
        <port xsi:type="esdl:OutPort" id="1522a888-88c6-4c3f-b729-f0ee94cc841b" name="Out"/>
      </asset>
      <asset xsi:type="esdl:ElectricityCable" id="74749415-f070-40ae-bf0d-53339f971e89" name="ElectricityCable_7474" length="228.6">
        <geometry xsi:type="esdl:Line">
          <point lat="51.4471008477508" lon="5.497804372736596"/>
          <point lat="51.44661872932291" lon="5.495985746383668"/>
          <point lat="51.44579458889623" lon="5.495985075831414"/>
        </geometry>
        <port xsi:type="esdl:InPort" id="dd11af71-d8d3-4af7-bb02-b125fca4a415" carrier="ca9149c7-2a72-43ec-a481-5c81d619b8b1" name="In" connectedTo="220ede13-6dba-4d7b-98e8-3cbffce77245"/>
        <port xsi:type="esdl:OutPort" id="ef76b1e5-1eca-4aa2-8e3f-6aed79134bab" carrier="ca9149c7-2a72-43ec-a481-5c81d619b8b1" name="Out" connectedTo="35ee69ac-50c5-4042-88bc-3b2a56987948"/>
      </asset>
      <asset xsi:type="esdl:Bus" id="fdbfaaa1-e82b-4adb-90de-289bbb5d9e56" name="Bus_fdbf">
        <geometry xsi:type="esdl:Point" lat="51.44577202094711" lon="5.4959575831890115"/>
        <port xsi:type="esdl:InPort" id="35ee69ac-50c5-4042-88bc-3b2a56987948" carrier="ca9149c7-2a72-43ec-a481-5c81d619b8b1" name="In" connectedTo="ef76b1e5-1eca-4aa2-8e3f-6aed79134bab 27234ec3-d308-455e-80a8-ff17f552bc27"/>
        <port xsi:type="esdl:OutPort" id="99ba81a5-3d32-4666-b2b8-56a3aa47819b" carrier="ca9149c7-2a72-43ec-a481-5c81d619b8b1" name="Out" connectedTo="e88f4f46-ddf2-4d9f-8885-af1f534b113c 43e04703-f56b-4ef7-9ed3-abfe5f11e4f6 81a5b223-8031-432b-b8bd-1d04d2a33214 635bcf3b-0fa9-43c9-aaf6-2e4622ab9778"/>
      </asset>
      <asset xsi:type="esdl:Import" id="d685bf68-b2a4-42f4-b199-32389134b669" name="Import_d685" controlStrategy="772ce384-7b87-4bd9-a59e-78643ac63743" power="1.0E7">
        <geometry xsi:type="esdl:Point" lat="51.44556222207159" lon="5.496892333030702"/>
        <port xsi:type="esdl:OutPort" id="27234ec3-d308-455e-80a8-ff17f552bc27" carrier="ca9149c7-2a72-43ec-a481-5c81d619b8b1" name="Out" connectedTo="35ee69ac-50c5-4042-88bc-3b2a56987948"/>
      </asset>
      <asset xsi:type="esdl:Export" id="166adc33-92ca-4d94-96b8-cb482d253bd2" name="Export_166a" power="1.0E8">
        <geometry xsi:type="esdl:Point" CRS="WGS84" lat="51.44550872738337" lon="5.495041608810426"/>
        <costInformation>
          <marginalCosts xsi:type="esdl:SingleValue" name="Export_166a-MarginalCosts" id="985f6af6-a7c9-49ce-8173-f7dd77e50bb2" value="0.1"/>
        </costInformation>
        <port xsi:type="esdl:InPort" id="635bcf3b-0fa9-43c9-aaf6-2e4622ab9778" carrier="ca9149c7-2a72-43ec-a481-5c81d619b8b1" name="In" connectedTo="99ba81a5-3d32-4666-b2b8-56a3aa47819b"/>
      </asset>
    </area>
  </instance>
  <energySystemInformation id="312a60e4-b66e-4c39-adec-61c3980277f6">
    <carriers id="5eacce9a-7a91-49a3-b8b0-130419f8fa0a">
      <carrier xsi:type="esdl:ElectricityCommodity" name="MS" id="ca9149c7-2a72-43ec-a481-5c81d619b8b1" voltage="20000.0"/>
    </carriers>
    <quantityAndUnits id="960ddf7e-29ae-4c94-a0ab-f43649794ad8">
      <quantityAndUnit physicalQuantity="ENERGY" multiplier="GIGA" unit="JOULE" description="Energy in GJ" id="eb07bccb-203f-407e-af98-e687656a221d"/>
      <quantityAndUnit physicalQuantity="ENERGY" multiplier="KILO" unit="WATTHOUR" description="Energy in kWh" id="12c481c0-f81e-49b6-9767-90457684d24a"/>
    </quantityAndUnits>
  </energySystemInformation>
  <services id="3ad92844-1093-4573-b31d-a7ff886455c7">
    <service xsi:type="esdl:StorageStrategy" id="af062f20-b3a9-4480-9bfe-6a0737830663" name="StorageStrategy for Batterij busstation" energyAsset="e297277d-e002-45de-bc2f-9b13dcd94c12">
      <marginalChargeCosts xsi:type="esdl:SingleValue" name="marginalChargeCosts for Batterij busstation" id="99be13cb-4a99-4834-bf72-2ebe426a9bd8" value="0.2"/>
      <marginalDischargeCosts xsi:type="esdl:SingleValue" name="marginalChargeCosts for Batterij busstation" id="e739c523-7d59-44ab-a1d1-55e53d1f4738" value="0.8"/>
    </service>
    <service xsi:type="esdl:DrivenByDemand" id="772ce384-7b87-4bd9-a59e-78643ac63743" name="DrivenByDemand for Import_d685" energyAsset="d685bf68-b2a4-42f4-b199-32389134b669" outPort="27234ec3-d308-455e-80a8-ff17f552bc27"/>
  </services>
</esdl:EnergySystem>
