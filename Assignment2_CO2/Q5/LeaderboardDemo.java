import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

class Team {
    final String name; final int points; final int goalDiff;
    Team(String name, int points, int goalDiff) {
        this.name = name; this.points = points; this.goalDiff = goalDiff;
    }
    @Override public String toString() {
        return name + "(" + points + "," + goalDiff + ")";
    }
}

class Leaderboard<T> {
    private final List<T> items = new ArrayList<>();
    private final Comparator<? super T> order;
    Leaderboard(Comparator<? super T> order) { this.order = order; }
    void add(T item) { items.add(item); }
    List<T> ranking() {
        // TODO 1: Return a new sorted copy of items using order, without modifying
        // items
        List<T> copy = new ArrayList<>(items);
        copy.sort(order);
        return copy;
    }
}

public class LeaderboardDemo {
    public static void main(String[] args) {
        // TODO 2: Build the comparator: points (high first), then goal difference (high
        // first), then name (A–Z)
        Comparator<Team> byRank = Comparator.comparingInt((Team t) -> t.points).reversed()
                .thenComparing(Comparator.comparingInt((Team t) -> t.goalDiff).reversed())
                .thenComparing(t -> t.name);
        Leaderboard<Team> board = new Leaderboard<>(byRank);
        board.add(new Team("Panthers", 9, 4));
        board.add(new Team("Eagles", 9, 6));
        board.add(new Team("Cobras", 6, 1));
        board.add(new Team("Bisons", 9, 4));
        System.out.println(board.ranking());
    }
}

/** Extra tests for Q5: tie-heavy data, a deliberately faulty comparator, wildcard + bounded type, Comparable and 3-sort alternatives. Run: java ComparatorTests */
class ComparatorTests {
    // bounded generic method: works for any T that can compare itself
    static <T extends Comparable<? super T>> T best(List<? extends T> list) { return Collections.max(list); }
    // wildcard: a List<? extends T> can be READ as T (a List<Team> is accepted for a Leaderboard<Team>)
    static <T> void addAll(Leaderboard<T> b, List<? extends T> src) { for (T t : src) b.add(t); }

    static boolean consistent(Comparator<Team> c, List<Team> data) {      // checks sgn(compare(a,b)) == -sgn(compare(b,a)) and transitivity
        for (Team a : data) for (Team b : data) {
            if (Integer.signum(c.compare(a, b)) != -Integer.signum(c.compare(b, a))) return false;
            for (Team x : data) if (c.compare(a, b) < 0 && c.compare(b, x) < 0 && c.compare(a, x) >= 0) return false;
        }
        return true;
    }
    public static void main(String[] args) {
        Comparator<Team> good = Comparator.comparingInt((Team t) -> t.points).reversed()
                .thenComparing(Comparator.comparingInt((Team t) -> t.goalDiff).reversed()).thenComparing(t -> t.name);
        List<Team> ties = List.of(new Team("Panthers", 9, 4), new Team("Eagles", 9, 6), new Team("Cobras", 6, 1), new Team("Bisons", 9, 4),
                new Team("Akulas", 6, 1), new Team("Dragons", 6, 3), new Team("Falcons", 3, 0), new Team("Gators", 3, 0));
        Leaderboard<Team> lb = new Leaderboard<>(good); addAll(lb, ties);
        System.out.println("Tie-heavy data, correct comparator : " + lb.ranking());

        Comparator<Team> subtraction = (a, b) -> b.points - a.points;                    // faulty: overflow
        List<Team> extreme = List.of(new Team("Low", Integer.MIN_VALUE, 0), new Team("High", 5, 0), new Team("Mid", -5, 0));
        Comparator<Team> noTie = Comparator.comparingInt((Team t) -> t.points).reversed();   // faulty: ignores tie-breakers
        System.out.println("Faulty comparator (subtraction) on extreme points: compare(Low,High) = " + subtraction.compare(extreme.get(0), extreme.get(1))
                + "  (should be > 0 because High ranks above Low)");
        System.out.println("  consistent? " + consistent(subtraction, extreme) + "   | correct comparator consistent? " + consistent(good, extreme));
        List<Team> a = new ArrayList<>(ties); a.sort(noTie);
        System.out.println("Faulty comparator (ignores tie-breakers) keeps input order among ties: " + a);

        // Alternative: three stable sorts, last key sorted LAST (name, then goalDiff, then points)
        List<Team> three = new ArrayList<>(ties);
        three.sort(Comparator.comparing((Team t) -> t.name));
        three.sort(Comparator.comparingInt((Team t) -> t.goalDiff).reversed());
        three.sort(Comparator.comparingInt((Team t) -> t.points).reversed());
        System.out.println("Three stable sorts gives same result as one chain : " + three.equals(lb.ranking()) + "  (3 x O(n log n) instead of 1)");
        System.out.println("Bounded type demo: best of {3,9,5} = " + best(List.of(3, 9, 5)) + ", best of {Ann,Zed,Bob} = " + best(List.of("Ann", "Zed", "Bob")));
        System.out.println("Why Bisons < Panthers: equal points (9) and goal difference (4), so the name tie-breaker decides: 'Bisons' < 'Panthers' alphabetically.");
    }
}
