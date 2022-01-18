CREATE TABLE Caregivers (
    Username varchar(255),
    Salt BINARY(16),
    Hash BINARY(16),
    PRIMARY KEY (Username)
);

CREATE TABLE Availabilities (
    Time date,
    Username varchar(255) REFERENCES Caregivers,
    PRIMARY KEY (Time, Username)
);

CREATE TABLE Vaccines (
    Name varchar(255),
    Doses int,
    PRIMARY KEY (Name)
);

CREATE TABLE Patients (
     Username varchar(255),
     Salt BINARY(16),
     Hash BINARY(16),
     PRIMARY KEY (Username)
);

CREATE TABLE Appointments (
    AppointmentID int PRIMARY KEY,
    Time date,
    PatientID varchar(255) FOREIGN KEY REFERENCES Patients(Username),
    CaregiverID varchar(255) FOREIGN KEY REFERENCES Caregivers(Username),
    Vaccine varchar(255) FOREIGN KEY REFERENCES Vaccines(Name)
);