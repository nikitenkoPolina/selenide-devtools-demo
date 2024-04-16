package selenide.devtools.demo.service;

import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.devtools.DevTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.codeborne.selenide.Selenide.open;
import static com.codeborne.selenide.WebDriverRunner.getWebDriver;

public class DevToolsService {

    final Logger logger = LoggerFactory.getLogger(DevToolsService.class);

    /**
     * Метод для создания и настройки доступа к Developer Tools
     *
     * @return devTools - объект класса DevTools
     */
    public DevTools connectDTools() {

        open();
        logger.info("Настройка ChromeDriver");
        ChromeDriver driver = (ChromeDriver) getWebDriver();

        logger.info("Получим доступ к Developer Tools");
        DevTools devTools = driver.getDevTools();

        logger.info("Создадим сессию в ChromeDriver");
        devTools.createSession();

//        ChromeDriver driver = new ChromeDriver();
//
//        DevTools chromeDevTools = driver.getDevTools();
//        chromeDevTools.createSession();

        return devTools;
    }
}
