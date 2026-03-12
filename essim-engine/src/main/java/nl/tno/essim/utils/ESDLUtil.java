package nl.tno.essim.utils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.TimeZone;
import java.util.UUID;

import org.eclipse.emf.common.util.BasicEList;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.xmi.XMIResource;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceImpl;
import org.jetbrains.annotations.NotNull;

import esdl.AbstractBuilding;
import esdl.Area;
import esdl.Asset;
import esdl.DateTimeProfile;
import esdl.EnergyAsset;
import esdl.EnergySystem;
import esdl.EnergySystemInformation;
import esdl.EsdlFactory;
import esdl.EsdlPackage;
import esdl.ExternalProfile;
import esdl.GenericProfile;
import esdl.Instance;
import esdl.MultiplierEnum;
import esdl.PhysicalQuantityEnum;
import esdl.Port;
import esdl.ProfileElement;
import esdl.QuantityAndUnitReference;
import esdl.QuantityAndUnitScopeEnum;
import esdl.QuantityAndUnitType;
import esdl.TimeSeriesProfile;
import esdl.TimeUnitEnum;
import esdl.UnitEnum;
import lombok.extern.slf4j.Slf4j;
import nl.tno.essim.profiles.ProfileDataManager;
import nl.tno.essim.profiles.ProfileException;
import nl.tno.essim.time.EssimTime;
import nl.tno.essim.time.Horizon;
import nl.tno.essim.util.Converter;

@Slf4j
public class ESDLUtil {
    static {
        // Initialize the models
        EsdlPackage.eINSTANCE.eClass();
    }

    public interface EnergyAssetVisitor {
        void visit(EnergyAsset energyAsset);
    }

    public static void findAllEnergyAssets(Area area, List<EnergyAsset> assetList) {
        visitAllEnergyAssets(area, assetList::add);
    }

    public static void visitAllEnergyAssets(Area area, EnergyAssetVisitor visitor) {
        // Process assets within the current area
        processAssets(area.getAsset(), visitor);

        // Recursively process sub-areas
        EList<Area> subAreas = area.getArea();
        if (subAreas != null) {
            for (Area subArea : subAreas) {
                visitAllEnergyAssets(subArea, visitor);
            }
        }
    }

    public static void visitAllEnergyAssets(AbstractBuilding building, EnergyAssetVisitor visitor) {
        // Process assets within the current building
        processAssets(building.getAsset(), visitor);
    }

    // Refactored shared code logic to handle any type of asset list
    private static void processAssets(EList<Asset> assets, EnergyAssetVisitor visitor) {
        if (assets != null) {
            for (Asset asset : assets) {
                if (asset instanceof EnergyAsset) {
                    EnergyAsset energyAsset = (EnergyAsset) asset;
                    visitor.visit(energyAsset);
                } else if (asset instanceof AbstractBuilding) {
                    AbstractBuilding subBuilding = (AbstractBuilding) asset;
                    visitAllEnergyAssets(subBuilding, visitor);
                }
            }
        }
    }

    public static XMIResourceImpl loadEcoreResource(InputStream stream) throws IOException {
        XMIResourceImpl resource = new XMIResourceImpl();
        resource.getDefaultLoadOptions().put(XMIResource.OPTION_DEFER_IDREF_RESOLUTION, Boolean.TRUE);
        resource.setIntrinsicIDToEObjectMap(new HashMap<>());
        resource.load(stream, null);

        return resource;
    }

    public static <T extends EObject> String ecoreResourceToString(T ecoreResource) throws IOException {
        XMIResource xmiResource = new XMIResourceImpl();
        xmiResource.getContents().add(ecoreResource);
        HashMap<String, Object> opts = new HashMap<>();
        opts.put(XMIResource.OPTION_SCHEMA_LOCATION, true);
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        xmiResource.save(stream, opts);
        return stream.toString(StandardCharsets.UTF_8);
    }

    /**
     * Converts all external profiles in an ESDL energy system to time series profiles.
     * This transformation updates the profiles of energy assets by replacing any
     * {@code ExternalProfile} instances with {@code TimeSeriesProfile} instances, which are
     * compatible with the simulation time step and duration provided by {@code EssimTime}.
     * <p>
     * The method iterates through all energy assets and their associated ports in the
     * given energy system, creating time series profiles with interpolated values based on
     * the simulation time and step length. Each external profile's data is processed and
     * incorporated into a new time series format suitable for simulations.
     *
     * @param xmiResource The {@code XMIResourceImpl} object representing the ESDL energy system
     *                    resource. It serves as the container for the energy system model.
     * @param essimTime   An instance of {@code EssimTime}, which contains simulation parameters
     *                    such as the simulation start time, end time, and time step. These parameters
     *                    are used to generate the time series profiles and timestamp elements.
     * @throws IllegalArgumentException If no area is found in the given energy system instance.
     */
    public static void convertExternalToTimeSeriesProfiles(XMIResourceImpl xmiResource, EssimTime essimTime) {
        EnergySystem energySystem = (EnergySystem) xmiResource.getContents().getFirst();
        Instance instance = energySystem.getInstance().getFirst();
        if (instance.getArea() == null) {
            throw new IllegalArgumentException("No area found in ESDL energy instance !");
        }
        Area mainArea = instance.getArea();
        ProfileDataManager profileDataManager = new ProfileDataManager(xmiResource, essimTime);
        visitAllEnergyAssets(mainArea, energyAsset -> {
            for (Port port : energyAsset.getPort()) {
                ArrayList<GenericProfile> newProfiles = new ArrayList<>();
                for (var i = port.getProfile().iterator(); i.hasNext(); ) {
                    GenericProfile origProfile = i.next();
                    if (origProfile instanceof ExternalProfile) {
                        TimeSeriesProfile profile = EsdlFactory.eINSTANCE.createTimeSeriesProfile();
                        profile.setTimestep((int) essimTime.getSimulationStepLength().getSeconds());
                        profile.setStartDateTime(EssimTime.localDateTimeToDate(essimTime.getSimulationStartTime()));
                        profile.setProfileType(origProfile.getProfileType());
                        QuantityAndUnitReference newQaU = EsdlFactory.eINSTANCE.createQuantityAndUnitReference();
                        if (origProfile.getProfileQuantityAndUnit() instanceof QuantityAndUnitReference) {
                            newQaU.setReference(((QuantityAndUnitReference) origProfile.getProfileQuantityAndUnit()).getReference());
                        } else if (origProfile.getProfileQuantityAndUnit() instanceof QuantityAndUnitType) {
                            QuantityAndUnitType standardizedUnit = Converter.resolveAbstractQuantityAndUnit(origProfile.getProfileQuantityAndUnit());
                            QuantityAndUnitType quantityAndUnitType = EsdlFactory.eINSTANCE.createQuantityAndUnitType();
                            quantityAndUnitType.setUnit(standardizedUnit.getUnit());
                            quantityAndUnitType.setDescription(standardizedUnit.getDescription());
                            quantityAndUnitType.setMultiplier(MultiplierEnum.NONE);
                            quantityAndUnitType.setPerUnit(standardizedUnit.getPerUnit());
                            quantityAndUnitType.setPhysicalQuantity(standardizedUnit.getPhysicalQuantity());
                            quantityAndUnitType.setPerTimeUnit(standardizedUnit.getPerTimeUnit());
                            quantityAndUnitType.setPerMultiplier(standardizedUnit.getPerMultiplier());
                            quantityAndUnitType.setPerScope(standardizedUnit.getPerScope());
                            if (energySystem.getEnergySystemInformation().getQuantityAndUnits() == null) {
                                energySystem.getEnergySystemInformation().setQuantityAndUnits(EsdlFactory.eINSTANCE.createQuantityAndUnits());
                                energySystem.getEnergySystemInformation().getQuantityAndUnits().setId(UUID.randomUUID().toString());
                            }
                            energySystem.getEnergySystemInformation().getQuantityAndUnits().getQuantityAndUnit().add(quantityAndUnitType);
                            newQaU.setReference((quantityAndUnitType));
                        } else {
                            throw new IllegalArgumentException("Unknown QuantityAndUnitType type: " + origProfile.getProfileQuantityAndUnit().getClass().getName());
                        }
                        profile.setProfileQuantityAndUnit(newQaU);
                        profile.setInterpolationMethod(origProfile.getInterpolationMethod());
                        EList<ProfileElement> profileElements = new BasicEList<>();
                        double div = Converter.abstractQuantityToStandardizedUnits(1, newQaU);
                        for (var t = essimTime.getSimulationStartTime(); t.isBefore(essimTime.getSimulationEndTime()); t = t.plus(essimTime.getSimulationStepLength().toDuration())) {
                            double v = 0;
                            try {
                                v = profileDataManager.readProfile(origProfile, new Horizon(t, essimTime.getSimulationStepLength())) / div;
                            } catch (ProfileException e) {
                                throw new RuntimeException(e);
                            }
                            ProfileElement profileElement = EsdlFactory.eINSTANCE.createProfileElement();
                            profileElement.setFrom(EssimTime.localDateTimeToDate(t));
                            profileElement.setTo(EssimTime.localDateTimeToDate(t.plus(essimTime.getSimulationStepLength().toDuration())));
                            profileElement.setValue(v);
                            profileElements.add(profileElement);
                        }
                        profileDataManager.getProfileDataSupplier(profile).setProfileElements(profileElements);
                        i.remove();
                        newProfiles.add(profile);
                    }
                }
                port.getProfile().addAll(newProfiles);
            }
        });
    }

    public static LocalDateTime[] getProfilesStartAndEnd(XMIResourceImpl esdlResource) {
        Long startAndEnd[] = new Long[2];
        for (var iterator = esdlResource.getAllContents(); iterator.hasNext(); ) {
            EObject object = iterator.next();
            if (object instanceof ExternalProfile profile) {
                updateStartAndEnd(startAndEnd, profile.getStartDate().getTime(), profile.getEndDate().getTime());

            } else if (object instanceof TimeSeriesProfile profile) {
                long startTime = profile.getStartDateTime().getTime();
                long endTime = startTime + ((long) profile.getValues().size() * (long) profile.getTimestep() * 1000);
                updateStartAndEnd(startAndEnd, startTime, endTime);

            } else if (object instanceof DateTimeProfile profile) {
                profile.getElement().forEach(element -> {
                    updateStartAndEnd(startAndEnd, element.getFrom().getTime(), element.getTo().getTime());
                });
            }
        }
        return new LocalDateTime[]{
                startAndEnd[0] == null ? null : LocalDateTime.ofInstant(Instant.ofEpochMilli(startAndEnd[0]), TimeZone.getDefault().toZoneId()),
                startAndEnd[1] == null ? null : LocalDateTime.ofInstant(Instant.ofEpochMilli(startAndEnd[1]), TimeZone.getDefault().toZoneId())};
    }

    private static void updateStartAndEnd(Long[] startAndEnd, Long startTime, Long endTime) {
        if (startTime != null && (startAndEnd[0] == null || startTime < startAndEnd[0])) {
            startAndEnd[0] = startTime;
        }
        if (endTime != null && (startAndEnd[1] == null || endTime > startAndEnd[1])) {
            startAndEnd[1] = endTime;
        }
    }

    
    /**
     * Returns or creates a ENERGY in Joule {@link esdl.QuantityAndUnitType} in the QuanityAndUnits part of the {@link esdl.EnergySystemInformation}, 
     * if not present already.
     * @param energySystem
     * @return Energy in Joule Quantity and Unit.
     */
	@NotNull
	public static QuantityAndUnitType getOrCreateEnergyInJouleQuantityAndUnit(EnergySystem energySystem) {
		if (energySystem.getEnergySystemInformation() == null) {
			EnergySystemInformation energySystemInformation = EsdlFactory.eINSTANCE.createEnergySystemInformation();
			energySystemInformation.setId(UUID.randomUUID().toString());
			energySystem.setEnergySystemInformation(energySystemInformation);
		}
		if (energySystem.getEnergySystemInformation().getQuantityAndUnits() == null) {
			energySystem.getEnergySystemInformation()
					.setQuantityAndUnits(EsdlFactory.eINSTANCE.createQuantityAndUnits());
			energySystem.getEnergySystemInformation().getQuantityAndUnits().setId(UUID.randomUUID().toString());
		}
		QuantityAndUnitType energyInJoule = null;
		// find EnergyInJoule in ESI of ESDL. If not create one and add it.
		for (QuantityAndUnitType qau : energySystem.getEnergySystemInformation().getQuantityAndUnits().getQuantityAndUnit()) {
			if (qau.getPhysicalQuantity() == PhysicalQuantityEnum.ENERGY && qau.getUnit() == UnitEnum.JOULE &&
					qau.getMultiplier() == MultiplierEnum.NONE && qau.getPerUnit() == UnitEnum.NONE && 
					qau.getPerMultiplier() == MultiplierEnum.NONE && qau.getPerScope()==QuantityAndUnitScopeEnum.UNDEFINED &&
					qau.getPerTimeUnit() == TimeUnitEnum.NONE) {
				energyInJoule = qau;
				break;
			}
		}
		if (energyInJoule == null) {
			energyInJoule = EsdlFactory.eINSTANCE.createQuantityAndUnitType();
			energyInJoule.setId(UUID.randomUUID().toString());
			energyInJoule.setUnit(UnitEnum.JOULE);
			energyInJoule.setDescription("Energy in J");
			energyInJoule.setMultiplier(MultiplierEnum.NONE);
			energyInJoule.setPerUnit(UnitEnum.NONE);
			energyInJoule.setPhysicalQuantity(PhysicalQuantityEnum.ENERGY);
			energyInJoule.setPerTimeUnit(TimeUnitEnum.NONE);
			energyInJoule.setPerMultiplier(MultiplierEnum.NONE);
			energyInJoule.setPerScope(QuantityAndUnitScopeEnum.UNDEFINED);
			energySystem.getEnergySystemInformation().getQuantityAndUnits().getQuantityAndUnit().add(energyInJoule);
		}
		return energyInJoule;
	}
	
	
	/**
	 * Create a reference to an existing {@link esdl.QuantityAndUnitType}.
	 * @param quantityAndUnitType
	 * @return
	 */
	@NotNull
	public static QuantityAndUnitReference createQaUReference(QuantityAndUnitType quantityAndUnitType) {
		QuantityAndUnitReference quantityAndUnitReference = EsdlFactory.eINSTANCE.createQuantityAndUnitReference();
		quantityAndUnitReference.setReference(quantityAndUnitType);		
		return quantityAndUnitReference;
	}
}
