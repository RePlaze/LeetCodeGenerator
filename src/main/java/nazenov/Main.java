package nazenov;

public class Main {
    public static void main(String[] args) {
        String problemUrl = "https://leetcode.com/problems/majority-element/?envType=study-plan-v2&envId=top-interview-150";
        LeetCodeTestGenerator generator = new LeetCodeTestGenerator();
        generator.generateTest(problemUrl);
    }
}

//        LeetCodeAuth auth = new LeetCodeAuth();
//
//        System.out.println("Начинаем процесс авторизации...");
//
//        if (auth.isLoggedIn()) {
//            System.out.println("Успешный вход с использованием сохраненных куки!");
//        } else {
//            // Если куки нет или они недействительны, выполняем вход
//            System.out.println("Выполняем вход...");
//            if (auth.login()) {
//                System.out.println("Куки успешно сохранены!");
//            } else {
//                System.out.println("Ошибка при сохранении куки.");
//            }
//        }
