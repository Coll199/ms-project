package ms.passengeranalytics;

import com.google.firebase.database.IgnoreExtraProperties;

@IgnoreExtraProperties
public class User {

    public String username;
    public Long expirationTime;

    public User() {
        // Default constructor required for calls to DataSnapshot.getValue(User.class)
    }

    public User(String username, Long expirationTime) {
        this.username = username;
        this.expirationTime = expirationTime;
    }

}
