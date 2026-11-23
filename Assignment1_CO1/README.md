# Assignment 1 - CO1: Object-Oriented Model for a Campus Smart-Parking System

Name: ____________   Register number: ____________   Java version: 21 (OpenJDK)
Suggested GitHub repository name: `CSA09-<RegisterNumber>-Assignment1` (one commit per task)

Every task folder has the Java source (`*.java`, plus `pseudocode.txt`/`schema.sql` where needed) and `output.txt` (program output + notes/answers).

| Task folder | Compile and run (from inside the folder) |
|---|---|
| `Task01` | `javac Task01.java ; java Task01` |
| `Task02` | `javac Task02.java ; java Task02` |
| `Task03` | `javac ParkingFacility.java ; java ParkingFacility` |
| `Task04` | `javac Task04.java ; java Task04` |
| `Task05` | `javac Task05.java ; java Task05` |
| `Task06` | `javac Task06.java ; java Task06` |
| `Task07` | `javac Task07.java ; java Task07` |
| `Task08` | `javac FeeDemo.java ; java FeeDemo ; java Overloads` |
| `Task09` | `javac Task09.java ; java Task09` |
| `Task10` | `rm -rf /tmp/t10 && mkdir -p /tmp/t10 && javac -d /tmp/t10 $(find . -name '*.java') ; java -cp /tmp/t10 parking.app.Main` |
| `Task11` | `javac ParkingSystem.java ; java ParkingSystem` |
| `Task12` | `javac DesignChallenge.java ; java DesignChallenge` |

Notes:
- Timing results depend on your machine: re-run and use your own numbers/screenshots in the report.
- GUI programs need a desktop; when started headless they run a logic self-test instead (these were the outputs captured).
- Programs using MySQL need the MySQL Connector/J JAR (`java -cp .:mysql-connector-j.jar ...`) and the `schema.sql` in the task folder.
