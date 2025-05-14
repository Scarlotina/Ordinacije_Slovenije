import javax.swing.*;
import java.awt.*;

public class DatabaseUI {
    private JPanel panel1;
    private JTabbedPane tabbedpane1; // JTabbedPane declaration
    private JTextField searchField;
    private JTable table1;
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

        // Set up the search field
        searchField = new JTextField(20);
        panel1.add(searchField, BorderLayout.NORTH);  // Add search field to the north section of the panel
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
