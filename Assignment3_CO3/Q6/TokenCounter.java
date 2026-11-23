import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;

public class TokenCounter {
    private final int[] buffer = new int[5]; // the counter holds at most 5 tokens
    private int count = 0, in = 0, out = 0;

    public synchronized void put(int token) throws InterruptedException {
        // TODO 1: Wait while the counter is full, add the token at position in, then
        // wake waiting threads
        while (count == buffer.length) wait();
        buffer[in] = token; in = (in + 1) % buffer.length; count++;
        notifyAll();
    }
    public synchronized int take() throws InterruptedException {
        // TODO 2: Wait while the counter is empty, remove the token at position out,
        // wake waiting threads and return the token
        while (count == 0) wait();
        int t = buffer[out]; out = (out + 1) % buffer.length; count--;
        notifyAll();
        return t;
    }

    public static void main(String[] args) throws InterruptedException {
        TokenCounter counter = new TokenCounter();
        int[] collected = new int[2];
        Thread cook = new Thread(() -> {
            try {
                for (int t = 1; t <= 20; t++) counter.put(t);
            } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        });
        Thread[] students = new Thread[2];
        for (int s = 0; s < 2; s++) {
            final int id = s;
            students[s] = new Thread(() -> {
                try {
                    for (int k = 0; k < 10; k++) collected[id] += counter.take();
                } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            });
        }
        cook.start(); for (Thread t : students) t.start();
        cook.join(); for (Thread t : students) t.join();
        System.out.println("Sum of tokens collected: " + (collected[0] + collected[1])
                + " (expected 210)");
    }
}

/** Part A experiments: while vs if, counter size log, notify vs notifyAll. Run: java ProducerConsumerExperiments */
class ProducerConsumerExperiments {
    static class Counter {                                  // same logic as TokenCounter, parameterised and instrumented
        final int[] buf = new int[5]; int count, in, out, min = 0, max = 0; final boolean useWhile;
        Counter(boolean useWhile) { this.useWhile = useWhile; }
        synchronized void put(int t) throws InterruptedException {
            if (useWhile) { while (count == 5) wait(); } else { if (count == 5) wait(); }
            buf[in] = t; in = (in + 1) % 5; count++; max = Math.max(max, count); min = Math.min(min, count); notifyAll();
        }
        synchronized int take() throws InterruptedException {
            if (useWhile) { while (count == 0) wait(); } else { if (count == 0) wait(); }
            int t = buf[out]; out = (out + 1) % 5; count--; max = Math.max(max, count); min = Math.min(min, count); notifyAll(); return t;
        }
    }
    static String run(boolean useWhile) throws Exception {
        Counter c = new Counter(useWhile); int[] got = new int[3];
        Thread cook = new Thread(() -> { try { for (int t = 1; t <= 30; t++) c.put(t); } catch (InterruptedException e) { } });
        Thread[] st = new Thread[3];
        for (int s = 0; s < 3; s++) { final int id = s; st[s] = new Thread(() -> { try { for (int k = 0; k < 10; k++) got[id] += c.take(); } catch (InterruptedException e) { } }); st[s].setDaemon(true); }
        cook.setDaemon(true); cook.start(); for (Thread t : st) t.start();
        cook.join(2000); boolean hung = false; for (Thread t : st) { t.join(1000); hung |= t.isAlive(); }
        int sum = got[0] + got[1] + got[2];
        return (hung ? "HUNG (a consumer waits forever)" : "finished") + ", sum=" + sum + " (expected 465), counter size range [" + c.min + ", " + c.max + "]";
    }
    public static void main(String[] args) throws Exception {
        System.out.println("1 producer, 3 consumers, 30 tokens. Counter capacity = 5.");
        System.out.println("with WHILE (correct)  : " + run(true));
        int wrong = 0; String last = "";
        for (int i = 1; i <= 20; i++) { String r = run(false); if (!r.contains("sum=465") || r.startsWith("HUNG") || r.contains("[-")) { wrong++; last = r; } }
        System.out.println("with IF (20 runs)     : " + wrong + " of 20 runs were wrong. Example: " + (wrong == 0 ? "none observed on this machine" : last));
        System.out.println("sleep() keeps the monitor and only pauses; wait() RELEASES the monitor and needs notify. wait() is in a loop because the condition must be re-checked after waking (another consumer may have taken the token; spurious wake-ups exist).");
        System.out.println("notify() wakes only ONE arbitrary waiter: with several consumers it may wake a thread that still cannot proceed while the thread that could proceed keeps waiting (lost wake-up / hang); notifyAll() wakes all and each re-checks its condition.");
    }
}

/** Part A: deadlock between wallet transfers and the lock-ordering fix. Run: java DeadlockDemo */
class DeadlockDemo {
    static class Wallet { final int id; int balance; Wallet(int id, int b) { this.id = id; balance = b; } }
    static void pause(long ms) { try { Thread.sleep(ms); } catch (InterruptedException e) { } }
    static void unsafe(Wallet from, Wallet to, int amt) {
        synchronized (from) { pause(100); synchronized (to) { from.balance -= amt; to.balance += amt; } }      // lock order depends on arguments
    }
    static void safe(Wallet from, Wallet to, int amt) {
        Wallet first = from.id < to.id ? from : to, second = first == from ? to : from;                           // ALWAYS lock the lower id first
        synchronized (first) { pause(100); synchronized (second) { from.balance -= amt; to.balance += amt; } }
    }
    static boolean tryLockVersion(ReentrantLock l1, ReentrantLock l2) throws InterruptedException {              // alternative: tryLock with timeout
        if (l1.tryLock(200, TimeUnit.MILLISECONDS)) { try { pause(100); if (l2.tryLock(200, TimeUnit.MILLISECONDS)) { l2.unlock(); return true; } } finally { l1.unlock(); } }
        return false;
    }
    static String attempt(boolean safeVersion) throws Exception {
        Wallet a = new Wallet(1, 1000), b = new Wallet(2, 1000);
        Thread t1 = new Thread(() -> { if (safeVersion) safe(a, b, 100); else unsafe(a, b, 100); }, "T1 A->B " + (safeVersion ? "[SAFE]" : "[UNSAFE]"));
        Thread t2 = new Thread(() -> { if (safeVersion) safe(b, a, 50); else unsafe(b, a, 50); }, "T2 B->A " + (safeVersion ? "[SAFE]" : "[UNSAFE]"));
        t1.setDaemon(true); t2.setDaemon(true); t1.start(); t2.start(); t1.join(1500); t2.join(1500);
        ThreadMXBean mx = ManagementFactory.getThreadMXBean(); long[] all = mx.findDeadlockedThreads(); long[] dead = null;
        if (all != null) { List<Long> mine = new ArrayList<>(); for (java.lang.management.ThreadInfo ti : mx.getThreadInfo(all)) if (ti != null && ti.getThreadName().endsWith(safeVersion ? "[SAFE]" : "[UNSAFE]")) mine.add(ti.getThreadId()); if (!mine.isEmpty()) { dead = new long[mine.size()]; for (int i = 0; i < dead.length; i++) dead[i] = mine.get(i); } }
        return dead != null ? "DEADLOCK detected, " + dead.length + " threads blocked forever (T1 holds A wants B; T2 holds B wants A)"
                : "completed, balances A=" + a.balance + " B=" + b.balance + " (total " + (a.balance + b.balance) + " preserved)";
    }
    public static void main(String[] args) throws Exception {
        System.out.println("unsafe transfers (A->B and B->A at the same time): " + attempt(false));
        System.out.println("lock-ordering transfers                           : " + attempt(true));
        ReentrantLock l1 = new ReentrantLock(), l2 = new ReentrantLock();
        Thread other = new Thread(() -> { l2.lock(); pause(150); try { l1.tryLock(200, TimeUnit.MILLISECONDS); } catch (InterruptedException e) { } }); other.setDaemon(true); other.start(); pause(20);
        System.out.println("tryLock(timeout) version gives up instead of blocking forever: acquired both = " + tryLockVersion(l1, l2));
        System.out.println("Four conditions present in the unsafe version: mutual exclusion (synchronized), hold-and-wait (holds 'from' while waiting for 'to'), no pre-emption (locks cannot be taken away), circular wait (T1->B, T2->A).");
        System.out.println("Lock ordering removes CIRCULAR WAIT: every thread acquires in increasing id order, so a cycle cannot form.");
    }
}
