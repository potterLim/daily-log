package com.potterlim.daylog.service;

import com.potterlim.daylog.config.DayLogApplicationProperties;
import com.potterlim.daylog.entity.UserAccount;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnBean(JavaMailSender.class)
public class SmtpAuthenticationMailService implements IAuthenticationMailService {

    private final JavaMailSender mJavaMailSender;
    private final DayLogApplicationProperties mDayLogApplicationProperties;

    public SmtpAuthenticationMailService(
        JavaMailSender javaMailSender,
        DayLogApplicationProperties dayLogApplicationProperties
    ) {
        mJavaMailSender = javaMailSender;
        mDayLogApplicationProperties = dayLogApplicationProperties;
    }

    @Override
    public void sendEmailVerificationMail(UserAccount userAccount, String verificationUrl) {
        if (userAccount == null) {
            throw new IllegalArgumentException("userAccount must not be null.");
        }

        if (verificationUrl == null || verificationUrl.isBlank()) {
            throw new IllegalArgumentException("verificationUrl must not be blank.");
        }

        SimpleMailMessage simpleMailMessage = new SimpleMailMessage();
        simpleMailMessage.setTo(userAccount.getEmailAddress());
        simpleMailMessage.setFrom(mDayLogApplicationProperties.getMail().getFromAddress());
        simpleMailMessage.setSubject("Daymark 이메일 인증 안내");
        simpleMailMessage.setText("""
            안녕하세요, %s님.

            아래 링크를 열면 Daymark 계정의 이메일 소유 확인이 완료됩니다.
            %s

            본인이 요청하지 않았다면 이 메일을 무시하셔도 됩니다.
            """.formatted(userAccount.getUsername(), verificationUrl));

        mJavaMailSender.send(simpleMailMessage);
    }

    @Override
    public void sendPasswordResetMail(UserAccount userAccount, String resetPasswordUrl) {
        if (userAccount == null) {
            throw new IllegalArgumentException("userAccount must not be null.");
        }

        if (resetPasswordUrl == null || resetPasswordUrl.isBlank()) {
            throw new IllegalArgumentException("resetPasswordUrl must not be blank.");
        }

        SimpleMailMessage simpleMailMessage = new SimpleMailMessage();
        simpleMailMessage.setTo(userAccount.getEmailAddress());
        simpleMailMessage.setFrom(mDayLogApplicationProperties.getMail().getFromAddress());
        simpleMailMessage.setSubject("Daymark 비밀번호 재설정 안내");
        simpleMailMessage.setText("""
            안녕하세요, %s님.

            아래 링크에서 Daymark 비밀번호를 다시 설정하실 수 있습니다.
            %s

            이 링크는 일정 시간이 지나면 만료되며 한 번만 사용할 수 있습니다.
            본인이 요청하지 않았다면 이 메일을 무시하셔도 됩니다.
            """.formatted(userAccount.getUsername(), resetPasswordUrl));

        mJavaMailSender.send(simpleMailMessage);
    }
}
