import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

/**
 * Beginner-friendly Student Record Management System.
 * Java Swing is the user interface; Oracle PL/SQL handles add/update/delete.
 */
public class StudentManagementApp extends JFrame {
    // Change these values to match your Oracle installation.
    private static final String DB_URL = "jdbc:oracle:thin:@//localhost:1521/XEPDB1";
    private static final String DB_USER = "SYSTEM";
    private static final String DB_PASSWORD = "123456";

    private final JTextField idField = new JTextField();
    private final JTextField nameField = new JTextField();
    private final JTextField courseField = new JTextField();
    private final JTextField emailField = new JTextField();

    private final DefaultTableModel tableModel = new DefaultTableModel(
            new String[]{"ID", "Student Name", "Course", "Email"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable studentTable = new JTable(tableModel);

    public StudentManagementApp() {
        setTitle("Student Record Management System");
        setSize(850, 520);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel form = new JPanel(new GridLayout(4, 2, 8, 8));
        form.setBorder(BorderFactory.createTitledBorder("Student Details"));
        form.add(new JLabel("Student ID (for search/update/delete):"));
        form.add(idField);
        form.add(new JLabel("Student Name:"));
        form.add(nameField);
        form.add(new JLabel("Course:"));
        form.add(courseField);
        form.add(new JLabel("Email:"));
        form.add(emailField);

        JButton addButton = new JButton("Add Student");
        JButton updateButton = new JButton("Update Student");
        JButton deleteButton = new JButton("Delete Student");
        JButton searchButton = new JButton("Search by ID");
        JButton showAllButton = new JButton("Show All");
        JButton clearButton = new JButton("Clear");

        JPanel buttons = new JPanel();
        buttons.add(addButton);
        buttons.add(updateButton);
        buttons.add(deleteButton);
        buttons.add(searchButton);
        buttons.add(showAllButton);
        buttons.add(clearButton);

        add(form, BorderLayout.NORTH);
        add(new JScrollPane(studentTable), BorderLayout.CENTER);
        add(buttons, BorderLayout.SOUTH);

        addButton.addActionListener(e -> addStudent());
        updateButton.addActionListener(e -> updateStudent());
        deleteButton.addActionListener(e -> deleteStudent());
        searchButton.addActionListener(e -> searchStudent());
        showAllButton.addActionListener(e -> loadStudents());
        clearButton.addActionListener(e -> clearFields());

        studentTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && studentTable.getSelectedRow() >= 0) {
                int row = studentTable.getSelectedRow();
                idField.setText(String.valueOf(tableModel.getValueAt(row, 0)));
                nameField.setText(String.valueOf(tableModel.getValueAt(row, 1)));
                courseField.setText(String.valueOf(tableModel.getValueAt(row, 2)));
                emailField.setText(String.valueOf(tableModel.getValueAt(row, 3)));
            }
        });

        loadStudents();
    }

    private Connection connect() throws SQLException {
        return DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
    }

    private boolean validateDetails(boolean requireId) {
        if (requireId && idField.getText().trim().isEmpty()) {
            showMessage("Please enter a Student ID.");
            return false;
        }
        if (nameField.getText().trim().isEmpty() || courseField.getText().trim().isEmpty()) {
            showMessage("Student name and course are required.");
            return false;
        }
        return true;
    }

    private void addStudent() {
        if (!validateDetails(false)) return;
        String sql = "{call add_student(?,?,?)}";
        try (Connection con = connect(); CallableStatement cs = con.prepareCall(sql)) {
            cs.setString(1, nameField.getText().trim());
            cs.setString(2, courseField.getText().trim());
            cs.setString(3, emailField.getText().trim());
            cs.execute();
            showMessage("Student added successfully.");
            loadStudents();
            clearFields();
        } catch (SQLException ex) {
            showDatabaseError(ex);
        }
    }

    private void updateStudent() {
        if (!validateDetails(true)) return;
        try {
            long id = Long.parseLong(idField.getText().trim());
            String sql = "{call update_student(?,?,?,?)}";
            try (Connection con = connect(); CallableStatement cs = con.prepareCall(sql)) {
                cs.setLong(1, id);
                cs.setString(2, nameField.getText().trim());
                cs.setString(3, courseField.getText().trim());
                cs.setString(4, emailField.getText().trim());
                cs.execute();
            }
            showMessage("Student updated successfully.");
            loadStudents();
            clearFields();
        } catch (NumberFormatException ex) {
            showMessage("Student ID must be a number.");
        } catch (SQLException ex) {
            showDatabaseError(ex);
        }
    }

    private void deleteStudent() {
        if (idField.getText().trim().isEmpty()) {
            showMessage("Enter the Student ID to delete.");
            return;
        }
        int answer = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to delete this student?",
                "Confirm Delete", JOptionPane.YES_NO_OPTION);
        if (answer != JOptionPane.YES_OPTION) return;

        try {
            long id = Long.parseLong(idField.getText().trim());
            try (Connection con = connect();
                 CallableStatement cs = con.prepareCall("{call delete_student(?)}")) {
                cs.setLong(1, id);
                cs.execute();
            }
            showMessage("Student deleted successfully.");
            loadStudents();
            clearFields();
        } catch (NumberFormatException ex) {
            showMessage("Student ID must be a number.");
        } catch (SQLException ex) {
            showDatabaseError(ex);
        }
    }

    private void searchStudent() {
        if (idField.getText().trim().isEmpty()) {
            showMessage("Enter the Student ID to search.");
            return;
        }
        try {
            long id = Long.parseLong(idField.getText().trim());
            try (Connection con = connect();
                 PreparedStatement ps = con.prepareStatement(
                         "SELECT student_id, student_name, course, email FROM students WHERE student_id = ?")) {
                ps.setLong(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    tableModel.setRowCount(0);
                    if (rs.next()) {
                        tableModel.addRow(new Object[]{
                                rs.getLong("student_id"), rs.getString("student_name"),
                                rs.getString("course"), rs.getString("email")
                        });
                    } else {
                        showMessage("No student found with ID " + id);
                    }
                }
            }
        } catch (NumberFormatException ex) {
            showMessage("Student ID must be a number.");
        } catch (SQLException ex) {
            showDatabaseError(ex);
        }
    }

    private void loadStudents() {
        tableModel.setRowCount(0);
        String sql = "SELECT student_id, student_name, course, email FROM students ORDER BY student_id";
        try (Connection con = connect();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                tableModel.addRow(new Object[]{
                        rs.getLong("student_id"), rs.getString("student_name"),
                        rs.getString("course"), rs.getString("email")
                });
            }
        } catch (SQLException ex) {
            showDatabaseError(ex);
        }
    }

    private void clearFields() {
        idField.setText("");
        nameField.setText("");
        courseField.setText("");
        emailField.setText("");
        studentTable.clearSelection();
    }

    private void showMessage(String message) {
        JOptionPane.showMessageDialog(this, message);
    }

    private void showDatabaseError(SQLException ex) {
        JOptionPane.showMessageDialog(this,
                "Database operation failed.\nCheck Oracle is running, the JDBC driver is added,\n"
                + "and DB_URL / DB_USER / DB_PASSWORD are correct.\n\nDetails: " + ex.getMessage(),
                "Database Error", JOptionPane.ERROR_MESSAGE);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new StudentManagementApp().setVisible(true));
    }
}
