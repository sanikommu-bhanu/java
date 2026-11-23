import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/** Q4 - Four scanner stations: Thread vs Runnable, life cycle, join, priorities, daemon, run() vs start(), ExecutorService. */
public class ScannerStations {
    static final AtomicInteger total = new AtomicInteger();
    static final List<String> finishOrder = Collections.synchronizedList(new ArrayList<>());

    static class StationThread extends Thread {                      // way 1: extend Thread
        final int scripts; StationThread(String name, int scripts) { super(name); this.scripts = scripts; }
        @Override public void run() { scan(getName(), scripts); }
    }
    static class StationTask implements Runnable {                   // way 2: implement Runnable (preferred: class can still extend another class)
        final String name; final int scripts; StationTask(String n, int s) { name = n; scripts = s; }
        @Override public void run() { scan(name, scripts); }
    }
    static void scan(String station, int scripts) {
        try { Thread.sleep(50 + (int) (Math.random() * 150)); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }  // TIMED_WAITING
        total.addAndGet(scripts); finishOrder.add(station);
    }
    public static void main(String[] args) throws Exception {
        System.out.println("=== Way 1: extends Thread ===");
        StationThread[] ts = new StationThread[4]; for (int i = 0; i < 4; i++) ts[i] = new StationThread("S" + (i + 1), 100 * (i + 1));
        System.out.println("state before start: " + ts[0].getState() + " (NEW)");
        for (Thread t : ts) t.start();
        System.out.println("right after start : " + ts[0].getState() + " / isAlive=" + ts[0].isAlive());
        for (Thread t : ts) t.join();                                  // main waits (WAITING) until each station is TERMINATED
        System.out.println("after join        : " + ts[0].getState() + " / isAlive=" + ts[0].isAlive() + "   total scanned = " + total.get() + " (expected 1000)");

        System.out.println("=== Way 2: implements Runnable ===");
        total.set(0); Thread[] rs = new Thread[4]; for (int i = 0; i < 4; i++) { rs[i] = new Thread(new StationTask("R" + (i + 1), 100 * (i + 1)), "R" + (i + 1)); rs[i].start(); }
        for (Thread t : rs) t.join(); System.out.println("total scanned = " + total.get());

        System.out.println("=== run() vs start() ===");
        Thread probe = new Thread(() -> System.out.println("   body executed by thread: " + Thread.currentThread().getName()), "station-probe");
        probe.run();  probe.join();                                    // plain method call on main thread
        Thread probe2 = new Thread(() -> System.out.println("   body executed by thread: " + Thread.currentThread().getName()), "station-probe");
        probe2.start(); probe2.join();

        System.out.println("=== 10 runs: order in which stations finish (not deterministic) ===");
        for (int run = 1; run <= 10; run++) {
            finishOrder.clear(); Thread[] x = new Thread[4];
            for (int i = 0; i < 4; i++) { x[i] = new StationThread("S" + (i + 1), 10); x[i].start(); }
            for (Thread t : x) t.join();
            System.out.println("run " + run + ": " + finishOrder);
        }
        System.out.println("=== priorities (S4 = MAX, S1 = MIN) : 5 runs ===");
        for (int run = 1; run <= 5; run++) {
            finishOrder.clear(); Thread[] x = new Thread[4];
            for (int i = 0; i < 4; i++) { x[i] = new StationThread("S" + (i + 1), 10); x[i].setPriority(i == 3 ? Thread.MAX_PRIORITY : i == 0 ? Thread.MIN_PRIORITY : Thread.NORM_PRIORITY); x[i].start(); }
            for (Thread t : x) t.join();
            System.out.println("run " + run + ": " + finishOrder);
        }
        System.out.println("(priority is only a hint to the OS scheduler; with sleeping threads it does not decide the order)");

        System.out.println("=== Alternative: ExecutorService, fixed pool of 2 threads, 4 stations ===");
        ExecutorService pool = Executors.newFixedThreadPool(2); long t0 = System.nanoTime(); total.set(0);
        List<Future<?>> fs = new ArrayList<>();
        for (int i = 0; i < 4; i++) { final int id = i + 1; fs.add(pool.submit(() -> { scan("E" + id, 100 * id); System.out.println("   station E" + id + " ran on " + Thread.currentThread().getName()); })); }
        for (Future<?> f : fs) f.get(); pool.shutdown();
        System.out.println("total = " + total.get() + ", elapsed " + (System.nanoTime() - t0) / 1_000_000 + " ms (~ 4 stations / 2 threads = 2 batches)");

        System.out.println("=== daemon thread: main() ends first ===");
        Thread daemon = new Thread(() -> { try { Thread.sleep(1500); System.out.println("DAEMON FINISHED (should never appear)"); } catch (InterruptedException e) { } }, "daemon-station");
        daemon.setDaemon(true); daemon.start();
        Thread.sleep(200);
        System.out.println("main() ending now; daemon alive=" + daemon.isAlive() + ", isDaemon=" + daemon.isDaemon() + " -> JVM exits without waiting for it");
    }
}
