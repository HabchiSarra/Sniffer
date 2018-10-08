package fr.inria.sniffer.tracker.analysis.query;


/**
 * Enhance developers data with start, contributors, and every available data.
 */
public class DevelopersQuery implements Query {
    private static final String GITHUB_API_URL = "https://api.github.com/repos/";
    private final String repo;
    private final String token;

    public DevelopersQuery(String repo, String token) {
        this.repo = repo;
        this.token = token;
    }

    @Override
    public void query() {
//        ContentFetcher fetcher = new ContentFetcher(token);
//        Repository repository = fetcher.getRepository(getUrl(repo));
//
//         TODO insert result in SQL Database
//        ModelToGraph modelToGraph = new ModelToGraph("developerDB");
//        modelToGraph.insertRepository(repository);
//        modelToGraph.closeDB();

        // Retrieve the developers and save them as CSV
//        QueryEngine queryEngine = new QueryEngine(databasePath);
//        CommitsQuery commitsQuery = CommitsQuery.createCommitsQuery(queryEngine);
//        try {
//            commitsQuery.execute();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
    }

    private String getUrl(String repo) {
        return GITHUB_API_URL + repo;
    }
}
