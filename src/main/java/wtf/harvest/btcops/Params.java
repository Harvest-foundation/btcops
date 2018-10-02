package wtf.harvest.btcops;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 *
 * @since
 */
final class Params {
    /**
     * Environment.
     */
    private final Map<String, String> envs;

    /**
     * Properties.
     */
    private final Properties props;

    /**
     * Arguments.
     */
    private final Arguments args;

    Params(final List<String> args) {
        this(System.getenv(), System.getProperties(), args);
    }

    Params(final Map<String, String> envs,
        final Properties props, final List<String> args) {
        this.envs = Collections.unmodifiableMap(envs);
        this.props = props;
        this.args = new Arguments(Collections.unmodifiableList(args));
    }

    /**
     * Get environment.
     *
     * @param name Variable name
     * @return Variable value
     */
    public String env(final String name) {
        return this.envs.get(name);
    }

    public Map<String, String> env() {
        return this.envs;
    }

    /**
     * Argument.
     *
     * @return Argument value
     */
    public Arguments arg() {
        return this.args;
    }
}
