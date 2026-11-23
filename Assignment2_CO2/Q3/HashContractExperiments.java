import java.util.*;

/** Q3 - three designed tests of the equals()/hashCode() contract, each with a prediction made BEFORE running. */
class BadStudent {                                   // the junior developer's class: equals() only
    final String regNo; BadStudent(String r) { regNo = r; }
    @Override public boolean equals(Object o) { return o instanceof BadStudent b && regNo.equals(b.regNo); }
}
class MutableStudent {                               // correct equals+hashCode but regNo is NOT final
    String regNo; final String name; MutableStudent(String r, String n) { regNo = r; name = n; }
    @Override public boolean equals(Object o) { return o instanceof MutableStudent m && regNo.equals(m.regNo); }
    @Override public int hashCode() { return regNo.hashCode(); }
}
class NameOrdered implements Comparable<NameOrdered> {   // compareTo inconsistent with equals
    final String regNo, name; NameOrdered(String r, String n) { regNo = r; name = n; }
    @Override public int compareTo(NameOrdered o) { return name.compareTo(o.name); }
    @Override public boolean equals(Object o) { return o instanceof NameOrdered n && regNo.equals(n.regNo); }
    @Override public int hashCode() { return regNo.hashCode(); }
}
public class HashContractExperiments {
    static void show(String test, int predicted, int actual, String why) {
        System.out.println(test + "\n   predicted size = " + predicted + ", observed size = " + actual + (predicted == actual ? "  (prediction correct)" : "  (prediction WRONG)") + "\n   reason: " + why);
    }
    public static void main(String[] args) {
        // Test 1: no repeated scans -> the faulty class APPEARS to work
        Set<BadStudent> s1 = new HashSet<>();
        for (String r : new String[]{"A1", "A2", "A3"}) s1.add(new BadStudent(r));
        show("Test 1 - three different students, no repeats (BadStudent)", 3, s1.size(), "no duplicates exist, so nothing can go wrong; the defect is hidden");

        // Test 2: repeated scans -> duplicates appear
        Set<BadStudent> s2 = new HashSet<>();
        for (String r : new String[]{"A1", "A2", "A1", "A3", "A2"}) s2.add(new BadStudent(r));
        show("Test 2 - 5 scans of 3 students (BadStudent, hashCode not overridden)", 5, s2.size(),
                "Object.hashCode() is identity-based, equal students land in different buckets, so equals() is never even called");
        Set<Student> fixed = new HashSet<>();
        for (String r : new String[]{"A1", "A2", "A1", "A3", "A2"}) fixed.add(new Student(r, "x"));
        show("Test 2b - same scans with the corrected Student class", 3, fixed.size(), "equal objects now share a hash code, same bucket, equals() detects the duplicate");

        // Test 3: mutate a field used by hashCode() after insertion
        Set<MutableStudent> s3 = new HashSet<>();
        MutableStudent m = new MutableStudent("B1", "Meena"); s3.add(m);
        m.regNo = "B9";                                   // hash code changes while the object sits in its old bucket
        System.out.println("Test 3 - regNo changed after insertion");
        System.out.println("   contains(m) = " + s3.contains(m) + " (predicted false: searched in the NEW bucket), remove(m) = " + s3.remove(m)
                + ", size still " + s3.size() + " -> the object is lost inside the set (memory leak)");

        // Test 4: TreeSet with compareTo inconsistent with equals
        TreeSet<NameOrdered> t = new TreeSet<>();
        t.add(new NameOrdered("C1", "Ravi")); t.add(new NameOrdered("C2", "Ravi")); t.add(new NameOrdered("C1", "Zoya"));
        show("Test 4 - TreeSet ordered by name; C1/Ravi, C2/Ravi (different students, same name), C1/Zoya (same regNo as first)", 2, t.size(),
                "TreeSet uses compareTo only: C2/Ravi is dropped as a 'duplicate' of C1/Ravi although equals() says they differ");

        // Contract check on the corrected class
        Student a = new Student("A1", "Arun"), b = new Student("A1", "Arun2");
        System.out.println("Contract check: a.equals(b)=" + a.equals(b) + ", hash(a)==hash(b)=" + (a.hashCode() == b.hashCode()));

        // Alternative designs: time for 500,000 scans (200,000 unique)
        int scans = 500_000; String[] ids = new String[scans]; Random rnd = new Random(7);
        for (int i = 0; i < scans; i++) ids[i] = "ID" + rnd.nextInt(200_000);
        long t0 = System.nanoTime(); Set<Student> hs = new HashSet<>(); for (String id : ids) hs.add(new Student(id, "n"));
        long tHash = (System.nanoTime() - t0) / 1_000_000;
        t0 = System.nanoTime(); TreeSet<Student> ts = new TreeSet<>(Comparator.comparing(Student::toString)); for (String id : ids) ts.add(new Student(id, "n"));
        long tTree = (System.nanoTime() - t0) / 1_000_000;
        t0 = System.nanoTime(); Map<String, Student> hm = new HashMap<>(); for (String id : ids) hm.putIfAbsent(id, new Student(id, "n"));
        long tMap = (System.nanoTime() - t0) / 1_000_000;
        System.out.println("500,000 scans: HashSet " + tHash + " ms (" + hs.size() + " unique), TreeSet(comparator) " + tTree + " ms (" + ts.size() + " unique), HashMap<regNo,Student> " + tMap + " ms (" + hm.size() + " unique)");
    }
}
