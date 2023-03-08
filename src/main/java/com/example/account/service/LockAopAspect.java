package com.example.account.service;

import com.example.account.aop.AccountLockIdInterface;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
public class LockAopAspect {
    private final LockService lockService;

    // 잔액을 사용하거나 취소할 떄 동시성 이슈가 발생할 수 있기 때문에 이에 대한 lock을 걸어준다
    @Around("@annotation(com.example.account.aop.AccountLock) && args(request)")  // Around : 타켓에 대한 내용 수행 전, 후 를 감싸 어드바이스 내용이 주입, 수행된다.
    public Object aroundMethod(
            ProceedingJoinPoint pjp,
            AccountLockIdInterface request  // 인터페이스의 상속을 통해 UseBalance, CancelBalance 모두에서 공통의 자료형으로 가져올 수 있도록 조치
    ) throws Throwable {
        // lock 취득 시도
        lockService.lock(request.getAccountNumber());

        try {
            Thread.sleep(3000L);
            return pjp.proceed();
        } finally {  // try 절에서 예외가 발생하든 말든 finally로 lock을 해제시킨다
            // lock 해제
            lockService.unlock(request.getAccountNumber());
        }
    }

}
