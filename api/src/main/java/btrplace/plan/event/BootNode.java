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

package btrplace.plan.event;


import btrplace.model.Model;
import btrplace.model.Node;

import java.util.Objects;

/**
 * An action to boot an offline node.
 * Once the execution is finished, the node is online.
 *
 * @author Fabien Hermenier
 */
public class BootNode extends Action implements NodeEvent {

    private Node node;

    /**
     * Create a new action on an offline node.
     *
     * @param node  The node to boot
     * @param start the moment the action starts
     * @param end   the moment the action is finished
     */
    public BootNode(Node node, int start, int end) {
        super(start, end);
        this.node = node;
    }

    /**
     * Test the equality with another object.
     *
     * @param obj The object to compare with
     * @return {@code true} if {@code obj} is an instance of {@link BootNode}
     *         and if both actions act on the same node
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        } else if (obj == this) {
            return true;
        } else if (obj.getClass() == this.getClass()) {
            BootNode that = (BootNode) obj;
            return this.node.equals(that.node) &&
                    this.getStart() == that.getStart() &&
                    this.getEnd() == that.getEnd();
        }
        return false;
    }

    @Override
    public Node getNode() {
        return node;
    }

    @Override
    public int hashCode() {
        return Objects.hash(getStart(), getEnd(), node);
    }

    @Override
    public String pretty() {
        StringBuilder buffer = new StringBuilder("boot(");
        buffer.append("node=").append(node).append(")");
        return buffer.toString();
    }

    /**
     * Put the node online on the model.
     *
     * @param c the model to alter
     * @return {@code true} iff the node was offline and is now online
     */
    @Override
    public boolean applyAction(Model c) {
        if (c.getMapping().getOfflineNodes().contains(node)) {
            c.getMapping().addOnlineNode(node);
            return true;
        }
        return false;
    }

    @Override
    public Object visit(ActionVisitor v) {
        return v.visit(this);
    }
}
