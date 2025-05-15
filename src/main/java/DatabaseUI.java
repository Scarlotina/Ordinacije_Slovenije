import io.github.cdimascio.dotenv.Dotenv;
import javax.swing.*;
import java.awt.*;
import java.sql.*;
import javax.swing.table.DefaultTableModel;

public class DatabaseUI {
    private JPanel panel1;
    private JTabbedPane tabbedpane1;
    private JTable table1;
    private DefaultTableModel model;
    private JButton updateButton;
    private JButton editButton;
    private JButton showLogButton;

    // your connection info
    Dotenv dotenv = Dotenv.load();
    String dbUrl = dotenv.get("db_url");
    String dbUsername = dotenv.get("db_username");
    String dbPassword = dotenv.get("db_password");
    public DatabaseUI() {


        panel1 = new JPanel(new BorderLayout());

        // --- tabs ---
        tabbedpane1 = new JTabbedPane();
        tabbedpane1.add("Tab 1", new JPanel());
        tabbedpane1.add("Tab 2", new JPanel());
        panel1.add(tabbedpane1, BorderLayout.CENTER);

        // --- main table ---
        model  = new DefaultTableModel();
        table1 = new JTable(model);
        panel1.add(new JScrollPane(table1), BorderLayout.CENTER);

        // --- buttons at bottom ---
        JPanel btnBar = new JPanel();
        updateButton  = new JButton("Update Data");
        editButton    = new JButton("Edit Selected Ordinacija");
        showLogButton = new JButton("Show log");
        btnBar.add(updateButton);
        btnBar.add(editButton);
        btnBar.add(showLogButton);
        panel1.add(btnBar, BorderLayout.SOUTH);

        // --- wire actions ---
        updateButton .addActionListener(e -> {
            // placeholder: replace with your real updateData() call
            updateData("exampleName","exampleIme","exampleContact","exampleHours","exampleLocation","exampleSpecialization");
        });
        editButton   .addActionListener(e -> editOrdinacija());
        showLogButton.addActionListener(e -> showLogDialog());

        // initial load
        loadData();
    }

    /** Load the main ordinacija data into table1 **/
    private void loadData() {
        model.setRowCount(0);
        model.setColumnCount(0);
        String sql = "SELECT * FROM get_ordinacija_data('')";
        try (Connection c = DriverManager.getConnection(dbUrl, dbUsername, dbPassword);
             PreparedStatement p = c.prepareStatement(sql);
             ResultSet rs = p.executeQuery())
        {
            ResultSetMetaData md = rs.getMetaData();
            int cols = md.getColumnCount();
            for (int i = 1; i <= cols; i++) {
                model.addColumn(md.getColumnName(i));
            }
            while (rs.next()) {
                Object[] row = new Object[cols];
                for (int i = 1; i <= cols; i++) row[i-1] = rs.getObject(i);
                model.addRow(row);
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(panel1,
                    "Error loading data:\n" + ex.getMessage(),
                    "Load Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /** Pop-up a scrollable table showing EVERY row from ordinacija_log **/
    private void showLogDialog() {
        DefaultTableModel logModel = new DefaultTableModel();
        JTable logTable = new JTable(logModel);

        String sql = "SELECT * FROM ordinacija_log ORDER BY log_id";
        try (Connection c = DriverManager.getConnection(dbUrl, dbUsername, dbPassword);
             PreparedStatement p = c.prepareStatement(sql);
             ResultSet rs = p.executeQuery())
        {
            ResultSetMetaData md = rs.getMetaData();
            int cols = md.getColumnCount();
            // add each column name to the model
            for (int i = 1; i <= cols; i++) {
                logModel.addColumn(md.getColumnName(i));
            }
            // add every row
            while (rs.next()) {
                Object[] row = new Object[cols];
                for (int i = 1; i <= cols; i++) row[i-1] = rs.getObject(i);
                logModel.addRow(row);
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(panel1,
                    "Error loading change log:\n" + ex.getMessage(),
                    "Log Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // show it in a scroll pane
        JScrollPane scroll = new JScrollPane(logTable);
        scroll.setPreferredSize(new Dimension(700, 300));
        JOptionPane.showMessageDialog(
                panel1,
                scroll,
                "Ordinacija Change Log",
                JOptionPane.PLAIN_MESSAGE
        );
    }

    /** Your existing edit-dialog + updateData() wiring **/
    private void editOrdinacija() {
        int selectedRow = table1.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(panel1,
                    "Please select an Ordinacija to edit!",
                    "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String ordinacijaName = (String)model.getValueAt(selectedRow, 0);
        JTextField imeField   = new JTextField((String)model.getValueAt(selectedRow, 0));
        JTextField contactField = new JTextField((String)model.getValueAt(selectedRow, 1));
        JTextField hoursField   = new JTextField((String)model.getValueAt(selectedRow, 2));
        JTextField locField     = new JTextField((String)model.getValueAt(selectedRow, 3));
        JTextField specField    = new JTextField((String)model.getValueAt(selectedRow, 4));

        JPanel editPanel = new JPanel(new GridLayout(6,2));
        editPanel.add(new JLabel("Ordinacija Name:")); editPanel.add(imeField);
        editPanel.add(new JLabel("Contact:"));           editPanel.add(contactField);
        editPanel.add(new JLabel("Working Hours:"));     editPanel.add(hoursField);
        editPanel.add(new JLabel("Location:"));          editPanel.add(locField);
        editPanel.add(new JLabel("Specialization:"));    editPanel.add(specField);
        JButton confirm = new JButton("Update");
        editPanel.add(new JLabel()); // spacer
        editPanel.add(confirm);

        JFrame f = new JFrame("Edit Ordinacija");
        f.setContentPane(editPanel);
        f.pack();
        f.setLocationRelativeTo(panel1);
        f.setVisible(true);

        confirm.addActionListener(e -> {
            updateData(
                    ordinacijaName,
                    imeField.getText(),
                    contactField.getText(),
                    hoursField.getText(),
                    locField.getText(),
                    specField.getText()
            );
            f.dispose();
        });
    }

    /** Calls your update_ordinacija_data(...) function **/
    private void updateData(String ordinacijaName,
                            String newIme,
                            String newContact,
                            String newWorkingHours,
                            String newLocation,
                            String newSpecialization)
    {
        int locationId       = getLocationId(newLocation);
        int specializationId = getSpecializationId(newSpecialization);
        if (locationId < 0 || specializationId < 0) {
            JOptionPane.showMessageDialog(panel1,
                    "Invalid Location or Specialization!",
                    "Validation Error",
                    JOptionPane.ERROR_MESSAGE
            );
            return;
        }

        String sql = "SELECT update_ordinacija_data(?, ?, ?, ?, ?, ?)";
        try (Connection c = DriverManager.getConnection(dbUrl, dbUsername, dbPassword);
             PreparedStatement p = c.prepareStatement(sql))
        {
            p.setString(1, ordinacijaName);
            p.setString(2, newIme);
            p.setString(3, newContact);
            p.setString(4, newWorkingHours);
            p.setInt   (5, locationId);
            p.setInt   (6, specializationId);
            p.executeQuery();
            loadData();
        } catch (SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(panel1,
                    "Error updating data:\n" + ex.getMessage(),
                    "Update Error",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }

    private int getLocationId(String location) {
        String q = "SELECT get_location_id(?)";
        try (Connection c = DriverManager.getConnection(dbUrl, dbUsername, dbPassword);
             PreparedStatement p = c.prepareStatement(q))
        {
            p.setString(1, location);
            try (ResultSet rs = p.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return -1;
    }

    private int getSpecializationId(String specialization) {
        String q = "SELECT get_specialization_id(?)";
        try (Connection c = DriverManager.getConnection(dbUrl, dbUsername, dbPassword);
             PreparedStatement p = c.prepareStatement(q))
        {
            p.setString(1, specialization);
            try (ResultSet rs = p.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return -1;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Database UI Example");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(900, 600);
            frame.setContentPane(new DatabaseUI().panel1);
            frame.setVisible(true);
        });
    }
}