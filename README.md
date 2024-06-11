# vaccine-schedueler
Project for Introduction to Database Systems in the Paul G. Allen School of Computer Science & Engineering.
## Commands
### create_patient <username> <password>
Creates a patient object that takes in fields of username and password.
### create_caregiver <username> <password>
Creates a caregiver object that takes in fields of username and password.
### login_patient <username> <password>
Logs in user being a patient with valid username and password information.
### login_caregiver <username> <password>
Logs in user being a caregiver with valid username and password information.
### search_caregiver_schedule <date>
Both patients and caregivers can perform this operation. Outputs the username for the caregivers that are available for the date ordered alphabetically by the username of the caregiver. Then, output the vaccine name and number of available doses for that vaccine separated by a space.
### reserve <date> <vaccine>
Only patients perform this operation to reserve an appointment. If reservation was successfully made, the caregiver is no longer available for the date selected. If there are available caregivers, it chooses the caregiver by alphabetical order and print “Appointment ID {appointment_id}, Caregiver username {username}”. If no caregiver is available, print “No caregiver is available” and return. If not enough vaccine doses are available, print "Not enough available doses" and return. If no user is logged in, print “Please login first” and return. If the current user logged in is not a patient, print “Please login as a patient” and return. For all other errors, print "Please try again".
### upload_availability <date>
Only caregivers have authorization to perform this operation. User inputs a date where they are available.
### cancel <appointment_id>
Both patients and caregivers and perform this operation deleting an appointment reservation by providing the appointment_id.
### add_doses <vaccine> <number>
Caregivers only have authorization to perform this operation. User can update the number of available doses for a specific vaccine.
### show_appointments
Output the scheduled appointments for the current user. For caregivers, it prints the appointment ID, vaccine name, date, and patient name ordered by the appointment ID. For patients, you should print the appointment ID, vaccine name, date, and caregiver name ordered by the appointment ID. If no user is logged in, it prints “Please login first”. For all other errors, it prints "Please try again".
### logout
Logs out current user. If not logged in, system prints “Please login first”. Otherwise, systems prints “Successfully logged out”. For all other errors, system prints "Please try again".
### quit
