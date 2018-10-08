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
