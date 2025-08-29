/* USER: charlie TASK: bankacc */
import java.util.*;
import java.io.*;

public class charlie_solution {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        int N = scanner.nextInt();
        
        Map<Integer, Long> accounts = new HashMap<>();
        
        for (int i = 0; i < N; i++) {
            String transaction = scanner.next();
            
            if (transaction.equals("d")) {
                int account = scanner.nextInt();
                long amount = scanner.nextLong();
                accounts.put(account, accounts.getOrDefault(account, 0L) + amount);
                System.out.println("s");
            } else if (transaction.equals("w")) {
                int account = scanner.nextInt();
                long amount = scanner.nextLong();
                long currentBalance = accounts.getOrDefault(account, 0L);
                if (currentBalance >= amount) {
                    accounts.put(account, currentBalance - amount);
                    System.out.println("s");
                } else {
                    System.out.println("f");
                }
            } else if (transaction.equals("q")) {
                int account = scanner.nextInt();
                System.out.println(accounts.getOrDefault(account, 0L));
            }
        }
        
        scanner.close();
    }
}
