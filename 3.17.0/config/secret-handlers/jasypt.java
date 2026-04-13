///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS info.picocli:picocli:4.5.0
//DEPS org.jasypt:jasypt:1.9.3

import org.jasypt.encryption.pbe.StandardPBEStringEncryptor;
import org.jasypt.iv.RandomIvGenerator;
import org.jasypt.properties.PropertyValueEncryptionUtils;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.util.concurrent.Callable;
import java.util.logging.Logger;

@Command(name = "jasypt", mixinStandardHelpOptions = true)
class jasypt implements Callable<Integer> {
    @Option(names = {"-s", "--secret" }, description = "Secret", required = true)
    private String secret;
    @Option(names = {"-p", "--password" }, description = "Password", required = true)
    private String password;
    @Option(names = {"-a", "--algorithm" }, description = "Algorithm", defaultValue = "PBEWithHMACSHA512AndAES_256")
    private String algorithm;

    public static void main(String... args) {
        int exitCode = new CommandLine(new jasypt()).execute(args);
        System.exit(exitCode);
    }

    @Override
    public Integer call() {
        StandardPBEStringEncryptor encryptor = new StandardPBEStringEncryptor();
        encryptor.setPassword(password);
        encryptor.setAlgorithm(algorithm);
        encryptor.setIvGenerator(new RandomIvGenerator());
        encryptor.initialize();

        String encrypt = PropertyValueEncryptionUtils.encrypt(secret, encryptor);
        System.out.println("${jasypt::" + encrypt + "}");

        return 0;
    }
}
