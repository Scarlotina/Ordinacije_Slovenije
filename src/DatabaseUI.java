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

    public DatabaseUI() {
        panel1 = new JPanel();
        panel1.setLayout(new BorderLayout());

        tabbedpane1 = new JTabbedPane();
        tabbedpane1.add("Tab 1", new JPanel());
        tabbedpane1.add("Tab 2", new JPanel());
        panel1.add(tabbedpane1, BorderLayout.CENTER);

        model = new DefaultTableModel();
        table1 = new JTable(model);
        panel1.add(new JScrollPane(table1), BorderLayout.CENTER);

        updateButton = new JButton("Update Data");
        panel1.add(updateButton, BorderLayout.SOUTH);
        editButton = new JButton("Edit Selected Ordinacija");
        panel1.add(editButton, BorderLayout.SOUTH);

        updateButton.addActionListener(e -> {
            String ordinacijaName = "exampleName";  // Replace with actual data
            String newIme = "exampleIme";  // Replace with actual data
            String newContact = "exampleContact";  // Replace with actual data
            String newWorkingHours = "exampleHours";  // Replace with actual data
            String newLocation = "exampleLocation";  // Replace with actual data
            String newSpecialization = "exampleSpecialization";  // Replace with actual data

            updateData(ordinacijaName, newIme, newContact, newWorkingHours, newLocation, newSpecialization);
        });
        editButton.addActionListener(e -> editOrdinacija());

        loadData();
    }
    String url = "jdbc:postgresql://ep-little-mouse-a96ghw1s-pooler.gwc.azure.neon.tech:5432/neondb?sslmode=require";
    String username = "neondb_owner"; // Your username
    String password = "npg_DJ2q5CahBrcm"; // Your password

    private void loadData() {
        // 1) Wipe out everything from the model
        model.setRowCount(0);
        model.setColumnCount(0);

        String query = "SELECT * FROM get_ordinacija_data('')";

        try (Connection connection = DriverManager.getConnection(url, username, password);
             PreparedStatement stmt = connection.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {

            ResultSetMetaData meta = rs.getMetaData();
            int cols = meta.getColumnCount();

            // 2) Add columns
            for (int i = 1; i <= cols; i++) {
                model.addColumn(meta.getColumnName(i));
            }

            // 3) Add rows
            while (rs.next()) {
                Object[] row = new Object[cols];
                for (int i = 1; i <= cols; i++) {
                    row[i - 1] = rs.getObject(i);
                }
                model.addRow(row);
            }

        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "Error loading data: " + e.getMessage());
        }
    }


    private void updateData(String ordinacijaName, String newIme, String newContact, String newWorkingHours, String newLocation, String newSpecialization) {
        // Get location and specialization IDs
        int locationId = getLocationId(newLocation);
        int specializationId = getSpecializationId(newSpecialization);

        // Check if either ID is -1 (not found)
        if (locationId == -1 || specializationId == -1) {
            JOptionPane.showMessageDialog(null, "Invalid Location or Specialization!");
            return; // Stop the update if either is invalid
        }

        String query = "SELECT update_ordinacija_data(?, ?, ?, ?, ?, ?)"; // This line is fine

        try (Connection connection = DriverManager.getConnection(url, username, password);
             PreparedStatement stmt = connection.prepareStatement(query)) {

            // Pass all 6 parameters
            stmt.setString(1, ordinacijaName);  // Ordinacija name
            stmt.setString(2, newIme);  // New name
            stmt.setString(3, newContact);  // New contact
            stmt.setString(4, newWorkingHours);  // New working hours
            stmt.setInt(5, locationId);  // Location ID
            stmt.setInt(6, specializationId);  // Specialization ID

            // Use executeQuery instead of execute since it's a function
            stmt.executeQuery(); // Executes the function (as opposed to a stored procedure)

            // Reload the data after update
            loadData();
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "Error updating data: " + e.getMessage());
        }
    }


    private void editOrdinacija() {
        int selectedRow = table1.getSelectedRow();
        if (selectedRow != -1) { // Check if a row is selected
            String ordinacijaName = (String) model.getValueAt(selectedRow, 0);  // Get the ordinacija name

            // Create a new frame for editing
            JFrame editFrame = new JFrame("Edit Ordinacija");
            editFrame.setSize(400, 300);

            // Create fields to edit, populate them with the current values from the selected row
            JTextField imeField = new JTextField((String) model.getValueAt(selectedRow, 0));
            JTextField contactField = new JTextField((String) model.getValueAt(selectedRow, 1));
            JTextField workingHoursField = new JTextField((String) model.getValueAt(selectedRow, 2));
            JTextField locationField = new JTextField((String) model.getValueAt(selectedRow, 3));
            JTextField specializationField = new JTextField((String) model.getValueAt(selectedRow, 4));

            // Add the fields to the frame
            JPanel editPanel = new JPanel();
            editPanel.setLayout(new GridLayout(6, 2));
            editPanel.add(new JLabel("Ordinacija Name:"));
            editPanel.add(imeField);
            editPanel.add(new JLabel("Contact:"));
            editPanel.add(contactField);
            editPanel.add(new JLabel("Working Hours:"));
            editPanel.add(workingHoursField);
            editPanel.add(new JLabel("Location:"));
            editPanel.add(locationField);
            editPanel.add(new JLabel("Specialization:"));
            editPanel.add(specializationField);

            // Add a confirm button
            JButton confirmButton = new JButton("Update");
            editPanel.add(confirmButton);

            // When confirm button is clicked, update the data
            confirmButton.addActionListener(e -> {
                // Call updateData method to update the data in the database
                updateData(ordinacijaName, imeField.getText(), contactField.getText(), workingHoursField.getText(), locationField.getText(), specializationField.getText());
                editFrame.dispose();  // Close the edit window
            });

            // Add the panel to the frame
            editFrame.setContentPane(editPanel);
            editFrame.setVisible(true);
        } else {
            JOptionPane.showMessageDialog(null, "Please select an Ordinacija to edit!");
        }
    }



    private int getLocationId(String location) {
        String query = "SELECT get_location_id(?)";  // Call the function for location ID
        try (Connection connection = DriverManager.getConnection(url, username, password);
             PreparedStatement stmt = connection.prepareStatement(query)) {

            stmt.setString(1, location);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);  // Return the ID
            } else {
                // If no location found, return -1 and handle the error in the updateData method
                return -1;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;  // In case of SQL error
    }


    private int getSpecializationId(String specialization) {
        String query = "SELECT get_specialization_id(?)";  // Call the function for specialization ID
        try (Connection connection = DriverManager.getConnection(url, username, password);
             PreparedStatement stmt = connection.prepareStatement(query)) {

            stmt.setString(1, specialization);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);  // Return the ID
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame("Database UI Example");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(800, 600);

        DatabaseUI databaseUI = new DatabaseUI();
        frame.setContentPane(databaseUI.panel1);

        frame.setVisible(true);
    }
}
