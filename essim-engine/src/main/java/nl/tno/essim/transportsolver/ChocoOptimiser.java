package nl.tno.essim.transportsolver;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import java.util.stream.IntStream;

import org.chocosolver.solver.Model;
import org.chocosolver.solver.Solution;
import org.chocosolver.solver.Solver;
import org.chocosolver.solver.variables.IntVar;

public class ChocoOptimiser {

	private List<TransportSolver> networks;
	private List<Integer[]> orders;

	public ChocoOptimiser(List<TransportSolver> networks, List<Integer[]> orders) {
		this.networks = networks;
		this.orders = orders;		
	}

	public TreeMap<Integer, List<TransportSolver>> solve() {
		TreeMap<Integer, List<TransportSolver>> order = new TreeMap<Integer, List<TransportSolver>>();
		
		if(networks.size() == 1) {
			order.put(0, networks);
			return order;
		}
		
		Model model = new Model("myModel");
		int n = networks.size();
		IntVar[] w = IntStream.range(0, n)
				.mapToObj(i -> model.intVar(networks.get(i)
						.getId(), 0, n - 1, false))
				.toArray(IntVar[]::new);

		for (Integer[] constraint : orders) {
			model.arithm(w[constraint[0]], "<", w[constraint[1]])
					.post();
		}

		Solver solver = model.getSolver();
		Solution solution = solver.findSolution();

		if (solution != null) {
			for (IntVar var : solution.retrieveIntVars(true)) {
				List<TransportSolver> list = order.get(var.getValue());
				if (list == null) {
					list = new ArrayList<TransportSolver>();
				}
				list.add(findSolverByName(var.getName()));
				order.put(var.getValue(), list);
			}

		}
		return order;
	}

	private TransportSolver findSolverByName(String name) {
		for (TransportSolver solver : networks) {
			if(solver.getId().equals(name)) {
				return solver;
			}
		}
		return null;
	}
}
