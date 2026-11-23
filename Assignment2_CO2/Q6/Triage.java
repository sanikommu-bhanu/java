import java.util.Comparator;
import java.util.PriorityQueue;

class Patient {
    final String name; final int severity; final long arrival;
    Patient(String name, int severity, long arrival) {
        this.name = name; this.severity = severity; this.arrival = arrival;
    }
    @Override public String toString() { return name + "(S" + severity + ")"; }
}

public class Triage {
    private long counter = 0;
    // TODO 1: Create a PriorityQueue that serves the lowest severity number first and,
    // for equal severity, the earliest arrival first
    private final PriorityQueue<Patient> queue = new PriorityQueue<>(
            Comparator.comparingInt((Patient p) -> p.severity).thenComparingLong(p -> p.arrival));

    void admit(String name, int severity) {
        queue.add(new Patient(name, severity, counter++));
    }
    Patient next() { return queue.poll(); }

    public static void main(String[] args) {
        Triage er = new Triage();
        er.admit("Ravi", 3); er.admit("Meena", 1); er.admit("John", 3);
        er.admit("Asha", 5); er.admit("Kumar", 1); er.admit("Lata", 2);
        StringBuilder order = new StringBuilder();
        Patient p;
        while ((p = er.next()) != null) order.append(p).append(' ');
        System.out.println(order.toString().trim());
    }
}

/** Extra experiments for Q6. Run: java TriageExperiments */
class TriageExperiments {
    public static void main(String[] args) {
        // Hand-checked instance: 8 patients. Correct treatment order worked out by hand:
        // severity 1: Meena(arr 1), Kumar(arr 4) | severity 2: Lata(5) | severity 3: Ravi(0), John(2), Omar(6) | severity 4: Dev(7)... listed below
        String[] names = {"Ravi", "Meena", "John", "Asha", "Kumar", "Lata", "Omar", "Dev"};
        int[] sev =      {3,       1,       3,      5,      1,       2,      3,      4};
        String[] hand = {"Meena", "Kumar", "Lata", "Ravi", "John", "Omar", "Dev", "Asha"};
        PriorityQueue<Patient> q = new PriorityQueue<>(Comparator.comparingInt((Patient p) -> p.severity).thenComparingLong(p -> p.arrival));
        for (int i = 0; i < names.length; i++) q.add(new Patient(names[i], sev[i], i));
        System.out.println("println(queue) (heap array order, NOT treatment order): " + q);
        StringBuilder sb = new StringBuilder(); boolean ok = true; int i = 0;
        while (!q.isEmpty()) { Patient p = q.poll(); sb.append(p).append(' '); ok &= p.name.equals(hand[i++]); }
        System.out.println("Treatment order (poll): " + sb.toString().trim());
        System.out.println("Matches hand-worked order " + String.join(", ", hand) + " ? " + ok);

        // Growth investigation: predicted add = O(log n), poll = O(log n)
        for (int n : new int[]{10_000, 100_000, 1_000_000}) {
            PriorityQueue<Patient> big = new PriorityQueue<>(Comparator.comparingInt((Patient p) -> p.severity).thenComparingLong(p -> p.arrival));
            java.util.Random r = new java.util.Random(3);
            long t0 = System.nanoTime(); for (int k = 0; k < n; k++) big.add(new Patient("P" + k, 1 + r.nextInt(5), k));
            long add = System.nanoTime() - t0;
            t0 = System.nanoTime(); while (!big.isEmpty()) big.poll(); long poll = System.nanoTime() - t0;
            System.out.printf("n=%,9d  add: %7.0f ns/op   poll: %7.0f ns/op%n", n, add / (double) n, poll / (double) n);
        }
        System.out.println("5,000 patients/day = ~0.06 patients per second -> a PriorityQueue handles millions per second: the design easily serves a city hospital.");

        // Starvation + fair rule: effective score = arrival + severity * W (each severity level counts as W arrivals of waiting)
        System.out.println("Starvation demo: one S5 patient arrives first, then S1 patients keep arriving (one per tick). Plain rule serves S5 last;");
        int W = 3; PriorityQueue<Patient> fair = new PriorityQueue<>(Comparator.comparingLong((Patient p) -> p.arrival + (long) p.severity * W));
        fair.add(new Patient("Asha", 5, 0));
        for (int k = 1; k <= 20; k++) fair.add(new Patient("Crit" + k, 1, k));
        int pos = 1; while (!fair.isEmpty()) { Patient p = fair.poll(); if (p.name.equals("Asha")) { System.out.println("With aging rule (W=3) Asha is served at position " + pos + " of 21 instead of 21."); } pos++; }
    }
}
