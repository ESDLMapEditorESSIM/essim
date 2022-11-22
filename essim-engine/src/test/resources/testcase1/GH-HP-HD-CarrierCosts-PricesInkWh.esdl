<?xml version='1.0' encoding='UTF-8'?>
<esdl:EnergySystem xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:esdl="http://www.tno.nl/esdl" id="175f2b97-4e0e-469f-a2dc-932140ed47ef" name="Untitled EnergySystem" description="" esdlVersion="v2207" version="4">
  <energySystemInformation xsi:type="esdl:EnergySystemInformation" id="ef353965-0b2f-46fc-9231-bca69172a027">
    <carriers xsi:type="esdl:Carriers" id="92bfb07e-e43b-4ead-a2c8-d2f322ba8437">
      <carrier xsi:type="esdl:ElectricityCommodity" id="88b89c02-9d99-4548-afe6-ff48ba4042dd" name="Elek">
        <cost xsi:type="esdl:TimeSeriesProfile" id="879629f0-7889-43ce-bbee-404b86f0d4bd" values="0.1850816 0.1850695 0.1857713 0.1858681 0.1883123 0.1970969 0.2011625 0.199287 0.2168199 0.2116653 0.1930918 0.1889173 0.1889173 0.1889173 0.1935153 0.1953303 0.2038245 0.2076602 0.2056395 0.1965403 0.1925836 0.1930071 0.1853962 0.1857713" startDateTime="2019-02-19T00:00:00.000000" name="Elek-prijs">
          <profileQuantityAndUnit xsi:type="esdl:QuantityAndUnitType" physicalQuantity="COST" id="29c2e656-bec3-478e-86af-5a3f7d53e57f" description="EUR / kW" perMultiplier="KILO" perUnit="WATT" unit="EURO"/>
        </cost>
      </carrier>
      <carrier xsi:type="esdl:GasCommodity" id="3cf3ac50-fa7f-4c03-b8df-42c68dd31bea" name="Gas">
        <cost xsi:type="esdl:TimeSeriesProfile" id="09492664-bcc0-4f0c-aa6a-0b4b35bb3132" values="0.8018 0.8018 0.8018 0.8018 0.8018 0.8018 0.8018 0.8018 0.8018 0.8018 0.8018 0.8018 0.8018 0.8018 0.8018 0.8018 0.8018 0.8018 0.8018 0.8018 0.8018 0.8018 0.8018 0.8018" startDateTime="2019-02-19T00:00:00.000000" name="Gas-prijs">
          <profileQuantityAndUnit xsi:type="esdl:QuantityAndUnitType" physicalQuantity="COST" id="e18e285d-1dbf-4623-bd37-9c4ffcfedac7" description="EUR / m3" perUnit="CUBIC_METRE" unit="EURO"/>
        </cost>
      </carrier>
      <carrier xsi:type="esdl:HeatCommodity" id="ce72e734-86d9-456b-abfa-fcc2478b4045" name="Heat"/>
    </carriers>
  </energySystemInformation>
  <instance xsi:type="esdl:Instance" id="5bda754c-3551-4305-8d5b-f561e5190768" name="Untitled Instance">
    <area xsi:type="esdl:Area" id="0d63b5c0-29d3-4546-bf5a-9e74def3211e" name="Untitled Area">
      <asset xsi:type="esdl:Import" name="Import_263a" id="263adb1b-1529-4db5-b995-b3016a84ef70" power="100000.0">
        <geometry xsi:type="esdl:Point" CRS="WGS84" lat="52.17520624111468" lon="4.9134206771850595"/>
        <port xsi:type="esdl:OutPort" id="ecb685a9-0da1-4e5c-a71e-1630d83d32d4" carrier="3cf3ac50-fa7f-4c03-b8df-42c68dd31bea" connectedTo="3f23353f-4813-46f0-9948-76c7aa0cafac" name="Out"/>
      </asset>
      <asset xsi:type="esdl:Import" name="Import_cb54" id="cb540053-94c7-4bd3-8a06-e6037b88a1ad" power="100000.0">
        <geometry xsi:type="esdl:Point" CRS="WGS84" lat="52.174827844612494" lon="4.913375079631806"/>
        <port xsi:type="esdl:OutPort" id="270c8fe1-3f21-4163-81b7-d692bc2c5f69" carrier="88b89c02-9d99-4548-afe6-ff48ba4042dd" connectedTo="c79aafe9-7765-4512-a349-9ebf1c7baa94" name="Out"/>
      </asset>
      <asset xsi:type="esdl:GasHeater" name="GasHeater_381e" id="381ef11a-b33c-4c9c-b39f-a5be3385d94b" efficiency="0.95" power="100000.0">
        <geometry xsi:type="esdl:Point" CRS="WGS84" lat="52.17520295072418" lon="4.9140456318855295"/>
        <port xsi:type="esdl:InPort" id="3f23353f-4813-46f0-9948-76c7aa0cafac" carrier="3cf3ac50-fa7f-4c03-b8df-42c68dd31bea" connectedTo="ecb685a9-0da1-4e5c-a71e-1630d83d32d4" name="In"/>
        <port xsi:type="esdl:OutPort" id="ecbda53e-db1b-4e4d-81eb-2836547479a1" carrier="ce72e734-86d9-456b-abfa-fcc2478b4045" connectedTo="861ed413-dc60-4a62-b714-8ca6db4dc30e" name="Out"/>
      </asset>
      <asset xsi:type="esdl:HeatPump" name="HeatPump_c314" id="c314b0a1-e8f2-4aba-b054-13b96df6f957" efficiency="1.0" power="100000.0" COP="2.0">
        <geometry xsi:type="esdl:Point" lon="4.914002716541291" lat="52.174818289639276"/>
        <port xsi:type="esdl:InPort" id="c79aafe9-7765-4512-a349-9ebf1c7baa94" carrier="88b89c02-9d99-4548-afe6-ff48ba4042dd" connectedTo="270c8fe1-3f21-4163-81b7-d692bc2c5f69" name="In"/>
        <port xsi:type="esdl:OutPort" id="b22c86de-69ef-4a2d-935a-bd7357cf0fe9" carrier="ce72e734-86d9-456b-abfa-fcc2478b4045" connectedTo="861ed413-dc60-4a62-b714-8ca6db4dc30e" name="Out"/>
      </asset>
      <asset xsi:type="esdl:HeatingDemand" name="HeatingDemand_1865" id="18656765-dca5-49b5-b82f-b262b1347411">
        <geometry xsi:type="esdl:Point" CRS="WGS84" lat="52.175007172051735" lon="4.914673268795014"/>
        <port xsi:type="esdl:InPort" id="861ed413-dc60-4a62-b714-8ca6db4dc30e" carrier="ce72e734-86d9-456b-abfa-fcc2478b4045" connectedTo="ecbda53e-db1b-4e4d-81eb-2836547479a1 b22c86de-69ef-4a2d-935a-bd7357cf0fe9" name="In">
          <profile xsi:type="esdl:SingleValue" value="10.0" id="e1e73ebf-ebd5-4152-9577-03cecc04936b">
            <profileQuantityAndUnit xsi:type="esdl:QuantityAndUnitType" physicalQuantity="ENERGY" multiplier="MEGA" id="deb2369b-5b09-4d28-a828-43de3e743d3f" description="" unit="JOULE"/>
          </profile>
        </port>
      </asset>
    </area>
  </instance>
</esdl:EnergySystem>
