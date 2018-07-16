package fr.inria.tandoori.analysis.query;

import fr.inria.tandoori.analysis.persistence.Persistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SmellTypeAnalysis implements Query {
    private static final Logger logger = LoggerFactory.getLogger(SmellTypeAnalysis.class.getName());

    private final int projectId;
    private final Persistence persistence;
    private final List<Smell> previousCommitSmells;
    private final List<Smell> currentCommitSmells;
    private final List<Smell> currentCommitOriginal;
    private final List<Smell> currentCommitRenamed;
    private final List<Map<String, Object>> smells;
    private String smellType;
    private final SmellDuplicationChecker duplicationChecker;

    public SmellTypeAnalysis(int projectId, Persistence persistence, List<Map<String, Object>> smells,
                             String smellType, SmellDuplicationChecker duplicationChecker) {
        this.projectId = projectId;
        this.persistence = persistence;
        this.smells = smells;
        this.smellType = smellType;
        this.duplicationChecker = duplicationChecker;
        previousCommitSmells = new ArrayList<>();
        currentCommitSmells = new ArrayList<>();
        currentCommitOriginal = new ArrayList<>();
        currentCommitRenamed = new ArrayList<>();
    }


    @Override
    public void query() {
        String currentSha = "";
        Smell currentSmell;
        for (Map<String, Object> instance : smells) {
            // We keep track of the smells present in our commit.
            currentSmell = Smell.fromInstance(instance, smellType);
            if (!currentSha.equals(currentSmell.commitSha)) {
                currentSha = changeCurrentCommit(currentSmell.commitSha);
            }
            currentCommitSmells.add(currentSmell);

            /* In this test we have a smell with its file directing to a newly renamed file.
             * We will have to guess the previous smell instance by rewriting its instance id with the file before being renamed.
             * This instance ID will then be compared with the list of instances from the previous smell to find a match.
             */
            Smell original = duplicationChecker.original(currentSmell);
            // If we correctly guessed the smell identifier, we will find it in the previous commit smells
            if (original != null && previousCommitSmells.contains(original)) {
                logger.debug("[" + projectId + "] => Guessed rename for smell: " + currentSmell);
                logger.trace("[" + projectId + "]   => potential parent: " + original);
                currentCommitOriginal.add(original);
                currentCommitRenamed.add(currentSmell);
                currentSmell.parentInstance = original.instance;
            }
            if (!previousCommitSmells.contains(currentSmell)) {
                insertSmellInstance(currentSmell);
            }
            insertSmellPresence(currentSmell);
        }
    }

    /**
     * Transfer all smells from current commit to the previous one, and change the current sha.
     *
     * @param commitSha The new sha.
     */
    private String changeCurrentCommit(String commitSha) {
        logger.debug("[" + projectId + "] ==> Handling commit: " + commitSha);
        if (logger.isTraceEnabled()) {
            traceCommitIdentifier(commitSha);
        }
        insertSmellIntroductions();
        insertSmellRefactoring();

        currentCommitOriginal.clear();
        currentCommitRenamed.clear();
        previousCommitSmells.clear();
        previousCommitSmells.addAll(currentCommitSmells);
        currentCommitSmells.clear();

        return commitSha;
    }

    private void insertSmellInstance(Smell smell) {
        // We know that the parent smell is the last inserted one.
        String parentSmellQuery = persistence.smellQueryStatement(projectId, smell.parentInstance, smell.type, true);
        String parentQuery = smell.parentInstance != null ? "(" + parentSmellQuery + ")" : null;
        String smellInsert = "INSERT INTO Smell (projectId, instance, type, file, renamedFrom) VALUES" +
                "(" + projectId + ", '" + smell.instance + "', '" + smell.type + "', '" + smell.file + "', " + parentQuery + ") ON CONFLICT DO NOTHING;";
        persistence.addStatements(smellInsert);
    }

    /**
     * Trace the identifier of the currently analyzed commit.
     *
     * @param commitSha The sha to print ID for.
     */
    private void traceCommitIdentifier(String commitSha) {
        String commitQuery = persistence.commitQueryStatement(this.projectId, commitSha);
        List<Map<String, Object>> result = persistence.query(commitQuery);
        if (!result.isEmpty()) {
            logger.trace("[" + projectId + "]  => commit id: " + String.valueOf(result.get(0).get("id")));
        } else {
            logger.trace("[" + projectId + "] NO FOUND COMMIT!");
        }
    }

    private void insertSmellPresence(Smell smell) {
        insertSmellInCategory(smell, "SmellPresence");
    }

    private void insertSmellIntroductions() {
        List<Smell> introduction = new ArrayList<>(currentCommitSmells);
        introduction.removeAll(previousCommitSmells);

        for (Smell smell : introduction) {
            if (!currentCommitRenamed.contains(smell)) {
                insertSmellInCategory(smell, "SmellIntroduction");
            }
        }
    }

    private void insertSmellRefactoring() {
        List<Smell> refactoring = new ArrayList<>(previousCommitSmells);
        refactoring.removeAll(currentCommitSmells);

        for (Smell smell : refactoring) {
            if (!currentCommitOriginal.contains(smell)) {
                insertSmellInCategory(smell, "SmellRefactor");
            }
        }
    }

    private void insertSmellInCategory(Smell smell, String category) {
        // We fetch only the last matching inserted smell
        // This helps us handling the case of Gaps between commits
        String smellQuery = persistence.smellQueryStatement(projectId, smell.instance, smell.type, true);
        String commitQuery = persistence.commitQueryStatement(this.projectId, smell.commitSha);
        String smellPresenceInsert = "INSERT INTO " + category + " (smellId, commitId) VALUES " +
                "((" + smellQuery + "), (" + commitQuery + "));";
        persistence.addStatements(smellPresenceInsert);
    }
}
