package gitlet;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashSet;

/** Driver class for Gitlet, the tiny stupid version-control system.
 *  @author Ruihan Zhao and Emily Pedersen.
 */
public class Main {
    /** Returns hashset of all the command names. */
    static HashSet<String> getCommandNames() {
        HashSet<String> commandNames = new HashSet<String>();
        commandNames.add("init");
        commandNames.add("add");
        commandNames.add("rm");
        commandNames.add("checkout");
        commandNames.add("rm-branch");
        commandNames.add("branch");
        commandNames.add("reset");
        commandNames.add("log");
        commandNames.add("global-log");
        commandNames.add("status");
        commandNames.add("merge");
        commandNames.add("commit");
        commandNames.add("find");
        return commandNames;
    }

    /** Returns a deserialized gitlet object from
    FILENAME. */
    static Object readPointer(String fileName) {
        Object temp;
        File inFile = new File(fileName);
        try {
            ObjectInputStream in =
                new ObjectInputStream(new FileInputStream(inFile));
            temp = in.readObject();
            in.close();
        } catch (IOException excp) {
            return null;
        } catch (ClassNotFoundException e) {
            return null;
        }
        return temp;
    }
    /** Returns a serialized gitlet OBJECT. */
    static void writePointer(Object object) {
        File serFile = new File(".gitlet/serFile");
        try {
            ObjectOutputStream out =
                new ObjectOutputStream(new FileOutputStream(serFile));
            out.writeObject(object);
            out.close();
        } catch (IOException excp) {
            return;
        }
    }

    /** Usage: java gitlet.Main ARGS, where ARGS contains
     *  <COMMAND> <OPERAND> .... */
    private static Gitlet gitlet;
    /** This is our main method given ARGS. */
    public static void main(String... args) throws IOException {
        HashSet<String> commands = getCommandNames();
        if (args.length == 0) {
            System.out.println("Please enter a command");
        } else if (args[0].equals("init")) {
            if (args.length > 1) {
                incorrectOperands();
            }
            gitlet = new Gitlet();
            gitlet.init();
            writePointer(gitlet);
            return;
        } else if (!Files.exists(Paths.get(".gitlet"))) {
            System.out.println("Not in an initialized gitlet directory.");
            return;
        }
        gitlet = (Gitlet) readPointer(".gitlet/serFile");
        if (args[0].equals("add")) {
            if (args.length > 2) {
                incorrectOperands();
            }
            gitlet.add(args[1]);
            writePointer(gitlet);
        } else if (args[0].equals("commit")) {
            if (args.length > 2) {
                incorrectOperands();
            }
            try {
                gitlet.commit(args[1]);
            } catch (IndexOutOfBoundsException e) {
                System.out.println("Please enter a commit message.");
            }
        } else if (args[0].equals("rm")) {
            if (args.length > 2) {
                incorrectOperands();
            }
            gitlet.remove(args[1]);
        } else if (args[0].equals("log")) {
            if (args.length > 1) {
                incorrectOperands();
            }
            gitlet.log();
        } else if (args[0].equals("global-log")) {
            if (args.length > 1) {
                incorrectOperands();
            }
            gitlet.globalLog();
        }
        mainHelper(args);
        if (!commands.contains(args[0])) {
            System.out.println("No command with that name exists.");
            return;
        }
        gitlet.writePointer(gitlet);
    }
    /** Returns incorrect operands. */
    static void incorrectOperands() {
        System.out.println("Incorrect operands.");
        System.exit(0);
    }
    /** Helps shorten the main method using ARGS. */
    static void mainHelper(String[] args) throws IOException {
        if (args[0].equals("find")) {
            if (args.length > 2) {
                incorrectOperands();
            }
            gitlet.find(args[1]);
        } else if (args[0].equals("status")) {
            if (args.length > 1) {
                incorrectOperands();
            }
            gitlet.status();
        } else if (args[0].equals("checkout")) {
            mainCheckout(args);
        } else if (args[0].equals("branch")) {
            if (args.length > 2) {
                incorrectOperands();
            }
            gitlet.branch(args[1]);
        } else if (args[0].equals("rm-branch")) {
            if (args.length > 2) {
                incorrectOperands();
            }
            gitlet.removeBranch(args[1]);
        } else if (args[0].equals("reset")) {
            if (args.length > 2) {
                incorrectOperands();
            }
            gitlet.reset(args[1]);
        } else if (args[0].equals("merge")) {
            if (args.length > 2) {
                incorrectOperands();
            }
            gitlet.merge(args[1]);
        }
    }
    /** Does checkout so main has less lines using ARGS. */
    static void mainCheckout(String[] args) {
        if (args[1] instanceof String && args.length == 2) {
            gitlet.checkout(args[1], 0);
        } else if (args[1].equals("--") && args.length == 3) {
            gitlet.checkout(args[2]);
        } else if (args[2].equals("--") && args.length == 4) {
            gitlet.checkout(args[1], args[3]);
        } else {
            incorrectOperands();
        }
    }
}
