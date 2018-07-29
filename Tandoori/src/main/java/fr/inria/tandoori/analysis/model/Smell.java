package fr.inria.tandoori.analysis.model;

import java.util.Map;
import java.util.Objects;

public class Smell {
    public final String type;
    public final String instance;
    public final String file;
    public String parentInstance;

    public Smell(String type, String instance, String file) {
        this.type = type;
        this.instance = instance;
        this.file = file;
        this.parentInstance = null;
    }


    public static Smell fromPaprikaInstance(Map<String, Object> smell, String type) {
        String identifier = (String) smell.get("instance");
        // The files given by paprika will have a leading '/'
        String file = ((String) smell.get("file_path")).substring(1);
        return new Smell(type, identifier, file);
    }

    public static Smell fromTandooriInstance(Map<String, Object> smell) {
        String type = ((String) smell.get("type"));
        String identifier = (String) smell.get("instance");
        String file = (String) smell.get("file");
        return new Smell(type, identifier, file);
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

    @Override
    public String toString() {
        return "Smell{" +
                "type='" + type + '\'' +
                ", instance='" + instance + '\'' +
                ", file='" + file + '\'' +
                ", parentInstance='" + parentInstance + '\'' +
                '}';
    }
}
