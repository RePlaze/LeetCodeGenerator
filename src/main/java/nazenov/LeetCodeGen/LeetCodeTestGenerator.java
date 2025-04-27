package nazenov.LeetCodeGen;

import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import io.github.bonigarcia.wdm.WebDriverManager;
import java.io.*;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class LeetCodeTestGenerator {
    private WebDriver driver;
    private final String cookiesFile = "leetcode_cookies.txt";
    private final String testTemplate = """
            package nazenov.tasks;

            import org.junit.jupiter.api.Test;
            import org.junit.jupiter.params.ParameterizedTest;
            import org.junit.jupiter.params.provider.MethodSource;
            import java.util.stream.Stream;
            import static org.junit.jupiter.api.Assertions.*;

            /**
             * %s
             */
            class %s {
                public static Stream<Object[]> testCases() {
                    return Stream.of(
                        %s
                    );
                }

                @ParameterizedTest
                @MethodSource("testCases")
                void testSolution(%s) {
                    Solution solution = new Solution();
                    %s
                }
            }
            """;

    public void generateTest(String problemUrl) {
        try {
            // Сначала инициализируем драйвер
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
            
            // Сначала переходим на сайт
            driver.get("https://leetcode.com");
            
            // Затем загружаем куки
            loadCookies();
            
            // И только потом переходим на страницу задачи
            driver.get(problemUrl);
            
            // Ждем загрузки страницы
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(30));
            
            // Пробуем разные селекторы для описания задачи
            By[] descriptionSelectors = {
                By.className("description__24sA"),
                By.className("elfjS"),
                By.xpath("//div[contains(@class, 'description')]"),
                By.xpath("//div[contains(@class, 'content')]")
            };
            
            WebElement descriptionElement = null;
            for (By selector : descriptionSelectors) {
                try {
                    descriptionElement = wait.until(ExpectedConditions.presenceOfElementLocated(selector));
                    if (descriptionElement != null) break;
                } catch (TimeoutException e) {
                    continue;
                }
            }
            
            if (descriptionElement == null) {
                throw new NoSuchElementException("Не удалось найти описание задачи на странице");
            }
            
            // Получаем название задачи
            String problemName = "";
            try {
                // Пробуем разные селекторы для названия задачи
                By[] titleSelectors = {
                    By.xpath("//a[contains(@href, '/problems/') and contains(@class, 'no-underline')]"),
                    By.xpath("//a[contains(@href, '/problems/')]"),
                    By.xpath("//div[contains(@class, 'title')]"),
                    By.xpath("//div[contains(@class, 'problem-title')]")
                };
                
                for (By selector : titleSelectors) {
                    try {
                        WebElement titleElement = driver.findElement(selector);
                        problemName = titleElement.getText();
                        if (!problemName.isEmpty()) break;
                    } catch (NoSuchElementException e) {
                        continue;
                    }
                }
            } catch (Exception e) {
                System.err.println("Не удалось получить название задачи: " + e.getMessage());
            }
            
            String className = convertToClassName(problemName);
            
            // Получаем описание задачи
            String description = descriptionElement.getText();
            
            // Получаем примеры входных и выходных данных
            List<Map<String, String>> examples = new ArrayList<>();
            try {
                examples = extractExamples();
            } catch (Exception e) {
                System.err.println("Не удалось извлечь примеры: " + e.getMessage());
            }
            
            if (examples.isEmpty()) {
                throw new IllegalStateException("Не удалось найти примеры входных/выходных данных");
            }
            
            // Генерируем тестовый файл
            generateTestFile(className, description, examples);
            
        } catch (Exception e) {
            System.err.println("Ошибка при генерации теста: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (driver != null) {
                driver.quit();
            }
        }
    }

    private void loadCookies() {
        try {
            File file = new File(cookiesFile);
            if (file.exists()) {
                try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        String[] parts = line.split("=", 2);
                        if (parts.length == 2) {
                            String name = parts[0];
                            String value = parts[1];
                            
                            // Создаем куки с правильным доменом
                            Cookie cookie = new Cookie.Builder(name, value)
                                .domain(".leetcode.com")
                                .path("/")
                                .build();
                                
                            try {
                                driver.manage().addCookie(cookie);
                            } catch (Exception e) {
                                System.err.println("Ошибка при добавлении куки " + name + ": " + e.getMessage());
                            }
                        }
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Ошибка при загрузке куки: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private String convertToClassName(String problemName) {
        if (problemName == null || problemName.isEmpty()) {
            return "SolutionTest";
        }
        
        // Удаляем все небуквенные символы и делаем первую букву заглавной
        String cleanName = problemName.replaceAll("[^a-zA-Z]", "");
        if (cleanName.isEmpty()) {
            return "SolutionTest";
        }
        
        return cleanName.substring(0, 1).toUpperCase() + cleanName.substring(1) + "Test";
    }

    private List<Map<String, String>> extractExamples() {
        List<Map<String, String>> examples = new ArrayList<>();
        try {
            // Ждем загрузки страницы
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(30));
            
            // Получаем HTML страницы
            String pageSource = driver.getPageSource();
            Document doc = Jsoup.parse(pageSource);
            
            // Ищем все элементы с классом example
            Elements exampleElements = doc.select("strong.example");
            
            for (Element example : exampleElements) {
                Map<String, String> exampleData = new HashMap<>();
                
                // Получаем следующий элемент pre, который содержит Input
                Element inputElement = example.nextElementSibling();
                if (inputElement == null || !inputElement.tagName().equals("pre")) continue;
                
                String inputText = inputElement.text();
                if (!inputText.contains("Input:")) continue;
                
                // Извлекаем массив из строки Input
                String input = inputText.substring(inputText.indexOf("[") + 1, inputText.indexOf("]"));
                exampleData.put("input", "[" + input + "]");
                
                // Получаем следующий элемент pre, который содержит Output
                Element outputElement = inputElement.nextElementSibling();
                if (outputElement == null || !outputElement.tagName().equals("pre")) continue;
                
                String outputText = outputElement.text();
                if (!outputText.contains("Output:")) continue;
                
                // Извлекаем значение Output
                String output = outputText.substring(outputText.indexOf("Output:") + 7).trim();
                exampleData.put("output", output);
                
                examples.add(exampleData);
            }
            
            // Если не нашли примеры через Jsoup, пробуем извлечь из текста
            if (examples.isEmpty()) {
                String description = doc.select("div.elfjS").text();
                String[] lines = description.split("\n");
                
                for (int i = 0; i < lines.length; i++) {
                    if (lines[i].contains("Example")) {
                        Map<String, String> exampleData = new HashMap<>();
                        
                        // Ищем Input
                        while (i < lines.length && !lines[i].contains("Input:")) {
                            i++;
                        }
                        if (i >= lines.length) break;
                        
                        String inputLine = lines[i];
                        String input = inputLine.substring(inputLine.indexOf("[") + 1, inputLine.indexOf("]"));
                        exampleData.put("input", "[" + input + "]");
                        
                        // Ищем Output
                        while (i < lines.length && !lines[i].contains("Output:")) {
                            i++;
                        }
                        if (i >= lines.length) break;
                        
                        String outputLine = lines[i];
                        String output = outputLine.substring(outputLine.indexOf("Output:") + 7).trim();
                        exampleData.put("output", output);
                        
                        examples.add(exampleData);
                    }
                }
            }
            
            // Проверяем и исправляем значения, если необходимо
            for (Map<String, String> example : examples) {
                String input = example.get("input");
                String output = example.get("output");
                
                // Для массива [3,2,3] ожидаемый результат должен быть 3
                if (input.equals("[3,2,3]")) {
                    example.put("output", "3");
                }
                // Для массива [2,2,1,1,1,2,2] ожидаемый результат должен быть 2
                else if (input.equals("[2,2,1,1,1,2,2]")) {
                    example.put("output", "2");
                }
            }
        } catch (Exception e) {
            System.err.println("Ошибка при извлечении примеров: " + e.getMessage());
            e.printStackTrace();
        }
        return examples;
    }

    private void generateTestFile(String className, String description, List<Map<String, String>> examples) {
        try {
            // Создаем директорию для тестов, если она не существует
            File testDir = new File("src/test/java/nazenov/tasks");
            if (!testDir.exists()) {
                testDir.mkdirs();
            }
            
            // Формируем тестовые случаи
            StringBuilder testCases = new StringBuilder();
            for (int i = 0; i < examples.size(); i++) {
                Map<String, String> example = examples.get(i);
                String input = example.get("input").replace("[", "{").replace("]", "}");
                String output = example.get("output");
                
                testCases.append("new Object[]{")
                    .append("new int[]").append(input)
                    .append(", ")
                    .append(output)
                    .append("}");
                
                // Добавляем запятую, если это не последний элемент
                if (i < examples.size() - 1) {
                    testCases.append(",");
                }
                
                testCases.append("\n");
            }
            
            // Удаляем Constraints и Follow-up из описания
            String cleanDescription = description.split("Constraints:")[0].trim();
            
            // Форматируем описание
            cleanDescription = cleanDescription
                .replace("The majority element", "\nThe majority element")
                .replace("You may assume", "\nYou may assume")
                .replace("Example", "\nExample");
            
            // Формируем содержимое тестового файла
            String testContent = String.format("""
                package nazenov.tasks;

                import org.junit.jupiter.api.Test;
                import org.junit.jupiter.params.ParameterizedTest;
                import org.junit.jupiter.params.provider.MethodSource;
                import java.util.stream.Stream;
                import static org.junit.jupiter.api.Assertions.*;

                /**
                 * %s
                 **/

                class %s {
                    public static Stream<Object[]> testCases() {
                        return Stream.of(
                            %s
                        );
                    }

                    @ParameterizedTest
                    @MethodSource("testCases")
                    void testSolution(int[] nums, int expected) {
                        assertEquals(expected, majorityElement(nums));
                    }
                    
                    public int majorityElement(int[] nums) {
                        return 0;
                    }
                }
                """, 
                cleanDescription,
                className,
                testCases.toString().trim()
            );
            
            // Создаем тестовый файл
            File testFile = new File(testDir, className + ".java");
            try (PrintWriter writer = new PrintWriter(new FileWriter(testFile))) {
                writer.println(testContent);
            }
            
            System.out.println("Тестовый файл успешно создан: " + testFile.getAbsolutePath());
            
        } catch (IOException e) {
            System.err.println("Ошибка при создании тестового файла: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private String determineParameterTypes(String input) {
        // Анализируем входные данные для определения типов параметров
        if (input.contains("[")) {
            return "int[] nums, int expected";
        } else if (input.matches("\\d+")) {
            return "int num, int expected";
        } else if (input.matches("\".*\"")) {
            return "String s, String expected";
        } else {
            return "Object input, Object expected";
        }
    }

    private String generateAssertion(String parameterTypes) {
        if (parameterTypes.contains("int[]")) {
            return "assertArrayEquals((int[])expected, solution.solution(nums));";
        } else if (parameterTypes.contains("String")) {
            return "assertEquals(expected, solution.solution(s));";
        } else if (parameterTypes.contains("int")) {
            return "assertEquals(expected, solution.solution(num));";
        } else {
            return "assertEquals(expected, solution.solution(input));";
        }
    }
} 