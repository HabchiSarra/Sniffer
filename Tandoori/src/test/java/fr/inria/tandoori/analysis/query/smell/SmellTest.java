package fr.inria.tandoori.analysis.query.smell;

import fr.inria.tandoori.analysis.model.Smell;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

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