# Assignment 5 - CO5: Data Persistence (Files and JDBC)

Name: ____________   Register number: ____________   Java version: 21 (OpenJDK)
Suggested GitHub repository name: `CSA09-<RegisterNumber>-Assignment5` (one commit per task)

Every task folder has the Java source (`*.java`, plus `pseudocode.txt`/`schema.sql` where needed) and `output.txt` (program output + notes/answers).

| Task folder | Compile and run (from inside the folder) |
|---|---|
| `TaskA1` | `javac RecordLayouts.java ; java RecordLayouts` |
| `TaskA2` | `javac LineCountDemo.java ; java LineCountDemo` |
| `TaskA3` | `javac DaoDemo.java ; java DaoDemo` |
| `TaskA4` | `javac HostelSerialization.java` |
| `TaskA5` | `javac TransferAndInjectionDemo.java ; java TransferAndInjectionDemo` |
| `TaskB1` | `javac PassStore.java ; java PassStore ; java PassStoreBench` |
| `TaskB2` | `javac Adversarial.java ; java Adversarial` |
| `TaskB3` | `javac PassLoader.java LoadBenchmark.java` |
| `TaskB4` | `javac PassLoader.java Migration.java ; java Migration` |

Notes:
- Timing results depend on your machine: re-run and use your own numbers/screenshots in the report.
- GUI programs need a desktop; when started headless they run a logic self-test instead (these were the outputs captured).
- Programs using MySQL need the MySQL Connector/J JAR (`java -cp .:mysql-connector-j.jar ...`) and the `schema.sql` in the task folder.
