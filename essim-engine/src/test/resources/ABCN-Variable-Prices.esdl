<?xml version='1.0' encoding='UTF-8'?>
<esdl:EnergySystem xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:esdl="http://www.tno.nl/esdl" name="Untitled EnergySystem" id="2ee64f26-6549-4088-a6b8-a03489bc6025" description="" esdlVersion="v2303" version="2">
  <services xsi:type="esdl:Services" id="2da223b5-9f85-406c-8449-425572cf913f">
    <service xsi:type="esdl:DrivenBySupply" name="DrivenBySupply for Electrolyzer_a114" energyAsset="a1147c18-c18b-4535-959b-ac447e1a0249" id="19fbf374-216e-4ce6-82f6-3942e6ccdff7" inPort="a14b7350-f3ed-43ec-b9de-fdf3c06010ed"/>
  </services>
  <instance xsi:type="esdl:Instance" id="b510fb05-876c-41ae-840e-19bed0c6f786" name="Untitled Instance">
    <area xsi:type="esdl:Area" id="5b57a707-7698-44a6-af66-074869dbdd7f" name="Untitled Area">
      <asset xsi:type="esdl:Import" power="10000.0" name="Import_8bfe" id="8bfe76e5-0eda-4148-86a5-790aa3f0c8a3">
        <geometry xsi:type="esdl:Point" lat="52.391261977967304" lon="5.39888446707825"/>
        <port xsi:type="esdl:OutPort" carrier="f4f43e41-34d5-42f7-98e8-5a1c489af439" connectedTo="2ed0db6b-85d7-4d52-95e6-4629c45b3094" name="Out" id="261d1e69-a3a1-4b1c-b4bd-8ba06e97dbd4"/>
        <costInformation xsi:type="esdl:CostInformation">
          <marginalCosts xsi:type="esdl:SingleValue" name="Import_8bfe-MarginalCosts" id="c6772c48-935b-4d14-97d7-251fbda02d18" value="0.9"/>
        </costInformation>
      </asset>
      <asset xsi:type="esdl:Import" power="10000.0" name="Import_a121" id="a1211753-1cda-4760-b9ce-972c8925fca0">
        <geometry xsi:type="esdl:Point" lat="52.39103413905114" lon="5.399423437826481"/>
        <port xsi:type="esdl:OutPort" carrier="83ff3b91-32cc-40d3-953a-3f0b8698866a" connectedTo="c8bf4131-5010-479a-a9bc-8a48bbb25240" name="Out" id="41079d9f-0aec-4452-8890-d52a3f14a743"/>
        <costInformation xsi:type="esdl:CostInformation">
          <marginalCosts xsi:type="esdl:SingleValue" name="Import_a121-MarginalCosts" id="31ee995f-80da-438b-8858-90cdfae34940" value="0.9"/>
        </costInformation>
      </asset>
      <asset xsi:type="esdl:GasNetwork" name="GasNetwork_3ae6" id="3ae648fa-d889-4f4c-9059-6dcc2bacd115">
        <geometry xsi:type="esdl:Point" lat="52.39115798293423" lon="5.399007662883607"/>
        <port xsi:type="esdl:InPort" carrier="f4f43e41-34d5-42f7-98e8-5a1c489af439" connectedTo="261d1e69-a3a1-4b1c-b4bd-8ba06e97dbd4 7c1f0cbb-a62f-44d8-8967-1da542a9cdb2" name="In" id="2ed0db6b-85d7-4d52-95e6-4629c45b3094"/>
        <port xsi:type="esdl:OutPort" carrier="f4f43e41-34d5-42f7-98e8-5a1c489af439" connectedTo="300349ba-129b-4317-b679-720785107b35" name="Out" id="2a091946-2759-4782-9aca-63599b5728b7"/>
      </asset>
      <asset xsi:type="esdl:ElectricityNetwork" name="ElectricityNetwork_2904" id="2904c4de-aaa4-4688-b01e-95a06f2ee089">
        <geometry xsi:type="esdl:Point" lat="52.39115540166964" lon="5.399495839227915"/>
        <port xsi:type="esdl:InPort" carrier="83ff3b91-32cc-40d3-953a-3f0b8698866a" connectedTo="be625810-1db5-482e-9274-d8115fe8b667 41079d9f-0aec-4452-8890-d52a3f14a743" name="In" id="c8bf4131-5010-479a-a9bc-8a48bbb25240"/>
        <port xsi:type="esdl:OutPort" carrier="83ff3b91-32cc-40d3-953a-3f0b8698866a" connectedTo="a14b7350-f3ed-43ec-b9de-fdf3c06010ed 55b61a25-081e-4b99-80f1-54ba1718b9b0" name="Out" id="ef12cf9c-8391-4488-9a08-1bf09f51cc92"/>
      </asset>
      <asset xsi:type="esdl:GasDemand" name="GasDemand_0be4" id="0be492ce-d8d1-4b43-8c6c-431b30b16a54">
        <geometry xsi:type="esdl:Point" lat="52.39105214969493" lon="5.398871039662151"/>
        <port xsi:type="esdl:InPort" carrier="f4f43e41-34d5-42f7-98e8-5a1c489af439" connectedTo="2a091946-2759-4782-9aca-63599b5728b7" name="In" id="300349ba-129b-4317-b679-720785107b35">
          <profile xsi:type="esdl:TimeSeriesProfile" id="3f8f623c-2b09-4fa6-8523-526650c7ee69" name="H2 Demand profile" values="4.0 4.0 4.0 4.0 4.0" startDateTime="2019-01-01T00:00:00.000000">
            <profileQuantityAndUnit xsi:type="esdl:QuantityAndUnitType" description="Energy in MJ" id="1924e63d-fbb6-4554-a919-5a733825f38e" unit="JOULE" physicalQuantity="ENERGY" multiplier="MEGA"/>
          </profile>
        </port>
      </asset>
      <asset xsi:type="esdl:PVInstallation" name="PVInstallation_3b73" id="3b735a52-1920-49e8-a4f4-647c722a42e4">
        <geometry xsi:type="esdl:Point" lat="52.39126196440932" lon="5.399704128199586"/>
        <port xsi:type="esdl:OutPort" carrier="83ff3b91-32cc-40d3-953a-3f0b8698866a" connectedTo="c8bf4131-5010-479a-a9bc-8a48bbb25240" name="Out" id="be625810-1db5-482e-9274-d8115fe8b667">
          <profile xsi:type="esdl:TimeSeriesProfile" id="e9e2a3df-c23c-4bb7-ac86-7342157af729" name="PV profile" values="0.0 0.0 0.0 0.0 0.0" startDateTime="2019-01-01T00:00:00.000000">
            <profileQuantityAndUnit xsi:type="esdl:QuantityAndUnitType" description="Energy in MJ" id="a8ce7b69-cb5f-40c6-ba57-c1128c35b051" unit="JOULE" physicalQuantity="ENERGY" multiplier="MEGA"/>
          </profile>
        </port>
      </asset>
      <asset xsi:type="esdl:ElectricityDemand" name="ElectricityDemand_a061" id="a06177c6-59fc-4286-9e53-09966b30cbc0">
        <geometry xsi:type="esdl:Point" lat="52.39102927533107" lon="5.399772066686501"/>
        <port xsi:type="esdl:InPort" carrier="83ff3b91-32cc-40d3-953a-3f0b8698866a" connectedTo="ef12cf9c-8391-4488-9a08-1bf09f51cc92" name="In" id="55b61a25-081e-4b99-80f1-54ba1718b9b0">
          <profile xsi:type="esdl:TimeSeriesProfile" id="58791b5d-81a4-4626-88d5-c01a13de553d" name="E Demand profile" values="2.0 2.0 2.0 2.0 2.0 " startDateTime="2019-01-01T00:00:00.000000">
            <profileQuantityAndUnit xsi:type="esdl:QuantityAndUnitType" description="Energy in MJ" id="200e5ff9-7712-48c0-baa1-bb21acebc5f7" unit="JOULE" physicalQuantity="ENERGY" multiplier="MEGA"/>
          </profile>
        </port>
      </asset>
      <asset xsi:type="esdl:Electrolyzer" controlStrategy="19fbf374-216e-4ce6-82f6-3942e6ccdff7" efficiency="0.5" power="5000.0" name="Electrolyzer_a114" id="a1147c18-c18b-4535-959b-ac447e1a0249">
        <geometry xsi:type="esdl:Point" lat="52.39122752083154" lon="5.399249142729886"/>
        <port xsi:type="esdl:InPort" carrier="83ff3b91-32cc-40d3-953a-3f0b8698866a" connectedTo="ef12cf9c-8391-4488-9a08-1bf09f51cc92" name="In" id="a14b7350-f3ed-43ec-b9de-fdf3c06010ed"/>
        <port xsi:type="esdl:OutPort" carrier="f4f43e41-34d5-42f7-98e8-5a1c489af439" connectedTo="2ed0db6b-85d7-4d52-95e6-4629c45b3094" name="Out" id="7c1f0cbb-a62f-44d8-8967-1da542a9cdb2"/>
      </asset>
    </area>
  </instance>
  <energySystemInformation xsi:type="esdl:EnergySystemInformation" id="fc0047bd-abec-4cef-905e-6aa5716c0925">
    <carriers xsi:type="esdl:Carriers" id="3bd5fd61-4ea9-4d95-ae12-36564b91958f">
      <carrier xsi:type="esdl:GasCommodity" id="f4f43e41-34d5-42f7-98e8-5a1c489af439" name="Hydrogen">
        <cost xsi:type="esdl:TimeSeriesProfile" values="0.3 0.3 0.3 0.3 0.3" id="bc4078ca-26d6-46d4-9365-9558bb097ad7" name="Gas prices"  startDateTime="2019-01-01T00:00:00.000000">
          <profileQuantityAndUnit xsi:type="esdl:QuantityAndUnitType" id="75d2da04-10c5-4837-98c5-3e287e2843ef" description="Cost in EUR/MJ" perUnit="JOULE" perMultiplier="MEGA" unit="EURO" physicalQuantity="COST"/>
        </cost>
        </carrier>
      <carrier xsi:type="esdl:ElectricityCommodity" id="83ff3b91-32cc-40d3-953a-3f0b8698866a" name="Electricity">
        <cost xsi:type="esdl:TimeSeriesProfile" values="0.1 0.2 0.3 0.2 0.1"  name="Electricity prices" id="3378e5ca-5d64-45e8-831c-a4cfd16ecdef" startDateTime="2019-01-01T00:00:00.000000">
          <profileQuantityAndUnit xsi:type="esdl:QuantityAndUnitType" id="b2cbbafe-7196-41a7-aa69-3f1e1cfa4199" description="Cost in EUR/MJ" perUnit="JOULE" perMultiplier="MEGA" unit="EURO" physicalQuantity="COST"/>
        </cost>
      </carrier>
    </carriers>
  </energySystemInformation>
</esdl:EnergySystem>
