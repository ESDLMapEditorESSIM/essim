<?xml version='1.0' encoding='UTF-8'?>
<esdl:EnergySystem xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:esdl="http://www.tno.nl/esdl" id="caedd7cc-9074-4c0f-bae3-f533bd3483b5" name="Untitled EnergySystem" esdlVersion="v2303" description="" version="1">
  <energySystemInformation xsi:type="esdl:EnergySystemInformation" id="8c6a9a19-08dd-473b-96b3-d18b30feb281">
    <carriers xsi:type="esdl:Carriers" id="2a53d35c-1735-48c5-94f8-546b29b6afdd">
      <carrier xsi:type="esdl:ElectricityCommodity" name="Electricity" voltage="10000.0" id="17f768ce-cc89-45ef-aa03-137923aff402"/>
      <carrier xsi:type="esdl:GasCommodity" name="Gas" id="c9c41cee-b7d6-415f-9823-bcf8cd5dda2e"/>
      <carrier xsi:type="esdl:HeatCommodity" name="Heat" id="c84fc45f-df47-4173-a39e-9cd781baa0c3"/>
      <carrier xsi:type="esdl:GasCommodity" name="H2" id="fecad2be-1496-43d8-9361-f34aba96b562"/>
    </carriers>
  </energySystemInformation>
  <instance xsi:type="esdl:Instance" name="Untitled Instance" id="93534bf7-f05b-4021-9ec2-8c9c9c1c1623">
    <area xsi:type="esdl:Area" id="82133d54-fbdf-4ceb-81f0-c93837ab20a4" name="Untitled Area">
      <asset xsi:type="esdl:GenericConversion" id="b57d565c-6fca-4eba-a7d6-edc7280111ff" name="GenericConversion_b57d" controlStrategy="3f514b6a-091f-4ed3-9f80-b8f6b3382186">
        <behaviour xsi:type="esdl:InputOutputRelation" mainPort="ce36b39b-f0be-464a-b40a-3d6b11b37e6e" name="NewInputOutputRelation" id="71616ebd-69aa-4c02-8b6e-62853d146f1c">
          <mainPortRelation xsi:type="esdl:PortRelation" ratio="0.5" port="b1248ffe-7670-4027-bfa0-0b970fd5568c"/>
          <mainPortRelation xsi:type="esdl:PortRelation" ratio="0.3" port="b280ab8d-d780-4e2a-80d6-97944a9e8ba2"/>
          <mainPortRelation xsi:type="esdl:PortRelation" ratio="0.8" port="da33e1d5-170f-42b7-b1e9-facfd9555e48"/>
        </behaviour>
        <geometry xsi:type="esdl:Point" lon="4.478495121002198" lat="52.17909969615957"/>
        <port xsi:type="esdl:InPort" carrier="c9c41cee-b7d6-415f-9823-bcf8cd5dda2e" connectedTo="39e2414c-893f-4964-933f-8161dce28284" name="In" id="ce36b39b-f0be-464a-b40a-3d6b11b37e6e"/>
        <port xsi:type="esdl:OutPort" carrier="17f768ce-cc89-45ef-aa03-137923aff402" connectedTo="f7614a1f-b1ed-4976-940c-da9c91252f83" name="Out" id="b1248ffe-7670-4027-bfa0-0b970fd5568c"/>
        <port xsi:type="esdl:OutPort" carrier="fecad2be-1496-43d8-9361-f34aba96b562" connectedTo="e6227c9f-9d2b-442a-9171-12c4deeb96da" name="OutExtra" id="b280ab8d-d780-4e2a-80d6-97944a9e8ba2"/>
        <port xsi:type="esdl:OutPort" carrier="c84fc45f-df47-4173-a39e-9cd781baa0c3" connectedTo="542d3c9a-86e4-4569-aa78-7767d91e36c8" name="OutExtra2" id="da33e1d5-170f-42b7-b1e9-facfd9555e48"/>
      </asset>
      <asset xsi:type="esdl:GasProducer" id="8b642c75-3089-4c09-b32c-9e09e029cbbc" name="GasProducer_8b64">
        <geometry xsi:type="esdl:Point" lon="4.478283813223244" lat="52.17897068841896"/>
        <port xsi:type="esdl:OutPort" carrier="c9c41cee-b7d6-415f-9823-bcf8cd5dda2e" connectedTo="ce36b39b-f0be-464a-b40a-3d6b11b37e6e" name="Out" id="39e2414c-893f-4964-933f-8161dce28284"/>
      </asset>
      <asset xsi:type="esdl:ElectricityDemand" id="3dc95e6c-1773-4b0e-b36d-fb4a654faecb" name="ElectricityDemand_3dc9">
        <geometry xsi:type="esdl:Point" lon="4.478576928377152" lat="52.17920865858126" CRS="WGS84"/>
        <port xsi:type="esdl:InPort" carrier="17f768ce-cc89-45ef-aa03-137923aff402" connectedTo="b1248ffe-7670-4027-bfa0-0b970fd5568c" name="In" id="f7614a1f-b1ed-4976-940c-da9c91252f83"/>
      </asset>
      <asset xsi:type="esdl:GasDemand" id="147ed282-e818-45d6-8c42-fd608421d085" name="GasDemand_147e">
        <geometry xsi:type="esdl:Point" lon="4.478684887290002" lat="52.17916301775033" CRS="WGS84"/>
        <port xsi:type="esdl:InPort" carrier="fecad2be-1496-43d8-9361-f34aba96b562" connectedTo="b280ab8d-d780-4e2a-80d6-97944a9e8ba2" name="In" id="e6227c9f-9d2b-442a-9171-12c4deeb96da"/>
      </asset>
      <asset xsi:type="esdl:HeatingDemand" id="be26b2fb-b099-444a-b5c4-8b15a927a004" name="HeatingDemand_be26">
        <geometry xsi:type="esdl:Point" lon="4.47870969772339" lat="52.17905940055486"/>
        <port xsi:type="esdl:InPort" carrier="c84fc45f-df47-4173-a39e-9cd781baa0c3" connectedTo="da33e1d5-170f-42b7-b1e9-facfd9555e48" name="In" id="542d3c9a-86e4-4569-aa78-7767d91e36c8"/>
      </asset>
    </area>
  </instance>
  <services xsi:type="esdl:Services">
    <service xsi:type="esdl:DrivenBySupply" id="3f514b6a-091f-4ed3-9f80-b8f6b3382186" energyAsset="b57d565c-6fca-4eba-a7d6-edc7280111ff" inPort="ce36b39b-f0be-464a-b40a-3d6b11b37e6e" name="DrivenBySupply for GenericConversion_b57d"/>
  </services>
</esdl:EnergySystem>
