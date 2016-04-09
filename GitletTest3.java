package gitlet;

import static org.junit.Assert.*;

import org.junit.Test;

import ucb.junit.textui;

public class GitletTest3 {

    @Test
    public void testBranch() {
        Gitlet gitlet = new Gitlet();
        gitlet.init();
        gitlet.add("test1.txt");
        gitlet.add("test2.txt");
        gitlet.commit("Added test1 and 2");
        gitlet.log();
        gitlet.branch("awesomeBranch");
        gitlet.add("hug.txt");
        gitlet.commit("added hug");
        gitlet.checkout("awesomeBranch", 0);
        gitlet.log();
        gitlet.status();
        gitlet.removeBranch("awesomeBranch");
        assertEquals(2, gitlet.getBranches().size());
        gitlet.checkout("master", 0);
        gitlet.removeBranch("awesomeBranch");
        assertEquals(1, gitlet.getBranches().size());
    }
    public static void main(String[] args) {
        System.exit(textui.runClasses(GitletTest3.class));
    }

}
