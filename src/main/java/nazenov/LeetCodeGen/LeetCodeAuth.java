package nazenov.LeetCodeGen;

import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import io.github.bonigarcia.wdm.WebDriverManager;
import java.io.*;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class LeetCodeAuth {
    private WebDriver driver;
    private final String cookiesFile = "leetcode_cookies.txt";

    public boolean login() {
        try {
            WebDriverManager.chromedriver().setup();
            
            ChromeOptions options = new ChromeOptions();
            options.addArguments("--start-maximized");
            options.addArguments("--disable-dev-shm-usage");
            options.addArguments("--no-sandbox");
            options.addArguments("--disable-gpu");
            options.addArguments("--disable-extensions");
            options.addArguments("--disable-infobars");
            options.addArguments("--disable-notifications");
            options.addArguments("--disable-popup-blocking");
            options.addArguments("--disable-web-security");
            options.addArguments("--ignore-certificate-errors");
            options.addArguments("--allow-running-insecure-content");
            
            driver = new ChromeDriver(options);
            driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));
            driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(30));
            
            // Переходим на страницу входа
            driver.get("https://leetcode.com/accounts/login/");
            
            // Ждем загрузки страницы
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(30));
            
            // Пробуем разные селекторы для поля ввода логина
            By[] loginSelectors = {
                By.id("id_login"),
                By.name("login"),
                By.cssSelector("input[name='login']"),
                By.xpath("//input[@name='login']")
            };
            
            WebElement loginField = null;
            for (By selector : loginSelectors) {
                try {
                    loginField = wait.until(ExpectedConditions.presenceOfElementLocated(selector));
                    if (loginField != null) break;
                } catch (TimeoutException e) {
                    continue;
                }
            }
            
            if (loginField == null) {
                System.out.println("Не удалось найти поле ввода логина. Пожалуйста, проверьте страницу вручную.");
                waitForManualLogin();
            } else {
                System.out.println("Поле ввода логина найдено. Пожалуйста, выполните вход вручную в открывшемся браузере.");
                System.out.println("После успешного входа нажмите Enter в консоли...");
                waitForManualLogin();
            }
            
            // Проверяем успешность входа
            wait.until(ExpectedConditions.urlContains("leetcode.com"));
            
            // Сохраняем куки
            saveCookies();
            
            return true;
        } catch (Exception e) {
            System.err.println("Ошибка при входе: " + e.getMessage());
            e.printStackTrace();
            return false;
        } finally {
            if (driver != null) {
                driver.quit();
            }
        }
    }

    private void waitForManualLogin() {
        Scanner scanner = new Scanner(System.in);
        scanner.nextLine();
        scanner.close();
    }

    private void saveCookies() {
        try {
            File file = new File(cookiesFile);
            if (!file.exists()) {
                file.createNewFile();
            }
            
            try (PrintWriter writer = new PrintWriter(new FileWriter(file))) {
                for (Cookie cookie : driver.manage().getCookies()) {
                    writer.println(cookie.getName() + "=" + cookie.getValue());
                }
            }
        } catch (IOException e) {
            System.err.println("Ошибка при сохранении куки: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private boolean loadCookies() {
        try {
            File file = new File(cookiesFile);
            if (!file.exists()) {
                return false;
            }
            
            Map<String, String> cookies = new HashMap<>();
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String[] parts = line.split("=", 2);
                    if (parts.length == 2) {
                        cookies.put(parts[0], parts[1]);
                    }
                }
            }
            
            if (!cookies.isEmpty()) {
                WebDriverManager.chromedriver().setup();
                ChromeOptions options = new ChromeOptions();
                options.addArguments("--start-maximized");
                options.addArguments("--disable-dev-shm-usage");
                options.addArguments("--no-sandbox");
                options.addArguments("--disable-gpu");
                options.addArguments("--disable-extensions");
                options.addArguments("--disable-infobars");
                options.addArguments("--disable-notifications");
                options.addArguments("--disable-popup-blocking");
                options.addArguments("--disable-web-security");
                options.addArguments("--ignore-certificate-errors");
                options.addArguments("--allow-running-insecure-content");
                
                driver = new ChromeDriver(options);
                
                // Добавляем куки в браузер
                for (Map.Entry<String, String> entry : cookies.entrySet()) {
                    Cookie cookie = new Cookie(entry.getKey(), entry.getValue());
                    driver.manage().addCookie(cookie);
                }

                // Переходим на страницу входа
                driver.get("https://leetcode.com/accounts/login/");
                
                // Ждем загрузки страницы
                WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(30));
                
                // Находим и нажимаем кнопку "Sign in"
                try {
                    By signInButtonSelector = By.xpath("//a[contains(@href, '/accounts/login/')]//span[text()='Sign in']");
                    WebElement signInButton = wait.until(ExpectedConditions.elementToBeClickable(signInButtonSelector));
                    signInButton.click();
                } catch (Exception e) {
                    System.err.println("Не удалось найти кнопку Sign in: " + e.getMessage());
                }
                
                return true;
            }
        } catch (IOException e) {
            System.err.println("Ошибка при загрузке куки: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    public boolean isLoggedIn() {
        try {
            if (!loadCookies()) {
                return false;
            }
            
            driver.get("https://leetcode.com/api/problems/all/");
            
            // Ждем загрузки страницы
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(30));
            wait.until(ExpectedConditions.urlContains("leetcode.com"));
            
            return driver.getCurrentUrl().contains("leetcode.com");
        } catch (Exception e) {
            System.err.println("Ошибка при проверке авторизации: " + e.getMessage());
            e.printStackTrace();
            return false;
        } finally {
            if (driver != null) {
                driver.quit();
            }
        }
    }
} 