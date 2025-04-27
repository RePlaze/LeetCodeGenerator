package nazenov;

import nazenov.LeetCodeGen.LeetCodeAuth;

public class GetCookie {
    public static void main(String[] args) {
        LeetCodeAuth auth = new LeetCodeAuth();
        System.out.println("Начинаем процесс авторизации...");

        if (auth.isLoggedIn())
            System.out.println("Успешный вход с использованием сохраненных куки!");
        else if (auth.login())
            System.out.println("Куки успешно сохранены!");
        else
            System.out.println("Ошибка при сохранении куки.");
    }
}
