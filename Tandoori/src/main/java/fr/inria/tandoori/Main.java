package fr.inria.tandoori;

import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;
import net.sourceforge.argparse4j.inf.Subparser;
import net.sourceforge.argparse4j.inf.Subparsers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class.getName());
    private static final String COMMAND_KEY = "sub_command";
    private static final String APP_ANALYSIS_COMMAND = "singleAnalysis";

    public static void main(String[] args) {
        ArgumentParser parser = ArgumentParsers.newArgumentParser("tandoori");
        Subparsers subparsers = parser.addSubparsers().dest(COMMAND_KEY);

        Subparser analyseParser = subparsers.addParser(APP_ANALYSIS_COMMAND).help("Analyse a single app");
        SingleAppAnalysis.setArguments(analyseParser);

        // TODO: Accept a CSV list of applications

        try {
            Namespace res = parser.parseArgs(args);
            switch (res.getString(COMMAND_KEY)) {
                case APP_ANALYSIS_COMMAND:
                    new SingleAppAnalysis(res).start();
                default:
                    logger.error("Unable to find command: " + res.getString(COMMAND_KEY));
            }
        } catch (ArgumentParserException e) {
            analyseParser.handleError(e);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
