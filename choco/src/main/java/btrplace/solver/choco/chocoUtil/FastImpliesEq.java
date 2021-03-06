/*
 * Copyright (c) 2012 University of Nice Sophia-Antipolis
 *
 * This file is part of btrplace.
 *
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
package btrplace.solver.choco.chocoUtil;

import choco.cp.solver.variables.integer.IntVarEvent;
import choco.kernel.common.util.iterators.DisposableIntIterator;
import choco.kernel.solver.ContradictionException;
import choco.kernel.solver.SolverException;
import choco.kernel.solver.constraints.integer.AbstractBinIntSConstraint;
import choco.kernel.solver.variables.integer.IntDomainVar;

/**
 * A fast implementation for BVAR <=> VAR = CSTE
 * <br/>
 *
 * @author Charles Prud'homme
 * @since 29/06/11
 */
public class FastImpliesEq extends AbstractBinIntSConstraint {

    private final int constante;

    /**
     * New instance.
     *
     * @param b        the boolean variable
     * @param var      the variable
     * @param constant the constant to use to set the variable if the boolean variable is set to true
     */
    public FastImpliesEq(IntDomainVar b, IntDomainVar var, int constant) {
        super(b, var);
        if ((!b.isInstantiated() && !b.hasBooleanDomain())
                || (b.isInstantiated() && !b.isInstantiatedTo(0) && !b.isInstantiatedTo(1))) {
            throw new SolverException(b.getName() + " is not a boolean variable");
        }
        this.constante = constant;
    }

    @Override
    public int getFilteredEventMask(int idx) {
        if (idx == 0) {
            return IntVarEvent.INSTINT_MASK;
        } else {
            return IntVarEvent.REMVAL_MASK;
        }
    }

    @Override
    public void propagate() throws ContradictionException {
        if (v0.isInstantiatedTo(1)) {
            v1.instantiate(constante, this, false);
            this.setEntailed();
        }
        if (!v1.canBeInstantiatedTo(constante)) {
            v0.instantiate(0, this, false);
            this.setEntailed();
        }
    }

    @Override
    public void awakeOnInst(int idx) throws ContradictionException {
        if (idx == 0 && v0.getVal() == 1) {
            v1.instantiate(constante, this, false);
        }
    }

    @Override
    public void awakeOnRemovals(int idx, DisposableIntIterator deltaDomain) throws ContradictionException {
        if (idx == 1 && !v1.canBeInstantiatedTo(constante)) {
            v0.instantiate(0, this, false);
            this.setEntailed();
        }
    }

    @Override
    public void awakeOnRem(int varIdx, int val) throws ContradictionException {
        throw new SolverException("foo");
    }

    @Override
    public boolean isSatisfied(int[] tuple) {
        return (tuple[0] == 1 && tuple[1] == constante) || tuple[0] == 0;
    }

    @Override
    public boolean isConsistent() {
        if (vars[0].isInstantiatedTo(1)) {
            return (vars[1].isInstantiatedTo(constante));
        } else if (vars[0].isInstantiatedTo(1)) {
            return (!vars[1].canBeInstantiatedTo(constante));
        }
        return true;
    }

    @Override
    public String toString() {
        return vars[0].pretty() + " -> " + vars[1].pretty() + " = " + constante;
    }

    @Override
    public String pretty() {
        return toString();
    }
}
