# Assignment 2 - CO2: Strings, Collections, Generics

Name: ____________   Register number: ____________   Java version: 21 (OpenJDK)
Suggested GitHub repository name: `CSA09-<RegisterNumber>-Assignment2` (one commit per task)

Every task folder has the Java source (`*.java`, plus `pseudocode.txt`/`schema.sql` where needed) and `output.txt` (program output + notes/answers).

| Task folder | Compile and run (from inside the folder) |
|---|---|
| `Q1` | `javac StringBench.java ; java StringBench` |
| `Q2` | `javac ListBench.java ; java ListBench` |
| `Q3` | `javac *.java ; java Attendance ; java HashContractExperiments` |
| `Q4` | `javac -Xlint:none MapBench.java 2>&1 \| grep -v Note ; java MapBench` |
| `Q5` | `javac LeaderboardDemo.java ; java LeaderboardDemo ; java ComparatorTests` |
| `Q6` | `javac Triage.java ; java Triage ; java TriageExperiments` |
| `Q7` | `javac AgingRedesign.java ; java AgingRedesign` |

Notes:
- Timing results depend on your machine: re-run and use your own numbers/screenshots in the report.
- GUI programs need a desktop; when started headless they run a logic self-test instead (these were the outputs captured).
- Programs using MySQL need the MySQL Connector/J JAR (`java -cp .:mysql-connector-j.jar ...`) and the `schema.sql` in the task folder.
