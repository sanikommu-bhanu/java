import java.sql.*;
import java.util.*;

/** A3 - generic Dao<T,K>; StudentDao (JDBC, compile-only here) and an in-memory implementation that runs without a database. */
record Student(String regNo, String name, String dept) {}

interface Dao<T, K> {
    void insert(T entity) throws SQLException;
    T findById(K id) throws SQLException;
    List<T> findAll() throws SQLException;
    boolean update(T entity) throws SQLException;
    boolean delete(K id) throws SQLException;
}
class StudentDao implements Dao<Student, String> {               // JDBC implementation: table student(reg_no PK, name, dept)
    private final String url, user, pwd;
    StudentDao(String url, String user, String pwd) { this.url = url; this.user = user; this.pwd = pwd; }
    private Connection con() throws SQLException { return DriverManager.getConnection(url, user, pwd); }
    private static Student map(ResultSet rs) throws SQLException { return new Student(rs.getString("reg_no"), rs.getString("name"), rs.getString("dept")); }
    public void insert(Student s) throws SQLException {
        try (Connection c = con(); PreparedStatement ps = c.prepareStatement("INSERT INTO student(reg_no,name,dept) VALUES (?,?,?)")) {
            ps.setString(1, s.regNo()); ps.setString(2, s.name()); ps.setString(3, s.dept()); ps.executeUpdate();
        }
    }
    public Student findById(String id) throws SQLException {
        try (Connection c = con(); PreparedStatement ps = c.prepareStatement("SELECT reg_no,name,dept FROM student WHERE reg_no=?")) {
            ps.setString(1, id); try (ResultSet rs = ps.executeQuery()) { return rs.next() ? map(rs) : null; }
        }
    }
    public List<Student> findAll() throws SQLException {
        List<Student> out = new ArrayList<>();
        try (Connection c = con(); Statement st = c.createStatement(); ResultSet rs = st.executeQuery("SELECT reg_no,name,dept FROM student")) { while (rs.next()) out.add(map(rs)); }
        return out;
    }
    public boolean update(Student s) throws SQLException {
        try (Connection c = con(); PreparedStatement ps = c.prepareStatement("UPDATE student SET name=?, dept=? WHERE reg_no=?")) {
            ps.setString(1, s.name()); ps.setString(2, s.dept()); ps.setString(3, s.regNo()); return ps.executeUpdate() == 1;
        }
    }
    public boolean delete(String id) throws SQLException {
        try (Connection c = con(); PreparedStatement ps = c.prepareStatement("DELETE FROM student WHERE reg_no=?")) { ps.setString(1, id); return ps.executeUpdate() == 1; }
    }
}
class InMemoryStudentDao implements Dao<Student, String> {
    private final Map<String, Student> rows = new LinkedHashMap<>();
    public void insert(Student s) { rows.put(s.regNo(), s); }
    public Student findById(String id) { return rows.get(id); }
    public List<Student> findAll() { return new ArrayList<>(rows.values()); }
    public boolean update(Student s) { return rows.replace(s.regNo(), s) != null; }
    public boolean delete(String id) { return rows.remove(id) != null; }
}
public class DaoDemo {
    public static void main(String[] args) throws Exception {
        Dao<Student, String> dao = new InMemoryStudentDao();           // service code depends only on the interface
        dao.insert(new Student("192311001", "Arun", "CSE")); dao.insert(new Student("192311002", "Divya", "ECE"));
        System.out.println("findById(192311002) -> " + dao.findById("192311002"));
        System.out.println("update -> " + dao.update(new Student("192311002", "Divya R", "ECE")) + ", now " + dao.findById("192311002"));
        System.out.println("delete(192311001) -> " + dao.delete("192311001") + ", findAll -> " + dao.findAll());
        System.out.println("Swapping InMemoryStudentDao for StudentDao(JDBC) changes ONE line; renaming a column changes ONE class (StudentDao) instead of every GUI/service class.");
    }
}
