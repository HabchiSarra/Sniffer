package fr.inria.tandoori.analysis;

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
    public static final String DATABASE_URL = "//127.0.0.1:5432/tandoori";
    public static final String DATABASE_USERNAME = "tandoori";
    public static final String DATABASE_PASSWORD = "tandoori";
    public static final String GITHUB_URL = "https://github.com/";

    private static final Logger logger = LoggerFactory.getLogger(Main.class.getName());
    private static final String COMMAND_KEY = "sub_command";
    private static final String APP_ANALYSIS_COMMAND = "singleAnalysis";
    private static final String MULTI_ANALYSIS_COMMAND = "multiAnalysis";

    public static void main(String[] args) {
        ArgumentParser parser = ArgumentParsers.newArgumentParser("tandoori");
        Subparsers subparsers = parser.addSubparsers().dest(COMMAND_KEY);

        Subparser analyseParser = subparsers.addParser(APP_ANALYSIS_COMMAND).help("Analyse a single app");
        SingleAppAnalysis.setArguments(analyseParser);

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
                default:
                    logger.error("Unable to find command: " + res.getString(COMMAND_KEY));
            }
        } catch (ArgumentParserException e) {
            analyseParser.handleError(e);
        } catch (Exception e) {
            logger.error("Error on analysis!", e);
        }
    }
}
