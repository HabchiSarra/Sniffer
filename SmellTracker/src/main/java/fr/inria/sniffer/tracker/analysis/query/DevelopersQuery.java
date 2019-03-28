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
