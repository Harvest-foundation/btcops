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

import com.jcabi.jdbc.JdbcSession;
import com.jcabi.jdbc.ListOutcome;
import com.jcabi.log.Logger;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;
import javax.sql.DataSource;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import org.bitcoinj.core.Address;
import org.bitcoinj.core.BlockChain;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.PeerGroup;
import org.bitcoinj.crypto.DeterministicKey;
import org.bitcoinj.net.discovery.DnsDiscovery;
import org.bitcoinj.params.TestNet3Params;
import org.bitcoinj.store.BlockStore;
import org.bitcoinj.store.SPVBlockStore;
import org.bitcoinj.wallet.UnreadableWalletException;
import org.bitcoinj.wallet.Wallet;
import org.cactoos.io.LengthOf;
import org.cactoos.io.OutputTo;
import org.cactoos.io.ResourceOf;
import org.cactoos.io.TeeInput;
import org.cactoos.list.ListOf;
import wtf.harvest.db.PooledDataSource;

/**
 * Bot entry point.
 *
 * @since 1.0
 * @todo #2:30min Reduce this class' DataAbstractionCoupling. It is currently
 *  at 21, which is way more than the max allowed of 7.
 * @todo #3:30min Use command line arguments `discovery` and `net` to configure
 *  bot on start up. For more details see #3.
 * @checkstyle ClassDataAbstractionCoupling (2 lines)
 */
public final class Bot {

    /**
     * Data folder.
     */
    private final File data;

    /**
     * Wallet file.
     */
    private final File wfile;

    /**
     * Bot parameters.
     */
    private final Params params;

    /**
     * HTTP client.
     */
    private final OkHttpClient http;

    /**
     * Ctor.
     * @param params Bot parameters
     * @param data Data directory
     * @param wfile Wallet file
     */
    Bot(final Params params, final File data, final File wfile) {
        this.params = params;
        this.http = new OkHttpClient.Builder()
            .addInterceptor(
                new HttpLoggingInterceptor(
                    message -> Logger.info("HTTP", message)
                ).setLevel(HttpLoggingInterceptor.Level.BODY)
            ).build();
        this.data = data;
        this.wfile = wfile;
    }

    /**
     * Primary ctor.
     *
     * @param params Bot parameters.
     */
    Bot(final Params params) {
        this(
            params,
            new File(params.arg().data()),
            new File(params.arg().data(), "btcops.wlt")
        );
    }

    /**
     * Bot entrypoint.
     * @param args Bot parameters
     * @throws Exception If an error occurs
     */
    @SuppressWarnings("PMD.ProhibitPublicStaticMethods")
    public static void main(final String... args) throws Exception {
        new Bot(new Params(new ListOf<>(args))).run();
    }

    /**
     * Wallet from the network.
     * @param net Network
     * @return Wallet
     * @throws IOException If something goes wrong
     */
    private Wallet walletFrom(final NetworkParameters net) throws IOException {
        if (!this.wfile.exists() || this.wfile.isDirectory()) {
            this.wfile.delete();
            Wallet.fromWatchingKey(
                net,
                DeterministicKey.deserializeB58(
                    this.params.env("BTC_DPUB"),
                    net
                )
            ).saveToFile(this.wfile);
        }
        try {
            return Wallet.loadFromFile(this.wfile);
        } catch (final UnreadableWalletException err) {
            throw new IOException("Failed to read wfile", err);
        }
    }

    /**
     * Run this bot.
     * @throws Exception If something goes wrong
     */
    private void run() throws Exception {
        final NetworkParameters net = TestNet3Params.get();
        final BlockStore store = new SPVBlockStore(net, this.blockchain());
        final BlockChain chain = new BlockChain(net, store);
        final PeerGroup peers = new PeerGroup(net, chain);
        peers.addPeerDiscovery(
            new DnsDiscovery.DnsSeedDiscovery(
                net,
                "testnet-seed.bitcoin.jonasschnelli.ch"
            )
        );
        final Wallet wlt = this.walletFrom(net);
        peers.addWallet(wlt);
        chain.addWallet(wlt);
        peers.start();
        peers.downloadBlockChain();
        final DataSource dbsource = new PooledDataSource(this.params.env())
            .value();
        this.checkInputs(dbsource, wlt);
        this.checkBalance(net, dbsource, wlt);
    }

    /**
     * Pre-validation checks.
     * @param dbsource Datasource
     * @param wallet Receiving wallet
     * @throws SQLException If error accessing database
     * @throws IOException If error accessing network
     */
    private void checkInputs(
        final DataSource dbsource,
        final Wallet wallet
    ) throws SQLException, IOException {
        final JdbcSession session = new JdbcSession(dbsource).autocommit(false);
        final List<Request> requests =
            session.sql(
                // @checkstyle LineLength (1 line)
                "SELECT ops.id, ops.amount, links.ref AS telegram FROM ioop_requests AS ops JOIN links ON ops.author = links.profile_id WHERE ops.name = 'deposit' AND ops.token = 'BTC' AND ops.status = 'pending' AND links.rel = 'telegram'"
            ).select(
                new ListOutcome<>(
                    rset -> new Request(
                        rset.getLong(1),
                        wallet.freshReceiveAddress(),
                        rset.getBigDecimal(2),
                        // @checkstyle MagicNumber (1 line)
                        rset.getLong(3)
                    )
                )
            );
        final String telegram = this.params.env("HV_TELEGRAM_HOST");
        for (final Request request : requests) {
            request.assign(session, wallet);
            request.notify(this.http, telegram);
        }
        wallet.saveToFile(this.wfile);
        session.commit();
    }

    /**
     * Checks wfile's balance.
     * @param net Network
     * @param dbsource Datasource
     * @param wallet Wallet
     * @throws SQLException If error accessing database
     */
    private void checkBalance(final NetworkParameters net,
        final DataSource dbsource, final Wallet wallet) throws SQLException {
        final JdbcSession session = new JdbcSession(dbsource).autocommit(false);
        final List<Balance> balances = session.sql(
            // @checkstyle LineLength (1 line)
            "SELECT request, address FROM ioop_btc_inputs WHERE status = 'waiting'"
        ).select(
            new ListOutcome<>(
                rset -> new Balance(
                    rset.getLong(1),
                    new BigDecimal(
                        wallet.getBalance(
                            new AddressBalance(
                                Address.fromBase58(net, rset.getString(2))
                            )
                        ).toPlainString()
                    )
                )
            )
        );
        for (final Balance balance : balances) {
            Logger.info(this, "Balance %s", balance);
            balance.accept(dbsource);
        }
    }

    /**
     * The blockchain file.
     * @return The blockchain file
     */
    private File blockchain() {
        final File chain = new File(this.data, "btcops.chain");
        if (!chain.exists() || chain.isDirectory()) {
            Logger.info(
                Bot.class,
                "extract: deleting existing chain: %b", chain.delete()
            );
            Logger.info(
                Bot.class,
                "extract: extracted SPV blockchain (%d bytes)",
                new LengthOf(
                    new TeeInput(
                        new ResourceOf("spvbstore.blockchain"),
                        new OutputTo(chain)
                    )
                ).longValue()
            );
        }
        Logger.info(
            Bot.class,
            "extract: chain file: %s", chain.getAbsolutePath()
        );
        return chain;
    }
}
