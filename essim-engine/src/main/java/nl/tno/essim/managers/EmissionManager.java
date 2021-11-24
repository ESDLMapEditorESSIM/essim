/**
 *  This work is based on original code developed and copyrighted by TNO 2020. 
 *  Subsequent contributions are licensed to you by the developers of such code and are
 *  made available to the Project under one or several contributor license agreements.
 *
 *  This work is licensed to you under the Apache License, Version 2.0.
 *  You may obtain a copy of the license at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Contributors:
 *      TNO         - Initial implementation
 *  Manager:
 *      TNO
 */

package nl.tno.essim.managers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import esdl.Carrier;
import esdl.EnergyAsset;
import esdl.EnergyCarrier;
import esdl.InPort;
import esdl.OutPort;
import esdl.Port;
import esdl.Producer;
import esdl.RenewableTypeEnum;
import esdl.Storage;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import nl.tno.essim.commons.Commons;
import nl.tno.essim.commons.ISimulationManager;
import nl.tno.essim.commons.Simulatable;
import nl.tno.essim.commons.SimulationStatus;
import nl.tno.essim.observation.IObservationManager;
import nl.tno.essim.observation.IObservationProvider;
import nl.tno.essim.observation.Observation;
import nl.tno.essim.time.EssimTime;

@Slf4j
public class EmissionManager implements Simulatable, IObservationProvider {

	private static final double eps = 1.0e-5;
	private static HashMap<String, EmissionManager> instanceMap = new HashMap<String, EmissionManager>();
	private IObservationManager observationManager;
	private HashMap<String, List<ConsumerProducerPair>> consumerMap;
	private HashMap<String, List<AssetEnergyPair>> producerMap;
	private ConcurrentHashMap<AssetEnergyPair, List<AssetEnergyPair>> consumerProducerMap;
	private HashMap<String, Boolean> networkError;
	private boolean overallError;
	private boolean enable;

	public synchronized static EmissionManager getInstance(String simulationId) {
		if (instanceMap.containsKey(simulationId)) {
			return instanceMap.get(simulationId);
		}
		EmissionManager newinstance = new EmissionManager();
		instanceMap.put(simulationId, newinstance);
		return newinstance;
	}

	private EmissionManager() {
		String enableString = System.getenv("EMISSION_MANAGER_ENABLE");
		if (enableString == null) {
			enable = false;
		} else if (enableString.trim().equalsIgnoreCase("true")) {
			enable = true;
		} else {
			enable = false;
		}
		initialise();
	}

	private synchronized void initialise() {
		consumerMap = new HashMap<String, List<ConsumerProducerPair>>();
		producerMap = new HashMap<String, List<AssetEnergyPair>>();
		consumerProducerMap = new ConcurrentHashMap<AssetEnergyPair, List<AssetEnergyPair>>();
		networkError = new HashMap<String, Boolean>();
		overallError = false;
	}

	public synchronized void addConsumer(String networkId, EnergyAsset asset, double energy) {
		if (!enable) {
			return;
		}

		List<ConsumerProducerPair> cppList = consumerMap.get(networkId);

		if (cppList == null) {
			cppList = new ArrayList<ConsumerProducerPair>();
		}

		ConsumerProducerPair cpp = new ConsumerProducerPair();
		cpp.setConsumer(new AssetEnergyPair(asset, energy));

		cppList.add(cpp);
		consumerMap.put(networkId, cppList);
	}

	public synchronized void addProducer(String networkId, EnergyAsset asset, double energy) {
		if (!enable) {
			return;
		}

		List<AssetEnergyPair> list = producerMap.get(networkId);
		if (list == null) {
			list = new ArrayList<AssetEnergyPair>();
		}
		list.add(new AssetEnergyPair(asset, energy));
		producerMap.put(networkId, list);
	}

	public synchronized void organiseNetwork(String networkId) {
		if (!enable) {
			return;
		}

		List<AssetEnergyPair> listOfProducers = producerMap.get(networkId);
		List<ConsumerProducerPair> listOfConsumers = consumerMap.get(networkId);

		if (listOfConsumers == null) {
			if (networkError.get(networkId) != null && !networkError.get(networkId)) {
				log.warn("Network {} has no consumers!", networkId);
			}
			networkError.put(networkId, true);
			overallError = true;
			return;
		}
		if (listOfProducers == null) {
			if (networkError.get(networkId) != null && !networkError.get(networkId)) {
				log.warn("Network {} has no producers!", networkId);
			}
			networkError.put(networkId, true);
			overallError = true;
			return;
		}

		double sumProduction = 0.0;
		sumProduction = listOfProducers.stream().mapToDouble(x -> x.getEnergy()).sum();
		sumProduction = sumProduction <= eps ? 1.0 : sumProduction;

		for (ConsumerProducerPair consumer : listOfConsumers) {
			for (AssetEnergyPair producer : listOfProducers) {
				List<AssetEnergyPair> producers = consumer.getProducers();
				if (producers == null) {
					producers = new ArrayList<AssetEnergyPair>();
				}
				producers.add(new AssetEnergyPair(producer.getAsset(),
						consumer.getConsumer().getEnergy() * producer.getEnergy() / sumProduction));
				consumer.setProducers(producers);
			}
		}

		producerMap.remove(networkId);
	}

	public synchronized void remapProducers() {
		if (!enable) {
			return;
		}

		for (List<ConsumerProducerPair> list : consumerMap.values()) {
			for (ConsumerProducerPair cpp : list) {
				consumerProducerMap.put(cpp.getConsumer(), cpp.getProducers());
			}
		}

		for (AssetEnergyPair consumerPair : consumerProducerMap.keySet()) {
			EnergyAsset consumer = consumerPair.getAsset();
			List<AssetEnergyPair> consumersProductions = new ArrayList<AssetEnergyPair>();
			for (List<AssetEnergyPair> list : consumerProducerMap.values()) {
				for (AssetEnergyPair producer : list) {
					if (producer.getAsset().equals(consumer)) {
						consumersProductions.add(producer);
					}
				}
			}

			double sumProduction = consumersProductions.stream().mapToDouble(x -> x.getEnergy()).sum();

			sumProduction = sumProduction <= eps ? 1.0 : sumProduction;

			List<AssetEnergyPair> consumersProducers = consumerProducerMap.get(consumerPair);

			for (Entry<AssetEnergyPair, List<AssetEnergyPair>> entry : consumerProducerMap.entrySet()) {
				double fraction = -1;
				int index = 0;
				for (AssetEnergyPair producer : entry.getValue()) {
					if (producer.getAsset().equals(consumer)) {
						fraction = producer.getEnergy() / sumProduction;
						break;
					}
					index++;
				}
				if (fraction != -1) {
					List<AssetEnergyPair> newProducers = new ArrayList<AssetEnergyPair>();
					for (AssetEnergyPair prod : consumersProducers) {
						newProducers.add(new AssetEnergyPair(prod.getAsset(), prod.getEnergy() * fraction));
					}
					entry.getValue().remove(index);
					entry.getValue().addAll(newProducers);
					consumerProducerMap.remove(consumerPair);
				}
			}
		}

		for (Entry<AssetEnergyPair, List<AssetEnergyPair>> e : consumerProducerMap.entrySet()) {
			List<AssetEnergyPair> newValues = new ArrayList<AssetEnergyPair>();

			for (AssetEnergyPair original : e.getValue()) {
				AssetEnergyPair valRef = null;
				for (AssetEnergyPair newValue : newValues) {
					if (newValue.getAsset().equals(original.getAsset())) {
						valRef = newValue;
						break;
					}
				}
				if (valRef != null) {
					valRef.setEnergy(valRef.getEnergy() + original.getEnergy());
				} else {
					newValues.add(original);
				}
			}

			e.setValue(newValues);

			// System.out.println(e.getKey()
			// .getAsset()
			// .getName() + "("
			// + e.getKey()
			// .getEnergy()
			// + ")");
			// for (AssetEnergyPair prod : e.getValue()) {
			// System.out.println("\t" + prod);
			// }
		}
	}

	@Override
	public synchronized void step(EssimTime timestamp) {
		if (!enable) {
			return;
		}

		if (overallError) {
			return;
		}

		remapProducers();

		HashMap<EnergyAsset, Double> consumerEmissionMap = new HashMap<EnergyAsset, Double>();
		HashMap<EnergyAsset, Double> producerEmissionMap = new HashMap<EnergyAsset, Double>();
		HashMap<EnergyAsset, Carrier> consumerCarrierMap = new HashMap<EnergyAsset, Carrier>();
		HashMap<EnergyAsset, Carrier> producerCarrierMap = new HashMap<EnergyAsset, Carrier>();

		for (AssetEnergyPair consumer : consumerProducerMap.keySet()) {
			EnergyAsset consumerAsset = consumer.getAsset();

			if (consumerAsset instanceof Storage) {
				for (Port port : consumerAsset.getPort()) {
					Carrier carrier = port.getCarrier();
					if (carrier != null) {
						consumerCarrierMap.put(consumerAsset, carrier);
						break;
					}
				}
			} else {
				for (Port port : consumerAsset.getPort()) {
					if (port instanceof InPort) {
						Carrier carrier = port.getCarrier();
						if (carrier != null) {
							consumerCarrierMap.put(consumerAsset, carrier);
							break;
						}
					}
				}
			}

			for (AssetEnergyPair producer : consumerProducerMap.get(consumer)) {
				// System.out.println(producer.getAsset().getName() + "(" +
				// producer.getAsset().getClass().getInterfaces()[0].getSimpleName() + ")");
				if (!(producer.getAsset() instanceof Producer)) {
					continue;
				}
				Producer producerAsset = (Producer) producer.getAsset();
				EnergyCarrier energyCarrier = null;

				if (producerAsset instanceof Storage) {
					for (Port port : producerAsset.getPort()) {
						Carrier carrier = port.getCarrier();
						if (carrier != null) {
							producerCarrierMap.put(producerAsset, carrier);
							if (carrier instanceof EnergyCarrier) {
								energyCarrier = (EnergyCarrier) carrier;
								break;
							}
						}
					}
				} else {
					for (Port port : producerAsset.getPort()) {
						if (port instanceof OutPort) {
							Carrier carrier = port.getCarrier();
							if (carrier != null) {
								producerCarrierMap.put(producerAsset, carrier);
								if (carrier instanceof EnergyCarrier) {
									energyCarrier = (EnergyCarrier) carrier;
									break;
								}
							}
						}
					}
				}

				if (energyCarrier != null) {
					double carrierEnergyContent = Commons.toStandardizedUnits(energyCarrier.getEnergyContent(),
							energyCarrier.getEnergyContentUnit());
					double carrierEmission = Commons.toStandardizedUnits(energyCarrier.getEmission(),
							energyCarrier.getEmissionUnit());

					double outputCarrierQuantity = producer.getEnergy() / carrierEnergyContent;
					double emission = outputCarrierQuantity * carrierEmission;

					if (producerAsset instanceof Producer) {
						if (producerAsset.getProdType().equals(RenewableTypeEnum.RENEWABLE)) {
							emission = 0;
						}
					}

					double consumerEmission = 0.0;
					double producerEmission = 0.0;
					if (consumerEmissionMap.containsKey(consumerAsset)) {
						consumerEmission = consumerEmissionMap.get(consumerAsset);
					}
					consumerEmissionMap.put(consumerAsset, consumerEmission + emission);

					if (producerEmissionMap.containsKey(producerAsset)) {
						producerEmission = producerEmissionMap.get(producerAsset);
					}
					producerEmissionMap.put(producerAsset, producerEmission + emission);
				}
			}
		}

		for (Entry<EnergyAsset, Double> entry : producerEmissionMap.entrySet()) {
			EnergyAsset producerAsset = entry.getKey();
			double emission = entry.getValue();
			Carrier producerCarrier = producerCarrierMap.get(producerAsset);
			if (observationManager != null) {
				observationManager.publish(this, Observation.builder().observedAt(timestamp.getTime())
						.tag("assetId", producerAsset.getId()).tag("capability", "Producer")
						.tag("assetName", producerAsset.getName() == null ? "UnnamedAsset" : producerAsset.getName())
						.tag("assetClass", producerAsset.getClass().getInterfaces()[0].getSimpleName())
						.tag("carrierId", producerCarrier.getId())
						.tag("carrierName",
								producerCarrier.getName() == null ? "UnnamedCarrier" : producerCarrier.getName())
						.value("emission", emission).build());
			}
		}

		for (Entry<EnergyAsset, Double> entry : consumerEmissionMap.entrySet()) {
			EnergyAsset consumerAsset = entry.getKey();
			double emission = entry.getValue();
			Carrier consumerCarrier = consumerCarrierMap.get(consumerAsset);
			if (observationManager != null) {
				observationManager.publish(this, Observation.builder().observedAt(timestamp.getTime())
						.tag("assetId", consumerAsset.getId()).tag("capability", "Consumer")
						.tag("sector",
								consumerAsset.getSector() == null ? "DefaultSector"
										: consumerAsset.getSector().getName())
						.tag("assetName", consumerAsset.getName() == null ? "UnnamedAsset" : consumerAsset.getName())
						.tag("assetClass", consumerAsset.getClass().getInterfaces()[0].getSimpleName())
						.tag("carrierId", consumerCarrier.getId())
						.tag("carrierName",
								consumerCarrier.getName() == null ? "UnnamedCarrier" : consumerCarrier.getName())
						.value("emission", emission).build());
			}
		}

		initialise();
	}

	@Override
	public void setObservationManager(IObservationManager manager) {
		this.observationManager = manager;
	}

	@Override
	public String getProviderName() {
		return "ESSIM";
	}

	@Override
	public String getProviderType() {
		return "General";
	}

	@Override
	public void init(EssimTime timestamp) {
		// TODO Auto-generated method stub
	}

	@Override
	public void stop() {
		instanceMap.clear();
	}

	@Override
	public void reset() {
		// TODO Auto-generated method stub
	}

	@Override
	public SimulationStatus getState() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void pause() {
		// TODO Auto-generated method stub
	}

	@Override
	public void setSimulationManager(ISimulationManager manager) {
		// TODO Auto-generated method stub
	}
}

@Data
@ToString
class ConsumerProducerPair {
	private AssetEnergyPair consumer;
	private List<AssetEnergyPair> producers;
}

@Data
@AllArgsConstructor
class AssetEnergyPair {
	private EnergyAsset asset;
	private double energy;

	@Override
	public String toString() {
		return asset.getName() + " : " + energy;
	}
}
