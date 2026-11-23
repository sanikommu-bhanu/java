import java.util.*;

/** Q2 - ArrayList vs LinkedList vs ArrayDeque for a playlist editor. Times in ms (best-effort, averaged over 3 runs). */
public class ListBench {
    static final int OPS = 1000;
    static long run(Runnable r) { long best = 0; for (int i = 0; i < 3; i++) { long t = System.nanoTime(); r.run(); best += System.nanoTime() - t; } return best / 3 / 1000; } // microseconds
    static List<Integer> fill(List<Integer> l, int n) { for (int i = 0; i < n; i++) l.add(i); return l; }

    public static void main(String[] args) {
        int[] sizes = {10_000, 50_000, 100_000};
        System.out.println("Times in microseconds (us) for " + OPS + " operations unless noted (iterate = whole list once)");
        System.out.printf("%-8s %-10s %12s %12s %12s %12s %12s%n", "size", "structure", "add-end", "insert-front", "insert-mid", "get(k)", "iterate");
        for (int n : sizes) {
            for (String name : new String[]{"ArrayList", "LinkedList", "ArrayDeque"}) {
                List<Integer> list = name.equals("ArrayList") ? new ArrayList<>() : name.equals("LinkedList") ? new LinkedList<>() : null;
                long addEnd, front, mid = -1, get = -1, iter;
                if (list != null) {
                    final List<Integer> L = fill(list, n);
                    addEnd = run(() -> { for (int i = 0; i < OPS; i++) { L.add(i); } for (int i = 0; i < OPS; i++) L.remove(L.size() - 1); });
                    front = run(() -> { for (int i = 0; i < OPS; i++) L.add(0, i); for (int i = 0; i < OPS; i++) L.remove(0); });
                    mid = run(() -> { for (int i = 0; i < OPS; i++) L.add(L.size() / 2, i); for (int i = 0; i < OPS; i++) L.remove(L.size() / 2); });
                    final Random rnd = new Random(1);
                    get = run(() -> { long s = 0; for (int i = 0; i < OPS; i++) s += L.get(rnd.nextInt(L.size())); if (s == 42) System.out.print(""); });
                    iter = run(() -> { long s = 0; for (int x : L) s += x; if (s == 42) System.out.print(""); });
                } else {
                    final ArrayDeque<Integer> D = new ArrayDeque<>(); for (int i = 0; i < n; i++) D.add(i);
                    addEnd = run(() -> { for (int i = 0; i < OPS; i++) D.addLast(i); for (int i = 0; i < OPS; i++) D.removeLast(); });
                    front = run(() -> { for (int i = 0; i < OPS; i++) D.addFirst(i); for (int i = 0; i < OPS; i++) D.removeFirst(); });
                    iter = run(() -> { long s = 0; for (int x : D) s += x; if (s == 42) System.out.print(""); });
                }
                System.out.printf("%-8d %-10s %12d %12d %12s %12s %12d%n", n, name, addEnd, front, mid < 0 ? "n/a" : mid, get < 0 ? "n/a" : get, iter);
            }
        }
        // plain array with manual shifting
        int n = 100_000; int[] arr = new int[n + OPS]; for (int i = 0; i < n; i++) arr[i] = i;
        long t0 = System.nanoTime(); int size = n;
        for (int i = 0; i < OPS; i++) { System.arraycopy(arr, 0, arr, 1, size); arr[0] = i; size++; }
        System.out.println("Plain int[] insert-front x" + OPS + " (n=100,000, manual shifting): " + (System.nanoTime() - t0) / 1000 + " us  (O(n) per insert, no auto-grow)");
        System.out.println("Operating boundary: ArrayList is fine for append-mostly + random access; stop recommending it when front/middle inserts dominate");
        System.out.println("and the list is large (cost grows linearly with size). Queue-like 'Up Next' usage -> ArrayDeque (O(1) at both ends, no node objects).");
    }
}
