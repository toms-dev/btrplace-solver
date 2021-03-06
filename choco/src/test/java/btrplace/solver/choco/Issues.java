/*
 * Copyright (c) 2013 University of Nice Sophia-Antipolis
 *
 * This file is part of btrplace.
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package btrplace.solver.choco;

import btrplace.model.*;
import btrplace.model.constraint.Fence;
import btrplace.model.constraint.Offline;
import btrplace.model.constraint.SatConstraint;
import btrplace.model.constraint.Spread;
import btrplace.model.view.ShareableResource;
import btrplace.plan.ReconfigurationPlan;
import btrplace.solver.SolverException;
import btrplace.solver.choco.chocoUtil.ChocoUtils;
import btrplace.solver.choco.objective.minMTTR.MinMTTR;
import choco.cp.solver.CPSolver;
import choco.cp.solver.constraints.integer.ElementV;
import choco.kernel.common.logging.ChocoLogging;
import choco.kernel.solver.ContradictionException;
import choco.kernel.solver.constraints.SConstraint;
import choco.kernel.solver.constraints.integer.IntExp;
import choco.kernel.solver.variables.integer.IntDomainVar;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.*;

/**
 * Unit tests related to the opened issues
 *
 * @author Fabien Hermenier
 */
public class Issues {

    /**
     * Another test related to issue #5.
     *
     * @throws SolverException
     */
    @Test
    public void testIssue5a() throws SolverException, ContradictionException {

        Model model = new DefaultModel();
        Node n1 = model.newNode();
        Node n2 = model.newNode();
        Node n3 = model.newNode();
        VM vm1 = model.newVM();
        VM vm2 = model.newVM();
        ShareableResource resources = new ShareableResource("vcpu", 1, 1);
        resources.setCapacity(n1, 2);
        resources.setCapacity(n2, 2);

        Mapping map = new MappingFiller(model.getMapping()).on(n1, n2).off(n3).run(n1, vm1, vm2).get();
        model.attach(resources);

        ReconfigurationProblem rp = new DefaultReconfigurationProblemBuilder(model)
                .labelVariables()
                .build();

        CPSolver solver = rp.getSolver();
        IntDomainVar[] VMsOnAllNodes = rp.getNbRunningVMs();

        int NUMBER_OF_NODE = map.getAllNodes().size();

        // Each element is the number of VMs on each node
        IntDomainVar[] vmsOnInvolvedNodes = new IntDomainVar[NUMBER_OF_NODE];

        IntDomainVar[] busy = new IntDomainVar[NUMBER_OF_NODE];

        rp.getEnd().setSup(10);
        int i = 0;
        int maxVMs = rp.getSourceModel().getMapping().getAllVMs().size();
        for (Node n : map.getAllNodes()) {
            vmsOnInvolvedNodes[i] = solver.createBoundIntVar("nVMs", -1, maxVMs);
            IntDomainVar state = rp.getNodeAction(n).getState();
            // If the node is offline -> the temporary variable is -1, otherwise, it equals the number of VMs on that node
            IntDomainVar[] c = new IntDomainVar[]{solver.makeConstantIntVar(-1), VMsOnAllNodes[rp.getNode(n)],
                    state, vmsOnInvolvedNodes[i]};
            solver.post(new ElementV(c, 0, solver.getEnvironment()));

            // IF the node is online and hosting VMs -> busy = 1.
            busy[i] = solver.createBooleanVar("busy" + n);
            ChocoUtils.postIfOnlyIf(solver, busy[i], solver.geq(vmsOnInvolvedNodes[i], 1));
            i++;
        }

        // idle is equals the number of vmsOnInvolvedNodes with value 0. (The node without VM)
        IntDomainVar idle = solver.createBoundIntVar("Nidles", 0, NUMBER_OF_NODE);
        solver.post(solver.occurence(vmsOnInvolvedNodes, idle, 0));
        // idle should be less than Amount for MaxSN (0, in this case)
        solver.post(solver.leq(idle, 0));

        // Extract all the state of the involved nodes (all nodes in this case)
        IntDomainVar[] states = new IntDomainVar[NUMBER_OF_NODE];
        int j = 0;
        for (Node n : map.getAllNodes()) {
            states[j++] = rp.getNodeAction(n).getState();
        }

        // In case the number of VMs is inferior to the number of online nodes, some nodes have to shutdown
        // to satisfy the constraint. This could be express as:
        // The addition of the idle nodes and busy nodes should be equals the number of online nodes.
        IntExp sumStates = CPSolver.sum(states);
        IntExp sumIB = solver.plus(CPSolver.sum(busy), idle);
        solver.post(solver.eq(sumStates, sumIB));

        ReconfigurationPlan plan = rp.solve(0, false);
        Assert.assertNotNull(plan);
    }

    /**
     * Test a suspicious bug in issue #5
     */
    @Test
    public void testIssue5b() throws SolverException {
        Model model = new DefaultModel();
        Node n1 = model.newNode();
        Node n2 = model.newNode();
        Node n3 = model.newNode();
        VM vm1 = model.newVM();
        VM vm2 = model.newVM();
        VM vm3 = model.newVM();
        VM vm4 = model.newVM();

        Mapping map = new MappingFiller(model.getMapping()).on(n1, n2, n3)
                .run(n2, vm1, vm2, vm3, vm4).get();


        ReconfigurationProblem rp = new DefaultReconfigurationProblemBuilder(model)
                .labelVariables()
                .build();

        IntDomainVar[] nodes_state = rp.getNbRunningVMs();
        IntDomainVar[] nodeVM = new IntDomainVar[map.getAllNodes().size()];

        int i = 0;

        for (Node n : map.getAllNodes()) {
            nodeVM[i++] = nodes_state[rp.getNode(n)];
        }
        CPSolver solver = rp.getSolver();
        IntDomainVar idle = solver.createBoundIntVar("Nidles", 0, map.getAllNodes().size());

        solver.post(solver.occurence(nodeVM, idle, 0));
        // Amount of maxSpareNode =  1
        solver.post(solver.leq(idle, 1));

        ReconfigurationPlan plan = rp.solve(0, false);
        Assert.assertNotNull(plan);
    }

    /**
     * Test a suspicious bug in issue #5
     */
    @Test
    public void testIssue5c() throws SolverException, ContradictionException {

        Model model = new DefaultModel();
        Node n1 = model.newNode();
        Node n2 = model.newNode();
        Node n3 = model.newNode();
        VM vm1 = model.newVM();
        VM vm2 = model.newVM();
        VM vm3 = model.newVM();
        VM vm4 = model.newVM();


        Mapping map = new MappingFiller(model.getMapping()).on(n1, n2, n3)
                .run(n2, vm1, vm2, vm3, vm4).get();

        ReconfigurationProblem rp = new DefaultReconfigurationProblemBuilder(model)
                .labelVariables()
                .build();

        IntDomainVar[] nodes_state = rp.getNbRunningVMs();
        IntDomainVar[] nodeVM = new IntDomainVar[map.getAllNodes().size()];

        int i = 0;

        for (Node n : map.getAllNodes()) {
            nodeVM[i++] = nodes_state[rp.getNode(n)];
            //rp.getNodeAction(n).getState().setVal(1);
        }
        CPSolver solver = rp.getSolver();
        IntDomainVar idle = solver.createBoundIntVar("Nidles", 0, map.getAllNodes().size());

        solver.post(solver.occurence(nodeVM, idle, 0));
        solver.post(solver.leq(idle, 1));
        ReconfigurationPlan plan = rp.solve(0, false);
        Assert.assertNotNull(plan);
    }

    @Test
    public void testIssue10() throws SolverException, ContradictionException {
        Model model = new DefaultModel();
        Node n1 = model.newNode();
        Node n2 = model.newNode();
        Node n3 = model.newNode();
        VM vm1 = model.newVM();
        VM vm2 = model.newVM();

        Mapping map = new MappingFiller(model.getMapping()).on(n1, n2).off(n3).run(n1, vm1, vm2).get();
        //model.attach(resources);
        ReconfigurationProblem rp = new DefaultReconfigurationProblemBuilder(model).labelVariables().build();
        CPSolver solver = rp.getSolver();
        rp.getNodeAction(n3).getState().setVal(1);  // n3 goes online
        solver.post(solver.leq(rp.getEnd(), 10));
        int NUMBER_OF_NODE = map.getAllNodes().size();
        // Extract all the state of the involved nodes (all nodes in this case)
        IntDomainVar[] VMsOnAllNodes = rp.getNbRunningVMs();
        // Each element is the number of VMs on each node
        IntDomainVar[] vmsOnInvolvedNodes = new IntDomainVar[NUMBER_OF_NODE];
        IntDomainVar[] idles = new IntDomainVar[NUMBER_OF_NODE];
        int i = 0;
        int maxVMs = rp.getSourceModel().getMapping().getAllVMs().size();
        List<SConstraint> elms = new ArrayList<>();
        for (Node n : map.getAllNodes()) {
            vmsOnInvolvedNodes[i] = solver.createBoundIntVar("nVMs" + n, -1, maxVMs);
            IntDomainVar state = rp.getNodeAction(n).getState();
            // If the node is offline -> the temporary variable is 1, otherwise, it equals the number of VMs on that node
            IntDomainVar[] c = new IntDomainVar[]{solver.makeConstantIntVar(-1), VMsOnAllNodes[rp.getNode(n)],
                    state, vmsOnInvolvedNodes[i]};
            SConstraint elem = new ElementV(c, 0, solver.getEnvironment());
            elms.add(elem);
            solver.post(elem);
            // IF number of VMs on a node is 0 -> Idle
            idles[i] = solver.createBooleanVar("idle" + n);
            ChocoUtils.postIfOnlyIf(solver, idles[i], solver.eq(vmsOnInvolvedNodes[i], 0));
            i++;
        }
        IntExp Sidle = CPSolver.sum(idles);
        // idle should be less than Amount for MaxSN (0, in this case)
        solver.post(solver.eq(Sidle, 0));
        System.err.flush();
        MinMTTR obj = new MinMTTR();
        obj.inject(rp);
        //System.err.println(solver.pretty());
        ChocoLogging.setLoggingMaxDepth(100);
        ReconfigurationPlan plan = rp.solve(0, false);
        Assert.assertNotNull(plan);
        System.out.println(plan);
        System.out.println(plan.getResult());
    }

    /**
     * Unit test derived from Issue 16.
     *
     * @throws SolverException
     */
    @Test
    public void test16b() throws SolverException {
        Model model = new DefaultModel();
        Node n1 = model.newNode();
        Node n2 = model.newNode();
        Node n3 = model.newNode();
        Node n4 = model.newNode();
        VM vm1 = model.newVM();
        VM vm2 = model.newVM();
        VM vm3 = model.newVM();
        VM vm4 = model.newVM();
        VM vm5 = model.newVM();
        VM vm6 = model.newVM();


        Mapping map = new MappingFiller(model.getMapping()).on(n1, n2, n3, n4)
                .run(n1, vm1, vm2)
                .run(n2, vm3, vm4)
                .run(n3, vm5, vm6)
                .get();

        Set<SatConstraint> ctrsC = new HashSet<SatConstraint>();
        Set<VM> vms1 = new HashSet<VM>(Arrays.asList(vm1, vm3, vm5));
        Set<VM> vms2 = new HashSet<VM>(Arrays.asList(vm2, vm4, vm6));

        ctrsC.add(new Spread(vms1));
        ctrsC.add(new Spread(vms2));
        ctrsC.add(new Fence(Collections.singleton(vm3), Collections.singleton(n1)));

        Offline off = new Offline(Collections.singleton(n2));
        ctrsC.add(off);
        ChocoReconfigurationAlgorithm cra = new DefaultChocoReconfigurationAlgorithm();
        ReconfigurationPlan dp = cra.solve(model, ctrsC);
    }

    @Test
    public void issue19() {
        Model m = new DefaultModel();
        ShareableResource cpu = new ShareableResource("cpu", 4, 1);
        Node n = m.newNode();
        Node n2 = m.newNode();
        m.attach(cpu);
        Assert.assertEquals(cpu.sumCapacities(Arrays.asList(n, n2), true), 8);
    }
}
