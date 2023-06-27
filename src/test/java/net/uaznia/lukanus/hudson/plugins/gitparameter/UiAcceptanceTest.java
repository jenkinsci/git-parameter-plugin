package net.uaznia.lukanus.hudson.plugins.gitparameter;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.apache.commons.lang.StringUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.recipes.LocalData;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

import static org.junit.Assert.assertTrue;

public class UiAcceptanceTest {
    @Rule
    public JenkinsRule j = new JenkinsRule();

    private WebDriver driver;

    @BeforeClass
    public static void setUpClass() {
        if (isCi()) {
            // The browserVersion needs to match what is provided by the Jenkins Infrastructure
            // If you see an exception like this:
            //
            // org.openqa.selenium.SessionNotCreatedException: Could not start a new session. Response code 500. Message: session not created: This version of ChromeDriver only supports Chrome version 114
            // Current browser version is 112.0.5615.49 with binary path /usr/bin/chromium-browser
            //
            // Then that means you need to update the version here to match the current browser version.
            WebDriverManager.chromedriver().browserVersion("112").setup();
        } else {
            WebDriverManager.chromedriver().setup();
        }
    }

    private static boolean isCi() {
        return StringUtils.isNotBlank(System.getenv("CI"));
    }

    @Before
    public void setUp() throws Exception {
        if (isCi()) {
            driver = new ChromeDriver(new ChromeOptions().addArguments("--headless", "--disable-dev-shm-usage", "--no-sandbox"));
        } else {
            driver = new ChromeDriver(new ChromeOptions());
        }
    }

    @After
    public void tearDown() {
        driver.quit();
    }

    @LocalData
    @Test
    public void test() throws Exception {
        driver.get(j.getURL().toString() + "job/test/build");

        WebElement branchParam = driver.findElement(byParamName("Branch"));
        WebElement tagParam = driver.findElement(byParamName("Tag"));
        WebElement revisionParam = driver.findElement(byParamName("Revision"));

        new WebDriverWait(driver, Duration.ofSeconds(60))
                .until(driver1 -> new Select(driver1.findElement(byParamName("Revision"))).getOptions().size() > 1);

        assertTrue(new Select(branchParam).getOptions().stream().anyMatch(option -> option.getAttribute("value").equals("origin/master")));
        assertTrue(new Select(tagParam).getOptions().stream().anyMatch(option -> option.getAttribute("value").equals("git-parameter-0.9.7")));
        assertTrue(new Select(revisionParam).getOptions().stream().anyMatch(option -> option.getAttribute("value").equals("00a8385cba1e4e32cf823775e2b3dbe5eb27931d")));

    }

    private static By byParamName(String paramName) {
        return By.cssSelector("div[name='parameter']:has([name='name'][value='" + paramName + "']) > select");
    }

}
