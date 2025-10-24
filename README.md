# CS-360-10404

Summary of App Requirements and Goals

The Weight Tracker app is designed for tracking the weight of individuals in order to help them meet their health objectives on a daily basis. The main objective of developing the weight tracker app is to make weight tracking an easy, continuous, and motivational process for its users with the help of its mobile interface.

Screens And Features Supporting User Needs
Subtopic: Displays

The app has two main screens:

Login/Registration Screen – Enables users to safely create an account using stored credentials in an SQLite database.

Dashboard Screen – Shows a grid view of the daily weight entries, input fields for entering new values, goal weight function, and an SMS alert feature.

The UI’s design emphasizes simplicity. By considering easy-to-read layout designs, easy-to-use input fields, and optimized paths for navigation, users can conveniently monitor their progress without any complications in the UI’s design. Similarly, color schemes, spacing between components, and font sizes also play an equally significant role in it.

Coding Approach and Techniques

I started programming with the integration of basic functions and then implemented other functionalities such as CRUD operations, SMS permissions, and goal tracking on top of it. I also used a modular approach with a separate DBHelper class for database-related operations and a WeightAdapter class for handling the RecyclerView.

During the development phase, I relied on incremental testing using the Android Emulator to ensure each feature functioned properly before continuing with the next feature. As discussed, in any app development project, a modular structure, build, and test strategy can be adopted for a seamless operation with reduced bugs during the execution phase.

Testing and Debugging
Software

Android Emulator for testing

I tested the app using the Android Emulator with each CRUD operation (Create, Read, Update, Delete) to verify they were functioning correctly with the two possible outcomes for SMS permissions (Allow/Deny). I also created breakpoints in the Debugger tool in Android Studio, specifically in the DBHelper class, for tracking database entries.

This process was essential for ensuring functionality, stability, and persistence of user data. Testing revealed where data handling needed to be more robust, leading to refinements in database queries and UI refreshes.

Overcoming Challenges and Innovations

There were a number of challenges in making the SMS notification system function properly in the app while ensuring it functioned to its maximum potential even if permission for it is not provided. I tackled such a challenge by implementing a feature involving conditional permissions whereby the app continues to run even if it doesn’t have SMS permissions.

Demonstrated Knowledge and Success
The piece that I am most proud of in terms of showcasing my skill level is the integration with the SQLite database. This is because it successfully enables a binding between the user input with a structural database that then dynamically updates the grid view in the RecyclerView. As a whole, the project covers an entire cycle associated with user-centric mobile app development, from idea and design to coding, testing, and preparing for release.
