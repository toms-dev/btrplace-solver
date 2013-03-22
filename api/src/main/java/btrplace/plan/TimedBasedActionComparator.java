package btrplace.plan;

import java.util.Comparator;

/**
 * A comparator to sort the actions in the increasing order of their start moment.
 * If they start at the same moment, the action that ends first in considered.
 * <p/>
 * It is possible to indicate the comparator to differentiate two actions that are simultaneous but not equals.
 * This is meaningful to sort set of actions as otherwise, actions that are equals
 * wrt. to the comparator will be removed from the set while being different wrt. their {@code equals()} method.
 *
 * @author Fabien Hermenier
 */
public class TimedBasedActionComparator implements Comparator<Action> {

    private boolean diffSimultaneous = false;

    /**
     * New comparator that does not differentiate
     * simultaneous actions
     */
    public TimedBasedActionComparator() {
        this(false);
    }

    /**
     * New comparator
     *
     * @param diffSimultaneous {@code true} to differentiate simultaneous actions.
     */
    public TimedBasedActionComparator(boolean diffSimultaneous) {
        this.diffSimultaneous = diffSimultaneous;
    }

    @Override
    public int compare(Action a1, Action a2) {
        int d = a1.getStart() - a2.getStart();
        if (d == 0) {
            if (a1.equals(a2)) {
                return 0;
            } else {
                d = a1.getEnd() - a2.getEnd();
                //At this level we don't care but we must not return 0 because the action will
                //not be added
                if (diffSimultaneous && d == 0) {
                    return -1;
                }
                return d;
            }
        } else {
            return d;
        }
    }
}
