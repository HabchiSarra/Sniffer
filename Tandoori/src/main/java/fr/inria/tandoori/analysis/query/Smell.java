package fr.inria.tandoori.analysis.query;

import java.util.Map;
import java.util.Objects;

public class Smell {
    public final String type;
    public final String commitSha;
    public final String instance;
    public final String file;
    public String parentInstance;

    public Smell(String type, String commitSha, String instance, String file) {
        this.type = type;
        this.commitSha = commitSha;
        this.instance = instance;
        this.file = file;
        this.parentInstance = null;
    }


    public static Smell fromInstance(Map<String, Object> smell, String type) {
        String sha1 = (String) smell.get("key");
        String identifier = (String) smell.get("instance");
        String file = (String) smell.get("file_path");
        return new Smell(type, sha1, identifier, file);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Smell smell = (Smell) o;
        return Objects.equals(type, smell.type) &&
                Objects.equals(instance, smell.instance);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, instance);
    }
}
