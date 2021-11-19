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

package nl.tno.essim.transportsolver;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.stream.IntStream;

import org.chocosolver.solver.Model;
import org.chocosolver.solver.Solution;
import org.chocosolver.solver.Solver;
import org.chocosolver.solver.variables.IntVar;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ChocoOptimiser {

	private List<TransportSolver> networks;
	private HashMap<String, List<Integer[]>> orders;

	public ChocoOptimiser(List<TransportSolver> networks, HashMap<String, List<Integer[]>> orders) {
		this.networks = networks;
		this.orders = orders;
	}

	public TreeMap<Integer, List<TransportSolver>> solve() {
		TreeMap<Integer, List<TransportSolver>> order = new TreeMap<Integer, List<TransportSolver>>();

		if (networks.size() == 1) {
			order.put(0, networks);
			return order;
		}

		Model model = new Model("myModel");
		int n = networks.size();
		IntVar[] w = IntStream.range(0, n).mapToObj(i -> model.intVar(networks.get(i).getId(), 0, n - 1, false))
				.toArray(IntVar[]::new);

		StringBuilder sb = new StringBuilder();
		for (Entry<String, List<Integer[]>> constraintSet : orders.entrySet()) {
			List<Integer[]> constraints = constraintSet.getValue();
			String responsible = constraintSet.getKey();
			for (Integer[] constraint : constraints) {
				sb.append(responsible);
				sb.append(" enforced ");
				sb.append(w[constraint[0]]);
				sb.append(" < ");
				sb.append(w[constraint[1]]);
				sb.append("\n");
//			System.out.println(w[constraint[0]].getName() + " < " + w[constraint[1]].getName() + " - "
//					+ w[constraint[0]].getId() + " < " + w[constraint[1]].getId());
				model.arithm(w[constraint[0]], "<", w[constraint[1]]).post();
			}
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

		} else {
			log.error("Circular dependency detected - conflicting control strategies configured!");
			log.error(sb.toString());
			throw new IllegalStateException(solver.getContradictionException());
		}
		return order;
	}

	private TransportSolver findSolverByName(String name) {
		for (TransportSolver solver : networks) {
			if (solver.getId().equals(name)) {
				return solver;
			}
		}
		return null;
	}
}
