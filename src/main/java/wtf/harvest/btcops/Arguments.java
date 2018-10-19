/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2018 Harvest foundation
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
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

import java.io.File;
import java.util.Arrays;
import java.util.List;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.params.MainNetParams;
import org.bitcoinj.params.TestNet3Params;

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
     * Web service port.
     */
    private static final String P_PORT = "port";

    /**
     * Command line arguments.
     */
    private final CommandLine args;

    /**
     * Constructs an {@code Arguments} with the specified arguments.
     * @param args Arguments
     */
    Arguments(final String... args) {
        this.args = Arguments.parse(args);
    }

    /**
     * Reruns value of the net argument.
     * @return Net argument
     * @throws IllegalStateException If net argument is missing
     */
    public NetworkParameters net() {
        this.check(Arguments.P_NET);
        final String net = this.args.getOptionValue(Arguments.P_NET);
        final NetworkParameters params;
        if ("test3".equals(net)) {
            params = TestNet3Params.get();
        } else if ("main".equals(net)) {
            params = MainNetParams.get();
        } else {
            throw new IllegalStateException(
                String.format("Unsupported net param: %s", net)
            );
        }
        return params;
    }

    /**
     * Reruns value of the net argument.
     * @return Net argument
     * @throws IllegalStateException If net argument is missing
     */
    public File data() {
        this.check(Arguments.P_DATA);
        return new File(this.args.getOptionValue(Arguments.P_DATA));
    }

    /**
     * Reruns value of the discovery argument.
     * @return Net argument
     * @throws IllegalStateException If net argument is missing
     */
    public List<String> discovery() {
        this.check(Arguments.P_DISCOVERY);
        return Arrays.asList(this.args.getOptionValues(Arguments.P_DISCOVERY));
    }

    /**
     * Web service port.
     *
     * @return Port number
     */
    public int port() {
        this.check(Arguments.P_PORT);
        return Integer.parseInt(this.args.getOptionValue(Arguments.P_PORT));
    }

    /**
     * Convert the provided arguments into commons.cli CommandLine object.
     * @param args Arguments to parse.
     * @return CommandLine object with parsed arguments.
     * @throws IllegalStateException If failed to parse arguments.
     */
    private static CommandLine parse(final String... args) {
        final Options options = new Options();
        //@checkstyle LineLengthCheck (3 lines)
        options.addOption("", Arguments.P_NET, true, "Net arg: can be either main or test3, if main then bot should use main network, test3 otherwise ");
        options.addOption("", Arguments.P_DATA, true, "Data arg: data directory");
        options.addOption("", Arguments.P_DISCOVERY, true, "Discovery args: host name (list) of peers discovery");
        options.addOption("", Arguments.P_PORT, true, "Web service port");
        final CommandLineParser parser = new DefaultParser();
        try {
            return parser.parse(options, args);
        } catch (final ParseException ex) {
            throw new IllegalStateException(
                "Failed to parse command line arguments", ex
            );
        }
    }

    /**
     * Checks if argument is present in {@link this.args}.
     * @param arg Argument to check
     * @throws IllegalStateException If arg is absent
     */
    private void check(final String arg) {
        if (!this.args.hasOption(arg)) {
            throw new IllegalStateException(
                String.format(
                    "Required command line argument %s is absent",
                    arg
                )
            );
        }
    }
}

