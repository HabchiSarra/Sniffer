package fr.inria.tandoori.analysis.query;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

public class SmellTest {

    private Smell smell;
    private String anotherCommit;
    private String anotherFile;
    private String anotherType;
    private String anotherInstance;

    @Before
    public void setUp() throws Exception {
        smell = new Smell("MIM", "commit", "my#instance.path$id", "instance/Path.java");
        anotherCommit = "commit2";
        anotherFile = "another/app/instance/Path.java";
        anotherType = "HMU";
        anotherInstance = "my#other.instance.path$id";
    }

    @Test
    public void testSameInstanceAndTypeAreEquals() {
        Smell differentFile = new Smell(smell.type, smell.commitSha, smell.instance, anotherFile);
        Smell differentCommit = new Smell(smell.type, anotherCommit, smell.instance, smell.file);
        Smell sameSmell = new Smell(smell.type, smell.commitSha, smell.instance, smell.file);

        assertEquals(smell, differentFile);
        assertEquals(smell, differentCommit);
        assertEquals(smell, sameSmell);
    }

    @Test
    public void testSameInstanceButNotTypeAreNotEquals() {
        Smell differentFile = new Smell(anotherType, smell.commitSha, smell.instance, anotherFile);
        Smell differentCommit = new Smell(anotherType, anotherCommit, smell.instance, smell.file);
        Smell sameSmell = new Smell(anotherType, smell.commitSha, smell.instance, smell.file);

        assertNotEquals(smell, differentFile);
        assertNotEquals(smell, differentCommit);
        assertNotEquals(smell, sameSmell);
    }

    @Test
    public void testDifferentInstanceButSameTypeAreNotEquals() {
        Smell differentFile = new Smell(smell.type, smell.commitSha, anotherInstance, anotherFile);
        Smell differentCommit = new Smell(smell.type, anotherCommit, anotherInstance, smell.file);
        Smell sameSmell = new Smell(smell.type, smell.commitSha, anotherInstance, smell.file);

        assertNotEquals(smell, differentFile);
        assertNotEquals(smell, differentCommit);
        assertNotEquals(smell, sameSmell);
    }

    @Test
    public void testNothingInCommonAreNotEquals() {
        Smell differentSmell = new Smell(anotherType, anotherCommit, anotherInstance, anotherFile);

        assertNotEquals(smell, differentSmell);
    }

    @Test
    public void testListContains() {
        List<Smell> smellList = new ArrayList<>();
        smellList.add(smell);
        Smell sameSmell = new Smell(smell.type, smell.commitSha, smell.instance, smell.file);

        assertTrue(smellList.contains(sameSmell));
    }

    @Test
    public void testMapContains() {
        Map<Smell, Smell> smellList = new HashMap<>();
        smellList.put(smell, smell);
        Smell sameSmell = new Smell(smell.type, smell.commitSha, smell.instance, smell.file);

        assertTrue(smellList.containsKey(sameSmell));
        assertTrue(smellList.containsValue(sameSmell));
    }
}