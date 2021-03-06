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

package btrplace.model.constraint;

import btrplace.model.Node;
import btrplace.model.VM;
import btrplace.model.constraint.checker.GatherChecker;
import btrplace.model.constraint.checker.SatConstraintChecker;

import java.util.Collection;
import java.util.Collections;

/**
 * A constraint to force a set of VMs, if running, to be
 * hosted on the same node.
 * <p/>
 * If the restriction is discrete, VMs may then be temporary not co-located during the reconfiguration process but they
 * are ensure of being co-located at the end of the reconfiguration.
 * <p/>
 * If the restriction is continuous, VMs will always be co-located. In practice, if the VMs are all running, they
 * have to already be co-located and it will not possible to relocate them to avoid a potential temporary separation.
 *
 * @author Fabien Hermenier
 */
public class Gather extends SatConstraint {

    /**
     * Make a new constraint with a discrete restriction.
     *
     * @param vms the VMs to group
     */
    public Gather(Collection<VM> vms) {
        this(vms, false);
    }

    /**
     * Make a new constraint.
     *
     * @param vms        the VMs to group
     * @param continuous {@code true} for a continuous restriction
     */
    public Gather(Collection<VM> vms, boolean continuous) {
        super(vms, Collections.<Node>emptySet(), continuous);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Gather that = (Gather) o;
        return getInvolvedVMs().equals(that.getInvolvedVMs());
    }

    @Override
    public int hashCode() {
        return getInvolvedVMs().hashCode();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("gather(")
                .append("vms=").append(getInvolvedVMs());

        if (!isContinuous()) {
            sb.append(", discrete");
        } else {
            sb.append(", continuous");
        }
        return sb.append(')').toString();
    }

    @Override
    public SatConstraintChecker getChecker() {
        return new GatherChecker(this);
    }

}
