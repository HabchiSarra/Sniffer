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
package fr.inria.sniffer.tracker.analysis;

import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;
import net.sourceforge.argparse4j.inf.Subparser;
import net.sourceforge.argparse4j.inf.Subparsers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {
    // TODO: tidy up this part
    public static final String DATABASE_URL = "//127.0.0.1:5432/tracker";
    public static final String DATABASE_USERNAME = "tracker";
    public static final String DATABASE_PASSWORD = "tracker";
    public static final String GITHUB_URL = "https://github.com/";

    private static final Logger logger = LoggerFactory.getLogger(Main.class.getName());
    private static final String COMMAND_KEY = "sub_command";
    private static final String APP_ANALYSIS_COMMAND = "singleAnalysis";
    private static final String SUPP_ANALYSIS_COMMAND = "supplementaryAnalysis";
    private static final String MULTI_ANALYSIS_COMMAND = "multiAnalysis";

    public static void main(String[] args) {
        ArgumentParser parser = ArgumentParsers.newArgumentParser("tracker");
        Subparsers subparsers = parser.addSubparsers().dest(COMMAND_KEY);

        Subparser analyseParser = subparsers.addParser(APP_ANALYSIS_COMMAND).help("Analyse a single app");
        SingleAppAnalysis.setArguments(analyseParser);

        Subparser supplementaryParser = subparsers.addParser(SUPP_ANALYSIS_COMMAND).help("Supplementary app analysis");
        SupplementaryAnalysis.setArguments(supplementaryParser);

        Subparser multiAppParser = subparsers.addParser(MULTI_ANALYSIS_COMMAND).help("Analyse multiple apps");
        MultiAppAnalysis.setArguments(multiAppParser);


        try {
            Namespace res = parser.parseArgs(args);
            switch (res.getString(COMMAND_KEY)) {
                case APP_ANALYSIS_COMMAND:
                    new SingleAppAnalysis(res).analyze();
                    break;
                case MULTI_ANALYSIS_COMMAND:
                    new MultiAppAnalysis(res).analyze();
                    break;
                case SUPP_ANALYSIS_COMMAND:
                    new SupplementaryAnalysis(res).analyze();
                    break;
                default:
                    logger.error("Unable to find command: " + res.getString(COMMAND_KEY));
            }
        } catch (ArgumentParserException e) {
            analyseParser.handleError(e);
        } catch (Exception | AnalysisException e) {
            logger.error("Error on analysis!", e);
        }
    }
}
