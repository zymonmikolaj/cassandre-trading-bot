package tech.cassandre.trading.bot.util.parameters;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * API parameters from application.properties.
 */
@Validated
@ConfigurationProperties(prefix = "cassandre.trading.bot.api")
public class APIParameters {

    /** API enabled parameter. */
    public static final String PARAMETER_API_ENABLED = "cassandre.trading.bot.api.enabled";

    /** API username parameter. */
    public static final String PARAMETER_API_USERNAME = "cassandre.trading.bot.api.username";

    /** API password parameter. */
    public static final String PARAMETER_API_PASSWORD = "cassandre.trading.bot.api.password";

    /** API Enabled. */
    private Boolean enabled;

    /** API username. */
    private String username;

    /** API password. */
    private String password;

    /**
     * Getter enabled.
     *
     * @return enabled
     */
    public Boolean getEnabled() {
        return enabled;
    }

    /**
     * Setter enabled.
     *
     * @param newEnabled the enabled to set
     */
    public void setEnabled(final Boolean newEnabled) {
        enabled = newEnabled;
    }

    /**
     * Getter username.
     *
     * @return username
     */
    public String getUsername() {
        return username;
    }

    /**
     * Setter username.
     *
     * @param newUsername the username to set
     */
    public void setUsername(final String newUsername) {
        username = newUsername;
    }

    /**
     * Getter password.
     *
     * @return password
     */
    public String getPassword() {
        return password;
    }

    /**
     * Setter password.
     *
     * @param newPassword the password to set
     */
    public final void setPassword(final String newPassword) {
        password = newPassword;
    }

    @Override
    public final String toString() {
        return "APIParameters{"
                + " enabled=" + enabled
                + ", username='" + username + '\''
                + ", password='" + password + '\''
                + '}';
    }

}
