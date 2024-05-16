package nl.tno.essim.rest;

import lombok.extern.slf4j.Slf4j;
import nl.tno.essim.model.Status;
import nl.tno.essim.mongo.IMongoBackend;

import java.io.PrintWriter;
import java.io.StringWriter;

@Slf4j
class EssimExceptionHandler implements Thread.UncaughtExceptionHandler {
    private IMongoBackend mongo;

    public EssimExceptionHandler(IMongoBackend mongo) {
        this.mongo = mongo;
    }

    @Override
    public void uncaughtException(Thread t, Throwable e) {
        StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw));
        log.error(e.getMessage(), e);
        mongo.updateSimulationStatus(t.getName(), Status.ERROR, sw.toString());
        mongo.updateStatus("Ready");
    }
}
