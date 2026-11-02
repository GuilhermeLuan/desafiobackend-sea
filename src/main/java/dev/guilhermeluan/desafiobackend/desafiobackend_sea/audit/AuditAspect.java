package dev.guilhermeluan.desafiobackend.desafiobackend_sea.audit;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.aop.support.AopUtils;
import org.springframework.context.expression.MethodBasedEvaluationContext;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.annotation.Order;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;


@Aspect
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@Slf4j
public class AuditAspect {

    private final AuditLogRepository auditLogRepository;

    public AuditAspect(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    private static String currentActor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication instanceof AnonymousAuthenticationToken) {
            return "anonymous";
        }

        return authentication.getName();
    }

    @Around("@annotation(dev.guilhermeluan.desafiobackend.desafiobackend_sea.audit.Audit)")
    public Object record(ProceedingJoinPoint joinPoint) throws Throwable {
        var method = ((MethodSignature) joinPoint.getSignature()).getMethod();
        Audit audit = AnnotationUtils.findAnnotation(AopUtils.getMostSpecificMethod(method, joinPoint.getTarget().getClass()), Audit.class);
        String actor = currentActor();


        Object result;

        try {
            result = joinPoint.proceed();

        } catch (Throwable e) {
            log.warn("Error occurred while executing audit: {}", e.getMessage());
            save(actor, audit, resourceId(joinPoint, method, audit, null), AuditOutcome.FAILURE, e.getClass().getSimpleName());
            throw e;
        }

        save(actor, audit, resourceId(joinPoint, method, audit, result), AuditOutcome.SUCCESS, null);

        return result;
    }

    private void save(String actor, Audit audit, Long resourceId, AuditOutcome outcome, String errorType) {
        try {
            auditLogRepository.save(new AuditLog(actor, audit.action(), resourceId, outcome, errorType));
        } catch (Exception e) {
            log.error("Failed to write audit log for {} ({})", audit.action(), outcome, e);
        }
    }

    private Long resourceId(ProceedingJoinPoint joinPoint, Method method, Audit audit, Object result) {
        if (audit.resourceId().isBlank()) {
            return null;
        }

        try {
            var context = new MethodBasedEvaluationContext(
                    joinPoint.getTarget(), method, joinPoint.getArgs(), new DefaultParameterNameDiscoverer()
            );
            context.setVariable("result", result);

            Number value = new SpelExpressionParser().parseExpression(audit.resourceId()).getValue(context, Number.class);

            return value == null ? null : value.longValue();

        } catch (Exception e) {
            log.warn("Cloud not evaluate audit resourceId: {}", e.getMessage());
            return null;
        }
    }
}
