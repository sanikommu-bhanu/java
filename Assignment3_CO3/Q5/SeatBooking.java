import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

public class SeatBooking {
    private final boolean[] booked;
    private int bookingsMade = 0;
    SeatBooking(int seats) { booked = new boolean[seats]; }

    // TODO 1: Make the check-then-book operation atomic so that a seat can never be
    // sold twice
    public synchronized boolean book(int seat) {
        if (!booked[seat]) { booked[seat] = true; bookingsMade++; return true; }
        return false;
    }

    public static void main(String[] args) throws InterruptedException {
        SeatBooking screen = new SeatBooking(10);
        Thread[] users = new Thread[100];
        for (int i = 0; i < users.length; i++) {
            final int seat = i % 10; // 10 users compete for every
            // seat
            users[i] = new Thread(() -> screen.book(seat));
            users[i].start();
        }
        for (Thread t : users) t.join();
        System.out.println("Seats sold: " + screen.bookingsMade);
    }
}

/** Extra: reproduces the race (Student A) and compares strategies (Student B). Run: java RaceExperiments
 *  CRITICAL SECTION = the check 'if (!booked[seat])' together with the writes 'booked[seat]=true; bookingsMade++'.
 *  Interleaving: T1 checks seat 3 (free) -> T2 checks seat 3 (free) -> T1 books -> T2 books -> seat sold twice, counter 2 for one seat.
 */
class RaceExperiments {
    interface Booker { void book(int seat); int sold(); }
    static class Unsafe implements Booker {
        final boolean[] b; int made; final boolean widen;
        Unsafe(int n, boolean widen) { b = new boolean[n]; this.widen = widen; }
        public void book(int s) { if (!b[s]) { if (widen) Thread.yield(); b[s] = true; made++; } }   // yield() widens the race window
        public int sold() { return made; }
    }
    static class SyncMethod implements Booker {
        final boolean[] b; int made; SyncMethod(int n) { b = new boolean[n]; }
        public synchronized void book(int s) { if (!b[s]) { b[s] = true; made++; } }                 // lock = this, whole method
        public int sold() { synchronized (this) { return made; } }
    }
    static class SyncBlock implements Booker {
        final boolean[] b; int made; final Object lock = new Object(); SyncBlock(int n) { b = new boolean[n]; }
        public void book(int s) { synchronized (lock) { if (!b[s]) { b[s] = true; made++; } } }       // lock only the critical section
        public int sold() { synchronized (lock) { return made; } }
    }
    static class WithReentrantLock implements Booker {
        final boolean[] b; int made; final ReentrantLock lock = new ReentrantLock(); WithReentrantLock(int n) { b = new boolean[n]; }
        public void book(int s) { lock.lock(); try { if (!b[s]) { b[s] = true; made++; } } finally { lock.unlock(); } }
        public int sold() { lock.lock(); try { return made; } finally { lock.unlock(); } }
    }
    static class WithAtomic implements Booker {
        final AtomicBoolean[] b; final java.util.concurrent.atomic.AtomicInteger made = new java.util.concurrent.atomic.AtomicInteger();
        WithAtomic(int n) { b = new AtomicBoolean[n]; for (int i = 0; i < n; i++) b[i] = new AtomicBoolean(); }
        public void book(int s) { if (b[s].compareAndSet(false, true)) made.incrementAndGet(); }
        public int sold() { return made.get(); }
    }
    static int race(Booker bk, int users, int seats) throws Exception {
        Thread[] ts = new Thread[users];
        for (int i = 0; i < users; i++) { final int seat = i % seats; ts[i] = new Thread(() -> bk.book(seat)); ts[i].start(); }
        for (Thread t : ts) t.join(); return bk.sold();
    }
    public static void main(String[] args) throws Exception {
        StringBuilder a = new StringBuilder(), b = new StringBuilder(), c = new StringBuilder();
        for (int run = 1; run <= 10; run++) {
            a.append(race(new Unsafe(10, false), 100, 10)).append(' ');
            b.append(race(new Unsafe(10, true), 100, 10)).append(' ');
            c.append(race(new SyncMethod(10), 100, 10)).append(' ');
        }
        System.out.println("10 runs, 100 users, 10 seats (correct answer is always 10)");
        System.out.println("  unsynchronised (natural timing)       : " + a);
        System.out.println("  unsynchronised (race window widened)  : " + b + "  <-- values above 10 = double bookings");
        System.out.println("  synchronized method                   : " + c);
        System.out.println("Timing: 10,000 bookings over 1,000 seats by 8 pool threads (ms, average of 5 runs)");
        String[] names = {"synchronized method", "synchronized block", "ReentrantLock", "AtomicBoolean CAS"};
        for (int k = 0; k < 4; k++) {
            long sum = 0; int sold = 0;
            for (int r = 0; r < 5; r++) {
                Booker bk = k == 0 ? new SyncMethod(1000) : k == 1 ? new SyncBlock(1000) : k == 2 ? new WithReentrantLock(1000) : new WithAtomic(1000);
                ExecutorService ex = Executors.newFixedThreadPool(8); long t0 = System.nanoTime();
                for (int i = 0; i < 10_000; i++) { final int seat = i % 1000; ex.execute(() -> bk.book(seat)); }
                ex.shutdown(); ex.awaitTermination(30, TimeUnit.SECONDS); sum += System.nanoTime() - t0; sold = bk.sold();
            }
            System.out.printf("  %-22s %6d ms   seats sold = %d%n", names[k], sum / 5 / 1_000_000, sold);
        }
        System.out.println("Static synchronization (synchronized static method) locks the Class object: needed only if the booking state is static / shared by ALL screens.");
        System.out.println("Each instance method above uses the intrinsic lock of 'this' (method version) or of the private 'lock' object (block version).");
        System.out.println("Deployment for 20 screens: one SeatBooking object per screen with synchronized block / lock -> screens never block each other.");
    }
}
