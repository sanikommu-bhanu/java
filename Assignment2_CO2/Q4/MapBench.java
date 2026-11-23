import java.util.*;

/** Q4 - HashMap vs TreeMap vs LinkedHashMap for a pharmacy inventory. Acceptable lookup time (defined): <= 1 microsecond average (far below 1 ms human-perceptible limit). */
public class MapBench {
    static String code(int i) { return String.format("MED%07d", i); }
    public static void main(String[] args) {
        int[] sizes = {1_000, 50_000, 300_000};
        for (int n : sizes) {
            List<Integer> perm = new ArrayList<>(); for (int i = 0; i < n; i++) perm.add(i);
            Collections.shuffle(perm, new Random(5));
            String[] insertion = new String[n]; for (int i = 0; i < n; i++) insertion[i] = code(perm.get(i));   // added in random code order
            String[] sorted = insertion.clone(); Arrays.sort(sorted);
            Random rnd = new Random(9); String[] probes = new String[100_000]; for (int i = 0; i < probes.length; i++) probes[i] = insertion[rnd.nextInt(n)];
            System.out.println("=== n = " + n + " medicines ===");
            System.out.printf("%-14s %10s %14s %16s %18s%n", "map", "put all ms", "100k get ms", "code-order rpt ms", "insertion-order rpt ms");
            Map<String, Integer>[] maps = new Map[]{new HashMap<>(), new TreeMap<>(), new LinkedHashMap<>()};
            String[] names = {"HashMap", "TreeMap", "LinkedHashMap"};
            for (int m = 0; m < 3; m++) {
                Map<String, Integer> map = maps[m];
                long t0 = System.nanoTime(); for (int i = 0; i < n; i++) map.put(insertion[i], i); long put = (System.nanoTime() - t0) / 1_000_000;
                long sum = 0; t0 = System.nanoTime(); for (String p : probes) sum += map.get(p); long get = (System.nanoTime() - t0) / 1_000_000;
                // code-order report
                t0 = System.nanoTime(); StringBuilder rep = new StringBuilder(); List<String> order;
                if (m == 1) order = new ArrayList<>(map.keySet()); else { order = new ArrayList<>(map.keySet()); Collections.sort(order); }   // extra sort for Hash/Linked
                for (String k : order) rep.append(k).append(map.get(k)).append('\n');
                long codeRpt = (System.nanoTime() - t0) / 1_000_000;
                boolean codeOk = order.equals(Arrays.asList(sorted));
                // insertion-order report
                String insRpt; boolean insOk = false; t0 = System.nanoTime();
                if (m == 2) { order = new ArrayList<>(map.keySet()); insOk = order.equals(Arrays.asList(insertion)); insRpt = String.valueOf((System.nanoTime() - t0) / 1_000_000); }
                else { insRpt = "n/a (side list)"; }
                System.out.printf("%-14s %10d %14d %12d %-4s %14s %-4s  (checksum %d)%n", names[m], put, get, codeRpt, codeOk ? "OK" : "BAD", insRpt, m == 2 ? (insOk ? "OK" : "BAD") : "", sum % 1000);
            }
            // sorted array + binarySearch
            long t0 = System.nanoTime(); long s = 0; for (String p : probes) s += Arrays.binarySearch(sorted, p); long bs = (System.nanoTime() - t0) / 1_000_000;
            ArrayList<String> sl = new ArrayList<>(Arrays.asList(sorted)); t0 = System.nanoTime();
            for (int i = 0; i < 2000; i++) { String k = "ZZZ" + i; int pos = Collections.binarySearch(sl, k); sl.add(-pos - 1, k); }
            long ins = (System.nanoTime() - t0) / 1000;
            System.out.println("sorted String[] + binarySearch: 100k lookups " + bs + " ms (O(log n)); 2,000 insertions into sorted list " + ins + " us (O(n) shift each); code-order report = plain O(n) scan");
        }
        System.out.println("Decision: HashMap for the 100k-lookups workload (O(1)); the nightly code-order report needs one O(n log n) sort, or use TreeMap if the code-order report must be cheap");
        System.out.println("and lookups are fewer; LinkedHashMap gives the insertion-order report for free at ~1 extra pointer pair per entry.");
        System.out.println("Hashtable is not preferred: legacy, every method synchronized (contention, no benefit single-threaded), no null keys; use HashMap or ConcurrentHashMap.");
    }
}
