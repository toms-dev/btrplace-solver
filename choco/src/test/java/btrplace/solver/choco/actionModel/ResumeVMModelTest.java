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

package btrplace.solver.choco.actionModel;

import btrplace.model.*;
import btrplace.plan.ReconfigurationPlan;
import btrplace.plan.event.Action;
import btrplace.plan.event.ResumeVM;
import btrplace.solver.SolverException;
import btrplace.solver.choco.DefaultReconfigurationProblemBuilder;
import btrplace.solver.choco.ReconfigurationProblem;
import btrplace.solver.choco.durationEvaluator.ConstantActionDuration;
import btrplace.solver.choco.durationEvaluator.DurationEvaluators;
import choco.cp.solver.CPSolver;
import choco.kernel.solver.ContradictionException;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.HashSet;
import java.util.Iterator;


/**
 * Basic unit tests for {@link btrplace.solver.choco.actionModel.ResumeVMModel}.
 *
 * @author Fabien Hermenier
 */
public class ResumeVMModelTest {

    /**
     * Just resume a VM on its current node.
     */
    @Test
    public void testBasics() throws SolverException, ContradictionException {
        Model mo = new DefaultModel();
        Mapping map = mo.getMapping();
        final VM vm1 = mo.newVM();
        Node n1 = mo.newNode();
        Node n2 = mo.newNode();

        map.addOnlineNode(n1);
        map.addOnlineNode(n2);
        map.addSleepingVM(vm1, n1);

        DurationEvaluators dev = new DurationEvaluators();
        dev.register(ResumeVM.class, new ConstantActionDuration(10));
        ReconfigurationProblem rp = new DefaultReconfigurationProblemBuilder(mo)
                .setDurationEvaluatators(dev)
                .labelVariables()
                .setNextVMsStates(new HashSet<VM>(), map.getAllVMs(), new HashSet<VM>(), new HashSet<VM>())
                .build();
        rp.getNodeActions()[0].getState().setVal(1);
        rp.getNodeActions()[1].getState().setVal(1);
        ResumeVMModel m = (ResumeVMModel) rp.getVMActions()[0];
        Assert.assertEquals(vm1, m.getVM());
        Assert.assertNull(m.getCSlice());
        Assert.assertTrue(m.getDuration().isInstantiatedTo(10));
        Assert.assertTrue(m.getState().isInstantiatedTo(1));
        Assert.assertFalse(m.getDSlice().getHoster().isInstantiated());
        Assert.assertFalse(m.getDSlice().getStart().isInstantiated());
        Assert.assertFalse(m.getDSlice().getEnd().isInstantiated());

        ReconfigurationPlan p = rp.solve(0, false);
        Assert.assertNotNull(p);
        ResumeVM a = (ResumeVM) p.getActions().iterator().next();

        Node dest = rp.getNode(m.getDSlice().getHoster().getVal());
        Assert.assertEquals(vm1, a.getVM());
        Assert.assertEquals(dest, a.getDestinationNode());
        Assert.assertEquals(n1, a.getSourceNode());
        Assert.assertEquals(10, a.getEnd() - a.getStart());
    }

    /**
     * Test that check when the action is shorter than the end of
     * the reconfiguration process.
     * In practice, 2 resume actions have to be executed sequentially
     */
    @Test
    public void testResumeSequence() throws SolverException, ContradictionException {
        Model mo = new DefaultModel();
        Mapping map = mo.getMapping();
        VM vm1 = mo.newVM();
        VM vm2 = mo.newVM();
        Node n1 = mo.newNode();
        Node n2 = mo.newNode();

        map.addOnlineNode(n1);
        map.addOnlineNode(n2);
        map.addSleepingVM(vm1, n1);
        map.addSleepingVM(vm2, n2);

        DurationEvaluators dev = new DurationEvaluators();
        dev.register(ResumeVM.class, new ConstantActionDuration(5));
        ReconfigurationProblem rp = new DefaultReconfigurationProblemBuilder(mo)
                .setDurationEvaluatators(dev)
                .labelVariables()
                .setNextVMsStates(new HashSet<VM>(), map.getAllVMs(), new HashSet<VM>(), new HashSet<VM>())
                .build();
        ResumeVMModel m1 = (ResumeVMModel) rp.getVMActions()[rp.getVM(vm1)];
        ResumeVMModel m2 = (ResumeVMModel) rp.getVMActions()[rp.getVM(vm2)];
        rp.getNodeActions()[0].getState().setVal(1);
        rp.getNodeActions()[1].getState().setVal(1);
        CPSolver s = rp.getSolver();
        s.post(s.geq(m2.getStart(), m1.getEnd()));

        ReconfigurationPlan p = rp.solve(0, false);
        Assert.assertNotNull(p);
        Iterator<Action> ite = p.iterator();
        ResumeVM b1 = (ResumeVM) ite.next();
        ResumeVM b2 = (ResumeVM) ite.next();
        Assert.assertEquals(vm1, b1.getVM());
        Assert.assertEquals(vm2, b2.getVM());
        Assert.assertTrue(b1.getEnd() <= b2.getStart());
        Assert.assertEquals(5, b1.getEnd() - b1.getStart());
        Assert.assertEquals(5, b2.getEnd() - b2.getStart());
    }
}
