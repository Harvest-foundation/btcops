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

import java.util.Collections;
import java.util.Map;

/**
 * Application parameters.
 * @since 2.0
 */
final class Params {
    /**
     * Environment.
     */
    private final Map<String, String> envs;

    /**
     * Arguments.
     */
    private final Arguments args;

    /**
     * Ctor.
     * @param args Command line arguments
     */
    Params(final String... args) {
        this(System.getenv(), args);
    }

    /**
     * Primary ctor.
     * @param envs Environment
     * @param args Command line arguments
     */
    Params(final Map<String, String> envs, final String... args) {
        this.envs = Collections.unmodifiableMap(envs);
        this.args = new Arguments(args);
    }

    /**
     * Get environment variable by name.
     *
     * @param name Variable name
     * @return Variable value
     */
    public String env(final String name) {
        return this.envs.get(name);
    }

    /**
     * Get environment map.
     *
     * @return Map with env params
     */
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
