package scheduler;

import scheduler.db.ConnectionManager;
import scheduler.model.Caregiver;
import scheduler.model.Patient;
import scheduler.model.Vaccine;
import scheduler.util.Util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Date;

public class Scheduler {

    // objects to keep track of the currently logged-in user
    // Note: it is always true that at most one of currentCaregiver and currentPatient is not null
    //       since only one user can be logged-in at a time
    private static Caregiver currentCaregiver = null;
    private static Patient currentPatient = null;

    public static void main(String[] args) {
        // printing greetings text
        System.out.println();
        System.out.println("Welcome to the COVID-19 Vaccine Reservation Scheduling Application!");
        System.out.println("*** Please enter one of the following commands ***");
        System.out.println("> create_patient <username> <password>");
        System.out.println("> create_caregiver <username> <password>");
        System.out.println("> login_patient <username> <password>");
        System.out.println("> login_caregiver <username> <password>");
        System.out.println("> search_caregiver_schedule <date>");
        System.out.println("> reserve <date> <vaccine>");
        System.out.println("> upload_availability <date>");
        System.out.println("> cancel <appointment_id>");
        System.out.println("> add_doses <vaccine> <number>");
        System.out.println("> show_appointments");
        System.out.println("> logout");
        System.out.println("> quit");
        System.out.println();

        // read input from user
        BufferedReader r = new BufferedReader(new InputStreamReader(System.in));
        while (true) {
            System.out.print("> ");
            String response = "";
            try {
                response = r.readLine();
            } catch (IOException e) {
                System.out.println("Please try again!");
            }
            // split the user input by spaces
            String[] tokens = response.split(" ");
            // check if input exists
            if (tokens.length == 0) {
                System.out.println("Please try again!");
                continue;
            }
            // determine which operation to perform
            String operation = tokens[0];
            if (operation.equals("create_patient")) {
                createPatient(tokens);
            } else if (operation.equals("create_caregiver")) {
                createCaregiver(tokens);
            } else if (operation.equals("login_patient")) {
                loginPatient(tokens);
            } else if (operation.equals("login_caregiver")) {
                loginCaregiver(tokens);
            } else if (operation.equals("search_caregiver_schedule")) {
                searchCaregiverSchedule(tokens);
            } else if (operation.equals("reserve")) {
                reserve(tokens);
            } else if (operation.equals("upload_availability")) {
                uploadAvailability(tokens);
            } else if (operation.equals("cancel")) {
                cancel(tokens);
            } else if (operation.equals("add_doses")) {
                addDoses(tokens);
            } else if (operation.equals("show_appointments")) {
                showAppointments(tokens);
            } else if (operation.equals("logout")) {
                logout(tokens);
            } else if (operation.equals("quit")) {
                System.out.println("Bye!");
                return;
            } else {
                System.out.println("Invalid operation name!");
            }
        }
    }

    private static void createPatient(String[] tokens) {
        // create_patient <username> <password>
        // check 1: the length for tokens need to be exactly 3 to include all information (with the operation name)
        if (tokens.length != 3) {
            System.out.println("Create patient failed.");
            return;
        }
        String username = tokens[1];
        String password = tokens[2];
        // check 2: check if the username has been taken already
        if (usernameExistsPatient(username)) {
            System.out.println("Username taken, try again!");
            return;
        }
        byte[] salt = Util.generateSalt();
        byte[] hash = Util.generateHash(password, salt);
        // create the patient
        try {
            Patient patient = new Patient.PatientBuilder(username, salt, hash).build();
            // save to patient information to our database
            patient.saveToDB();
            System.out.println("Created user " + username);
        } catch (SQLException e) {
            System.out.println("Create patient failed.");
            e.printStackTrace();
        }
    }

    private static void createCaregiver(String[] tokens) {
        // create_patient <username> <password>
        // check 1: the length for tokens need to be exactly 3 to include all information (with the operation name)
        if (tokens.length != 3) {
            System.out.println("Failed to create user.");
            return;
        }
        String username = tokens[1];
        String password = tokens[2];
        // check 2: check if the username has been taken already
        if (usernameExistsPatient(username)) {
            System.out.println("Username taken, try again!");
            return;
        }
        byte[] salt = Util.generateSalt();
        byte[] hash = Util.generateHash(password, salt);
        // create the caregiver
        try {
            Caregiver caregiver = new Caregiver.CaregiverBuilder(username, salt, hash).build();
            // save to caregiver information to our database
            caregiver.saveToDB();
            System.out.println("Created user " + username);
        } catch (SQLException e) {
            System.out.println("Failed to create user.");
            e.printStackTrace();
        }
    }

    private static boolean usernameExistsPatient(String username) {
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        String selectUsername = "SELECT * FROM Patients WHERE Username = ?";
        try {
            PreparedStatement statement = con.prepareStatement(selectUsername);
            statement.setString(1, username);
            ResultSet resultSet = statement.executeQuery();
            // returns false if the cursor is not before the first record or if there are no rows in the ResultSet.
            return resultSet.isBeforeFirst();
        } catch (SQLException e) {
            System.out.println("Error occurred when checking username");
            e.printStackTrace();
        } finally {
            cm.closeConnection();
        }
        return true;
    }

    private static void loginPatient(String[] tokens) {
        // login_caregiver <username> <password>
        // check 1: if someone's already logged-in, they need to log out first
        if (currentPatient != null || currentCaregiver != null) {
            System.out.println("User already logged in, try again.");
            return;
        }
        // check 2: the length for tokens need to be exactly 3 to include all information (with the operation name)
        if (tokens.length != 3) {
            System.out.println("Login patient failed.");
            return;
        }
        String username = tokens[1];
        String password = tokens[2];

        Patient patient = null;
        try {
            patient = new Patient.PatientGetter(username, password).get();
        } catch (SQLException e) {
            System.out.println("Login failed.");
            e.printStackTrace();
        }
        // check if the login was successful
        if (patient == null) {
            System.out.println("Login failed.");
        } else {
            System.out.println("Logged in as " + username);
            currentPatient = patient;
        }
    }

    private static void loginCaregiver(String[] tokens) {
        // login_caregiver <username> <password>
        // check 1: if someone's already logged-in, they need to log out first
        if (currentCaregiver != null || currentPatient != null) {
            System.out.println("User already logged in.");
            return;
        }
        // check 2: the length for tokens need to be exactly 3 to include all information (with the operation name)
        if (tokens.length != 3) {
            System.out.println("Login failed.");
            return;
        }
        String username = tokens[1];
        String password = tokens[2];

        Caregiver caregiver = null;
        try {
            caregiver = new Caregiver.CaregiverGetter(username, password).get();
        } catch (SQLException e) {
            System.out.println("Login failed.");
            e.printStackTrace();
        }
        // check if the login was successful
        if (caregiver == null) {
            System.out.println("Login failed.");
        } else {
            System.out.println("Logged in as: " + username);
            currentCaregiver = caregiver;
        }
    }

    private static void searchCaregiverSchedule(String[] tokens) {
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();
        String selectAvailableCaregivers = "SELECT Username FROM Availabilities WHERE Time = ? ORDER BY Username ASC;";
        String selectVaccines = "SELECT * FROM Vaccines;";

        // search_caregiver_schedule <date>
        // check 1: check if a caregiver or patient is logged in
        if (currentCaregiver == null && currentPatient == null) {
            System.out.println("Please login first!");
            return;
        }
        // check 2: the length for tokens need to be exactly 2 to include all information (with the operation name)
        if (tokens.length != 2) {
            System.out.println("Please try again!");
            return;
        }
        String date = tokens[1];
        try {
            // print all available caregivers on given date
            PreparedStatement caregiversStatement = con.prepareStatement(selectAvailableCaregivers);
            caregiversStatement.setString(1, date);
            ResultSet caregiversResultSet = caregiversStatement.executeQuery();
            while (caregiversResultSet.next())
            {
                System.out.println(caregiversResultSet.getString(1)); //or rs.getString("column name");
            }

            // print name of all available vaccines and their amount of doses
            PreparedStatement vaccinesStatement = con.prepareStatement(selectVaccines);
            ResultSet vaccinesResultSet = vaccinesStatement.executeQuery();
            while (vaccinesResultSet.next())
            {
                System.out.println(vaccinesResultSet.getString("Name") + " " + vaccinesResultSet.getString("Doses")); //or rs.getString("column name");
            }

        } catch (SQLException e) {
            System.out.println("Error occurred when checking username");
            e.printStackTrace();
        } finally {
            cm.closeConnection();
        }
    }

    private static void reserve(String[] tokens) { // [reserve, <date>, <vaccine>]
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();
        String selectCaregiver = "SELECT TOP 1 Username FROM Availabilities WHERE Time = ? ORDER BY Username ASC;";
        String removeCaregiver = "DELETE FROM Availabilities WHERE Time = ? AND Username = ?;";
        String updateVaccine = "UPDATE Vaccines SET Doses = Doses - 1 WHERE Name = ?";
        String insertAppointment = "INSERT INTO Appointments (Time , Caregiver, Patient, Vaccine_Name) VALUES (?, ?, ?, ?);";
        String selectAppointmentID = "SELECT appointment_id FROM Appointments WHERE Time = ? AND Caregiver = ? AND Patient = ? AND Vaccine_Name = ?;";
        // reserve <date> <vaccine>
        // check 1: check if the current logged-in user is a patient
        if (currentPatient == null) {
            System.out.println("Please login as a patient first!");
            return;
        }
        // check 2: the length for tokens need to be exactly 3 to include all information (with the operation name)
        if (tokens.length != 3) {
            System.out.println("Please try again!");
            return;
        }
        String date = tokens[1];
        String vaccine = tokens[2];
        try {
            // obtain the first available caregiver at the given date
            String caregiver = null;
            PreparedStatement selectCaregiverStatement = con.prepareStatement(selectCaregiver);
            selectCaregiverStatement.setString(1, date);
            ResultSet caregiversResultSet = selectCaregiverStatement.executeQuery();
            while (caregiversResultSet.next())
            {
                caregiver = caregiversResultSet.getString(1);
            }

            // remove the obtained caregiver from the Availability table
            PreparedStatement removeCaregiverStatement = con.prepareStatement(removeCaregiver);
            removeCaregiverStatement.setString(1, date);
            removeCaregiverStatement.setString(2, caregiver);
            removeCaregiverStatement.executeUpdate();

            // decrement the amount of doses for the chosen vaccine
            PreparedStatement updateVaccineStatement = con.prepareStatement(updateVaccine);
            updateVaccineStatement.setString(1, vaccine);
            updateVaccineStatement.executeUpdate();

            // inserting appointment with the date, caregiver, patient, and vaccine
            PreparedStatement insertAppointmentStatement = con.prepareStatement(insertAppointment);
            insertAppointmentStatement.setString(1, date);
            insertAppointmentStatement.setString(2, caregiver);
            insertAppointmentStatement.setString(3, currentPatient.getUsername());
            insertAppointmentStatement.setString(4, vaccine);
            insertAppointmentStatement.executeUpdate();

            // selecting the appointment_id
            PreparedStatement selectAppointmentIDStatement = con.prepareStatement(selectAppointmentID);
            selectAppointmentIDStatement.setString(1, date);
            selectAppointmentIDStatement.setString(2, caregiver);
            selectAppointmentIDStatement.setString(3, currentPatient.getUsername());
            selectAppointmentIDStatement.setString(4, vaccine);
            ResultSet selectAppointmentIDResultSet = selectAppointmentIDStatement.executeQuery();
            String appointmentID = null;
            while (selectAppointmentIDResultSet.next())
            {
                appointmentID = selectAppointmentIDResultSet.getString("appointment_id");
            }

            // printing out string to console for user
            System.out.println("Appointment ID " + appointmentID + ", Caregiver username " + caregiver);
        } catch (SQLException e) {
            System.out.println("Error occurred when checking username");
            e.printStackTrace();
        } finally {
            cm.closeConnection();
        }

    }

    private static void uploadAvailability(String[] tokens) {
        // upload_availability <date>
        // check 1: check if the current logged-in user is a caregiver
        if (currentCaregiver == null) {
            System.out.println("Please login as a caregiver first!");
            return;
        }
        // check 2: the length for tokens need to be exactly 2 to include all information (with the operation name)
        if (tokens.length != 2) {
            System.out.println("Please try again!");
            return;
        }
        String date = tokens[1];
        try {
            Date d = Date.valueOf(date);
            currentCaregiver.uploadAvailability(d);
            System.out.println("Availability uploaded!");
        } catch (IllegalArgumentException e) {
            System.out.println("Please enter a valid date!");
        } catch (SQLException e) {
            System.out.println("Error occurred when uploading availability");
            e.printStackTrace();
        }
    }

    private static void cancel(String[] tokens) {
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();
        String selectAppointment = "SELECT * FROM Appointments WHERE appointment_id = ?;";
        String deleteAppointment = "DELETE FROM Appointments WHERE appointment_id = ?;";
        String addAvailability = "INSERT INTO Availabilities VALUES (? , ?)";
        String addVaccineDose = "UPDATE Vaccines SET Doses = Doses + 1 WHERE Name = ?;";

        // check 1: if currentCaregiver and currentPatient are both null meaning there is no user logged in,
        // as the user to login first.
        if (currentCaregiver == null && currentPatient == null) {
            System.out.println("Please login first.");
            return;
        }
        // check 2: the length for tokens need to be exactly 2 to include all information (with the operation name)
        if (tokens.length != 2) {
            System.out.println("Please try again!");
            return;
        }
        String appointmentID = tokens[1];
        try {
            Date time = null;
            String caregiver = null;
            String vaccineName = null;

            PreparedStatement selectAppointmentStatement = con.prepareStatement(selectAppointment);
            selectAppointmentStatement.setString(1, appointmentID);
            ResultSet selectAppointmentsResultSet = selectAppointmentStatement.executeQuery();
            while (selectAppointmentsResultSet.next())
            {
                time = selectAppointmentsResultSet.getDate("Time");
                caregiver = selectAppointmentsResultSet.getString("Caregiver");
                vaccineName = selectAppointmentsResultSet.getString("Vaccine_Name");
            }

            PreparedStatement deleteAppointmentStatement = con.prepareStatement(deleteAppointment);
            deleteAppointmentStatement.setString(1, appointmentID);
            deleteAppointmentStatement.executeUpdate();

            PreparedStatement addAvailabilityStatement = con.prepareStatement(addAvailability);
            addAvailabilityStatement.setDate(1, time);
            addAvailabilityStatement.setString(2, caregiver);
            addAvailabilityStatement.executeUpdate();

            PreparedStatement addVaccineDoseStatement = con.prepareStatement(addVaccineDose);
            addVaccineDoseStatement.setString(1, vaccineName);
            addVaccineDoseStatement.executeUpdate();

            System.out.println("Appointment successfully deleted.");

        } catch (SQLException e) {
            System.out.println("Error occurred when checking username");
            e.printStackTrace();
        } finally {
            cm.closeConnection();
        }
    }

    private static void addDoses(String[] tokens) {
        // add_doses <vaccine> <number>
        // check 1: check if the current logged-in user is a caregiver
        if (currentCaregiver == null) {
            System.out.println("Please login as a caregiver first!");
            return;
        }
        // check 2: the length for tokens need to be exactly 3 to include all information (with the operation name)
        if (tokens.length != 3) {
            System.out.println("Please try again!");
            return;
        }
        String vaccineName = tokens[1];
        int doses = Integer.parseInt(tokens[2]);
        Vaccine vaccine = null;
        try {
            vaccine = new Vaccine.VaccineGetter(vaccineName).get();
        } catch (SQLException e) {
            System.out.println("Error occurred when adding doses");
            e.printStackTrace();
        }
        // check 3: if getter returns null, it means that we need to create the vaccine and insert it into the Vaccines
        //          table
        if (vaccine == null) {
            try {
                vaccine = new Vaccine.VaccineBuilder(vaccineName, doses).build();
                vaccine.saveToDB();
            } catch (SQLException e) {
                System.out.println("Error occurred when adding doses");
                e.printStackTrace();
            }
        } else {
            // if the vaccine is not null, meaning that the vaccine already exists in our table
            try {
                vaccine.increaseAvailableDoses(doses);
            } catch (SQLException e) {
                System.out.println("Error occurred when adding doses");
                e.printStackTrace();
            }
        }
        System.out.println("Doses updated!");
    }

    private static void showAppointments(String[] tokens) {
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();
        String selectAppointments = "SELECT * FROM Appointments ORDER BY appointment_id";
        // check 1: if currentCaregiver and currentPatient are both null meaning there is no user logged in,
        // as the user to login first.
        if (currentCaregiver == null && currentPatient == null) {
            System.out.println("Please login first.");
            return;
        }
        // check 2: the length for tokens need to be exactly 3 to include all information (with the operation name)
        if (tokens.length != 1) {
            System.out.println("Please try again!");
            return;
        }
        try {
            String appointmentID = null;
            String time = null;
            String caregiver = null;
            String patient = null;
            String vaccineName = null;

            // selecting all appointments and printing information based on circumstances
            PreparedStatement selectAppointmentsStatement = con.prepareStatement(selectAppointments);
            ResultSet selectAppointmentsResultSet = selectAppointmentsStatement.executeQuery();
            while (selectAppointmentsResultSet.next())
            {
                appointmentID = selectAppointmentsResultSet.getString("appointment_id");
                time = selectAppointmentsResultSet.getString("Time");
                caregiver = selectAppointmentsResultSet.getString("Caregiver");
                patient = selectAppointmentsResultSet.getString("Patient");
                vaccineName = selectAppointmentsResultSet.getString("Vaccine_Name");
                if (currentCaregiver != null) {
                    if (currentCaregiver.getUsername().equals(caregiver)) {
                        System.out.println(appointmentID + " " + vaccineName + " " + time + " " + patient);
                    }
                } else {
                    if (currentPatient.getUsername().equals(patient)) {
                        System.out.println(appointmentID + " " + vaccineName + " " + time + " " + caregiver);
                    }
                }
            }
        } catch (SQLException e) {
            System.out.println("Error occurred when checking username");
            e.printStackTrace();
        } finally {
            cm.closeConnection();
        }
    }

    private static void logout(String[] tokens) {
        // check 1: if currentCaregiver and currentPatient are both null meaning there is no user logged in,
        // as the user to login first.
        if (currentCaregiver == null && currentPatient == null) {
            System.out.println("Please login first.");
            return;
        }
        // check 2: the length for tokens need to be exactly 3 to include all information (with the operation name)
        if (tokens.length != 1) {
            System.out.println("Please try again!");
            return;
        }
        try {
            // setting to null to get rid of any login
            currentPatient = null;
            currentCaregiver = null;
            System.out.println("Successfully logged out");
        } catch (Exception e) {
            System.out.println("Please try again.");
            e.printStackTrace();
        }
    }
}
