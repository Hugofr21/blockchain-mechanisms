package org.graph.server;

import java.io.Console;

public class SecurityBootstrapper {
    public static char[] obtainNodePassword() {
        String envPassword = System.getenv("VAULT_SECRET_PASS");
        if (envPassword != null && !envPassword.trim().isEmpty()) {
            System.out.println("[SECURITY] Password obtained via the Vault environment variable.");
            return envPassword.toCharArray();
        }

        Console console = System.console();
        if (console != null) {
            char[] password = console.readPassword("Enter the Node Vault/Keystore password: ");
            if (password != null && password.length > 0) {
                return password;
            }
        }

        throw new SecurityException("Catastrophic failure: Unable to obtain startup credentials. Startup aborted.");
    }
}