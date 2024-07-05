package nl.tno.essim.tests;

import com.powsybl.ieeecdf.converter.IeeeCdfNetworkFactory;
import com.powsybl.iidm.network.Network;
import com.powsybl.loadflow.LoadFlow;
import com.powsybl.loadflow.LoadFlowParameters;
import com.powsybl.math.matrix.DenseMatrixFactory;
import com.powsybl.openloadflow.OpenLoadFlowParameters;
import com.powsybl.openloadflow.OpenLoadFlowProvider;
import com.powsybl.openloadflow.network.SlackBusSelectionMode;
import org.junit.jupiter.api.Test;

public class LoadflowTest {

    @Test
    public void example() {
        LoadFlowParameters parameters = new LoadFlowParameters().setDc(true);
        OpenLoadFlowParameters parametersExt = OpenLoadFlowParameters.create(parameters).setSlackBusSelectionMode(SlackBusSelectionMode.LARGEST_GENERATOR);
        OpenLoadFlowProvider loadFlowProvider = new OpenLoadFlowProvider(new DenseMatrixFactory());
        LoadFlow.Runner loadFlowRunner = new LoadFlow.Runner(loadFlowProvider);
    }
}
