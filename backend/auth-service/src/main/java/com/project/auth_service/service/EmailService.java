package com.project.auth_service.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    public void sendActivationEmail(String to, String username, String token) {

        log.info("📧 [EMAIL] Preparing activation email for {}", to);

        String link = "http://localhost:8098/api/auth/set-password?token=" + token;

        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setTo(to);
        msg.setSubject("مرحبًا بك في النظام الذكي لمعصرة الجمعية التعاونية – بيت جالا");
        msg.setText("""
                مرحبًا بك 🌿،

                يسرّنا انضمامك إلى النظام الذكي لمعصرة الجمعية التعاونية في بيت جالا.

                تم إنشاء حساب خاص بك على النظام بنجاح، واسم المستخدم الخاص بك هو:

                اسم المستخدم:
                %s

                لتفعيل حسابك وتعيين كلمة المرور الخاصة بك، يرجى الضغط على الرابط التالي:

                رابط تفعيل الحساب:
                %s

                بعد الضغط على الرابط سيتم نقلك مباشرة إلى صفحة تعيين كلمة المرور،
                ولا حاجة لنسخ أي رمز أو إدخال أي معلومات إضافية.

                ملاحظات مهمة:
                • رابط التفعيل صالح للاستخدام مرة واحدة فقط.
                • تنتهي صلاحية الرابط بعد 24 ساعة.

                مع خالص التحية،
                إدارة النظام الذكي
                معصرة الجمعية التعاونية – بيت جالا
                """.formatted(username, link));

        mailSender.send(msg);

        log.info("✅ [EMAIL] Activation email sent successfully to {}", to);
    }
}
