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
package fr.inria.sniffer.tracker.analysis.query.smell;

import fr.inria.sniffer.tracker.analysis.model.Smell;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

public class SmellTest {

    private Smell smell;
    private String anotherFile;
    private String anotherType;
    private String anotherInstance;

    @Before
    public void setUp() throws Exception {
        smell = new Smell("MIM", "my#instance.path$id", "instance/Path.java");
        anotherFile = "another/app/instance/Path.java";
        anotherType = "HMU";
        anotherInstance = "my#other.instance.path$id";
    }

    @Test
    public void testSameInstanceAndTypeAreEquals() {
        Smell differentFile = new Smell(smell.type, smell.instance, anotherFile);
        Smell sameSmell = new Smell(smell.type, smell.instance, smell.file);

        assertNotEquals(smell, differentFile);
        assertEquals(smell, sameSmell);
    }

    @Test
    public void testSameInstanceButNotTypeAreNotEquals() {
        Smell differentFile = new Smell(anotherType, smell.instance, anotherFile);
        Smell sameSmell = new Smell(anotherType, smell.instance, smell.file);

        assertNotEquals(smell, differentFile);
        assertNotEquals(smell, sameSmell);
    }

    @Test
    public void testDifferentInstanceButSameTypeAreNotEquals() {
        Smell differentFile = new Smell(smell.type,  anotherInstance, anotherFile);
        Smell sameSmell = new Smell(smell.type, anotherInstance, smell.file);

        assertNotEquals(smell, differentFile);
        assertNotEquals(smell, sameSmell);
    }

    @Test
    public void testNothingInCommonAreNotEquals() {
        Smell differentSmell = new Smell(anotherType, anotherInstance, anotherFile);

        assertNotEquals(smell, differentSmell);
    }

    @Test
    public void testListContains() {
        List<Smell> smellList = new ArrayList<>();
        smellList.add(smell);
        Smell sameSmell = new Smell(smell.type, smell.instance, smell.file);

        assertTrue(smellList.contains(sameSmell));
    }

    @Test
    public void testMapContains() {
        Map<Smell, Smell> smellList = new HashMap<>();
        smellList.put(smell, smell);
        Smell sameSmell = new Smell(smell.type, smell.instance, smell.file);

        assertTrue(smellList.containsKey(sameSmell));
        assertTrue(smellList.containsValue(sameSmell));
    }
}