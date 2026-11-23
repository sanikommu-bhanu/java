import java.util.*;

/**
 * Q7 - Diagnose, Redesign and Validate.  Chosen solution from Q1-Q6: Q6 emergency triage with the FAIR (aging) rule.
 *
 * A. DIAGNOSIS  : the plain PriorityQueue is O(log n), but to be FAIR a patient's priority must improve with waiting time.
 *    The obvious implementation recomputes "effective severity = severity*W - waited" for EVERY waiting patient each time a
 *    doctor asks for the next one: a full scan, O(n) per call, O(n^2) for n patients; repeated work, grows with n.
 *    Cause: the priority is expressed relative to 'now', so it changes with time and no heap can keep it ordered.
 * B. OBJECTIVE  : keep the SAME fair order but avoid scanning the waiting list; target: next() in O(log n); 40,000 patients in < 1 s.
 * C. REDESIGN   : effective(now) = severity*W + arrival - now.  'now' is the same for every patient, so ordering by
 *    score = arrival + severity*W gives the identical order. The score never changes -> compute ONCE at admission, keep in a PriorityQueue.
 *    Property exploited: subtracting a common value does not change an ordering. Cost: one long per patient; trade-off: W is fixed at
 *    admission (changing the aging rule requires rebuilding the heap).
 * D. PREDICTION : same serving order; time per next() grows ~log n instead of ~n, so 8x more patients -> ~8x time (heap) vs ~64x (scan).
 */
public class AgingRedesign {
    static final int W = 3;
    record P(String name, int severity, long arrival) {}

    static class NaiveER {                                    // ORIGINAL
        final List<P> list = new ArrayList<>(); long now = 0, evaluations = 0;
        void admit(String n, int s) { list.add(new P(n, s, now)); now++; }
        P next() {
            int bestIdx = -1; long bestEff = Long.MAX_VALUE;
            for (int i = 0; i < list.size(); i++) {
                P p = list.get(i); long eff = (long) p.severity() * W - (now - p.arrival()); evaluations++;   // recomputed for everybody
                if (eff < bestEff) { bestEff = eff; bestIdx = i; }
            }
            return bestIdx < 0 ? null : list.remove(bestIdx);
        }
    }
    static class FairER {                                     // REDESIGN
        long comparisons = 0, now = 0;
        final PriorityQueue<P> q = new PriorityQueue<>((a, b) -> { comparisons++; int c = Long.compare(a.arrival() + (long) a.severity() * W, b.arrival() + (long) b.severity() * W); return c != 0 ? c : Long.compare(a.arrival(), b.arrival()); });   // tie -> earlier arrival
        void admit(String n, int s) { q.add(new P(n, s, now)); now++; }
        P next() { return q.poll(); }
    }

    public static void main(String[] args) {
        System.out.printf("%-8s | %-34s | %-34s | %s%n", "n", "ORIGINAL scan (time ms, evaluations)", "REDESIGN heap (time ms, comparisons)", "same order?");
        for (int n : new int[]{5_000, 20_000, 40_000}) {
            Random r = new Random(11); int[] sev = new int[n]; for (int i = 0; i < n; i++) sev[i] = 1 + r.nextInt(5);
            NaiveER a = new NaiveER(); FairER b = new FairER();
            for (int i = 0; i < n; i++) { a.admit("P" + i, sev[i]); b.admit("P" + i, sev[i]); }
            String[] oa = new String[n], ob = new String[n];
            long t0 = System.nanoTime(); for (int i = 0; i < n; i++) oa[i] = a.next().name(); long ta = (System.nanoTime() - t0) / 1_000_000;
            t0 = System.nanoTime(); for (int i = 0; i < n; i++) ob[i] = b.next().name(); long tb = (System.nanoTime() - t0) / 1_000_000;
            System.out.printf("%-8d | %10d ms, %,15d evals | %10d ms, %,15d cmps | %s%n", n, ta, a.evaluations, tb, b.comparisons, Arrays.equals(oa, ob));
        }
        System.out.println("Original: evaluations ~ n^2/2 (8x patients -> ~64x work). Redesign: comparisons ~ n log n.");
        System.out.println("The original may still be preferable when n is tiny (<~100: no heap overhead) or when the aging rule is non-linear / changes every minute.");
    }
}
