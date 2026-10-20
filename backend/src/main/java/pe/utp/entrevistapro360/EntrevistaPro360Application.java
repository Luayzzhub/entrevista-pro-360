package pe.utp.entrevistapro360;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;

@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
public class EntrevistaPro360Application {
    public static void main(String[] args) {
        SpringApplication.run(EntrevistaPro360Application.class, args);
    }
}
