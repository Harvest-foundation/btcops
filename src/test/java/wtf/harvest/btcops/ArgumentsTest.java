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
import org.bitcoinj.params.TestNet3Params;
import org.cactoos.list.ListOf;
import org.hamcrest.MatcherAssert;
import org.hamcrest.collection.IsIterableContainingInAnyOrder;
import org.hamcrest.core.IsEqual;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * Test for {@link Arguments}.
 *
 * @since 1.0
 * @checkstyle JavadocMethodCheck (500 lines)
 */
public final class ArgumentsTest {

    /**
     * Junit expected exception.
     */
    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void returnsCorrectNetValue() {
        final String net = "test3";
        MatcherAssert.assertThat(
            new Arguments(String.format("--net=%s", net)).net(),
            new IsEqual<>(TestNet3Params.get())
        );
    }

    @Test
    public void returnsCorrectDataValue() {
        final String data = "/var/btcops";
        MatcherAssert.assertThat(
            new Arguments(String.format("--data=%s", data)).data(),
            new IsEqual<>(new File(data))
        );
    }

    @Test
    public void returnsCorrectDiscoveriesList() {
        final String first = "discovery1";
        final String second = "discovery2";
        final String third = "discovery3";
        final String param = "--discovery=%s";
        MatcherAssert.assertThat(
            new Arguments(
                String.format(param, first),
                String.format(param, second),
                String.format(param, third)
            ).discovery(),
            new IsIterableContainingInAnyOrder<>(
                new ListOf<>(
                    new IsEqual<>(first),
                    new IsEqual<>(second),
                    new IsEqual<>(third)
                )
            )
        );
    }

    @Test
    public void throwsExceptionIfNetNotFound() {
        this.thrown.expect(IllegalStateException.class);
        this.thrown.expectMessage(
            "Required command line argument net is absent"
        );
        new Arguments().net();
    }

    @Test
    public void throwsExceptionIfDataNotFound() {
        this.thrown.expect(IllegalStateException.class);
        this.thrown.expectMessage(
            "Required command line argument data is absent"
        );
        new Arguments().data();
    }

    @Test
    public void throwsExceptionIfDiscoveryNotFound() {
        this.thrown.expect(IllegalStateException.class);
        this.thrown.expectMessage(
            "Required command line argument discovery is absent"
        );
        new Arguments().discovery();
    }
}
