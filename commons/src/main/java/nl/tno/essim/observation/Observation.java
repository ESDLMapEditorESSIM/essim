/**
 *
 */

package nl.tno.essim.observation;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@AllArgsConstructor
@Getter
public class Observation implements IObservation {

    private LocalDateTime observedAt;
    private Map<String, Object> values;
    private Map<String, String> tags;

    public static class ObservationBuilder {
        private LocalDateTime observedAt;
        private Map<ObservationField, Object> values = new HashMap<>();
        private Map<String, String> tags = new HashMap<>();

        public ObservationBuilder value(ObservationField key, Object value) {
            if (values == null) {
                values = new HashMap<>();
            }
            this.values.put(key, value);
            return this;
        }


        public ObservationBuilder observedAt(LocalDateTime observedAt) {
            this.observedAt = observedAt;
            return this;
        }

        public ObservationBuilder tag(String key, String value) {
            if (this.tags == null) {
                this.tags = new HashMap<>();
            }
            this.tags.put(key, value);
            return this;
        }

        public Observation build() {
            HashMap<String, Object> newValues = new HashMap<>();
            values.forEach((key, value) -> newValues.put(key.getKey(), value));
            return new Observation(observedAt, newValues, tags);
        }
    }

    public static ObservationBuilder builder() {
        return new ObservationBuilder();
    }

}
