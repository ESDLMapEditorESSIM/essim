package nl.tno.essim.observation;

public interface IObservationProcessor {
    void process(IObservationProvider source, IObservation observation);
}
