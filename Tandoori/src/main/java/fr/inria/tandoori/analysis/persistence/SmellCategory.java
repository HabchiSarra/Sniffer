package fr.inria.tandoori.analysis.persistence;

/**
 * Represents the different categories in which a smell can be sorted
 * in a smell analysis.
 */
public enum SmellCategory {
    PRESENCE("smell_presence"),
    INTRODUCTION("smell_introduction"),
    REFACTOR("smell_refactoring");

    private final String name;

    SmellCategory(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

}
