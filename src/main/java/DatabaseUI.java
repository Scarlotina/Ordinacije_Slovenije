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
    private JButton deleteButton;

    // load credentials from .env
    private final Dotenv dotenv     = Dotenv.load();
    private final String dbUrl      = dotenv.get("db_url");
    private final String dbUsername = dotenv.get("db_username");
    private final String dbPassword = dotenv.get("db_password");

    public DatabaseUI() {
        panel1 = new JPanel(new BorderLayout());

        // --- tabs (unused for now) ---
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
        showLogButton = new JButton("Show Log");
        deleteButton  = new JButton("Delete Selected Ordinacija");
        btnBar.add(updateButton);
        btnBar.add(editButton);
        btnBar.add(showLogButton);
        btnBar.add(deleteButton);
        panel1.add(btnBar, BorderLayout.SOUTH);

        // --- wire actions ---
        updateButton .addActionListener(e -> loadData());
        editButton   .addActionListener(e -> editOrdinacija());
        showLogButton.addActionListener(e -> showLogDialog());
        deleteButton .addActionListener(e -> deleteOrdinacija());

        // initial load
        loadData();
    }

    /** Load the main ordinacija data into table1 **/
    private void loadData() {
        model.setRowCount(0);
        model.setColumnCount(0);

        String sql = "SELECT * FROM get_ordinacija_data('')";
        try (
                Connection c = DriverManager.getConnection(dbUrl, dbUsername, dbPassword);
                PreparedStatement p = c.prepareStatement(sql);
                ResultSet rs = p.executeQuery()
        ) {
            ResultSetMetaData md = rs.getMetaData();
            int cols = md.getColumnCount();
            for (int i = 1; i <= cols; i++) {
                model.addColumn(md.getColumnName(i));
            }
            while (rs.next()) {
                Object[] row = new Object[cols];
                for (int i = 1; i <= cols; i++) {
                    row[i-1] = rs.getObject(i);
                }
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
        try (
                Connection c = DriverManager.getConnection(dbUrl, dbUsername, dbPassword);
                PreparedStatement p = c.prepareStatement(sql);
                ResultSet rs = p.executeQuery()
        ) {
            ResultSetMetaData md = rs.getMetaData();
            int cols = md.getColumnCount();
            for (int i = 1; i <= cols; i++) {
                logModel.addColumn(md.getColumnName(i));
            }
            while (rs.next()) {
                Object[] row = new Object[cols];
                for (int i = 1; i <= cols; i++) {
                    row[i-1] = rs.getObject(i);
                }
                logModel.addRow(row);
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(panel1,
                    "Error loading change log:\n" + ex.getMessage(),
                    "Log Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        JScrollPane scroll = new JScrollPane(logTable);
        scroll.setPreferredSize(new Dimension(700, 300));
        JOptionPane.showMessageDialog(
                panel1, scroll,
                "Ordinacija Change Log",
                JOptionPane.PLAIN_MESSAGE
        );
    }

    /** Prompt & call delete_ordinacija(p_id) **/
    private void deleteOrdinacija() {
        int sel = table1.getSelectedRow();
        if (sel < 0) {
            JOptionPane.showMessageDialog(panel1,
                    "Please select an Ordinacija to delete!",
                    "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String name = (String)model.getValueAt(sel, 0);
        int id = getOrdinacijaId(name);
        if (id < 0) {
            JOptionPane.showMessageDialog(panel1,
                    "Couldn’t find ID for: " + name,
                    "Lookup Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(panel1,
                "Really delete \"" + name + "\" (id=" + id + ")?",
                "Confirm Delete", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;

        String sql = "SELECT delete_ordinacija(?)";
        try (
                Connection c = DriverManager.getConnection(dbUrl, dbUsername, dbPassword);
                PreparedStatement p = c.prepareStatement(sql)
        ) {
            p.setInt(1, id);
            p.executeQuery();
            loadData();
        } catch (SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(panel1,
                    "Error deleting ordinacija:\n" + ex.getMessage(),
                    "Delete Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /** helper: find ordinacija.id by name **/
    private int getOrdinacijaId(String name) {
        String q = "SELECT id FROM ordinacija WHERE ime = ?";
        try (
                Connection c = DriverManager.getConnection(dbUrl, dbUsername, dbPassword);
                PreparedStatement p = c.prepareStatement(q)
        ) {
            p.setString(1, name);
            try (ResultSet rs = p.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return -1;
    }

    /** Prompt the user to edit the selected row, then call update_ordinacija_data(...) **/
    private void editOrdinacija() {
        int sel = table1.getSelectedRow();
        if (sel < 0) {
            JOptionPane.showMessageDialog(panel1,
                    "Please select an Ordinacija to edit!",
                    "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String origName     = (String)model.getValueAt(sel, 0);
        JTextField nameF    = new JTextField(origName);
        JTextField contactF = new JTextField((String)model.getValueAt(sel, 1));
        JTextField hoursF   = new JTextField((String)model.getValueAt(sel, 2));
        JTextField locF     = new JTextField((String)model.getValueAt(sel, 3));
        JTextField specF    = new JTextField((String)model.getValueAt(sel, 4));

        JPanel editPanel = new JPanel(new GridLayout(6,2));
        editPanel.add(new JLabel("Ordinacija Name:")); editPanel.add(nameF);
        editPanel.add(new JLabel("Contact:"));          editPanel.add(contactF);
        editPanel.add(new JLabel("Working Hours:"));    editPanel.add(hoursF);
        editPanel.add(new JLabel("Location:"));         editPanel.add(locF);
        editPanel.add(new JLabel("Specialization:"));   editPanel.add(specF);
        JButton confirm = new JButton("Update");
        editPanel.add(new JLabel()); // filler
        editPanel.add(confirm);

        JFrame f = new JFrame("Edit Ordinacija");
        f.setContentPane(editPanel);
        f.pack();
        f.setLocationRelativeTo(panel1);
        f.setVisible(true);

        confirm.addActionListener(e -> {
            updateData(
                    origName,
                    nameF.getText(),
                    contactF.getText(),
                    hoursF.getText(),
                    locF.getText(),
                    specF.getText()
            );
            f.dispose();
        });
    }

    /** Calls your update_ordinacija_data(...) PL/pgSQL function **/
    private void updateData(
            String ordinacijaName,
            String newIme,
            String newContact,
            String newWorkingHours,
            String newLocation,
            String newSpecialization
    ) {
        int locId  = getLocationId(newLocation);
        int specId = getSpecializationId(newSpecialization);
        if (locId < 0 || specId < 0) {
            JOptionPane.showMessageDialog(panel1,
                    "Invalid Location or Specialization!",
                    "Validation Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String sql = "SELECT update_ordinacija_data(?, ?, ?, ?, ?, ?)";
        try (
                Connection c = DriverManager.getConnection(dbUrl, dbUsername, dbPassword);
                PreparedStatement p = c.prepareStatement(sql)
        ) {
            p.setString(1, ordinacijaName);
            p.setString(2, newIme);
            p.setString(3, newContact);
            p.setString(4, newWorkingHours);
            p.setInt(5, locId);
            p.setInt(6, specId);
            p.executeQuery();
            loadData();
        } catch (SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(panel1,
                    "Error updating data:\n" + ex.getMessage(),
                    "Update Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private int getLocationId(String location) {
        String q = "SELECT get_location_id(?)";
        try (
                Connection c = DriverManager.getConnection(dbUrl, dbUsername, dbPassword);
                PreparedStatement p = c.prepareStatement(q)
        ) {
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
        try (
                Connection c = DriverManager.getConnection(dbUrl, dbUsername, dbPassword);
                PreparedStatement p = c.prepareStatement(q)
        ) {
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
