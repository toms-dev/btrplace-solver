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

package btrplace.json.model.constraint;

import btrplace.json.JSONConverterException;
import btrplace.model.DefaultModel;
import btrplace.model.Model;
import btrplace.model.Node;
import btrplace.model.VM;
import btrplace.model.constraint.SplitAmong;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;


/**
 * Unit tests for {@link btrplace.json.model.constraint.SplitAmongConverter}.
 *
 * @author Fabien Hermenier
 */
public class SplitAmongConverterTest {

    @Test
    public void testViables() throws JSONConverterException, IOException {
        Model mo = new DefaultModel();
        Collection<VM> s1 = new HashSet<>(Arrays.asList(mo.newVM(), mo.newVM(), mo.newVM()));
        Collection<VM> s2 = new HashSet<>(Arrays.asList(mo.newVM(), mo.newVM(), mo.newVM()));
        Collection<VM> s3 = new HashSet<>(Arrays.asList(mo.newVM(), mo.newVM()));
        Collection<Collection<VM>> vgrps = new HashSet<>(Arrays.asList(s1, s2, s3));

        Collection<Node> p1 = new HashSet<>(Arrays.asList(mo.newNode(), mo.newNode()));
        Collection<Node> p2 = new HashSet<>(Arrays.asList(mo.newNode(), mo.newNode()));
        Collection<Node> p3 = new HashSet<>(Arrays.asList(mo.newNode()));
        Collection<Collection<Node>> pgrps = new HashSet<>(Arrays.asList(p1, p2, p3));

        SplitAmong d = new SplitAmong(vgrps, pgrps, false);
        SplitAmong c = new SplitAmong(vgrps, pgrps, true);
        SplitAmongConverter conv = new SplitAmongConverter();
        conv.setModel(mo);

        Assert.assertEquals(conv.fromJSON(conv.toJSONString(d)), d);
        Assert.assertEquals(conv.fromJSON(conv.toJSONString(c)), c);
    }
}
