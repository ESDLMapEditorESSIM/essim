<?xml version='1.0' encoding='UTF-8'?>
<esdl:EnergySystem xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:esdl="http://www.tno.nl/esdl" esdlVersion="v2401" name="TestLonger" version="1" id="3a27a7a1-022d-4411-ba7c-6a131cae4077" description="">
  <energySystemInformation xsi:type="esdl:EnergySystemInformation" id="299719da-a6f6-4f8c-a8ec-aa8e3a84841e">
    <carriers xsi:type="esdl:Carriers" id="8d75a436-c33d-4b17-8760-0d067c53b1b8">
      <carrier xsi:type="esdl:GasCommodity" id="2fa10edc-1258-4b76-bbaf-ec2b9dfbca33" name="Gas"/>
      <carrier xsi:type="esdl:ElectricityCommodity" id="1be459f4-a92b-44fe-a46b-63356a828249" name="Electricity"/>
      <carrier xsi:type="esdl:HeatCommodity" id="5793da35-ad0c-4ded-8326-61fdf5611511" name="Heat"/>
    </carriers>
    <quantityAndUnits xsi:type="esdl:QuantityAndUnits" id="4f80ddba-ec80-4a5a-9c93-bf3d01b43a2b">
      <quantityAndUnit xsi:type="esdl:QuantityAndUnitType" id="12c481c0-f81e-49b6-9767-90457684d24a" unit="WATTHOUR" multiplier="KILO" physicalQuantity="ENERGY" description="Energy in kWh"/>
    </quantityAndUnits>
  </energySystemInformation>
  <instance xsi:type="esdl:Instance" id="e0745e6e-1c5b-4f8a-b61b-e52b2a686e95" name="Untitled instance">
    <area xsi:type="esdl:Area" id="207a6c69-2a81-4f39-ba0b-5c052417a52f" name="Untitled area">
      <asset xsi:type="esdl:Import" name="Import_35b2" id="35b2983b-6d4f-4197-9baf-f26b3f13cef0">
        <port xsi:type="esdl:OutPort" connectedTo="6d630ed3-e897-484a-aa1a-37e7cad00c31" id="15db29a2-736d-4f68-9c15-d91cad1bde9a" carrier="2fa10edc-1258-4b76-bbaf-ec2b9dfbca33" name="Out"/>
        <geometry xsi:type="esdl:Point" lat="52.178860389073876" CRS="WGS84" lon="4.478241652250291"/>
        <costInformation xsi:type="esdl:CostInformation">
          <marginalCosts xsi:type="esdl:SingleValue" id="ed5c07da-af63-4851-8168-f22316286e07" value="0.9" name="Import_35b2-MarginalCosts"/>
        </costInformation>
      </asset>
      <asset xsi:type="esdl:GenericConversion" name="GenericConversion_2ef7" id="2ef7a3cc-cb41-41e3-97ac-9db738b8468d" power="10000.0" controlStrategy="30cea809-5287-4dcf-bede-75064b6ed488">
        <port xsi:type="esdl:InPort" connectedTo="16f4ec78-8067-43b0-b526-6a9ed674f9a9" id="21601fd2-5a13-4615-be0e-b55341939ca7" carrier="2fa10edc-1258-4b76-bbaf-ec2b9dfbca33" name="In"/>
        <port xsi:type="esdl:OutPort" connectedTo="ea15cb57-0d16-4f57-bba4-e85b49d59f40" id="9cba87de-9dc2-4d33-b33a-fd0246471708" carrier="1be459f4-a92b-44fe-a46b-63356a828249" name="Out"/>
        <geometry xsi:type="esdl:Point" lat="52.17905775583554" CRS="WGS84" lon="4.478428065776826"/>
      </asset>
      <asset xsi:type="esdl:GenericConversion" name="GenericConversion_db47" id="db47538b-2b4a-4565-9368-37bc460ad295" power="10000.0" controlStrategy="fcfdea92-53a7-414a-be2e-a2693687a019">
        <port xsi:type="esdl:InPort" connectedTo="16f4ec78-8067-43b0-b526-6a9ed674f9a9" id="a736271d-5bb1-438f-8f27-090857f955ba" carrier="2fa10edc-1258-4b76-bbaf-ec2b9dfbca33" name="In"/>
        <port xsi:type="esdl:OutPort" connectedTo="aa7f71cd-fae5-4ddd-8458-39f3bd19bdc5 c854ff78-2e1a-4427-8b04-de7afe3870f6" id="efbbad33-ce14-4087-b35e-38a89ad913e3" carrier="5793da35-ad0c-4ded-8326-61fdf5611511" name="Out"/>
        <geometry xsi:type="esdl:Point" lat="52.178968118539835" lon="4.478602409362794"/>
      </asset>
      <asset xsi:type="esdl:ElectricityDemand" name="ElectricityDemand_6d5d" id="6d5d824f-c865-47cb-b8f6-6f4cf994b17a">
        <port xsi:type="esdl:InPort" connectedTo="9cba87de-9dc2-4d33-b33a-fd0246471708" id="ea15cb57-0d16-4f57-bba4-e85b49d59f40" carrier="1be459f4-a92b-44fe-a46b-63356a828249" name="In">
          <profile xsi:type="esdl:InfluxDBProfile" host="http://localhost" filters="" startDate="2018-12-31T23:00:00.000000+0000" id="b418e0ba-2267-4fb2-b35c-ad0a25c48019" endDate="2019-12-31T22:00:00.000000+0000" measurement="standard_profiles" field="E1C" database="energy_profiles" port="8086">
            <profileQuantityAndUnit xsi:type="esdl:QuantityAndUnitReference" reference="12c481c0-f81e-49b6-9767-90457684d24a"/>
          </profile>
        </port>
        <geometry xsi:type="esdl:Point" lat="52.17920166854713" CRS="WGS84" lon="4.478601068258286"/>
      </asset>
      <asset xsi:type="esdl:HeatingDemand" name="HeatingDemand_8133" id="81332d83-2e71-4282-b83c-b631e49bcf45">
        <port xsi:type="esdl:InPort" connectedTo="efbbad33-ce14-4087-b35e-38a89ad913e3" id="aa7f71cd-fae5-4ddd-8458-39f3bd19bdc5" carrier="5793da35-ad0c-4ded-8326-61fdf5611511" name="In">
          <profile xsi:type="esdl:InfluxDBProfile" host="http://localhost" filters="" startDate="2018-12-31T23:00:00.000000+0000" id="a75ff614-6a3f-4db1-a14e-73fbe1ccfb3f" endDate="2019-12-31T22:00:00.000000+0000" measurement="standard_profiles" field="E1A" database="energy_profiles" port="8086">
            <profileQuantityAndUnit xsi:type="esdl:QuantityAndUnitReference" reference="12c481c0-f81e-49b6-9767-90457684d24a"/>
          </profile>
        </port>
        <geometry xsi:type="esdl:Point" lat="52.17912189984451" lon="4.478881359100343"/>
      </asset>
      <asset xsi:type="esdl:GasProducer" name="GasProducer_1861" id="18613676-8d37-431c-a263-f10c62abb8cd">
        <port xsi:type="esdl:OutPort" connectedTo="6d630ed3-e897-484a-aa1a-37e7cad00c31" id="c98bb86b-219e-4f5e-973d-879ac40006ab" carrier="2fa10edc-1258-4b76-bbaf-ec2b9dfbca33" name="Out">
          <profile xsi:type="esdl:InfluxDBProfile" host="http://localhost" filters="" startDate="2018-12-31T23:00:00.000000+0000" id="4edf3093-c0f3-4fd4-b85c-12ba2594e87a" endDate="2019-12-31T22:00:00.000000+0000" measurement="standard_profiles" field="E1A" database="energy_profiles" port="8086">
            <profileQuantityAndUnit xsi:type="esdl:QuantityAndUnitReference" reference="12c481c0-f81e-49b6-9767-90457684d24a"/>
          </profile>
        </port>
        <geometry xsi:type="esdl:Point" lat="52.17879871178123" CRS="WGS84" lon="4.4784602522850046"/>
      </asset>
      <asset xsi:type="esdl:GasNetwork" name="GasNetwork_7cda" id="7cdaab0c-803a-408e-8349-87539c347ac7">
        <port xsi:type="esdl:InPort" connectedTo="15db29a2-736d-4f68-9c15-d91cad1bde9a c98bb86b-219e-4f5e-973d-879ac40006ab" id="6d630ed3-e897-484a-aa1a-37e7cad00c31" carrier="2fa10edc-1258-4b76-bbaf-ec2b9dfbca33" name="In"/>
        <port xsi:type="esdl:OutPort" connectedTo="a736271d-5bb1-438f-8f27-090857f955ba 21601fd2-5a13-4615-be0e-b55341939ca7" id="16f4ec78-8067-43b0-b526-6a9ed674f9a9" carrier="2fa10edc-1258-4b76-bbaf-ec2b9dfbca33" name="Out"/>
        <geometry xsi:type="esdl:Point" lat="52.17894015824552" lon="4.47837844491005"/>
      </asset>
      <asset xsi:type="esdl:Export" name="Export_caca" id="cacab760-90e5-4dc2-a28e-b7d63a7e98d4">
        <port xsi:type="esdl:InPort" connectedTo="efbbad33-ce14-4087-b35e-38a89ad913e3" id="c854ff78-2e1a-4427-8b04-de7afe3870f6" carrier="5793da35-ad0c-4ded-8326-61fdf5611511" name="In"/>
        <geometry xsi:type="esdl:Point" lat="52.17901910491368" CRS="WGS84" lon="4.478854537010194"/>
        <costInformation xsi:type="esdl:CostInformation">
          <marginalCosts xsi:type="esdl:SingleValue" id="92eaa22d-33c0-4474-97c5-c67885b671cd" value="0.1" name="Export_caca-MarginalCosts"/>
        </costInformation>
      </asset>
    </area>
  </instance>
  <services xsi:type="esdl:Services">
    <service xsi:type="esdl:DrivenBySupply" id="fcfdea92-53a7-414a-be2e-a2693687a019" name="DrivenBySupply for GenericConversion_db47" inPort="a736271d-5bb1-438f-8f27-090857f955ba" energyAsset="db47538b-2b4a-4565-9368-37bc460ad295"/>
    <service xsi:type="esdl:DrivenByDemand" id="30cea809-5287-4dcf-bede-75064b6ed488" name="DrivenByDemand for GenericConversion_2ef7" outPort="9cba87de-9dc2-4d33-b33a-fd0246471708" energyAsset="2ef7a3cc-cb41-41e3-97ac-9db738b8468d"/>
  </services>
</esdl:EnergySystem>
