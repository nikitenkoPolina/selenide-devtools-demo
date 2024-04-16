package selenide.devtools.demo.extensions;

import com.codeborne.selenide.Configuration;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

public class ConfigExtension implements BeforeAllCallback {

    @Override
    public void beforeAll(ExtensionContext extensionContext) {
        Configuration.browserSize = "1920x1080";
        Configuration.headless = false;
    }
}
