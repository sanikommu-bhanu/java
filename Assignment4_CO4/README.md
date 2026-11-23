# Assignment 4 - CO4: Event-Driven GUI Design (AWT/Swing + JDBC)

Name: ____________   Register number: ____________   Java version: 21 (OpenJDK)
Suggested GitHub repository name: `CSA09-<RegisterNumber>-Assignment4` (one commit per task)

Every task folder has the Java source (`*.java`, plus `pseudocode.txt`/`schema.sql` where needed) and `output.txt` (program output + notes/answers).

| Task folder | Compile and run (from inside the folder) |
|---|---|
| `Task1` | `javac FeeWindow.java ; java -Djava.awt.headless=true FeeWindow` |
| `Task2` | `javac AppointmentDesk.java ; java -Djava.awt.headless=true AppointmentDesk` |
| `Task3` | `javac LibrarySearch.java ; java -Djava.awt.headless=true LibrarySearch` |
| `Task4` | `javac DrawPad.java` |
| `Task5` | `javac Converter.java ConverterStyles.java ; java -Djava.awt.headless=true ConverterStyles` |
| `Task6` | `javac EventRegistrationApp.java ; java -Djava.awt.headless=true EventRegistrationApp` |

Notes:
- Timing results depend on your machine: re-run and use your own numbers/screenshots in the report.
- GUI programs need a desktop; when started headless they run a logic self-test instead (these were the outputs captured).
- Programs using MySQL need the MySQL Connector/J JAR (`java -cp .:mysql-connector-j.jar ...`) and the `schema.sql` in the task folder.
