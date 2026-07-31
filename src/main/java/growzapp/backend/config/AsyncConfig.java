package growzapp.backend.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;

import java.lang.reflect.Method;

/**
 * Sans AsyncUncaughtExceptionHandler, toute exception levée dans une
 * méthode @Async (envoi d'email, notifications...) disparaît
 * silencieusement — aucune trace en logs, impossible de savoir qu'un
 * email n'est jamais parti (JAVA-06 de l'audit).
 */
@Slf4j
@Configuration
@EnableAsync
public class AsyncConfig implements AsyncConfigurer {

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (Throwable throwable, Method method, Object... params) -> {
            log.error("Exception non gérée dans une méthode asynchrone : {} — paramètres : {}",
                    method.getName(), params, throwable);
        };
    }
}