import java.sql.*;
import java.util.Scanner;

public class SimpleBankApp {

    // Database credentials - ensuring consistency with our previous setup
    static final String DB_URL = "jdbc:mysql://localhost:3306/simple_bank";
    static final String USER = "root"; 
    static final String PASS = "";     

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        // Load the driver explicitly to ensure terminal compatibility
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            System.out.println("❌ MySQL JDBC Driver not found. Ensure the connector JAR is in your classpath.");
            return;
        }

        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS)) {
            System.out.println("\n==================================");
            System.out.println("✅ SYSTEM: Database Connected");
            System.out.println("==================================");

            System.out.print("Enter your Account Number (e.g., 12345): ");
            String accountNumber = scanner.nextLine();

            if (!isAccountValid(conn, accountNumber)) {
                System.out.println("❌ Account not found. Exiting system...");
                return;
            }

            boolean exit = false;
            while (!exit) {
                System.out.println("\n=== TERMINAL BANKING MENU ===");
                System.out.println("[1] Check Balance");
                System.out.println("[2] Deposit");
                System.out.println("[3] Withdraw");
                System.out.println("[4] Exit");
                System.out.print("COMMAND> ");
                
                // Read input safely
                if (!scanner.hasNextInt()) {
                    System.out.println("❌ Invalid input. Please enter a number.");
                    scanner.next(); // Clear the bad input
                    continue;
                }
                
                int choice = scanner.nextInt();

                switch (choice) {
                    case 1:
                        checkBalance(conn, accountNumber);
                        break;
                    case 2:
                        System.out.print("Enter amount to deposit: PHP ");
                        double depAmount = scanner.nextDouble();
                        deposit(conn, accountNumber, depAmount);
                        break;
                    case 3:
                        System.out.print("Enter amount to withdraw: PHP ");
                        double withAmount = scanner.nextDouble();
                        withdraw(conn, accountNumber, withAmount);
                        break;
                    case 4:
                        exit = true;
                        System.out.println("Logging out... Thank you for using Terminal Bank!");
                        break;
                    default:
                        System.out.println("❌ Unknown command. Please enter a number between 1 and 4.");
                }
            }
        } catch (SQLException e) {
            System.out.println("❌ Database error: " + e.getMessage());
            System.out.println("Make sure MySQL is running and the database 'simple_bank' exists.");
        } finally {
            scanner.close();
        }
    }

    private static boolean isAccountValid(Connection conn, String accountNumber) throws SQLException {
        String query = "SELECT id FROM accounts WHERE account_number = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, accountNumber);
            ResultSet rs = pstmt.executeQuery();
            return rs.next();
        }
    }

    private static void checkBalance(Connection conn, String accountNumber) throws SQLException {
        String query = "SELECT account_name, balance FROM accounts WHERE account_number = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, accountNumber);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                System.out.println("\n----------------------------------");
                System.out.println("💳 Account Name : " + rs.getString("account_name"));
                System.out.println("💰 Balance      : PHP " + String.format("%.2f", rs.getDouble("balance")));
                System.out.println("----------------------------------");
            }
        }
    }

    private static void deposit(Connection conn, String accountNumber, double amount) throws SQLException {
        if (amount <= 0) {
            System.out.println("❌ Deposit amount must be greater than zero.");
            return;
        }
        String query = "UPDATE accounts SET balance = balance + ? WHERE account_number = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setDouble(1, amount);
            pstmt.setString(2, accountNumber);
            int rowsAffected = pstmt.executeUpdate();
            if (rowsAffected > 0) {
                System.out.println("✅ Deposit of PHP " + amount + " successful!");
                checkBalance(conn, accountNumber);
            }
        }
    }

    private static void withdraw(Connection conn, String accountNumber, double amount) throws SQLException {
        if (amount <= 0) {
            System.out.println("❌ Withdrawal amount must be greater than zero.");
            return;
        }

        String checkQuery = "SELECT balance FROM accounts WHERE account_number = ?";
        double currentBalance = 0;
        try (PreparedStatement pstmtCheck = conn.prepareStatement(checkQuery)) {
            pstmtCheck.setString(1, accountNumber);
            ResultSet rs = pstmtCheck.executeQuery();
            if (rs.next()) {
                currentBalance = rs.getDouble("balance");
            }
        }

        if (amount > currentBalance) {
            System.out.println("❌ Insufficient funds! Current balance: PHP " + String.format("%.2f", currentBalance));
            return;
        }

        String updateQuery = "UPDATE accounts SET balance = balance - ? WHERE account_number = ?";
        try (PreparedStatement pstmtUpdate = conn.prepareStatement(updateQuery)) {
            pstmtUpdate.setDouble(1, amount);
            pstmtUpdate.setString(2, accountNumber);
            int rowsAffected = pstmtUpdate.executeUpdate();
            if (rowsAffected > 0) {
                System.out.println("✅ Withdrawal of PHP " + amount + " successful!");
                checkBalance(conn, accountNumber);
            }
        }
    }
}
