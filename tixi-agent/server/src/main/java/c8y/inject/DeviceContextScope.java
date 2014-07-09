package c8y.inject;

import static com.cumulocity.agent.server.context.DeviceContextScope.CONTEXT_SCOPE;
import static org.springframework.context.annotation.ScopedProxyMode.TARGET_CLASS;

import java.lang.annotation.*;

import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;

@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Scope(value = CONTEXT_SCOPE, proxyMode = TARGET_CLASS)
public @interface DeviceContextScope {
    String value() default "";
}
