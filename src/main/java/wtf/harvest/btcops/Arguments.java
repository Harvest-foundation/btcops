/*
 * Copyright (c) 2018 Harvest foundation
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to read
 * the Software only. Permissions is hereby NOT GRANTED to use, copy, modify,
 * merge, publish, distribute, sublicense, and/or sell copies of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package wtf.harvest.btcops;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Command line arguments.
 * @since 1.0
 */
public final class Arguments {

    /**
     * Net argument name.
     */
    private static final String P_NET = "net";

    /**
     * Data argument name.
     */
    private static final String P_DATA = "data";

    /**
     * Discovery argument name.
     */
    private static final String P_DISCOVERY = "discovery";

    /**
     * Exception description.
     */
    private static final String EXCEPTION =
        "Required command line argument %s is absent";

    /**
     * Map of arguments and their values.
     */
    private final Map<String, List<String>> args;

    /**
     * Constructs an {@code Arguments} with the specified arguments.
     * @param args Arguments
     */
    Arguments(final Iterable<String> args) {
        this.args = Arguments.asMap(args);
    }

    /**
     * Reruns value of the net argument.
     * @return Net argument
     * @throws IllegalStateException If net argument is missing
     */
    public String net() {
        if (!this.args.containsKey(Arguments.P_NET)) {
            throw new IllegalStateException(
                String.format(Arguments.EXCEPTION, Arguments.P_NET)
            );
        }
        return this.args.get(Arguments.P_NET).get(0);
    }

    /**
     * Reruns value of the net argument.
     * @return Net argument
     * @throws IllegalStateException If net argument is missing
     */
    public String data() {
        if (!this.args.containsKey(Arguments.P_DATA)) {
            throw new IllegalStateException(
                String.format(Arguments.EXCEPTION, Arguments.P_DATA)
            );
        }
        return this.args.get(Arguments.P_DATA).get(0);
    }

    /**
     * Reruns value of the discovery argument.
     * @return Net argument
     * @throws IllegalStateException If net argument is missing
     */
    public List<String> discovery() {
        if (!this.args.containsKey(Arguments.P_DISCOVERY)) {
            throw new IllegalStateException(
                String.format(Arguments.EXCEPTION, Arguments.P_DISCOVERY)
            );
        }
        return this.args.get(Arguments.P_DISCOVERY);
    }

    /**
     * Convert the provided arguments into a Map.
     * @param args Arguments to parse.
     * @return Map A map containing all the arguments and their values.
     * @throws IllegalStateException If an argument doesn't match with the
     *  expected format which is {@code --([a-z\-]+)(=.+)?}.
     */
    @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops")
    private static Map<String, List<String>> asMap(
        final Iterable<String> args) {
        final Map<String, List<String>> map = new HashMap<>(0);
        final Pattern ptn = Pattern.compile("--([a-z\\-]+)(=.+)?");
        for (final String arg : args) {
            final Matcher matcher = ptn.matcher(arg);
            if (!matcher.matches()) {
                throw new IllegalStateException(
                    String.format("can't parse this argument: '%s'", arg)
                );
            }
            final String value = matcher.group(2);
            map.computeIfAbsent(matcher.group(1), k -> new ArrayList<>(0));
            if (value != null) {
                map.get(matcher.group(1)).add(value.substring(1));
            }
        }
        return map;
    }
}

