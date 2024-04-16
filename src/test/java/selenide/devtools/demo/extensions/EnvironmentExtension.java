package selenide.devtools.demo.extensions;

import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Properties;

public class EnvironmentExtension implements ExecutionCondition {
    @Override
    public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext extensionContext) {

        Logger logger = LoggerFactory.getLogger(EnvironmentExtension.class);
        Properties props = new Properties();

        try {
            props.load(EnvironmentExtension.class.getResourceAsStream("/extensions/application.properties"));
            String env = props.getProperty("env");
            if ("qa".equalsIgnoreCase(env)) {
                logger.info("Tests disabled on QA environment");
                return ConditionEvaluationResult.disabled("Test disabled on QA environment");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        logger.info("Tests enabled on DEV environment");
        return ConditionEvaluationResult.enabled("Test enabled on DEV environment");
    }
}
