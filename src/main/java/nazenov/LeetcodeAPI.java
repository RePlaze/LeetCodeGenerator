package nazenov;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import org.json.JSONObject;

public class LeetcodeAPI {

    private static final String API_URL = "https://leetcode.com/problems/majority-element/?envType=study-plan-v2&envId=top-interview-150";

    public static JSONObject getProblemData(String slug) {
        try {
            String query = "{ \"query\": \"query questionDetail($slug: String!) { question(titleSlug: $slug) { title content exampleTestcases { input output } } }\", \"variables\": {\"slug\": \"" + slug + "\"} }";
            URL url = new URL(API_URL);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setDoOutput(true);
            connection.getOutputStream().write(query.getBytes());

            BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String inputLine;
            StringBuilder response = new StringBuilder();
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();

            JSONObject jsonResponse = new JSONObject(response.toString());
            return jsonResponse.getJSONObject("data").getJSONObject("question");
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void main(String[] args) {
        // Получаем данные задачи по slug
        String slug = "majority-element";
        JSONObject problemData = getProblemData(slug);

        if (problemData != null) {
            System.out.println("Заголовок: " + problemData.getString("title"));
            System.out.println("Описание: " + problemData.getString("content"));

            // Выводим примеры входных данных и ожидаемых результатов
            problemData.getJSONArray("exampleTestcases").forEach(item -> {
                JSONObject testCase = (JSONObject) item;
                System.out.println("Input: " + testCase.getString("input"));
                System.out.println("Output: " + testCase.getString("output"));
            });
        }
    }
}

