package scheduler;

import scheduler.db.ConnectionManager;
import scheduler.model.Caregiver;
import scheduler.model.Patient;
import scheduler.model.Vaccine;
import scheduler.util.Util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.*;
import java.util.Random;

public class Scheduler {

    // objects to keep track of the currently logged-in user
    // Note: it is always true that at most one of currentCaregiver and currentPatient is not null
    //       since only one user can be logged-in at a time
    private static Caregiver currentCaregiver = null;
    private static Patient currentPatient = null;


    public static void main(String[] args) {
        String operation="";
        //greeting texts
        System.out.println("Welcome to the COVID-19 Vaccine Reservation Scheduling Application!");
        System.out.println("*** Please enter one of the following commands ***");
        // read input from user
        BufferedReader r = new BufferedReader(new InputStreamReader(System.in));
        while (true) {
            greetings(); //print greetings
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
            operation = tokens[0];
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
                showAppointments();
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
    // greeting texts
    private static void greetings(){
        System.out.println("> create_patient <username> <password>");  //TODO: implement create_patient (Part 1)
        System.out.println("> create_caregiver <username> <password>");
        System.out.println("> login_patient <username> <password>");  // TODO: implement login_patient (Part 1)
        System.out.println("> login_caregiver <username> <password>");
        System.out.println("> search_caregiver_schedule <date>");  // TODO: implement search_caregiver_schedule (Part 2)
        System.out.println("> reserve <date> <vaccine>");  // TODO: implement reserve (Part 2)
        System.out.println("> upload_availability <date>");
        System.out.println("> cancel <appointment_id>");  // TODO: implement cancel (extra credit)
        System.out.println("> add_doses <vaccine> <number>");
        System.out.println("> show_appointments");  // TODO: implement show_appointments (Part 2)
        System.out.println("> logout");  // TODO: implement logout (Part 2)
        System.out.println("> quit");
        System.out.println();
    }

    private static void createPatient(String[] tokens) {
        // create_patient <username> <password>
        // check 1: the length for tokens need to be exactly 3 to include all information (with the operation name)
        if (tokens.length != 3) {
            System.out.println("Please try again!");
            return;
        }
        if(currentCaregiver!=null || currentPatient!=null){
            System.out.println("Already logged in");
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
            currentPatient = new Patient.PatientBuilder(username, salt, hash).build();
            // save to caregiver information to our database
            currentPatient.saveToDB();
            System.out.println(" *** Account created successfully *** ");
        } catch (SQLException e) {
            System.out.println("Create failed");
            e.printStackTrace();
        }
        //do we need to set currentPatient to null?
    }

    private static void createCaregiver(String[] tokens) {
        // create_caregiver <username> <password>
        // check 1: the length for tokens need to be exactly 3 to include all information (with the operation name)
        if (tokens.length != 3) {
            System.out.println("Please try again!");
            return;
        }
        String username = tokens[1];
        String password = tokens[2];
        // check 2: check if the username has been taken already
        if (usernameExistsCaregiver(username)) {
            System.out.println("Username taken, try again!");
            return;
        }
        if(currentCaregiver!=null || currentPatient!=null){
            System.out.println("Already logged in");
            return;
        }

        byte[] salt = Util.generateSalt();
        byte[] hash = Util.generateHash(password, salt);
        // create the caregiver
        try {
            currentCaregiver = new Caregiver.CaregiverBuilder(username, salt, hash).build();
            // save to caregiver information to our database
            currentCaregiver.saveToDB();
            System.out.println(" *** Account created successfully *** ");
        } catch (SQLException e) {
            System.out.println("Create failed");
            e.printStackTrace();
        }
        //do we need to set currentCaregiver to null?
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

    private static boolean usernameExistsCaregiver(String username) {
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        String selectUsername = "SELECT * FROM Caregivers WHERE Username = ?";
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

    private static boolean dateExists(Date d, Caregiver c){
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();
        String selectDate= "SELECT * FROM Appointments WHERE Time = ? AND CaregiverID = ?";
        try {
            PreparedStatement statement = con.prepareStatement(selectDate);
            statement.setDate(1, d);
            statement.setString(2, c.getUsername());
            ResultSet resultSet = statement.executeQuery();
            // returns false if the cursor is not before the first record or if there are no rows in the ResultSet.
            if(resultSet.isBeforeFirst()){
            return resultSet.isBeforeFirst();
            }
        } catch (SQLException e) {
            System.out.println("Error occurred when checking date in appointments");
            e.printStackTrace();
        }
        selectDate= "SELECT * FROM Availabilities WHERE Time = ? AND Username = ?";
        try {
            PreparedStatement statement = con.prepareStatement(selectDate);
            statement.setDate(1, d);
            statement.setString(2, c.getUsername());
            ResultSet resultSet = statement.executeQuery();
            // returns false if the cursor is not before the first record or if there are no rows in the ResultSet.
            return resultSet.isBeforeFirst();
        } catch (SQLException e) {
            System.out.println("Error occurred when checking date in availabilities");
            e.printStackTrace();
        }

        finally {
            cm.closeConnection();
        }
        return true;
    }

    private static boolean AppointmentIDExists(int ID) {
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();
        String selectID = "SELECT * FROM Appointments WHERE AppointmentID = ?";
        try {
            PreparedStatement statement = con.prepareStatement(selectID);
            statement.setInt(1, ID);
            ResultSet resultSet = statement.executeQuery();
            // returns false if the cursor is not before the first record or if there are no rows in the ResultSet.
            return resultSet.isBeforeFirst();
        } catch (SQLException e) {
            System.out.println("Error occurred when checking AppointmentID");
            e.printStackTrace();
        } finally {
            cm.closeConnection();
        }
        return true;
    }

    private static void loginPatient(String[] tokens) {
        // login_patient <username> <password>
        // check 1: if someone's already logged-in, they need to log out first
        if (currentPatient != null || currentCaregiver != null) {
            System.out.println("Already logged-in!");
            return;
        }
        // check 2: the length for tokens need to be exactly 3 to include all information (with the operation name)
        if (tokens.length != 3) {
            System.out.println("Please try again!");
            return;
        }
        String username = tokens[1];
        String password = tokens[2];

        Patient patient = null;
        try {
            patient = new Patient.PatientGetter(username, password).get();
        } catch (SQLException e) {
            System.out.println("Error occurred when logging in");
            e.printStackTrace();
        }
        // check if the login was successful
        if (patient == null) {
            System.out.println("Please try again!");
        } else {
            System.out.println("Patient logged in as: " + username);
            currentPatient = patient;
        }
    }

    private static void loginCaregiver(String[] tokens) {
        // login_caregiver <username> <password>
        // check 1: if someone's already logged-in, they need to log out first
        if (currentCaregiver != null || currentPatient != null) {
            System.out.println("Already logged-in!");
            return;
        }
        // check 2: the length for tokens need to be exactly 3 to include all information (with the operation name)
        if (tokens.length != 3) {
            System.out.println("Please try again!");
            return;
        }
        String username = tokens[1];
        String password = tokens[2];

        Caregiver caregiver = null;
        try {
            caregiver = new Caregiver.CaregiverGetter(username, password).get();
        } catch (SQLException e) {
            System.out.println("Error occurred when logging in");
            e.printStackTrace();
        }
        // check if the login was successful
        if (caregiver == null) {
            System.out.println("Please try again!");
        } else {
            System.out.println("Caregiver logged in as: " + username);
            currentCaregiver = caregiver;
        }
    }

    private static void searchCaregiverSchedule(String[] tokens) {
        // TODO: Part 2
        // check 1, make sure user has already logged in
        if(currentCaregiver == null & currentPatient==null){
            System.out.println("Please login first.");
            return;
        }
        // check 2, make sure size of token is correct
        if (tokens.length!=2) {
            System.out.println("Please try again!");
            return;
        }
        // check 3
        Date d = Date.valueOf("1111-11-11");
        try{
            String date = tokens[1];
            d = Date.valueOf(date);
        } catch(Exception i){
            System.out.println("Error occurred when processing date");
            return;
        }

        // open connection to database
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();
        // print out all available caregivers
        try {
            System.out.println("Available caregivers:");
            String selectUsername = "SELECT Username FROM Availabilities WHERE Time = ?";
            PreparedStatement statement = con.prepareStatement(selectUsername);
            statement.setDate(1, d);
            ResultSet resultSet = statement.executeQuery();
            ResultSetMetaData rsmd = resultSet.getMetaData();
            int columnsNumber = rsmd.getColumnCount();
            while (resultSet.next()) {
                for (int i = 1; i <= columnsNumber; i++) {
                    if (i > 1) System.out.print(",  ");
                    String columnValue = resultSet.getString(i);
                    System.out.print(columnValue);
                }
                System.out.println("");
            }
            System.out.println();
        } catch (SQLException e) {
            System.out.println("Error occurred when finding available caregivers");
            e.printStackTrace();
        } finally {
            cm.closeConnection();
        }

        con = cm.createConnection();
        // print out vaccines and doses left
        try {
            System.out.println("Available vaccines:");
            String getDoses = "SELECT * FROM Vaccines";
            PreparedStatement statement = con.prepareStatement(getDoses);
            ResultSet resultSet = statement.executeQuery();

            ResultSetMetaData rsmd = resultSet.getMetaData();
            int columnsNumber = rsmd.getColumnCount();
            while (resultSet.next()) {
                for (int i = 1; i <= columnsNumber; i++) {
                    if (i > 1) System.out.print(",  ");
                    String columnValue = resultSet.getString(i);
                    System.out.print(columnValue);
                }
                System.out.println("");
            }
            System.out.println();
        } catch (SQLException e) {
            System.out.println("Error occurred when finding available vaccines");
            e.printStackTrace();
        } finally {
            cm.closeConnection();
        }
    }

    private static void reserve(String[] tokens) {
        // TODO: Part 2
        // check 1, make sure patient is logged in
        if (currentPatient == null){
            System.out.println("Please login as a patient first!");
            return;
        }
        // check 2, make sure size of token is correct
        if (tokens.length!=3) {
            System.out.println("Please try again!");
            return;
        }

        Date d = Date.valueOf("1111-11-11");
        try{
            String date = tokens[1];
            d = Date.valueOf(date);
        }catch(IllegalArgumentException i){
            System.out.println("Error occurred when processing date");
            return;
        }

        // open connection to database
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();
        String caregiver="";
        //TODO make sure patient does not have a appointment already on this date

        try {
            String selectDate = "SELECT * FROM Appointments WHERE Time = ? AND PatientID = ?";
            PreparedStatement statement = con.prepareStatement(selectDate);
            statement.setDate(1, d);
            statement.setString(2,currentPatient.getUsername());
            ResultSet resultSet = statement.executeQuery();
            if (resultSet.isBeforeFirst()){
                System.out.println("You already have an appointment on this date");
                return;
            }
        } catch (SQLException e) {
            System.out.println("Error occurred when finding available caregivers");
            e.printStackTrace();
            return;
        } finally {
            cm.closeConnection();
        }

        con = cm.createConnection();
        // Assign caregiver to patient
        try {
            String selectUsername = "SELECT Username FROM Availabilities WHERE Time = ?";
            PreparedStatement statement = con.prepareStatement(selectUsername);
            statement.setDate(1, d);
            ResultSet resultSet = statement.executeQuery();
            // check to see if there are caregivers available on that date
            if(!resultSet.isBeforeFirst()){
                System.out.println("There are no available caregivers on this day. Pick a different date please.");
                return;
            }
            if(resultSet.next()){
            caregiver=resultSet.getString(1);
            }else{
                return;
            }
        } catch (SQLException e) {
            System.out.println("Error occurred when finding available caregivers");
            e.printStackTrace();
            return;
        } finally {
            cm.closeConnection();
        }

        String v = tokens[2];
        con = cm.createConnection();
        //check to see if vaccine still has doses
        try {
            String selectDoses = "SELECT Doses FROM Vaccines WHERE Name = ?";
            PreparedStatement statement = con.prepareStatement(selectDoses);
            statement.setString(1, v);
            ResultSet resultSet = statement.executeQuery();
            if(!resultSet.isBeforeFirst()){
                System.out.println("Vaccine does not exist");
                return;
            }
            if(resultSet.next()){
                if(resultSet.getInt(1)<=0){
                System.out.println("Vaccine is out of doses");
                return;
                }
            }
        } catch (SQLException e) {
            System.out.println("Error occurred when finding vaccine dose amount");
            e.printStackTrace();
            return;
        } finally {
            cm.closeConnection();
        }
        //decrease dose of vaccine accordingly
        con=cm.createConnection();
        try {
            Vaccine vac= new Vaccine.VaccineGetter(v).get();
            vac.decreaseAvailableDoses(1);
        }
        catch (SQLException e) {
            System.out.println("Error occurred when updating dosage amount for vaccine.");
            e.printStackTrace();
            return;
        }
        catch(NullPointerException n){
            System.out.println("Vaccine does not exist");
            n.printStackTrace();
            return;
        }

        //remove availability of caregiver accordingly
        String removeAvailability = "DELETE FROM Availabilities WHERE TIME = ? AND USERNAME = ?";
        try {
            PreparedStatement statement = con.prepareStatement(removeAvailability);
            statement.setDate(1, d);
            statement.setString(2, caregiver);
            statement.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Error occurred when updating availability for caregiver.");
            e.printStackTrace();
            return;
        }
        finally {
            cm.closeConnection();
        }

        // All checks passed, can now create new appointment
        con = cm.createConnection();
        String addAppointment = "INSERT INTO Appointments VALUES (? , ? , ? , ? , ?)";
        try {
            PreparedStatement statement = con.prepareStatement(addAppointment);
            int AppointmentID = 0;
            Random rand=new Random();
            do{
            AppointmentID = rand.nextInt(9999999);}
            while(AppointmentIDExists(AppointmentID));

            String patientID=currentPatient.getUsername();
            statement.setInt(1, AppointmentID);
            statement.setDate(2, d);
            statement.setString(3, patientID);
            statement.setString(4, caregiver);
            statement.setString(5, v);
            statement.executeUpdate();
            System.out.println("Reserved successfully");
        }
        catch (SQLException e) {
            System.out.println("Error occurred when updating Appointment");
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
        Date d = Date.valueOf("1111-11-11");
        try{
            String date = tokens[1];
            d = Date.valueOf(date);
        }catch(IllegalArgumentException i){
            System.out.println("Error occurred when processing date");
            return;
        }

        if(dateExists(d,currentCaregiver)) {
            System.out.println("Date has already been uploaded, please pick a different date");
            return;
        }
        try {
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
        // TODO: Extra credit

        //check 1, make sure length is correct
        if (tokens.length != 2) {
            System.out.println("Please try again!");
            return;
        }

        //check 2, make sure user is logged in
        String user ="";
        String removeAppointment = "";
        int AppointmentID = Integer.parseInt(tokens[1]);
        String ID="";

        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();
        String getPatientID ="SELECT PatientID FROM Appointments WHERE AppointmentID = ?";
        if (currentCaregiver != null & currentPatient==null){
            user=currentCaregiver.getUsername();
            removeAppointment = "DELETE FROM Appointments WHERE AppointmentID = ? AND CaregiverID = ?";
            try{
                PreparedStatement statement = con.prepareStatement(getPatientID);
                statement.setInt(1, AppointmentID);
                ResultSet resultSet = statement.executeQuery();
                if (resultSet.next()) {
                    ID = resultSet.getString(1);
                }
            }catch(SQLException e){
                System.out.println("Error occurred when retrieving vaccine.");
                e.printStackTrace();
            }finally{
                cm.closeConnection();
            }
        }
        else if(currentCaregiver == null & currentPatient!=null){
            user=currentPatient.getUsername();
            removeAppointment= "DELETE FROM Appointments WHERE AppointmentID = ? AND PatientID = ?";
            con = cm.createConnection();
            try{
                PreparedStatement statement = con.prepareStatement(getPatientID);
                statement.setInt(1, AppointmentID);
                ResultSet resultSet = statement.executeQuery();
                if (resultSet.next()) {
                    ID = resultSet.getString(1);
                }
            }catch(SQLException e){
                System.out.println("Error occurred when retrieving vaccine.");
                e.printStackTrace();
            }finally{
                cm.closeConnection();
            }
        }
        else{
            System.out.println("Please login first.");
            return;
        }
        // check 3, make sure user cannot cancel someone else's appointment
        if(!user.equals(ID)){
            System.out.println("You cannot cancel someone else's appointment");
            return;
        }

        cm = new ConnectionManager();
        con = cm.createConnection();
        String vac="";
        String date="";
        String caregiver="";

        try{
            String getVaccine ="SELECT Vaccine FROM Appointments WHERE AppointmentID = ?";
            String getCaregiver ="SELECT CaregiverID FROM Appointments WHERE AppointmentID = ?";
            String getDate ="SELECT Time FROM Appointments WHERE AppointmentID = ?";

            PreparedStatement statement = con.prepareStatement(getVaccine);
            statement.setInt(1, AppointmentID);
            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                vac = resultSet.getString(1);
            }

            statement = con.prepareStatement(getCaregiver);
            statement.setInt(1, AppointmentID);
            resultSet = statement.executeQuery();
            if (resultSet.next()) {
                caregiver = resultSet.getString(1);
            }

            statement = con.prepareStatement(getDate);
            statement.setInt(1, AppointmentID);
            resultSet = statement.executeQuery();
            if (resultSet.next()) {
                date = resultSet.getString(1);
            }

        } catch (SQLException e) {
            System.out.println("Error occurred when retrieving vaccine.");
            e.printStackTrace();
        }
        try {
            PreparedStatement statement = con.prepareStatement(removeAppointment);
            statement.setInt(1, AppointmentID);
            statement.setString(2, user);
            statement.executeUpdate();
            System.out.println("Successfully cancelled appointment");
        } catch (SQLException e) {
            System.out.println("Error occurred when updating appointment.");
            e.printStackTrace();
        }
        finally {
            cm.closeConnection();
        }
        //TODO need fix, not added back

        Vaccine vaccine = null;
        try {
            vaccine = new Vaccine.VaccineGetter(vac).get();
        } catch (SQLException e) {
            System.out.println("Error occurred when adding doses");
            e.printStackTrace();
        }
        try {
            vaccine.increaseAvailableDoses(1);
        } catch (SQLException e) {
            System.out.println("Error occurred when adding doses");
            e.printStackTrace();
        }

        String addAvailability = "INSERT INTO Availabilities VALUES (? , ?)";
        con = cm.createConnection();
        try {
            PreparedStatement statement = con.prepareStatement(addAvailability);
            statement.setString(1, date);
            statement.setString(2, caregiver);
            statement.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Error occurred when updating Availabilities.");
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

        //check to make sure doses is positive
        if(doses<0){
            System.out.println("Doses cannot be negative");
            return;
        }

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

    private static void showAppointments() {
        String user="";
        String selectStatement = "";
        // check 1, make sure user is logged in
        if (currentCaregiver != null & currentPatient==null){
            user=currentCaregiver.getUsername();
            selectStatement = "SELECT AppointmentID, Vaccine, Time, PatientID FROM Appointments WHERE CaregiverID = ?";
            System.out.println("AppointmentID  Vaccine  Date  PatientID");
        }
        else if(currentCaregiver == null & currentPatient!=null){
            user=currentPatient.getUsername();
            selectStatement = "SELECT AppointmentID, Vaccine, Time, CaregiverID FROM Appointments WHERE PatientID = ?";
            System.out.println("AppointmentID  Vaccine  Date  CaregiverID");
        }
        else{
            System.out.println("Please login first.");
            return;
        }
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();
        try {
            PreparedStatement statement = con.prepareStatement(selectStatement);
            statement.setString(1, user);
            ResultSet resultSet = statement.executeQuery();

            ResultSetMetaData rsmd = resultSet.getMetaData();
            int columnsNumber = rsmd.getColumnCount();
            while (resultSet.next()) {
                for (int i = 1; i <= columnsNumber; i++) {
                    if (i > 1) System.out.print(",  ");
                    String columnValue = resultSet.getString(i);
                    System.out.print(columnValue);
                }
                System.out.println("");
            }
            System.out.println();
        } catch (SQLException e) {
            System.out.println("Error occurred when retrieving data");
            e.printStackTrace();
        } finally {
            cm.closeConnection();
        }
    }

    private static void logout(String[] tokens) {
        // add login check
        if(currentCaregiver == null & currentPatient == null){
            System.out.println("Please login first");
            return;
        }

        // token check
        if(tokens.length!=1){
            System.out.println("Try again");
            return;
        }
        currentPatient=null;
        currentCaregiver=null;
        System.out.println("You have successfully logged out");
    }
}
