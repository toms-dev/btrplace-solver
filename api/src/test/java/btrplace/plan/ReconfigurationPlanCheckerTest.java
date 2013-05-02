package btrplace.plan;

import btrplace.model.DefaultMapping;
import btrplace.model.DefaultModel;
import btrplace.model.Mapping;
import btrplace.model.Model;
import btrplace.model.constraint.checker.SatConstraintChecker;
import btrplace.plan.event.BootNode;
import btrplace.plan.event.BootVM;
import btrplace.plan.event.MigrateVM;
import btrplace.test.PremadeElements;
import org.mockito.InOrder;
import org.testng.Assert;
import org.testng.annotations.Test;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;

/**
 * Unit tests for {@link ReconfigurationPlanChecker}.
 *
 * @author Fabien Hermenier
 */
public class ReconfigurationPlanCheckerTest implements PremadeElements {


    @Test
    public void tesAddandRemove() {
        ReconfigurationPlanChecker rc = new ReconfigurationPlanChecker();
        SatConstraintChecker chk = mock(SatConstraintChecker.class);
        Assert.assertTrue(rc.addChecker(chk));
        Assert.assertTrue(rc.removeChecker(chk));
        Assert.assertFalse(rc.removeChecker(chk));
    }

    @Test(dependsOnMethods = {"tesAddandRemove"})
    public void testSequencing() throws ReconfigurationPlanCheckerException {
        Mapping m = new DefaultMapping();
        m.addOnlineNode(n1);
        m.addOnlineNode(n2);
        m.addOfflineNode(n4);
        m.addReadyVM(vm2);
        m.addRunningVM(vm1, n1);
        Model mo = new DefaultModel(m);
        ReconfigurationPlan p = new DefaultReconfigurationPlan(mo);
        SatConstraintChecker chk = mock(SatConstraintChecker.class);
        MigrateVM m1 = new MigrateVM(vm1, n1, n2, 0, 3);
        BootVM b1 = new BootVM(vm2, n1, 1, 5);
        BootNode bn1 = new BootNode(n4, 3, 6);
        p.add(m1);
        p.add(b1);
        p.add(bn1);
        Model res = p.getResult();
        Assert.assertNotNull(res);
        ReconfigurationPlanChecker rc = new ReconfigurationPlanChecker();

        rc.addChecker(chk);

        InOrder order = inOrder(chk);
        rc.check(p);
        order.verify(chk).startsWith(mo);
        order.verify(chk).start(m1);
        order.verify(chk).start(b1);
        order.verify(chk).end(m1);
        order.verify(chk).start(bn1);
        order.verify(chk).end(b1);
        order.verify(chk).end(bn1);
        order.verify(chk).endsWith(res);
    }

    @Test
    public void testWithNoActions() throws ReconfigurationPlanCheckerException {
        Mapping m = new DefaultMapping();
        m.addOnlineNode(n1);
        m.addOnlineNode(n2);
        m.addOfflineNode(n4);
        m.addReadyVM(vm2);
        m.addRunningVM(vm1, n1);
        Model mo = new DefaultModel(m);
        ReconfigurationPlan p = new DefaultReconfigurationPlan(mo);
        SatConstraintChecker chk = mock(SatConstraintChecker.class);
        ReconfigurationPlanChecker rc = new ReconfigurationPlanChecker();
        Assert.assertTrue(rc.addChecker(chk));

        InOrder order = inOrder(chk);
        rc.check(p);
        order.verify(chk).startsWith(mo);
        order.verify(chk).endsWith(mo);

    }
}