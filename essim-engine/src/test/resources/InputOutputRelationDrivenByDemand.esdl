<?xml version='1.0' encoding='UTF-8'?>
<esdl:EnergySystem xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:esdl="http://www.tno.nl/esdl" version="3" id="caedd7cc-9074-4c0f-bae3-f533bd3483b5" description="" esdlVersion="v2303" name="Untitled EnergySystem">
  <instance xsi:type="esdl:Instance" id="93534bf7-f05b-4021-9ec2-8c9c9c1c1623" name="Untitled Instance">
    <area xsi:type="esdl:Area" name="Untitled Area" id="82133d54-fbdf-4ceb-81f0-c93837ab20a4">
      <asset xsi:type="esdl:GenericConversion" id="b57d565c-6fca-4eba-a7d6-edc7280111ff" power="10000.0" name="GenericConversion_b57d">
        <behaviour xsi:type="esdl:InputOutputRelation" id="71616ebd-69aa-4c02-8b6e-62853d146f1c" name="NewInputOutputRelation" mainPort="b1248ffe-7670-4027-bfa0-0b970fd5568c">
          <mainPortRelation xsi:type="esdl:PortRelation" ratio="0.85" port="ce36b39b-f0be-464a-b40a-3d6b11b37e6e"/>
          <mainPortRelation xsi:type="esdl:PortRelation" ratio="0.3" port="b280ab8d-d780-4e2a-80d6-97944a9e8ba2"/>
          <mainPortRelation xsi:type="esdl:PortRelation" ratio="0.2" port="da33e1d5-170f-42b7-b1e9-facfd9555e48"/>
        </behaviour>
        <port xsi:type="esdl:InPort" id="ce36b39b-f0be-464a-b40a-3d6b11b37e6e" carrier="c9c41cee-b7d6-415f-9823-bcf8cd5dda2e" name="In" connectedTo="1ff6b0ae-997e-4680-b0aa-fb177fbddb06"/>
        <port xsi:type="esdl:OutPort" id="b1248ffe-7670-4027-bfa0-0b970fd5568c" connectedTo="f7614a1f-b1ed-4976-940c-da9c91252f83" carrier="17f768ce-cc89-45ef-aa03-137923aff402" name="Out"/>
        <port xsi:type="esdl:OutPort" id="b280ab8d-d780-4e2a-80d6-97944a9e8ba2" connectedTo="9ef56486-de07-4f59-8547-4a74b2c526c3" carrier="fecad2be-1496-43d8-9361-f34aba96b562" name="OutExtra"/>
        <port xsi:type="esdl:OutPort" id="da33e1d5-170f-42b7-b1e9-facfd9555e48" connectedTo="4017600d-70fd-4efe-a62f-9758350f2b86" carrier="c84fc45f-df47-4173-a39e-9cd781baa0c3" name="OutExtra2"/>
        <geometry xsi:type="esdl:Point" lon="4.478495121002198" lat="52.17909969615957"/>
      </asset>
      <asset xsi:type="esdl:ElectricityDemand" id="3dc95e6c-1773-4b0e-b36d-fb4a654faecb" name="ElectricityDemand">
        <port xsi:type="esdl:InPort" connectedTo="b1248ffe-7670-4027-bfa0-0b970fd5568c" id="f7614a1f-b1ed-4976-940c-da9c91252f83" carrier="17f768ce-cc89-45ef-aa03-137923aff402" name="In">
          <profile xsi:type="esdl:TimeSeriesProfile" name="E Demand profile" id="e9e2a3df-c23c-4bb7-ac86-7342157af729" startDateTime="2019-01-01T00:00:00.000000" values="12.0 0.0 1.0 2.0 3.0 5.0 7.0 12.0 3.0 0.0 1.0 0.0">
            <profileQuantityAndUnit xsi:type="esdl:QuantityAndUnitReference" reference="12c481c0-f81e-49b6-9767-90457684d24a"/>
          </profile>
        </port>
        <geometry xsi:type="esdl:Point" lon="4.478694945573808" lat="52.17922387218116"/>
      </asset>
      <asset xsi:type="esdl:Import" id="59a7f30f-d203-4ec4-b656-d2973968b20c" power="100000.0" name="Import_59a7">
        <port xsi:type="esdl:OutPort" id="1ff6b0ae-997e-4680-b0aa-fb177fbddb06" connectedTo="ce36b39b-f0be-464a-b40a-3d6b11b37e6e" carrier="c9c41cee-b7d6-415f-9823-bcf8cd5dda2e" name="Out"/>
        <geometry xsi:type="esdl:Point" lon="4.478292614221574" CRS="WGS84" lat="52.17896071728716"/>
      </asset>
      <asset xsi:type="esdl:Export" id="7bba343f-becb-4c52-bc84-62509bdb4cc6" power="20000.0" name="H2_Export">
        <port xsi:type="esdl:InPort" id="9ef56486-de07-4f59-8547-4a74b2c526c3" carrier="fecad2be-1496-43d8-9361-f34aba96b562" name="In" connectedTo="b280ab8d-d780-4e2a-80d6-97944a9e8ba2"/>
        <geometry xsi:type="esdl:Point" lon="4.478807598352433" CRS="WGS84" lat="52.1791531494564"/>
      </asset>
      <asset xsi:type="esdl:Export" id="e99ea61a-7b2a-4ddc-95e1-4a20cc57a530" power="20000.0" name="HeatExport">
        <port xsi:type="esdl:InPort" id="4017600d-70fd-4efe-a62f-9758350f2b86" carrier="c84fc45f-df47-4173-a39e-9cd781baa0c3" name="In" connectedTo="da33e1d5-170f-42b7-b1e9-facfd9555e48"/>
        <geometry xsi:type="esdl:Point" lon="4.478808939456941" CRS="WGS84" lat="52.17905693347585"/>
      </asset>
    </area>
  </instance>
  <services xsi:type="esdl:Services">
    <service xsi:type="esdl:DrivenBySupply" id="3f514b6a-091f-4ed3-9f80-b8f6b3382186" name="DrivenBySupply for GenericConversion_b57d" inPort="ce36b39b-f0be-464a-b40a-3d6b11b37e6e"/>
  </services>
  <energySystemInformation xsi:type="esdl:EnergySystemInformation" id="8c6a9a19-08dd-473b-96b3-d18b30feb281">
    <carriers xsi:type="esdl:Carriers" id="2a53d35c-1735-48c5-94f8-546b29b6afdd">
      <carrier xsi:type="esdl:ElectricityCommodity" voltage="10000.0" name="Electricity" id="17f768ce-cc89-45ef-aa03-137923aff402"/>
      <carrier xsi:type="esdl:GasCommodity" name="Gas" id="c9c41cee-b7d6-415f-9823-bcf8cd5dda2e"/>
      <carrier xsi:type="esdl:HeatCommodity" name="Heat" id="c84fc45f-df47-4173-a39e-9cd781baa0c3"/>
      <carrier xsi:type="esdl:GasCommodity" name="H2" id="fecad2be-1496-43d8-9361-f34aba96b562"/>
    </carriers>
    <quantityAndUnits xsi:type="esdl:QuantityAndUnits" id="93ce1d5f-7f85-46f4-926b-aef443312d91">
      <quantityAndUnit xsi:type="esdl:QuantityAndUnitType" description="Energy in kWh" physicalQuantity="ENERGY" id="12c481c0-f81e-49b6-9767-90457684d24a" multiplier="KILO" unit="WATTHOUR"/>
    </quantityAndUnits>
  </energySystemInformation>
</esdl:EnergySystem>
