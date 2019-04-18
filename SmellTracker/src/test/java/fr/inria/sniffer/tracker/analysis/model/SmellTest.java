/**
 *   Sniffer - Analyze the history of Android code smells at scale.
 *   Copyright (C) 2019 Sarra Habchi
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU Affero General Public License as published
 *   by the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU Affero General Public License for more details.
 *
 *   You should have received a copy of the GNU Affero General Public License
 *   along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package fr.inria.sniffer.tracker.analysis.model;

import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

public class SmellTest {
   private Smell first;
   private Smell second;
   private Smell third;

    @Before
    public void setUp() throws Exception {
        first = new Smell("a", "b", "c");
        second = new Smell("a", "b", "c");
        third = new Smell("d", "b", "c");
    }

    @Test
    public void testSmellEquality() {
        assertEquals(first, second);
        assertNotEquals(first, third);
        second.parent = first;
        assertNotEquals(first, second);
    }

    @Test
    public void testSmellHashcodeEquality() {
        assertEquals(first.hashCode(), second.hashCode());
        assertNotEquals(first.hashCode(), third.hashCode());
        second.parent = first;
        assertNotEquals(first.hashCode(), second.hashCode());
    }

    @Test
    public void testHashMap() {
        Map<Smell, Smell> myMap = new HashMap<>();
        myMap.put(first, third);
        assertEquals(third, myMap.get(first));
        assertEquals(third, myMap.get(second));
        assertEquals(third, myMap.get(Smell.copyWithoutParent(first)));

        second.parent = first;
        third.parent = second;
        myMap.put(third, first);
        assertEquals(first, myMap.get(third));
        assertTrue(myMap.containsKey(myMap.keySet().toArray()[0]));
    }
}
