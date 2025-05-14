import javax.swing.*;
import java.awt.*;
import java.sql.*;  // Add this import to include all necessary JDBC classes
import javax.swing.table.DefaultTableModel;

public class DatabaseUI {
    private JPanel panel1;
    private JTabbedPane tabbedpane1; // JTabbedPane declaration
    private JTable table1;
    private DefaultTableModel model;  // Declare the model for JTable

    public DatabaseUI() {
        // Set up the main panel and its layout
        panel1 = new JPanel();
        panel1.setLayout(new BorderLayout());  // Set layout to BorderLayout

        // Create and set up JTabbedPane
        tabbedpane1 = new JTabbedPane();
        tabbedpane1.add("Tab 1", new JPanel());  // Add first tab
        tabbedpane1.add("Tab 2", new JPanel());  // Add second tab

        // Add the tabbed pane to the center of the panel
        panel1.add(tabbedpane1, BorderLayout.CENTER);

        // Set up the table model and JTable
        model = new DefaultTableModel();
        table1 = new JTable(model);  // Initialize table with model
        panel1.add(new JScrollPane(table1), BorderLayout.CENTER);  // Add table to the center of the panel

        // Load data into the table when the UI is created
        loadData();
    }

    private void loadData() {
        String url = "jdbc:postgresql://ep-little-mouse-a96ghw1s-pooler.gwc.azure.neon.tech/neondb?sslmode=require";
        String username = "neondb_owner";
        String password = "npg_DJ2q5CahBrcm";

        // SQL query to call the stored procedure for fetching data
        String query = "SELECT * FROM get_ordinacija_data('')";  // Call the stored procedure with empty search (fetch all)

        try (Connection connection = DriverManager.getConnection(url, username, password);
             PreparedStatement stmt = connection.prepareStatement(query)) {

            ResultSet rs = stmt.executeQuery();

            // Get column names from ResultSet and add them to the table model
            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();

            // Clear existing columns if any
            model.setColumnCount(0);

            // Add column names
            for (int i = 1; i <= columnCount; i++) {
                model.addColumn(metaData.getColumnName(i));
            }

            // Add rows from ResultSet
            while (rs.next()) {
                Object[] row = new Object[columnCount];
                for (int i = 1; i <= columnCount; i++) {
                    row[i - 1] = rs.getObject(i);
                }
                model.addRow(row);
            }

        } catch (SQLException e) {
            e.printStackTrace();  // Print any SQL exceptions
        }
    }

    public static void main(String[] args) {
        // Create the main frame
        JFrame frame = new JFrame("Database UI Example");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(800, 600);

        // Create the DatabaseUI instance and add the panel
        DatabaseUI databaseUI = new DatabaseUI();
        frame.setContentPane(databaseUI.panel1);  // Set the content panel

        frame.setVisible(true);
    }
}
