package wtf.harvest.btcops;

import com.jcabi.jdbc.JdbcSession;
import com.jcabi.jdbc.ListOutcome;
import com.jcabi.jdbc.Outcome;
import com.jcabi.jdbc.SingleOutcome;
import com.jcabi.log.Logger;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.sql.SQLException;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import javax.sql.DataSource;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;
import org.bitcoinj.core.Address;
import org.bitcoinj.core.BlockChain;
import org.bitcoinj.core.Coin;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.PeerGroup;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.net.discovery.DnsDiscovery;
import org.bitcoinj.params.TestNet3Params;
import org.bitcoinj.store.BlockStore;
import org.bitcoinj.store.SPVBlockStore;
import org.bitcoinj.wallet.Wallet;
import org.bitcoinj.wallet.listeners.WalletCoinsReceivedEventListener;
import org.cactoos.crypto.digest.DigestFrom;
import org.cactoos.crypto.digest.DigestOf;
import org.cactoos.io.BytesOf;
import org.cactoos.io.InputOf;
import org.cactoos.io.LengthOf;
import org.cactoos.io.OutputTo;
import org.cactoos.io.ResourceOf;
import org.cactoos.io.TeeInput;
import org.cactoos.io.UncheckedBytes;
import org.cactoos.list.ListOf;
import wtf.harvest.db.PooledDataSource;

/**
 * Bot entry point.
 *
 * @since 1.0
 */
public final class Bot {

    private final Bot.Params params;
    private final OkHttpClient http;

    Bot(final Bot.Params params) {
        this.params = params;
        this.http = new OkHttpClient.Builder()
            .addInterceptor(
                new HttpLoggingInterceptor(
                    message -> Logger.info("HTTP", message)
                ).setLevel(HttpLoggingInterceptor.Level.BODY)
            ).build();
    }

    private void loop() throws Exception {
        final NetworkParameters net = TestNet3Params.get();
        final BlockStore store = new SPVBlockStore(net, this.blockchain());
        final BlockChain chain = new BlockChain(net, store);
        final PeerGroup peers = new PeerGroup(net, chain);
        peers.addPeerDiscovery(new DnsDiscovery.DnsSeedDiscovery(net, "testnet-seed.bitcoin.jonasschnelli.ch"));
        peers.start();
        peers.downloadBlockChain();
        final DataSource data = new PooledDataSource(this.params.env()).value();
        final WalletCoinsReceivedEventListener receiver = new WalletCoinsReceivedEventListener() {
            @Override
            public void onCoinsReceived(final Wallet wallet, final Transaction tx,
                final Coin prevBalance, final Coin newBalance) {
                final long oid = Long.parseLong(wallet.getDescription());
                final BigDecimal amount =
                    new BigDecimal(newBalance.toString()).divide(new BigDecimal(Coin.COIN.toString()));
                try {
                    Bot.this.acceptOperation(tx, data, oid, amount);
                } catch (final SQLException err) {
                    Logger.error(this, "Failed to accept operation: %[exception]s", err);
                }
            }
        };
        while (!Thread.currentThread().isInterrupted()) {
            final Iterable<Wallet> wallets = this.check(net, data);
            for (final Wallet wallet : wallets) {
                wallet.addCoinsReceivedEventListener(receiver);
                peers.addWallet(wallet);
                chain.addWallet(wallet);
            }
            try {
                Thread.sleep(1000L);
            } catch (final InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private void acceptOperation(final Transaction tx,
        final DataSource data, final long oid, final BigDecimal amount) throws SQLException {
        final JdbcSession session = new JdbcSession(data).autocommit(false);
        final Long author = session.sql("SELECT author FROM ioop_requests WHERE id = ?").set(oid).select(new SingleOutcome<>(Long.class));
        session.sql("INSERT INTO ledger (src, dst, amount, details, token) VALUES (?, ?, ?,?, ?)")
            .prepare(
                stmt -> {
                    stmt.setString(1, "s1input");
                    stmt.setString(2, Bot.userAddress(author));
                    stmt.setBigDecimal(3, amount);
                    stmt.setString(4, String.format("Receiving BTC operation=%d tx=%s (by btcops bot)", oid, tx.getHash().toString()));
                    stmt.setString(5, "BTC");
                }
            ).insert(Outcome.VOID);
        session.sql("UPDATE ioop_requests SET status = 'completed'::ioop_status WHERE id = ?")
            .set(oid)
            .update(Outcome.VOID);
        session.commit();
        Logger.info(this, "Received BTC: %s", tx);
    }

    private static String userAddress(final long uid) {
        final ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
        buffer.putLong(uid);
        return String.format(
            "u1%s",
            Base64.getEncoder().encodeToString(
                new UncheckedBytes(
                    new DigestOf(
                        new InputOf(new BytesOf(buffer.array())),
                        new DigestFrom("SHA-256")
                    )
                ).asBytes()
            )
        );
    }

    private Iterable<Wallet> check(final NetworkParameters net,
        final DataSource data) throws SQLException {
        final JdbcSession session = new JdbcSession(data).autocommit(false);
        final ListOf<Wallet> wallets = new ListOf<>(
            session
                .sql("SELECT ops.id, ops.amount, links.ref AS telegram FROM ioop_requests AS ops JOIN links ON ops.author = links.profile_id WHERE ops.name = 'deposit' AND ops.token = 'BTC' AND ops.status = 'pending' AND links.rel = 'telegram'")
                .select(
                    new ListOutcome<>(
                        rset -> {
                            final long oid = rset.getLong(1);
                            final BigDecimal amt = rset.getBigDecimal(2);
                            final long tid = rset.getLong(3);
                            final Wallet wallet = this.wallet(session, net, oid);
                            wallet.setDescription(Long.toString(oid));
                            session.sql("UPDATE ioop_requests SET status = 'assigned'::ioop_status WHERE id = ?")
                                .set(oid)
                                .update(Outcome.VOID);
                            try {
                                this.notifyTelegram(tid, amt, wallet.currentReceiveAddress());
                            } catch (final IOException err) {
                                throw new SQLException("Failed to notify", err);
                            }
                            return wallet;
                        }
                    )
                )
        );
        session.commit();
        return wallets;
    }

    private void notifyTelegram(final long tid, final BigDecimal amount, final Address address) throws IOException {
        final Response resp = this.http.newCall(
            new Request.Builder()
                .url(
                    new HttpUrl.Builder()
                        .host(this.params.env("HV_TELEGRAM_HOST"))
                        .encodedPath(String.format("/notifications/%d", tid))
                        .build()
                ).build()
        ).execute();
        if (!resp.isSuccessful()) {
            throw new IOException(String.format("Failed to notify the user: %d", resp.code()));
        }
        Logger.debug(this, "Send me %s btc to %s bitch", address, amount);
    }

    private Wallet wallet(final JdbcSession session, final NetworkParameters net,
        final long oid) throws SQLException {
        final ECKey keys = new ECKey();
        final Wallet wallet = Wallet.fromKeys(net, Collections.singletonList(keys));
        session.sql("INSERT INTO btcops (oid, priv, pub) VALUES (?, ?, ?)")
            .set(oid)
            .set(keys.getPrivateKeyAsWiF(net))
            .set(keys.getPublicKeyAsHex())
            .insert(Outcome.VOID);
        return wallet;
    }

    private File blockchain() throws IOException {
        final File out = File.createTempFile("spv_", ".chain");
        if (out.exists()) {
            out.delete();
        }
        out.deleteOnExit();
        Logger.info(
            this,
            "Extracted SPV blockchain (%d bytes)",
            new LengthOf(
                new TeeInput(
                    new ResourceOf("spvbstore.blockchain"),
                    new OutputTo(out)
                )
            ).longValue()
        );
        return out;
    }

    public static void main(String[] args) throws Exception {
        new Bot(new Bot.Params(new ListOf<>(args))).loop();
    }

    private static final class Params {
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
        private final List<String> args;

        Params(final List<String> args) {
            this(System.getenv(), System.getProperties(), args);
        }

        Params(final Map<String, String> envs,
            final Properties props, final List<String> args) {
            this.envs = Collections.unmodifiableMap(envs);
            this.props = props;
            this.args = Collections.unmodifiableList(args);
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
         * @param pos Position
         * @return Argument value
         */
        public String arg(final int pos) {
            return this.args.get(pos);
        }
    }
}
