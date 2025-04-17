<?xml version='1.0' encoding='UTF-8'?>
<esdl:EnergySystem xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:esdl="http://www.tno.nl/esdl" id="2ee64f26-6549-4088-a6b8-a03489bc6025" name="Local Network And Grid With Prices" description="" esdlVersion="v2303" version="10">
  <instance xsi:type="esdl:Instance" id="b510fb05-876c-41ae-840e-19bed0c6f786" name="Untitled Instance">
    <area xsi:type="esdl:Area" name="Untitled Area" id="5b57a707-7698-44a6-af66-074869dbdd7f">
      <asset xsi:type="esdl:Import" power="25000.0" name="Import_Global" id="a1211753-1cda-4760-b9ce-972c8925fca0">
        <port xsi:type="esdl:OutPort" carrier="60c23a8a-bc0c-4918-82b6-637eb5c1fd76" connectedTo="4030a6d5-6221-4424-8c89-1d15b67e6f00" name="Out" id="41079d9f-0aec-4452-8890-d52a3f14a743"/>
        <costInformation xsi:type="esdl:CostInformation">
          <marginalCosts xsi:type="esdl:SingleValue" name="Import_a121-MarginalCosts" value="9e-7" id="31ee995f-80da-438b-8858-90cdfae34940"/>
        </costInformation>
        <geometry xsi:type="esdl:Point" lon="5.398090481758119" lat="52.391469684257515"/>
      </asset>
      <asset xsi:type="esdl:ElectricityNetwork" name="ElectricityNetwork_Local" id="2904c4de-aaa4-4688-b01e-95a06f2ee089">
        <port xsi:type="esdl:InPort" carrier="83ff3b91-32cc-40d3-953a-3f0b8698866a" connectedTo="be625810-1db5-482e-9274-d8115fe8b667 ae79bffe-f291-405c-93c7-16e12b3cb422" name="In" id="c8bf4131-5010-479a-a9bc-8a48bbb25240"/>
        <port xsi:type="esdl:OutPort" carrier="83ff3b91-32cc-40d3-953a-3f0b8698866a" connectedTo="55b61a25-081e-4b99-80f1-54ba1718b9b0 c5e6c52b-a174-4f12-8c19-dd1e6f4731b9 77eb449b-aa54-457a-a228-a63efb6d97b4" name="Out" id="ef12cf9c-8391-4488-9a08-1bf09f51cc92"/>
        <geometry xsi:type="esdl:Point" lon="5.399552285671235" lat="52.3911930551951"/>
      </asset>
      <asset xsi:type="esdl:PVInstallation" id="3b735a52-1920-49e8-a4f4-647c722a42e4" name="PVInstallation_Local">
        <port xsi:type="esdl:OutPort" carrier="83ff3b91-32cc-40d3-953a-3f0b8698866a" connectedTo="c8bf4131-5010-479a-a9bc-8a48bbb25240" name="Out" id="be625810-1db5-482e-9274-d8115fe8b667">
          <profile xsi:type="esdl:TimeSeriesProfile" startDateTime="2019-01-01T00:00:00.000000" name="PV profile" values="3.0 3.0 3.0 3.0 3.0 3.0 3.0 3.0 3.0 3.0" id="e9e2a3df-c23c-4bb7-ac86-7342157af729">
            <profileQuantityAndUnit xsi:type="esdl:QuantityAndUnitType" id="a8ce7b69-cb5f-40c6-ba57-c1128c35b051" description="Energy in MJ" physicalQuantity="ENERGY" multiplier="MEGA" unit="JOULE"/>
          </profile>
        </port>
        <geometry xsi:type="esdl:Point" lon="5.400198698043823" lat="52.391456589429765"/>
        <costInformation xsi:type="esdl:CostInformation">
          <marginalCosts xsi:type="esdl:SingleValue" name="PVInstallation_3b73-MarginalCosts" id="e8c91400-86ce-41b0-b445-ed68589961d7"/>
        </costInformation>
      </asset>
      <asset xsi:type="esdl:ElectricityDemand" id="a06177c6-59fc-4286-9e53-09966b30cbc0" name="ElectricityDemand_Local">
        <port xsi:type="esdl:InPort" carrier="83ff3b91-32cc-40d3-953a-3f0b8698866a" connectedTo="ef12cf9c-8391-4488-9a08-1bf09f51cc92" name="In" id="55b61a25-081e-4b99-80f1-54ba1718b9b0">
          <profile xsi:type="esdl:TimeSeriesProfile" startDateTime="2019-01-01T00:00:00.000000" name="E Local demand profile" values="2.0 2.0 2.0 2.0 2.0 2.0 2.0 2.0 2.0 2.0" id="58791b5d-81a4-4626-88d5-c01a13de553d">
            <profileQuantityAndUnit xsi:type="esdl:QuantityAndUnitType" id="200e5ff9-7712-48c0-baa1-bb21acebc5f7" description="Energy in MJ" physicalQuantity="ENERGY" multiplier="MEGA" unit="JOULE"/>
          </profile>
        </port>
        <geometry xsi:type="esdl:Point" lon="5.400069952011109" lat="52.390895145035074"/>
        <costInformation xsi:type="esdl:CostInformation">
          <marginalCosts xsi:type="esdl:SingleValue" name="ElectricityDemand_a061-MarginalCosts" id="d5a9f22a-21c7-40bd-b583-e20292e5ae62"/>
        </costInformation>
      </asset>
      <asset xsi:type="esdl:GenericConversion" controlStrategy="09137773-ac8b-44ab-81bd-41220700f58f" power="10000.0" efficiency="1.0" name="Teruglevering" id="49e9d4e8-b6b0-4691-82af-05ccecddd5ec">
        <port xsi:type="esdl:InPort" carrier="83ff3b91-32cc-40d3-953a-3f0b8698866a" connectedTo="ef12cf9c-8391-4488-9a08-1bf09f51cc92" name="In" id="c5e6c52b-a174-4f12-8c19-dd1e6f4731b9"/>
        <port xsi:type="esdl:OutPort" carrier="60c23a8a-bc0c-4918-82b6-637eb5c1fd76" connectedTo="4030a6d5-6221-4424-8c89-1d15b67e6f00" name="Out" id="4ef4f8e8-d825-41e9-aa92-8c633f552ba1"/>
        <costInformation xsi:type="esdl:CostInformation">
          <marginalCosts xsi:type="esdl:SingleValue" name="GenericConversion_49e9-MarginalCosts" value="1e-7" id="91c17e20-ab02-4fbe-a880-406a87134972"/>
        </costInformation>
        <geometry xsi:type="esdl:Point" lon="5.3989943861961365" lat="52.39126671399055"/>
      </asset>
      <asset xsi:type="esdl:ElectricityNetwork" name="ElectricityNetwork_Global" id="605c47c5-5207-4307-824c-577b28f3f583">
        <port xsi:type="esdl:InPort" carrier="60c23a8a-bc0c-4918-82b6-637eb5c1fd76" connectedTo="41079d9f-0aec-4452-8890-d52a3f14a743 4ef4f8e8-d825-41e9-aa92-8c633f552ba1" name="In" id="4030a6d5-6221-4424-8c89-1d15b67e6f00"/>
        <port xsi:type="esdl:OutPort" carrier="60c23a8a-bc0c-4918-82b6-637eb5c1fd76" connectedTo="8853184e-164c-4263-8c0e-cff32916527a 630059ed-36d0-493a-a170-f148c3ccb7aa" name="Out" id="aed6de20-db31-4639-9666-16b8230dae6b"/>
        <geometry xsi:type="esdl:Point" lon="5.398492813110352" lat="52.391207786963996"/>
      </asset>
      <asset xsi:type="esdl:ElectricityDemand" power="10000.0" name="ElectricityDemand_Global" id="e112f7c4-2200-4fe1-95c9-94180ad6b472">
        <port xsi:type="esdl:InPort" carrier="60c23a8a-bc0c-4918-82b6-637eb5c1fd76" connectedTo="aed6de20-db31-4639-9666-16b8230dae6b" name="In" id="630059ed-36d0-493a-a170-f148c3ccb7aa">
          <profile xsi:type="esdl:TimeSeriesProfile" startDateTime="2019-01-01T00:00:00.000000" name="E global demand profile" values="5.0 5.0 5.0 5.0 5.0 5.0 5.0 5.0 5.0 5.0" id="d6db889e-8b44-4a37-baca-36bd7b8be991">
            <profileQuantityAndUnit xsi:type="esdl:QuantityAndUnitType" id="3cec72f7-8754-43bb-b2fe-489b95da8426" description="Energy in MJ" physicalQuantity="ENERGY" multiplier="MEGA" unit="JOULE"/>
          </profile>
        </port>
        <geometry xsi:type="esdl:Point" lon="5.398109257221223" lat="52.39094097749824"/>
      </asset>
      <asset xsi:type="esdl:GenericConversion" controlStrategy="c0763ec0-cb9a-4d9c-9225-563356f15408" power="10000.0" efficiency="1.0" name="Levering" id="6684cf87-1a7f-4cb2-b74c-4b9e04766b9e">
        <port xsi:type="esdl:InPort" carrier="60c23a8a-bc0c-4918-82b6-637eb5c1fd76" connectedTo="aed6de20-db31-4639-9666-16b8230dae6b" name="In" id="8853184e-164c-4263-8c0e-cff32916527a"/>
        <port xsi:type="esdl:OutPort" carrier="83ff3b91-32cc-40d3-953a-3f0b8698866a" connectedTo="c8bf4131-5010-479a-a9bc-8a48bbb25240" name="Out" id="ae79bffe-f291-405c-93c7-16e12b3cb422"/>
        <costInformation xsi:type="esdl:CostInformation">
          <marginalCosts xsi:type="esdl:SingleValue" name="GenericConversion_6684-MarginalCosts" value="2e-7" id="764206ce-c88d-4bda-aa91-5f66eb3ef83d"/>
        </costInformation>
        <geometry xsi:type="esdl:Point" lon="5.399002432823182" lat="52.391163591642474"/>
      </asset>
      <asset xsi:type="esdl:Battery" capacity="20000000.0" maxDischargeRate="8000.0" controlStrategy="4d4ebf7d-010b-4867-86ec-6da4256995c9" maxChargeRate="8000.0" name="Battery_Local" id="8444618a-3d38-4d43-8b64-60cbf1188e4a">
        <port xsi:type="esdl:InPort" carrier="83ff3b91-32cc-40d3-953a-3f0b8698866a" connectedTo="ef12cf9c-8391-4488-9a08-1bf09f51cc92" name="In" id="77eb449b-aa54-457a-a228-a63efb6d97b4"/>
        <geometry xsi:type="esdl:Point" lon="5.399385988712312" lat="52.39084767564802"/>
      </asset>
    </area>
  </instance>
  <services xsi:type="esdl:Services" id="2da223b5-9f85-406c-8449-425572cf913f">
    <service xsi:type="esdl:DrivenByDemand" energyAsset="6684cf87-1a7f-4cb2-b74c-4b9e04766b9e" name="DrivenByDemand for GenericConversion_6684" id="c0763ec0-cb9a-4d9c-9225-563356f15408" outPort="ae79bffe-f291-405c-93c7-16e12b3cb422"/>
    <service xsi:type="esdl:DrivenBySupply" energyAsset="49e9d4e8-b6b0-4691-82af-05ccecddd5ec" name="DrivenBySupply for GenericConversion_49e9" inPort="c5e6c52b-a174-4f12-8c19-dd1e6f4731b9" id="09137773-ac8b-44ab-81bd-41220700f58f"/>
    <service xsi:type="esdl:StorageStrategy" energyAsset="8444618a-3d38-4d43-8b64-60cbf1188e4a" name="StorageStrategy for Battery_Local" id="4d4ebf7d-010b-4867-86ec-6da4256995c9">
      <marginalDischargeCosts xsi:type="esdl:SingleValue" name="marginalChargeCosts for Battery_Local" value="3e-7" id="9f1ae76e-44ce-41f0-8c97-53baab7a40ab"/>
      <marginalChargeCosts xsi:type="esdl:SingleValue" name="marginalChargeCosts for Battery_Local" value="1e-7" id="08e6c657-91ca-4405-9aff-8f4f6f3a32a4"/>
    </service>
  </services>
  <energySystemInformation xsi:type="esdl:EnergySystemInformation" id="fc0047bd-abec-4cef-905e-6aa5716c0925">
    <carriers xsi:type="esdl:Carriers" id="3bd5fd61-4ea9-4d95-ae12-36564b91958f">
      <carrier xsi:type="esdl:ElectricityCommodity" name="Grid E" id="60c23a8a-bc0c-4918-82b6-637eb5c1fd76">
        <cost xsi:type="esdl:TimeSeriesProfile" startDateTime="2019-01-01T00:00:00.000000" name="Electricity prices" values="0.05 0.1 0.3 0.3 0.05 0.05 0.1 0.3 0.3 0.05" id="3378e5ca-5d64-45e8-831c-a4cfd16ecdef">
          <profileQuantityAndUnit xsi:type="esdl:QuantityAndUnitType" id="b2cbbafe-7196-41a7-aa69-3f1e1cfa4199" description="Cost in EUR/MJ" perUnit="JOULE" perMultiplier="MEGA" unit="EURO" physicalQuantity="COST"/>
        </cost>
      </carrier>
      <carrier xsi:type="esdl:ElectricityCommodity" name="Local E" id="83ff3b91-32cc-40d3-953a-3f0b8698866a"/>
    </carriers>
  </energySystemInformation>
</esdl:EnergySystem>
