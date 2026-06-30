import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import javax.xml.bind.DatatypeConverter;

/**
 * Minimal PKCS12 -> PEM converter so nginx can read a keytool-generated cert
 * without needing OpenSSL on the machine. Uses only the JDK (Java 7+).
 *
 * Usage: java CertToPem <p12file> <password> <alias> <outputDir>
 * Writes <outputDir>/fullchain.pem and <outputDir>/privkey.pem
 */
public class CertToPem {

    public static void main(String[] args) throws Exception {
        if (args.length < 4) {
            System.err.println("Usage: java CertToPem <p12> <password> <alias> <outDir>");
            System.exit(2);
        }
        String p12 = args[0];
        char[] pass = args[1].toCharArray();
        String alias = args[2];
        String outDir = args[3];

        KeyStore ks = KeyStore.getInstance("PKCS12");
        FileInputStream in = new FileInputStream(p12);
        try {
            ks.load(in, pass);
        } finally {
            in.close();
        }

        PrivateKey key = (PrivateKey) ks.getKey(alias, pass);
        Certificate[] chain = ks.getCertificateChain(alias);
        if (key == null || chain == null) {
            System.err.println("Alias '" + alias + "' not found or has no key/chain.");
            System.exit(1);
        }

        // privkey.pem (PKCS#8, unencrypted - what nginx expects)
        PrintWriter k = new PrintWriter(new FileWriter(new File(outDir, "privkey.pem")));
        try {
            k.print("-----BEGIN PRIVATE KEY-----\n");
            k.print(wrap(DatatypeConverter.printBase64Binary(key.getEncoded())));
            k.print("-----END PRIVATE KEY-----\n");
        } finally {
            k.close();
        }

        // fullchain.pem (leaf + any intermediates)
        PrintWriter c = new PrintWriter(new FileWriter(new File(outDir, "fullchain.pem")));
        try {
            for (Certificate cert : chain) {
                c.print("-----BEGIN CERTIFICATE-----\n");
                c.print(wrap(DatatypeConverter.printBase64Binary(cert.getEncoded())));
                c.print("-----END CERTIFICATE-----\n");
            }
        } finally {
            c.close();
        }

        System.out.println("PEM files written to " + outDir);
    }

    private static String wrap(String s) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < s.length(); i += 64) {
            b.append(s, i, Math.min(i + 64, s.length())).append('\n');
        }
        return b.toString();
    }
}
