package nl.tno.essim.tests;

import nl.tno.essim.model.EssimSimulation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class EssimSimulationTest {

    @Test
    void testAddSchemeIfMissing() {
        assertNull(EssimSimulation.addSchemeIfMissing(null));

        // No-op
        assertEquals("http://influxdb:8086", EssimSimulation.addSchemeIfMissing("http://influxdb:8086"));
        assertEquals("https://influx.hesi.energy:8086", EssimSimulation.addSchemeIfMissing("https://influx.hesi.energy:8086"));

        // Single-label hostnames are internal, get http
        assertEquals("http://influxdb", EssimSimulation.addSchemeIfMissing("influxdb"));
        assertEquals("http://influxdb:8086", EssimSimulation.addSchemeIfMissing("influxdb:8086"));
        assertEquals("http://localhost:8086", EssimSimulation.addSchemeIfMissing("localhost:8086"));

        // FQDNs get https
        assertEquals("https://edr.hesi.energy:443", EssimSimulation.addSchemeIfMissing("edr.hesi.energy:443"));
    }
}
