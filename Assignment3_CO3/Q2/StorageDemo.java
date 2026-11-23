import java.io.IOException;

class ExamException extends Exception {
    ExamException(String message) { super(message); }
    ExamException(String message, Throwable cause) { super(message, cause); }
}
class SubmissionStorageException extends ExamException {
    SubmissionStorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
class Storage {
    void save(String fileName, long sizeKb) throws SubmissionStorageException {
        try {
            if (sizeKb > 500) throw new IOException("Disk quota exceeded");
            System.out.println("Saved " + fileName);
        } catch (IOException e) {
            // TODO 1: Wrap the IOException in a SubmissionStorageException, keeping the
            // IOException as its cause
            throw new SubmissionStorageException("Could not store " + fileName, e);
        }
    }
}
public class StorageDemo {
    public static void main(String[] args) {
        Storage storage = new Storage();
        try {
            storage.save("answer01.pdf", 120);
            storage.save("answer02.pdf", 800);
        } catch (ExamException e) {
            System.out.println("Error: " + e.getMessage());
            System.out.println("Cause: " + e.getCause().getMessage());
        }
    }
}

/** Extra: three-layer portal (controller -> service -> storage). Run: java ExamPortalDemo
 *  Hierarchy:  Exception <- ExamException <- { ExamClosedException, NotRegisteredException, FileTooLargeException } (checked: the caller can recover/inform)
 *                                          <- SubmissionStorageException (checked, wraps IOException)
 */
class ExamClosedException extends ExamException { ExamClosedException(String m) { super(m); } }
class NotRegisteredException extends ExamException { NotRegisteredException(String m) { super(m); } }
class FileTooLargeException extends ExamException { FileTooLargeException(String m) { super(m); } }
class SubmissionService {
    private final Storage storage = new Storage();
    void submit(String student, boolean windowOpen, boolean registered, String file, long kb) throws ExamException {
        if (!windowOpen) throw new ExamClosedException("Exam window closed");
        if (!registered) throw new NotRegisteredException(student + " is not registered for this course");
        if (kb > 5000) throw new FileTooLargeException("File " + kb + " KB exceeds the 5000 KB limit");
        storage.save(file, kb);                                      // may throw SubmissionStorageException
    }
}
class SubmissionController {
    private final SubmissionService service = new SubmissionService();
    String handle(String student, boolean open, boolean reg, String file, long kb) {
        try { service.submit(student, open, reg, file, kb); return "OK: submission accepted"; }
        catch (ExamClosedException | NotRegisteredException | FileTooLargeException e) { return "Student sees: " + e.getMessage(); }
        catch (SubmissionStorageException e) { return "Student sees: Your answer could not be saved, please try again later. (log: " + e + " | cause: " + e.getCause() + ")"; }
        catch (ExamException e) { return "Student sees: unexpected exam error"; }
    }
}
class ExamPortalDemo {
    public static void main(String[] args) {
        SubmissionController c = new SubmissionController();
        System.out.println("1 " + c.handle("Arun", false, true, "a.pdf", 100));
        System.out.println("2 " + c.handle("Divya", true, false, "b.pdf", 100));
        System.out.println("3 " + c.handle("Kiran", true, true, "c.pdf", 9000));
        System.out.println("4 " + c.handle("Meena", true, true, "d.pdf", 800));
        System.out.println("5 " + c.handle("Ravi", true, true, "e.pdf", 120));
        System.out.println("--- full chained stack trace (storage -> service -> controller) ---");
        try { new SubmissionService().submit("Meena", true, true, "d.pdf", 800); } catch (ExamException e) { e.printStackTrace(System.out); }
        System.out.println("--- same failure re-thrown WITHOUT the cause ---");
        try { try { new Storage().save("d.pdf", 800); } catch (ExamException e) { throw new ExamException("Could not store d.pdf"); } }
        catch (ExamException e) { System.out.println("getCause() = " + e.getCause() + "  -> 'Disk quota exceeded' and the original stack trace are LOST"); }
        StorageDemoNoCause.run();
    }
}
class StorageDemoNoCause {          // answers: what would the Cause line print if the IOException were not passed on?
    static void run() {
        ExamException e = new ExamException("Could not store answer02.pdf");
        System.out.println("Without cause: e.getCause() is null, so e.getCause().getMessage() throws NullPointerException");
        try { System.out.println(e.getCause().getMessage()); } catch (NullPointerException npe) { System.out.println("NullPointerException caught"); }
    }
}
