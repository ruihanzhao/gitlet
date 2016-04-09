package gitlet;
import java.io.Serializable;
import java.util.Date;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.TreeMap;
/** A commit class that represents a
 * commit node.
 * @author Ruihan Zhao and Emily Pedersen. */
public class Commit implements Serializable {
    /** A commit's message. */
    private String _message;
    /** A commit's parentHash. */
    private String _parentHash;
    /** Map associating file name to its corresponding SHA1. */
    private TreeMap<String, String> _tracking = new TreeMap<String, String>();
    /** A string representation of a Commit's date. */
    private String _date;
    /** The date format. */
    private DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    /** The date of a commit. */
    private Date _dateObj;
    /** The commit node that takes in a MESSAGE,
     * PARENTHASH, treemap of TRACKING files,
     * the DATE, and DATEOBJ. */
    public Commit(String message, String parentHash, TreeMap<String,
              String> tracking, String date, Date dateObj) {
        _message = message;
        _parentHash = parentHash;
        _tracking = tracking;
        _date = date;
        _dateObj = dateObj;
    }

    /** Returns hashCode of this commit Node. */
    String hashName() {
        String result = "";
        for (String key : _tracking.keySet()) {
            result += key;
        }
        String hash = _message + _parentHash + _date + result;
        return Utils.sha1(hash);
    }
    /** Returns this commit's message. */
    String getMessage() {
        return _message;
    }
    /** Return this commit's parent hash. */
    String getparentHash() {
        return _parentHash;
    }
    /**Returns this commit's hashmap of tracked files. */
    TreeMap<String, String> getTracking() {
        return _tracking;
    }
    /** Return this commit's date. */
    String getDate() {
        return _date;
    }
    /** Return this commit's date object.*/
    Date getDateobj() {
        return _dateObj;
    }
}
