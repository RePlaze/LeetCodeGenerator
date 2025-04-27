package nazenov;
import nazenov.LeetCodeGen.LeetCodeTestGenerator;
public class Generate {
     static String url =
             "https://leetcode.com/problems/rotate-array/description/?envType=study-plan-v2&envId=top" +
            "-interview-150";
    public static void main(String[] args) {
        new LeetCodeTestGenerator().generateTest(url);
    }
}
