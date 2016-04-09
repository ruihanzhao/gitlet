package gitlet;

import static org.junit.Assert.*;

import org.junit.Test;

import ucb.junit.textui;

public class GitletTest {

    @Test
    public void addTest() {
        Gitlet gitlet = new Gitlet();
        gitlet.init();
        gitlet.add("test1.txt");
        assertEquals(1, gitlet.getStaged().size());
        for (String key: gitlet.getStaged().keySet()) {
            System.out.println("File: " + key);
            System.out.println("Sha1: " + gitlet.getStaged().get(key));
        }
        gitlet.add("fakeFile.txt");
    }

    @Test
    public void commitTest() {
        Gitlet gitlet = new Gitlet();
        gitlet.init();
        gitlet.add("test1.txt");
        assertEquals(1, gitlet.getStaged().size());
        gitlet.commit("Commiting test1.txt.");
        assertEquals(0, gitlet.getStaged().size());
        assertEquals(2, gitlet.getCommits().size());
        Commit curr = gitlet.getHead();
        System.out.println(curr.getMessage());
        String parentHash = curr.getparentHash();
        Commit initial = gitlet.getCommits().get(parentHash);
        System.out.println(initial.getparentHash());
        gitlet.commit("Commiting test2.txt");
        assertEquals(2, gitlet.getCommits().size());
        gitlet.add("test2.txt");
        gitlet.commit("Actually commiting test2.");
        assertEquals(3, gitlet.getCommits().size());
        gitlet.find("Actually commiting test2.");
        System.out.println(gitlet.getHead().hashName());
        gitlet.log();
        gitlet.globalLog();
    }

    @Test
    public void removeTest() {
        Gitlet gitlet = new Gitlet();
        gitlet.init();
        gitlet.add("test2.txt");
        assertEquals(1, gitlet.getStaged().size());
        gitlet.remove("test2.txt");
        assertEquals(0, gitlet.getStaged().size());
        gitlet.remove("test1.txt");
    }

    @Test
    public void findTest() {
        Gitlet gitlet = new Gitlet();
        gitlet.init();
        gitlet.add("test1.txt");
        gitlet.find("this shouldn't exist.");
        gitlet.commit("Message.");
        gitlet.add("test2.txt");
        gitlet.commit("Message.");
        gitlet.find("Message.");
    }

    public static void main(String[] args) {
        System.exit(textui.runClasses(GitletTest.class));
    }

}
