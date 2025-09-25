package nl.tno.essim.profiles;

import esdl.*;
import lombok.extern.slf4j.Slf4j;
import nl.tno.essim.time.EssimTime;
import nl.tno.essim.util.Converter;
import org.eclipse.emf.common.util.ECollections;
import org.eclipse.emf.common.util.EList;

import java.net.URI;
import java.util.Date;
import java.util.UUID;

@Slf4j
public class DataTableProfileDataHandler implements ProfileDataHandler {
    private final DataTableProfile profile;
    private final DataProcessor dataProcessor;
    private final ProfileCacheSupplier cacheSupplier;
    private TimeSeriesDataCache dataCache;

    public DataTableProfileDataHandler(DataTableProfile profile, EssimTime essimTime) {
        this.profile = profile;
        Date profileStartDate = profile.getStartDate();
        if (profile.getProfileQuantityAndUnit() != null) {
            dataProcessor = new DataProcessor(profile.getProfileQuantityAndUnit(), profile.getMultiplier());
        } else {
            dataProcessor = new DataProcessor(profile.getProfileType(), profile.getMultiplier());
        }
        cacheSupplier = ProfileCacheSupplierFactory.getProfileCacheSupplier(profile.getConfiguration());
        if (profileStartDate != null) {
            // Make sure the cache is loaded
            try {
                getProfileElements(profileStartDate, Date.from(profileStartDate.toInstant().plusSeconds(essimTime.getSimulationStepLength().getSeconds())), Converter.toESDLDuration(essimTime.getSimulationStepLength()));
            } catch (ProfileException e) {
                log.warn("Could not load profile elements for profile with id {}.", profile.getId(), e);
            }
        }
    }

    @Override
    public EList<ProfileElement> getProfileElements(Date from, Date to, Duration aggregationPrecision) throws ProfileException {
        if (dataCache == null) {
            ProfileCacheKey profileCacheKey = getProfileCacheKey();
            synchronized (ProfileCache.getInstance()) {
                dataCache = ProfileCache.getInstance().getDataCache(profileCacheKey);
                if (dataCache == null) {
                    dataCache = createTimeSeriesDataCache(profileCacheKey);
                } else {
                    log.debug("Using profile {} from cache", profileCacheKey);
                }
            }
        }
        return dataCache == null ? ECollections.newBasicEList() : dataCache.get(EssimTime.dateToLocalDateTime(from), EssimTime.dateToLocalDateTime(to), dataProcessor);

    }

    @Override
    public ProfileCacheKey getProfileCacheKey() {
        String uri = cacheSupplier.createUri(profile.getConfiguration());
        return new ProfileCacheKey(uri, profile.getSchema(), profile.getTableName(), profile.getColumnName(), profile.getFilter(), 0d);
    }

    private TimeSeriesDataCache createTimeSeriesDataCache(ProfileCacheKey profileCacheKey) throws ProfileException {
        try {
            return cacheSupplier.getTimeSeriesDataCache(profileCacheKey, profile.getTimeColumnName(), profile.getStartDate(), profile.getEndDate());
        } catch (Exception e) {
            log.error("Could not create TimeSeriesDataCache for profile {}", profileCacheKey, e);
            throw new ProfileException(e);
        }
    }

    @Override
    public boolean setProfileElements(EList<ProfileElement> profileElementList) {
        throw new UnsupportedOperationException("setProfileElements not implemented for DataTableProfile");
    }

    public static DatabaseConfiguration getDatabaseConfiguration(EnergySystem energySystem, String uriString, DatabaseTypeEnum type) {
        DatabaseConfiguration dbc = EsdlFactory.eINSTANCE.createDatabaseConfiguration();
        dbc.setType(type);
        dbc.setId(UUID.randomUUID().toString());
        URI uri = URI.create(uriString);
        dbc.setHost(uri.getHost());
        if (uri.getPort() > 0) {
            dbc.setPort(uri.getPort());
        }
        String dbName = uri.getPath().replaceFirst("/", "");

        if (!dbName.isEmpty()) {
            dbc.setDatabase(dbName);
        }
        String query = uri.getQuery();
        if (query != null && query.contains("tls")) {
            // Parse the query string to find the value of 'tls'
            String[] queryParams = query.split("&");
            for (String param : queryParams) {
                String[] keyValue = param.split("=");
                if (keyValue.length == 2 && keyValue[0].equalsIgnoreCase("tls")) {
                    // Set the tls value on dbc based on the URI's tls value
                    dbc.setTls(Boolean.parseBoolean(keyValue[1]));
                    break;
                }
            }
        }
        getDataConfigurations(energySystem).getConfigurations().add(dbc);
    	return dbc;
    }

    public static DataConfigurations getDataConfigurations(EnergySystem energySystem) {
        DataConfigurations dataConfigurations = energySystem.getEnergySystemInformation().getDataconfigurations();
        if (dataConfigurations == null) {
            dataConfigurations = EsdlFactory.eINSTANCE.createDataConfigurations();
            energySystem.getEnergySystemInformation().setDataconfigurations(dataConfigurations);
        }
        return dataConfigurations;
    }
}
