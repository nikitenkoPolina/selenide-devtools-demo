package selenide.devtools.demo;

import org.junit.jupiter.api.extension.ExtendWith;
import selenide.devtools.demo.extensions.ConditionalExtension;
import selenide.devtools.demo.extensions.ConfigExtension;
import selenide.devtools.demo.extensions.EnvironmentExtension;
import selenide.devtools.demo.extensions.TestResultLoggerExtension;
import selenide.devtools.demo.paramresolver.DevToolsParamResolver;

@ExtendWith({
        DevToolsParamResolver.class,
        TestResultLoggerExtension.class,
        ConfigExtension.class,
        ConditionalExtension.class,
        EnvironmentExtension.class
        //ThrowableExtension.class
})
public abstract class ServiceTest {


}
