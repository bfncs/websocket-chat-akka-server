package us.byteb.app.wschat.entity;

public class Login extends MessagePayload {

    // TODO: find a better way to provide type in serialization
    private String type = "Login";
    private String user;

    public Login() {
    }

    public Login(String user) {
        this.user = user;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }
}
