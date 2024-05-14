package nl.tno.essim.model;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.util.StdDateFormat;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

@Slf4j
public class TimestampDeserializer extends StdDeserializer<Date> {
    private final static List<DateFormat> DATE_FORMATS = Arrays.asList(new StdDateFormat(), new SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy"));


    public TimestampDeserializer() {
        super(Date.class);
    }

    public Date deserialize(JsonParser jsonParser, DeserializationContext jsonContext) throws
            IOException, JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        String dateString = mapper.readValue(jsonParser, String.class);
        for (DateFormat format : DATE_FORMATS) {
            try {
                Date date = format.parse(dateString);
                return date;
            } catch (ParseException e) {
            }
        }
        log.warn("Could not parse date {}", dateString);
        throw new IOException("Could not parse date " + dateString);
    }
}
