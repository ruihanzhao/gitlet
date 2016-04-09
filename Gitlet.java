package gitlet;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Date;

/** Our mini version control system.
@author Ruihan Zhao and Emily Pedersen. */
public class Gitlet implements Serializable {
    /** String representation of home directory. */
    private String homedir = ".gitlet";
    /** String representation of staging directory. */
    private String stagingdir = ".staging";
    /** String representation of blob directory. */
    private String blobdir = ".blobs";
    /** HashMap mapping branch names to the
     * corresponding head commit of that branch. */
    private HashMap<String, Commit> branches = new HashMap<String, Commit>();
    /** HashMap mapping file name to its corresponding SHA1. */
    private HashMap<String, String> stagedfiles = new HashMap<String, String>();
    /** HashMap of SHA1s of commits to corresponding commit nodes. */
    private HashMap<String, Commit> commits = new HashMap<String, Commit>();
    /** Treeset of names of removed files. */
    private TreeSet<String> removedFiles = new TreeSet<String>();
    /** Pointer to current commit. */
    private Commit head;
    /** Pointer to current branch. */
    private String currBranch;
    /** HashSet of removed files that have not been committed. */
    private TreeSet<String> rmNotCommitted = new TreeSet<String>();
    /** Return stagedFiles. */
    HashMap<String, String> getStaged() {
        return stagedfiles;
    }
    /** Return commits. */
    HashMap<String, Commit> getCommits() {
        return commits;
    }
    /** Return head. */
    Commit getHead() {
        return head;
    }
    /** Returns a list of branches. */
    HashMap<String, Commit> getBranches() {
        return branches;
    }
    /** A mini version control system! */
    public Gitlet() {
        DateFormat time = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date date = new Date();
        Commit initialCommit = new Commit("initial commit", "",
            new TreeMap<String, String>(), time.format(date), date);
        head = initialCommit;
        branches.put("master", head);
        commits.put(initialCommit.hashName(), initialCommit);
        currBranch = "master";
    }
    /** Returns a deserialized gitlet object from FILE. */
    Gitlet readPointer(File file) {
        Gitlet temp;
        try {
            ObjectInputStream in =
                new ObjectInputStream(new FileInputStream(file));
            temp = (Gitlet) in.readObject();
            in.close();
        } catch (IOException excp) {
            return null;
        } catch (ClassNotFoundException e) {
            return null;
        }
        return temp;
    }
    /** Serializes a gitlet OBJECT. */
    void writePointer(Object object) {
        File serFile = new File(".gitlet" + "/" + "serFile");
        try {
            ObjectOutputStream out =
                new ObjectOutputStream(new FileOutputStream(serFile));
            out.writeObject(object);
            out.close();
        } catch (IOException excp) {
            return;
        }

    }
    /** The init method of gitlet
     * that create a .gitlet, .staging,
     * and .blobs folder. */
    void init() {
        Path path = Paths.get(".gitlet");
        if (!Files.exists(path)) {
            File git = new File(".gitlet");
            git.mkdir();
            File staging = new File(git, ".staging");
            staging.mkdir();
            File blobs = new File(git, ".blobs");
            blobs.mkdir();
        } else {
            System.out.println("A gitlet version-control system already"
                + " exists in the current directory.");
        }
    }
    /** Add the file corresponding to FILENAME to the staging area. */
    void add(String fileName) {
        if (rmNotCommitted.contains(fileName)) {
            rmNotCommitted.remove(fileName);
        }
        if (removedFiles.contains(fileName)) {
            removedFiles.remove(fileName);
        }
        File file = new File(fileName);
        if (!file.exists()) {
            System.out.println("File does not exist.");
            return;
        }
        byte[] read = Utils.readContents(file);
        String fileSha = Utils.sha1(read);
        if (stagedfiles.containsKey(file.getName())) {
            if (!stagedfiles.containsValue(fileSha)) {
                File oldFile = new File(homedir + "/" + stagingdir
                    + "/" + stagedfiles.get(file.getName()));
                oldFile.delete();
                stagedfiles.put(file.getName(), fileSha);
                File newFile = new File(homedir + "/" + stagingdir
                    + "/" + fileSha);
                Utils.writeContents(newFile, read);
            } else {
                return;
            }
        }
        if (!stagedfiles.containsKey(file.getName())) {
            if (head.getTracking().containsKey(fileName)) {
                String headSha = head.getTracking().get(fileName);
                if (headSha.equals(fileSha)) {
                    return;
                }
            }
            File stagedFile = new File(homedir + "/" + stagingdir
                + "/" + fileSha);
            Utils.writeContents(stagedFile, read);
            stagedfiles.put(file.getName(), fileSha);
        }
    }
    /** Commit the files with this MESSAGE. */
    void commit(String message) {
        if (message.equals("")) {
            System.out.println("Please enter a commit message.");
            return;
        }
        if (stagedfiles.isEmpty() && rmNotCommitted.isEmpty()) {
            System.out.println("No changes added to the commit.");
            return;
        }
        String parentHash = head.hashName();
        TreeMap<String, String> trackingMap = new TreeMap<String, String>();
        trackingMap.putAll(stagedfiles);
        for (String key: head.getTracking().keySet()) {
            if (rmNotCommitted.contains(key)) {
                trackingMap.remove(key);
                continue;
            }
            if (!trackingMap.containsKey(key)) {
                trackingMap.put(key, head.getTracking().get(key));
            }
        }
        DateFormat time = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date date = new Date();
        Commit commit = new Commit(message, parentHash, trackingMap,
            time.format(date), date);
        String commitHash = commit.hashName();
        commits.put(commitHash, commit);
        head = commit;
        branches.put(currBranch, head);
        for (String key : stagedfiles.keySet()) {
            String sha = stagedfiles.get(key);
            File stagedFile = new File(homedir + "/" + stagingdir + "/" + sha);
            byte[] read = Utils.readContents(stagedFile);
            File blob = new File(homedir + "/" + blobdir + "/" + sha);
            Utils.writeContents(blob, read);
        }
        stagedfiles.clear();
        rmNotCommitted.clear();
    }
    /** Remove the file with this FILENAME. */
    void remove(String fileName) {
        if (!stagedfiles.containsKey(fileName)
            && !head.getTracking().containsKey(fileName)) {
            System.out.println("No reason to remove the file.");
            return;
        }
        if (stagedfiles.containsKey(fileName)) {
            stagedfiles.remove(fileName);
        }
        if (head.getTracking().containsKey(fileName)) {
            rmNotCommitted.add(fileName);
            removedFiles.add(fileName);
            Utils.restrictedDelete(fileName);
            return;
        }
    }
    /** The log message of all the commits on a certain branch. */
    void log() {
        Commit currHead = head;
        while (currHead != null) {
            System.out.println("===");
            System.out.println("Commit " + currHead.hashName());
            System.out.println(currHead.getDate());
            System.out.println(currHead.getMessage());
            System.out.println();
            currHead = commits.get(currHead.getparentHash());
        }
    }
    /** The log message of all the commits. */
    void globalLog() {
        for (Commit val : commits.values()) {
            System.out.println("===");
            System.out.println("Commit " + val.hashName());
            System.out.println(val.getDate());
            System.out.println(val.getMessage());
            System.out.println();
        }
    }
    /** Find the commit ID with this COMMITMES.*/
    void find(String commitMes) {
        boolean contained = false;
        for (Commit val : commits.values()) {
            if (val.getMessage().equals(commitMes)) {
                contained = true;
                System.out.println(val.hashName());
            }
        }
        if (!contained) {
            System.out.println("Found no commit with that message.");
        }
    }
    /** The status of files. */
    void status() {
        TreeSet<String> orderedBranches = new TreeSet<String>();
        TreeSet<String> orderedStaged = new TreeSet<String>();
        orderedBranches.addAll(branches.keySet());
        orderedStaged.addAll(stagedfiles.keySet());
        System.out.println("=== Branches ===");
        for (String key : orderedBranches) {
            if (key.equals(currBranch)) {
                System.out.println("*" + key);
            } else {
                System.out.println(key);
            }
        }
        System.out.println();
        System.out.println("=== Staged Files ===");
        for (String key : orderedStaged) {
            System.out.println(key);
        }
        System.out.println();
        System.out.println("=== Removed Files ===");
        for (String key : rmNotCommitted) {
            System.out.println(key);
        }
        System.out.println();
        System.out.println("=== Modifications Not Staged For Commit ===");
        TreeSet<String> modified = modifications();
        String currentDir = System.getProperty("user.dir");
        File directory = new File(currentDir);
        File[] workingDirFiles = directory.listFiles();
        for (String file : modified) {
            System.out.println(file);
        }
        System.out.println();
        TreeSet<String> untracked = new TreeSet<String>();
        System.out.println("=== Untracked Files ===");
        for (File file : workingDirFiles) {
            if (file.isDirectory()) {
                continue;
            }
            if (!head.getTracking().containsKey(file.getName())) {
                if (!stagedfiles.containsKey(file.getName())) {
                    untracked.add(file.getName());
                }
            }
        }
        for (String file : untracked) {
            System.out.println(file);
        }
    }
    /** Returns a list of modified files.*/
    TreeSet<String> modifications() {
        TreeSet<String> modified = new TreeSet<String>();
        String currentDir = System.getProperty("user.dir");
        File directory = new File(currentDir);
        ArrayList<String> fileNames = new ArrayList<String>();
        File[] workingDirFiles = directory.listFiles();
        for (File file : workingDirFiles) {
            if (file.isDirectory()) {
                continue;
            }
            fileNames.add(file.getName());
            byte[] read = Utils.readContents(file);
            String shaWorking = Utils.sha1(read);
            if (stagedfiles.containsKey(file.getName())) {
                String shaStaged = stagedfiles.get(file.getName());
                if (!shaStaged.equals(shaWorking)) {
                    modified.add(file.getName() + " (modified)");
                    continue;
                }
            } else if (head.getTracking().containsKey(file.getName())) {
                String headSha = head.getTracking().get(file.getName());
                if (!shaWorking.equals(headSha)) {
                    modified.add(file.getName() + " (modified)");
                }
            }
        }
        for (String file : stagedfiles.keySet()) {
            if (!fileNames.contains(file)) {
                modified.add(file + " (deleted)");
            }
        }
        for (String file : head.getTracking().keySet()) {
            if (!rmNotCommitted.contains(file)) {
                if (!fileNames.contains(file)) {
                    modified.add(file + " (deleted)");
                }
            }
        }
        return modified;
    }
    /** The checkout method that checks out a file
     * by this FILENAME. */
    void checkout(String fileName) {
        String currentDir = System.getProperty("user.dir");
        File work = new File(currentDir + "/" + fileName);
        if (!head.getTracking().containsKey(fileName)) {
            System.out.println("File does not exist in that commit.");
            return;
        }
        String sha = head.getTracking().get(fileName);
        File fileToBeCheckedOut = new File(homedir + "/"
            + blobdir + "/" + sha);
        byte[] read = Utils.readContents(fileToBeCheckedOut);
        Utils.writeContents(work, read);
    }
    /** The checkout method that checkouts out a file by
     * this FILENAME in the given commit by this COMMITID. */
    void checkout(String commitID, String fileName) {
        Commit checkoutCom = null;
        boolean exists = false;
        for (String key: commits.keySet()) {
            String subSHA1 = key.substring(0, 6);
            String subcommitID = commitID.substring(0, 6);
            if (subSHA1.equals(subcommitID)) {
                checkoutCom = commits.get(key);
                exists = true;
                break;
            }
        }
        if (!exists) {
            System.out.println("No commit with that id exists.");
            return;
        }
        if (checkoutCom != null) {
            if (!checkoutCom.getTracking().containsKey(fileName)) {
                System.out.println("File does not exist in that commit.");
                return;
            } else {
                String currentDir = System.getProperty("user.dir");
                File work = new File(currentDir + "/" + fileName);
                String sha = checkoutCom.getTracking().get(fileName);
                File fileToBeCheckedOut = new File(homedir + "/"
                    + blobdir + "/" + sha);
                byte[] read = Utils.readContents(fileToBeCheckedOut);
                Utils.writeContents(work, read);
            }
        }
    }
    /** The checkout method that checkouts the files
     * in this BRANCH with an arbitrary FAKENUMBER
     * to make the checkout signatures different.  */
    void checkout(String branch, int fakeNumber) {
        if (!branches.containsKey(branch)) {
            System.out.println("No such branch exists.");
            return;
        }
        if (branch.equals(currBranch)) {
            System.out.println("No need to checkout the current branch.");
            return;
        }
        String currentDir = System.getProperty("user.dir");
        File directory = new File(currentDir);
        File[] workingDirFiles = directory.listFiles();
        Commit newHead = branches.get(branch);
        for (File file : workingDirFiles) {
            if (file.isDirectory()) {
                continue;
            }
            byte[] read = Utils.readContents(file);
            String fileSha = Utils.sha1(read);
            if (newHead.getTracking().containsKey(file.getName())) {
                if (!head.getTracking().containsValue(fileSha)) {
                    System.out.println("There is an untracked file in the way;"
                        + " delete it or add it first.");
                    return;
                }
            }
        }
        for (String fileName : head.getTracking().keySet()) {
            if (!newHead.getTracking().containsKey(fileName)) {
                Utils.restrictedDelete(fileName);
            }
        }
        for (String fileName : newHead.getTracking().keySet()) {
            File work = new File(currentDir + "/" + fileName);
            String sha = newHead.getTracking().get(fileName);
            File fileToBeCheckedOut = new File(homedir + "/"
                + blobdir + "/" + sha);
            byte[] read = Utils.readContents(fileToBeCheckedOut);
            Utils.writeContents(work, read);
        }
        head = newHead;
        currBranch = branch;
        stagedfiles.clear();
    }
    /** Create a branch with this BRANCHNAME. */
    void branch(String branchName) {
        if (!branches.containsKey(branchName)) {
            branches.put(branchName, head);
        } else {
            System.out.println("A branch with that name already exists");
        }
    }
    /** Remove the branch with this BRANCHNAME. */
    void removeBranch(String branchName) {
        if (currBranch.equals(branchName)) {
            System.out.println("Cannot remove the current branch.");
        } else if (branches.containsKey(branchName)) {
            branches.remove(branchName);
        } else {
            System.out.println("A branch with that name does not exist.");
        }
    }
    /** Checkout out the files in the commit
     * corresponding to this COMMITID. */
    void reset(String commitID) {
        Commit checkoutCom = null;
        boolean exists = false;
        for (String key: commits.keySet()) {
            String subSHA1 = key.substring(0, 6);
            String subcommitID = commitID.substring(0, 6);
            if (subSHA1.equals(subcommitID)) {
                checkoutCom = commits.get(key);
                exists = true;
                break;
            }
        }
        if (!exists) {
            System.out.println("No commit with that id exists.");
            return;
        }
        String currentDir = System.getProperty("user.dir");
        File directory = new File(currentDir);
        File[] workingDirFiles = directory.listFiles();
        for (File file : workingDirFiles) {
            if (file.isDirectory()) {
                continue;
            }
            byte[] read = Utils.readContents(file);
            String fileSha = Utils.sha1(read);
            String f = file.getName();
            if (head.getTracking().containsKey(file.getName())) {
                if (head.getTracking().get(f).equals(fileSha)) {
                    if (!checkoutCom.getTracking().containsKey(f)) {
                        Utils.restrictedDelete(file);
                    }
                } else {
                    if (checkoutCom.getTracking().containsKey(f)) {
                        System.out.println("There is an untracked file "
                                + "in the way; delete it or add it first.");
                        return;
                    }
                }
            } else if (checkoutCom.getTracking().containsKey(f)) {
                System.out.println("There is an untracked file in the way;"
                    + " delete it or add it first.");
                return;
            }
        }
        if (checkoutCom != null) {
            for (String fileName : checkoutCom.getTracking().keySet()) {
                File work = new File(currentDir + "/" + fileName);
                String sha = checkoutCom.getTracking().get(fileName);
                File fileToBeCheckedOut = new File(homedir + "/" + blobdir
                    + "/" + sha);
                byte[] read = Utils.readContents(fileToBeCheckedOut);
                Utils.writeContents(work, read);
            }
        }
        branches.replace(currBranch, checkoutCom);
        head = checkoutCom;
        stagedfiles.clear();

    }
    /** Returns untracked files from GIVENCOMMIT and CURRCOMMIT. */
    void findUntracked(Commit givenCommit, Commit currCommit) {
        List<String> workingDirFiles = Utils.plainFilenamesIn(new File("."));
        for (String f : workingDirFiles) {
            File file = new File(f);
            if (file.isDirectory()) {
                continue;
            }
            byte[] read = Utils.readContents(file);
            String fileSha = Utils.sha1(read);
            if (givenCommit.getTracking().containsKey(f)) {
                if (!currCommit.getTracking().containsKey(f)) {
                    System.out.println("There is an untracked file in the way;"
                        + " delete it or add it first.");
                    System.exit(0);
                } else if (!currCommit.getTracking().get(f).equals(fileSha)) {
                    System.out.println("There is an untracked file in the way;"
                        + " delete it or add it first.");
                    System.exit(0);
                }
            }
        }
    }
    /** Returns if merge composed of CURRCOMMIT, GIVENCOMMIT, SPLITNODE
    with files in MODIFIED, UNMODIFIED, written to CURRENTDIR with
    return a conflict or not represented by MERGED. */
    boolean mergeHelper(Commit currCommit, Commit givenCommit,
              Commit splitNode, HashSet<String> modified,
              HashSet<String> unmodified, boolean merged) {
        for (String key: givenCommit.getTracking().keySet()) {
            if (splitNode.getTracking().containsValue
                      (givenCommit.getTracking().get(key))) {
                merged = true;
                continue;
            }
            if (unmodified.contains(key)) {
                if (!givenCommit.getTracking().get(key).equals
                          (currCommit.getTracking().get(key))) {
                    merged = true;
                    checkout(givenCommit.hashName(), key);
                    add(key);
                }
            } else if (!splitNode.getTracking().containsKey(key)
                    && !currCommit.getTracking().containsKey(key)) {
                merged = true;
                checkout(givenCommit.hashName(), key);
                add(key);
            }
        }
        for (String key : unmodified) {
            if (!givenCommit.getTracking().containsKey(key)) {
                merged = true;
                remove(key);
            }
        }
        return merged;
    }
    /** Merge current branch with BRANCHNAME. */
    void merge(String branchName) throws IOException {
        if (stagedfiles.size() > 0 || rmNotCommitted.size() > 0) {
            System.out.println("You have uncommitted changes.");
            return;
        }
        if (!branches.containsKey(branchName)) {
            System.out.println("A branch with that name does not exist.");
            return;
        }
        File currentDir = new File(".");
        Commit givenCommit = branches.get(branchName);
        Commit currCommit = branches.get(currBranch);
        findUntracked(givenCommit, currCommit);
        if (currBranch.equals(branchName)) {
            System.out.println("Cannot merge a branch with itself.");
            return;
        }
        if (currCommit.hashName().equals(givenCommit.hashName())) {
            System.out.println("No changes added to the commit.");
            return;
        }
        Commit splitNode = findSplitNode(currCommit, givenCommit);
        if (splitNode.hashName().equals(givenCommit.hashName())) {
            System.out.println("Given branch is an ancestor"
                + "of the current branch.");
            return;
        }
        if (splitNode.hashName().equals(currCommit.hashName())) {
            branches.replace(currBranch, givenCommit);
            checkout(branchName, 0);
            System.out.println("Current branch fast-forwarded.");
            return;
        }
        HashMap<String, HashSet<String>> modandUnmod =
                  getFiles(currCommit, givenCommit, splitNode);
        HashSet<String> unmodified = modandUnmod.get("unmodified");
        HashSet<String> modified = modandUnmod.get("modified");
        boolean merged = false;
        merged = mergeHelper(currCommit, givenCommit, splitNode,
                  modified, unmodified, merged);
        merged = mergeConflictHelper(currCommit, givenCommit, splitNode,
                modified, unmodified, merged, currentDir);
        if (merged) {
            commit("Merged " + currBranch + " with " + branchName + ".");
            return;
        } else {
            System.out.println("Encountered a merge conflict.");
        }
    }
    /** Returns if merge composed of CURRCOMMIT, GIVENCOMMIT, SPLITNODE
    with files in MODIFIED, UNMODIFIED, written to CURRENTDIR with
    return a conflict or not represented by MERGED. */
    boolean mergeConflictHelper(Commit currCommit, Commit givenCommit,
          Commit splitNode, HashSet<String> modified,
          HashSet<String> unmodified,
          boolean merged, File currentDir) throws IOException {
        for (String key : modified) {
            String sha1Curr = currCommit.getTracking().get(key);
            String sha1Given = givenCommit.getTracking().get(key);
            if (givenCommit.getTracking().containsKey(key)
                && currCommit.getTracking().containsKey(key)) {
                if (!sha1Curr.equals(sha1Given)) {
                    if (sha1Curr.equals(splitNode.getTracking().get(key))) {
                        continue;
                    } else if (sha1Given.equals
                              (splitNode.getTracking().get(key))) {
                        continue;
                    } else {
                        merged = false;
                        mergeConflict(sha1Curr, sha1Given, currentDir, key);
                    }
                }
            } else {
                if (splitNode.getTracking().containsKey(key)) {
                    merged = false;
                    mergeConflict(sha1Curr, sha1Given, currentDir, key);
                }
            }
        }
        return merged;
    }
    /** Return a HASHMAP that maps modified to a list of
    *  modified file and unmodified to a list of unmodified
    * files. This hashmap is composed of CURRCOMMIT, GIVENCOMMIT
    and SPLITNODE. */
    HashMap<String, HashSet<String>> getFiles(Commit currCommit,
        Commit givenCommit, Commit splitNode) {
        HashMap<String, HashSet<String>>
            allFiles = new HashMap<String, HashSet<String>>();
        HashSet<String> modified = new HashSet<String>();
        HashSet<String> unmodified = new HashSet<String>();
        for (String key : currCommit.getTracking().keySet()) {
            String sha = currCommit.getTracking().get(key);
            if (splitNode.getTracking().containsValue(sha)) {
                unmodified.add(key);
            } else {
                modified.add(key);
            }
        }
        for (String key : givenCommit.getTracking().keySet()) {
            String sha = givenCommit.getTracking().get(key);
            if (!splitNode.getTracking().containsValue(sha)) {
                modified.add(key);
            } else {
                unmodified.add(key);
            }
        }
        allFiles.put("modified", modified);
        allFiles.put("unmodified", unmodified);
        return allFiles;
    }
    /** Write to a file if there merge conflict given
     * the given the SHA1CURR of a file in the current commit,
     * SHA1GIVEN of a file in the given commit,
     * the CURRENTDIR, and fileName which is KEY. */
    void mergeConflict(String sha1Curr, String sha1Given, File currentDir,
        String key) throws IOException {
        File work = new File(currentDir + "/" + key);
        String h = "<<<<<<< HEAD\n";
        String equals = "=======\n";
        String end = ">>>>>>>\n";
        File fileCurrBranch = new File(homedir + "/" + blobdir
            + "/" + sha1Curr);
        File fileGivenBranch = new File(homedir + "/" + blobdir
             + "/" + sha1Given);
        byte[] readCurr;
        byte[] readGiven;
        String empty = "";
        if (sha1Curr == null) {
            readCurr = empty.getBytes();
            readGiven = Utils.readContents(fileGivenBranch);
        } else if (sha1Given == null) {
            readGiven = empty.getBytes();
            readCurr = Utils.readContents(fileCurrBranch);
        } else {
            readGiven = Utils.readContents(fileGivenBranch);
            readCurr = Utils.readContents(fileCurrBranch);
        }
        byte[] readHead = h.getBytes();
        byte[] readEquals = equals.getBytes();
        byte[] readEnd = end.getBytes();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        outputStream.write(readHead);
        outputStream.write(readCurr);
        outputStream.write(readEquals);
        outputStream.write(readGiven);
        outputStream.write(readEnd);
        byte[] finalResult = outputStream.toByteArray();
        Utils.writeContents(work, finalResult);
    }
    /** Returns the commit that is the split node given
     * the CURRCOMMIT and the GIVENCOMMIT. */
    Commit findSplitNode(Commit currCommit, Commit givenCommit) {
        Commit laterTime = null;
        Commit earlierTime = null;
        Commit splitNode = null;
        Date currComDate = currCommit.getDateobj();
        Date givenComDate = givenCommit.getDateobj();
        Integer compare = currComDate.compareTo(givenComDate);
        if (compare > 0) {
            laterTime = currCommit;
            earlierTime = givenCommit;
        } else {
            laterTime = givenCommit;
            earlierTime = currCommit;
        }
        while (laterTime != null) {
            String parentHashLater = laterTime.getparentHash();
            Commit parentLater = commits.get(parentHashLater);
            Date parentLaterDate = parentLater.getDateobj();
            Date earlierTimeDate = earlierTime.getDateobj();
            Integer compareDate = parentLaterDate.compareTo(earlierTimeDate);
            if (compareDate > 0) {
                laterTime = parentLater;
            } else if (compareDate < 0) {
                laterTime = earlierTime;
                earlierTime = parentLater;
            } else {
                laterTime = null;
                splitNode = parentLater;
            }
        }
        return splitNode;
    }
}
